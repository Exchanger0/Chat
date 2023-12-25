package client;

import javafx.scene.control.TextArea;

import java.util.ArrayList;
import java.util.List;

//текстовое поле с возможностью сохранять каждую новую
//строку в коллекцию
public class ListTextArea extends TextArea {
    private final List<String> lines = new ArrayList<>();

    @Override
    public void appendText(String s) {
        super.appendText(s);
        lines.add(s.trim());
    }

    public List<String> getLines() {
        return lines;
    }
}
