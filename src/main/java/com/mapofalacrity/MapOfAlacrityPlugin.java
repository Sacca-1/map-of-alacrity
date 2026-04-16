package com.mapofalacrity;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.inject.Inject;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.MenuAction;
import net.runelite.api.ItemContainer;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.ScriptPreFired;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.MouseManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Map of Alacrity",
	description = "Displays Map of Alacrity shortcut locations on the world map and lets you teleport to them directly",
	tags = {"Alacrity", "league", "map", "worldmap", "teleport"}
)
@Slf4j
public class MapOfAlacrityPlugin extends Plugin
{
	private static final int LEAGUE_AGILITY_MAP_ITEM_ID = 33233;
	private static final int INVENTORY_WIDGET_ID = 9764864;
	private static final int MENU_SETUP_SCRIPT = 219;
	private static final int MENU_NEW_SETUP_SCRIPT = 9142;
	private static final int MENU_CREATEENTRY_SCRIPT = 218;
	private static final int MENU_NEW_CREATEENTRY_SCRIPT = 9143;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MouseManager mouseManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MapOfAlacrityWorldMapOverlay worldMapOverlay;

	@Inject
	private MapOfAlacrityMarkerManager markerManager;

	@Inject
	private ShortcutLocationRepository locationRepository;

	@Inject
	private MapOfAlacrityConfig config;

