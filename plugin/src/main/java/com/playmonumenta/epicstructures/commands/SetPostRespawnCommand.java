package com.playmonumenta.epicstructures.commands;

import com.playmonumenta.epicstructures.Plugin;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.DynamicSuggestedStringArgument;
import io.github.jorelali.commandapi.api.arguments.TextArgument;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class SetPostRespawnCommand {
	public static void register(Plugin plugin) {
		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new DynamicSuggestedStringArgument(() -> {return plugin.mRespawnManager.listStructures();}));
		CommandAPI.getInstance().register("setpostrespawncommand",
		                                  new CommandPermission("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      setcommand(sender, plugin, (String)args[0], null);
		                                  }
		);

		arguments.put("command", new TextArgument());
		CommandAPI.getInstance().register("setpostrespawncommand",
		                                  new CommandPermission("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      setcommand(sender, plugin, (String)args[0], (String)args[1]);
		                                  }
		);
	}

	private static void setcommand(CommandSender sender, Plugin plugin, String label, String command) {
		if (command != null && command.startsWith("/")) {
			command = command.substring(1);
		}

		try {
			plugin.mRespawnManager.setPostRespawnCommand(label, command);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to set post respawn command: " + e.getMessage());
			return;
		}

		if (command != null) {
			sender.sendMessage("Successfully set post_respawn_command to '" + command + "'");
		} else {
			sender.sendMessage("Successfully unset post_respawn_command");
		}
	}
}
