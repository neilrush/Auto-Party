package com.autoparty;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("autoparty")
public interface AutoPartyConfig extends Config
{
    @ConfigItem(
        keyName = "showPassphrase",
        name = "Show Passphrase on Login",
        description = "Shows the party passphrase in the chat when the user logs in.",
        hidden = false)
    default boolean showPassphrase()
    {
        return false;
    }
    @ConfigItem(
        keyName = "joinedParty",
        name = "Joined Party",
        description = "Has the user joined a party while the plugin was enabled.",
        hidden = true)
    default boolean joinedParty()
    {
        return false;
    }

    @ConfigItem(
        keyName = "joinedParty",
        name = "Joined Party",
        description = "Has the user joined a party while the plugin was enabled.",
        hidden = true)
    void setJoinedParty(boolean joinedParty);
}

