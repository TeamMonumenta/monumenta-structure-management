package com.playmonumenta.epicstructures.utils;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.bergerkiller.bukkit.common.wrappers.LongHashSet;
import com.bergerkiller.bukkit.lightcleaner.lighting.LightingService;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.util.EditSessionBuilder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

public class StructureUtils {
	private static final boolean LIGHT_CLEANER_ENABLED = Bukkit.getPluginManager().isPluginEnabled("LightCleaner");

	// Custom paste function copied and modified from
	// FastAsyncWorldedit/core/src/main/java/com/boydti/fawe/object/schematic/Schematic.java
	//
	// Ignores structure void, leaving the original block in place
	public static void paste(Plugin plugin, BlockArrayClipboard clipboard, World world, BlockVector3 to) {
		// TODO: Whatever is going on here... entities are broken IF:
		// fastmode = true (regardless of combine stages setting)
		// fastmode = false AND combineStages = true
		EditSession extent = new EditSessionBuilder(world.getName()).autoQueue(true).fastmode(false).combineStages(false).build();

		Region sourceRegion = clipboard.getRegion().clone();
		final BlockVector3 size = sourceRegion.getMaximumPoint().subtract(sourceRegion.getMinimumPoint());
		final int relx = to.getBlockX();
		final int rely = to.getBlockY();
		final int relz = to.getBlockZ();

		clipboard.IMP.forEach(new FaweClipboard.BlockReader() {
			@Override
			public <B extends BlockStateHolder<B>> void run(int x, int y, int z, B block) {
				if (!block.getBlockType().equals(BlockTypes.STRUCTURE_VOID)) {
					extent.setBlock(x + relx, y + rely, z + relz, block);
				}
			}
		}, true);

		extent.flushQueue();

		/*
		 * Fix lighting after the structure loads (if plugin present)
		 */
		new BukkitRunnable() {
			@Override
			public void run()
			{
				scheduleLighting(world, to, size);
			}
		}.runTaskLater(plugin, 40);
	}

	public static void scheduleLighting(World world, BlockVector3 to, BlockVector3 size) {
		if (!LIGHT_CLEANER_ENABLED) {
			return;
		}

		// Pad one chunk on all sides
		final int originX = (to.getBlockX() / 16) - 1;
		final int originZ = (to.getBlockZ() / 16) - 1;
		final int sizeX = (((size.getBlockX() - 1) / 16) + 3);
		final int sizeZ = (((size.getBlockZ() - 1) / 16) + 3);

        LongHashSet chunks = new LongHashSet(sizeX * sizeZ);
		for (int x = 0; x < sizeX; x++) {
			for (int z = 0; z < sizeZ; z++) {
				chunks.add(originX + x, originZ + z);
			}
		}
		LightingService.schedule(world, chunks);
	}
}
