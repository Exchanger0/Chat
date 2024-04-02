package main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class User implements Serializable {
    private final String username;
    private final String password;
    private final HashMap<String,Chat> chats = new HashMap<>();

    public User(String username, String password) {
        if (username.equals("admin")){
            chats.put("job", new Chat("job"));
            Chat job = chats.get("job");
            job.sendMessage("welcome");
            job.sendMessage("to");
            job.sendMessage("job");
        }
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public boolean equalsPassword(String password){
        return this.password.equals(password);
    }

    public List<Chat> getChats(){
        synchronized (chats) {
            return new ArrayList<>(chats.values());
        }
    }

    public Chat getChat(String name){
        return chats.get(name);
    }

    public synchronized void addChat(Chat chat){
        synchronized (chats) {
            chats.put(chat.getName(), chat);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username) && Objects.equals(password, user.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }
}
