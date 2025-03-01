package ru.gordeev.chat.database;

import ru.gordeev.chat.handlers.UserRole;
import ru.gordeev.chat.helpers.UserNotFoundException;

public interface UserService {

    String getUsernameByLoginAndPassword(String login, String password);

    boolean isUserAlreadyRegistered(String login, String username);

    boolean registerUser(String login, String password, String username);

    boolean changeUsername(String login, String newUsername);

    UserRole getUserRole(String username);

    String getUserLogin(String username);

    boolean setBan(String username, Integer durationMinutes);

    boolean setBan(String username);

    boolean unsetBan(String username);

    boolean isBanned(String username) throws UserNotFoundException;
}
