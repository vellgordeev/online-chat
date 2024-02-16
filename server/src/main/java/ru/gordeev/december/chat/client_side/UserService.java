package ru.gordeev.december.chat.client_side;

public interface UserService {

    String getUsernameByLoginAndPassword(String login, String password);

    boolean isUserAlreadyRegistered(String login, String username);

    boolean registerUser(String login, String password, String username);

    boolean changeUsername(String login, String newUsername);

    UserRole getUserRole(String username);

    String getUserLogin(String username);

}
