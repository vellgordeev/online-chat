package ru.gordeev.december.chat;

import java.util.*;

public class InMemoryUserService implements UserService {

    record User(String login, String password, String username) { }

    private List<User> users;

    public InMemoryUserService() {
        this.users = new ArrayList<>(Arrays.asList(
                new User("login1", "pass1", "John"),
                new User("login2", "pass2", "Vell"),
                new User("login3", "pass3", "Max")
        ));
    }

    @Override
    public String getUserByLoginAndPassword(String login, String password) {
        for (User user : users) {
            if (user.login.equals(login) && user.password.equals(password)) {
                return user.username;
            }
        }
        return null;
    }

    @Override
    public boolean registerUser(String login, String password, String username) {
        if (!isUserAlreadyRegistered(login, username)) {
            users.add(new User(login, password, username));
            return true;
        }
        return false;
    }

    @Override
    public boolean isUserAlreadyRegistered(String login, String username) {
        for (User user : users) {
            if (user.login.equals(login) || user.username.equals(username)) {
                return true;
            }
        }
        return false;
    }
}

