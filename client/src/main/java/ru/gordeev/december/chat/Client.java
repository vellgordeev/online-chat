package ru.gordeev.december.chat;

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
    private final ExecutorService threadPool;
    private boolean isOnline;

    public Client() {
        this.logger = LogManager.getLogger(Client.class);
        this.threadPool = Executors.newCachedThreadPool();
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
            threadPool.execute(() -> {
                try {
                    readMessagesFromServerAndPrintThem(in);
                    listenToBreakCommandsAndDoThem(in);
                } catch (IOException e) {
                    logger.warn("You have been disconnected from the server");
                }
            });
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
            System.out.println(message);
        }
    }
}
