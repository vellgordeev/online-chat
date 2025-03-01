package ru.gordeev.chat.database.utils;

import java.sql.SQLException;

/**
 * A functional interface that allows throwing SQLException
 * so that we do not have to wrap all parameter-setting operations
 * in try-catch blocks when using PreparedStatement.
 *
 * @param <T> the type of object (e.g., PreparedStatement)
 */
@FunctionalInterface
public interface SqlConsumer<T> {
    void accept(T t) throws SQLException;
}
