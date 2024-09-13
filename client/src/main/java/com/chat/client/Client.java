package com.chat.client;

import com.chat.client.elements.*;
import com.chat.shared.ChatData;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.Socket;

public class Client extends Application {

    private Stage primaryStage;
    private Scene scene;
    private Socket socket;

    private final MainMenu mainMenu = new MainMenu(this);
    private final RegMenu registrationMenu = new RegMenu(this);
    private final LogMenu logInMenu = new LogMenu(this);
    private ChatMenu chatMenu;
    private ChatUI chat;
    private FriendMenu friendMenu;
    private final TabMenu tabMenu = new TabMenu();

    public Controller controller;
    private Thread listenerThread;

    private boolean allOk = true;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        super.init();
        try {
            socket = new Socket("localhost", 8099);
            controller = new Controller(this, socket);
        } catch (IOException ex) {
            allOk = false;
        }

        listenerThread = new Thread(controller);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        if (allOk) {
            scene = new Scene(mainMenu);
            stage.setOnCloseRequest(e -> {
                try {
                    controller.exit();
                    socket.close();
                    listenerThread.interrupt();
                } catch (IOException ex) {
                    stage.close();
                }

            });
            stage.setScene(scene);
            stage.setTitle("Chat");
            stage.setHeight(600);
            stage.setWidth(600);
            stage.centerOnScreen();
            stage.show();
        }else {
            showError("Connection error");
        }
    }

    public void addMessage(int id, String message) {
        if (chat != null && chat.getData().id() == id) {
            chat.addMessage(message);
        }
    }

    public void addChat(ChatData chatData){
        chatMenu.addChat(chatData);
    }

    public void deleteChat(int id){
        chatMenu.deleteChat(id);
    }

    public void deleteFriend(String username){
        friendMenu.deleteFriend(username);
    }

    public void addFriend(String username){
        friendMenu.addFriend(username);
    }

    public void removeFRForUser(String username){
        friendMenu.removeFRForUser(username);
    }

    public void removeFRFromUser(String username){
        friendMenu.removeFRFromUser(username);
    }

    public void addFRForUser(String username){
        friendMenu.addFRForUser(username);
    }

    public void addFRFromUser(String username){
        friendMenu.addFRFromUser(username);
    }

    public void successLogIn() {
        chatMenu = new ChatMenu(this, controller.getChatNames());

        friendMenu = new FriendMenu(this);
        tabMenu.addTab("Chats", chatMenu);
        tabMenu.addTab("Friends", friendMenu);
        scene.setRoot(tabMenu);
        primaryStage.setTitle(primaryStage.getTitle() + "(" + controller.getCurrentUser().getUsername() + ")");
    }

    public void showError(String message){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.show();
    }

    public void setRoot(Parent parent) {
        scene.setRoot(parent);
    }

    public void setChat(ChatUI chat) {
        this.chat = chat;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public RegMenu getRegistrationMenu() {
        return registrationMenu;
    }

    public LogMenu getLogInMenu() {
        return logInMenu;
    }

    public MainMenu getMainMenu() {
        return mainMenu;
    }

    public TabMenu getTabMenu() {
        return tabMenu;
    }

    public void refresh() {
        friendMenu.refresh();
    }
}
