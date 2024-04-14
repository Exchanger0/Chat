package main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Group implements Serializable {
    protected final String name;
    protected final ArrayList<User> members = new ArrayList<>();
    protected final List<String> messages = new ArrayList<>();

    public Group(String name) {
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
        Group chat = (Group) o;
        return Objects.equals(name, chat.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
