package client;

import client.elements.*;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;
import main.Chat;
import main.User;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

//todo: исправить баги с множеством одного человека в друзьях и запросах
public class UIClient extends Application {

    private Stage primaryStage;
    private Scene scene;
    private Socket socket;

    private final MainMenu mainMenu = new MainMenu();
    private RegLogMenu registrationMenu;
    private RegLogMenu logInMenu;
    private ChatMenu chatMenu;
    private ChatUI chat;
    private FriendMenu friendMenu;
    private final CTabPane cTabPane = new CTabPane();

    private Controller controller;
    private Thread listenerThread;

    public static void main(String[] args) {
        System.out.println("start");
        launch(args);
    }

    @Override
    public void init() throws Exception {
        super.init();
        socket = new Socket("localhost", 8099);

        this.controller = new Controller(this, socket);

        listenerThread = new Thread(controller);
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        System.out.println("Start");
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
        System.out.println("Show");
    }

    private void initMainMenuButtonActions() {

        mainMenu.setRegButtonAction(e -> {
            if (registrationMenu == null) {
                registrationMenu = new RegLogMenu();
                initRegLogEvent(true, registrationMenu);
            }
            scene.setRoot(registrationMenu);
        });
        mainMenu.setLogInButtonAction(e -> {
            if (logInMenu == null) {
                logInMenu = new RegLogMenu();
                initRegLogEvent(false, logInMenu);
            }
            scene.setRoot(logInMenu);
        });
    }

    private void initRegLogEvent(boolean isRegistration, RegLogMenu regLogMenu){
        regLogMenu.setSendButtonAction(e -> {
            if (isRegistration) {
                System.out.println("Start reg");
                controller.registration(regLogMenu.getUsernameField().getText(),
                        regLogMenu.getPasswordField().getText());
                System.out.println("End reg");
            }
            boolean result = controller.login(regLogMenu.getUsernameField().getText(),
                    regLogMenu.getPasswordField().getText());

            if (result){
                chatMenu = new ChatMenu(FXCollections.observableList(controller.getChats()));
                chatMenu.setListCellFactory(getChatNamesCellFactory());
                chatMenu.setCreateChatAction(getCreateChatEvent(chatMenu.getChats()));

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
                primaryStage.setTitle(primaryStage.getTitle() + "(" + regLogMenu.getUsernameField().getText() + "");
            }
        });

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

    private EventHandler<ActionEvent> getCreateChatEvent(ListView<Chat> chatNames) {
        return e -> {
            Dialog<Boolean> dialog = new Dialog<>();
            dialog.setTitle("Create chat");
            DialogPane dialogPane = dialog.getDialogPane();
            VBox box = new VBox();
            box.setSpacing(10);
            box.setPadding(new Insets(10));
            box.setAlignment(Pos.CENTER);

            Label title = new Label("Title of chat: ");
            TextField titleField = new TextField();

            HBox titleBox = new HBox(title, titleField);
            titleBox.setAlignment(Pos.CENTER);
            titleBox.setSpacing(5);

            ScrollPane usernames = new ScrollPane();
            usernames.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
            usernames.setFitToWidth(true);
            usernames.setMinViewportHeight(100);
            VBox content = new VBox();
            content.setSpacing(10);
            content.setPadding(new Insets(15));
            usernames.setContent(content);

            Button addUserButton = new Button("Add User");
            addUserButton.setOnAction(event -> ((VBox)usernames.getContent()).getChildren().add(new TextField()));

            Button okButton = new Button("OK");
            okButton.disableProperty().bind(titleField.textProperty().isEmpty());
            okButton.setOnAction(e2 -> {
                if (chatNames.getItems().stream().map(Chat::getName)
                        .anyMatch(name -> titleField.getText().trim().equals(name))){
                    dialog.setResult(true);
                    dialog.close();
                    getChatExistsAlert().show();
                    return;
                }

                ObservableList<Node> nodes = ((VBox) usernames.getContent()).getChildren();
                List<String> users = new ArrayList<>();
                for (int i = 0; i < nodes.size(); i++) {
                    users.add(((TextField)nodes.get(i)).getText().trim());
                }
                controller.createChat(titleField.getText().trim(),users);
                dialog.setResult(true);
                dialog.close();
            });

            Button cancleButton = new Button("Cancel");
            cancleButton.setOnAction(e2 -> {
                dialog.setResult(true);
                dialog.close();
            });

            HBox buttonBox = new HBox(okButton, cancleButton);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);
            buttonBox.setSpacing(5);

            box.getChildren().addAll(titleBox,usernames, addUserButton, buttonBox);
            dialog.setWidth(300);
            dialog.setHeight(300);
            dialog.setResizable(false);
            dialogPane.setContent(box);
            dialog.initOwner(primaryStage);
            dialog.show();
        };
    }

    //настраивает отображение элементов ListCell<String> и добавляет слушателя событий
    //для каждого элемента списка
    private Callback<ListView<Chat>, ListCell<Chat>> getChatNamesCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<Chat> call(ListView<Chat> chatListView) {
                ListCell<Chat> listCell = new ListCell<>() {
                    @Override
                    protected void updateItem(Chat chat, boolean empty) {
                        super.updateItem(chat, empty);
                        if (chat == null && empty) {
                            setText("");
                        } else {
                            setText(chat.getName());
                        }
                    }
                };

                listCell.setFont(new Font(20));
                listCell.setOnMouseClicked(e -> {
                    int index = listCell.getIndex();
                    if (index >= 0 && index < chatListView.getItems().size()) {
                        System.out.println("Click from " + chatListView.getItems().get(index));

                        controller.setCurrentChat(chatListView.getItems().get(index));

                        scene.setRoot(createChat());

                        TextArea textArea = (TextArea) chat.getCenter();
                        System.out.println(controller.getMessages());
                        for (String s : controller.getMessages()){
                            textArea.appendText(s + "\n");
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

    public HBox createChat() {
        HBox root = new HBox();
        chat = new ChatUI(controller.getCurrentChat(), root);
        chat.setBackButtonAction(e1 -> scene.setRoot(cTabPane));
        chat.setSendButtonAction(e1 -> {
            if (!chat.getMessage().getText().trim().isEmpty()) {
                controller.sendMessage(chat.getMessage().getText().trim());
                chat.getMessage().setText("");
            }
        });
        HBox.setHgrow(chat, Priority.SOMETIMES);
        root.getChildren().add(chat);
        return root;
    }

    public void addMessage(String message) {
        if (chat != null) {
            chat.addMessage(message);
        }
    }

    public void addChat(Chat chat){
        chatMenu.addChat(chat);
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
