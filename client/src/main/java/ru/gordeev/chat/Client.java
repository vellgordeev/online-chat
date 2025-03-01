package ru.gordeev.chat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

/**
 * Represents the chat client that connects to the server,
 * listens for incoming messages, and sends commands
 * or chat messages based on user input.
 */
public class Client {

    private final Logger logger;
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
            Thread readThread  = new Thread(() -> {
                try {
                    readMessagesFromServer(in);
                } catch (IOException e) {
                    logger.warn(e);
                }
            });
            readThread.start();
            listenToUserInputAndSendItToServer(scanner, out, readThread);
        } catch (IOException e) {
            logger.warn("You have been disconnected from the server");
        }
    }

    private void listenToUserInputAndSendItToServer(Scanner scanner, DataOutputStream out, Thread readThread) throws IOException {
        while (true) {
            String message = scanner.nextLine();
            if (message.equals("/exit") || !isOnline) {
                out.writeUTF("/exit");
                readThread.interrupt();
                break;
            }
            out.writeUTF(message);
        }
    }

    private void readMessagesFromServer(DataInputStream in) throws IOException {
        while (!Thread.currentThread().isInterrupted()) {
            String message = in.readUTF();
            if (message.contains("/kicked")
                    || message.contains("/inactive")
                    || message.contains("/banned")
                    || message.contains("/tempBanned")
                    || message.contains("/shutdown")) {
                isOnline = false;
                break;
            }
            System.out.println(message);
        }
    }
}
