package client.elements;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import main.User;

import java.util.ArrayList;
import java.util.List;

public class CreateGroupMenu extends VBox {
    private final List<String> selectedUsers = new ArrayList<>();
    private final Button okButton;
    private final Button cancelButton;
    private final TextField titleField;

    public CreateGroupMenu(List<User> friendsList) {
        setSpacing(10);
        setAlignment(Pos.CENTER);

        Label title = new Label("Title of chat: ");
        titleField = new TextField();

        HBox titleBox = new HBox(title, titleField);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setSpacing(5);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setFitToWidth(true);
        scrollPane.setMaxHeight(100);
        scrollPane.setPadding(new Insets(5));

        VBox friends = new VBox();
        friends.setPadding(new Insets(5));
        friends.setSpacing(5);

        for (User user : friendsList){
            CheckBox checkBox = new CheckBox(user.getUsername());
            checkBox.selectedProperty().addListener((observer, oldVal, newVal) -> {
                if (newVal){
                    selectedUsers.add(checkBox.getText());
                }else {
                    selectedUsers.remove(checkBox.getText());
                }
            });
            friends.getChildren().add(checkBox);
        }
        scrollPane.setContent(friends);

        okButton = new Button("OK");
        okButton.disableProperty().bind(titleField.textProperty().isEmpty());
        cancelButton = new Button("Cancel");

        HBox buttonBox = new HBox(okButton, cancelButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.setSpacing(5);

        getChildren().addAll(titleBox, scrollPane, buttonBox);
    }

    public void setOkButtonAction(EventHandler<ActionEvent> action){
        okButton.setOnAction(action);
    }

    public void setCancelButtonAction(EventHandler<ActionEvent> action){
        cancelButton.setOnAction(action);
    }

    public List<String> getSelectedUsers() {
        return selectedUsers;
    }

    public String getChatName(){
        return titleField.getText().trim();
    }
}
