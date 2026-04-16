package com.mapofalacrity;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class MapOfAlacrityPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(MapOfAlacrityPlugin.class);
		RuneLite.main(args);
	}
}
