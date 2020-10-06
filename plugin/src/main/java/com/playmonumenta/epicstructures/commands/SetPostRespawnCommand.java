package com.playmonumenta.epicstructures.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.epicstructures.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;

public class SetPostRespawnCommand {
	public static void register(Plugin plugin) {
		final String command = "setpostrespawncommand";
		final CommandPermission perms = CommandPermission.fromString("epicstructures");

		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new StringArgument().overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}));
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				setcommand(sender, plugin, (String)args[0], null);
			})
			.register();

		arguments.put("command", new TextArgument());
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				setcommand(sender, plugin, (String)args[0], (String)args[1]);
			})
			.register();
	}

	private static void setcommand(CommandSender sender, Plugin plugin, String label, String command) {
		if (plugin.mRespawnManager == null) {
			return;
		}
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
