package server;

import main.RequestResponse;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    //ответы которые надо отправить клиенту
    private final BlockingQueue<ServerResponse> responses = new ArrayBlockingQueue<>(30);
    private final BufferedReader reader;
    private final ObjectOutputStream objectOutputStream;

    private User currentUser;
    private Chat currentChat;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            String currThreadName = Thread.currentThread().getName();
            String str;
            Thread senderThread = new Thread(new Sender(), "Sender"+currThreadName.substring(currThreadName.indexOf("-")));
            senderThread.start();
            while (!Thread.currentThread().isInterrupted() && (str = reader.readLine()) != null) {
                if (str.equals(RequestResponse.REGISTRATION.name())) {
                    System.out.println("Start registration " + currThreadName);
                    registration();
                    responses.add(new ServerResponse(RequestResponse.SUCCESSFUL_REGISTRATION, null));
                    System.out.println("End registration " + currThreadName);
                } else if (str.equals(RequestResponse.LOG_IN.name())) {
                    System.out.println("Start login " + currThreadName);
                    logIn();
                    System.out.println("End login " + currThreadName);
                } else if (str.equals(RequestResponse.GET_CHAT_NAMES.name())) {
                    responses.add(new ServerResponse(RequestResponse.GET_CHAT_NAMES, currentUser.getChatNames()));
                } else if (str.equals(RequestResponse.CREATE_CHAT.name())) {
                    Chat chat = new Chat(reader.readLine());
                    chat.addMember(currentUser);
                    String[] memberNames = reader.readLine().split("\\Q$\\E");
                    for (String memberName : memberNames){
                        User user = server.getUser(memberName);
                        if (user != null) {
                            user.addChat(chat);
                            server.notifyClientHandlers(user, new ServerResponse(RequestResponse.UPDATE_CHATS, user.getChatNames()));
                            chat.addMember(user);
                        }
                    }
                    currentUser.addChat(chat);
                    server.notifyClientHandlers(currentUser, new ServerResponse(RequestResponse.UPDATE_CHATS, currentUser.getChatNames()));
                } else if (str.equals(RequestResponse.SET_CURRENT_CHAT.name())) {
                    currentChat = currentUser.getChat(reader.readLine());
                } else if (str.equals(RequestResponse.GET_CHAT_MESSAGE.name())) {
                    responses.add(new ServerResponse(RequestResponse.UPDATE_MESSAGES,
                            new ArrayList<>(currentChat.getMessages())));
                } else if (str.equals(RequestResponse.SEND_MESSAGE.name())) {
                    currentChat.sendMessage(currentUser.getUsername() + ": " + reader.readLine());
                    for (User user : currentChat.getMembers()){
                        server.notifyClientHandlers(user, new ServerResponse(RequestResponse.UPDATE_MESSAGES,
                                new ArrayList<>(currentChat.getMessages())));
                    }
                } else if (str.equals(RequestResponse.GET_MEMBERS_NAME.name())) {
                    responses.add(new ServerResponse(RequestResponse.MEMBERS_NAME,
                            currentChat.getMembers().stream().map(User::getUsername).collect(Collectors.toList())));
                } else if (str.equals(RequestResponse.EXIT.name())) {
                    System.out.println("Exit from system " + currThreadName);
                    responses.add(new ServerResponse(RequestResponse.EXIT, null));
                    server.deleteClientHandler(currentUser, this);
                    server.removeThisThread(Thread.currentThread());
                    break;
                }
            }
            reader.close();
            objectOutputStream.close();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void registration() {
        try {
            String username = reader.readLine();
            String password = reader.readLine();

            User user = new User(username, getHash(password));
            server.addUser(user);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void logIn() {
        try {
            String username = reader.readLine();
            String password = reader.readLine();

            User user = server.getUser(username, getHash(password));
            RequestResponse response;
            if (user != null) {
                currentUser = user;
                server.addClientHandler(user, this);
                response = RequestResponse.SUCCESSFUL_LOGIN;
            } else {
                response = RequestResponse.LOGIN_ERROR;
            }
            responses.add(new ServerResponse(response, null));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            BigInteger bigInteger = new BigInteger(1, hash);

            return bigInteger.toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }


    public void addServerResponse(ServerResponse serverResponse) {
        responses.add(serverResponse);
    }

    //поток отправляющий все задачи клиенту
    private class Sender implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    ServerResponse serverResponse = responses.take();
                    if (serverResponse.response().equals(RequestResponse.EXIT)) {
                        break;
                    }
                    objectOutputStream.writeUTF(serverResponse.response().name());
                    objectOutputStream.flush();
                    if (serverResponse.writeObject() != null) {
                        objectOutputStream.writeObject(serverResponse.writeObject());
                        objectOutputStream.flush();
                    }
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
