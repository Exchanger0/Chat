package main;

public enum RequestResponse {
    //requests
    REGISTRATION,
    LOG_IN,
    EXIT,
    CREATE_CHAT,
    SET_CURRENT_CHAT,
    SEND_MESSAGE,
    UPDATE_MESSAGES,
    //responses
    SUCCESSFUL_REGISTRATION,
    SUCCESSFUL_LOGIN, LOGIN_ERROR,
    UPDATE_CHATS
}
