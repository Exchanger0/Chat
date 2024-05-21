package com.chat.client.elements;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.ArrayList;

public class CTabPane extends VBox {
//    private final VBox main = new VBox();
    private final GridPane header = new GridPane();
    private final GridPane content = new GridPane();
    private final ArrayList<Node> nodes = new ArrayList<>();

    private Button currentButton;
    private int indexOfCurrentContent = 0;

    private static final ColumnConstraints COLUMN_CONSTRAINTS = new ColumnConstraints(-1,-1,-1,Priority.ALWAYS, HPos.CENTER, true);

    private final EventHandler<ActionEvent> buttonHandler = actionEvent -> {
        indexOfCurrentContent = (Integer) ((Button) actionEvent.getSource()).getUserData();
        content.getChildren().set(0, nodes.get(indexOfCurrentContent));
        setCurrentButton((Button) actionEvent.getSource());
    };

    public CTabPane() {
        header.setBorder(new Border(new BorderStroke(null, null, Color.GREY, null,
                null, null, BorderStrokeStyle.SOLID, null,
                null, new BorderWidths(1), new Insets(0))));
        content.getColumnConstraints().add(COLUMN_CONSTRAINTS);
        RowConstraints rowConstraints = new RowConstraints(-1,-1,-1, Priority.ALWAYS, VPos.BASELINE, true);
        content.getRowConstraints().add(rowConstraints);
        getChildren().addAll(header, content);
    }

    public void addTab(String title, Node content){
        Button titleButton = new Button(title);
        titleButton.setUserData(nodes.size());
        titleButton.setBackground(null);
        titleButton.setMaxWidth(Double.MAX_VALUE);
        titleButton.setOnAction(buttonHandler);

        header.getColumnConstraints().add(COLUMN_CONSTRAINTS);
        header.add(titleButton, header.getColumnCount()-1, 0);

        nodes.add(content);

        if (this.content.getChildren().isEmpty()){
            setCurrentButton(titleButton);
            this.content.add(content,0,0);
        }
    }

    public void setContent(int index, Node newContent){
        if (index > nodes.size()) throw new IllegalArgumentException(index + " < amount of tabs");
        nodes.set(index, newContent);
        if (index == indexOfCurrentContent){
            content.getChildren().set(0, nodes.get(indexOfCurrentContent));
        }

    }

    private void setCurrentButton(Button button){
        if (currentButton != null) {
            currentButton.setBackground(null);
        }
        currentButton = button;
        currentButton.setBackground(new Background(new BackgroundFill(Color.rgb(199, 199, 199), null, null)));
    }

}