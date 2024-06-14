package com.chat.server.model;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "\"user\"")
public class User implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private int id;
    @Column(name = "username")
    private String username;
    @Column(name = "password")
    private String password;
    @ManyToMany(mappedBy = "members")
    private List<AbstractChat> chats = new ArrayList<>();
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "friend",
            joinColumns = @JoinColumn(name = "user1_id"),
            foreignKey = @ForeignKey(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "user2_id"),
            inverseForeignKey = @ForeignKey(name = "user_id")
    )
    private List<User> friends = new ArrayList<>();
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "friend_request_for_user",
            joinColumns = @JoinColumn(name = "user_id"),
            foreignKey = @ForeignKey(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "from_user_id"),
            inverseForeignKey = @ForeignKey(name = "user_id")
    )
    private List<User> fRequestsForUser = new ArrayList<>();
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "friend_request_from_user",
            joinColumns = @JoinColumn(name = "user_id"),
            foreignKey = @ForeignKey(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "to_user_id"),
            inverseForeignKey = @ForeignKey(name = "user_id")
    )
    private List<User> fRequestsFromUser = new ArrayList<>();

    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public List<AbstractChat> getChats() {
        return chats;
    }

    public AbstractChat getChat(int id){
        return chats.stream()
                .filter(ch -> ch.getId() == id)
                .findFirst().orElse(null);
    }

    public void addChat(AbstractChat chat) {
        chats.add(chat);
    }

    public void deleteChat(AbstractChat chat){
        chats.remove(chat);
    }

    public boolean deleteFriend(User username){
        return friends.remove(username);
    }

    public List<User> getFriends() {
        return friends;
    }

    public void addFRequestForUser(User user) {
        fRequestsForUser.add(user);
    }

    public boolean canAddFRequestForUser(User user) {
        return !friends.contains(user) && !fRequestsForUser.contains(user) && !fRequestsFromUser.contains(user);
    }

    public void deleteFRequestForUser(User user){
        fRequestsForUser.remove(user);
    }

    public List<User> getFRequestsForUser() {
        return fRequestsForUser;
    }

    public void addFRequestFromUser(User user) {
        fRequestsFromUser.add(user);
    }

    public boolean canAddFRequestFromUser(User user) {
        return !friends.contains(user) && !fRequestsFromUser.contains(user) && !fRequestsForUser.contains(user);
    }

    public void deleteFRequestFromUser(User user){
        fRequestsFromUser.remove(user);
    }

    public List<User> getFRequestsFromUser() {
        return fRequestsFromUser;
    }

    public void addFriend(User user) {
        friends.add(user);
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && Objects.equals(username, user.username) && Objects.equals(password, user.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }

}
