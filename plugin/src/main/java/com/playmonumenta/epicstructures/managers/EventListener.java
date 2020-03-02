package com.playmonumenta.epicstructures.managers;

import java.util.EnumSet;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.playmonumenta.epicstructures.Plugin;
import com.playmonumenta.scriptedquests.zones.zonetree.BaseZoneTree;

public class EventListener implements Listener {
	private static final EnumSet<SpawnReason> DISALLOWED_STRUCTURE_SPAWN_REASONS = EnumSet.of(
		SpawnReason.CHUNK_GEN,
		SpawnReason.NATURAL,
		SpawnReason.VILLAGE_DEFENSE,
		SpawnReason.VILLAGE_INVASION
	);

	Plugin mPlugin = null;

	public EventListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void PlayerInteractEvent(PlayerInteractEvent event) {
		Action action = event.getAction();
		Player player = event.getPlayer();
		ItemStack item = event.getItem();

		// Sneak + right click with compass tells player info about nearby respawning structures
		if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) &&
			item != null && item.getType() == Material.COMPASS && player.isSneaking()) {

			mPlugin.mRespawnManager.tellNearbyRespawnTimes(player);

			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockBreakEvent(BlockBreakEvent event) {
		if (!event.isCancelled()) {
			Block block = event.getBlock();
			if (block.getType() == Material.SPAWNER) {
				mPlugin.mRespawnManager.spawnerBreakEvent(block.getLocation());
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void BlockExplodeEvent(BlockExplodeEvent event) {
		if (!event.isCancelled()) {
			for (Block block : event.blockList()) {
				if (block.getType() == Material.SPAWNER) {
					mPlugin.mRespawnManager.spawnerBreakEvent(block.getLocation());
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void EntityExplodeEvent(EntityExplodeEvent event) {
		if (!event.isCancelled()) {
			for (Block block : event.blockList()) {
				if (block.getType() == Material.SPAWNER) {
					mPlugin.mRespawnManager.spawnerBreakEvent(block.getLocation());
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	void CreatureSpawnEvent(CreatureSpawnEvent event) {
		Entity entity = event.getEntity();
		Vector loc = entity.getLocation().toVector();

		// We need to allow spawning mobs intentionally, but disable natural spawns.
		// It's easier to check the intentional ways than the natural ones.
		if (DISALLOWED_STRUCTURE_SPAWN_REASONS.contains(event.getSpawnReason())) {
			// Only cancel spawns in respawning structures
			String zoneLayerName = mPlugin.mRespawnManager.mZoneLayerName;
			BaseZoneTree<RespawningStructure> zoneTree = mPlugin.mRespawnManager.mZoneTree;

			if (zoneTree.getZone(loc, zoneLayerName) != null) {
				event.setCancelled(true);
				return;
			}
		}
	}
}
