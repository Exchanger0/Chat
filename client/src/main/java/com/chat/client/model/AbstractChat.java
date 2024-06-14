package com.chat.client.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class AbstractChat implements Serializable {
    protected int id;
    protected String name;
    protected List<String> members = new ArrayList<>();
    protected List<String> messages = new ArrayList<>();

    public AbstractChat() {
    }

    public AbstractChat(Integer id, String name) {
        this.name = name;
        this.id = id;
    }

    public void sendMessage(String message){
        messages.add(message);
    }

    public List<String> getMessages(){
        return this.messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }

    public String getName(){
        return name;
    }

    public abstract void addMember(String user);


    public List<String> getMembers(){
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }

    public void deleteMember(String user){
        members.remove(user);
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractChat that = (AbstractChat) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

}
