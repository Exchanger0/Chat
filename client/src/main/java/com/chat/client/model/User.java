package com.chat.client.model;


import com.chat.shared.ChatData;
import com.chat.shared.ChatType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class User {
    private final String username;
    private List<AbstractChat> chats = new ArrayList<>();
    private List<ChatData> chatData = new ArrayList<>();
    private List<String> friends = new ArrayList<>();
    private List<String> fRequestsForUser = new ArrayList<>();
    private List<String> fRequestsFromUser = new ArrayList<>();

    public User(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public List<AbstractChat> getChats() {
        return chats;
    }

    public void setChats(List<AbstractChat> chats) {
        this.chats = chats;
    }

    public AbstractChat getChat(int id){
        return chats.stream()
                .filter(ch -> ch.getId() == id)
                .findFirst().orElse(null);
    }

    public void addChat(AbstractChat chat) {
        chats.add(chat);
        if (chatData
                .stream()
                .noneMatch(data -> chat.getName().equals(data.privateName()))) {
            ChatType type = ChatType.GROUP;
            String publicName = chat.getName();
            if (chat instanceof Chat ch) {
                type = ChatType.CHAT;
                publicName = ch.getPseudonym(username);
            }
            chatData.add(new ChatData(chat.getId(), type, publicName, chat.getName()));
        }
    }

    public void deleteChat(AbstractChat chat){
        chats.remove(chat);
        chatData.removeIf(data -> data.privateName().equals(chat.getName()) && data.id() == chat.getId());
    }

    public void deleteChat(int id) {
        chats.removeIf(chat -> chat.getId() == id);
        chatData.removeIf(data -> data.id() == id);
    }

    public void deleteFriend(String username){
        friends.remove(username);
    }

    public List<String> getFriends() {
        return friends;
    }

    public void setFriends(List<String> friends) {
        this.friends = friends;
    }

    public void addFRequestForUser(String user) {
        if (!friends.contains(user) && !fRequestsForUser.contains(user) && !fRequestsFromUser.contains(user)) {
            fRequestsForUser.add(user);
        }
    }

    public void deleteFRequestForUser(String user){
        fRequestsForUser.remove(user);
    }

    public List<String> getFRequestsForUser() {
        return fRequestsForUser;
    }

    public void setfRequestsForUser(List<String> fRequestsForUser) {
        this.fRequestsForUser = fRequestsForUser;
    }

    public void addFRequestFromUser(String user) {
        if (!friends.contains(user) && !fRequestsFromUser.contains(user) && !fRequestsForUser.contains(user)) {
            fRequestsFromUser.add(user);
        }
    }

    public void deleteFRequestFromUser(String user){
        fRequestsFromUser.remove(user);
    }

    public List<String> getFRequestsFromUser() {
        return fRequestsFromUser;
    }

    public void setfRequestsFromUser(List<String> fRequestsFromUser) {
        this.fRequestsFromUser = fRequestsFromUser;
    }

    public void addFriend(String user) {
        if (!friends.contains(user)) {
            friends.add(user);
        }
    }

    public List<ChatData> getChatData() {
        return chatData;
    }

    public void setChatData(List<ChatData> chatData) {
        this.chatData = chatData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
}
