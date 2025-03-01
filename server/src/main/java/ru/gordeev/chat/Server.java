package ru.gordeev.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.gordeev.chat.database.PostgresUserService;
import ru.gordeev.chat.database.UserService;
import ru.gordeev.chat.handlers.BanManagementService;
import ru.gordeev.chat.handlers.ClientHandler;
import ru.gordeev.chat.helpers.ServerMessages;
import ru.gordeev.chat.helpers.UserNotFoundException;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * The main server class responsible for accepting client connections,
 * managing connected ClientHandlers, and performing global operations
 * such as broadcasting messages or banning users. It also periodically
 * checks for inactive clients.
 */
public class Server {

    private ServerSocket serverSocket;
    private final Logger logger;
    private final int port;
    private final List<ClientHandler> clientHandlerList;
    private final UserService userService;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

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
            BanManagementService banManagementService = new BanManagementService();
            banManagementService.startBanCheck();
            logger.info("Server has been started at port {}", port);

            scheduler.scheduleAtFixedRate(this::checkInactivity, 1, 1, TimeUnit.MINUTES);

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

    public synchronized void sendPrivateMessage(ClientHandler sender, String receiverUsername, String message) {
        for (ClientHandler receiver : clientHandlerList)
            if (receiver.getUsername().equals(receiverUsername)) {
                sender.sendMessage(String.format("Your private message to %s: %s", receiver.getUsername(), message));
                receiver.sendMessage(String.format("Private message from %s: %s", sender.getUsername(), message));
                break;
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

    public synchronized void printActiveUsersList(ClientHandler user) {
        StringBuilder sb = new StringBuilder();
        sb.append("Users are online now:\n");

        for (ClientHandler client : clientHandlerList) {
            sb.append("- ").append(client.getUsername()).append("\n");
        }

        user.sendMessage(sb.toString().trim());
    }

    public synchronized void printServerCommandsListList(String username) {
        for (ClientHandler client : clientHandlerList) {
            if (client.getUsername().equals(username)) {
                client.sendMessage(ServerMessages.SERVER_COMMANDS);
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
        return false;
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
        return false;
    }

    public synchronized boolean unbanUser(String username) {
        return getUserService().unsetBan(username);
    }

    public synchronized boolean isBanned(String username) throws UserNotFoundException {
        return getUserService().isBanned(username);
    }

    private void checkInactivity() {
        long now = System.currentTimeMillis();
        long inactivityLimit = 20L * 60L * 1000L; // 20 minutes

        List<ClientHandler> toDisconnect = new ArrayList<>();
        synchronized (clientHandlerList) {
            for (ClientHandler client : clientHandlerList) {
                long lastActivity = client.getLastActivityTime();
                if ((now - lastActivity) > inactivityLimit) {
                    toDisconnect.add(client);
                }
            }
        }

        for (ClientHandler client : toDisconnect) {
            logger.info("Disconnecting user {} due to inactivity", client.getUsername());
            disconnectUserDueToInactivity(client);
        }
    }

    private void disconnectUserDueToInactivity(ClientHandler client) {
        client.sendMessage("Server: you have been disconnected due to inactivity");
        client.sendMessage("/inactive");
        unsubscribe(client);
        client.disconnect();
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
