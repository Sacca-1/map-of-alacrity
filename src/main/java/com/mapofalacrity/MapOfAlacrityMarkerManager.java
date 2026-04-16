package com.mapofalacrity;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MapOfAlacrityMarkerManager
{
	private static final int ICON_SIZE = 27;

	private final ShortcutLocationRepository repository;
	private final BufferedImage normalImage;

	private List<MapOfAlacrityPoint> points = List.of();

	@Inject
	public MapOfAlacrityMarkerManager(ShortcutLocationRepository repository)
	{
		this.repository = repository;
		this.normalImage = buildImage(new Color(255, 191, 0), new Color(34, 15, 15), new Color(255, 247, 180, 180));
	}

	public void rebuildPoints()
	{
		List<MapOfAlacrityPoint> rebuilt = new ArrayList<>();
		for (ShortcutLocation location : repository.getLocations())
		{
			rebuilt.add(new MapOfAlacrityPoint(location, normalImage));
		}
		this.points = List.copyOf(rebuilt);
	}

	public List<MapOfAlacrityPoint> getPoints()
	{
		return points;
	}

	private static BufferedImage buildImage(Color fill, Color outline, Color glow)
	{
		BufferedImage image = new BufferedImage(ICON_SIZE, ICON_SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(glow);
		graphics.fill(new Ellipse2D.Double(1, 1, ICON_SIZE - 2, ICON_SIZE - 2));
		graphics.setColor(fill);
		graphics.fill(new Ellipse2D.Double(3, 3, ICON_SIZE - 6, ICON_SIZE - 6));
		graphics.setStroke(new BasicStroke(2.2f));
		graphics.setColor(outline);
		graphics.draw(new Ellipse2D.Double(3, 3, ICON_SIZE - 6, ICON_SIZE - 6));
		graphics.setColor(new Color(255, 255, 255, 220));
		graphics.fill(new Ellipse2D.Double(6, 6, ICON_SIZE - 12, ICON_SIZE - 12));
		graphics.dispose();
		return image;
	}
}
