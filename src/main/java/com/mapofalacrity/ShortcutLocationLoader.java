package com.mapofalacrity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.coords.WorldPoint;

@Singleton
public class ShortcutLocationLoader
{
	static final String RESOURCE_PATH = "/locations.json";

	private static final Type LOCATION_RECORD_LIST = new TypeToken<List<LocationRecord>>() { }.getType();

	private final Gson gson = new Gson();

	@Inject
	public ShortcutLocationLoader()
	{
	}

	public List<ShortcutLocation> load() throws IOException
	{
		try (InputStream inputStream = ShortcutLocationLoader.class.getResourceAsStream(RESOURCE_PATH))
		{
			if (inputStream == null)
			{
				throw new IOException("Missing resource " + RESOURCE_PATH);
			}

			return load(inputStream);
		}
	}

	List<ShortcutLocation> load(InputStream inputStream) throws IOException
	{
		try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8))
		{
			List<LocationRecord> records = gson.fromJson(reader, LOCATION_RECORD_LIST);
			if (records == null)
			{
				throw new IOException("No Map of Alacrity locations were found in " + RESOURCE_PATH);
			}

			List<ShortcutLocation> locations = new ArrayList<>(records.size());
			for (LocationRecord record : records)
			{
				validate(record);
				locations.add(new ShortcutLocation(
					TeleportRegion.fromDisplayName(record.getRegion()),
					record.getName().trim(),
					new WorldPoint(record.getX(), record.getY(), record.getPlane())));
			}
			return locations;
		}
	}

	private static void validate(LocationRecord record) throws IOException
	{
		if (record == null)
		{
			throw new IOException("Encountered null location record");
		}

		if (record.getRegion() == null || record.getRegion().trim().isEmpty())
		{
			throw new IOException("Location record is missing region");
		}

		if (record.getName() == null || record.getName().trim().isEmpty())
		{
			throw new IOException("Location record is missing name");
		}

		if (record.getPlane() < 0)
		{
			throw new IOException("Location record has invalid plane for " + record.getName());
		}
	}
}
