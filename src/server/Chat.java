package server;

import java.util.ArrayList;

public class Chat {
    private final String name;
    private final ArrayList<User> members = new ArrayList<>();
    private final ArrayList<String> messages = new ArrayList<>();

    public Chat(String name) {
        this.name = name;
    }

    public void sendMessage(String message){
        synchronized (messages){
            messages.add(message);
        }
    }

    public ArrayList<String> getMessages(){
        return messages;
    }

    public String getName(){
        return name;
    }
}
