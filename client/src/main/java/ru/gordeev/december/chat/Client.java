package ru.gordeev.december.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private final Logger logger;
    private String username;
    private boolean isOnline;

    public Client() {
        this.logger = LogManager.getLogger(Client.class);
    }

    public void start() {
        try (
                Socket socket = new Socket("localhost", 8089);
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream())
        ) {
            logger.info("Successful connection to server");
            isOnline = true;
            Scanner scanner = new Scanner(System.in);
            new Thread(() -> {
                try {
                    readMessagesFromServerAndPrintThem(in);
                    listenToBreakCommandsAndDoThem(in);
                } catch (IOException e) {
                    logger.warn("You have been disconnected from the server");
                }
            }).start();
            listenToUserInputAndSendItToServer(scanner, out);
        } catch (IOException e) {
            logger.warn("You have been disconnected from the server");
        }
    }

    private void listenToUserInputAndSendItToServer(Scanner scanner, DataOutputStream out) throws IOException {
        while (isOnline) {
            String message = scanner.nextLine();
            out.writeUTF(message);
            if (message.equals("/exit")) {
                break;
            }
        }
    }

    private void listenToBreakCommandsAndDoThem(DataInputStream in) throws IOException {
        while (true) {
            String message = in.readUTF();
            if (message.startsWith("/kicked")) {
                isOnline = false;
                logger.warn("You have been kicked from the server");
                break;
            }
            System.out.println(message);
        }
    }

    private void readMessagesFromServerAndPrintThem(DataInputStream in) throws IOException {
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
    }
}
