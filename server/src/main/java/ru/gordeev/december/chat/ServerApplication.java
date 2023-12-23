package ru.gordeev.december.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerApplication {

    public static void main(String[] args) {
        Server server = new Server(8089);

        server.start();
    }
}
