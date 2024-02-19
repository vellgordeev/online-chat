package ru.gordeev.chat.helpers;

public class ServerMessages {

    public static final String YOU_DONT_HAVE_RIGHTS = "Server: you don't have rights for this command";

    public static final String COULD_NOT_FIND_USER = "Server: couldn't find such user";

    private static final String INCORRECT_COMMAND_FORMAT = "Server: incorrect '%s' command format";

    public static String getIncorrectCommandFormatMessage(String command) {
        return String.format(INCORRECT_COMMAND_FORMAT, command);
    }
}
