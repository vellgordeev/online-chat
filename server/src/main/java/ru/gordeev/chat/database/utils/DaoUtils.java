package ru.gordeev.chat.database.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Utility class for JDBC operations that hides the repetitive code
 * of opening/closing Connection, PreparedStatement, and ResultSet.
 *
 * Methods:
 *  - queryForObject: For a SELECT returning one object (or null)
 *  - executeUpdate: For INSERT/UPDATE/DELETE
 */
public final class DaoUtils {

    private static final Logger logger = LogManager.getLogger(DaoUtils.class);

    private DaoUtils() {}

    /**
     * Executes a SELECT statement that should return one object, or null if no rows are found.
     *
     * @param dataSource  the DataSource (from your Hikari pool)
     * @param sql         the SQL query with placeholders
     * @param paramSetter a lambda for setting parameters on the PreparedStatement
     * @param extractor   a lambda for extracting the desired value from the ResultSet
     * @param <T>         the type of object to be returned
     * @return the extracted object or null if no rows were found
     */
    public static <T> T queryForObject(
            javax.sql.DataSource dataSource,
            String sql,
            SqlConsumer<PreparedStatement> paramSetter,
            ResultSetExtractor<T> extractor
    ) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (paramSetter != null) {
                paramSetter.accept(statement);
            }

            try (ResultSet rs = statement.executeQuery()) {
                return extractor.extract(rs);
            }

        } catch (SQLException e) {
            logger.error("Error in queryForObject: {}", sql, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes an INSERT/UPDATE/DELETE statement and returns the number of rows affected.
     *
     * @param dataSource  the DataSource (from your Hikari pool)
     * @param sql         the SQL statement
     * @param paramSetter a lambda for setting parameters on the PreparedStatement
     * @return the number of rows affected by the statement
     */
    public static int executeUpdate(
            javax.sql.DataSource dataSource,
            String sql,
            SqlConsumer<PreparedStatement> paramSetter
    ) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (paramSetter != null) {
                paramSetter.accept(statement);
            }

            return statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error in executeUpdate: {}", sql, e);
            throw new RuntimeException(e);
        }
    }
}
