package com.chat.server;


import com.chat.shared.Chat;
import com.chat.shared.Group;
import com.chat.shared.RequestResponse;
import com.chat.shared.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static com.chat.shared.RequestResponse.Title.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    //ответы которые надо отправить клиенту
    private final BlockingQueue<RequestResponse> responses = new ArrayBlockingQueue<>(100);
    private final ObjectInputStream reader;
    private final ObjectOutputStream objectOutputStream;

    private User currentUser;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            reader = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            String currThreadName = Thread.currentThread().getName();
            RequestResponse request;
            Thread senderThread = new Thread(new Sender(), "Sender"+currThreadName.substring(currThreadName.indexOf("-")));
            senderThread.start();

            while (!Thread.currentThread().isInterrupted() && (request = (RequestResponse) reader.readObject()) != null) {
                if (request.getTitle().equals(REGISTRATION)) {
                    System.out.println("Start registration");
                    registration(request);
                    System.out.println("End registration");
                } else if (request.getTitle().equals(LOG_IN)) {
                    System.out.println("Start login");
                    logIn(request);
                    System.out.println("End login. Current user: "
                            + (currentUser != null ? currentUser.getUsername() : " error"));
                } else if (request.getTitle().equals(CREATE_GROUP)) {
                    createGroup(request);
                    System.out.println("Create group '" + request.getField("name") + "' by '"
                            + currentUser.getUsername() + "'");
                } else if (request.getTitle().equals(CREATE_CHAT)) {
                    createChat(request);
                    System.out.println("'" + currentUser.getUsername() + "' create chat");
                } else if (request.getTitle().equals(DELETE_GROUP)) {
                    deleteGroup(request);
                    System.out.println("delete group '" + request.getField("groupName") + "' by '"
                            + currentUser.getUsername() + "'");
                } else if (request.getTitle().equals(DELETE_CHAT)) {
                    deleteChat(request);
                    System.out.println("delete chat '" + request.getField("chatName") + "' by '"
                            + currentUser.getUsername() + "'");
                } else if (request.getTitle().equals(SEND_MESSAGE)) {
                    sendMessage(request);
                    System.out.println("'" + currentUser.getUsername() + "' send message to chat '"
                            + request.getField("chatName") + "'");
                } else if (request.getTitle().equals(DELETE_FRIEND)) {
                    deleteFriend(request);
                    System.out.println("'" + currentUser.getUsername() + "' delete friend '"
                            + request.getField("friendUsername" + "'"));
                } else if (request.getTitle().equals(ADD_FRIEND)) {
                    addFriend(request);
                    System.out.println("'" + currentUser.getUsername() + "' add friend '"
                            + request.getField("friendUsername") + "'");
                } else if (request.getTitle().equals(REMOVE_FR_FOR_USER)) {
                    removeFRForUser(request);
                } else if (request.getTitle().equals(REMOVE_FR_FROM_USER)) {
                    removeFRFromUser(request);
                } else if (request.getTitle().equals(SEND_FRIEND_REQUEST)) {
                    sendFriendRequest(request);
                } else if (request.getTitle().equals(EXIT)) {
                    System.out.println("Exit from system " + currThreadName);
                    responses.add(new RequestResponse(EXIT));
                    server.deleteClientHandler(currentUser, this);
                    server.removeThisThread(Thread.currentThread());
                    break;
                }
            }
            reader.close();
            objectOutputStream.close();
            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void registration(RequestResponse request) {
        String username = request.getField("username");
        String password = request.getField("password");

        User user = new User(username, getHash(password));
        boolean res = server.addUser(user);
        if (res){
            responses.add(new RequestResponse(SUCCESSFUL_REGISTRATION));
        }else {
            responses.add(new RequestResponse(REGISTRATION_ERROR));
        }
    }

    private void createGroup(RequestResponse request) {
        Group group = new Group(request.getField("name"));
        group.addMember(currentUser);
        ArrayList<String> memberNames = request.getField("members");
        RequestResponse response = new RequestResponse(UPDATE_CHATS);
        response.setField("chat", group);
        for (String memberName : memberNames){
            User user = server.getUser(memberName);
            if (user != null) {
                user.addChat(group);
                server.notifyClientHandlers(user, response);
                group.addMember(user);
            }
        }
        currentUser.addChat(group);
        server.notifyClientHandlers(currentUser, response);
    }

    private void createChat(RequestResponse request) {
        User user = server.getUser(request.getField("username"));
        if (user != null) {
            Chat chat = new Chat(currentUser, user);
            user.addChat(chat);
            currentUser.addChat(chat);
            RequestResponse response = new RequestResponse(UPDATE_CHATS);
            response.setField("chat", chat);
            server.notifyClientHandlers(user, response);
            server.notifyClientHandlers(currentUser, response);
        }
    }

    private void deleteGroup(RequestResponse request) {
        Group group = currentUser.getChat(request.getField("groupName"));
        group.deleteMember(currentUser);
        currentUser.deleteChat(group);
        RequestResponse response = new RequestResponse(DELETE_MEMBER);
        response.setField("groupName", group.getName());
        response.setField("deleteUser", currentUser);
        for (User user : group.getMembers()){
            server.notifyClientHandlers(user, response);
        }
        RequestResponse response1 = new RequestResponse(DELETE_GROUP);
        response1.setField("chat", group);
        server.notifyClientHandlers(currentUser, response1);
    }

    private void deleteChat(RequestResponse request) {
        Group chat = currentUser.getChat(request.getField("chatName"));
        RequestResponse response = new RequestResponse(DELETE_CHAT);
        response.setField("chat", chat);
        for (User user : chat.getMembers()){
            user.deleteChat(chat);
            server.notifyClientHandlers(user, response);
        }
    }

    private void sendMessage(RequestResponse request) {
        String message = currentUser.getUsername() + ": " + request.getField("message");
        Group chat = currentUser.getChat(request.getField("chatName"));
        chat.sendMessage(message);
        RequestResponse response = new RequestResponse(UPDATE_MESSAGES);
        response.setField("chatName", chat.getName());
        response.setField("message", currentUser.getUsername() + ": " + message);
        for (User user : chat.getMembers()){
            server.notifyClientHandlers(user, response);
        }
    }

    private void deleteFriend(RequestResponse request) {
        User deleteFriend = server.getUser(request.getField("friendUsername"));
        if (currentUser.deleteFriend(deleteFriend) && deleteFriend.deleteFriend(currentUser)) {
            RequestResponse response = new RequestResponse(DELETE_FRIEND);
            response.setField("deleteFriend", deleteFriend);
            server.notifyClientHandlers(currentUser, response);
            RequestResponse response1 = new RequestResponse(DELETE_FRIEND);
            response1.setField("deleteFriend", currentUser);
            server.notifyClientHandlers(deleteFriend, response1);
        }
    }

    private void addFriend(RequestResponse request) {
        User newFriend = server.getUser(request.getField("friendUsername"));
        if (currentUser.addFriend(newFriend) && newFriend.addFriend(currentUser)
                && currentUser.deleteFRequestForUser(newFriend) && newFriend.deleteFRequestFromUser(currentUser)){
            RequestResponse response = new RequestResponse(REMOVE_FR_FOR_USER);
            response.setField("user", newFriend);
            RequestResponse response1 = new RequestResponse(ADD_FRIEND);
            response1.setField("newFriend", newFriend);
            server.notifyClientHandlers(currentUser, response);
            server.notifyClientHandlers(currentUser, response1);
            RequestResponse response2 = new RequestResponse(REMOVE_FR_FROM_USER);
            response2.setField("user", currentUser);
            RequestResponse response3 = new RequestResponse(ADD_FRIEND);
            response3.setField("newFriend", currentUser);
            server.notifyClientHandlers(newFriend, response2);
            server.notifyClientHandlers(newFriend, response3);
        }
    }

    private void removeFRForUser(RequestResponse request) {
        User user = server.getUser(request.getField("username"));
        if (currentUser.deleteFRequestForUser(user) && user.deleteFRequestFromUser(currentUser)) {
            RequestResponse response = new RequestResponse(REMOVE_FR_FOR_USER);
            response.setField("user", user);
            server.notifyClientHandlers(currentUser, response);
            RequestResponse response1 = new RequestResponse(REMOVE_FR_FROM_USER);
            response1.setField("user", currentUser);
            server.notifyClientHandlers(user, response1);
        }
    }

    private void removeFRFromUser(RequestResponse request) {
        User user = server.getUser(request.getField("username"));
        if (currentUser.deleteFRequestFromUser(user) && user.deleteFRequestForUser(currentUser)) {
            RequestResponse response = new RequestResponse(REMOVE_FR_FROM_USER);
            response.setField("user", user);
            server.notifyClientHandlers(currentUser, response);
            RequestResponse response1 = new RequestResponse(REMOVE_FR_FOR_USER);
            response1.setField("user", currentUser);
            server.notifyClientHandlers(user, response1);
        }
    }

    private void sendFriendRequest(RequestResponse request) {
        User user = server.getUser(request.getField("username"));
        if (user != null){
            if (currentUser.addFRequestFromUser(user) && user.addFRequestForUser(currentUser)){
                RequestResponse response = new RequestResponse(ADD_FR_FROM_USER);
                response.setField("user", user);
                server.notifyClientHandlers(currentUser, response);
                RequestResponse response1 = new RequestResponse(ADD_FR_FOR_USER);
                response1.setField("user", currentUser);
                server.notifyClientHandlers(user, response1);
            }
        }
    }

    private void logIn(RequestResponse request) {
        String username = request.getField("username");
        String password = request.getField("password");

        User user = server.getUser(username, getHash(password));
        RequestResponse.Title title;
        if (user != null) {
            currentUser = user;
            server.addClientHandler(user, this);
            title = SUCCESSFUL_LOGIN;
        } else {
            title = LOGIN_ERROR;
        }
        RequestResponse response = new RequestResponse(title);
        response.setField("user", user);
        responses.add(response);
    }

    private String getHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            BigInteger bigInteger = new BigInteger(1, hash);

            return bigInteger.toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    public void addServerResponse(RequestResponse serverResponse) {
        responses.add(serverResponse);
    }

    //поток отправляющий все задачи клиенту
    private class Sender implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    RequestResponse serverResponse = responses.take();
                    if (serverResponse.getTitle().equals(EXIT)) {
                        break;
                    }
                    objectOutputStream.writeObject(serverResponse);
                    objectOutputStream.flush();
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
