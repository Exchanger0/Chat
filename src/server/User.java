package server;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class User {
    private final String username;
    private final String password;
    private final ArrayList<Chat> chats = new ArrayList<>();

    public User(String username, String password) {
        if (username.equals("admin")){
            chats.add(new Chat("work"));
            chats.add(new Chat("friends"));
            chats.add(new Chat("Mother"));
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

    public List<String> getChatNames(){
        return chats.stream().map(Chat::getName).collect(Collectors.toList());
    }

    public void addChat(Chat chat){
        chats.add(chat);
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
