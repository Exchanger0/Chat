package com.chat.client.elements;

import com.chat.client.Client;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

public class RegMenu extends RegLogMenu{
    public RegMenu(Client client) {
        super(client, "Registration");
    }

    @Override
    protected EventHandler<ActionEvent> getSendButtonAction() {
        return e -> client.controller.registration(usernameField.getText(), passwordField.getText());
    }
}
