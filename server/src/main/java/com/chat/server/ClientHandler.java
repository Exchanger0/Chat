package com.chat.server;


import com.chat.server.model.AbstractChat;
import com.chat.server.model.Chat;
import com.chat.server.model.Group;
import com.chat.server.model.User;
import com.chat.shared.*;
import org.hibernate.Session;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;

import static com.chat.shared.RequestResponse.Title.*;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    //ответы которые надо отправить клиенту
    private final BlockingQueue<RequestResponse> responses = new ArrayBlockingQueue<>(100);
    private final ObjectInputStream reader;
    private final ObjectOutputStream objectOutputStream;

    private User currentUser;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
        try {
            objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
            reader = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        try {
            String currThreadName = Thread.currentThread().getName();
            RequestResponse request;
            Thread senderThread = new Thread(new Sender(), "Sender"+currThreadName.substring(currThreadName.indexOf("-")));
            senderThread.start();
            outer:
            while (!Thread.currentThread().isInterrupted() && (request = (RequestResponse) reader.readObject()) != null) {
                switch (request.getTitle()) {
                    case REGISTRATION -> {
                        System.out.println("\nStart registration");
                        registration(request);
                        System.out.println("End registration");
                    }
                    case LOG_IN -> {
                        System.out.println("\nStart login");
                        logIn(request);
                        System.out.println("End login. Current user: "
                                + (currentUser != null ? currentUser.getUsername() : " error"));
                    }
                    case CREATE_GROUP -> {
                        System.out.println("\nCreate group");
                        createGroup(request);
                        System.out.println("Create group '" + request.getField("chatName") + "' by '"
                                + currentUser.getUsername() + "'");
                    }
                    case CREATE_CHAT ->  {
                        System.out.println("\nCreate chat");
                        createChat(request);
                        System.out.println("'" + currentUser.getUsername() + "' create chat");
                    }
                    case DELETE_GROUP -> {
                        System.out.println("\nDelete group");
                        deleteGroup(request);
                        System.out.println("delete group '" + request.getField("chatName") + "' by '"
                                + currentUser.getUsername() + "'");
                    }
                    case DELETE_CHAT -> {
                        System.out.println("\nCreate chat");
                        deleteChat(request);
                        System.out.println("delete chat '" + request.getField("chatName") + "' by '"
                                + currentUser.getUsername() + "'");
                    }
                    case GET_CHAT -> getChat(request);
                    case SEND_MESSAGE -> {
                        System.out.println("\nSend message");
                        sendMessage(request);
                        System.out.println("'" + currentUser.getUsername() + "' send message to chat '"
                                + request.getField("chatName") + "'");
                    }
                    case DELETE_FRIEND -> {
                        System.out.println("\nDelete friend");
                        deleteFriend(request);
                        System.out.println("'" + currentUser.getUsername() + "' delete friend '"
                                + request.getField("username" + "'"));
                    }
                    case ADD_FRIEND -> {
                        System.out.println("\nAdd friend");
                        addFriend(request);
                        System.out.println("'" + currentUser.getUsername() + "' add friend '"
                                + request.getField("username") + "'");
                    }
                    case REMOVE_FR_FOR_USER -> {
                        System.out.println("\nRemove fr_for_user");
                        removeFRForUser(request);
                    }
                    case REMOVE_FR_FROM_USER -> {
                        System.out.println("\nRemove fr_from_user");
                        removeFRFromUser(request);
                    }
                    case SEND_FRIEND_REQUEST -> {
                        System.out.println("\nSend friend request");
                        sendFriendRequest(request);
                    }
                    case EXIT -> {
                        System.out.println("Exit from system " + currThreadName);
                        responses.add(new RequestResponse(EXIT));
                        server.deleteClientHandler(currentUser, this);
                        server.removeThisThread(Thread.currentThread());
                        break outer;
                    }
                }
            }
            reader.close();
            objectOutputStream.close();
            socket.close();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void registration(RequestResponse request) {
        String username = request.getField("username");
        String password = request.getField("password");

        Session session = server.getSessionFactory().openSession();
        try {
            session.beginTransaction();
            User user = new User(username, getHash(password));
            session.persist(user);
            session.getTransaction().commit();
            RequestResponse response = new RequestResponse(SUCCESSFUL_REGISTRATION);
            response.setField("username", request.getField("username"));
            response.setField("password", request.getField("password"));
            responses.add(response);
        }catch (Exception ex){
            session.getTransaction().rollback();
            responses.add(new RequestResponse(REGISTRATION_ERROR));
        }finally {
            session.close();
        }
    }

    private void logIn(RequestResponse request) {
        String username = request.getField("username");
        String password = request.getField("password");

        Session session = server.getSessionFactory().openSession();
        try {
            session.beginTransaction();

            User user = session.createQuery(
                "SELECT u FROM User u " +
                        "LEFT JOIN u.friends " +
                        "LEFT JOIN u.fRequestsForUser " +
                        "LEFT JOIN u.fRequestsFromUser " +
                        "LEFT JOIN u.chats " +
                        "where u.username = :username and u.password = :password", User.class)

                    .setParameter("username", username).setParameter("password", getHash(password))
                    .getSingleResult();

            if (user != null) {
                currentUser = user;
                server.addClientHandler(user, this);
                RequestResponse response = new RequestResponse(SUCCESSFUL_LOGIN);
                response.setField("username", user.getUsername());
                response.setField("chatData", user.getChats()
                        .stream()
                        .map(chat -> {
                            ChatType type = ChatType.GROUP;
                            String publicName = chat.getName();
                            if (chat instanceof Chat ch) {
                                type = ChatType.CHAT;
                                publicName = ch.getPseudonym(currentUser);
                            }
                            return new ChatData(chat.getId(), type, publicName, chat.getName());
                        })
                        .collect(Collectors.toCollection(ArrayList::new)));
                response.setField("friends", user.getFriends()
                        .stream()
                        .map(User::getUsername)
                        .collect(Collectors.toCollection(ArrayList::new)));
                response.setField("fRequestsForUser", user.getFRequestsForUser()
                        .stream()
                        .map(User::getUsername)
                        .collect(Collectors.toCollection(ArrayList::new)));
                response.setField("fRequestsFromUser", user.getFRequestsFromUser()
                        .stream()
                        .map(User::getUsername)
                        .collect(Collectors.toCollection(ArrayList::new)));
                responses.add(response);
            }
            session.getTransaction().commit();
        }catch (Exception ex) {
            RequestResponse response = new RequestResponse(LOGIN_ERROR);
            responses.add(response);
            session.getTransaction().rollback();
        }finally {
            session.close();
        }

    }

    private void createGroup(RequestResponse request) {
        Session session = server.getSessionFactory().openSession();
        try {
            session.beginTransaction();
            Group group = new Group(request.getField("chatName"));

            ArrayList<String> memberNames = request.getField("members");
            memberNames.add(currentUser.getUsername());

            List<User> users = session.createQuery("select u from User u where u.username in :name_list", User.class)
                    .setParameter("name_list", memberNames)
                    .getResultList();

            session.persist(group);
            System.out.println(group.getId());
            RequestResponse response = new RequestResponse(UPDATE_CHATS);
            response.setField("id", group.getId());
            response.setField("type", ChatType.GROUP);
            response.setField("chatName", group.getName());
            response.setField("members", memberNames);

            for (User user : users) {
                group.addMember(user);
                server.notifyClientHandlers(user, response);
            }
            session.getTransaction().commit();
        }catch (Exception ex) {
            session.getTransaction().rollback();
        } finally {
            session.close();
        }
    }

    private void createChat(RequestResponse request) {
        Session session = server.getSessionFactory().openSession();
        try {
            session.beginTransaction();
            currentUser = session.get(User.class, currentUser.getId());

            User user = session.createQuery("select u from User u where u.username = :name", User.class)
                    .setParameter("name", request.getField("username"))
                    .getSingleResult();

            if (user != null) {
                Chat chat = new Chat(currentUser, user);
                chat.addMember(currentUser);
                chat.addMember(user);
                session.persist(chat);
                RequestResponse response = new RequestResponse(UPDATE_CHATS);
                response.setField("id", chat.getId());
                response.setField("type", ChatType.CHAT);
                response.setField("members", new ArrayList<>(List.of(currentUser.getUsername(), user.getUsername())));
                server.notifyClientHandlers(user, response);
                server.notifyClientHandlers(currentUser, response);
            }
            session.getTransaction().commit();
        }catch (Exception ex) {
            session.getTransaction().rollback();
        }finally {
            session.close();
        }
    }

    private void deleteGroup(RequestResponse request) {
        Session session = server.getSessionFactory().openSession();
        try {
            session.beginTransaction();
            currentUser = session.get(User.class, currentUser.getId());

            AbstractChat group = currentUser.getChat(request.getField("id"));
            group.deleteMember(currentUser);
            currentUser.deleteChat(group);

            if (group.getMembers().isEmpty()) {
                session.remove(group);
            }else {
                RequestResponse response = new RequestResponse(DELETE_MEMBER);
                response.setField("id", group.getId());
                response.setField("chatName", group.getName());
                response.setField("username", currentUser.getUsername());
                for (User user : group.getMembers()) {
                    server.notifyClientHandlers(user, response);
                }
            }
            RequestResponse response1 = new RequestResponse(DELETE_GROUP);
            response1.setField("id", group.getId());
            server.notifyClientHandlers(currentUser, response1);

            session.getTransaction().commit();
        }catch (Exception ex){
            session.getTransaction().rollback();
        }finally {
            session.close();
        }
    }

    private void deleteChat(RequestResponse request) {
        System.out.println(currentUser.getUsername() + " delete chat " + request.getField("chatName"));
        Session session = server.getSessionFactory().openSession();
        try {
            session.beginTransaction();
            currentUser = session.get(User.class, currentUser.getId());

            AbstractChat chat = currentUser.getChat(request.getField("id"));

            RequestResponse response = new RequestResponse(DELETE_CHAT);
            response.setField("id", chat.getId());

            for (User user : chat.getMembers()) {
                user.deleteChat(chat);
                server.notifyClientHandlers(user, response);
            }
            session.remove(chat);
            session.getTransaction().commit();
        }catch (Exception ex) {
            session.getTransaction().rollback();
        }finally {
            session.close();
        }
    }

    private void sendMessage(RequestResponse request) {
        Session session = server.getSessionFactory().openSession();
        try {
            session.beginTransaction();
            currentUser = session.get(User.class, currentUser.getId());

            String message = currentUser.getUsername() + ": " + request.getField("message");
            AbstractChat chat = currentUser.getChat(request.getField("id"));
            chat.sendMessage(message);
            RequestResponse response = new RequestResponse(UPDATE_MESSAGES);
            response.setField("id", chat.getId());
            response.setField("chatName", chat.getName());
            response.setField("message", message);
            for (User user : chat.getMembers()) {
                server.notifyClientHandlers(user, response);
            }
            session.getTransaction().commit();
        }catch (Exception ex) {
            session.getTransaction().rollback();
        }finally {
            session.close();
        }
    }

    private void deleteFriend(RequestResponse request) {
        Session session = server.getSessionFactory().openSession();
        try {
            session.beginTransaction();
            currentUser = session.get(User.class, currentUser.getId());

            User deleteFriend = session.createQuery("select df from User df where df.username = :username", User.class)
                    .setParameter("username",request.getField("friendUsername"))
                    .getSingleResult();
            if (currentUser.deleteFriend(deleteFriend)
                    && deleteFriend.deleteFriend(currentUser)) {
                RequestResponse response = new RequestResponse(DELETE_FRIEND);
                response.setField("username", deleteFriend.getUsername());
                server.notifyClientHandlers(currentUser, response);
                RequestResponse response1 = new RequestResponse(DELETE_FRIEND);
                response1.setField("username", currentUser.getUsername());
                server.notifyClientHandlers(deleteFriend, response1);
            }
            session.getTransaction().commit();
        }catch (Exception ex) {
            session.getTransaction().rollback();
        }finally {
            session.close();
        }
    }


    private void addFriend(RequestResponse request) {
        Session session = server.getSessionFactory().openSession();
        try {
            session.beginTransaction();
            currentUser = session.get(User.class, currentUser.getId());
            User newFriend  = session.createQuery("select nf from User nf where nf.username = :username", User.class)
                    .setParameter("username", request.getField("friendUsername"))
                    .getSingleResult();

            System.out.println(currentUser.getFriends() + "\n" +
                    currentUser.getFRequestsForUser() + "\n" +
                    currentUser.getFRequestsFromUser() + "\n--------\n" +
                    newFriend.getFriends() + "\n" +
                    newFriend.getFRequestsForUser() + "\n" +
                    newFriend.getFRequestsFromUser());

            if (!currentUser.getFriends().contains(newFriend) && !newFriend.getFriends().contains(currentUser)) {
                currentUser.addFriend(newFriend);
                newFriend.addFriend(currentUser);
                currentUser.deleteFRequestForUser(newFriend);
                newFriend.deleteFRequestFromUser(currentUser);

                RequestResponse response = new RequestResponse(REMOVE_FR_FOR_USER);
                response.setField("username", newFriend.getUsername());
                RequestResponse response1 = new RequestResponse(ADD_FRIEND);
                response1.setField("username", newFriend.getUsername());
                server.notifyClientHandlers(currentUser, response);
                server.notifyClientHandlers(currentUser, response1);
                RequestResponse response2 = new RequestResponse(REMOVE_FR_FROM_USER);
                response2.setField("username", currentUser.getUsername());
                RequestResponse response3 = new RequestResponse(ADD_FRIEND);
                response3.setField("username", currentUser.getUsername());
                server.notifyClientHandlers(newFriend, response2);
                server.notifyClientHandlers(newFriend, response3);
            }
            session.getTransaction().commit();
        }catch (Exception ex) {
            session.getTransaction().rollback();
        }finally {
            session.close();
        }
    }

    private void removeFRForUser(RequestResponse request) {
        Session session = server.getSessionFactory().openSession();
        try {
            session.beginTransaction();
            currentUser = session.get(User.class, currentUser.getId());

            User user = session.createQuery("select u from User u where u.username = :username", User.class)
                    .setParameter("username", request.getField("username"))
                    .getSingleResult();
            currentUser.deleteFRequestForUser(user);
            user.deleteFRequestFromUser(currentUser);

            RequestResponse response = new RequestResponse(REMOVE_FR_FOR_USER);
            response.setField("username", user.getUsername());
            server.notifyClientHandlers(currentUser, response);
            RequestResponse response1 = new RequestResponse(REMOVE_FR_FROM_USER);
            response1.setField("username", currentUser.getUsername());
            server.notifyClientHandlers(user, response1);

            session.getTransaction().commit();
        }catch (Exception ex) {
            session.getTransaction().rollback();
        }finally {
            session.close();
        }
    }

    private void removeFRFromUser(RequestResponse request) {
        Session session = server.getSessionFactory().openSession();
        try {
            session.beginTransaction();
            currentUser = session.get(User.class, currentUser.getId());

            User user = session.createQuery("select u from User u where u.username = :username", User.class)
                    .setParameter("username", request.getField("username"))
                    .getSingleResult();
            currentUser.deleteFRequestFromUser(user);
            user.deleteFRequestForUser(currentUser);
            RequestResponse response = new RequestResponse(REMOVE_FR_FROM_USER);
            response.setField("username", user.getUsername());
            server.notifyClientHandlers(currentUser, response);
            RequestResponse response1 = new RequestResponse(REMOVE_FR_FOR_USER);
            response1.setField("username", currentUser.getUsername());
            server.notifyClientHandlers(user, response1);

            session.getTransaction().commit();
        }catch (Exception ex) {
            session.getTransaction().rollback();
        }finally {
            session.close();
        }
    }

    private void sendFriendRequest(RequestResponse request) {
        Session session = server.getSessionFactory().openSession();
        try {
            session.beginTransaction();
            currentUser = session.get(User.class, currentUser.getId());

            System.out.println("Send frequest from: " + currentUser.getUsername() + " for " + request.getField("username"));
            User user = session.createQuery("select u from User u where u.username = :username", User.class)
                    .setParameter("username", request.getField("username"))
                    .getSingleResult();
            if (user != null) {
                if (currentUser.canAddFRequestFromUser(user) && user.canAddFRequestForUser(currentUser)) {
                    currentUser.addFRequestFromUser(user);
                    user.addFRequestForUser(currentUser);

                    RequestResponse response = new RequestResponse(ADD_FR_FROM_USER);
                    response.setField("username", user.getUsername());
                    server.notifyClientHandlers(currentUser, response);
                    RequestResponse response1 = new RequestResponse(ADD_FR_FOR_USER);
                    response1.setField("username", currentUser.getUsername());
                    server.notifyClientHandlers(user, response1);

                    session.persist(user);
                }
            }
            session.getTransaction().commit();
        }catch (Exception ex) {
            session.getTransaction().rollback();
        }finally {
            session.close();
        }
    }

    private void getChat(RequestResponse request) {
        Session session = server.getSessionFactory().openSession();
        try {
            session.beginTransaction();
            currentUser = session.get(User.class, currentUser.getId());

            AbstractChat chat = currentUser.getChat(request.getField("id"));
            ChatType type = ChatType.GROUP;
            if (chat instanceof Chat) type = ChatType.CHAT;
            RequestResponse response = new RequestResponse(GET_CHAT);
            response.setField("id", chat.getId());
            response.setField("type", type);
            response.setField("chatName", request.getField("chatName"));
            response.setField("members", chat.getMembers()
                    .stream()
                    .map(User::getUsername)
                    .collect(Collectors.toCollection(ArrayList::new)));
            response.setField("messages", new ArrayList<>(chat.getMessages()));
            addServerResponse(response);

            session.getTransaction().commit();
        }catch (Exception ex) {
            session.getTransaction().rollback();
        }finally {
            session.close();
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


    public void addServerResponse(RequestResponse serverResponse) {
        responses.add(serverResponse);
    }

    //поток отправляющий все задачи клиенту
    private class Sender implements Runnable {

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    RequestResponse serverResponse = responses.take();
                    if (serverResponse.getTitle().equals(EXIT)) {
                        break;
                    }
                    objectOutputStream.writeObject(serverResponse);
                    objectOutputStream.flush();
                } catch (InterruptedException | IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
