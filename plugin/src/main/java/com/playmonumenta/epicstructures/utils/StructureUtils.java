package com.playmonumenta.epicstructures.utils;

import com.bergerkiller.bukkit.common.wrappers.LongHashSet;
import com.bergerkiller.bukkit.lightcleaner.lighting.LightingService;

import com.boydti.fawe.FaweAPI;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.SetQueue;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.MutableBlockVector;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.world.block.BlockState;
import com.sk89q.worldedit.world.block.BlockTypes;

import java.util.List;

import org.bukkit.World;

public class StructureUtils {
	// Custom paste function copied and modified from
	// FastAsyncWorldedit/core/src/main/java/com/boydti/fawe/object/schematic/Schematic.java
	//
	// Ignores structure void, leaving the original block in place
	public static void paste(BlockArrayClipboard clipboard, World world, Vector to) {
		// TODO: Whatever is going on here... entities are broken IF:
		// fastmode = true (regardless of combine stages setting)
		// fastmode = false AND combineStages = true
		EditSession extent = new EditSessionBuilder(world.getName()).autoQueue(true).fastmode(false).combineStages(false).build();

		Region sourceRegion = clipboard.getRegion().clone();
		final Vector size = sourceRegion.getMaximumPoint().subtract(sourceRegion.getMinimumPoint());
		Region destRegion = new CuboidRegion(to, new Vector(to).add(size));
		final Vector origin = clipboard.getOrigin();
		final int relx = to.getBlockX() - origin.getBlockX();
		final int rely = to.getBlockY() - origin.getBlockY();
		final int relz = to.getBlockZ() - origin.getBlockZ();
		final int maxx = size.getBlockX() + 1;
		final int maxy = size.getBlockY() + 1;
		final int maxz = size.getBlockZ() + 1;

		clipboard.IMP.forEach(new FaweClipboard.BlockReader() {
			@Override
			public void run(int x, int y, int z, BlockState block) {
				if (!block.getBlockType().equals(BlockTypes.STRUCTURE_VOID)) {
					extent.setBlock(x + relx, y + rely, z + relz, block);
				}
			}
		}, true);

		/*
		 * TODO:
		 * This causes any mob spawned after relighting this area to be invisible.
		 * It does this even without all of the other code in this function
		 * It does not matter what the relighting settings are set to
		 * It does not matter if any other entities are in the chunk
		 * It does not matter if you summon the mob with /summon or a spawner
		 *
		// After entities & blocks, fix lighting
		FaweAPI.fixLighting(FaweAPI.getWorld(world.getName()),
							destRegion, extent.getQueue(), FaweQueue.RelightMode.ALL);
		 */

		extent.flushQueue();

		/*
		 * Fix lighting after the structure loads
		 */
		scheduleLighting(world, to, size);
	}

	public static void scheduleLighting(World world, Vector to, Vector size) {
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
