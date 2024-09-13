package com.chat.client.elements;

import com.chat.client.Client;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class LogMenu extends RegLogMenu{
    public LogMenu(Client client) {
        super(client, "Log In");
    }

    @Override
    protected EventHandler<ActionEvent> getSendButtonAction() {
        return e -> client.controller.login(usernameField.getText(), passwordField.getText());
    }
}
