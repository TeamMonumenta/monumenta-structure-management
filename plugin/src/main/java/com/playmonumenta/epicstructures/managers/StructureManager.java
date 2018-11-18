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
	private ConcurrentSkipListMap<String, Schematic> mSchematics = new ConcurrentSkipListMap<String, Schematic>();
	Plugin mPlugin;
	org.bukkit.World mWorld;
	private final ClipboardFormat FORMAT;

	public StructureManager(Plugin plugin, org.bukkit.World world) {
		mPlugin = plugin;
		mWorld = world;
		FORMAT = ClipboardFormat.findByAlias("structure");
	}

	/* It *should* be safe to call this async */
	public Schematic loadSchematic(String baseFolderName, String baseName) throws Exception {
		if (baseFolderName == null || baseFolderName.isEmpty() || baseName == null || baseName.isEmpty()) {
			throw new Exception("Structure name is empty!");
		}

		Schematic schem;
		schem = mSchematics.get(baseFolderName + baseName);
		if (schem == null) {
			// Schematic not already loaded - need to read it from disk and load it into RAM

			final String fileName = _getFileName(baseFolderName, baseName);

			File file = new File(fileName);
			if (!file.exists()) {
				throw new Exception("Structure '" + baseName + "' does not exist");
			}

			schem = FORMAT.load(file);

			// Cache the schematic for fast access later
			mSchematics.put(baseFolderName + baseName, schem);
		}

		return schem;
	}

	/* It *should* be safe to call this async */
	public BlockArrayClipboard loadSchematicClipboard(String baseFolderName, String baseName) throws Exception {
		Clipboard clipboard = loadSchematic(baseFolderName, baseName).getClipboard();
		if (!(clipboard instanceof BlockArrayClipboard)) {
			throw new Exception("Clipboard is not a BlockArrayClipboard!");
		}
		return (BlockArrayClipboard)clipboard;
	}

	// This code adapted from forum post here: https://www.spigotmc.org/threads/saving-schematics-to-file-with-worldedit-api.276078/
	public void saveSchematic(String baseFolderName, String baseName, Vector minpos, Vector maxpos, Runnable whenDone) throws Exception {
		if (baseFolderName == null || baseFolderName.isEmpty() || baseName == null || baseName.isEmpty()) {
			throw new Exception("Structure name is empty!");
		}

		final String fileName = _getFileName(baseFolderName, baseName);

		File file = new File(fileName);
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			file.createNewFile();
		}

		World world = new BukkitWorld(mWorld);
		CuboidRegion cReg = new CuboidRegion(world, minpos, maxpos);
		Schematic schem = new Schematic(cReg);
		schem.save(file, FORMAT);

		// Re-load the schematic from disk into the cache
		mSchematics.remove(baseFolderName + baseName);
		loadSchematic(baseFolderName, baseName);
	}

	private String _getFileName(String baseFolderName, String baseName) {
		return Paths.get(mPlugin.getDataFolder().toString(), baseFolderName, baseName + ".schematic").toString();
	}
}
