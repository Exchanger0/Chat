package client;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.Callback;
import main.Chat;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CyclicBarrier;

public class UIClient extends Application {

    private Scene scene;
    private Socket socket;

    private VBox mainMenu;
    private GridPane registrationMenu;
    private GridPane logInMenu;
    private BorderPane chatsMenu;
    private BorderPane chat;

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
        System.out.println("Start");
        initMainMenu();

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

    //создает главное меню с кнопкой для регистрации и входа
    private void initMainMenu() {
        mainMenu = new VBox();
        mainMenu.setAlignment(Pos.CENTER);
        mainMenu.setSpacing(15);

        Label title = new Label("Chat");
        title.setFont(new Font(50));

        Button registrationButton = new Button("Registration");
        registrationButton.setPrefWidth(100);
        registrationButton.setOnAction(e -> {
            if (registrationMenu == null) {
                registrationMenu = createRegLogMenu(true);
            }
            scene.setRoot(registrationMenu);
        });
        Button logInButton = new Button("Log in");
        logInButton.setOnAction(e -> {
            if (logInMenu == null) {
                logInMenu = createRegLogMenu(false);
            }
            scene.setRoot(logInMenu);
        });
        logInButton.setPrefWidth(100);

        mainMenu.getChildren().addAll(title, registrationButton, logInButton);
    }

    //создает меню входа или регистрации
    private GridPane createRegLogMenu(boolean isRegistration) {
        GridPane gridPane = new GridPane();
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setHgap(30);
        gridPane.setVgap(5);

        Label usernameLabel = new Label("Username:");
        usernameLabel.setFont(new Font(20));
        Label errorUsernameLabel = new Label(" ");
        errorUsernameLabel.setMaxHeight(0);
        errorUsernameLabel.setTextFill(Color.RED);
        GridPane.setColumnSpan(errorUsernameLabel, 2);
        TextField usernameField = new TextField();
        usernameField.setTextFormatter(new TextFormatter<>(TextFormatter.IDENTITY_STRING_CONVERTER, "",
                change -> {
                    if (change.getControlNewText().matches("[a-zA-z0-9_]{4,}")) {
                        errorUsernameLabel.setText("");
                        return change;
                    }
                    errorUsernameLabel.setText("Username short");
                    return change;
                }));

        Label passwordLabel = new Label("Password:");
        passwordLabel.setFont(new Font(20));
        Label errorPasswordLabel = new Label(" ");
        errorPasswordLabel.setMaxHeight(0);
        errorPasswordLabel.setTextFill(Color.RED);
        GridPane.setColumnSpan(errorPasswordLabel, 2);
        PasswordField passwordField = new PasswordField();
        passwordField.setTextFormatter(new TextFormatter<>(TextFormatter.IDENTITY_STRING_CONVERTER, "",
                change -> {
                    if (change.getControlNewText().matches("[a-zA-z0-9]{8,}")) {
                        errorPasswordLabel.setText("");
                        return change;
                    }
                    errorPasswordLabel.setText("Password must contain 8 characters");
                    return change;
                }));

        Button sendButton = new Button("Send");
        //пока в usernameField или passwordField введены неверные данные кнопка неактивна
        sendButton.disableProperty().bind(errorUsernameLabel.textProperty().isEmpty().not()
                .or(errorPasswordLabel.textProperty().isEmpty().not()));

        sendButton.setOnAction(e -> {
            if (isRegistration) {
                System.out.println("Start reg");
                controller.registration(usernameField.getText(), passwordField.getText());
                System.out.println("End reg");
            }
            boolean result = controller.login(usernameField.getText(), passwordField.getText());

            if (result){
                initChatsMenu();
                scene.setRoot(chatsMenu);
            }
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            usernameField.setText("");
            passwordField.setText("");
            scene.setRoot(mainMenu);
        });

        ButtonBar buttonBar = new ButtonBar();
        buttonBar.getButtons().addAll(sendButton, backButton);
        GridPane.setColumnSpan(buttonBar, 2);
        GridPane.setHalignment(buttonBar, HPos.CENTER);

        gridPane.add(usernameLabel, 0, 0);
        gridPane.add(usernameField, 1, 0);
        gridPane.add(errorUsernameLabel, 0, 1);
        gridPane.add(passwordLabel, 0, 2);
        gridPane.add(passwordField, 1, 2);
        gridPane.add(errorPasswordLabel, 0, 3);
        gridPane.add(buttonBar, 0, 4);

        return gridPane;
    }

    //создает меню со списком чатов пользователя
    private void initChatsMenu() {
        if (chatsMenu == null){
            chatsMenu = new BorderPane();
        }else {
            return;
        }

        ListView<Chat> chats = new ListView<>();
        chats.setCellFactory(getChatNamesCellFactory());
        chats.setItems(FXCollections.observableList(controller.getChats()));
        chats.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        //создает новый чат
        Button createChat = new Button("+");
        createChat.setFont(new Font(15));
        createChat.setShape(new Circle(1));
        createChat.setOnAction(getCreateChatEvent(chats));
        BorderPane.setAlignment(createChat, Pos.CENTER_RIGHT);
        BorderPane.setMargin(createChat, new Insets(5));

        chatsMenu.setTop(createChat);
        chatsMenu.setCenter(chats);
    }

