package client;

import javafx.application.Platform;
import main.Chat;
import main.Group;
import main.RequestResponse;
import main.User;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class Controller implements Runnable{
    private final UIClient client;
    private final ObjectInputStream reader;
    private final ObjectOutputStream writer;
    private User user;
    private Group currentGroup;
    private final CyclicBarrier waitResponse = new CyclicBarrier(2);
    private boolean registrationRes = false;


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
                } else if (serverResponse.equals(RequestResponse.LOGIN_ERROR.name())) {
                    waitResponse.await();
                } else if (serverResponse.equals(RequestResponse.SUCCESSFUL_REGISTRATION.name())) {
                    registrationRes = true;
                    waitResponse.await();
                } else if (serverResponse.equals(RequestResponse.REGISTRATION_ERROR.name())) {
                    registrationRes = false;
                    waitResponse.await();
                } else if (serverResponse.equals(RequestResponse.UPDATE_CHATS.name())) {
                    Group newGroup = (Group) reader.readObject();
                    user.addGroup(newGroup);
                    Platform.runLater(() -> client.addGroup(newGroup));
                } else if (serverResponse.equals(RequestResponse.UPDATE_MESSAGES.name())) {
                    List<String> data = (List<String>) reader.readObject();
                    Group group = user.getGroup(data.getFirst());
                    if (group != null) {
                        group.sendMessage(data.get(1));
                        if (currentGroup != null && currentGroup.getName().equals(data.get(0))) {
                            Platform.runLater(() -> client.addMessage(data.get(1)));
                        }
                    }
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
                    Platform.runLater(() -> client.removeFRForUser(user1));
                } else if (serverResponse.equals(RequestResponse.REMOVE_FR_FROM_USER.name())) {
                    User user1 = (User) reader.readObject();
                    user.getFRequestsFromUser().remove(user1);
                    Platform.runLater(() -> client.removeFRFromUser(user1));
                } else if (serverResponse.equals(RequestResponse.ADD_FR_FOR_USER.name())) {
                    User user1 = (User) reader.readObject();
                    user.getFRequestsForUser().add(user1);
                    Platform.runLater(() -> client.addFRForUser(user1));
                } else if (serverResponse.equals(RequestResponse.ADD_FR_FROM_USER.name())) {
                    User user1 = (User) reader.readObject();
                    user.getFRequestsFromUser().add(user1);
                    Platform.runLater(() -> client.addFRFromUser(user1));
                } else if (serverResponse.equals(RequestResponse.DELETE_GROUP.name()) ||
                        serverResponse.equals(RequestResponse.DELETE_CHAT.name())) {
                    Group group = (Group) reader.readObject();
                    user.deleteGroup(group);
                    Platform.runLater(() -> client.deleteGroup(group));
                } else if (serverResponse.equals(RequestResponse.DELETE_MEMBER.name())) {
                    List<Object> data = (List<Object>) reader.readObject();
                    user.getGroup((String) data.getFirst()).deleteMember((User) data.getLast());
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

    public boolean registration(String username, String password){
        try {
            writer.writeUTF(RequestResponse.REGISTRATION.name());
            writer.writeUTF(username);
            writer.writeUTF(password);
            writer.flush();
            waitResponse.await();
            return registrationRes;
        } catch (IOException | InterruptedException | BrokenBarrierException e) {
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

    public List<Group> getGroups(){
        return user.getGroups();
    }

    public void setCurrentChat(Group group) {
        currentGroup = group;
        try {
            writer.writeUTF(RequestResponse.SET_CURRENT_CHAT.name());
            writer.writeObject(group.getName());
            writer.flush();
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<String> getMessages(){
        return currentGroup.getMessages();
    }

    public void createGroup(String title, List<String> users){
        try {
            writer.writeUTF(RequestResponse.CREATE_GROUP.name());
            writer.writeUTF(title);
            writer.writeObject(users);
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void createChat(String user){
        try {
            writer.writeUTF(RequestResponse.CREATE_CHAT.name());
            writer.writeUTF(user);
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

    public Group getCurrentChat() {
        return currentGroup;
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

    public User getCurrentUser(){
        return user;
    }

    public void deleteGroup(Group group){
        try {
            writer.writeUTF(RequestResponse.DELETE_GROUP.name());
            writer.writeUTF(group.getName());
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteChat(Chat chat){
        try {
            writer.writeUTF(RequestResponse.DELETE_CHAT.name());
            writer.writeUTF(chat.getName());
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
