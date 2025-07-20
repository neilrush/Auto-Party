/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2025, neilrush <https://github.com/neilrush/>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
import javax.swing.SwingUtilities;
import net.runelite.client.events.PartyChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDependency;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.party.PartyPlugin;

@Slf4j
@PluginDescriptor(
	name = "Auto Party",
	description = "Automatically rejoins the last party upon login.",
	tags = {"party", "automatic", "rejoin", "group"})
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

	@Override
	protected void startUp()
	{
		log.debug("Auto Party started!");
	}

	/**
	 * Checks if the Party plugin is enabled.
	 *
	 * @return true if the Party plugin is enabled, false otherwise.
	 */
	private boolean CheckForPartyPlugin()
	{
		boolean enabled = false;
		// look for the party plugin
		for (Plugin plugin : pluginManager.getPlugins())
		{
			if (plugin instanceof PartyPlugin)
			{
				// Once we find it, check if it's currently enabled by the user
				if (pluginManager.isPluginEnabled(plugin))
				{
					enabled = true;
				}
				break; // Exit the loop once found
			}
		}
		return enabled;
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
				if (!CheckForPartyPlugin())
				{
					log.info("Party plugin is not enabled, cannot rejoin party.");
					return;
				}
				else if (getLastPartyID() != null)
				{
					SwingUtilities.invokeLater(
						() -> partyService.changeParty(getLastPartyID())
					);
					log.debug("Attempting to rejoin last used party: {}", getLastPartyID());
					sendPartyJoinedMessage();
					loggedInOnce = true; // set flag so this only happens on the first login
					return;
				}
				else
				{
					// No last party ID found, reset the joinedParty flag
					partyConfig.setJoinedParty(false);
				}
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
