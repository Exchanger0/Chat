package server;

import main.User;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    //список пользователей
    private final HashMap<String, User> users = new HashMap<>();
    //список созданных потоков
    private final ConcurrentHashMap<String,Thread> threads = new ConcurrentHashMap<>();
    private boolean isStop = false;
    //сеансы всех клиентов одного пользователя
    private final HashMap<User, ArrayList<ClientHandler>> userSessions = new HashMap<>();

    private Server() {
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }
    private void start(){
        try {
            //при завершении работы сервера
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                isStop = true;
                interruptAllThreads();
            }));

            ServerSocket serverSocket = new ServerSocket(8099);
            while (!isStop) {
                Socket clientSocket = serverSocket.accept();
                if (isStop){
                    break;
                }
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                Thread thread = new Thread(clientHandler);
                threads.put(thread.getName(), thread);
                thread.start();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //сообщаем каждому потоку о его завершении
    private void interruptAllThreads(){
        for (Thread t : threads.values()){
            t.interrupt();
        }
    }

    //добавляем пользователя
    public void addUser(User user){
        synchronized (users) {
            users.put(user.getUsername(), user);
        }
    }

    public User getUser(String username, String password){
        synchronized (users) {
            User u = users.get(username);
            if (u == null){
                return null;
            }
            if (u.equalsPassword(password)){
                return u;
            }
            return null;
        }
    }
    public User getUser(String username){
        synchronized (users) {
            return users.get(username);
        }
    }

    //удаление нерабочего потока
    public void removeThisThread(Thread th){
        threads.remove(th.getName());
    }

    //уведомление всех клиентов одного пользователя
    public void notifyClientHandlers(User user, ServerResponse notification){
        synchronized (userSessions) {
            for (ClientHandler handler : userSessions.get(user)) {
                handler.addServerResponse(notification);
            }
        }
    }

    //добавление клиента к текущей сессии
    public void addClientHandler(User user, ClientHandler clientHandler){
        synchronized (userSessions){
            if (userSessions.containsKey(user)){
                userSessions.get(user).add(clientHandler);
            }else {
                userSessions.put(user, new ArrayList<>(){{add(clientHandler);}});
            }
        }
    }

    //удаление клиента из текущей сессии
    public void deleteClientHandler(User user, ClientHandler handler){
        if (user == null)
            return;
        synchronized (userSessions){
            userSessions.get(user).remove(handler);
        }
    }

}