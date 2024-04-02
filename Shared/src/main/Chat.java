package main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Chat implements Serializable {
    private final String name;
    private final ArrayList<User> members = new ArrayList<>();
    private final List<String> messages = new ArrayList<>();

    public Chat(String name) {
        this.name = name;
    }

    public void sendMessage(String message){
        synchronized (messages){
            messages.add(message);
        }
    }

    public List<String> getMessages(){
        synchronized (messages) {
            return this.messages;
        }
    }

    public String getName(){
        return name;
    }

    public void addMember(User user){
        synchronized (members) {
            members.add(user);
        }
    }

    public List<User> getMembers(){
        synchronized (members) {
            return members;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Chat chat = (Chat) o;
        return Objects.equals(name, chat.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
