package ru.gordeev.chat.client_handlers;

import java.util.Timer;
import java.util.TimerTask;

public class UserActivityWatcher {
    private Timer timer;
    private static final long INACTIVITY_LIMIT = 20L * 60L * 1000L;
    private final ClientHandler client;

    public UserActivityWatcher(ClientHandler client) {
        this.client = client;
        startInactivityTimer();
    }

    private void startInactivityTimer() {
        if (timer != null) {
            timer.cancel();
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                disconnectUserDueToInactivity();
            }
        }, INACTIVITY_LIMIT);
    }

    public void onUserActivity() {
        startInactivityTimer();
    }

    private void disconnectUserDueToInactivity() {
        client.executeInactiveCommand();
    }
}
