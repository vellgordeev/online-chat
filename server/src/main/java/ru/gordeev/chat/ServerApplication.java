package ru.gordeev.chat;

public class ServerApplication {

    public static void main(String[] args) {
        Server server = new Server(8089);

        server.start();
    }
}
