package com.playmonumenta.epicstructures.commands;

import com.playmonumenta.epicstructures.Plugin;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.DynamicSuggestedStringArgument;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class RemoveRespawningStructure {
	public static void register(Plugin plugin) {
		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new DynamicSuggestedStringArgument(() -> {return plugin.mRespawnManager.listStructures();}));
		CommandAPI.getInstance().register("removerespawningstructures",
		                                  CommandPermission.fromString("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      remove(sender, plugin, (String)args[0]);
		                                  }
		);
	}

	private static void remove(CommandSender sender, Plugin plugin, String label) {
		try {
			plugin.mRespawnManager.removeStructure(label);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to remove structure: " + e.getMessage());
			return;
		}

		sender.sendMessage("Structure '" + label + "' no longer respawns automatically");
	}
}
