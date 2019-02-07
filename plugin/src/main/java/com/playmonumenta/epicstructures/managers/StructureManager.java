package com.playmonumenta.epicstructures.managers;

import com.boydti.fawe.object.schematic.Schematic;
import com.boydti.fawe.util.EditSessionBuilder;

import com.playmonumenta.epicstructures.Plugin;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.world.World;

import java.io.File;
import java.io.FileOutputStream;

import java.nio.file.Paths;

import java.util.concurrent.ConcurrentSkipListMap;

public class StructureManager {
	private static final String BASE_FOLDER_NAME = "structures";

	private final ConcurrentSkipListMap<String, BlockArrayClipboard> mSchematics = new ConcurrentSkipListMap<String, BlockArrayClipboard>();
	private final Plugin mPlugin;
	private final org.bukkit.World mWorld;
	private final ClipboardFormat format;

	public StructureManager(Plugin plugin, org.bukkit.World world) {
		mPlugin = plugin;
		mWorld = world;
		format = ClipboardFormat.findByAlias("sponge");
	}

	/* It *should* be safe to call this async */
	public BlockArrayClipboard loadSchematic(String baseName) throws Exception {
		if (baseName == null || baseName.isEmpty()) {
			throw new Exception("Structure name is empty!");
		}

		BlockArrayClipboard clipboard;
		clipboard = mSchematics.get(baseName);
		if (clipboard == null) {
			// Schematic not already loaded - need to read it from disk and load it into RAM

			final String fileName = _getFileName(baseName);

			File file = new File(fileName);
			if (!file.exists()) {
				throw new Exception("Structure '" + baseName + "' does not exist");
			}

			Clipboard newClip = format.load(file).getClipboard();
			if (!(newClip instanceof BlockArrayClipboard)) {
				throw new Exception("Clipboard is not a BlockArrayClipboard!");
			}
			clipboard = (BlockArrayClipboard)newClip;

			// Cache the schematic for fast access later
			mSchematics.put(baseName, clipboard);
		}

		return clipboard;
	}

	// This code adapted from forum post here: https://www.spigotmc.org/threads/saving-schematics-to-file-with-worldedit-api.276078/
	public void saveSchematic(String baseName, Vector minpos, Vector maxpos, Runnable whenDone) throws Exception {
		if (baseName == null || baseName.isEmpty()) {
			throw new Exception("Structure name is empty!");
		}

		final String fileName = _getFileName(baseName);

		File file = new File(fileName);
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			file.createNewFile();
		}

		World world = new BukkitWorld(mWorld);
		CuboidRegion cReg = new CuboidRegion(world, minpos, maxpos);
		Schematic schem = new Schematic(cReg);
		schem.save(file, format);

		// Re-load the schematic from disk into the cache
		mSchematics.remove(baseName);
		loadSchematic(baseName);

		/*
		 * Cache has been updated - but the respawning structures need to be updated too
		 * to get this new cached copy
		 */
		mPlugin.saveConfig();
		mPlugin.reloadConfig();
	}

	private String _getFileName(String baseName) {
		return Paths.get(mPlugin.getDataFolder().toString(), BASE_FOLDER_NAME, baseName + ".schematic").toString();
	}
}
