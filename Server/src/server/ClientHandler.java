package server;

import main.Chat;
import main.Group;
import main.RequestResponse;
import main.User;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    //ответы которые надо отправить клиенту
    private final BlockingQueue<ServerResponse> responses = new ArrayBlockingQueue<>(100);
    private final ObjectInputStream reader;
    private final ObjectOutputStream objectOutputStream;

    private User currentUser;
    private Group currentGroup;

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
            String str;
            Thread senderThread = new Thread(new Sender(), "Sender"+currThreadName.substring(currThreadName.indexOf("-")));
            senderThread.start();

            while (!Thread.currentThread().isInterrupted() && (str = reader.readUTF()) != null) {
                if (str.equals(RequestResponse.REGISTRATION.name())) {
                    System.out.println("Start registration " + currThreadName);
                    registration();
                    System.out.println("End registration " + currThreadName);
                } else if (str.equals(RequestResponse.LOG_IN.name())) {
                    System.out.println("Start login " + currThreadName);
                    logIn();
                    System.out.println("End login " + currThreadName);
                } else if (str.equals(RequestResponse.CREATE_GROUP.name())) {
                    Group group = new Group(reader.readUTF());
                    group.addMember(currentUser);
                    List<String> memberNames = (List<String>) reader.readObject();
                    for (String memberName : memberNames){
                        User user = server.getUser(memberName);
                        if (user != null) {
                            user.addGroup(group);
                            server.notifyClientHandlers(user, new ServerResponse(RequestResponse.UPDATE_CHATS, group));
                            group.addMember(user);
                        }
                    }
                    currentUser.addGroup(group);
                    server.notifyClientHandlers(currentUser, new ServerResponse(RequestResponse.UPDATE_CHATS, group));
                } else if (str.equals(RequestResponse.CREATE_CHAT.name())) {
                    User user = server.getUser(reader.readUTF());
                    if (user != null) {
                        Chat chat = new Chat(currentUser, user);
                        chat.addMember(currentUser);
                        user.addGroup(chat);
                        chat.addMember(user);
                        server.notifyClientHandlers(user, new ServerResponse(RequestResponse.UPDATE_CHATS, chat));
                        currentUser.addGroup(chat);
                        server.notifyClientHandlers(currentUser, new ServerResponse(RequestResponse.UPDATE_CHATS, chat));
                    }
                } else if (str.equals(RequestResponse.DELETE_GROUP.name())) {
                    System.out.println("DELETE GROUP");
                    System.out.println("Before: " + currentUser.getGroups());
                    Group group = currentUser.getGroup(reader.readUTF());
                    group.deleteMember(currentUser);
                    currentUser.deleteGroup(group);
                    for (User user : group.getMembers()){
                        server.notifyClientHandlers(user, new ServerResponse(RequestResponse.DELETE_MEMBER,
                                List.<Object>of(group.getName(), currentUser)));
                    }
                    server.notifyClientHandlers(currentUser, new ServerResponse(RequestResponse.DELETE_GROUP, group));
                    System.out.println("After: " + currentUser.getGroups());
                } else if (str.equals(RequestResponse.DELETE_CHAT.name())) {
                    System.out.println("DELETE CHAT");
                    System.out.println("Before: " + currentUser.getGroups());
                    Chat chat = (Chat) currentUser.getGroup(reader.readUTF());
                    for (User user : chat.getMembers()){
                        user.deleteGroup(chat);
                        server.notifyClientHandlers(user, new ServerResponse(RequestResponse.DELETE_CHAT, chat));
                    }
                    System.out.println("After: " + currentUser.getGroups());
                } else if (str.equals(RequestResponse.SET_CURRENT_CHAT.name())) {
                    currentGroup = currentUser.getGroup((String) reader.readObject());
                } else if (str.equals(RequestResponse.SEND_MESSAGE.name())) {
                    String message = reader.readUTF();
                    currentGroup.sendMessage(currentUser.getUsername() + ": " + message);
                    for (User user : currentGroup.getMembers()){
                        server.notifyClientHandlers(user, new ServerResponse(RequestResponse.UPDATE_MESSAGES,
                                List.of(currentGroup.getName(), currentUser.getUsername() + ": " + message)));
                    }
                } else if (str.equals(RequestResponse.DELETE_FRIEND.name())) {
                    User deleteFriend = server.getUser(reader.readUTF());
                    if (currentUser.deleteFriend(deleteFriend) && deleteFriend.deleteFriend(currentUser)) {
                        server.notifyClientHandlers(currentUser, new ServerResponse(RequestResponse.DELETE_FRIEND, deleteFriend));
                        server.notifyClientHandlers(deleteFriend, new ServerResponse(RequestResponse.DELETE_FRIEND, currentUser));
                    }

                } else if (str.equals(RequestResponse.ADD_FRIEND.name())) {
                    User newFriend = server.getUser(reader.readUTF());
                    if (currentUser.addFriend(newFriend) && newFriend.addFriend(currentUser)
                       && currentUser.deleteFRequestForUser(newFriend) && newFriend.deleteFRequestFromUser(currentUser)){
                        server.notifyClientHandlers(currentUser, new ServerResponse(RequestResponse.REMOVE_FR_FOR_USER, newFriend));
                        server.notifyClientHandlers(currentUser, new ServerResponse(RequestResponse.ADD_FRIEND, newFriend));
                        server.notifyClientHandlers(newFriend, new ServerResponse(RequestResponse.REMOVE_FR_FROM_USER, currentUser));
                        server.notifyClientHandlers(newFriend, new ServerResponse(RequestResponse.ADD_FRIEND, currentUser));
                    }
                } else if (str.equals(RequestResponse.REMOVE_FR_FOR_USER.name())) {
                    User user = server.getUser(reader.readUTF());
                    if (currentUser.deleteFRequestForUser(user) && user.deleteFRequestFromUser(currentUser)) {
                        server.notifyClientHandlers(currentUser, new ServerResponse(RequestResponse.REMOVE_FR_FOR_USER, user));
                        server.notifyClientHandlers(user, new ServerResponse(RequestResponse.REMOVE_FR_FROM_USER, currentUser));
                    }

                } else if (str.equals(RequestResponse.REMOVE_FR_FROM_USER.name())) {
                    User user = server.getUser(reader.readUTF());
                    if (currentUser.deleteFRequestFromUser(user) && user.deleteFRequestForUser(currentUser)) {
                        server.notifyClientHandlers(currentUser, new ServerResponse(RequestResponse.REMOVE_FR_FROM_USER, user));
                        server.notifyClientHandlers(user, new ServerResponse(RequestResponse.REMOVE_FR_FOR_USER, currentUser));
                    }

                } else if (str.equals(RequestResponse.SEND_FRIEND_REQUEST.name())) {
                    String username = reader.readUTF();
                    User user = server.getUser(username);
                    if (user != null){
                        if (currentUser.addFRequestFromUser(user) && user.addFRequestForUser(currentUser)){
                            server.notifyClientHandlers(currentUser, new ServerResponse(RequestResponse.ADD_FR_FROM_USER, user));
                            server.notifyClientHandlers(user, new ServerResponse(RequestResponse.ADD_FR_FOR_USER, currentUser));
                        }
                    }
                } else if (str.equals(RequestResponse.EXIT.name())) {
                    System.out.println("Exit from system " + currThreadName);
                    responses.add(new ServerResponse(RequestResponse.EXIT, null));
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

    private void registration() {
        try {
            String username = reader.readUTF();
            String password = reader.readUTF();

            User user = new User(username, getHash(password));
            boolean res = server.addUser(user);
            if (res){
                responses.add(new ServerResponse(RequestResponse.SUCCESSFUL_REGISTRATION, null));
            }else {
                responses.add(new ServerResponse(RequestResponse.REGISTRATION_ERROR, null));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void logIn() {
        try {
            String username = reader.readUTF();
            String password = reader.readUTF();

            User user = server.getUser(username, getHash(password));
            RequestResponse response;
            if (user != null) {
                currentUser = user;
                server.addClientHandler(user, this);
                response = RequestResponse.SUCCESSFUL_LOGIN;
            } else {
                response = RequestResponse.LOGIN_ERROR;
            }
            responses.add(new ServerResponse(response, user));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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


    public void addServerResponse(ServerResponse serverResponse) {
        responses.add(serverResponse);
    }

    //поток отправляющий все задачи клиенту
    private class Sender implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ServerResponse serverResponse = responses.take();
                    if (serverResponse.response().equals(RequestResponse.EXIT)) {
                        break;
                    }
                    objectOutputStream.writeUTF(serverResponse.response().name());
                    objectOutputStream.flush();
                    if (serverResponse.writeObject() != null) {
                        objectOutputStream.writeObject(serverResponse.writeObject());
                        objectOutputStream.flush();
                    }
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
