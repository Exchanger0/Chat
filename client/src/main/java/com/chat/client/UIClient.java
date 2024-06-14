package com.chat.client;

import com.chat.client.elements.*;
import com.chat.client.model.AbstractChat;
import com.chat.shared.ChatData;
import com.chat.shared.ChatType;
import javafx.application.Application;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.net.Socket;

//todo: рефакторинг, коммит
public class UIClient extends Application {

    private Stage primaryStage;
    private Scene scene;
    private Socket socket;

    private final MainMenu mainMenu = new MainMenu();
    private final RegLogMenu registrationMenu = new RegLogMenu("Registration");
    private final RegLogMenu logInMenu = new RegLogMenu("Log In");
    private ChatMenu chatMenu;
    private ChatUI chat;
    private FriendMenu friendMenu;
    private final CTabPane cTabPane = new CTabPane();

    private static Controller controller;
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
            initLogInEvent();
            initRegEvent();
            initMainMenuButtonActions();

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

    private void initMainMenuButtonActions() {
        mainMenu.setRegButtonAction(e -> {
            registrationMenu.cleanErrorLabels();
            scene.setRoot(registrationMenu);
        });
        mainMenu.setLogInButtonAction(e -> {
            logInMenu.cleanErrorLabels();
            scene.setRoot(logInMenu);
        });
    }

    private void initRegEvent(){
        registrationMenu.setSendButtonAction(e -> controller.registration(registrationMenu.getUsernameField().getText(),
                registrationMenu.getPasswordField().getText()));
        initBackButtonEvent(registrationMenu);
    }

    private void initLogInEvent(){
        logInMenu.setSendButtonAction(e -> controller.login(logInMenu.getUsernameField().getText(),
                logInMenu.getPasswordField().getText()));
        initBackButtonEvent(logInMenu);
    }

    private void initBackButtonEvent(RegLogMenu regLogMenu){
        regLogMenu.setBackButtonAction(e -> {
            regLogMenu.getUsernameField().setText("");
            regLogMenu.getPasswordField().setText("");
            scene.setRoot(mainMenu);
        });
    }

