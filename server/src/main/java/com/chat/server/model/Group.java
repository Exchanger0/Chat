
package com.chat.server.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("G")
public class Group extends AbstractChat {
    public Group() {
    }

    public Group(String name) {
        super(name);
    }

    @Override
    public void addMember(User user){
        synchronized (members) {
            members.add(user);
        }
    }

}
