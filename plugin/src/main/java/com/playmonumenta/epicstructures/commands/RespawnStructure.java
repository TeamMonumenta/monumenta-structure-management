package com.playmonumenta.epicstructures.commands;

import com.playmonumenta.epicstructures.Plugin;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class RespawnStructure {
	public static void register(Plugin plugin) {
		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new StringArgument());
		arguments.put("ticks_until_respawn", new IntegerArgument(0));

		CommandAPI.getInstance().register("respawnstructure",
		                                  new CommandPermission("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      respawn(sender, plugin, (String)args[0], (Integer)args[1]);
		                                  }
		);

		arguments.put("label", new StringArgument());

		CommandAPI.getInstance().register("respawnstructure",
		                                  new CommandPermission("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      respawn(sender, plugin, (String)args[0], 600); // Default 30s
		                                  }
		);
	}

	private static void respawn(CommandSender sender, Plugin plugin, String label, Integer ticksUntilRespawn) {
		try {
			plugin.mRespawnManager.setTimer(label, ticksUntilRespawn);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to respawn structure: " + e.getMessage());
		}
	}
}