    private Alert getChatExistsAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Chat already exist");
        return alert;
    }

    //возвращает ивент кнопки, который создает диалоговое окно, отвечающее за создание чата/группы
    private EventHandler<ActionEvent> getCreateChatEvent(ListView<ChatData> chatNames) {
        return e -> {
            Dialog<Boolean> dialog = new Dialog<>();
            dialog.setTitle("Create chat");
            DialogPane dialogPane = dialog.getDialogPane();

            VBox root = new VBox();
            root.setSpacing(10);
            root.setAlignment(Pos.CENTER);

            ToggleGroup toggleGroup = new ToggleGroup();
            RadioButton groupButton = new RadioButton("Group");
            groupButton.setUserData("group");
            groupButton.setSelected(true);
            groupButton.setToggleGroup(toggleGroup);
            RadioButton chatButton = new RadioButton("Chat");
            chatButton.setUserData("chat");
            chatButton.setToggleGroup(toggleGroup);
            VBox buttonBox = new VBox(groupButton, chatButton);
            buttonBox.setSpacing(5);

            CreateGroupMenu createGroupMenu = new CreateGroupMenu(controller.getFriends());
            createGroupMenu.setOkButtonAction(e1 -> {
                if (chatNames.getItems()
                        .stream()
                        .anyMatch(data -> createGroupMenu.getChatName().equals(data.privateName()))){
                    dialog.setResult(true);
                    dialog.close();
                    getChatExistsAlert().show();
                    return;
                }
                controller.createGroup(createGroupMenu.getChatName(), createGroupMenu.getSelectedUsers());
                dialog.setResult(true);
                dialog.close();
            });
            createGroupMenu.setCancelButtonAction(e1 -> {
                dialog.setResult(true);
                dialog.close();
            });

            CreateChatMenu createChatMenu = new CreateChatMenu(controller.getFriends());
            createChatMenu.setOkButtonAction(e1 -> {
                if (chatNames.getItems()
                        .stream()
                        .anyMatch(data -> createChatMenu.getSelectedUser().equals(data.publicName()))) {
                    dialog.setResult(true);
                    dialog.close();
                    getChatExistsAlert().show();
                    return;
                }
                controller.createChat(createChatMenu.getSelectedUser());
                dialog.setResult(true);
                dialog.close();
            });
            createChatMenu.setCancelButtonAction(e1 -> {
                dialog.setResult(true);
                dialog.close();
            });

            toggleGroup.selectedToggleProperty().addListener((observer, oldVal, newVal) -> {
                if (newVal.getUserData().equals("group")) {
                    root.getChildren().set(1, createGroupMenu);
                } else if (newVal.getUserData().equals("chat")) {
                    root.getChildren().set(1, createChatMenu);
                }
            });

            root.getChildren().addAll(buttonBox, createGroupMenu);

            dialog.setWidth(300);
            dialog.setHeight(300);
            dialog.setResizable(false);
            dialogPane.setContent(root);
            dialog.initOwner(primaryStage);
            dialog.show();
        };
    }

    //настраивает отображение элементов ListCell<String> и добавляет слушателя событий
    //для каждого элемента списка
    private Callback<ListView<ChatData>, ListCell<ChatData>> getChatNamesCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<ChatData> call(ListView<ChatData> chatListView) {
                ListCell<ChatData> listCell = new ListCell<>() {
                    @Override
                    protected void updateItem(ChatData data, boolean empty) {
                        super.updateItem(data, empty);
                        if (data == null && empty) {
                            setText("");
                        } else {
                            setText(data.publicName());
                        }
                    }
                };

                listCell.setFont(new Font(20));
                listCell.setOnMouseClicked(e -> {
                    if (e.getButton().equals(MouseButton.PRIMARY)) {
                        int index = listCell.getIndex();
                        if (index >= 0 && index < chatListView.getItems().size()) {
                            ChatData data = chatListView.getItems().get(index);
                            AbstractChat chat = controller.load(data.id());

                            UIClient.this.chat = new ChatUI(data, chat.getMembers());
                            initChatActions();
                            scene.setRoot(UIClient.this.chat);

                            for (String s : controller.getMessages(chat.getId())) {
                                UIClient.this.chat.addMessage(s);
                            }
                        }
                    }
                });

                MenuItem menuItem = new MenuItem("Delete");
                menuItem.setOnAction(e -> {
                    ChatData data = listCell.getItem();
                    if (data.type() == ChatType.GROUP) {
                        controller.deleteGroup(data.id(), data.privateName());
                    } else {
                        controller.deleteChat(data.id(), data.privateName());
                    }
                });
                listCell.setContextMenu(new ContextMenu(menuItem));

                return listCell;
            }
        };
    }

    //настраивает отображение списка друзей
    private Callback<ListView<String>, ListCell<String>> getFriendCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> userListView) {
                ListCell<String> listCell = new ListCell<>() {
                    @Override
                    protected void updateItem(String username, boolean empty) {
                        super.updateItem(username, empty);
                        if (username == null && empty) {
                            setText("");
                        } else {
                            setText(username);
                        }
                    }
                };

                MenuItem delete = new MenuItem("Delete");
                delete.setOnAction(e -> controller.deleteFriend(listCell.getItem()));
                listCell.setContextMenu(new ContextMenu(delete));
                return listCell;
            }
        };
    }

    //настраивает отображение списка запросов на дружбу для текущего пользователя
    private Callback<ListView<String>, ListCell<String>> getFRForUserCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> userListView) {
                ListCell<String> listCell = new ListCell<>() {
                    @Override
                    protected void updateItem(String username, boolean empty) {
                        super.updateItem(username, empty);
                        if (username == null && empty) {
                            setText("");
                        } else {
                            setText(username);
                        }
                    }
                };

                MenuItem disagree = new MenuItem("Disagree");
                disagree.setOnAction(e -> controller.removeFRForUser(listCell.getItem()));

                MenuItem agree = new MenuItem("Agree");
                agree.setOnAction(e -> {
                    String username = listCell.getItem();
                    if (username != null){
                        controller.addFriend(username);
                    }
                });
                listCell.setContextMenu(new ContextMenu(disagree, agree));
                return listCell;
            }
        };
    }

    //настраивает отображение списка запросов на дружбу от текущего пользователя
    private Callback<ListView<String>, ListCell<String>> getFRFromUserCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> userListView) {
                ListCell<String> listCell = new ListCell<>() {
                    @Override
                    protected void updateItem(String username, boolean empty) {
                        super.updateItem(username, empty);
                        if (username == null && empty) {
                            setText("");
                        } else {
                            setText(username);
                        }
                    }
                };

                MenuItem cancellation = new MenuItem("Cancellation");
                cancellation.setOnAction(e -> controller.removeFRFromUser(listCell.getItem()));
                
                listCell.setContextMenu(new ContextMenu(cancellation));
                return listCell;
            }
        };
    }

    public void initChatActions() {
        chat.setBackButtonAction(e1 -> scene.setRoot(cTabPane));
        chat.setSendButtonAction(e1 -> {
            if (!chat.getMessage().getText().trim().isEmpty()) {
                controller.sendMessage(chat.getData().id(), chat.getData().privateName(), chat.getMessage().getText().trim());
                chat.getMessage().setText("");
            }
        });
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

    public void registration(boolean success) {
        if (success) {
            logInMenu.send(registrationMenu.getUsernameField().getText(), registrationMenu.getPasswordField().getText());
        } else {
            registrationMenu.setError("User with this name already exists");
        }
    }

    public void logIn(boolean success) {
        if (success){
            chatMenu = new ChatMenu(controller.getChatNames());
            chatMenu.setListCellFactory(getChatNamesCellFactory());
            chatMenu.setCreateChatAction(getCreateChatEvent(chatMenu.getChatNames()));

            friendMenu = new FriendMenu(controller.getFriends(), controller.getFRequestsForUser(),
                    controller.getFRequestsFromUser());
            friendMenu.setFriendsCellFactory(getFriendCellFactory());
            friendMenu.setFRForUserCellFactory(getFRForUserCellFactory());
            friendMenu.setFRFromUserCellFactory(getFRFromUserCellFactory());
            friendMenu.setSendRequestAction(e1 -> {
                controller.sendFriendRequest(friendMenu.getUsernameField().getText());
                friendMenu.getUsernameField().setText("");
            });
            cTabPane.addTab("Chats", chatMenu);
            cTabPane.addTab("Friends", friendMenu);
            scene.setRoot(cTabPane);
            primaryStage.setTitle(primaryStage.getTitle() + "(" + logInMenu.getUsernameField().getText() + ")");
        }else {
            logInMenu.setError("Invalid name and/or password");
        }
    }

    public void showError(String message){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setContentText(message);
        alert.show();
    }
}
