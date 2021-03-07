package com.playmonumenta.structures;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import com.playmonumenta.structures.utils.CommandUtils;
import com.playmonumenta.structures.utils.StructureUtils;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.math.BlockVector3;

import org.bukkit.Bukkit;
import org.bukkit.Location;

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class StructuresAPI {
	/**
	 * Trigger a structure to be loaded asynchronously at the specified location.
	 *
	 * Can be called from the main thread or an async thread.
	 *
	 * @param path Relative path under the structures/ folder of the structure to load, not including the extension
	 * @param loadLoc Minimum corner where the structure should be loaded
	 * @param includeEntities Whether or not entities saved in the structure will be pasted in
	 *
	 * @return Returns a future which will be completed when loading is complete or an error occurs.
	 *         If you need to do something with the result, suggest calling .get() on the result in an async thread,
	 *         which will block until complete or an exception is generated
	 */
	public static CompletableFuture<Void> loadStructureAsync(String path, Location loadLoc, boolean includeEntities) {
		StructuresPlugin plugin = StructuresPlugin.getInstance();

		CompletableFuture<Void> future = new CompletableFuture<>();

		// Load the structure asynchronously (this might access the disk!)
		// Then switch back to the main thread to initiate pasting
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			final BlockArrayClipboard clipboard;

			try {
				CommandUtils.getAndValidateSchematicPath(plugin, path, true);
			} catch (WrapperCommandSyntaxException e) {
				future.completeExceptionally(e);
				return;
			}

			BlockVector3 loadPos = BlockVector3.at(loadLoc.getBlockX(), loadLoc.getBlockY(), loadLoc.getBlockZ());

			try {
				clipboard = plugin.mStructureManager.loadSchematic(path);
			} catch (Exception e) {
				plugin.asyncLog(Level.SEVERE, "Failed to load schematic '" + path + "'", e);
				future.completeExceptionally(e);
				return;
			}

			/* Once the schematic is loaded, this task is used to paste it */
			Bukkit.getScheduler().runTask(plugin, () -> {
				StructureUtils.paste(plugin, clipboard, loadLoc.getWorld(), loadPos, includeEntities, () -> {
					future.complete(null);
				}, (e) -> {
					future.completeExceptionally(e);
				});
			});
		});

		return future;
	}

	/**
	 * Save a structure given a bounding box at the specified path.
	 *
	 * Note that this function will block whatever thread it is called on, causing lag if that's the main thread.
	 *
	 * Can be called from the main thread or an async thread.
	 *
	 * @param path Relative path under the structures/ folder of the schematic to load, not including the extension
	 * @param loc1 One corner of the bounding box to save
	 * @param loc2 The opposite corner
	 */
	public static void save(String path, Location loc1, Location loc2) throws Exception {
		if (path.contains("..")) {
			throw new Exception("Paths containing '..' are not allowed");
		}

		StructuresPlugin plugin = StructuresPlugin.getInstance();

		// Parse the coordinates of the structure to save
		BlockVector3 pos1 = BlockVector3.at(loc1.getBlockX(), loc1.getBlockY(), loc1.getBlockZ());
		BlockVector3 pos2 = BlockVector3.at(loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ());

		BlockVector3 minpos = pos1.getMinimum(pos2);
		BlockVector3 maxpos = pos1.getMaximum(pos2);

		// Save it
		plugin.mStructureManager.saveSchematic(path, minpos, maxpos, loc1.getWorld());
	}
}
