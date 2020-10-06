package com.playmonumenta.epicstructures.commands;

import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import com.playmonumenta.epicstructures.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;

public class ActivateSpecialStructure {
	private static final Pattern INVALID_PATH_PATTERN = Pattern.compile("[^-/_a-zA-Z0-9]");
	public static void register(Plugin plugin) {
		final String command = "activatespecialstructure";
		final CommandPermission perms = CommandPermission.fromString("epicstructures");

		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new StringArgument().overrideSuggestions((sender) -> {return plugin.mRespawnManager.listStructures();}));
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				activate(sender, plugin, (String)args[0], null);
			})
			.register();

		arguments.put("special_structure_path", new TextArgument());
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				activate(sender, plugin, (String)args[0], (String)args[1]);
			})
			.register();
	}

	private static void activate(CommandSender sender, Plugin plugin, String label, String path) {
		if (path != null && INVALID_PATH_PATTERN.matcher(path).find()) {
			sender.sendMessage(ChatColor.RED + "Path contains illegal characters!");
			return;
		}
		if (plugin.mRespawnManager == null) {
			return;
		}

		try {
			plugin.mRespawnManager.activateSpecialStructure(label, path);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to activate special structure: " + e.getMessage());
		}

		if (path != null) {
			sender.sendMessage("Successfully activated special structure");
		} else {
			sender.sendMessage("Successfully deactivated special structure");
		}
	}
}
