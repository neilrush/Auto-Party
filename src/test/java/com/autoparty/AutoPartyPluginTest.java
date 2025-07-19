package com.autoparty;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AutoPartyPluginTest {
	public static void main(String[] args) throws Exception {
		ExternalPluginManager.loadBuiltin(AutoPartyPlugin.class);
		RuneLite.main(args);
	}
}
