package com.playmonumenta.epicstructures.utils;

import com.boydti.fawe.example.NMSMappedFaweQueue;
import com.boydti.fawe.example.Relighter;
import com.boydti.fawe.object.FaweQueue;
import com.boydti.fawe.util.EditSessionBuilder;
import com.boydti.fawe.util.SetQueue;

import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.entity.metadata.EntityType;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.EntityFunction;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.visitor.EntityVisitor;
import com.sk89q.worldedit.function.visitor.RegionVisitor;
import com.sk89q.worldedit.MutableBlockVector2D;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.world.World;

import java.util.List;

public class StructureUtils {
	// Custom paste function copied and modified from
	// FastAsyncWorldedit/core/src/main/java/com/boydti/fawe/object/schematic/Schematic.java
	//
	// Ignores structure void, leaving the original block in place
	public static void paste(Clipboard clipboard, org.bukkit.World world, Vector to) {
		// TODO: Whatever is going on here... entities are broken IF:
		// fastmode = true (regardless of combine stages setting)
		// fastmode = false AND combineStages = true
		EditSession extent = new EditSessionBuilder(world.getName()).autoQueue(true).fastmode(false).combineStages(false).build();

		Region sourceRegion = clipboard.getRegion().clone();
		Region destRegion = new CuboidRegion(to, new Vector(to).add(sourceRegion.getMaximumPoint()).subtract(sourceRegion.getMinimumPoint()));
		final Vector origin = clipboard.getOrigin();

		// ******************************** Blocks *************************************** //
		final int relx = to.getBlockX() - origin.getBlockX();
		final int rely = to.getBlockY() - origin.getBlockY();
		final int relz = to.getBlockZ() - origin.getBlockZ();
		RegionVisitor regionVisitor = new RegionVisitor(sourceRegion, new RegionFunction() {
			MutableBlockVector2D mpos2d_2 = new MutableBlockVector2D();
			MutableBlockVector2D mpos2d = new MutableBlockVector2D();
			{
				mpos2d.setComponents(Integer.MIN_VALUE, Integer.MIN_VALUE);
			}
			@Override
			public boolean apply(Vector mutable) throws WorldEditException {
				BaseBlock block = clipboard.getBlock(mutable);
				int xx = mutable.getBlockX() + relx;
				int yy = mutable.getBlockY() + rely;
				int zz = mutable.getBlockZ() + relz;

				// Don't paste in structure void
				if (block.getId() == 217) {
					return false;
				}

				extent.setBlock(xx, yy, zz, block);
				return false;
			}
		}, extent);

		// ******************************** Entities *************************************** //
		/*
		 * Make an iterable function that removes all the things that should
		 * be removed when a POI resets
		 */
		EntityFunction removeFunc = new EntityFunction() {
			@Override
			public boolean apply(Entity entity) throws WorldEditException {
				EntityType type = entity.getFacet(EntityType.class);
				Vector loc = entity.getLocation().toVector();

				// Don't remove entities in parts of the POI that don't reset
				if (clipboard.getBlock(loc.subtract(to).add(origin)).getId() == 217) {
					return false;
				}

				if (type == null ||
				type.isPlayerDerived() ||
				type.isAnimal() ||
				type.isTamed() ||
				type.isGolem() ||
				type.isNPC() ||
				type.isArmorStand()) {
					return false;
				}

				if (type.isLiving() ||
				//type.isItem() ||
				type.isProjectile() ||
				type.isFallingBlock() ||
				type.isBoat() ||
				type.isTNT() ||
				type.isExperienceOrb()) {
					entity.remove();
					return true;
				}

				return false;
			}
		};

		Runnable entityOperation = new Runnable() {
			public void run() {

				// Get the currently loaded entities in the destination region
				List <? extends Entity > entities = extent.getEntities(destRegion);

				// Visit those entities and when visited, remove them
				EntityVisitor EntityVisitor = new EntityVisitor(entities.iterator(), removeFunc);

				// After removing entities, fix the lighting
				Operations.completeSmart(EntityVisitor, new Runnable() {
					public void run() {
						fixLighting(extent, destRegion);
					}
				}, false);
			}
		};

		// First update the blocks, then remove entities, then fix lighting
		Operations.completeSmart(regionVisitor, entityOperation, false);

		extent.flushQueue();
	}

	/* This function derived from the one in FaweAPI */
	private static void fixLighting(EditSession extent, Region selection) {
		final Vector bot = selection.getMinimumPoint();
		final Vector top = selection.getMaximumPoint();

		final int minX = bot.getBlockX() >> 4;
		final int minZ = bot.getBlockZ() >> 4;

		final int maxX = top.getBlockX() >> 4;
		final int maxZ = top.getBlockZ() >> 4;

		Relighter relighter = extent.getQueue().getRelighter();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				relighter.addChunk(x, z, null, 65535);
			}
		}

		relighter.fixBlockLighting();
	}
}
