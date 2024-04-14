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
    private final ListView<Group> groups;
    private final Button createChat;
    public ChatMenu(ObservableList<Group> groupList) {
        groups = new ListView<>();
        groups.setItems(groupList);
        groups.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        //создает новый чат
        createChat = new Button("+");
        createChat.setFont(new Font(15));
        createChat.setShape(new Circle(1));
        BorderPane.setAlignment(createChat, Pos.CENTER_RIGHT);
        BorderPane.setMargin(createChat, new Insets(5));

        setTop(createChat);
        setCenter(groups);
    }

    public void setListCellFactory(Callback<ListView<Group>, ListCell<Group>> factory){
        groups.setCellFactory(factory);
    }


    public void setCreateChatAction(EventHandler<ActionEvent> action){
        createChat.setOnAction(action);
    }

    public ListView<Group> getGroups() {
        return groups;
    }

    public void addGroup(Group group){
        groups.getItems().add(group);
    }
}
