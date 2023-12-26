package ru.gordeev.december.chat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private int port;
    private List<ClientHandler> clientHandlerList;

    public Server(int port) {
        this.port = port;
        this.clientHandlerList = new ArrayList<>();
    }

    public void start() {
        try (ServerSocket socket = new ServerSocket(port)) {
            System.out.printf("Server has been started at port %d\n", port);
            while (true) {
                Socket clientSocket = socket.accept();
                subscribe(new ClientHandler(this, clientSocket));
            }
        } catch (IOException e) {
            System.out.println("Failed to connect user");
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
                ch.sendMessage("private message from " + clientHandler.getUsername() + ": " + message);
            }
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clientHandlerList.add(clientHandler);
        System.out.println("New user connected: " + clientHandler.getUsername());
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) {
        clientHandlerList.remove(clientHandler);
        System.out.println("User disconnected: " + clientHandler.getUsername());
    }
}
