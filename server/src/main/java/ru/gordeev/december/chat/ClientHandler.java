package ru.gordeev.december.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    private Server server;
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String username;

    public String getUsername() {
        return username;
    }


    public ClientHandler(Server server, Socket socket) throws IOException {
        this.server = server;
        this.socket = socket;
        this.in = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        new Thread(() -> {
            try {
                sendMessage("Server: please login or register");
                authentication();
                processClientsChatMessages();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                disconnect();
            }
        }).start();
    }

    private void processClientsChatMessages() throws IOException {
        while (true) {
            String message = in.readUTF();
            if (message.startsWith("/")) {
                if (message.equals("/exit")) {
                    break;
                }
                if (message.contains("/w")) {
                    String[] splitMessage = message.split(" ", 3);
                    if (splitMessage.length != 3) {
                        sendMessage("Server: incorrect wisp command");
                        continue;
                    }
                    server.sendPrivateMessage(this, splitMessage[1], splitMessage[2]);
                    continue;
                }
            }
            server.broadcastMessage(username + ": " + message);
        }
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
                sendMessage("Server: please login or register");
            }

            if (isSucceed) {
                break;
            }
        }
    }

    private boolean tryToAuthenticate(String message) {
        String[] elements = message.split(" ");
        if (elements.length != 3) {
            sendMessage("Server: incorrect auth command");
            return false;
        }
        String login = elements[1];
        String password = elements[2];
        String usernameFromService = server.getUserService().getUserByLoginAndPassword(login, password);
        if (usernameFromService == null) {
            sendMessage("Server: user doesn't exist with such login and password");
            return false;
        }
        if (server.isUserBusy(usernameFromService)) {
            sendMessage("Server: user is already logged in");
            return false;
        }
        username = usernameFromService;
        sendMessage("/authok " + username);
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
            sendMessage("/authok " + username);
            sendMessage("Server: registration was successful");
            server.subscribe(this);
            return true;
        } else {
            sendMessage("Server: login or username is already taken");
            return false;
        }
    }

    public void sendMessage(String message) {
        try {
            out.writeUTF(message);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void disconnect() {
        server.unsubscribe(this);
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
