package server;

import main.RequestResponse;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final ObjectOutputStream objectOutputStream;

    private User currentUser;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());
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
            while (!Thread.currentThread().isInterrupted() && (str = reader.readLine()) != null) {
                if (str.equals(RequestResponse.REGISTRATION.name())) {
                    System.out.println("Start registration " + currThreadName);
                    registration();
                    writer.println(RequestResponse.SUCCESSFUL_REGISTRATION.name());
                    writer.flush();
                    System.out.println("End registration " + currThreadName);
                } else if (str.equals(RequestResponse.LOG_IN.name())) {
                    System.out.println("Start login " + currThreadName);
                    logIn();
                    System.out.println("End login " + currThreadName);
                } else if (str.equals(RequestResponse.GET_CHAT_NAMES.name())) {
                    objectOutputStream.writeObject(currentUser.getChatNames());
                    objectOutputStream.flush();
                } else if (str.equals(RequestResponse.ADD_CHAT.name())) {
                    currentUser.addChat(new Chat(reader.readLine()));
                    objectOutputStream.writeObject(currentUser.getChatNames());
                    objectOutputStream.flush();
                } else if (str.equals(RequestResponse.EXIT.name())) {
                    System.out.println("Exit from system " + currThreadName);
                    server.removeThisThread(Thread.currentThread());
                    break;
                }
            }
            reader.close();
            writer.close();
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
            System.out.println(user);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void logIn() {
        try {
            String username = reader.readLine();
            String password = reader.readLine();

            User user = server.getUser(username, getHash(password));
            if (user != null) {
                currentUser = user;
                writer.println(RequestResponse.SUCCESSFUL_LOGIN.name());
            } else {
                writer.println(RequestResponse.LOGIN_ERROR.name());
            }
            writer.flush();
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
}
