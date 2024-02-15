package ru.gordeev.december.chat;

public interface UserService {

    String getUsernameByLoginAndPassword(String login, String password);

    boolean isUserAlreadyRegistered(String login, String username);

    boolean registerUser(String login, String password, String username);

    UserRole getUserRole(String username);

}
