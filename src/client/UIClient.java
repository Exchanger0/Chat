package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import main.RequestResponse;

import java.io.*;
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
        mainMenu = initMainMenu();

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
    private VBox initMainMenu() {
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setSpacing(15);

        Label title = new Label("Chat");
        title.setFont(new Font(50));

        Button registrationButton = new Button("Registration");
        registrationButton.setPrefWidth(100);
        registrationButton.setOnAction(e -> {
            if (registrationMenu == null) {
                registrationMenu = initRegLogMenu(true);
            }
            scene.setRoot(registrationMenu);
        });
        Button logInButton = new Button("Log in");
        logInButton.setOnAction(e -> {
            if (logInMenu == null) {
                logInMenu = initRegLogMenu(false);
            }
            scene.setRoot(logInMenu);
        });
        logInButton.setPrefWidth(100);

        root.getChildren().addAll(title, registrationButton, logInButton);

        return root;
    }

    //создает меню входа или регистрации
    private GridPane initRegLogMenu(boolean isRegistration) {
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
            if (response.equals(RequestResponse.SUCCESSFUL_REGISTRATION.name())){
                if (logInMenu == null){
                    logInMenu = initRegLogMenu(false);
                }
                //имитация входа в аккаунт
                ((TextField)logInMenu.getChildren().get(1)).setText(usernameField.getText());
                ((TextField)logInMenu.getChildren().get(4)).setText(passwordField.getText());
                ((Button)((ButtonBar)logInMenu.getChildren().get(6)).getButtons().getFirst()).fire();
            } else if (response.equals(RequestResponse.SUCCESSFUL_LOGIN.name())) {
                if (chatsMenu == null){
                    chatsMenu = initChatsMenu();
                }
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
    private BorderPane initChatsMenu() {
        BorderPane root = new BorderPane();

        ListView<String> chatNames = new ListView<>();
        writer.println(RequestResponse.GET_CHAT_NAMES.name());
        writer.flush();
        chatNames.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        //создает новый чат
        Button createChat = new Button("+");
        createChat.setFont(new Font(15));
        createChat.setShape(new Circle(1));
        createChat.setOnAction(e -> {
            Stage stage = new Stage();

            VBox box = new VBox();
            box.setSpacing(10);
            box.setPadding(new Insets(10));
            box.setAlignment(Pos.CENTER);

            Button okButton = new Button("OK");

            Label title = new Label("Title of chat: ");
            TextField titleField = new TextField();
            titleField.setTextFormatter(new TextFormatter<>(TextFormatter.IDENTITY_STRING_CONVERTER, "",
                    change -> {
                        if (change.getControlNewText().trim().isEmpty()){
                           okButton.setDisable(true);
                           return change;
                        }
                        okButton.setDisable(false);
                        return change;
                    }));
            HBox titleBox = new HBox(title, titleField);
            titleBox.setAlignment(Pos.CENTER);
            titleBox.setSpacing(5);

            okButton.setOnAction(e2 -> {
                writer.println(RequestResponse.UPDATE_CHAT.name());
                writer.println(titleField.getText());
                writer.flush();
                stage.close();
            });

            box.getChildren().addAll(titleBox, okButton);
            stage.setWidth(300);
            stage.setHeight(120);
            stage.setResizable(false);
            stage.setAlwaysOnTop(true);
            stage.setScene(new Scene(box));
            stage.centerOnScreen();
            stage.show();
        });
        BorderPane.setAlignment(createChat, Pos.CENTER_RIGHT);
        BorderPane.setMargin(createChat, new Insets(5));

        root.setTop(createChat);
        root.setCenter(chatNames);
        return root;
    }

    //слушатель ответов от сервера
    private class Listener implements Runnable{
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()){
                try {
                    String serverResponse = objectInputStream.readUTF();

                    if (serverResponse.equals(RequestResponse.UPDATE_CHAT.name()) || serverResponse.equals(RequestResponse.GET_CHAT_NAMES.name())){
                        Object obj = objectInputStream.readObject();
                        ObservableList<String> list = FXCollections.observableList((List<String>) obj);
                        Platform.runLater(() -> ((ListView<String>)chatsMenu.getCenter()).setItems(list));
                    } else {
                        response = serverResponse;
                        forListener.await();
                    }
                } catch (SocketException socketException){
                    break;
                } catch (IOException | BrokenBarrierException | InterruptedException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
