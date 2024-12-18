package ru.gordeev.chat.callback;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Deprecated
public class Network implements AutoCloseable {

    private Logger logger;
    private final ExecutorService threadPool;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Callback onMessageReceived;

    public Network() {
        this.logger = LogManager.getLogger(Network.class);
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void setOnMessageReceived(Callback onMessageReceived) {
        this.onMessageReceived = onMessageReceived;
    }

    public void connect(int port) throws IOException {
        this.socket = new Socket("localhost", port);
        this.out = new DataOutputStream(socket.getOutputStream());
        this.in = new DataInputStream(socket.getInputStream());

        try {
            threadPool.execute(() -> {
                try {
                    while (true) {
                        String messageFromServer = in.readUTF();
                        System.out.println(messageFromServer);
                    }
                } catch (IOException e) {
                    logger.error(e);
                }
            });
            while (true) {
                String message = in.readUTF();
                if (onMessageReceived != null) {
                    onMessageReceived.callback(message);
                }
            }
        } catch (IOException e) {
            logger.error(e);
        } finally {
            close();
        }
    }

    public void sendMessage(String message) throws IOException {
        out.writeUTF(message);
    }

    @Override
    public void close() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (IOException e) {
            logger.error(e);
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            logger.error(e);
        }

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            logger.error(e);
        }
    }

}
