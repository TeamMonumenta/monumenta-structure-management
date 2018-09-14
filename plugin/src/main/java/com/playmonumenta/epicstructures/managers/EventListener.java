package com.playmonumenta.epicstructures.managers;

import com.playmonumenta.epicstructures.Plugin;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

public class EventListener implements Listener {
	Plugin mPlugin = null;

	public EventListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
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

	@EventHandler(priority = EventPriority.HIGHEST)
	public void BlockBreakEvent(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (block.getType() == Material.SPAWNER) {
			mPlugin.mRespawnManager.spawnerBreakEvent(block.getLocation());
		}
	}
}
