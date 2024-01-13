package client;

import javafx.application.Application;
import javafx.application.Platform;
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
import main.RequestResponse;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class UIClient extends Application {

    private Scene scene;
    private Socket socket;
    private PrintWriter writer;
    private ObjectInputStream objectInputStream;

    private VBox mainMenu;
    private GridPane registrationMenu;
    private GridPane logInMenu;
    private BorderPane chatsMenu;
    private BorderPane chat;

    private String response;
    private final CyclicBarrier forListener = new CyclicBarrier(2);
    private Thread listenerThread;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        super.init();
        socket = new Socket("localhost", 8099);
        writer = new PrintWriter(socket.getOutputStream());
        objectInputStream = new ObjectInputStream(socket.getInputStream());
        listenerThread = new Thread(new Listener());
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    @Override
    public void start(Stage stage) {
        initMainMenu();

        scene = new Scene(mainMenu);
        stage.setOnCloseRequest(e -> {
            try {
                writer.println(RequestResponse.EXIT.name());
                writer.flush();
                writer.close();
                objectInputStream.close();
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

        RequestResponse request;
        if (isRegistration) {
            request = RequestResponse.REGISTRATION;
        } else {
            request = RequestResponse.LOG_IN;
        }
        sendButton.setOnAction(e -> {
            writer.println(request.name());
            writer.println(usernameField.getText());
            writer.println(passwordField.getText());
            writer.flush();
            try {
                forListener.await();
            } catch (InterruptedException | BrokenBarrierException ex) {
                throw new RuntimeException(ex);
            }
            if (response.equals(RequestResponse.SUCCESSFUL_REGISTRATION.name())) {
                if (logInMenu == null) {
                    logInMenu = createRegLogMenu(false);
                }
                //имитация входа в аккаунт
                ((TextField) logInMenu.getChildren().get(1)).setText(usernameField.getText());
                ((TextField) logInMenu.getChildren().get(4)).setText(passwordField.getText());
                ((Button) ((ButtonBar) logInMenu.getChildren().get(6)).getButtons().getFirst()).fire();
            } else if (response.equals(RequestResponse.SUCCESSFUL_LOGIN.name())) {
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

        ListView<String> chatNames = new ListView<>();
        chatNames.setCellFactory(getChatNamesCellFactory());
        writer.println(RequestResponse.GET_CHAT_NAMES.name());
        writer.flush();
        chatNames.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        //создает новый чат
        Button createChat = new Button("+");
        createChat.setFont(new Font(15));
        createChat.setShape(new Circle(1));
        createChat.setOnAction(getCreateChatEvent(chatNames));
        BorderPane.setAlignment(createChat, Pos.CENTER_RIGHT);
        BorderPane.setMargin(createChat, new Insets(5));

        chatsMenu.setTop(createChat);
        chatsMenu.setCenter(chatNames);
    }

    private BorderPane createChat(String chatName, Pane mainRoot) {
        BorderPane root = new BorderPane();

        TextArea messages = new ListTextArea();
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
                writer.println(RequestResponse.SEND_MESSAGE.name());
                writer.println(message.getText().trim());
                writer.flush();
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
    private EventHandler<ActionEvent> getCreateChatEvent(ListView<String> chatNames) {
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
                if (chatNames.getItems().contains(titleField.getText().trim())) {
                    getChatExistsStage().show();
                    stage.close();
                    return;
                }
                writer.println(RequestResponse.CREATE_CHAT.name());
                writer.println(titleField.getText().trim());
                StringBuilder names = new StringBuilder();
                ObservableList<Node> nodes = ((VBox) usernames.getContent()).getChildren();
                for (int i = 0; i < nodes.size(); i++) {
                    names.append(((TextField)nodes.get(i)).getText().trim());
                    if (i != nodes.size()-1){
                        names.append("$");
                    }
                }
                writer.println(names);
                writer.flush();
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
    private Callback<ListView<String>, ListCell<String>> getChatNamesCellFactory() {
        return stringListView -> {
            ListCell<String> listCell = new ListCell<>() {
                @Override
                protected void updateItem(String s, boolean empty) {
                    super.updateItem(s, empty);
                    if (s == null && empty) {
                        setText("");
                    } else {
                        setText(s);
                    }
                }
            };
            listCell.setFont(new Font(20));
            listCell.setOnMouseClicked(e -> {
                int index = listCell.getIndex();
                if (index >= 0 && index < stringListView.getItems().size()) {
                    System.out.println("Click from " + stringListView.getItems().get(index));
                    writer.println(RequestResponse.SET_CURRENT_CHAT.name());
                    writer.println(stringListView.getItems().get(index));
                    writer.flush();

                    HBox root = new HBox();
                    chat = createChat(stringListView.getItems().get(index), root);
                    HBox.setHgrow(chat, Priority.SOMETIMES);
                    root.getChildren().add(chat);
                    System.out.println(root.getChildren());
                    scene.setRoot(root);

                    writer.println(RequestResponse.GET_CHAT_MESSAGE.name());
                    writer.println(RequestResponse.GET_MEMBERS_NAME.name());
                    writer.flush();
                }
            });
            return listCell;
        };
    }

    //добавляет сообщение из коллекции в текущий чат
    private void addMessages(List<String> messages) {
        if (chat != null) {
            ListTextArea textArea = (ListTextArea) chat.getCenter();
            List<String> lines = textArea.getLines();
            messages.subList(lines.size(), messages.size()).forEach(s -> textArea.appendText(s + "\n"));
        }
    }

    //слушатель ответов от сервера
    private class Listener implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String serverResponse = objectInputStream.readUTF();

                    if (serverResponse.equals(RequestResponse.UPDATE_CHATS.name()) || serverResponse.equals(RequestResponse.GET_CHAT_NAMES.name())) {
                        ObservableList<String> list = FXCollections.observableList((List<String>) objectInputStream.readObject());
                        Platform.runLater(() -> ((ListView<String>) chatsMenu.getCenter()).setItems(list));
                    } else if (serverResponse.equals(RequestResponse.UPDATE_MESSAGES.name())) {
                        ObservableList<String> list = FXCollections.observableList((List<String>) objectInputStream.readObject());
                        Platform.runLater(() -> addMessages(list));
                    } else if (serverResponse.equals(RequestResponse.MEMBERS_NAME.name())) {
                        chat.setUserData(FXCollections.observableList(((List<String>) objectInputStream.readObject())));
                    } else {
                        response = serverResponse;
                        forListener.await();
                    }
                } catch (SocketException socketException) {
                    break;
                } catch (IOException | BrokenBarrierException | InterruptedException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
