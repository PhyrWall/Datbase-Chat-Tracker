# Chat Message Tracker

The Chat Message Tracker is a plugin designed to track and count chat messages in the game using the RuneLite API. This plugin interacts with a PostgreSQL database to store and update chat message counts per user.

## Features

- Tracks the number of messages sent by users.
- Supports filtering and counting specific types of chat messages.
- Asynchronously updates the database to avoid blocking the main game thread.
- Handles multiple users and prevents race conditions using transactions with row-level locking.

## Requirements

- PostgreSQL database.

## Database Setup

Before running the plugin, you need to set up a PostgreSQL database that the plugin will use to store chat message data.

## 1. Create the Database

First, create a PostgreSQL database for the plugin:

```sql
CREATE DATABASE chat_message_tracker;
CREATE TABLE lastmessage (
    id SERIAL PRIMARY KEY,
    rsn VARCHAR(255) NOT NULL,
    chatname VARCHAR(255) NOT NULL,
    messagecount INT NOT NULL,
    lastmessage TIMESTAMP NOT NULL,
    CONSTRAINT unique_rsn_chat UNIQUE (rsn, chatname)
);
```
## 2. Configure the Plugin

    Database Address: The IP address or hostname of your PostgreSQL server.
    Database Port: The port on which your PostgreSQL server is running (default is 5432).
    Database Name: The name of the database you created (e.g., chat_message_tracker).
    Username: The username for accessing the PostgreSQL database.
    Password: The password for the PostgreSQL user.

![image](https://github.com/user-attachments/assets/7199251c-7388-4311-a2e3-28d2fa6e74ed)


## 3. How It Works

The plugin listens for chat messages in the game. When a message is detected:

  - It checks the configuration to ensure all necessary details are provided.
  - It verifies if the message type should be tracked.
  - It processes the RuneScape name to ensure it's valid.
  - It asynchronously updates the database with the message count and timestamp, using transactions to avoid race conditions.

## 4. Search for Last Message on PostgreSQL

RSNs with spaces will be replaced with an underscore. For example, "Phyr Wall" will become "Phyr_wall".
To query the last message for a specific user:
```sql
SELECT *
FROM lastmessage
WHERE rsn = 'USERNAME';
```
You can replace PUBLICCHAT with the clan name or the friendschat identifier.The identifier is the text in the brackets [Pg]; this chatname would be Pg and is case-sensitive.
```sql
SELECT *
FROM lastmessage
WHERE rsn = 'rsn' AND chatname = 'PUBLICCHAT';
```
