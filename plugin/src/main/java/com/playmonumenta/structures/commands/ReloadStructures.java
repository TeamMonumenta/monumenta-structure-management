package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.Plugin;

import org.bukkit.ChatColor;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;

public class ReloadStructures {
	public static void register(Plugin plugin) {
		new CommandAPICommand("reloadstructures")
			.withPermission(CommandPermission.fromString("monumenta.structures"))
			.executes((sender, args) -> {
				plugin.reloadConfig();
				sender.sendMessage(ChatColor.GREEN + "Structures reloaded");
			})
			.register();
	}
}
