package client.elements;

import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import main.Chat;
import main.User;

public class ChatUI extends BorderPane {
    private final Button backButton;
    private final Button sendButton;
    private final TextField message;
    private final TextArea messages;
    public ChatUI(Chat chat, Pane mainRoot) {
        messages = new TextArea();
        messages.setEditable(false);
        messages.setWrapText(true);
        setCenter(messages);

        GridPane topPane = new GridPane();
        ColumnConstraints con1 = new ColumnConstraints();
        con1.setHgrow(Priority.SOMETIMES);
        con1.setFillWidth(true);
        topPane.getColumnConstraints().addAll(con1, con1);
        topPane.setPadding(new Insets(10));

        backButton = new Button("<-");
        topPane.add(backButton, 0,0);

        Label chatNameLabel = new Label(chat.getName());
        chatNameLabel.setFont(new Font(15));
        chatNameLabel.setOnMouseClicked(getMembers(mainRoot, chat));
        topPane.add(chatNameLabel, 1,0);
        setTop(topPane);

        HBox sendBox = new HBox();
        sendBox.setPadding(new Insets(10));
        sendBox.setSpacing(5);
        sendBox.setAlignment(Pos.CENTER);

        message = new TextField();
        message.setPrefColumnCount(messages.getPrefColumnCount());

        sendButton = new Button("Send");
        sendBox.getChildren().addAll(message, sendButton);
        setBottom(sendBox);
    }

    private EventHandler<MouseEvent> getMembers(Pane mainRoot, Chat chat) {
        return e -> {
            if (mainRoot.getChildren().size() < 2) {
                VBox box = new VBox();

                Label membersLabel = new Label("Members");
                membersLabel.setFont(new Font(15));

                Button closeButton = new Button("✕");
                closeButton.setFont(new Font(10));
                closeButton.setOnAction(actionEvent -> mainRoot.getChildren().removeLast());

                HBox topBox = new HBox(membersLabel, closeButton);
                topBox.setSpacing(20);
                VBox.setMargin(topBox, new Insets(12, 10, 12, 10));

                ListView<String> membersName = new ListView<>(FXCollections.observableList(chat.getMembers()
                        .stream().map(User::getUsername).toList()));
                membersName.setSelectionModel(null);
                membersName.setFocusModel(null);

                box.getChildren().addAll(topBox, membersName);
                box.setMinSize(100, 200);
                mainRoot.getChildren().add(box);
            }
        };
    }

    public void setSendButtonAction(EventHandler<ActionEvent> action){
        sendButton.setOnAction(action);
    }

    public void setBackButtonAction(EventHandler<ActionEvent> action){
        backButton.setOnAction(action);
    }

    public TextField getMessage() {
        return message;
    }

    public void addMessage(String message){
        messages.appendText(message + "\n");
    }
}
