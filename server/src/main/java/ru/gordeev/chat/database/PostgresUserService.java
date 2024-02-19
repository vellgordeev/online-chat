package ru.gordeev.chat.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.gordeev.chat.client_handlers.UserRole;
import ru.gordeev.chat.client_handlers.UserService;
import ru.gordeev.chat.helpers.UserNotFoundException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class PostgresUserService implements UserService {
    /* Таблица выглядит так (role - мой кастомный тип данных enum, бывает 'admin' или 'user'):

        CREATE TABLE users (
            id              SERIAL PRIMARY KEY,
            login           VARCHAR(255)       NOT NULL,
            password        TEXT               NOT NULL,
            username        VARCHAR(255)       NOT NULL,
            role            role               NOT NULL,
            is_banned       BOOLEAN            NOT NULL,
            ban_expiration  TIMESTAMP,         NOT NULL
        )

     */

    private final Logger logger;
    private static final String SELECT_USER_BY_LOGIN_AND_PASSWORD = "SELECT username FROM users WHERE login = ? AND password = crypt(?, password)";
    private static final String SELECT_USER_BY_LOGIN_OR_USERNAME = "SELECT id FROM users WHERE login = ? OR username = ?";
    private static final String INSERT_USER_BY_LOGIN_PASSWORD_USERNAME = "INSERT INTO users (login, password, username, role) " +
            "VALUES (?, crypt(?, gen_salt('bf')), ?, 'user')";
    private static final String SELECT_ROLE_BY_USERNAME = "SELECT role FROM users WHERE username = ?";
    private static final String SELECT_LOGIN_BY_USERNAME = "SELECT login FROM users WHERE username = ?";
    private static final String UPDATE_USERNAME = "UPDATE users SET username = ? WHERE login = ?";
    private static final String BAN_USER_WITHOUT_DATE = "UPDATE users SET is_banned = TRUE, ban_expiration = NULL WHERE username = ?";
    private static final String UNBAN_USER = "UPDATE users SET is_banned = FALSE, ban_expiration = NULL WHERE username = ?";
    private static final String SELECT_USER_BAN_STATE_BY_USERNAME = "SELECT is_banned FROM users WHERE username = ?";

    public PostgresUserService() {
        this.logger = LogManager.getLogger(PostgresUserService.class.getName());
    }

    private Connection getConnection() {
        try {
            return DataBaseConnection.getDataSource().getConnection();
        } catch (SQLException e) {
            logger.error("Failed to get database connection", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized String getUsernameByLoginAndPassword(String login, String password) {
        String username = null;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_USER_BY_LOGIN_AND_PASSWORD)) {
            statement.setString(1, login);
            statement.setString(2, password);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                username = resultSet.getString(1);
            }
        } catch (SQLException e) {
            logger.error(e);
        }
        return username;
    }

    @Override
    public synchronized boolean isUserAlreadyRegistered(String login, String username) {
        boolean isUserRegister = false;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_USER_BY_LOGIN_OR_USERNAME)) {
            statement.setString(1, login);
            statement.setString(2, username);
            ResultSet resultSet = statement.executeQuery();

            isUserRegister = resultSet.next();
        } catch (SQLException e) {
            logger.error(e);
        }
        return isUserRegister;
    }

    @Override
    public synchronized boolean registerUser(String login, String password, String username) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_USER_BY_LOGIN_PASSWORD_USERNAME)) {
            if (isUserAlreadyRegistered(login, username)) {
                return false;
            }
            statement.setString(1, login);
            statement.setString(2, password);
            statement.setString(3, username);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error(e);
        }
        return false;
    }

    @Override
    public synchronized boolean changeUsername(String login, String newUsername) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_USERNAME)) {
            statement.setString(1, newUsername);
            statement.setString(2, login);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error(String.format("Postgres error while changing username for user %s", login), e);
        }
        return false;
    }

    @Override
    public synchronized UserRole getUserRole(String username) {
        UserRole userRole = null;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_ROLE_BY_USERNAME)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                String string = resultSet.getString(1);

                if (string.equals("user")) {
                    userRole = UserRole.USER;
                }
                if (string.equals("admin")) {
                    userRole = UserRole.ADMIN;
                }
            }
        } catch (SQLException e) {
            logger.error(String.format("Postgres error while getting role for user %s", username), e);
        }
        return Objects.requireNonNull(userRole);
    }

    @Override
    public synchronized String getUserLogin(String username) {
        String login = null;
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_LOGIN_BY_USERNAME)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                login = resultSet.getString(1);
            }
        } catch (SQLException e) {
            logger.error(String.format("Postgres error while getting login for user %s", username), e);
        }
        return Objects.requireNonNull(login);
    }

    @Override
    public synchronized boolean setBan(String username) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(BAN_USER_WITHOUT_DATE)) {
            statement.setString(1, username);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error(String.format("Postgres error while setting ban for user %s", username), e);
            return false;
        }
    }

    @Override
    public synchronized boolean setBan(String username, Integer durationMinutes) {
        String sqlStatement = "UPDATE users SET is_banned = TRUE, ban_expiration = (NOW() + INTERVAL '" + durationMinutes + " minutes') WHERE username = ?";

        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(sqlStatement)) {
            statement.setString(1, username);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error(String.format("Postgres error while setting ban for user %s", username), e);
            return false;
        }
    }

    @Override
    public synchronized boolean unsetBan(String username) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(UNBAN_USER)) {
            statement.setString(1, username);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            logger.error(String.format("Postgres error while setting ban for user %s", username), e);
            return false;
        }
    }

    @Override
    public synchronized boolean isBanned(String username) throws UserNotFoundException {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_USER_BAN_STATE_BY_USERNAME)) {
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getBoolean(1);
            } else {
                throw new UserNotFoundException("User not found: " + username);
            }
        } catch (SQLException e) {
            logger.error(String.format("Postgres error while checking ban for user %s", username), e);
        }
        return false;
    }
}
