package client;

import javafx.application.Platform;
import main.Chat;
import main.RequestResponse;
import main.User;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

public class Controller implements Runnable{
    private final UIClient client;
    private final ObjectInputStream objectInputStream;
    private final ObjectOutputStream writer;
    private User user;
    private Chat currentChat;
    private final CyclicBarrier waitResponse = new CyclicBarrier(2);


    public Controller(UIClient client, Socket socket) throws IOException {
        this.client = client;
        this.writer = new ObjectOutputStream(socket.getOutputStream());
        this.objectInputStream = new ObjectInputStream(socket.getInputStream());
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String serverResponse = objectInputStream.readUTF();

                if (serverResponse.equals(RequestResponse.SUCCESSFUL_LOGIN.name())){
                    user = (User) objectInputStream.readObject();
                    waitResponse.await();
                } else if (serverResponse.equals(RequestResponse.UPDATE_CHATS.name())) {
                    Chat newChat = (Chat) objectInputStream.readObject();
                    user.addChat(newChat);
                    Platform.runLater(() -> client.addChat(newChat));
                } else if (serverResponse.equals(RequestResponse.UPDATE_MESSAGES.name())) {
                    List<String> data = (List<String>) objectInputStream.readObject();
                    user.getChat(data.getFirst()).sendMessage(data.get(1));
                    Platform.runLater(() -> client.addMessage(data.get(1)));
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
            objectInputStream.close();
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
}
