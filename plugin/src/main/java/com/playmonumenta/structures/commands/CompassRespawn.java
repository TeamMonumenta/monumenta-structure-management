package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.StringArgument;

public class CompassRespawn {
	public static void register(Plugin plugin) {
		final String command = "compassrespawn";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures.compassrespawn");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}))
			.executes((sender, args) -> {
				if (sender instanceof Player) {
					forceRespawn(sender, plugin);
				}
			})
			.register();
	}

	private static void forceRespawn(CommandSender sender, Plugin plugin) {
		if (plugin.mRespawnManager == null) {
			return;
		}
		try {
			Player player = (Player) sender;
			if (player.hasMetadata("ForceResetPOI")) {
				String label = (String)player.getMetadata("ForceResetPOI").get(0).value();
				plugin.mRespawnManager.compassRespawn(player, label);
			} else {
				player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Waited too long!");
			}

		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to force respawn on structure: " + e.getMessage());
		}
	}
}
