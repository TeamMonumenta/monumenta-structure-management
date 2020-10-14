package com.playmonumenta.epicstructures.utils;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.bergerkiller.bukkit.common.wrappers.LongHashSet;
import com.bergerkiller.bukkit.lightcleaner.lighting.LightingService;
import com.boydti.fawe.util.EditSessionBuilder;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.function.RegionFunction;
import com.sk89q.worldedit.function.mask.SingleBlockTypeMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.block.BlockType;
import com.sk89q.worldedit.world.block.BlockTypes;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Furnace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Tameable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class StructureUtils {
	private static final boolean LIGHT_CLEANER_ENABLED = Bukkit.getPluginManager().isPluginEnabled("LightCleaner");
	private static final BlockType shulkerBoxes[] = {
		BlockTypes.SHULKER_BOX,
		BlockTypes.WHITE_SHULKER_BOX,
		BlockTypes.ORANGE_SHULKER_BOX,
		BlockTypes.MAGENTA_SHULKER_BOX,
		BlockTypes.LIGHT_BLUE_SHULKER_BOX,
		BlockTypes.YELLOW_SHULKER_BOX,
		BlockTypes.LIME_SHULKER_BOX,
		BlockTypes.PINK_SHULKER_BOX,
		BlockTypes.GRAY_SHULKER_BOX,
		BlockTypes.LIGHT_GRAY_SHULKER_BOX,
		BlockTypes.CYAN_SHULKER_BOX,
		BlockTypes.PURPLE_SHULKER_BOX,
		BlockTypes.BLUE_SHULKER_BOX,
		BlockTypes.BROWN_SHULKER_BOX,
		BlockTypes.GREEN_SHULKER_BOX,
		BlockTypes.RED_SHULKER_BOX,
		BlockTypes.BLACK_SHULKER_BOX
	};
	private static final Set<BlockType> shulkerSet = new HashSet<>(Arrays.asList(shulkerBoxes));

	// Ignores structure void, leaving the original block in place
	public static void paste(Plugin plugin, BlockArrayClipboard clipboard, World world, BlockVector3 to, boolean includeEntities) {

		long startTime = System.currentTimeMillis(); // <-- START

		EditSession extent = new EditSessionBuilder(new BukkitWorld(world))
			.autoQueue(true)
			.fastmode(true)
			.combineStages(true)
			.changeSetNull()
			.checkMemory(false)
			.allowedRegionsEverywhere()
			.limitUnlimited()
			.build();

		Region sourceRegion = clipboard.getRegion();
		final BlockVector3 size = sourceRegion.getMaximumPoint().subtract(sourceRegion.getMinimumPoint());
		final Vector pos1 = new Vector((double)to.getX(), (double)to.getY(), (double)to.getZ());
		final Vector pos2 = pos1.clone().add(new Vector(size.getX() + 1, size.getY() + 1, size.getZ() + 1));
		final BoundingBox box = new BoundingBox(pos1.getX(), pos1.getY(), pos1.getZ(), pos2.getX(), pos2.getY(), pos2.getZ());

		/* Filter blocks from being pasted that would land on top of A */
		final int relx = to.getBlockX();
		final int rely = to.getBlockY();
		final int relz = to.getBlockZ();
		RegionFunction filterFunction = position -> {
				int x = position.getBlockX();
				int y = position.getBlockY();
				int z = position.getBlockZ();
				BlockType oldBlockType = null;

				/*
				 * Attempt to get the existing block type. If this errors for some reason, just proceed to overwrite
				 * the block anyway if it's not structure void
				 */
				try {
					oldBlockType = extent.getBlockType(x + relx, y + rely, z + relz);
				} catch (Exception e) {
					plugin.getLogger().warning("Caught error in FAWE extent.getBlock(). This should be reported to FAWE");
					e.printStackTrace();
				}
				BlockType newBlockType = clipboard.getBlock(position).getBlockType();
				if (oldBlockType != null && oldBlockType.equals(BlockTypes.CHEST)) {
					Chest chest = (Chest) world.getBlockAt(x + relx, y + rely, z + relz).getState();
					if (chest.getCustomName() != null && chest.getCustomName().endsWith("'s Grave")) {
						// Check if the grave has items inside. If it is empty, it can be overwritten.
						for (ItemStack item : chest.getInventory()) {
							if (item != null) {
								return false;
							}
						}
					}
				}
				if (oldBlockType != null && shulkerSet.contains(oldBlockType)) {
					// Don't allow Shulker Boxes to be overwritten
					return false;
				}
				if (newBlockType == null || !newBlockType.equals(BlockTypes.STRUCTURE_VOID)) {
					return true;
				}
				return false;
			};


		Region shiftedRegion = clipboard.getRegion().clone();
		shiftedRegion.shift(to);

		Set<BlockVector2> chunks = shiftedRegion.getChunks();
		AtomicInteger numRemaining = new AtomicInteger(chunks.size());

		/* This chunk consumer removes entities and sets spawners/brewstands/furnaces to air */
		Consumer<Chunk> chunkConsumer = (Chunk chunk) -> {
			numRemaining.decrementAndGet();
			for (BlockState state : chunk.getTileEntities()) {
				if (state instanceof CreatureSpawner || state instanceof BrewingStand || state instanceof Furnace) {
					org.bukkit.Location loc = state.getLocation();
					BlockVector3 relPos = BlockVector3.at(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()).subtract(to);
					if (box.contains(loc.toVector()) && !clipboard.getBlock(relPos).getBlockType().equals(BlockTypes.STRUCTURE_VOID)) {
						Block block = state.getBlock();
						block.setType(Material.DIRT);
						block.setBlockData(Material.DIRT.createBlockData());
					}
				}
			}

			if (includeEntities) {
				for (Entity entity : chunk.getEntities()) {
					if (box.contains(entity.getLocation().toVector()) && entityShouldBeRemoved(entity)) {
						Vector relPos = entity.getLocation().toVector().subtract(pos1);
						if (!clipboard.getBlock(BlockVector3.at(relPos.getBlockX(), relPos.getBlockY(), relPos.getBlockZ())).getBlockType().equals(BlockTypes.STRUCTURE_VOID)) {
							entity.remove();
						}
					}
				}
			}
		};

		/* Load all the chunks in the region and run the chunk consumer */
		for (BlockVector2 chunkCoords : shiftedRegion.getChunks()) {
			world.getChunkAtAsync(chunkCoords.getX(), chunkCoords.getZ(), chunkConsumer);
		}

		new BukkitRunnable() {
			int numTicksWaited = 0;
			@Override
			public void run() {
				numTicksWaited++;
				if (numTicksWaited >= 30 * 20) {
					plugin.getLogger().severe("Timed out waiting for chunks to load to paste structure!");
					this.cancel();
					return;
				}
				if (numRemaining.get() == 0) {
					this.cancel();

					new BukkitRunnable() {
						@Override
						public void run() {
							ForwardExtentCopy copy = new ForwardExtentCopy(clipboard, clipboard.getRegion(), clipboard.getOrigin(), extent, to);
							copy.setCopyingBiomes(false);
							copy.setFilterFunction(filterFunction);
							copy.setCopyingEntities(includeEntities);

							plugin.getLogger().info("Preparing paste took " + Long.toString(System.currentTimeMillis() - startTime) + " milliseconds"); // STOP -->

							long startTime = System.currentTimeMillis(); // <-- START
							Operations.completeBlindly(copy);
							plugin.getLogger().info("completeBlindly took " + Long.toString(System.currentTimeMillis() - startTime) + " milliseconds"); // STOP -->

							startTime = System.currentTimeMillis(); // <-- START
							extent.flushQueue();
							plugin.getLogger().info("flushQueue took " + Long.toString(System.currentTimeMillis() - startTime) + " milliseconds"); // STOP -->

							/*
							 * Fix lighting after the structure loads (if plugin present)
							 */
							new BukkitRunnable() {
								@Override
								public void run() {
									long startTime = System.currentTimeMillis(); // <-- START
									scheduleLighting(world, to, size);
									plugin.getLogger().info("scheduleLighting took " + Long.toString(System.currentTimeMillis() - startTime) + " milliseconds"); // STOP -->
								}
							}.runTaskLater(plugin, 40);
						}
					}.runTaskLater(plugin, 2);
				}
			}
		}.runTaskTimer(plugin, 0, 1);
	}

	private static final EnumSet<EntityType> keptEntities = EnumSet.of(
		EntityType.PLAYER,
		EntityType.DROPPED_ITEM,
		EntityType.EXPERIENCE_ORB,
		EntityType.IRON_GOLEM,
		EntityType.VILLAGER,
		EntityType.TRIDENT,
		EntityType.HORSE,
		EntityType.COW,
		EntityType.PIG,
		EntityType.SHEEP,
		EntityType.CHICKEN
	);

	private static boolean entityShouldBeRemoved(Entity entity) {
		/* Keep some entity types always */
		if (keptEntities.contains(entity.getType())) {
			return false;
		}

		/* Keep armor stands that have a name, are markers, or have tags */
		if (entity instanceof ArmorStand) {
			ArmorStand stand = (ArmorStand)entity;
			if ((stand.getCustomName() != null && !stand.getCustomName().isEmpty())
				|| stand.isMarker()
				|| (stand.getScoreboardTags() != null && !stand.getScoreboardTags().isEmpty())) {
				return false;
			}
		}

		/* Keep tameable critters that have an owner */
		if (entity instanceof Tameable) {
			Tameable critter = (Tameable)entity;
			if (critter.getOwner() != null) {
				return false;
			}
		}

		/* Remove otherwise */
		return true;
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
