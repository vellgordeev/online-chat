package ru.gordeev.december.chat.database;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.gordeev.december.chat.client_side.UserRole;
import ru.gordeev.december.chat.client_side.UserService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public class PostgresUserService implements UserService {
    /* Таблица выглядит так (role - мой кастомный тип данных enum, бывает 'admin' или 'user'):

        CREATE TABLE users (
            id       SERIAL PRIMARY KEY,
            login    VARCHAR(255) NOT NULL,
            password TEXT         NOT NULL,
            username VARCHAR(255) NOT NULL,
            role     role         NOT NULL
        )

     */

    private final Logger logger;
    private static final String SELECT_USER_BY_LOGIN_AND_PASSWORD = "SELECT username FROM users WHERE login = ? AND password = crypt(?, password)";
    private static final String SELECT_USER_BY_LOGIN_OR_USERNAME = "SELECT id FROM users WHERE login = ? OR username = ?";
    private static final String INSERT_USER_BY_LOGIN_PASSWORD_USERNAME = "INSERT INTO users (login, password, username, role) " +
            "VALUES (?, crypt(?, gen_salt('bf')), ?, 'user')";
    private static final String SELECT_ROLE_BY_USERNAME = "SELECT role FROM users WHERE username = ?";

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
    public String getUsernameByLoginAndPassword(String login, String password) {
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
    public boolean isUserAlreadyRegistered(String login, String username) {
        Connection connection = getConnection();
        boolean isUserRegisted = false;
        try (PreparedStatement statement = connection.prepareStatement(SELECT_USER_BY_LOGIN_OR_USERNAME)) {
            statement.setString(1, login);
            statement.setString(2, username);
            ResultSet resultSet = statement.executeQuery();

            isUserRegisted = resultSet.next();
        } catch (SQLException e) {
            logger.error(e);
        } finally {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.error(e);
            }
        }
        return isUserRegisted;
    }

    @Override
    public boolean registerUser(String login, String password, String username) {
        Connection connection = getConnection();
        boolean isSuccess = false;
        try (PreparedStatement statement = connection.prepareStatement(INSERT_USER_BY_LOGIN_PASSWORD_USERNAME)) {
            if (isUserAlreadyRegistered(login, username)) {
                return false;
            }

            statement.setString(1, login);
            statement.setString(2, password);
            statement.setString(3, username);
            if (statement.executeUpdate() != 0) {
                isSuccess = true;
            }
        } catch (SQLException e) {
            logger.error(e);
        } finally {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.error(e);
            }
        }
        return isSuccess;
    }

    @Override
    public UserRole getUserRole(String username) {
        Connection connection = getConnection();
        UserRole userRole = null;
        try (PreparedStatement statement = connection.prepareStatement(SELECT_ROLE_BY_USERNAME)) {
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
            logger.error(e);
        } finally {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                logger.error(e);
            }
        }
        return Objects.requireNonNull(userRole);
    }
}
