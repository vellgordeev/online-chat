package ru.gordeev.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.gordeev.chat.client_handlers.BanManagementService;
import ru.gordeev.chat.client_handlers.ClientHandler;
import ru.gordeev.chat.client_handlers.UserService;
import ru.gordeev.chat.database.PostgresUserService;
import ru.gordeev.chat.helpers.UserNotFoundException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private ServerSocket serverSocket;
    private final Logger logger;
    private BanManagementService banManagementService;
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
        try {
            serverSocket = new ServerSocket(port);
            this.banManagementService = new BanManagementService();
            banManagementService.startBanCheck();
            logger.info("Server has been started at port {}", port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                connectUser(clientSocket);
            }
        } catch (IOException e) {
            logger.error("Error while starting server", e);
        }
    }

    private void connectUser(Socket clientSocket) {
        try {
            new ClientHandler(this, clientSocket);
        } catch (IOException e) {
            logger.error("Failed to connect user", e);
        }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clientHandlerList.add(clientHandler);
        broadcastMessage("Server: new user connected - " + clientHandler.getUsername());
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        if (clientHandlerList.remove(clientHandler)) {
            broadcastMessage("Server: user disconnected - " + clientHandler.getUsername());
        }
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
                client.sendMessage("Server: you have been kicked from the server");
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
                client.sendMessage("Server: you have been disconnected from the server due to inactivity");
                client.sendMessage("/inactive");
                unsubscribe(client);
                return true;
            }
        }
        return false;
    }

    public synchronized void printActiveUsersList(String username) {
        StringBuilder sb = new StringBuilder();
        sb.append("Users are online now:\n");

        for (ClientHandler client : clientHandlerList) {
            sb.append("- ").append(client.getUsername()).append("\n");
        }

        for (ClientHandler client : clientHandlerList) {
            if (client.getUsername().equals(username)) {
                client.sendMessage(sb.toString().trim());
            }
        }
    }

    public synchronized void printServerCommandsListList(String username) {
        StringBuilder sb = new StringBuilder();
        sb.append("Commands list (put a '/' before the name):\n");

        sb.append("- register {login} {password} {username} – registration\n");
        sb.append("- auth {login} {password} – authentication\n");
        sb.append("- w {username} – private message\n");
        sb.append("- exit – exit (for client)\n");
        sb.append("- shutdown – stop the server (for admin)\n");
        sb.append("- ban – ban user\n");
        sb.append("- ban {time in minutes}– ban user for some time\n");
        sb.append("- activelist – active clients list\n");
        sb.append("- changenick – change nickname (for admin)\n");

        for (ClientHandler client : clientHandlerList) {
            if (client.getUsername().equals(username)) {
                client.sendMessage(sb.toString().trim());
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

    public synchronized boolean banUser(String username) {
        for (ClientHandler client : clientHandlerList) {
            if (client.getUsername().equals(username)) {
                getUserService().setBan(username);
                client.sendMessage("Server: you have been banned permanently");
                client.sendMessage("/banned");
                unsubscribe(client);
                return true;
            }
        }
        return getUserService().setBan(username);
    }

    public synchronized boolean banUser(String username, Integer durationMinutes) {
        for (ClientHandler client : clientHandlerList) {
            if (client.getUsername().equals(username)) {
                getUserService().setBan(username, durationMinutes);
                client.sendMessage(String.format("Server: you have been banned for %d minutes", durationMinutes));
                client.sendMessage("/tempBanned " + durationMinutes);
                unsubscribe(client);
                return true;
            }
        }
        return getUserService().setBan(username, durationMinutes);
    }

    public synchronized boolean unbanUser(String username) {
        return getUserService().unsetBan(username);
    }

    public synchronized boolean isBanned(String username) throws UserNotFoundException {
        return getUserService().isBanned(username);
    }

    public synchronized void shutdown() {
        var clientsToUnsubscribe = new ArrayList<>(clientHandlerList);
        for (ClientHandler client : clientsToUnsubscribe) {
            client.sendMessage("/shutdown");
            unsubscribe(client);
            client.disconnect();
        }

        try {
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.error("Error closing server socket", e);
        }
    }
}
