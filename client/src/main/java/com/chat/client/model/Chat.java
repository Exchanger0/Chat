package com.chat.client.model;

public class Chat extends AbstractChat {
    public Chat(int id, String user1, String user2) {
        super(id, user1+ "_" + user2);
        addMember(user1);
        addMember(user2);
    }

    public String getPseudonym(String from) {
        if (members.getFirst().equals(from)){
            return members.getLast();
        }else {
            return members.getFirst();
        }
    }

    @Override
    public void addMember(String user) {
        if (members.size() < 2){
            members.add(user);
        }
    }
}
