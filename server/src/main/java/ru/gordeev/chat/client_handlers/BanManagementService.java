package ru.gordeev.chat.client_handlers;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.gordeev.chat.database.DataBaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class BanManagementService {

    private Logger logger;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final String UPDATE_USERS = "UPDATE users SET is_banned = FALSE, ban_expiration = NULL WHERE ban_expiration <= NOW() AND is_banned = TRUE";

    public BanManagementService() {
        this.logger = LogManager.getLogger(BanManagementService.class);
    }

    public void startBanCheck() {
        Runnable checkBans = () -> {
            try (Connection connection = DataBaseConnection.getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement(UPDATE_USERS)) {
                if (statement.executeUpdate() > 0) {
                    logger.info("Ban statuses for expired bans has been updated");
                }
            } catch (SQLException e) {
                logger.error("Error while starting ban check service", e);
            }
        };

        scheduler.scheduleWithFixedDelay(checkBans, 0, 1, TimeUnit.MINUTES);
    }
}