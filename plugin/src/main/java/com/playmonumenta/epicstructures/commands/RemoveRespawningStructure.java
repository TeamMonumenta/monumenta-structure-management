package com.playmonumenta.epicstructures.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.epicstructures.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;

public class RemoveRespawningStructure {
	public static void register(Plugin plugin) {
		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new StringArgument().overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}));
		new CommandAPICommand("removerespawningstructure")
			.withPermission(CommandPermission.fromString("epicstructures"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				remove(sender, plugin, (String)args[0]);
			})
			.register();
	}

	private static void remove(CommandSender sender, Plugin plugin, String label) {
		if (plugin.mRespawnManager == null) {
			return;
		}
		try {
			plugin.mRespawnManager.removeStructure(label);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to remove structure: " + e.getMessage());
			return;
		}

		sender.sendMessage("Structure '" + label + "' no longer respawns automatically");
	}
}
