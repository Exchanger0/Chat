
package com.chat.shared;

public class Chat extends Group {
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
