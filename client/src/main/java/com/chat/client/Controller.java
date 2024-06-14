package com.chat.client;

import com.chat.client.model.*;
import com.chat.shared.*;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

import static com.chat.shared.RequestResponse.Title.*;


public class Controller implements Runnable{
    private final UIClient client;
    private final ObjectInputStream reader;
    private final ObjectOutputStream writer;
    private User currentUser;
    private final CyclicBarrier wait = new CyclicBarrier(2);
    private AbstractChat loadedChat;

    public Controller(UIClient client, Socket socket) throws IOException {
        this.client = client;
        this.writer = new ObjectOutputStream(socket.getOutputStream());
        this.reader = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                RequestResponse serverResponse = (RequestResponse) reader.readObject();

                if (serverResponse.getTitle().equals(SUCCESSFUL_LOGIN)){
                    currentUser = new User(serverResponse.getField("username"));
                    currentUser.setChatData(serverResponse.getField("chatData"));
                    currentUser.setFriends(serverResponse.getField("friends"));
                    currentUser.setfRequestsForUser(serverResponse.getField("fRequestsForUser"));
                    currentUser.setfRequestsFromUser(serverResponse.getField("fRequestsFromUser"));
                    Platform.runLater(() -> client.logIn(true));
                } else if (serverResponse.getTitle().equals(LOGIN_ERROR)) {
                    Platform.runLater(() -> client.logIn(false));
                } else if (serverResponse.getTitle().equals(SUCCESSFUL_REGISTRATION)) {
                    Platform.runLater(() -> client.registration(true));
                } else if (serverResponse.getTitle().equals(REGISTRATION_ERROR)) {
                    Platform.runLater(() -> client.registration(false));
                } else if (serverResponse.getTitle().equals(UPDATE_CHATS)) {
                    AbstractChat chat;
                    ChatType type = serverResponse.getField("type");
                    String publicName;
                    String privateName;
                    if (type == ChatType.CHAT) {
                        chat = new Chat(serverResponse.getField("id"),
                                serverResponse.<ArrayList<String>>getField("members").getFirst(),
                                serverResponse.<ArrayList<String>>getField("members").get(1));
                        publicName = ((Chat) chat).getPseudonym(currentUser.getUsername());
                        privateName = chat.getName();
                    } else if (type == ChatType.GROUP) {
                        chat = new Group(serverResponse.getField("id"), serverResponse.getField("chatName"));
                        chat.setMembers(serverResponse.getField("members"));
                        publicName = chat.getName();
                        privateName = chat.getName();
                    } else {
                        chat = null;
                        privateName = "";
                        publicName = "";
                    }
                    currentUser.addChat(chat);
                    Platform.runLater(() -> client.addChat(new ChatData(chat.getId(), type, publicName, privateName)));
                } else if (serverResponse.getTitle().equals(GET_CHAT)) {
                    ChatType type = serverResponse.getField("type");
                    if (type == ChatType.GROUP) {
                        loadedChat = new Group(serverResponse.getField("id"), serverResponse.getField("chatName"));
                        loadedChat.setMembers(serverResponse.getField("members"));
                    } else if (type == ChatType.CHAT) {
                        loadedChat = new Chat(serverResponse.getField("id"),
                                serverResponse.<ArrayList<String>>getField("members").getFirst(),
                                serverResponse.<ArrayList<String>>getField("members").getLast());
                    }
                    loadedChat.setMessages(serverResponse.getField("messages"));
                    currentUser.addChat(loadedChat);
                    wait.await();
                } else if (serverResponse.getTitle().equals(UPDATE_MESSAGES)) {
                    int id = serverResponse.getField("id");
                    String message = serverResponse.getField("message");
                    AbstractChat chat = currentUser.getChat(id);
                    if (chat != null) {
                        chat.sendMessage(message);
                        Platform.runLater(() -> client.addMessage(id, message));
                    }
                } else if (serverResponse.getTitle().equals(DELETE_FRIEND)) {
                    String username = serverResponse.getField("username");
                    currentUser.deleteFriend(username);
                    Platform.runLater(() -> client.deleteFriend(username));
                } else if (serverResponse.getTitle().equals(ADD_FRIEND)) {
                    String username = serverResponse.getField("username");
                    currentUser.addFriend(username);
                    Platform.runLater(() -> client.addFriend(username));
                } else if (serverResponse.getTitle().equals(REMOVE_FR_FOR_USER)) {
                    String username = serverResponse.getField("username");
                    currentUser.deleteFRequestForUser(username);
                    Platform.runLater(() -> client.removeFRForUser(username));
                } else if (serverResponse.getTitle().equals(REMOVE_FR_FROM_USER)) {
                    String username = serverResponse.getField("username");
                    currentUser.deleteFRequestFromUser(username);
                    Platform.runLater(() -> client.removeFRFromUser(username));
                } else if (serverResponse.getTitle().equals(ADD_FR_FOR_USER)) {
                    String username = serverResponse.getField("username");
                    currentUser.addFRequestForUser(username);
                    Platform.runLater(() -> client.addFRForUser(username));
                } else if (serverResponse.getTitle().equals(ADD_FR_FROM_USER)) {
                    String username = serverResponse.getField("username");
                    currentUser.addFRequestFromUser(username);
                    Platform.runLater(() -> client.addFRFromUser(username));
                }  else if (serverResponse.getTitle().equals(DELETE_GROUP) ||
                        serverResponse.getTitle().equals(DELETE_CHAT)) {
                    int id = serverResponse.getField("id");
                    currentUser.deleteChat(id);
                    Platform.runLater(() -> client.deleteChat(id));
                } else if (serverResponse.getTitle().equals(DELETE_MEMBER)) {
                    int id = serverResponse.getField("id");
                    String username = serverResponse.getField("username");
                    AbstractChat chat = currentUser.getChat(id);
                    if (chat != null) {
                        chat.deleteMember(username);
                    }
                }
            } catch (SocketException socketException) {
                break;
            } catch (IOException | ClassNotFoundException e) {
                Platform.runLater(() -> client.showError("Connection problems"));
            } catch (BrokenBarrierException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void exit(){
        try {
            writer.writeObject(new RequestResponse(EXIT));
            writer.flush();
            writer.close();
            reader.close();
        } catch (IOException ignored) {}
    }

    public void registration(String username, String password){
        try {
            RequestResponse request = new RequestResponse(REGISTRATION);
            request.setField("username", username);
            request.setField("password", password);
            writer.writeObject(request);
            writer.flush();
        } catch (IOException e) {
            client.showError("Registration error");
        }
    }

    public void login(String username, String password) {
        try {
            RequestResponse request = new RequestResponse(LOG_IN);
            request.setField("username", username);
            request.setField("password", password);
            writer.writeObject(request);
            writer.flush();
        } catch (IOException e) {
            client.showError("Login error");
        }
    }

    public List<ChatData> getChatNames(){
        return currentUser.getChatData();
    }

    public List<String> getMessages(int id){
        return currentUser.getChat(id).getMessages();
    }

    public void createGroup(String title, ArrayList<String> users){
        try {
            RequestResponse request = new RequestResponse(CREATE_GROUP);
            request.setField("chatName", title);
            request.setField("members", users);
            writer.writeObject(request);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void createChat(String user){
        try {
            RequestResponse response = new RequestResponse(CREATE_CHAT);
            response.setField("username", user);
            writer.writeObject(response);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(int id, String chatName, String message){
        try {
            RequestResponse request = new RequestResponse(SEND_MESSAGE);
            request.setField("id", id);
            request.setField("chatName", chatName);
            request.setField("message", message);
            writer.writeObject(request);
            writer.flush();
        } catch (IOException e) {
            client.showError("Connection error");
        }
    }

    public List<String> getFriends(){
        return currentUser.getFriends();
    }

    public List<String> getFRequestsForUser(){
        return currentUser.getFRequestsForUser();
    }

    public List<String> getFRequestsFromUser(){
        return currentUser.getFRequestsFromUser();
    }

    public void deleteFriend(String friend){
        try {
            RequestResponse request = new RequestResponse(DELETE_FRIEND);
            request.setField("friendUsername", friend);
            writer.writeObject(request);
            writer.flush();
        } catch (IOException e) {
            client.showError("Delete friend error");
        }
    }

    public void addFriend(String friend){
        try {
            RequestResponse request = new RequestResponse(ADD_FRIEND);
            request.setField("friendUsername", friend);
            writer.writeObject(request);
            writer.flush();
        } catch (IOException e) {
            client.showError("Add friend error");
        }
    }

    public void removeFRForUser(String user){
        try{
            RequestResponse request = new RequestResponse(REMOVE_FR_FOR_USER);
            request.setField("username", user);
            writer.writeObject(request);
            writer.flush();
        } catch (IOException e) {
            client.showError("Connection error");
        }
    }

    public void removeFRFromUser(String user){
        try{
            RequestResponse request = new RequestResponse(REMOVE_FR_FROM_USER);
            request.setField("username", user);
            writer.writeObject(request);
            writer.flush();
        } catch (IOException e) {
            client.showError("Connection error");
        }
    }

    public void sendFriendRequest(String username){
        try {
            RequestResponse request = new RequestResponse(SEND_FRIEND_REQUEST);
            request.setField("username", username);
            writer.writeObject(request);
            writer.flush();
        } catch (IOException e) {
            client.showError("Connection error");
        }
    }

    public void deleteChat(int id, String chatName){
       delete(id, chatName, DELETE_CHAT);
    }

    public void deleteGroup(int id, String groupName){
        delete(id, groupName, DELETE_GROUP);
    }

    private void delete(int id, String chatName, RequestResponse.Title title) {
        try {
            RequestResponse request = new RequestResponse(title);
            request.setField("id", id);
            request.setField("chatName", chatName);
            writer.writeObject(request);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public AbstractChat load(int id) {
        AbstractChat chat = currentUser.getChat(id);
        if (chat != null) {
            return chat;
        }else {
            try {
                RequestResponse request = new RequestResponse(GET_CHAT);
                request.setField("id", id);
                writer.writeObject(request);
                writer.flush();
                wait.await();
                return loadedChat;
            } catch (IOException | BrokenBarrierException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
