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

public class CompassRespawn {
	public static void register(Plugin plugin) {
		final String command = "compassrespawn";
		final CommandPermission perms = CommandPermission.fromString("epicstructures");

		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new StringArgument().overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}));
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				forceRespawn(sender, plugin, args[0]);
			})
			.register();
	}
	
	private static void forceRespawn(CommandSender sender, Plugin plugin, String label) {
		if (plugin.mRespawnManager == null) {
			return;
		}
		try {
			plugin.mRespawnManager.compassRespawn((Player) sender, true);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to force respawn on structure: " + e.getMessage());
		}
	}
}
