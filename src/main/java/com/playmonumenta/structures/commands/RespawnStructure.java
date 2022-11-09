package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class RespawnStructure {
	public static void register() {
		final String command = "respawnstructure";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").replaceSuggestions((info) -> RespawnManager.getInstance().listStructures()))
			.executes((sender, args) -> {
				respawn(sender, (String)args[0], 600); // Default 30s
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").replaceSuggestions((info) -> RespawnManager.getInstance().listStructures()))
			.withArguments(new IntegerArgument("ticks_until_respawn", 0))
			.executes((sender, args) -> {
				respawn(sender, (String)args[0], (Integer)args[1]);
			})
			.register();
	}

	private static void respawn(CommandSender sender, String label, Integer ticksUntilRespawn) {
		try {
			RespawnManager.getInstance().setTimer(label, ticksUntilRespawn);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to respawn structure: " + e.getMessage());
		}
	}
}
