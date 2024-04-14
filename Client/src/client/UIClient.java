package client;

import client.elements.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;
import main.Chat;
import main.Group;
import main.User;

import java.io.IOException;
import java.net.Socket;

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

    private Controller controller;
    private Thread listenerThread;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        super.init();
        socket = new Socket("192.168.100.7", 8099);

        this.controller = new Controller(this, socket);

        listenerThread = new Thread(controller);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
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
                throw new RuntimeException(ex);
            }

        });
        stage.setScene(scene);
        stage.setTitle("Chat");
        stage.setHeight(600);
        stage.setWidth(600);
        stage.centerOnScreen();
        stage.show();
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
        registrationMenu.setSendButtonAction(e -> {
            boolean res = controller.registration(registrationMenu.getUsernameField().getText(),
                    registrationMenu.getPasswordField().getText());
            if (res){
                logInMenu.send(registrationMenu.getUsernameField().getText(), registrationMenu.getPasswordField().getText());
            }else {
                registrationMenu.setError("User with this name already exists");
            }
        });
        initBackButtonEvent(registrationMenu);
    }

    private void initLogInEvent(){
        logInMenu.setSendButtonAction(e -> {
            boolean result = controller.login(logInMenu.getUsernameField().getText(),
                    logInMenu.getPasswordField().getText());

            if (result){
                chatMenu = new ChatMenu(FXCollections.observableList(controller.getChats()));
                chatMenu.setListCellFactory(getGroupNamesCellFactory());
                chatMenu.setCreateChatAction(getCreateChatEvent(chatMenu.getGroups()));

                friendMenu = new FriendMenu(controller.getFriends(), controller.getFRequestsForUser(), controller.getFRequestsFromUser());
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
        });
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

    private EventHandler<ActionEvent> getCreateChatEvent(ListView<Group> chatNames) {
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
                if (chatNames.getItems().stream().map(Group::getName)
                        .anyMatch(name -> createGroupMenu.getChatName().equals(name))){
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
                if (chatNames.getItems().stream().map(group -> {
                            String name = group.getName();
                            if (group instanceof Chat ch) name = ch.getPseudonym(controller.getCurrentUser());
                            return name;
                        })
                        .anyMatch(name -> createChatMenu.getSelectedUser().equals(name))) {
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
    private Callback<ListView<Group>, ListCell<Group>> getGroupNamesCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<Group> call(ListView<Group> chatListView) {
                ListCell<Group> listCell = new ListCell<>() {
                    @Override
                    protected void updateItem(Group group, boolean empty) {
                        super.updateItem(group, empty);
                        if (group == null && empty) {
                            setText("");
                        } else {
                            if (group instanceof Chat ch){
                                setText(ch.getPseudonym(controller.getCurrentUser()));
                            }else {
                                setText(group.getName());
                            }
                        }
                    }
                };

                listCell.setFont(new Font(20));
                listCell.setOnMouseClicked(e -> {
                    int index = listCell.getIndex();
                    if (index >= 0 && index < chatListView.getItems().size()) {
                        controller.setCurrentChat(chatListView.getItems().get(index));

                        Group group = controller.getCurrentChat();
                        String chatName = group.getName();
                        if (group instanceof Chat ch){
                            chatName = ch.getPseudonym(controller.getCurrentUser());
                        }
                        chat = new ChatUI(chatName, group.getMembers());
                        initChatActions();
                        scene.setRoot(chat);

                        for (String s : controller.getMessages()){
                            chat.addMessage(s);
                        }
                    }
                });

                return listCell;
            }
        };
    }

    private Callback<ListView<User>, ListCell<User>> getFriendCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<User> call(ListView<User> userListView) {
                ListCell<User> listCell = new ListCell<>() {
                    @Override
                    protected void updateItem(User user, boolean empty) {
                        super.updateItem(user, empty);
                        if (user == null && empty) {
                            setText("");
                        } else {
                            setText(user.getUsername());
                        }
                    }
                };

                MenuItem delete = new MenuItem("Delete");
                delete.setOnAction(e -> {
                    controller.deleteFriend(listCell.getItem());
                });
                listCell.setContextMenu(new ContextMenu(delete));
                return listCell;
            }
        };
    }

    private Callback<ListView<User>, ListCell<User>> getFRForUserCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<User> call(ListView<User> userListView) {
                ListCell<User> listCell = new ListCell<>() {
                    @Override
                    protected void updateItem(User user, boolean empty) {
                        super.updateItem(user, empty);
                        if (user == null && empty) {
                            setText("");
                        } else {
                            setText(user.getUsername());
                        }
                    }
                };

                MenuItem disagree = new MenuItem("Disagree");
                disagree.setOnAction(e -> {
                    controller.removeFRForUser(listCell.getItem());
                });

                MenuItem agree = new MenuItem("Agree");
                agree.setOnAction(e -> {
                    controller.addFriend(listCell.getItem());
                });
                listCell.setContextMenu(new ContextMenu(disagree, agree));
                return listCell;
            }
        };
    }

    private Callback<ListView<User>, ListCell<User>> getFRFromUserCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<User> call(ListView<User> userListView) {
                ListCell<User> listCell = new ListCell<>() {
                    @Override
                    protected void updateItem(User user, boolean empty) {
                        super.updateItem(user, empty);
                        if (user == null && empty) {
                            setText("");
                        } else {
                            setText(user.getUsername());
                        }
                    }
                };

                MenuItem cancellation = new MenuItem("Cancellation");
                cancellation.setOnAction(e -> {
                    controller.removeFRFromUser(listCell.getItem());
                });
                
                listCell.setContextMenu(new ContextMenu(cancellation));
                return listCell;
            }
        };
    }

    public void initChatActions() {
        chat.setBackButtonAction(e1 -> scene.setRoot(cTabPane));
        chat.setSendButtonAction(e1 -> {
            if (!chat.getMessage().getText().trim().isEmpty()) {
                controller.sendMessage(chat.getMessage().getText().trim());
                chat.getMessage().setText("");
            }
        });
    }

    public void addMessage(String message) {
        if (chat != null) {
            chat.addMessage(message);
        }
    }

    public void addGroup(Group group){
        chatMenu.addGroup(group);
    }

    public void deleteFriend(User friend){
        friendMenu.deleteFriend(friend);
    }

    public void addFriend(User friend){
        friendMenu.addFriend(friend);
    }

    public void removeFRForUser(User user){
        friendMenu.removeFRForUser(user);
    }

    public void removeFRFromUser(User user){
        friendMenu.removeFRFromUser(user);
    }

    public void addFRForUser(User user){
        friendMenu.addFRForUser(user);
    }

    public void addFRFromUser(User user){
        friendMenu.addFRFromUser(user);
    }
}
