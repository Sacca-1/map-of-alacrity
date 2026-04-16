package com.mapofalacrity;

import java.util.Arrays;
import java.util.Locale;

public enum TeleportRegion
{
	ASGARNIA("Asgarnia", 65538),
	DESERT("Desert", 131074),
	FREMENNIK("Fremennik", 196610),
	KANDARIN("Kandarin", 262146),
	KARAMJA("Karamja", 327682),
	KOUREND("Kourend", 393218),
	MORYTANIA("Morytania", 458754),
	TIRANNWN("Tirannwn", 524290),
	VARLAMORE("Varlamore", 589826),
	WILDERNESS("Wilderness", 655362);

	private final String displayName;
	private final int menuIdentifier;
	private final String normalizedName;

	TeleportRegion(String displayName, int menuIdentifier)
	{
		this.displayName = displayName;
		this.menuIdentifier = menuIdentifier;
		this.normalizedName = normalize(displayName);
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public int getMenuIdentifier()
	{
		return menuIdentifier;
	}

	public static TeleportRegion fromDisplayName(String name)
	{
		final String normalized = normalize(name);
		return Arrays.stream(values())
			.filter(region -> region.normalizedName.equals(normalized))
			.findFirst()
			.orElseThrow(() -> new IllegalArgumentException("Unknown Map of Alacrity region: " + name));
	}

	public static String normalize(String value)
	{
		return value == null ? "" : value.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]+", " ").trim();
	}
}
