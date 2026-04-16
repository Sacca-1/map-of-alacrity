package com.mapofalacrity;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Getter;

@Singleton
public class ShortcutLocationRepository
{
	private final ShortcutLocationLoader loader;

	@Getter
	private ImmutableList<ShortcutLocation> locations = ImmutableList.of();

	@Inject
	public ShortcutLocationRepository(ShortcutLocationLoader loader)
	{
		this.loader = loader;
	}

	public void reload() throws IOException
	{
		List<ShortcutLocation> loaded = loader.load();
		this.locations = ImmutableList.copyOf(loaded);
	}
}
