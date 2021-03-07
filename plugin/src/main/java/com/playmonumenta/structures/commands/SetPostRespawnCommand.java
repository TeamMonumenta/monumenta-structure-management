package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;

public class SetPostRespawnCommand {
	public static void register(Plugin plugin) {
		final String command = "setpostrespawncommand";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}))
			.executes((sender, args) -> {
				setcommand(sender, plugin, (String)args[0], null);
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}))
			.withArguments(new TextArgument("command"))
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
