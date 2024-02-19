package ru.gordeev.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    private final Logger logger;
    private ExecutorService executorService;
    private boolean isOnline;

    public Client() {
        this.logger = LogManager.getLogger(Client.class);
        this.executorService = Executors.newSingleThreadExecutor();
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
            executorService.execute(() -> {
                try {
                    readMessagesFromServer(in);
                } catch (IOException e) {
                    logger.warn(e);
                }
            });
            listenToUserInputAndSendItToServer(scanner, out);
        } catch (IOException e) {
            logger.warn("You have been disconnected from the server");
        }
    }

    private void listenToUserInputAndSendItToServer(Scanner scanner, DataOutputStream out) throws IOException {
        while (true) {
            String message = scanner.nextLine();
            if (message.equals("/exit") || !isOnline) {
                executorService.shutdown();
                break;
            }
            out.writeUTF(message);
        }
    }

    private void readMessagesFromServer(DataInputStream in) throws IOException {
        while (true) {
            String message = in.readUTF();
            if (message.contains("/kicked")) {
                isOnline = false;
                break;
            }
            if (message.contains("/inactive")) {
                isOnline = false;
                break;
            }
            if (message.contains("/banned")) {
                isOnline = false;
                break;
            }
            if (message.contains("/tempBanned ")) {
                isOnline = false;
                break;
            }
            if (message.contains("/shutdown")) {
                isOnline = false;
                break;
            }
            System.out.println(message);
        }
    }
}
