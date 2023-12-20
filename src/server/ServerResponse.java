package server;

import main.RequestResponse;

public record ServerResponse(RequestResponse response, Object writeObject) {}
