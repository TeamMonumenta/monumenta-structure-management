package com.playmonumenta.epicstructures.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.epicstructures.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class RespawnStructure {
	public static void register(Plugin plugin) {
		final String command = "respawnstructure";
		final CommandPermission perms = CommandPermission.fromString("epicstructures");

		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new StringArgument().overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}));
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				respawn(sender, plugin, (String)args[0], 600); // Default 30s
			})
			.register();

		arguments.put("ticks_until_respawn", new IntegerArgument(0));
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				respawn(sender, plugin, (String)args[0], (Integer)args[1]);
			})
			.register();
	}

	private static void respawn(CommandSender sender, Plugin plugin, String label, Integer ticksUntilRespawn) {
		if (plugin.mRespawnManager == null) {
			return;
		}
		try {
			plugin.mRespawnManager.setTimer(label, ticksUntilRespawn);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to respawn structure: " + e.getMessage());
		}
	}
}
