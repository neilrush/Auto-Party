package com.autoparty;

import javax.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.party.PartyService;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.events.PluginChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.party.PartyPlugin;

@Slf4j
@PluginDescriptor(name = "Auto Party", description = "Automatically rejoins the last party upon login.", tags = {"party", "automatic", "rejoin", "group"})
@PluginDependency(PartyPlugin.class)
public class AutoPartyPlugin extends Plugin
{
	private static final String PARTY_CONFIG_GROUP = "party";
	private static final String PARTY_PREVIOUS_ID_KEY = "previousPartyId";

	@Inject
	private PartyService partyService;

	@Inject
	private ConfigManager configManager;

	@Inject
	private AutoPartyConfig partyConfig;

	@Inject
	private PluginManager pluginManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	private Boolean loggedInOnce = false;

	/**
	 * Check if the party plugin is enabled when Auto Party starts. If the Party plugin is not
	 * enabled, Auto Party will disable itself and log a warning.
	 */
	@Override
	protected void startUp()
	{
		// look for the party plugin
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (plugin instanceof PartyPlugin)
			{
				// Once we find it, check if it's currently enabled by the user
				if (pluginManager.isPluginEnabled(plugin))
				{
					log.debug("Party plugin is enabled. Auto Party started!");
				}
				else
				{
					log.debug(
						"Party plugin is required for Auto Party to function. Disabling Auto Party.");
					pluginManager.setPluginEnabled(this, false);
				}
				break; // Exit the loop once found
			}
		}
	}

	/**
	 * if the plugin is disabled clear the flag that indicates the user left the last party. maybe
	 * do this with onPluginchanged instead?
	 */
	@Override
	protected void shutDown()
	{
		// clear the left last party flag as we will now possibly lose track
		partyConfig.setJoinedParty(false);
		log.debug("Auto Party stopped!");
	}

	/**
	 * make sure the party plugin is enabled on pluginchange and if it is not disable this plugin
	 */
	@Subscribe
	public void onPluginChanged(PluginChanged pluginChanged)
	{
		// get the plugin that was changed and check if it is the Party plugin
		Plugin plugin = pluginChanged.getPlugin();
		if (plugin != null && plugin instanceof PartyPlugin)
		{
			if (!pluginManager.isPluginEnabled(plugin))
			{
				log.debug(
					"Party plugin is required for Auto Party to function. Disabling Auto Party.");
				pluginManager.setPluginEnabled(this, false);
			}
		}
	}

	/**
	 * Wait for login gamestate before rejoining the party. send a chat console message to
	 * indicate the party was rejoined.
	 *
	 * @param gameStateChanged The event object containing the new game state.
	 */
	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		// Attempt to rejoin party once the player is logged in
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
		{
			if (!partyService.isInParty() && loggedInOnce == false && partyConfig.joinedParty())
			{
				partyService.changeParty(getLastPartyID());
				sendPartyJoinedMessage();
				loggedInOnce = true; // set flag so this only happens on the first login after
									// client startup
				log.info("Joined Last used party after login");
			}
		}
	}

	/**
	 * Track if the user has joined or left a party.
	 * stored in config to persist over client restarts.
	 */
	@Subscribe
	public void onPartyChanged(PartyChanged event)
	{
		// When the party changes (join, leave, create), update our flags
		if (event.getPartyId() != null)
		{
			// A new party was joined or created
			// User is in a party, so clear the "left" flag to allow auto-rejoin next time.
			partyConfig.setJoinedParty(true);
		}
		else if (!partyService.isInParty())
		{
			// The player has left the party, set the flag to prevent rejoining on next
			// login.
			partyConfig.setJoinedParty(false);
			log.debug("User left the party, will not auto-rejoin next time. setting flag.");
		}
	}

	/**
	 * Notifies the user that they have rejoined the last party.
	 */
	private void sendPartyJoinedMessage()
	{
		String message;
		if (partyConfig.showPassphrase())
		{
			message = new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT).append("Auto Party: Last party rejoined.")
				.append("\n").append(ChatColorType.HIGHLIGHT).append("Passphrase: ").append(getLastPartyID()).build();
		}
		else
		{
			message = new ChatMessageBuilder().append(ChatColorType.HIGHLIGHT).append("Auto Party: Last party rejoined.").build();
		}

		// send a message with the chat message manager that the user has rejoined the last party
		chatMessageManager.queue(QueuedMessage.builder().type(ChatMessageType.CONSOLE).runeLiteFormattedMessage(message).build());


	}

	/**
	 * Retrieves the last used partyID from the official Party plugin's config.
	 *
	 * @return The last party ID as a string, or null if not found.
	 */
	private String getLastPartyID()
	{
		return configManager.getConfiguration(PARTY_CONFIG_GROUP, PARTY_PREVIOUS_ID_KEY);
	}

	@Provides
	AutoPartyConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AutoPartyConfig.class);
	}
}
