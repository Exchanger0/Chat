package com.chat.client.elements;

import com.chat.shared.User;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

import java.util.List;

public class CreateChatMenu extends VBox {
    private String selectedUser = "";
    private final Button okButton;
    private final Button cancelButton;

    public CreateChatMenu(List<User> friendsList) {
        setSpacing(10);
        setPadding(new Insets(5));
        setAlignment(Pos.CENTER);

        Label label = new Label("Chat with: ");
        label.setFont(new Font(30));

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(100);
        scrollPane.setPadding(new Insets(5));

        VBox friends = new VBox();
        friends.setPadding(new Insets(5));
        friends.setSpacing(5);

        ToggleGroup toggleGroup = new ToggleGroup();
        toggleGroup.selectedToggleProperty().addListener((observer, oldVal, newVal) -> {
            selectedUser = ((RadioButton) newVal).getText();
        });
        for (User user : friendsList){
            RadioButton radioButton = new RadioButton(user.getUsername());
            radioButton.setToggleGroup(toggleGroup);
            friends.getChildren().add(radioButton);
        }
        scrollPane.setContent(friends);

        okButton = new Button("OK");
        okButton.disableProperty().bind(toggleGroup.selectedToggleProperty().isNull());
        cancelButton = new Button("Cancel");

        HBox buttonBox = new HBox(okButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setSpacing(5);

        getChildren().addAll(label, scrollPane, buttonBox);
    }

    public void setOkButtonAction(EventHandler<ActionEvent> action){
        okButton.setOnAction(action);
    }

    public void setCancelButtonAction(EventHandler<ActionEvent> action){
        cancelButton.setOnAction(action);
    }

    public String getSelectedUser() {
        return selectedUser;
    }
}
