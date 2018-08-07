package com.playmonumenta.epicstructures.managers;

import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import com.playmonumenta.epicstructures.Plugin;

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
}
