package com.playmonumenta.epicstructures.utils;

import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Chest;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.bergerkiller.bukkit.common.wrappers.LongHashSet;
import com.bergerkiller.bukkit.lightcleaner.lighting.LightingService;
import com.boydti.fawe.object.clipboard.FaweClipboard;
import com.boydti.fawe.util.EditSessionBuilder;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.block.BlockStateHolder;
import com.sk89q.worldedit.world.block.BlockTypes;

public class StructureUtils {
	private static final boolean LIGHT_CLEANER_ENABLED = Bukkit.getPluginManager().isPluginEnabled("LightCleaner");

	// Custom paste function copied and modified from
	// FastAsyncWorldedit/core/src/main/java/com/boydti/fawe/object/schematic/Schematic.java
	//
	// Ignores structure void, leaving the original block in place
	public static void paste(Plugin plugin, BlockArrayClipboard clipboard, World world, BlockVector3 to, boolean includeEntities) {
		// TODO: Whatever is going on here... entities are broken IF:
		// fastmode = true (regardless of combine stages setting)
		// fastmode = false AND combineStages = true
		EditSession extent = new EditSessionBuilder(world.getName()).autoQueue(true).fastmode(false).combineStages(false).build();

		Region sourceRegion = clipboard.getRegion().clone();
		final BlockVector3 origin = clipboard.getOrigin();
		final BlockVector3 size = sourceRegion.getMaximumPoint().subtract(sourceRegion.getMinimumPoint());
		final int relx = to.getBlockX();
		final int rely = to.getBlockY();
		final int relz = to.getBlockZ();

		clipboard.IMP.forEach(new FaweClipboard.BlockReader() {
			@Override
			public <B extends BlockStateHolder<B>> void run(int x, int y, int z, B block) {
				if (extent.getBlockType(x + relx, y + rely, z + relz).equals(BlockTypes.CHEST)) {
					Chest chest = (Chest) world.getBlockAt(x + relx, y + rely, z + relz).getState();
					if (chest.getCustomName() != null && chest.getCustomName().endsWith("'s Grave")) {
						// Check if the grave has items inside. If it is empty, it can be overwritten.
						for (ItemStack item : chest.getInventory()) {
							if (item != null) {
								return;
							}
						}
					}
				}
				if (!block.getBlockType().equals(BlockTypes.STRUCTURE_VOID)) {
					extent.setBlock(x + relx, y + rely, z + relz, block);
				}
			}
		}, true);

		if (includeEntities) {
			// entities
			final int entityOffsetX = to.getBlockX() - origin.getBlockX();
			final int entityOffsetY = to.getBlockY() - origin.getBlockY();
			final int entityOffsetZ = to.getBlockZ() - origin.getBlockZ();
			ArrayList<Entity> entityList = new ArrayList<>();
			// parse all entities of the world and put then in a saved list if the entity is in the structure location
			for (Entity e : extent.getEntities()) {
				if (e.getLocation().containedWithin(to.toVector3(), to.add(size).toVector3())){
					entityList.add(e);
				}
			}
			// summon new entities from the clipboard
			for (Entity entity : clipboard.getEntities()) {
				Location pos = entity.getLocation();
				Location newPos = new Location(pos.getExtent(), pos.getX() + entityOffsetX, pos.getY() + entityOffsetY, pos.getZ() + entityOffsetZ, pos.getYaw(), pos.getPitch());
				extent.createEntity(newPos, entity.getState());
			}
			// remove entities of the old list.
			// dont ask why i delete them now, and not earlier. it just wont work if i do anything else
			for (Entity e : entityList) {
				if (entityShouldBeRemoved(e)) {
					e.remove();
				}
			}
		}

		extent.flushQueue();

		/*
		 * Fix lighting after the structure loads (if plugin present)
		 */
		new BukkitRunnable() {
			@Override
			public void run() {
				scheduleLighting(world, to, size);
			}
		}.runTaskLater(plugin, 40);
	}

	public static boolean entityShouldBeRemoved(Entity entity) {
		// i cant seem to be able to use EntityType enum to mach check
		// so im using ID comparaison check
		String type = entity.getType().getName();
		if (type == null) {
			//entity is invalid, should be removed
			return true;
		}

		// entitytypes to be kept
		switch (type) {
			case "minecraft:player":
			case "minecraft:item":
			case "minecraft:experience_orb":
			case "minecraft:iron_golem":
			case "minecraft:villager":
			case "minecraft:trident":
			case "minecraft:horse":
				return false;
		}

		//special cases
		CompoundTag data = entity.getState().getNbtData();
		switch (type) {
			case "minecraft:armor_stand":
				// if the entity is an armorstand and has tags/name, do not remove it
				if (!data.getString("CustomName").isEmpty() || data.getByte("Marker") > 0 || data.getList("Tags").size() > 0) {
					return false;
				}
				break;
			case "minecraft:wolf":
			case "minecraft:ocelot":
				// if the mob is tamed, keep it
				if (!data.getString("OwnerUUID").isEmpty()) {
					return false;
				}
				break;
		}

		//rest is removed
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
