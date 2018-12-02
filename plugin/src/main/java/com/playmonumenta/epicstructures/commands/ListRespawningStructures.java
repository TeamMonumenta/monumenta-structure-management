package com.playmonumenta.epicstructures.commands;

import com.playmonumenta.epicstructures.Plugin;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.DynamicSuggestedStringArgument;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ListRespawningStructures {
	public static void register(Plugin plugin) {
		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		CommandAPI.getInstance().register("listrespawningstructures",
		                                  CommandPermission.fromString("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      list(sender, plugin, null);
		                                  }
		);

		arguments.put("label", new DynamicSuggestedStringArgument(() -> {return plugin.mRespawnManager.listStructures();}));
		CommandAPI.getInstance().register("listrespawningstructures",
		                                  CommandPermission.fromString("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      list(sender, plugin, (String)args[0]);
		                                  }
		);
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
