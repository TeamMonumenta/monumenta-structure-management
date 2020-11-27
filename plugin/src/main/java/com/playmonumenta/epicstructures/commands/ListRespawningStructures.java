package com.playmonumenta.epicstructures.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.epicstructures.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;

public class ListRespawningStructures {
	public static void register(Plugin plugin) {
		final String command = "listrespawningstructures";
		final CommandPermission perms = CommandPermission.fromString("epicstructures");

		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				list(sender, plugin, null);
			})
			.register();

		arguments.put("label", new StringArgument().overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}));
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
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
