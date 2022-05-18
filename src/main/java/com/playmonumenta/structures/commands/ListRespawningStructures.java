package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.StringArgument;

public class ListRespawningStructures {
	public static void register() {
		final String command = "listrespawningstructures";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		new CommandAPICommand(command)
			.withPermission(perms)
			.executes((sender, args) -> {
				list(sender, null);
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").overrideSuggestions((sender) -> {return RespawnManager.getInstance().listStructures();}))
			.executes((sender, args) -> {
				list(sender, (String)args[0]);
			})
			.register();
	}

	private static void list(CommandSender sender, String label) {
		if (label == null) {
			RespawnManager.getInstance().listStructures(sender);
		} else {
			try {
				RespawnManager.getInstance().structureInfo(sender, label);
			} catch (Exception e) {
				sender.sendMessage(ChatColor.RED + "Got error while attempting to get structure info: " + e.getMessage());
				return;
			}
		}
	}
}
