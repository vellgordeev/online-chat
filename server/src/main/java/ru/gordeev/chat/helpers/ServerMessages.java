package ru.gordeev.chat.helpers;

public class ServerMessages {

    private ServerMessages() {}

    public static final String YOU_DONT_HAVE_RIGHTS = "Server: you don't have rights for this command";

    public static final String COULD_NOT_FIND_USER = "Server: couldn't find such user";

    public static final String NEW_USER_HELP = """
            /register {login} {password} {username} – registration
            /auth {login} {password} – authentication
            """;

    public static final String SERVER_COMMANDS = """
            Commands list (put a '/' before the name):
            - register {login} {password} {username} – registration
            - auth {login} {password} – authentication
            - w {username} – private message
            - exit – exit (for client)
            - shutdown – stop the server (for admin)
            - ban – ban user
            - ban {time in minutes} – ban user for some time
            - activelist – active clients list
            - changenick – change nickname (for admin)
            """;

    private static final String INCORRECT_COMMAND_FORMAT = "Server: incorrect '%s' command format";

    public static String getIncorrectCommandFormatMessage(String command) {
        return String.format(INCORRECT_COMMAND_FORMAT, command);
    }

}