	@Provides
	MapOfAlacrityConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MapOfAlacrityConfig.class);
	}

	private final List<TeleMenu> teleMenus = new ArrayList<>();
	private String activeMenu = null;
	private ShortcutLocation pendingShortcutTarget;

	@Override
	protected void startUp() throws Exception
	{
		locationRepository.reload();
		markerManager.rebuildPoints();
		worldMapOverlay.setPointClickConsumer(this::handlePointClick);
		overlayManager.add(worldMapOverlay);
		mouseManager.registerMouseListener(worldMapOverlay);
	}

	@Override
	protected void shutDown()
	{
		mouseManager.unregisterMouseListener(worldMapOverlay);
		overlayManager.remove(worldMapOverlay);
		activeMenu = null;
		pendingShortcutTarget = null;
		teleMenus.clear();
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() != GameState.LOGGED_IN)
		{
			log.info("Game state changed away from logged in; clearing menu state");
			pendingShortcutTarget = null;
			activeMenu = null;
			teleMenus.clear();
		}
	}

	@Subscribe
	public void onGameTick(GameTick gameTick)
	{
		List<TeleMenu> change = null;
		for (TeleMenu menu : teleMenus)
		{
			boolean found = false;
			for (var entry : client.getComponentTable())
			{
				if (entry.getId() == menu.textWidget.getId() >> 16)
				{
					found = true;
					break;
				}
			}

			if (!found)
			{
				if (change == null)
				{
					change = new ArrayList<>(teleMenus);
				}
				change.remove(menu);
			}
		}
		if (change != null)
		{
			teleMenus.clear();
			teleMenus.addAll(change);
		}
	}

	@Subscribe
	public void onWidgetLoaded(WidgetLoaded widgetLoaded)
	{
		if (widgetLoaded.getGroupId() == InterfaceID.MENU)
		{
			activeMenu = null;
		}
	}

	@Subscribe
	public void onScriptPreFired(ScriptPreFired event)
	{
		switch (event.getScriptId())
		{
			case MENU_SETUP_SCRIPT:
			{
				String title = (String) client.getObjectStack()[client.getObjectStackSize() - 1];
				activeMenu = cleanify(title);
				teleMenus.clear();
				break;
			}
			case MENU_NEW_SETUP_SCRIPT:
			{
				String title = (String) event.getScriptEvent().getArguments()[1];
				activeMenu = cleanify(title);
				teleMenus.clear();
				break;
			}
			default:
				break;
		}
	}

	@Subscribe(priority = 1.f)
	public void onScriptPostFired(ScriptPostFired event)
	{
		switch (event.getScriptId())
		{
			case MENU_CREATEENTRY_SCRIPT:
				if (activeMenu != null)
				{
					buildMenuEntry(true);
				}
				break;
			case MENU_NEW_CREATEENTRY_SCRIPT:
				if (activeMenu != null)
				{
					buildMenuEntry(false);
				}
				break;
			default:
				break;
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuAction() == MenuAction.WIDGET_CONTINUE)
		{
			for (TeleMenu menu : teleMenus)
			{
				if (menu.opWidget.getId() == event.getParam1() && menu.opWidget.getIndex() == event.getParam0())
				{
					log.info("Widget continue clicked: option='{}' target='{}' id={} itemId={} widgetId={} index={} action={} pending='{}'",
						event.getMenuOption(),
						event.getMenuTarget(),
						event.getId(),
						event.getItemId(),
						event.getParam1(),
						event.getParam0(),
						event.getMenuAction(),
						pendingShortcutTarget == null ? "" : pendingShortcutTarget.getName());
					if (pendingShortcutTarget != null && cleanify(menu.displayText).equals(cleanify(pendingShortcutTarget.getName())))
					{
						log.info("Clearing pending target after widget continue: pending='{}' entry='{}'",
							pendingShortcutTarget.getName(),
							menu.displayText);
						pendingShortcutTarget = null;
					}
				}
			}
		}
	}

	private void handlePointClick(ShortcutLocation location)
	{
		log.info("Map click queued: location='{}' region='{}' worldPoint={}", location.getName(), location.getRegion().getDisplayName(), location.getWorldPoint());
		clientThread.invokeLater(() ->
		{
			pendingShortcutTarget = location;
			log.info("Dispatching region action: location='{}' region='{}' inventorySlot={}", location.getName(), location.getRegion().getDisplayName(), findMapItemSlot());
			invokeRegionAction(location.getRegion());
		});
	}

	private void invokeRegionAction(TeleportRegion region)
	{
		int inventorySlot = findMapItemSlot();
		if (inventorySlot < 0)
		{
			log.info("Map item not found; region action skipped for region='{}'", region.getDisplayName());
			return;
		}

		log.info("Invoking region action: region='{}' menuIdentifier={} inventorySlot={}", region.getDisplayName(), region.getMenuIdentifier(), inventorySlot);
		client.menuAction(
			inventorySlot,
			INVENTORY_WIDGET_ID,
			MenuAction.CC_OP,
			region.getMenuIdentifier(),
			LEAGUE_AGILITY_MAP_ITEM_ID,
			region.getDisplayName(),
			"Map of Alacrity");
	}

	private int findMapItemSlot()
	{
		ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
		if (inventory == null)
		{
			return -1;
		}

		return inventory.find(LEAGUE_AGILITY_MAP_ITEM_ID);
	}

	private void buildMenuEntry(boolean legacyMenu)
	{
		Widget textWidget = client.getScriptActiveWidget();
		if (textWidget == null)
		{
			return;
		}

		Widget resumeWidget = client.getScriptActiveWidget();
		Widget opWidget = client.getScriptActiveWidget();
		if (opWidget == null)
		{
			return;
		}

		if (opWidget.getOnOpListener() == null)
		{
			opWidget.setOnOpListener((JavaScriptCallback) (ev -> { }));
		}

		log.info("Captured tele menu entry: activeMenu='{}' text='{}' legacy={} opWidget={} resumeWidget={}",
			activeMenu,
			textWidget.getText(),
			legacyMenu,
			opWidget.getId(),
			resumeWidget.getId());

		new TeleMenu()
			.textWidget(textWidget)
			.resumeWidget(resumeWidget)
			.opWidget(opWidget)
			.legacyMenu(legacyMenu)
			.build();
	}

	private static String cleanify(String input)
	{
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < input.length(); i++)
		{
			char c = Character.toLowerCase(input.charAt(i));
			if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9'))
			{
				builder.append(c);
			}
			else if (c == '-' || c == ' ')
			{
				builder.append('-');
			}
		}
		return builder.toString();
	}

	private static String stripTags(String text)
	{
		return text == null ? "" : text.replaceAll("<[^>]+>", "");
	}

	@NoArgsConstructor
	@Accessors(fluent = true, chain = true)
	class TeleMenu
	{
		@Setter
		Widget textWidget;

		@Setter
		Widget resumeWidget;

		@Setter
		Widget opWidget;

		@Setter
		boolean legacyMenu;

		String rawText;
		String displayText;

		public void build()
		{
			rawText = textWidget.getText();
			displayText = stripTags(rawText);
			disabledIfUnavailable();
			teleMenus.add(this);
			teleMenus.sort(Comparator.comparing((TeleMenu tm) -> tm.displayText));
			log.info("Tele menu normalized: display='{}' normalized='{}' pending='{}'",
				displayText,
				cleanify(displayText),
				pendingShortcutTarget == null ? "" : cleanify(pendingShortcutTarget.getName()));
			if (pendingShortcutTarget != null && cleanify(displayText).contains(cleanify(pendingShortcutTarget.getName())))
			{
				log.info("Matched pending target: pending='{}' entry='{}'", pendingShortcutTarget.getName(), displayText);
				clientThread.invokeLater(this::onTrigger);
			}
		}

		private void disabledIfUnavailable()
		{
			if (rawText.startsWith("<str>"))
			{
				textWidget.setHidden(true);
				if (legacyMenu)
				{
					client.getIntStack()[client.getIntStackSize() - 1] -= textWidget.getOriginalHeight();
				}
			}
		}

		void onTrigger()
		{
			log.info("Triggering target: name='{}' display='{}'", pendingShortcutTarget == null ? "" : pendingShortcutTarget.getName(), displayText);
			resume(resumeWidget);
			if (config.closeMapAfterTeleport())
			{
				log.info("Closing world map after trigger");
				closeWorldMap();
			}
			textWidget.setText("Please wait...");
			log.info("Trigger complete: display='{}'", displayText);
		}
	}

	private void resume(Widget widget)
	{
		if (widget == null)
		{
			log.info("Resume skipped: widget missing");
			return;
		}

		log.info("Replaying widget continue: id={} index={}", widget.getId(), widget.getIndex());
		client.menuAction(
			widget.getIndex(),
			widget.getId(),
			MenuAction.WIDGET_CONTINUE,
			0,
			-1,
			"Continue",
			"");
	}

	private void closeWorldMap()
	{
		Widget closeWidget = client.getWidget(InterfaceID.Worldmap.CLOSE);
		if (closeWidget == null || closeWidget.isHidden())
		{
			log.info("World map close skipped: close widget missing or hidden");
			return;
		}

		log.info("Closing world map widget: id={} index={}", closeWidget.getId(), closeWidget.getIndex());
		client.menuAction(
			closeWidget.getIndex(),
			closeWidget.getId(),
			MenuAction.CC_OP,
			1,
			-1,
			"Close",
			"");
	}
}
