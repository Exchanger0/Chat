package client;

import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import main.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class UIClient extends Application {

    private Scene scene;
    private PrintWriter writer;
    private BufferedReader reader;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void init() throws Exception {
        super.init();
        Socket socket = new Socket("localhost", 8099);
        writer = new PrintWriter(socket.getOutputStream());
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent mainMenu = initMainMenu();

        scene = new Scene(mainMenu);
        stage.setScene(scene);
        stage.setTitle("Chat");
        stage.setHeight(600);
        stage.setWidth(600);
        stage.centerOnScreen();
        stage.show();
    }

    private Parent initMainMenu(){
        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setSpacing(15);

        Label title = new Label("Chat");
        title.setFont(new Font(50));

        Button registrationButton = new Button("Registration");
        registrationButton.setPrefWidth(100);
        registrationButton.setOnAction(e -> scene.setRoot(initRegLogNode(true)));
        Button logInButton = new Button("Log in");
        logInButton.setOnAction(e -> scene.setRoot(initRegLogNode(false)));
        logInButton.setPrefWidth(100);

        root.getChildren().addAll(title,registrationButton, logInButton);

        return root;
    }

    private Parent initRegLogNode(boolean isRegistration){
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
                    if (change.getControlNewText().matches("[a-zA-z0-9_]{4,}")){
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
                    if (change.getControlNewText().matches("[a-zA-z0-9]{8,}")){
                        errorPasswordLabel.setText("");
                        return change;
                    }
                    errorPasswordLabel.setText("Password must contain 8 characters");
                    return change;
                }));

        Button sendButton = new Button("Send");
        sendButton.disableProperty().bind(errorUsernameLabel.textProperty().isEmpty().not()
                        .or(errorPasswordLabel.textProperty().isEmpty().not()));
        Request request;
        if (isRegistration){
            request = Request.REGISTRATION;
        }else {
            request = Request.LOG_IN;
        }
        sendButton.setOnAction(e -> {
            writer.println(request.name());
            writer.println(usernameField.getText());
            writer.println(passwordField.getText());
            writer.flush();
            try {
                System.out.println(reader.readLine());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });

        Button backButton = new Button("Back");
        backButton.setOnAction(e -> {
            scene.setRoot(initMainMenu());
        });

        ButtonBar buttonBar = new ButtonBar();
        buttonBar.getButtons().addAll(sendButton, backButton);
        GridPane.setColumnSpan(buttonBar,2);
        GridPane.setHalignment(buttonBar, HPos.CENTER);

        gridPane.add(usernameLabel, 0,0);
        gridPane.add(usernameField, 1,0);
        gridPane.add(errorUsernameLabel, 0,1);
        gridPane.add(passwordLabel, 0,2);
        gridPane.add(passwordField, 1,2);
        gridPane.add(errorPasswordLabel, 0, 3);
        gridPane.add(buttonBar, 0,4);

        return gridPane;
    }
}
