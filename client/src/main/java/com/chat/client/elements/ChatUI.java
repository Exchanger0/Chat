package com.chat.client.elements;

import com.chat.client.Client;
import com.chat.client.model.AbstractChat;
import com.chat.shared.ChatData;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.util.List;


public class ChatUI extends HBox {
    private final ChatData data;
    private final VBox members = new VBox();
    private final TextField message;
    private final VBox messages;

    public ChatUI(Client client, ChatData data, AbstractChat chat) {
        this.data = data;
        messages = new VBox();
        messages.setAlignment(Pos.TOP_LEFT);
        messages.setSpacing(10);
        messages.setPadding(new Insets(15));
        messages.setFillWidth(true);
        messages.maxWidthProperty().bind(Stage.getWindows().getFirst().widthProperty().subtract(60));

        ScrollPane msgScrollPane = new ScrollPane();
        msgScrollPane.setContent(messages);
        msgScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        msgScrollPane.setBorder(new Border(
                new BorderStroke(Color.rgb(99, 108, 118), BorderStrokeStyle.SOLID,
                        null, new BorderWidths(1))));

        messages.heightProperty().addListener((ob, oldVal, newVal) -> msgScrollPane.setVvalue(1D));

        BorderPane chatPane = new BorderPane();
        chatPane.setCenter(msgScrollPane);

        GridPane topPane = new GridPane();
        ColumnConstraints con1 = new ColumnConstraints();
        con1.setHgrow(Priority.SOMETIMES);
        con1.setFillWidth(true);
        topPane.getColumnConstraints().addAll(con1, con1);
        topPane.setPadding(new Insets(10));

        Button backButton = new Button("\u2190");
        topPane.add(backButton, 0,0);
        backButton.setOnAction(e1 -> client.setRoot(client.getTabMenu()));

        Label chatNameLabel = new Label(data.publicName());
        chatNameLabel.setFont(new Font(15));
        chatNameLabel.setOnMouseClicked(getMembers(chat.getMembers()));
        topPane.add(chatNameLabel, 1,0);
        chatPane.setTop(topPane);

        GridPane sendPane = new GridPane();
        sendPane.setPadding(new Insets(10));
        ColumnConstraints c = new ColumnConstraints();
        c.setFillWidth(true);
        c.setHgrow(Priority.ALWAYS);
        sendPane.getColumnConstraints().add(c);

        Button sendButton = new Button("\u2A65");

        message = new TextField();
        message.setMaxWidth(Double.MAX_VALUE);
        message.setOnKeyReleased(e -> {
            if (e.getCode().getCode() == KeyCode.ENTER.getCode()){
                sendButton.fire();
            }
        });

        sendButton.setOnAction(e1 -> {
            if (!message.getText().trim().isEmpty()) {
                client.controller.sendMessage(data.id(), data.privateName(), message.getText().trim());
                message.setText("");
            }
        });

        GridPane.setMargin(message, new Insets(0,10,0,0));
        sendPane.add(message, 0 ,0);
        sendPane.add(sendButton, 1 ,0);
        chatPane.setBottom(sendPane);
        setHgrow(chatPane, Priority.ALWAYS);
        getChildren().add(chatPane);

        for (String s : chat.getMessages()) {
            addMessage(s);
        }
    }

    private EventHandler<MouseEvent> getMembers(List<String> membersList) {
        return e -> {
            if (getChildren().size() < 2) {
                if (members.getChildren().isEmpty()) {
                    Label membersLabel = new Label("Members");
                    membersLabel.setFont(new Font(15));

                    Button closeButton = new Button("\u274c");
                    closeButton.setFont(new Font(10));
                    closeButton.setOnAction(actionEvent -> getChildren().removeLast());

                    HBox topBox = new HBox(membersLabel, closeButton);
                    topBox.setSpacing(20);
                    VBox.setMargin(topBox, new Insets(12, 10, 12, 10));

                    ListView<String> membersName = new ListView<>(FXCollections.observableList(membersList));
                    membersName.setSelectionModel(null);
                    membersName.setFocusModel(null);

                    members.getChildren().addAll(topBox, membersName);
                    members.setMinSize(100, 200);
                }
                getChildren().add(members);
            }
        };
    }

    public void addMessage(String message){
        Label text = new Label(message);
        text.setWrapText(true);
        text.setPadding(new Insets(5));
        text.setBackground(new Background(
                new BackgroundFill(Color.rgb(211, 211, 211), new CornerRadii(10), null)));
        text.setWrapText(true);
        messages.getChildren().add(text);
    }

    public ChatData getData() {
        return data;
    }
}
