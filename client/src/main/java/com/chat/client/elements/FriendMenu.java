package com.chat.client.elements;

import com.chat.shared.User;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.List;

public class FriendMenu extends VBox {

    private final ListView<User> friends;
    private final ListView<User> frForUser;
    private final ListView<User> frFromUser;
    private final TextField username;
    private final Button sendRequest;
    public FriendMenu(List<User> friendsList, List<User> frForUserList, List<User> frFromUserList) {
        setPadding(new Insets(10));

        friends = new ListView<>();
        friends.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        frForUser = new ListView<>();
        frForUser.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        frFromUser = new ListView<>();
        frFromUser.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        for (int i = 0; i < Math.max(friendsList.size(), Math.max(frForUserList.size(), frFromUserList.size())); i++) {
            if (i < friendsList.size()){
                friends.getItems().add(friendsList.get(i));
            }
            if (i < frForUserList.size()){
                frForUser.getItems().add(frForUserList.get(i));
            }
            if (i < frFromUserList.size()){
                frFromUser.getItems().add(frFromUserList.get(i));
            }
        }

        username = new TextField();

        sendRequest = new Button("Send friend request");
        sendRequest.disableProperty().bind(username.textProperty().isEmpty());

        HBox hBox = new HBox(username, sendRequest);
        hBox.setSpacing(10);
        hBox.setPadding(new Insets(0,0,10,0));

        getChildren().addAll(hBox, new TitledPane("Friends", friends),
                new TitledPane("Friend requests for you", frForUser),
                new TitledPane("Friend requests from you", frFromUser));
    }

    public void setFriendsCellFactory(Callback<ListView<User>, ListCell<User>> factory){
        friends.setCellFactory(factory);
    }

    public void setFRForUserCellFactory(Callback<ListView<User>, ListCell<User>> factory){
        frForUser.setCellFactory(factory);
    }

    public void setFRFromUserCellFactory(Callback<ListView<User>, ListCell<User>> factory){
        frFromUser.setCellFactory(factory);
    }

    public void setSendRequestAction(EventHandler<ActionEvent> action){
        sendRequest.setOnAction(action);
    }

    public void deleteFriend(User friend){
        friends.getItems().remove(friend);
    }

    public void addFriend(User friend){
            friends.getItems().add(friend);
    }

    public void removeFRForUser(User user){
        frForUser.getItems().remove(user);
    }

    public void removeFRFromUser(User user){
        frFromUser.getItems().remove(user);
    }

    public TextField getUsernameField(){
        return username;
    }

    public void addFRForUser(User user){
        frForUser.getItems().add(user);
    }

    public void addFRFromUser(User user){
        frFromUser.getItems().add(user);
    }

}
