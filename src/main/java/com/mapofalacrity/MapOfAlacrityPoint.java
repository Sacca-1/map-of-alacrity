package com.mapofalacrity;

import java.awt.image.BufferedImage;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.worldmap.WorldMapPoint;

@Getter
public class MapOfAlacrityPoint extends WorldMapPoint
{
	private final ShortcutLocation location;

	MapOfAlacrityPoint(ShortcutLocation location, BufferedImage normalImage)
	{
		super(location.getWorldPoint(), normalImage);
		this.location = location;
		setName(location.getName());
		setTooltip(location.getName() + "<br>" + location.getRegion().getDisplayName());
	}

	WorldPoint getPoint()
	{
		return getWorldPoint();
	}
}
