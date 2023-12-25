package main;

public enum RequestResponse {
    //requests
    REGISTRATION,
    LOG_IN,
    GET_CHAT_NAMES,
    EXIT,
    UPDATE_CHAT,
    SET_CURRENT_CHAT,
    GET_CHAT_MESSAGE,
    SEND_MESSAGE,
    UPDATE_MESSAGES,
    //responses
    SUCCESSFUL_REGISTRATION,
    SUCCESSFUL_LOGIN, LOGIN_ERROR
}
