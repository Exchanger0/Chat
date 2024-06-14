package com.chat.server;


import com.chat.shared.RequestResponse;
import com.chat.server.model.User;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    //список созданных потоков
    private final ConcurrentHashMap<String,Thread> threads = new ConcurrentHashMap<>();
    private boolean isStop = false;
    //сеансы всех клиентов одного пользователя
    private final ConcurrentHashMap<User, ArrayList<ClientHandler>> userSessions = new ConcurrentHashMap<>();

    private SessionFactory sessionFactory;

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

            System.out.println(this.getClass().getResource("/hibernate.cfg.xml"));
            StandardServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder().configure(
                    this.getClass().getResource("/hibernate.cfg.xml")
            ).build();
            Metadata metadata = new MetadataSources(serviceRegistry).getMetadataBuilder().build();
            sessionFactory = metadata.getSessionFactoryBuilder().build();

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

    //удаление нерабочего потока
    public void removeThisThread(Thread th){
        threads.remove(th.getName());
    }

    //уведомление всех клиентов одного пользователя
    public void notifyClientHandlers(User user, RequestResponse notification) {
        for (ClientHandler handler : userSessions.get(user)) {
            handler.addServerResponse(notification);
        }
    }

    //добавление клиента к текущей сессии
    public void addClientHandler(User user, ClientHandler clientHandler) {
        if (userSessions.containsKey(user)) {
            userSessions.get(user).add(clientHandler);
        } else {
            userSessions.put(user, new ArrayList<>() {{
                add(clientHandler);
            }});
        }
    }

    //удаление клиента из текущей сессии
    public void deleteClientHandler(User user, ClientHandler handler) {
        if (user == null)
            return;
        userSessions.get(user).remove(handler);
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }
}
