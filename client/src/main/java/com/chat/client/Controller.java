package com.chat.client;

import com.chat.shared.Group;
import com.chat.shared.RequestResponse;
import com.chat.shared.User;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import static com.chat.shared.RequestResponse.Title.*;


public class Controller implements Runnable{
    private final UIClient client;
    private final ObjectInputStream reader;
    private final ObjectOutputStream writer;
    private User currentUser;
    private Group currentChat;

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
                    currentUser = serverResponse.getField("user");
                    Platform.runLater(() -> client.logIn(true));
                } else if (serverResponse.getTitle().equals(LOGIN_ERROR)) {
                    Platform.runLater(() -> client.logIn(false));
                } else if (serverResponse.getTitle().equals(SUCCESSFUL_REGISTRATION)) {
                    Platform.runLater(() -> client.registration(true));
                } else if (serverResponse.getTitle().equals(REGISTRATION_ERROR)) {
                    Platform.runLater(() -> client.registration(false));
                } else if (serverResponse.getTitle().equals(UPDATE_CHATS)) {
                    Group newChat = serverResponse.getField("chat");
                    currentUser.addChat(newChat);
                    Platform.runLater(() -> client.addChat(newChat));
                } else if (serverResponse.getTitle().equals(UPDATE_MESSAGES)) {
                    String chatName = serverResponse.getField("chatName");
                    String message = serverResponse.getField("message");
                    Group chat = currentUser.getChat(chatName);
                    if (chat != null) {
                        chat.sendMessage(message);
                        if (currentChat != null && currentChat.getName().equals(chatName)) {
                            Platform.runLater(() -> client.addMessage(message));
                        }
                    }
                } else if (serverResponse.getTitle().equals(DELETE_FRIEND)) {
                    User deleteFriend = serverResponse.getField("deleteFriend");
                    currentUser.getFriends().remove(deleteFriend);
                    Platform.runLater(() -> client.deleteFriend(deleteFriend));
                } else if (serverResponse.getTitle().equals(ADD_FRIEND)) {
                    User newFriend = serverResponse.getField("newFriend");
                    currentUser.getFriends().add(newFriend);
                    Platform.runLater(() -> client.addFriend(newFriend));
                } else if (serverResponse.getTitle().equals(REMOVE_FR_FOR_USER)) {
                    User user1 = serverResponse.getField("user");
                    currentUser.getFRequestsForUser().remove(user1);
                    Platform.runLater(() -> client.removeFRForUser(user1));
                } else if (serverResponse.getTitle().equals(REMOVE_FR_FROM_USER)) {
                    User user1 = serverResponse.getField("user");
                    currentUser.getFRequestsFromUser().remove(user1);
                    Platform.runLater(() -> client.removeFRFromUser(user1));
                } else if (serverResponse.getTitle().equals(ADD_FR_FOR_USER)) {
                    User user1 = serverResponse.getField("user");
                    currentUser.getFRequestsForUser().add(user1);
                    Platform.runLater(() -> client.addFRForUser(user1));
                } else if (serverResponse.getTitle().equals(ADD_FR_FROM_USER)) {
                    User user1 = serverResponse.getField("user");
                    currentUser.getFRequestsFromUser().add(user1);
                    Platform.runLater(() -> client.addFRFromUser(user1));
                }  else if (serverResponse.getTitle().equals(DELETE_GROUP) ||
                        serverResponse.getTitle().equals(DELETE_CHAT)) {
                    Group group = serverResponse.getField("chat");
                    currentUser.deleteChat(group);
                    Platform.runLater(() -> client.deleteChat(group));
                } else if (serverResponse.getTitle().equals(DELETE_MEMBER)) {
                    String chatName = serverResponse.getField("chatName");
                    User user = serverResponse.getField("deleteUser");
                    currentUser.getChat(chatName).deleteMember(user);
                }
            } catch (SocketException socketException) {
                break;
            } catch (IOException | ClassNotFoundException e) {
                client.showError("Connection problems");
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

    public List<Group> getChats(){
        return currentUser.getChats();
    }

    public void setCurrentChat(Group chat) {
        currentChat = chat;
    }

    public List<String> getMessages(){
        return currentChat.getMessages();
    }

    public void createGroup(String title, ArrayList<String> users){
        try {
            RequestResponse request = new RequestResponse(CREATE_GROUP);
            request.setField("name", title);
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

    public void sendMessage(String message){
        try {
            RequestResponse request = new RequestResponse(SEND_MESSAGE);
            request.setField("chatName", currentChat.getName());
            request.setField("message", message);
            writer.writeObject(request);
            writer.flush();
        } catch (IOException e) {
            client.showError("Connection error");
        }
    }

    public Group getCurrentChat() {
        return currentChat;
    }

    public List<User> getFriends(){
        return currentUser.getFriends();
    }

    public List<User> getFRequestsForUser(){
        return currentUser.getFRequestsForUser();
    }

    public List<User> getFRequestsFromUser(){
        return currentUser.getFRequestsFromUser();
    }

    public void deleteFriend(User friend){
        try {
            RequestResponse request = new RequestResponse(DELETE_FRIEND);
            request.setField("friendUsername", friend.getUsername());
            writer.writeObject(request);
            writer.flush();
        } catch (IOException e) {
            client.showError("Delete friend error");
        }
    }

    public void addFriend(User friend){
        try {
            RequestResponse request = new RequestResponse(ADD_FRIEND);
            request.setField("friendUsername", friend.getUsername());
            writer.writeObject(request);
            writer.flush();
        } catch (IOException e) {
            client.showError("Add friend error");
        }
    }

    public void removeFRForUser(User user){
        try{
            RequestResponse request = new RequestResponse(REMOVE_FR_FOR_USER);
            request.setField("username", user.getUsername());
            writer.writeObject(request);
            writer.flush();
        } catch (IOException e) {
            client.showError("Connection error");
        }
    }

    public void removeFRFromUser(User user){
        try{
            RequestResponse request = new RequestResponse(REMOVE_FR_FROM_USER);
            request.setField("username", user.getUsername());
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

    public User getCurrentUser(){
        return currentUser;
    }
    
    public void deleteChat(Group chat){
        try {
            RequestResponse request = new RequestResponse(DELETE_CHAT);
            request.setField("chatName", chat.getName());
            writer.writeObject(request);
            writer.flush();
        } catch (IOException e) {
            client.showError("Connection error");
        }
    }
}
