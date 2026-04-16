package com.mapofalacrity;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.Area;
import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.input.MouseListener;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Singleton
public class MapOfAlacrityWorldMapOverlay extends Overlay
	implements MouseListener
{
	private static final int TOOLTIP_OFFSET_HEIGHT = 25;
	private static final int TOOLTIP_OFFSET_WIDTH = 5;
	private static final int TOOLTIP_PADDING_HEIGHT = 2;
	private static final int TOOLTIP_PADDING_WIDTH = 4;
	private static final Splitter TOOLTIP_SPLITTER = Splitter.on("<br>").trimResults().omitEmptyStrings();

	private final Client client;
	private final MapOfAlacrityMarkerManager markerManager;
	private volatile Consumer<ShortcutLocation> pointClickConsumer = location -> { };

	private volatile MapOfAlacrityPoint hoveredPoint;

	@Inject
	public MapOfAlacrityWorldMapOverlay(
		Client client,
		MapOfAlacrityMarkerManager markerManager)
	{
		this.client = client;
		this.markerManager = markerManager;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(PRIORITY_HIGHEST);
		setLayer(OverlayLayer.MANUAL);
		drawAfterInterface(InterfaceID.WORLDMAP);
	}

	public void setPointClickConsumer(Consumer<ShortcutLocation> pointClickConsumer)
	{
		this.pointClickConsumer = pointClickConsumer == null ? location -> { } : pointClickConsumer;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!client.getWorldType().contains(WorldType.SEASONAL))
		{
			hoveredPoint = null;
			return null;
		}

		final List<MapOfAlacrityPoint> points = markerManager.getPoints();
		if (points.isEmpty())
		{
			return null;
		}

		Widget mapContainer = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		if (mapContainer == null || mapContainer.isHidden())
		{
			hoveredPoint = null;
			return null;
		}

		final Rectangle worldMapRectangle = mapContainer.getBounds();
		final Shape mapClip = getWorldMapClipArea(worldMapRectangle);
		final Shape previousClip = graphics.getClip();
		Point mousePosition = client.getMouseCanvasPosition();
		if (mousePosition == null || !mapClip.contains(mousePosition.getX(), mousePosition.getY()))
		{
			mousePosition = null;
		}

		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setClip(mapClip);
			MapOfAlacrityPoint hoveredThisFrame = findPointAt(mousePosition, points);
			MapOfAlacrityPoint tooltipPoint = hoveredThisFrame;

			for (MapOfAlacrityPoint point : points)
			{
				Point drawPoint = mapWorldPointToGraphicsPoint(point.getPoint());
				if (drawPoint == null)
				{
					continue;
				}

				int drawX = drawPoint.getX() - point.getImage().getWidth() / 2;
				int drawY = drawPoint.getY() - point.getImage().getHeight() / 2;

				graphics.drawImage(point.getImage(), drawX, drawY, null);

				if (point == hoveredThisFrame)
				{
					graphics.setColor(new Color(255, 255, 255, 180));
					graphics.setStroke(new BasicStroke(2f));
					graphics.drawOval(drawX - 2, drawY - 2, point.getImage().getWidth() + 4, point.getImage().getHeight() + 4);
				}
			}

			hoveredPoint = hoveredThisFrame;

			final Widget rsTooltip = client.getWidget(InterfaceID.Worldmap.TOOLTIP);
			if (rsTooltip != null)
			{
				rsTooltip.setHidden(tooltipPoint != null);
			}

			if (tooltipPoint != null && !Strings.isNullOrEmpty(tooltipPoint.getTooltip()))
			{
				drawTooltip(graphics, tooltipPoint);
			}
		}
		finally
		{
			graphics.setClip(previousClip);
		}

		return null;
	}

	@Override
	public MouseEvent mouseClicked(MouseEvent event)
	{
		MapOfAlacrityPoint clickedPoint = findPointAt(client.getMouseCanvasPosition(), markerManager.getPoints());
		hoveredPoint = null;
		if (event.getButton() == MouseEvent.BUTTON1 && clickedPoint != null)
		{
			pointClickConsumer.accept(clickedPoint.getLocation());
			return null;
		}
		return event;
	}

	@Override
	public MouseEvent mousePressed(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseReleased(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseEntered(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseExited(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseDragged(MouseEvent event)
	{
		return event;
	}

	@Override
	public MouseEvent mouseMoved(MouseEvent event)
	{
		return event;
	}

	private void drawTooltip(Graphics2D graphics, MapOfAlacrityPoint point)
	{
		Point drawPoint = mapWorldPointToGraphicsPoint(point.getPoint());
		if (drawPoint == null)
		{
			return;
		}

		List<String> rows = TOOLTIP_SPLITTER.splitToList(point.getTooltip());
		if (rows.isEmpty())
		{
			return;
		}

		drawPoint = new Point(drawPoint.getX() + TOOLTIP_OFFSET_WIDTH, drawPoint.getY() + TOOLTIP_OFFSET_HEIGHT);
		graphics.setFont(FontManager.getRunescapeFont());
		FontMetrics fontMetrics = graphics.getFontMetrics();
		int width = rows.stream().map(fontMetrics::stringWidth).max(Integer::compareTo).orElse(0);
		int height = fontMetrics.getHeight();
		Rectangle tooltipRect = new Rectangle(
			drawPoint.getX() - TOOLTIP_PADDING_WIDTH,
			drawPoint.getY() - TOOLTIP_PADDING_HEIGHT,
			width + TOOLTIP_PADDING_WIDTH * 2,
			height * rows.size() + TOOLTIP_PADDING_HEIGHT * 2);
		graphics.setColor(JagexColors.TOOLTIP_BACKGROUND);
		graphics.fillRect(tooltipRect.x, tooltipRect.y, tooltipRect.width, tooltipRect.height);
		graphics.setColor(JagexColors.TOOLTIP_BORDER);
		graphics.drawRect(tooltipRect.x, tooltipRect.y, tooltipRect.width, tooltipRect.height);
		graphics.setColor(JagexColors.TOOLTIP_TEXT);
		for (int i = 0; i < rows.size(); i++)
		{
			graphics.drawString(rows.get(i), drawPoint.getX(), drawPoint.getY() + ((i + 1) * height) - 2);
		}
	}

	private MapOfAlacrityPoint findPointAt(Point mousePosition, List<MapOfAlacrityPoint> points)
	{
		if (mousePosition == null)
		{
			return null;
		}

		for (MapOfAlacrityPoint point : points)
		{
			Point drawPoint = mapWorldPointToGraphicsPoint(point.getPoint());
			if (drawPoint == null)
			{
				continue;
			}

			int drawX = drawPoint.getX() - point.getImage().getWidth() / 2;
			int drawY = drawPoint.getY() - point.getImage().getHeight() / 2;
			Rectangle clickbox = new Rectangle(drawX, drawY, point.getImage().getWidth(), point.getImage().getHeight());
			if (clickbox.contains(mousePosition.getX(), mousePosition.getY()))
			{
				return point;
			}
		}

		return null;
	}

	private Point mapWorldPointToGraphicsPoint(net.runelite.api.coords.WorldPoint worldPoint)
	{
		WorldMap worldMap = client.getWorldMap();
		if (worldMap == null || worldMap.getWorldMapData() == null
			|| !worldMap.getWorldMapData().surfaceContainsPosition(worldPoint.getX(), worldPoint.getY()))
		{
			return null;
		}

		Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
		if (map == null)
		{
			return null;
		}

		float pixelsPerTile = worldMap.getWorldMapZoom();
		Rectangle worldMapRect = map.getBounds();
		int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
		int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);
		Point worldMapPosition = worldMap.getWorldMapPosition();

		int yTileMax = worldMapPosition.getY() - heightInTiles / 2;
		int yTileOffset = (yTileMax - worldPoint.getY() - 1) * -1;
		int xTileOffset = worldPoint.getX() + widthInTiles / 2 - worldMapPosition.getX();
		int xGraphDiff = (int) (xTileOffset * pixelsPerTile);
		int yGraphDiff = (int) (yTileOffset * pixelsPerTile);
		yGraphDiff -= pixelsPerTile - Math.ceil(pixelsPerTile / 2f);
		xGraphDiff += pixelsPerTile - Math.ceil(pixelsPerTile / 2f);
		yGraphDiff = worldMapRect.height - yGraphDiff;
		yGraphDiff += (int) worldMapRect.getY();
		xGraphDiff += (int) worldMapRect.getX();
		return new Point(xGraphDiff, yGraphDiff);
	}

	private Shape getWorldMapClipArea(Rectangle baseRectangle)
	{
		final Widget overview = client.getWidget(InterfaceID.Worldmap.OVERVIEW_CONTAINER);
		final Widget surfaceSelector = client.getWidget(InterfaceID.Worldmap.MAPLIST_BOX_GRAPHIC0);

		Area clipArea = new Area(baseRectangle);
		boolean subtracted = false;
		if (overview != null && !overview.isHidden())
		{
			clipArea.subtract(new Area(overview.getBounds()));
			subtracted = true;
		}

		if (surfaceSelector != null && !surfaceSelector.isHidden())
		{
			clipArea.subtract(new Area(surfaceSelector.getBounds()));
			subtracted = true;
		}

		return subtracted ? clipArea : baseRectangle;
	}
}
