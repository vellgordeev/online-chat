package ru.gordeev.chat;

import org.flywaydb.core.Flyway;

public class ServerApplication {

    public static void main(String[] args) {
        Flyway flyway = Flyway.configure()
                .dataSource("jdbc:postgresql://localhost:5432/postgres",
                        System.getenv("database.user"),
                        System.getenv("database.password"))
                .load();

        flyway.migrate();

        Server server = new Server(8089);

        server.start();
    }
}
