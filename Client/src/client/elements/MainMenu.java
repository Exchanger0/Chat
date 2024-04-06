package client.elements;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

public class MainMenu extends VBox {

    private final Button registrationButton;
    private final Button logInButton;
    public MainMenu() {
        setAlignment(Pos.CENTER);
        setSpacing(15);

        Label title = new Label("Chat");
        title.setFont(new Font(50));

        registrationButton = new Button("Registration");
        registrationButton.setPrefWidth(100);

        logInButton = new Button("Log in");
        logInButton.setPrefWidth(100);

        getChildren().addAll(title, registrationButton, logInButton);
    }

    public void setRegButtonAction(EventHandler<ActionEvent> action){
        registrationButton.setOnAction(action);
    }

    public void setLogInButtonAction(EventHandler<ActionEvent> action){
        logInButton.setOnAction(action);
    }
}
