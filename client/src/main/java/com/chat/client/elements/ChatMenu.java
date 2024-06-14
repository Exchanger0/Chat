package com.chat.client.elements;

import com.chat.client.model.AbstractChat;
import com.chat.client.model.Chat;
import com.chat.shared.ChatData;
import com.chat.shared.ChatType;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatMenu extends BorderPane {
    private final ListView<ChatData> chatNames;
    private final Button createChat;
    public ChatMenu(List<ChatData> chatList) {
        chatNames = new ListView<>();
        chatNames.setItems(FXCollections.observableList(new ArrayList<>(chatList)));
        chatNames.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        //создает новый чат
        createChat = new Button("+");
        createChat.setFont(new Font(15));
        createChat.setShape(new Circle(1));
        BorderPane.setAlignment(createChat, Pos.CENTER_RIGHT);
        BorderPane.setMargin(createChat, new Insets(5));

        setTop(createChat);
        setCenter(chatNames);
    }

    public void setListCellFactory(Callback<ListView<ChatData>, ListCell<ChatData>> factory){
        chatNames.setCellFactory(factory);
    }


    public void setCreateChatAction(EventHandler<ActionEvent> action){
        createChat.setOnAction(action);
    }

    public ListView<ChatData> getChatNames() {
        return chatNames;
    }

    public void addChat(ChatData data){
        chatNames.getItems().add(data);
    }

    public void deleteChat(int id){
        chatNames.getItems().removeIf(data -> data.id() == id);
    }
}
