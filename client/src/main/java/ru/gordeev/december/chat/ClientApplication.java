package ru.gordeev.december.chat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ClientApplication {

    private static String username;
    private static boolean isOnline;

    public static void main(String[] args) {
        try (
                Socket socket = new Socket("localhost", 8089);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            System.out.println("Successful connection to server");
            isOnline = true;
            Scanner scanner = new Scanner(System.in);
            new Thread(() -> {
                try {
                    while (true) {
                        String message = in.readUTF();
                        if (message.startsWith("/")) {
                            if (message.startsWith("/authok ")) {
                                username = message.split(" ")[1];
                                break;
                            }
                        }
                        System.out.println(message);
                    }
                    while (true) {
                        String message = in.readUTF();
                        if (message.startsWith("/kicked")) {
                            isOnline = false;
                            System.out.println("You have been kicked from the server");
                            break;
                        }
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    System.out.println("You have been disconnected from the server");
                }
            }).start();
            while (isOnline) {
                // чтобы юзер выпал из этого цикла приходится всё равно 1 раз сделать ввод строки
                // как можно сделать так, чтобы его сразу кикало отсюда?
                String message = scanner.nextLine();
                out.writeUTF(message);
                if (message.equals("/exit")) {
                    break;
                }

            }
        } catch (IOException e) {
            System.out.println("You have been disconnected from the server");
        }
    }
}
