package ru.gordeev.december.chat.callback;

import java.io.IOException;
import java.util.Scanner;

public class ClientApplicationWithCallbacks {

    public static void main(String[] args) {
        try (Network network = new Network()) {
            Scanner scanner = new Scanner(System.in);
            network.connect(8189);
            System.out.println("Подключились к серверу");

            while (true) {
                String message = scanner.nextLine();
                network.sendMessage(message);
                network.setOnMessageReceived(arguments -> System.out.println((String) arguments[0]));
                if (message.equals("/exit")) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
