package ru.gordeev.chat.database.utils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A functional interface for extracting a value from a ResultSet.
 *
 * @param <T> the type of object to be extracted from the ResultSet
 */
@FunctionalInterface
public interface ResultSetExtractor<T> {
    T extract(ResultSet rs) throws SQLException;
}
