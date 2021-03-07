package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.StringArgument;

public class RemoveRespawningStructure {
	public static void register(Plugin plugin) {
		new CommandAPICommand("removerespawningstructure")
			.withPermission(CommandPermission.fromString("monumenta.structures"))
			.withArguments(new StringArgument("label").overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}))
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
