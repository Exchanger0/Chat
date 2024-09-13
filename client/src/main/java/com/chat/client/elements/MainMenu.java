package com.chat.client.elements;

import com.chat.client.Client;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class MainMenu extends VBox {

    public MainMenu(Client client) {
        setAlignment(Pos.CENTER);
        setSpacing(15);

        Label title = new Label("Chat");
        title.setFont(new Font(50));

        Button registrationButton = new Button("Registration");
        registrationButton.setPrefWidth(100);
        registrationButton.setOnAction(e -> {
            client.getRegistrationMenu().cleanErrorLabels();
            client.setRoot(client.getRegistrationMenu());
        });

        Button logInButton = new Button("Log in");
        logInButton.setPrefWidth(100);
        logInButton.setOnAction(e -> {
            client.getLogInMenu().cleanErrorLabels();
            client.setRoot(client.getLogInMenu());
        });

        getChildren().addAll(title, registrationButton, logInButton);
    }
}
