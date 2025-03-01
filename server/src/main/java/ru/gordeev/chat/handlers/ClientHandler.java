package ru.gordeev.chat.handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.gordeev.chat.Server;
import ru.gordeev.chat.helpers.UserNotFoundException;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static ru.gordeev.chat.helpers.ServerMessages.*;

/**
 * Manages interaction with a single connected client.
 * Handles commands, message input, and user authentication.
 * Uses the associated Server instance for high-level actions.
 */
public class ClientHandler {

    private final Logger logger;
    private final Server server;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private volatile long lastActivityTime;
    private String login;
    private String username;
    private UserRole userRole;

    public String getUsername() {
        return username;
    }

    public void setUsername(String newUserName) {
        if (this.username.equals(newUserName)) {
            logger.error(new RuntimeException("The old name cannot be equal to the new one!"));
            return;
        }
        this.username = newUserName;
    }

    public String getLogin() {
        return login;
    }

    public long getLastActivityTime() { return lastActivityTime; }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.logger = LogManager.getLogger(ClientHandler.class);
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        Thread clientThread = new Thread(() -> {
            try {
                sendMessage("Server: please login or register");
                authentication();
                processClientsChatMessages();
            } catch (EOFException e) {
                logger.info("Client {} disconnected (EOF)", username);
            } catch (IOException e) {
                logger.error("Error while reading from client {}", username, e);
            } finally {
                disconnect();
            }
        });

