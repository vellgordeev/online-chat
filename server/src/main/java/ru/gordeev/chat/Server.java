package ru.gordeev.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.gordeev.chat.client_handlers.ClientHandler;
import ru.gordeev.chat.client_handlers.UserService;
import ru.gordeev.chat.database.PostgresUserService;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private final Logger logger;
    private int port;
    private List<ClientHandler> clientHandlerList;
    private UserService userService;

    public UserService getUserService() {
        return userService;
    }

    public Server(int port) {
        this.logger = LogManager.getLogger(Server.class);
        this.port = port;
        this.clientHandlerList = new ArrayList<>();
        this.userService = new PostgresUserService();
    }

    public void start() {
        try (ServerSocket socket = new ServerSocket(port)) {
            logger.info("Server has been started at port {}", port);
            while (true) {
                Socket clientSocket = socket.accept();
                try {
                    new ClientHandler(this, clientSocket);
                } catch (IOException e) {
                    logger.info("Failed to connect user");
                }
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clientHandlerList.add(clientHandler);
        broadcastMessage("Server: new user connected - " + clientHandler.getUsername());
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clientHandlerList.remove(clientHandler);
        broadcastMessage("Server: user disconnected - " + clientHandler.getUsername());
    }

    public synchronized void broadcastMessage(String message) {
        for (ClientHandler client : clientHandlerList) {
            client.sendMessage(message);
        }
    }

    public synchronized void sendPrivateMessage(ClientHandler clientHandler, String receiverUsername, String message) {
        for (ClientHandler ch : clientHandlerList)
            if (ch.getUsername().equals(receiverUsername)) {
                ch.sendMessage(String.format("private message from %s: %s", clientHandler.getUsername(), message));
            }
    }

    public synchronized boolean kickUser(String username) {
        for (ClientHandler client : clientHandlerList) {
            if (client.getUsername().equals(username)) {
                client.sendMessage("/kicked");
                unsubscribe(client);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean disconnectUserDueToInactivity(String username) {
        for (ClientHandler client : clientHandlerList) {
            if (client.getUsername().equals(username)) {
                client.sendMessage("/inactive");
                unsubscribe(client);
                return true;
            }
        }
        return false;
    }

    public synchronized void printActiveUsersList(String username) {
        StringBuilder sb = new StringBuilder();

        sb.append("\nUsers are online now:\n");
        for (ClientHandler client : clientHandlerList) {
            sb.append(client.getUsername());
            sb.append("\n");
        }
        for (ClientHandler client : clientHandlerList) {
            if (client.getUsername().equals(username)) {
                client.sendMessage(sb.toString());
            }
        }
    }

    public synchronized boolean changeUsername(String oldUsername, String newUsername) {
        for (ClientHandler client : clientHandlerList) {
            if (client.getUsername().equals(oldUsername)) {
                client.setUsername(newUsername);
                getUserService().changeUsername(client.getLogin(), newUsername);
                client.sendMessage("Server: your nickname has been changed to " + newUsername);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean isUserBusy(String username) {
        for (ClientHandler client : clientHandlerList) {
            if (client.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }
}
