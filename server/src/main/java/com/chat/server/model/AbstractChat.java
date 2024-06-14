package com.chat.server.model;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "chat")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type")
public abstract class AbstractChat implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_id")
    private int id;
    @Column(name = "name")
    protected String name;
    @ManyToMany
    @JoinTable(name = "user_chat",
            joinColumns = @JoinColumn(name = "chat_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    protected final List<User> members = new ArrayList<>();
    @Column(name = "messages", columnDefinition = "text[]")
    protected final List<String> messages = new ArrayList<>();

    public AbstractChat() {
    }

    public AbstractChat(String name) {
        this.name = name;
    }

    public void sendMessage(String message){
        messages.add(message);
    }

    public List<String> getMessages(){
        return this.messages;
    }

    public String getName(){
        return name;
    }

    public abstract void addMember(User user);


    public List<User> getMembers(){
        return members;
    }

    public void deleteMember(User user){
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
        return id == that.id && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

}
