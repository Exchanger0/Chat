package com.chat.client.elements;

import com.chat.client.Client;
import com.chat.client.model.AbstractChat;
import com.chat.shared.ChatData;
import com.chat.shared.ChatType;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.util.Callback;

import java.util.ArrayList;
import java.util.List;

public class ChatMenu extends BorderPane {
    private final ListView<ChatData> chatNames;
    private final Client client;

    public ChatMenu(Client client, List<ChatData> chatList) {
        this.client = client;

        chatNames = new ListView<>();
        chatNames.setItems(FXCollections.observableList(new ArrayList<>(chatList)));
        chatNames.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        chatNames.setCellFactory(getChatNamesCellFactory());

        //создает новый чат
        Button createChat = new Button("+");
        createChat.setFont(new Font(15));
        createChat.setShape(new Circle(1));
        createChat.setOnAction(getCreateChatEvent(chatNames));
        BorderPane.setAlignment(createChat, Pos.CENTER_RIGHT);
        BorderPane.setMargin(createChat, new Insets(5));

        setTop(createChat);
        setCenter(chatNames);
    }

    public void addChat(ChatData data){
        chatNames.getItems().add(data);
    }

    public void deleteChat(int id){
        chatNames.getItems().removeIf(data -> data.id() == id);
    }

    private Callback<ListView<ChatData>, ListCell<ChatData>> getChatNamesCellFactory() {
        return new Callback<>() {
            @Override
            public ListCell<ChatData> call(ListView<ChatData> chatListView) {
                ListCell<ChatData> listCell = new ListCell<>() {
                    @Override
                    protected void updateItem(ChatData data, boolean empty) {
                        super.updateItem(data, empty);
                        if (data == null && empty) {
                            setText("");
                        } else {
                            setText(data.publicName());
                        }
                    }
                };

                listCell.setFont(new Font(20));
                listCell.setOnMouseClicked(e -> {
                    if (e.getButton().equals(MouseButton.PRIMARY)) {
                        int index = listCell.getIndex();
                        if (index >= 0 && index < chatListView.getItems().size()) {
                            ChatData data = chatListView.getItems().get(index);
                            AbstractChat chat = client.controller.load(data.id());

                            ChatUI chatUI = new ChatUI(client, data, chat);
                            client.setChat(chatUI);
                            client.setRoot(chatUI);
                        }
                    }
                });

                MenuItem menuItem = new MenuItem("Delete");
                menuItem.setOnAction(e -> {
                    ChatData data = listCell.getItem();
                    if (data.type() == ChatType.GROUP) {
                        client.controller.deleteGroup(data.id(), data.privateName());
                    } else {
                        client.controller.deleteChat(data.id(), data.privateName());
                    }
                });
                listCell.setContextMenu(new ContextMenu(menuItem));

                return listCell;
            }
        };
    }

    private EventHandler<ActionEvent> getCreateChatEvent(ListView<ChatData> chatNames) {
        return e -> {
            Dialog<Boolean> dialog = new Dialog<>();
            dialog.setTitle("Create chat");
            DialogPane dialogPane = dialog.getDialogPane();

            VBox root = new VBox();
            root.setSpacing(10);
            root.setAlignment(Pos.CENTER);

            ToggleGroup toggleGroup = new ToggleGroup();
            RadioButton groupButton = new RadioButton("Group");
            groupButton.setUserData("group");
            groupButton.setSelected(true);
            groupButton.setToggleGroup(toggleGroup);
            RadioButton chatButton = new RadioButton("Chat");
            chatButton.setUserData("chat");
            chatButton.setToggleGroup(toggleGroup);
            VBox buttonBox = new VBox(groupButton, chatButton);
            buttonBox.setSpacing(5);

            CreateGroupMenu createGroupMenu = new CreateGroupMenu(client.controller.getFriends());
            createGroupMenu.setOkButtonAction(e1 -> {
                if (chatNames.getItems()
                        .stream()
                        .anyMatch(data -> createGroupMenu.getChatName().equals(data.privateName()))){
                    dialog.setResult(true);
                    dialog.close();
                    getChatExistsAlert().show();
                    return;
                }
                client.controller.createGroup(createGroupMenu.getChatName(), createGroupMenu.getSelectedUsers());
                dialog.setResult(true);
                dialog.close();
            });
            createGroupMenu.setCancelButtonAction(e1 -> {
                dialog.setResult(true);
                dialog.close();
            });

            CreateChatMenu createChatMenu = new CreateChatMenu(client.controller.getFriends());
            createChatMenu.setOkButtonAction(e1 -> {
                if (chatNames.getItems()
                        .stream()
                        .anyMatch(data -> createChatMenu.getSelectedUser().equals(data.publicName()))) {
                    dialog.setResult(true);
                    dialog.close();
                    getChatExistsAlert().show();
                    return;
                }
                client.controller.createChat(createChatMenu.getSelectedUser());
                dialog.setResult(true);
                dialog.close();
            });
            createChatMenu.setCancelButtonAction(e1 -> {
                dialog.setResult(true);
                dialog.close();
            });

            toggleGroup.selectedToggleProperty().addListener((observer, oldVal, newVal) -> {
                if (newVal.getUserData().equals("group")) {
                    root.getChildren().set(1, createGroupMenu);
                } else if (newVal.getUserData().equals("chat")) {
                    root.getChildren().set(1, createChatMenu);
                }
            });

            root.getChildren().addAll(buttonBox, createGroupMenu);

            dialog.setWidth(300);
            dialog.setHeight(300);
            dialog.setResizable(false);
            dialogPane.setContent(root);
            dialog.initOwner(client.getPrimaryStage());
            dialog.show();
        };
    }

    private Alert getChatExistsAlert() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Chat already exist");
        return alert;
    }
}