        clientThread.start();
    }

    private void processClientsChatMessages() throws IOException {
        while (true) {
            String message = in.readUTF();
            lastActivityTime = System.currentTimeMillis();

            if (message.startsWith("/")) {
                if (message.equals("/exit")) {
                    break;
                }
                if (message.startsWith("/w ")) {
                    sendPrivateMessage(message);
                    continue;
                }
                if (message.startsWith("/kick ")) {
                    executeKickCommand(message);
                    continue;
                }
                if (message.startsWith("/changenick ")) {
                    executeChangeUsernameCommand(message);
                    continue;
                }
                if (message.startsWith("/activelist") && message.equals("/activelist")) {
                    server.printActiveUsersList(this);
                    continue;
                }
                if (message.startsWith("/ban")) {
                    executeBanCommand(message);
                    continue;
                }
                if (message.startsWith("/unban")) {
                    executeUnbanCommand(message);
                    continue;
                }
                if (message.startsWith("/shutdown")) {
                    executeShutdownCommand(message);
                    break;
                }
                if (message.startsWith("/help") && message.equals("/help")) {
                    server.printServerCommandsListList(username);
                    continue;
                }
            }
            server.broadcastMessage(username + ": " + message);
        }
    }

    private void executeShutdownCommand(String message) {
        if (!"/shutdown".equals(message.trim())) {
            sendMessage(getIncorrectCommandFormatMessage("/shutdown"));
            return;
        }

        if (userRole == UserRole.ADMIN) {
            server.broadcastMessage("Server: is shutting down...");
            server.shutdown();
        } else {
            sendMessage(YOU_DONT_HAVE_RIGHTS);
        }
    }

    private void executeKickCommand(String message) {
        String[] splitMessage = message.trim().split(" ", 2);
        if (splitMessage.length != 2 || splitMessage[1].isEmpty()) {
            sendMessage(getIncorrectCommandFormatMessage("/kick"));
            return;
        }
        String userToBeKicked = splitMessage[1];
        if (username.equals(userToBeKicked)) {
            sendMessage("Server: you cannot kick yourself");
            return;
        }

        if (userRole != UserRole.ADMIN) {
            sendMessage(YOU_DONT_HAVE_RIGHTS);
            return;
        }

        if (server.kickUser(userToBeKicked)) {
            server.broadcastMessage(String.format("%s kicked %s", username, userToBeKicked));
        } else {
            sendMessage(COULD_NOT_FIND_USER);
        }
    }

    private void executeBanCommand(String message) {
        String[] splitMessage = message.trim().split(" ", 3);
        if (splitMessage.length < 2 || splitMessage[1].isEmpty()) {
            sendMessage(getIncorrectCommandFormatMessage("/ban"));
            return;
        }

        String username = splitMessage[1];
        Integer banDuration = null;

        if (splitMessage.length == 3 && !splitMessage[2].isEmpty()) {
            try {
                banDuration = Integer.parseInt(splitMessage[2]);
            } catch (NumberFormatException e) {
                sendMessage("Server: incorrect ban duration. Please specify the number of minutes");
                return;
            }
        }

        if (userRole != UserRole.ADMIN) {
            sendMessage(YOU_DONT_HAVE_RIGHTS);
            return;
        }

        boolean banResult;
        if (banDuration == null) {
            banResult = server.banUser(username);
        } else {
            banResult = server.banUser(username, banDuration);
        }
        if (banResult) {
            sendMessage("Server: user " + username + " has been banned" + (banDuration != null ? " for " + banDuration + " minutes" : " permanently"));
        } else {
            sendMessage(COULD_NOT_FIND_USER);
        }
    }

    private void executeUnbanCommand(String message) {
        String[] splitMessage = message.trim().split(" ", 2);
        if (splitMessage.length != 2 || splitMessage[1].isEmpty()) {
            sendMessage(getIncorrectCommandFormatMessage("/unban"));
            return;
        }

        String usernameToUnban = splitMessage[1];
        if (userRole != UserRole.ADMIN) {
            sendMessage(YOU_DONT_HAVE_RIGHTS);
            return;
        }

        if (server.unbanUser(usernameToUnban)) {
            sendMessage("Server: user " + usernameToUnban + " has been unbanned successfully");
        } else {
            sendMessage(COULD_NOT_FIND_USER);
        }
    }

    private void executeChangeUsernameCommand(String message) {
        String[] splitMessage = message.trim().split(" ", 4);
        if (splitMessage.length != 3 || splitMessage[1].isEmpty() || splitMessage[2].isEmpty()) {
            sendMessage(getIncorrectCommandFormatMessage("/changenick"));
            return;
        }

        String oldUsername = splitMessage[1];
        String newUsername = splitMessage[2];
        if (userRole != UserRole.ADMIN) {
            sendMessage(YOU_DONT_HAVE_RIGHTS);
            return;
        }

        if (server.changeUsername(oldUsername, newUsername)) {
            sendMessage("Server: successful name change");
        } else {
            sendMessage(COULD_NOT_FIND_USER);
        }
    }

    private void sendPrivateMessage(String message) {
        String[] splitMessage = message.split(" ", 3);
        if (splitMessage.length != 3) {
            sendMessage(getIncorrectCommandFormatMessage("/w"));
            return;
        }
        server.sendPrivateMessage(this, splitMessage[1], splitMessage[2]);
    }

    private void authentication() throws IOException {
        while (true) {
            String message = in.readUTF();
            boolean isSucceed = false;

            if (message.startsWith("/auth")) {
                isSucceed = tryToAuthenticate(message);
            } else if (message.startsWith("/register")) {
                isSucceed = tryToRegister(message);
            } else {
                sendMessage("Server: please login or register using\n%s".formatted(NEW_USER_HELP));
            }

            if (isSucceed) {
                break;
            }
        }
    }

    private boolean tryToAuthenticate(String message) {
        String[] elements = message.split(" ");
        if (elements.length != 3) {
            sendMessage(getIncorrectCommandFormatMessage("/auth"));
            return false;
        }
        String usernameFromService = server.getUserService().getUsernameByLoginAndPassword(elements[1], elements[2]);
        if (usernameFromService == null) {
            sendMessage("Server: user doesn't exist with such login and password");
            return false;
        }
        if (server.isUserBusy(usernameFromService)) {
            sendMessage("Server: user is already logged in");
            return false;
        }
        try {
            if (server.isBanned(usernameFromService)) {
                sendMessage("Server: user is currently banned");
                return false;
            }
        } catch (UserNotFoundException e) {
            logger.error("Error when searching for a user by the ban service", e);
            sendMessage("Server: it's impossible to identify the user's status");
            return false;
        }
        this.username = usernameFromService;
        this.login = server.getUserService().getUserLogin(username);
        this.userRole = server.getUserService().getUserRole(username);
        sendMessage(String.format(
                "\nServer: welcome to the chat, %s!\n" +
                        "Server: you can find out the list of server commands by calling '/help'", username));
        server.subscribe(this);
        return true;
    }

    private boolean tryToRegister(String message) {
        String[] elements = message.split(" ");
        if (elements.length != 4) {
            sendMessage(getIncorrectCommandFormatMessage("/register"));
            return false;
        }
        String login = elements[1];
        String password = elements[2];
        String usernameFromRegister = elements[3];
        if (server.getUserService().registerUser(login, password, usernameFromRegister)) {
            this.username = usernameFromRegister;
            this.login = login;
            sendMessage("Server: registration was successful");
            server.subscribe(this);
            return true;
        } else {
            sendMessage("Server: login or username is already taken");
            return false;
        }
    }

    public void sendMessage(String message) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedDateTime = now.format(formatter);

        try {
            out.writeUTF("[" + formattedDateTime + "] " + message);
        } catch (IOException e) {
            logger.error("Error while sending message", e);
        }
    }

    public void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            logger.error(e);
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            logger.error(e);
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }
}
