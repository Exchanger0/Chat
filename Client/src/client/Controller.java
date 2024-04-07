package client;

import javafx.application.Platform;
import main.Chat;
import main.RequestResponse;
import main.User;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

public class Controller implements Runnable{
    private final UIClient client;
    private final ObjectInputStream reader;
    private final ObjectOutputStream writer;
    private User user;
    private Chat currentChat;
    private final CyclicBarrier waitResponse = new CyclicBarrier(2);


    public Controller(UIClient client, Socket socket) throws IOException {
        this.client = client;
        this.writer = new ObjectOutputStream(socket.getOutputStream());
        this.reader = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String serverResponse = reader.readUTF();

                if (serverResponse.equals(RequestResponse.SUCCESSFUL_LOGIN.name())){
                    user = (User) reader.readObject();
                    waitResponse.await();
                } else if (serverResponse.equals(RequestResponse.UPDATE_CHATS.name())) {
                    Chat newChat = (Chat) reader.readObject();
                    user.addChat(newChat);
                    Platform.runLater(() -> client.addChat(newChat));
                } else if (serverResponse.equals(RequestResponse.UPDATE_MESSAGES.name())) {
                    List<String> data = (List<String>) reader.readObject();
                    user.getChat(data.getFirst()).sendMessage(data.get(1));
                    Platform.runLater(() -> client.addMessage(data.get(1)));
                } else if (serverResponse.equals(RequestResponse.DELETE_FRIEND.name())) {
                    User deleteFriend = (User) reader.readObject();
                    user.getFriends().remove(deleteFriend);
                    Platform.runLater(() -> client.deleteFriend(deleteFriend));
                } else if (serverResponse.equals(RequestResponse.ADD_FRIEND.name())) {
                    User newFriend = (User) reader.readObject();
                    user.getFriends().add(newFriend);
                    Platform.runLater(() -> client.addFriend(newFriend));
                } else if (serverResponse.equals(RequestResponse.REMOVE_FR_FOR_USER.name())) {
                    User user1 = (User) reader.readObject();
                    user.getFRequestsForUser().remove(user1);
                    System.out.println("Controller: remove fr for user");
                    Platform.runLater(() -> client.removeFRForUser(user1));
                } else if (serverResponse.equals(RequestResponse.REMOVE_FR_FROM_USER.name())) {
                    User user1 = (User) reader.readObject();
                    user.getFRequestsFromUser().remove(user1);
                    System.out.println("Controller: remove fr from user");
                    Platform.runLater(() -> client.removeFRFromUser(user1));
                } else if (serverResponse.equals(RequestResponse.ADD_FR_FOR_USER.name())) {
                    User user1 = (User) reader.readObject();
                    user.getFRequestsForUser().add(user1);
                    Platform.runLater(() -> client.addFRForUser(user1));
                } else if (serverResponse.equals(RequestResponse.ADD_FR_FROM_USER.name())) {
                    User user1 = (User) reader.readObject();
                    user.getFRequestsFromUser().add(user1);
                    Platform.runLater(() -> client.addFRFromUser(user1));
                }
            } catch (SocketException socketException) {
                break;
            } catch (IOException | ClassNotFoundException | BrokenBarrierException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void exit(){
        try {
            writer.writeUTF(RequestResponse.EXIT.name());
            writer.flush();
            writer.close();
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void registration(String username, String password){
        try {
            writer.writeUTF(RequestResponse.REGISTRATION.name());
            writer.writeUTF(username);
            writer.writeUTF(password);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean login(String username, String password) {
        try {
            writer.writeUTF(RequestResponse.LOG_IN.name());
            writer.writeUTF(username);
            writer.writeUTF(password);
            writer.flush();
            waitResponse.await();
        } catch (InterruptedException | BrokenBarrierException | IOException e) {
            throw new RuntimeException(e);
        }

        return user != null;
    }

    public List<Chat> getChats(){
        return user.getChats();
    }

    public void setCurrentChat(Chat chat) {
        currentChat = chat;
        try {
            writer.writeUTF(RequestResponse.SET_CURRENT_CHAT.name());
            writer.writeObject(chat.getName());
            writer.flush();
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getMessages(){
        return currentChat.getMessages();
    }

    public List<String> getMembersName(){
        return currentChat.getMembers().stream().map(User::getUsername).collect(Collectors.toList());
    }

    public void createChat(String title, List<String> users){
        try {
            writer.writeUTF(RequestResponse.CREATE_CHAT.name());
            writer.writeUTF(title);
            writer.writeObject(users);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendMessage(String message){
        try {
            writer.writeUTF(RequestResponse.SEND_MESSAGE.name());
            writer.writeUTF(message);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Chat getCurrentChat() {
        return currentChat;
    }

    public List<User> getFriends(){
        return user.getFriends();
    }

    public List<User> getFRequestsForUser(){
        return user.getFRequestsForUser();
    }

    public List<User> getFRequestsFromUser(){
        return user.getFRequestsFromUser();
    }

    public void deleteFriend(User friend){
        try {
            writer.writeUTF(RequestResponse.DELETE_FRIEND.name());
            writer.writeUTF(friend.getUsername());
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void addFriend(User friend){
        try {
            writer.writeUTF(RequestResponse.ADD_FRIEND.name());
            writer.writeUTF(friend.getUsername());
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeFRForUser(User user){
        try{
            writer.writeUTF(RequestResponse.REMOVE_FR_FOR_USER.name());
            writer.writeUTF(user.getUsername());
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void removeFRFromUser(User user){
        try{
            writer.writeUTF(RequestResponse.REMOVE_FR_FROM_USER.name());
            writer.writeUTF(user.getUsername());
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendFriendRequest(String username){
        try {
            writer.writeUTF(RequestResponse.SEND_FRIEND_REQUEST.name());
            writer.writeUTF(username);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
