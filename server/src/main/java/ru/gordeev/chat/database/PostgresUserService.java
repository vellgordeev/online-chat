package ru.gordeev.chat.database;

import ru.gordeev.chat.database.utils.DaoUtils;
import ru.gordeev.chat.handlers.UserRole;
import ru.gordeev.chat.helpers.UserNotFoundException;

import java.util.Objects;

/**
 * An example of a DAO (Data Access Object) implementation for Postgres
 * that handles user-related queries. It uses DaoUtils methods for
 * executing JDBC statements and performing queries.
 */
public class PostgresUserService implements UserService {

    private static final String SELECT_USER_BY_LOGIN_AND_PASSWORD =
            "SELECT username FROM users WHERE login = ? AND password = crypt(?, password)";
    private static final String SELECT_USER_BY_LOGIN_OR_USERNAME =
            "SELECT id FROM users WHERE login = ? OR username = ?";
    private static final String INSERT_USER_BY_LOGIN_PASSWORD_USERNAME =
            "INSERT INTO users (login, password, username, role) VALUES (?, crypt(?, gen_salt('bf')), ?, 'user')";
    private static final String SELECT_ROLE_BY_USERNAME =
            "SELECT role FROM users WHERE username = ?";
    private static final String SELECT_LOGIN_BY_USERNAME =
            "SELECT login FROM users WHERE username = ?";
    private static final String UPDATE_USERNAME =
            "UPDATE users SET username = ? WHERE login = ?";
    private static final String BAN_USER_WITHOUT_DATE =
            "UPDATE users SET is_banned = TRUE, ban_expiration = NULL WHERE username = ?";
    private static final String UNBAN_USER =
            "UPDATE users SET is_banned = FALSE, ban_expiration = NULL WHERE username = ?";
    private static final String SELECT_USER_BAN_STATE_BY_USERNAME =
            "SELECT is_banned FROM users WHERE username = ?";


    /**
     * Retrieves the Hikari DataSource connection pool.
     *
     * @return the DataSource object
     */
    private javax.sql.DataSource getDataSource() {
        return DataBaseConnection.getDataSource();
    }

    @Override
    public synchronized String getUsernameByLoginAndPassword(String login, String password) {
        return DaoUtils.queryForObject(
                getDataSource(),
                SELECT_USER_BY_LOGIN_AND_PASSWORD,
                st -> {
                    st.setString(1, login);
                    st.setString(2, password);
                },
                rs -> {
                    if (rs.next()) {
                        return rs.getString("username");
                    }
                    return null;
                }
        );
    }

    @Override
    public synchronized boolean isUserAlreadyRegistered(String login, String username) {
        Integer userId = DaoUtils.queryForObject(
                getDataSource(),
                SELECT_USER_BY_LOGIN_OR_USERNAME,
                st -> {
                    st.setString(1, login);
                    st.setString(2, username);
                },
                rs -> rs.next() ? rs.getInt("id") : null
        );
        return userId != null;
    }

    @Override
    public synchronized boolean registerUser(String login, String password, String newUsername) {
        if (isUserAlreadyRegistered(login, newUsername)) {
            return false;
        }
        int rows = DaoUtils.executeUpdate(
                getDataSource(),
                INSERT_USER_BY_LOGIN_PASSWORD_USERNAME,
                st -> {
                    st.setString(1, login);
                    st.setString(2, password);
                    st.setString(3, newUsername);
                }
        );
        return rows > 0;
    }

    @Override
    public synchronized boolean changeUsername(String login, String newUsername) {
        int rows = DaoUtils.executeUpdate(
                getDataSource(),
                UPDATE_USERNAME,
                st -> {
                    st.setString(1, newUsername);
                    st.setString(2, login);
                }
        );
        return rows > 0;
    }

    @Override
    public synchronized UserRole getUserRole(String username) {
        UserRole role = DaoUtils.queryForObject(
                getDataSource(),
                SELECT_ROLE_BY_USERNAME,
                st -> st.setString(1, username),
                rs -> {
                    if (rs.next()) {
                        String r = rs.getString("role");
                        if ("USER".equalsIgnoreCase(r)) return UserRole.USER;
                        if ("ADMIN".equalsIgnoreCase(r)) return UserRole.ADMIN;
                    }
                    return null;
                }
        );
        return Objects.requireNonNull(role);
    }

    @Override
    public synchronized String getUserLogin(String username) {
        String login = DaoUtils.queryForObject(
                getDataSource(),
                SELECT_LOGIN_BY_USERNAME,
                st -> st.setString(1, username),
                rs -> {
                    if (rs.next()) {
                        return rs.getString("login");
                    }
                    return null;
                }
        );
        return Objects.requireNonNull(login);
    }

    @Override
    public synchronized boolean setBan(String username) {
        int rows = DaoUtils.executeUpdate(
                getDataSource(),
                BAN_USER_WITHOUT_DATE,
                st -> st.setString(1, username)
        );
        return rows > 0;
    }

    @Override
    public synchronized boolean setBan(String username, Integer durationMinutes) {
        String sql = "UPDATE users SET is_banned = TRUE, ban_expiration = (NOW() + INTERVAL '" + durationMinutes + " minutes') WHERE username = ?";
        int rows = DaoUtils.executeUpdate(
                getDataSource(),
                sql,
                st -> st.setString(1, username)
        );
        return rows > 0;
    }

    @Override
    public synchronized boolean unsetBan(String username) {
        int rows = DaoUtils.executeUpdate(
                getDataSource(),
                UNBAN_USER,
                st -> st.setString(1, username)
        );
        return rows > 0;
    }

    @Override
    public synchronized boolean isBanned(String username) throws UserNotFoundException {
        Boolean banned = DaoUtils.queryForObject(
                getDataSource(),
                SELECT_USER_BAN_STATE_BY_USERNAME,
                st -> st.setString(1, username),
                rs -> {
                    if (rs.next()) {
                        return rs.getBoolean("is_banned");
                    }
                    return null; // user not found
                }
        );
        if (banned == null) {
            throw new UserNotFoundException("User not found: " + username);
        }
        return banned;
    }
}
