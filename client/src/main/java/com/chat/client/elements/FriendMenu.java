package com.chat.client.elements;

import com.chat.client.Client;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import java.util.ArrayList;

public class FriendMenu extends VBox {

    private final ListView<String> friends;
    private final ListView<String> frForUser;
    private final ListView<String> frFromUser;
    private final TextField username;
    private final Client client;

    public FriendMenu(Client client) {
        this.client = client;
        setPadding(new Insets(10));

        friends = new ListView<>(FXCollections.observableList(new ArrayList<>(client.controller.getFriends())));
        friends.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        friends.setCellFactory(getFriendCellFactory());

        frForUser = new ListView<>(FXCollections.observableList(new ArrayList<>(client.controller.getFRequestsForUser())));
        frForUser.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        frForUser.setCellFactory(getFRForUserCellFactory());

        frFromUser = new ListView<>(FXCollections.observableList(new ArrayList<>(client.controller.getFRequestsFromUser())));
        frFromUser.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        frFromUser.setCellFactory(getFRFromUserCellFactory());

        username = new TextField();

        Button sendRequest = new Button("Send friend request");
        sendRequest.disableProperty().bind(username.textProperty().isEmpty());
        sendRequest.setOnAction(e1 -> {
            client.controller.sendFriendRequest(username.getText());
            username.setText("");
        });

        HBox hBox = new HBox(username, sendRequest);
        hBox.setSpacing(10);
        hBox.setPadding(new Insets(0,0,10,0));

        getChildren().addAll(hBox, new TitledPane("Friends", friends),
                new TitledPane("Friend requests for you", frForUser),
                new TitledPane("Friend requests from you", frFromUser));
    }

    //настраивает отображение списка друзей
    private Callback<ListView<String>, ListCell<String>> getFriendCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> userListView) {
                ListCell<String> listCell = new ListCell<>() {
                    @Override
                    protected void updateItem(String username, boolean empty) {
                        super.updateItem(username, empty);
                        if (username == null && empty) {
                            setText("");
                        } else {
                            setText(username);
                        }
                    }
                };

                MenuItem delete = new MenuItem("Delete");
                delete.setOnAction(e -> client.controller.deleteFriend(listCell.getItem()));
                listCell.setContextMenu(new ContextMenu(delete));
                return listCell;
            }
        };
    }

    //настраивает отображение списка запросов на дружбу для текущего пользователя
    private Callback<ListView<String>, ListCell<String>> getFRForUserCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> userListView) {
                ListCell<String> listCell = new ListCell<>() {
                    @Override
                    protected void updateItem(String username, boolean empty) {
                        super.updateItem(username, empty);
                        if (username == null && empty) {
                            setText("");
                        } else {
                            setText(username);
                        }
                    }
                };

                MenuItem disagree = new MenuItem("Disagree");
                disagree.setOnAction(e -> client.controller.removeFRForUser(listCell.getItem()));

                MenuItem agree = new MenuItem("Agree");
                agree.setOnAction(e -> {
                    String username = listCell.getItem();
                    if (username != null){
                        client.controller.addFriend(username);
                    }
                });
                listCell.setContextMenu(new ContextMenu(disagree, agree));
                return listCell;
            }
        };
    }

    //настраивает отображение списка запросов на дружбу от текущего пользователя
    private Callback<ListView<String>, ListCell<String>> getFRFromUserCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> userListView) {
                ListCell<String> listCell = new ListCell<>() {
                    @Override
                    protected void updateItem(String username, boolean empty) {
                        super.updateItem(username, empty);
                        if (username == null && empty) {
                            setText("");
                        } else {
                            setText(username);
                        }
                    }
                };

                MenuItem cancellation = new MenuItem("Cancellation");
                cancellation.setOnAction(e -> client.controller.removeFRFromUser(listCell.getItem()));

                listCell.setContextMenu(new ContextMenu(cancellation));
                return listCell;
            }
        };
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

    public void addFRForUser(String user){
        frForUser.getItems().add(user);
    }

    public void addFRFromUser(String user){
        frFromUser.getItems().add(user);
    }

    public void refresh() {
        friends.refresh();
        frForUser.refresh();
        frFromUser.refresh();
    }

}
