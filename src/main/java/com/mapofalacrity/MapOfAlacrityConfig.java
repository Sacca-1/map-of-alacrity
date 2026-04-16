package com.mapofalacrity;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("mapofalacrity")
public interface MapOfAlacrityConfig extends Config
{
	@ConfigItem(
		keyName = "closeMapAfterTeleport",
		name = "Close map after teleport",
		description = "Closes the world map after the plugin triggers a destination teleport"
	)
	default boolean closeMapAfterTeleport()
	{
		return true;
	}
}
