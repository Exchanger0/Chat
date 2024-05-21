package client.elements;

import javafx.collections.ObservableList;
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
import main.Group;

public class ChatMenu extends BorderPane {
    private final ListView<Group> chats;
    private final Button createChat;
    public ChatMenu(ObservableList<Group> chatList) {
        chats = new ListView<>();
        chats.setItems(chatList);
        chats.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        //создает новый чат
        createChat = new Button("+");
        createChat.setFont(new Font(15));
        createChat.setShape(new Circle(1));
        BorderPane.setAlignment(createChat, Pos.CENTER_RIGHT);
        BorderPane.setMargin(createChat, new Insets(5));

        setTop(createChat);
        setCenter(chats);
    }

    public void setListCellFactory(Callback<ListView<Group>, ListCell<Group>> factory){
        chats.setCellFactory(factory);
    }


    public void setCreateChatAction(EventHandler<ActionEvent> action){
        createChat.setOnAction(action);
    }

    public ListView<Group> getChats() {
        return chats;
    }

    public void addChat(Group chat){
        chats.getItems().add(chat);
    }

    public void deleteChat(Group chat){
        chats.getItems().remove(chat);
    }
}
