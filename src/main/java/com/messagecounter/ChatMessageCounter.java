package com.messagecounter;

import com.google.inject.Provides;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
		name = "Chat Message Tracker"
)
public class ChatMessageCounter extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ChatMessageCounterConfig config;

	private static final Set<ChatMessageType> SKIP_MESSAGE_TYPES = EnumSet.of(
			ChatMessageType.AUTOTYPER,
			ChatMessageType.FRIENDNOTIFICATION,
			ChatMessageType.BROADCAST,
			ChatMessageType.FRIENDSCHATNOTIFICATION,
			ChatMessageType.CLAN_MESSAGE,
			ChatMessageType.TRADE,
			ChatMessageType.TRADEREQ,
			ChatMessageType.SPAM,
			ChatMessageType.WELCOME,
			ChatMessageType.CLAN_GUEST_MESSAGE);

	private Connection connect() throws SQLException {
		String url = "jdbc:postgresql://" + config.databaseAddress() + ":" + config.databasePort() + "/" + config.databaseName();
		return DriverManager.getConnection(url, config.username(), config.password());
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage) {

		// Exit early if the configuration values are missing
		if (isConfigInvalid()) {
			log.warn("Configuration is missing. Skipping message processing.");
			return;
		}

		// Exit early if the message type should be skipped
		if (SKIP_MESSAGE_TYPES.contains(chatMessage.getType())) {
			log.debug("Skipping message type: " + chatMessage.getType());
			return;
		}

		Optional<String> sender = Optional.ofNullable(chatMessage.getSender());

		if (config.allChats() || sender.map(config.chat()::equals).orElse(false)) {
			String rsn = processRsn(chatMessage.getName());

			// Run the database update asynchronously
			CompletableFuture.runAsync(() -> updateDatabase(rsn, sender.orElse(chatMessage.getType().toString())));
		}
	}

	private boolean isConfigInvalid() {
		return config.databaseAddress() == null ||
				config.username() == null ||
				config.password() == null ||
				config.databasePort() == null ||
				config.databaseName() == null;
	}

	private String processRsn(String rsn) {
		// Replace any non-ASCII characters with an underscore
		rsn = rsn.replaceAll("[^\\p{ASCII}]", "_");

		// Further process the RSN as needed (e.g., remove image tags)
		return rsn.replaceAll("<img=.*?>", "");
	}

	private void updateDatabase(String rsn, String chatname) {
		if (rsn == null || rsn.isEmpty()) {
			log.error("Attempted to update database with null or empty RSN. Chatname: " + chatname);
			return;
		}

		try (Connection conn = connect()) {
			conn.setAutoCommit(false); // Begin transaction

			// Lock the row by using SELECT FOR UPDATE
			try (PreparedStatement selectStmt = conn.prepareStatement(
					"SELECT messagecount FROM lastmessage WHERE rsn = ? AND chatname = ? FOR UPDATE")) {
				selectStmt.setString(1, rsn);
				selectStmt.setString(2, chatname);
				ResultSet rs = selectStmt.executeQuery();

				if (rs.next()) {
					int messageCount = rs.getInt("messagecount") + 1;
					try (PreparedStatement updateStmt = conn.prepareStatement(
							"UPDATE lastmessage SET messagecount = ?, lastmessage = ? WHERE rsn = ? AND chatname = ?")) {
						updateStmt.setInt(1, messageCount);
						updateStmt.setTimestamp(2, Timestamp.from(Instant.now()));
						updateStmt.setString(3, rsn);
						updateStmt.setString(4, chatname);
						updateStmt.executeUpdate();
					}
				} else {
					try (PreparedStatement insertStmt = conn.prepareStatement(
							"INSERT INTO lastmessage (rsn, chatname, messagecount, lastmessage) VALUES (?, ?, ?, ?)")) {
						insertStmt.setString(1, rsn);
						insertStmt.setString(2, chatname);
						insertStmt.setInt(3, 1);
						insertStmt.setTimestamp(4, Timestamp.from(Instant.now()));
						insertStmt.executeUpdate();
					}
				}

				conn.commit(); // Commit transaction
			} catch (SQLException e) {
				conn.rollback(); // Rollback transaction on error
				log.error("Database update error", e);
			}
		} catch (SQLException e) {
			log.error("Database connection error", e);
		}
	}


	@Provides
	ChatMessageCounterConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(ChatMessageCounterConfig.class);
	}
}
