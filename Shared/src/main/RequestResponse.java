package main;

public enum RequestResponse {
    //requests
    REGISTRATION,
    LOG_IN,
    EXIT,
    CREATE_GROUP,CREATE_CHAT,
    DELETE_GROUP, DELETE_CHAT,
    SET_CURRENT_CHAT,
    SEND_MESSAGE,
    UPDATE_MESSAGES,
    DELETE_FRIEND, ADD_FRIEND, REMOVE_FR_FOR_USER, REMOVE_FR_FROM_USER, SEND_FRIEND_REQUEST, ADD_FR_FROM_USER,
    ADD_FR_FOR_USER,
    DELETE_MEMBER,
    //responses
    SUCCESSFUL_REGISTRATION, REGISTRATION_ERROR,
    SUCCESSFUL_LOGIN, LOGIN_ERROR,
    UPDATE_CHATS
}
