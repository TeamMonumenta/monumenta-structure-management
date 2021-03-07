package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class RespawnStructure {
	public static void register(Plugin plugin) {
		final String command = "respawnstructure";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}))
			.executes((sender, args) -> {
				respawn(sender, plugin, (String)args[0], 600); // Default 30s
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}))
			.withArguments(new IntegerArgument("ticks_until_respawn", 0))
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
