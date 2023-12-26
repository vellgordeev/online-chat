package ru.gordeev.december.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientApplication {

    public static void main(String[] args) {
        try (
                Socket socket = new Socket("localhost", 8089);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            System.out.println("Successful connection to server");
            Scanner scanner = new Scanner(System.in);
            Thread t = new Thread(() -> {
                try {
                    while (true) {
                        String message = in.readUTF();
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            t.start();
            while (true) {
                String message = scanner.nextLine();
                out.writeUTF(message);
                if (message.equals("/exit")) {
                    t.stop();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
