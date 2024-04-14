package main;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class User implements Serializable {
    private final String username;
    private final String password;
    private final HashMap<String, Group> groups = new HashMap<>();
    private final List<User> friends = new ArrayList<>();
    private final List<User> fRequestsForUser = new ArrayList<>();
    private final List<User> fRequestsFromUser = new ArrayList<>();

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public boolean equalsPassword(String password){
        return this.password.equals(password);
    }

    public List<Group> getGroups(){
        synchronized (groups) {
            return new ArrayList<>(groups.values());
        }
    }

    public Group getGroup(String name){

        return groups.get(name);
    }

    public synchronized void addGroup(Group group){
        synchronized (groups) {
            groups.put(group.getName(), group);
        }
    }

    public boolean deleteFriend(User user){
        synchronized (friends) {
            return friends.remove(user);
        }
    }

    public List<User> getFriends() {
        synchronized (friends) {
            return friends;
        }
    }

    public boolean addFRequestForUser(User user){
        synchronized (friends) {
            synchronized (fRequestsFromUser) {
                synchronized (fRequestsForUser) {
                    if (!friends.contains(user) && !fRequestsForUser.contains(user) && !fRequestsFromUser.contains(user)) {
                        fRequestsForUser.add(user);
                        return true;
                    }
                    return false;
                }
            }
        }
    }

    public boolean deleteFRequestForUser(User user){
        synchronized (fRequestsForUser) {
            return fRequestsForUser.remove(user);
        }
    }

    public List<User> getFRequestsForUser() {
        synchronized (fRequestsForUser) {
            return fRequestsForUser;
        }
    }

    public boolean addFRequestFromUser(User user){
        synchronized (friends) {
            synchronized (fRequestsFromUser) {
                synchronized (fRequestsForUser) {
                    if (!friends.contains(user) && !fRequestsFromUser.contains(user) && !fRequestsForUser.contains(user)) {
                        fRequestsFromUser.add(user);
                        return true;
                    }
                    return false;
                }
            }
        }
    }

    public boolean deleteFRequestFromUser(User user){
        synchronized (fRequestsFromUser) {
            return fRequestsFromUser.remove(user);
        }
    }

    public List<User> getFRequestsFromUser() {
        synchronized (fRequestsFromUser) {
            return fRequestsFromUser;
        }
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

    public boolean addFriend(User user){
        synchronized (friends) {
            if (!friends.contains(user)) {
                friends.add(user);
                return true;
            }
            return false;
        }
    }
}
