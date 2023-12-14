package com.playmonumenta.structures.managers;

import com.playmonumenta.structures.StructuresPlugin;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Location;
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
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class EventListener implements Listener {
	private static final EnumSet<SpawnReason> DISALLOWED_STRUCTURE_SPAWN_REASONS = EnumSet.of(
		SpawnReason.NATURAL,
		SpawnReason.VILLAGE_DEFENSE,
		SpawnReason.VILLAGE_INVASION
	);

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		Action action = event.getAction();
		Player player = event.getPlayer();
		ItemStack item = event.getItem();

		// Sneak + right click with compass tells player info about nearby respawning structures
		if ((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) &&
			item != null && item.getType() == Material.COMPASS && player.isSneaking()) {

			StructuresPlugin.getRespawnManager().tellNearbyRespawnTimes(player);

			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (block.getType() == Material.SPAWNER) {
			StructuresPlugin.getRespawnManager().spawnerBreakEvent(block.getLocation());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void blockExplodeEvent(BlockExplodeEvent event) {
		for (Block block : event.blockList()) {
			if (block.getType() == Material.SPAWNER) {
				StructuresPlugin.getRespawnManager().spawnerBreakEvent(block.getLocation());
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityExplodeEvent(EntityExplodeEvent event) {
		for (Block block : event.blockList()) {
			if (block.getType() == Material.SPAWNER) {
				StructuresPlugin.getRespawnManager().spawnerBreakEvent(block.getLocation());
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void creatureSpawnEvent(CreatureSpawnEvent event) {
		Entity entity = event.getEntity();
		Location loc = entity.getLocation();

		// We need to allow spawning mobs intentionally, but disable natural spawns.
		if (DISALLOWED_STRUCTURE_SPAWN_REASONS.contains(event.getSpawnReason())) {
			// Only cancel spawns in respawning structures
			String zoneLayerNameInside = RespawnManager.ZONE_NAMESPACE_INSIDE;

			// We don't care which poi it is, just that the poi exists at that location
			if (StructuresPlugin.getRespawnManager().mZoneManager.getZone(loc, zoneLayerNameInside) != null) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();
		Vector loc = player.getLocation().toVector();

		if (player.getHealth() > 0 || event.isCancelled()) {
			return;
		}

		List<RespawningStructure> structs = StructuresPlugin.getRespawnManager().getStructures(loc, false);
		if (structs != null) {
			for (RespawningStructure s : structs) {
				if (s.getTicksLeft() < Math.min(20 * 11 * 60, s.getRespawnTime())) {
					s.setRespawnTimer(Math.min(20 * 11 * 60, s.getRespawnTime()));
				}
				if (s.isForced()) {
					s.undoForce();
				}
			}
		}
	}
}
