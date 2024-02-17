package ru.gordeev.chat.client_handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.gordeev.chat.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientHandler {

    private Logger logger;
    private UserActivityWatcher userActivityWatcher;
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
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

    public UserRole getUserRole() {
        return userRole;
    }

    public String getLogin() {
        return login;
    }

    public ClientHandler(Server server, Socket socket) throws IOException {
        this.logger = LogManager.getLogger(ClientHandler.class);
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                sendMessage("Server: please login (/auth {login} {password}) or register (/register {login} {password} {nickname})");
                authentication();
                processClientsChatMessages();
            } catch (IOException e) {
                logger.error(e);
            } finally {
                disconnect();
            }
        }).start();
    }

    public void executeInactiveCommand() {
        if (server.disconnectUserDueToInactivity(username)) {
            server.broadcastMessage(String.format("Server: the user %s was disconnected due to inactivity", username));
        }
    }

    private void processClientsChatMessages() throws IOException {
        while (true) {
            String message = in.readUTF();
            if (userActivityWatcher != null) {
                userActivityWatcher.onUserActivity();
            }
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
                    server.printActiveUsersList(username);
                    continue;
                }
            }
            server.broadcastMessage(username + ": " + message);
        }
    }

    private void executeKickCommand(String message) {
        String[] splitMessage = message.trim().split(" ", 2);
        if (splitMessage.length != 2 || splitMessage[1].isEmpty()) {
            sendMessage("Server: Incorrect '/kick' command format");
            return;
        }
        String userToBeKicked = splitMessage[1];
        if (username.equals(userToBeKicked)) {
            sendMessage("Server: you cannot kick yourself");
            return;
        }

        if (userRole == UserRole.ADMIN) {
            if (server.kickUser(userToBeKicked)) {
                server.broadcastMessage(String.format("%s kicked %s", username, userToBeKicked));
            } else {
                sendMessage("Server: couldn't find such user");
            }
        } else {
            sendMessage("Server: you don't have rights for this command");
        }
    }

    private void executeChangeUsernameCommand(String message) {
        String[] splitMessage = message.trim().split(" ", 4);
        if (splitMessage.length != 3 || splitMessage[1].isEmpty() || splitMessage[2].isEmpty()) {
            sendMessage("Server: incorrect '/changenick' command format");
            return;
        }

        String oldUsername = splitMessage[1];
        String newUsername = splitMessage[2];
        if (userRole == UserRole.ADMIN) {
            if (server.changeUsername(oldUsername, newUsername)) {
                sendMessage("Server: successful name change");
            } else {
                sendMessage("Server: couldn't find such user");
            }
        } else {
            sendMessage("Server: you don't have rights for this command");
        }
    }

    private void sendPrivateMessage(String message) {
        String[] splitMessage = message.split(" ", 3);
        if (splitMessage.length != 3) {
            sendMessage("Server: incorrect wisp command");
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
                sendMessage("Server: please login (/auth {login} {password}) or register (/register {login} {password} {nickname})");
            }

            if (isSucceed) {
                break;
            }
        }
        if (userRole == UserRole.USER) {
            this.userActivityWatcher = new UserActivityWatcher(this);
        }
    }

    private boolean tryToAuthenticate(String message) {
        String[] elements = message.split(" ");
        if (elements.length != 3) {
            sendMessage("Server: incorrect auth command");
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
        this.username = usernameFromService;
        this.login = server.getUserService().getUserLogin(username);
        this.userRole = server.getUserService().getUserRole(username);
        sendMessage(String.format("Server: welcome to the chat, %s!", username));
        server.subscribe(this);
        return true;
    }

    private boolean tryToRegister(String message) {
        String[] elements = message.split(" ");
        if (elements.length != 4) {
            sendMessage("Server: incorrect register command");
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
            logger.error(e);
        }
    }

    private void disconnect() {
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
