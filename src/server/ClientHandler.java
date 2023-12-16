package server;

import main.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

    private User currentUser;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream());
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
                if (str.equals(Request.REGISTRATION.name())) {
                    System.out.println("Start registration " + currThreadName);
                    registration();
                    writer.println("Registration success");
                    writer.flush();
                    System.out.println("End registration " + currThreadName);
                } else if (str.equals(Request.LOG_IN.name())) {
                    System.out.println("Start login " + currThreadName);
                    logIn();
                    System.out.println("End login " + currThreadName);
                } /*else if (str.equals(Request.LOG_OUT.name())) {
                    currentUser = null;
                    System.out.println("Log out " + currThreadName);
                } */else if (str.equals("exit")) {
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
                writer.println("Successful login");
            } else {
                writer.println("Login error");
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