    private BorderPane createChat(String chatName, Pane mainRoot) {
        BorderPane root = new BorderPane();

        TextArea messages = new TextArea();
        messages.setEditable(false);
        messages.setWrapText(true);
        root.setCenter(messages);

        GridPane topPane = new GridPane();
        ColumnConstraints con1 = new ColumnConstraints();
        con1.setHgrow(Priority.SOMETIMES);
        con1.setFillWidth(true);
        topPane.getColumnConstraints().addAll(con1, con1);
        topPane.setPadding(new Insets(10));

        Button back = new Button("<-");
        back.setOnAction(e -> scene.setRoot(chatsMenu));
        topPane.add(back, 0,0);

        Label chatNameLabel = new Label(chatName);
        chatNameLabel.setFont(new Font(15));
        chatNameLabel.setOnMouseClicked(e -> {
            if (mainRoot.getChildren().size() < 2) {
                VBox box = new VBox();

                Label membersLabel = new Label("Members");
                membersLabel.setFont(new Font(15));

                Button closeButton = new Button("✕");
                closeButton.setFont(new Font(10));
                closeButton.setOnAction(actionEvent -> mainRoot.getChildren().removeLast());

                HBox topBox = new HBox(membersLabel, closeButton);
                topBox.setSpacing(20);
                VBox.setMargin(topBox, new Insets(12, 10,12,10));

                ListView<String> membersName = new ListView<>((ObservableList<String>) this.chat.getUserData());
                membersName.setSelectionModel(null);
                membersName.setFocusModel(null);

                box.getChildren().addAll(topBox,membersName);
                box.setMinSize(100, 200);
                mainRoot.getChildren().add(box);
            }
        });
        topPane.add(chatNameLabel, 1,0);

        root.setTop(topPane);

        HBox sendBox = new HBox();
        sendBox.setPadding(new Insets(10));
        sendBox.setSpacing(5);
        sendBox.setAlignment(Pos.CENTER);

        TextField message = new TextField();
        message.setPrefColumnCount(messages.getPrefColumnCount());

        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> {
            if (!message.getText().trim().isEmpty()) {
                System.out.println("Send message");
                controller.sendMessage(message.getText().trim());
                message.setText("");
            }
        });

        sendBox.getChildren().addAll(message, sendButton);
        root.setBottom(sendBox);

        return root;
    }

    //возвращает окно с надписью о том что такой чат уже существует
    private Stage getChatExistsStage() {
        Stage errorStage = new Stage();
        Label error = new Label("Chat already exists");
        error.setFont(new Font(25));
        error.setTextFill(Color.RED);
        error.setStyle("-fx-font-weight: bold");
        errorStage.setScene(new Scene(new StackPane(error)));
        errorStage.setWidth(300);
        errorStage.setHeight(200);
        return errorStage;
    }

    //возвращает слушатель событий для кнопки "+" (создать чат)
    private EventHandler<ActionEvent> getCreateChatEvent(ListView<Chat> chatNames) {
        return e -> {
            Stage stage = new Stage();

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
                    getChatExistsStage().show();
                    stage.close();
                    return;
                }

                ObservableList<Node> nodes = ((VBox) usernames.getContent()).getChildren();
                List<String> users = new ArrayList<>();
                for (int i = 0; i < nodes.size(); i++) {
                    users.add(((TextField)nodes.get(i)).getText().trim());
                }
                controller.createChat(titleField.getText().trim(),users);
                stage.close();
            });

            box.getChildren().addAll(titleBox,usernames, addUserButton, okButton);
            stage.setWidth(300);
            stage.setHeight(300);
            stage.setResizable(false);
            stage.setAlwaysOnTop(true);
            stage.setScene(new Scene(box));
            stage.centerOnScreen();
            stage.show();
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

                        HBox root = new HBox();
                        chat = createChat(chatListView.getItems().get(index).getName(), root);
                        HBox.setHgrow(chat, Priority.SOMETIMES);
                        root.getChildren().add(chat);
                        System.out.println(root.getChildren());
                        scene.setRoot(root);

                        TextArea textArea = (TextArea) chat.getCenter();
                        System.out.println(controller.getMessages());
                        for (String s : controller.getMessages()){
                            textArea.appendText(s + "\n");
                        }
                        chat.setUserData(FXCollections.observableList(controller.getMembersName()));
                    }
                });

                return listCell;
            }
        };
    }


    //добавляет сообщение из коллекции в текущий чат
    public void addMessage(String message) {
        if (chat != null) {
            TextArea textArea = (TextArea) chat.getCenter();
            textArea.appendText(message + "\n");
        }
    }

    public void addChat(Chat chat){
        ((ListView<Chat>)chatsMenu.getCenter()).getItems().add(chat);
    }
}
