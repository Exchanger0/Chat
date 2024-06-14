package com.chat.server.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("C")
public class Chat extends AbstractChat {
    public Chat() {
    }

    public Chat(User user1, User user2) {
        super(user1.getUsername() + "_" + user2.getUsername());
        addMember(user1);
        addMember(user2);
    }

    public String getPseudonym(User from) {
        if (members.getFirst().equals(from)){
            return members.getLast().getUsername();
        }else {
            return members.getFirst().getUsername();
        }
    }

    @Override
    public void addMember(User user) {
        synchronized (members){
            if (members.size() < 2){
                members.add(user);
            }
        }
    }
}
