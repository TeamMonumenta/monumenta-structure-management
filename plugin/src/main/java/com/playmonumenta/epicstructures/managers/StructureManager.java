package com.playmonumenta.epicstructures.managers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

import com.boydti.fawe.object.clipboard.DiskOptimizedClipboard;
import com.boydti.fawe.util.EditSessionBuilder;
import com.playmonumenta.epicstructures.Plugin;
import com.playmonumenta.epicstructures.utils.CommandUtils;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.util.io.Closer;
import com.sk89q.worldedit.world.World;

public class StructureManager {
	private static final String FORMAT = "sponge";

	private final Plugin mPlugin;

	public StructureManager(Plugin plugin) {
		mPlugin = plugin;
	}

	/* It *should* be safe to call this async */
	public BlockArrayClipboard loadSchematic(String baseName) throws Exception {
		BlockArrayClipboard clipboard = null;
		if (clipboard == null) {
			// Schematic not already loaded - need to read it from disk and load it into RAM

			File file = CommandUtils.getAndValidateSchematicPath(mPlugin, baseName, true);

			ClipboardFormat format = ClipboardFormats.findByAlias(FORMAT);
			Clipboard newClip = format.load(file);
			if (newClip instanceof BlockArrayClipboard) {
				clipboard = (BlockArrayClipboard)newClip;
			} else if (newClip instanceof DiskOptimizedClipboard) {
				clipboard = ((DiskOptimizedClipboard)newClip).toClipboard();
			} else {
				throw new Exception("Loaded unknown clipboard type: " + newClip.getClass().toString());
			}
		}

		return clipboard;
	}

	public void saveSchematic(String baseName, BlockVector3 minpos, BlockVector3 maxpos, org.bukkit.World bukkitWorld) throws Exception {
		File file = CommandUtils.getAndValidateSchematicPath(mPlugin, baseName, false);
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			file.createNewFile();
		}

		World world = new BukkitWorld(bukkitWorld);
		CuboidRegion cReg = new CuboidRegion(world, minpos, maxpos);
		Clipboard clipboard = new BlockArrayClipboard(cReg);
		clipboard.setOrigin(cReg.getMinimumPoint());

		/* Copy the region (including entities and biomes) into the clipboard object */
		EditSession extent = new EditSessionBuilder(world)
			.autoQueue(true)
			.fastmode(true)
			.combineStages(true)
			.changeSetNull()
			.checkMemory(false)
			.allowedRegionsEverywhere()
			.limitUnlimited()
			.build();
		ForwardExtentCopy copy = new ForwardExtentCopy(extent, cReg, clipboard, cReg.getMinimumPoint());
		copy.setCopyingEntities(true);
		copy.setCopyingBiomes(false); // TODO: Re enable when FAWE supports this again
		Operations.completeLegacy(copy);

		try (Closer closer = Closer.create()) {
			ClipboardFormat format = ClipboardFormats.findByAlias(FORMAT);
			FileOutputStream fos = closer.register(new FileOutputStream(file));
			BufferedOutputStream bos = closer.register(new BufferedOutputStream(fos));
			ClipboardWriter writer = closer.register(format.getWriter(bos));
			writer.write(clipboard);
		} catch (Exception ex) {
			throw ex;
		}
	}
}
