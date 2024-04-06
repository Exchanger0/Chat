package client.elements;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class RegLogMenu extends GridPane {

    private final Button sendButton;
    private final Button backButton;
    private final TextField usernameField;
    private final TextField passwordField;
    public RegLogMenu() {
        setAlignment(Pos.CENTER);
        setHgap(30);
        setVgap(5);

        Label usernameLabel = new Label("Username:");
        usernameLabel.setFont(new Font(20));
        Label errorUsernameLabel = new Label(" ");
        errorUsernameLabel.setMaxHeight(0);
        errorUsernameLabel.setTextFill(Color.RED);
        GridPane.setColumnSpan(errorUsernameLabel, 2);
        usernameField = new TextField();
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
        passwordField = new PasswordField();
        passwordField.setTextFormatter(new TextFormatter<>(TextFormatter.IDENTITY_STRING_CONVERTER, "",
                change -> {
                    if (change.getControlNewText().matches("[a-zA-z0-9]{8,}")) {
                        errorPasswordLabel.setText("");
                        return change;
                    }
                    errorPasswordLabel.setText("Password must contain 8 characters");
                    return change;
                }));

        sendButton = new Button("Send");
        //пока в usernameField или passwordField введены неверные данные кнопка неактивна
        sendButton.disableProperty().bind(errorUsernameLabel.textProperty().isEmpty().not()
                .or(errorPasswordLabel.textProperty().isEmpty().not()));

        backButton = new Button("Back");

        ButtonBar buttonBar = new ButtonBar();
        buttonBar.getButtons().addAll(sendButton, backButton);
        GridPane.setColumnSpan(buttonBar, 2);
        GridPane.setHalignment(buttonBar, HPos.CENTER);

        add(usernameLabel, 0, 0);
        add(usernameField, 1, 0);
        add(errorUsernameLabel, 0, 1);
        add(passwordLabel, 0, 2);
        add(passwordField, 1, 2);
        add(errorPasswordLabel, 0, 3);
        add(buttonBar, 0, 4);
    }

    public void setSendButtonAction(EventHandler<ActionEvent> action){
        sendButton.setOnAction(action);
    }

    public void setBackButtonAction(EventHandler<ActionEvent> action){
        backButton.setOnAction(action);
    }

    public TextField getUsernameField(){
        return usernameField;
    }

    public TextField getPasswordField(){
        return passwordField;
    }
}
