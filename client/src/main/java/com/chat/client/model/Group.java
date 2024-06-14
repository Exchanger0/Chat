
package com.chat.client.model;

public class Group extends AbstractChat {

    public Group(int id, String name) {
        super(id, name);
    }

    @Override
    public void addMember(String user){
        members.add(user);
    }
}
