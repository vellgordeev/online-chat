# Console Chat Application

Welcome to the **Console Chat** project! This repository showcases a multi-user, console-based chat server and its 
corresponding client application. The project demonstrates various skills including networking, concurrency, 
PostgreSQL integration :elephant:, password hashing with pgcrypto, Docker Compose usage for database setup, Flyway migrations to automate schema and more. :tada:

Below you will find a comprehensive guide on how to set up, run, and use this chat application. 
Enjoy your reading! :star2:

---

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Server Side](#server-side)
- [Client Side](#client-side)
- [Available Commands](#available-commands)
- [Database & Docker Compose](#database--docker-compose)
- [How to Run](#how-to-run)
- [Security & Password Hashing](#security--password-hashing)
- [Credits and Acknowledgments](#credits-and-acknowledgments)

---

## Overview

This application is a console-based chat system built with **Java**. It allows multiple clients to connect to a single server, exchange messages, and use special commands for administrative tasks like banning, kicking :warning:, and shutting down the server. :speech_balloon:

**Key highlights**:
- Written in Java, using sockets (`ServerSocket` / `Socket`) for communication.
- Incorporates concurrency (each connected user runs in a separate thread).
- Stores user data in a **PostgreSQL** database, which is automatically launched via **Docker Compose**.
- Passwords are **hashed** (using PostgreSQL’s `pgcrypto` extension and `crypt()`) rather than stored in plain text.
- Supports automatic inactivity disconnection and basic ban/unban capabilities.
- Provides a set of chat commands (like `/kick`, `/ban`, `/help`, etc.) for users and administrators.

---

## Features

- **Multi-user support**: Multiple clients can connect to the server simultaneously. :busts_in_silhouette:
- **Command-driven**: Users can issue commands (e.g., `/help`, `/ban`, `/unban`) to perform specific actions.
- **Inactivity check**: Users inactive for a certain duration (default 20 minutes) are disconnected automatically.
- **Role-based access**: Ordinary users vs. admin privileges (admins can ban or kick users, shut down the server, etc.).
- **Password security**: Passwords are hashed with `crypt(...)` from the PostgreSQL `pgcrypto` extension. :lock:
- **Docker Compose**: Quickly spin up a PostgreSQL container, making it easy for anyone to run the application locally. :whale:

---

## Architecture
```text
                +--------------+
                |   Client     |
                | (console)    |
                +--------------+
                       | 
                       |  (TCP socket)
                       v
                +------------------+
                |     Server      |
                | - handles users |
                | - sends/receives|
                +--------+--------+
                         |
                         | (JDBC via HikariCP)
                         v
                +------------------+
                | PostgreSQL (DB) |
                | via DockerCompose|
                +------------------+
```
- **Server**

Created via new Server(port), listens on a TCP port.
For each incoming connection, spawns a ClientHandler.
Uses a scheduler to periodically disconnect inactive users.
Communicates with the PostgreSQL DB (via JDBC) for user information and ban status.

- **ClientHandler**

Represents a single connected user, handling commands and messages.
Updates lastActivityTime to track inactivity.
Authenticates or registers users against the database.

- **Client**

Runs locally on the user’s machine.
Reads console input, sends messages to the server, and listens for server responses in a background thread.

---

## Server Side

The core server class is **`Server`** (`ru.gordeev.chat.Server`).

- **`start()`**: Opens a `ServerSocket` on the specified port, creates a `ScheduledExecutorService` for `checkInactivity()`, and waits for client connections in a loop.  
- **`subscribe(ClientHandler)` / `unsubscribe(ClientHandler)`**: Manage the list of active client handlers.  
- **`broadcastMessage(...)`**: Sends a message to all connected clients.  
- **Ban/kick logic**: Methods such as `banUser(...)`, `unbanUser(...)`, and `kickUser(...)` either affect the in-memory clients or update the DB accordingly.  

**Inactivity**  
- The server runs `checkInactivity()` periodically, looking for clients who have been idle beyond the threshold (20 minutes by default).  
- Those clients are disconnected through `disconnectUserDueToInactivity(...)`.

---

## Client Side

The main client class is **`Client`** (`ru.gordeev.chat.Client`).

- **`start()`**:  
  1. Establishes a TCP connection to the server.  
  2. Spawns a thread to read messages from the server.  
  3. Waits for user input in the console loop, sending each line to the server via `DataOutputStream`.  

- Gracefully exits on `/exit` or if it detects certain shutdown signals from the server.

---

## Available Commands

Below is a summary of the commands you can type in the client’s console. Some commands require **admin** privileges:

| Command                                      | Description                                                                                               |
|----------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| **`/auth <login> <password>`**               | Authenticates an existing user.                                                                           |
| **`/register <login> <password> <username>`**| Registers a new user in the DB.                                                                           |
| **`/w <username> <message>`**                | Sends a private message to `<username>`.                                                                  |
| **`/exit`**                                  | Quits the chat application (client disconnects).                                                          |
| **`/help`**                                  | Displays the list of available commands.                                                                  |
| **`/ban <username> [<minutes>]`** (admin)    | Bans a user permanently or temporarily (if `<minutes>` is specified).                                     |
| **`/unban <username>`** (admin)              | Unbans the specified user.                                                                                |
| **`/kick <username>`** (admin)               | Kicks a user out of the server.                                                                           |
| **`/activelist`**                            | Shows the list of currently active users.                                                                 |
| **`/changenick <oldName> <newName>`** (admin)| Changes the username (the server also updates the DB).                                                    |
| **`/shutdown`** (admin)                      | Stops the server, disconnecting all users.                                                                |

---

## Database & Docker Compose

**PostgreSQL** is used to store user credentials, ban states, and user roles. This project uses `pgcrypto` for password hashing with `crypt(...)` and **bcrypt** salts. :lock:

### How the DB is started
- A `docker-compose.yml` file is included in the repository.  
- It pulls the official **Postgres** image.  
- **Important**: You must set environment variables for your database credentials. The application reads:
  ```java
  System.getenv("database.user");
  System.getenv("database.password");
    ```
Make sure these are set before running the server, or it won't have valid credentials to connect.
Once you have the correct environment variables set, simply run:
```bash
docker-compose up -d
```
This spins up the PostgreSQL container. The server can then connect to it using the host, port, DB name, user, and 
password you have specified.

### Flyway Migrations
On application startup, Flyway automatically runs migration scripts (in the configured `db/migration` folder). These 
scripts create or update the necessary tables and also ensure the `pgcrypto` extension is enabled. If you need to 
adjust the database schema, simply add or modify the SQL migration files, and Flyway will handle the rest.

### How to Run

1. **Clone the repository**:
   ```bash
   git clone https://github.com/vellgordeev/online-chat.git
   cd console-chat
    ```
2. **Start the DB (Docker Compose)**:
    ```bash
    docker-compose up -d
     ```
Ensure environment variables (POSTGRES_USER, POSTGRES_PASSWORD, POSTGRES_DB) match, and that in your Java code, you set:
  ```java
System.getenv("database.user");
System.getenv("database.password");
   ```
to the appropriate credentials.

3. **Compile and run the server**:
    ```bash
    mvn clean package
    java -jar target/chat-server.jar
    ```
Or run from your IDE. The server listens on localhost:8089 by default (or whatever port you specify).

4. **Start the client**:
    ```bash
    java -cp target/chat-server.jar ru.gordeev.chat.Client
    ```
Multiple clients can be started to simulate different users. They all connect to the same server.

5. **Login or Register**:
- In the client console, type **`/auth <login> <password>`** to log in an existing user.  
- Or **`/register <login> <password> <username>`** to create a new account.

6. **Try out some commands**:
- **`/help`** to list commands
- **`/ban <username>`** if you’re an admin
- **`/exit`** to quit the client

7. **Stop**:
- Client: type **`/exit`** in the console.
- Server: if you’re an admin, type **`/shutdown`** from your client session.

---

## Security & Password Hashing
All passwords are hashed using PostgreSQL’s `crypt()` function with `gen_salt('bf')` (bcrypt). 
This ensures you are never storing plain-text passwords in the database. :exclamation:

**Important**: With Flyway migrations, the `pgcrypto` extension is enabled automatically, assuming your database user has the necessary permissions.
If you run into permission issues, you may need to install/enable `pgcrypto` manually:
```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```
Otherwise, user authentication with hashed passwords will fail.

---

## Credits and Acknowledgments
- Docker and Docker Compose for containerized PostgreSQL. :whale:
- PostgreSQL pgcrypto for secure password hashing. :lock:
- Java concurrency (threads, scheduling) for handling multiple clients and inactivity checks.
- Log4j for logging.
- HikariCP for efficient database connection pooling. :rocket:

:wave: Thank you for checking out this Console Chat project! Feel free to explore, modify, and expand upon it to learn 
more about Java sockets, concurrency, and secure credential storage. If you have any questions or suggestions, please 
open an issue or reach out.

Enjoy chatting! :sparkles:
