package ru.gordeev.december.chat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InMemoryUserService implements UserService {

    record User(String login, String password, String username, UserRole userRole) { }

    private List<User> users;

    public InMemoryUserService() {
        this.users = new ArrayList<>(Arrays.asList(
                new User("admin", "admin", "Administrator", UserRole.ADMIN),
                new User("login1", "pass1", "John", UserRole.USER),
                new User("login2", "pass2", "Vell", UserRole.USER),
                new User("login3", "pass3", "Max", UserRole.USER)
        ));
    }

    @Override
    public String getUsernameByLoginAndPassword(String login, String password) {
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
            users.add(new User(login, password, username, UserRole.USER));
            return true;
        }
        return false;
    }

    @Override
    public UserRole getUserRole(String username) {
        for (User user : users) {
            if (user.username.equals(username)) {
                return user.userRole;
            }
        }
        return null;
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

