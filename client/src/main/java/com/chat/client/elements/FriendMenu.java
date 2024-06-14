package com.chat.client.elements;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.List;

public class FriendMenu extends VBox {

    private final ListView<String> friends;
    private final ListView<String> frForUser;
    private final ListView<String> frFromUser;
    private final TextField username;
    private final Button sendRequest;
    public FriendMenu(List<String> friendsList, List<String> frForUserList, List<String> frFromUserList) {
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

    public void setFriendsCellFactory(Callback<ListView<String>, ListCell<String>> factory){
        friends.setCellFactory(factory);
    }

    public void setFRForUserCellFactory(Callback<ListView<String>, ListCell<String>> factory){
        frForUser.setCellFactory(factory);
    }

    public void setFRFromUserCellFactory(Callback<ListView<String>, ListCell<String>> factory){
        frFromUser.setCellFactory(factory);
    }

    public void setSendRequestAction(EventHandler<ActionEvent> action){
        sendRequest.setOnAction(action);
    }

    public void deleteFriend(String friend){
        friends.getItems().remove(friend);
    }

    public void addFriend(String friend){
            friends.getItems().add(friend);
    }

    public void removeFRForUser(String user){
        frForUser.getItems().remove(user);
    }

    public void removeFRFromUser(String user){
        frFromUser.getItems().remove(user);
    }

    public TextField getUsernameField(){
        return username;
    }

    public void addFRForUser(String user){
        frForUser.getItems().add(user);
    }

    public void addFRFromUser(String user){
        frFromUser.getItems().add(user);
    }

}
