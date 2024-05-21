package com.chat.client.elements;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
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
    private final Label errorLabel = new Label();
    private final Label errorUsernameLabel = new Label(" ");
    private final Label errorPasswordLabel = new Label(" ");
    public RegLogMenu(String title) {
        setAlignment(Pos.CENTER);
        setHgap(30);
        setVgap(5);

        SimpleBooleanProperty u = new SimpleBooleanProperty(false);
        SimpleBooleanProperty p = new SimpleBooleanProperty(false);

        Label titleLabel = new Label(title);
        titleLabel.setFont(new Font(50));

        Label usernameLabel = new Label("Username:");
        usernameLabel.setFont(new Font(20));
        errorUsernameLabel.setMaxHeight(0);
        errorUsernameLabel.setTextFill(Color.RED);
        GridPane.setColumnSpan(errorUsernameLabel, 2);
        usernameField = new TextField();
        usernameField.textProperty().addListener((observable, oldString, newString) -> {
            if (newString.length() < 4){
                u.set(false);
                errorUsernameLabel.setText("Username must contain 4 characters");
            }else {
                u.set(true);
                errorUsernameLabel.setText("");
            }
        });

        Label passwordLabel = new Label("Password:");
        passwordLabel.setFont(new Font(20));
        errorPasswordLabel.setTextFill(Color.RED);
        GridPane.setColumnSpan(errorPasswordLabel, 2);
        passwordField = new PasswordField();
        passwordField.textProperty().addListener((observable, oldString, newString) -> {
            if (newString.length() < 8){
                p.set(false);
                errorPasswordLabel.setText("Password must contain 8 characters");
            }else {
                p.set(true);
                errorPasswordLabel.setText("");
            }
        });

        errorLabel.setTextFill(Color.RED);
        sendButton = new Button("Send");
        //пока в usernameField или passwordField введены неверные данные кнопка неактивна
        sendButton.disableProperty().bind(u.not().or(p.not()));

        backButton = new Button("Back");

        ButtonBar buttonBar = new ButtonBar();
        buttonBar.getButtons().addAll(sendButton, backButton);
        GridPane.setColumnSpan(buttonBar, 2);
        GridPane.setHalignment(buttonBar, HPos.CENTER);

        GridPane.setColumnSpan(titleLabel, 2);
        GridPane.setHalignment(titleLabel, HPos.CENTER);
        GridPane.setMargin(titleLabel, new Insets(0,0,20,0));
        add(titleLabel, 0, 0);
        add(usernameLabel, 0, 1);
        add(usernameField, 1, 1);
        add(errorUsernameLabel, 0, 2);
        add(passwordLabel, 0, 3);
        add(passwordField, 1, 3);
        add(errorPasswordLabel, 0, 4);
        add(buttonBar, 0, 5);
        GridPane.setColumnSpan(errorLabel, 2);
        GridPane.setHalignment(errorLabel, HPos.CENTER);
        GridPane.setMargin(errorLabel, new Insets(5));
        add(errorLabel, 0, 6);
    }

    public void setSendButtonAction(EventHandler<ActionEvent> action){
        sendButton.setOnAction(action);
    }

    public void send(String username, String passsword){
        usernameField.setText(username);
        passwordField.setText(passsword);
        sendButton.fire();
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

    public void setError(String string){
        errorLabel.setText(string);
    }

    public void cleanErrorLabels(){
        errorPasswordLabel.setText("");
        errorUsernameLabel.setText("");
        errorLabel.setText("");
    }
}
