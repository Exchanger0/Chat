package com.chat.shared;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RequestResponse implements Serializable {

    public enum Title {
        REGISTRATION,
        LOG_IN,
        EXIT,
        CREATE_GROUP, DELETE_GROUP,
        CREATE_CHAT, DELETE_CHAT,
        GET_CHAT,
        SEND_MESSAGE, UPDATE_MESSAGES,
        DELETE_FRIEND, ADD_FRIEND, REMOVE_FR_FOR_USER, REMOVE_FR_FROM_USER, SEND_FRIEND_REQUEST, ADD_FR_FROM_USER,
        ADD_FR_FOR_USER,
        DELETE_MEMBER,
        SUCCESSFUL_REGISTRATION, REGISTRATION_ERROR,
        SUCCESSFUL_LOGIN, LOGIN_ERROR,
        UPDATE_CHATS
    }

    private final Title title;
    private final Map<String, Serializable> fields = new HashMap<>();

    public RequestResponse(Title title) {
        this.title = title;
    }

    public void setField(String name, Serializable value) {
        fields.put(name, value);
    }

    public <T extends Serializable> T getField(String name) {
        return (T) fields.get(name);
    }

    public Title getTitle() {
        return title;
    }

    public Set<String> getKeys() {
        return fields.keySet();
    }
}
