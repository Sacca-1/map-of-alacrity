package com.mapofalacrity;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@Getter
@EqualsAndHashCode
public final class ShortcutLocation
{
	private final TeleportRegion region;
	private final String name;
	private final WorldPoint worldPoint;

	public ShortcutLocation(TeleportRegion region, String name, WorldPoint worldPoint)
	{
		this.region = region;
		this.name = name;
		this.worldPoint = worldPoint;
	}
}
