package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.StringArgument;

public class ListRespawningStructures {
	public static void register(Plugin plugin) {
		final String command = "listrespawningstructures";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		new CommandAPICommand(command)
			.withPermission(perms)
			.executes((sender, args) -> {
				list(sender, plugin, null);
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}))
			.executes((sender, args) -> {
				list(sender, plugin, (String)args[0]);
			})
			.register();
	}

	private static void list(CommandSender sender, Plugin plugin, String label) {
		if (plugin.mRespawnManager == null) {
			return;
		}
		if (label == null) {
			plugin.mRespawnManager.listStructures(sender);
		} else {
			try {
				plugin.mRespawnManager.structureInfo(sender, label);
			} catch (Exception e) {
				sender.sendMessage(ChatColor.RED + "Got error while attempting to get structure info: " + e.getMessage());
				return;
			}
		}
	}
}
