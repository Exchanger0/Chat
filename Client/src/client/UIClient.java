package client;

import client.elements.ChatMenu;
import client.elements.ChatUI;
import client.elements.MainMenu;
import client.elements.RegLogMenu;
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

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class UIClient extends Application {

    private Stage primaryStage;
    private Scene scene;
    private Socket socket;

    private final MainMenu mainMenu = new MainMenu();
    private RegLogMenu registrationMenu;
    private RegLogMenu logInMenu;
    private ChatMenu chatMenu;
    private ChatUI chat;

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
                scene.setRoot(chatMenu);
            }
        });

        regLogMenu.setBackButtonAction(e -> {
            System.out.println("start back");
            regLogMenu.getUsernameField().setText("");
            regLogMenu.getPasswordField().setText("");
            scene.setRoot(mainMenu);
            System.out.println("end back");
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

            box.getChildren().addAll(titleBox,usernames, addUserButton, okButton);
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

    public HBox createChat() {
        HBox root = new HBox();
        chat = new ChatUI(controller.getCurrentChat(), root);
        chat.setBackButtonAction(e1 -> scene.setRoot(chatMenu));
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

}
