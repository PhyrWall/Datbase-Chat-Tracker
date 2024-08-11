package com.messagecounter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("friendMessageTracker")
public interface ChatMessageCounterConfig extends Config
{
	@ConfigSection(
			name = "Database",
			description = "Settings related to the database connection",
			position = 0,
			closedByDefault = true
	)
	String databaseSection = "database";

	@ConfigItem(
			keyName = "database_address",
			name = "Database Address",
			description = "IP Address for database",
			secret = true,
			position = 1,
			section = databaseSection
	)
	default String databaseAddress()
	{
		return null;
	}

	@ConfigItem(
			keyName = "database_port",
			name = "Database Port",
			description = "Port for the database connection",
			secret = true,
			position = 2,
			section = databaseSection
	)
	default String databasePort()
	{
		return "5432"; // Default port for PostgreSQL
	}

	@ConfigItem(
			keyName = "database_name",
			name = "Database Name",
			description = "Database to submit data to",
			position = 3,
			section = databaseSection
	)
	default String databaseName()
	{
		return null;
	}

	@ConfigItem(
			keyName = "username",
			name = "Username",
			description = "Username for the database connection",
			position = 4,
			secret = true,
			section = databaseSection
	)
	default String username()
	{
		return null;
	}

	@ConfigItem(
			keyName = "password",
			name = "Password",
			description = "Password for the database connection",
			secret = true,
			section = databaseSection,
			position = 5
	)
	default String password()
	{
		return null;
	}

	@ConfigItem(
			keyName = "chat",
			name = "Chat to Track",
			description = "Chat to track",
			position = 2,
			hidden = true // Hide this field when the condition is met
	)
	default String chat()
	{
		return null;
	}

	@ConfigItem(
			keyName = "allChat",
			name = "Track all chats",
			description = "Track all chats (Friends Chat, Clan Chat)",
			position = 1
	)
	default boolean allChats()
	{
		return false;
	}
}
