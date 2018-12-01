package com.playmonumenta.epicstructures.commands;

import com.playmonumenta.epicstructures.Plugin;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.DynamicSuggestedStringArgument;
import io.github.jorelali.commandapi.api.arguments.TextArgument;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class ActivateSpecialStructure {
	private static final Pattern INVALID_PATH_PATTERN = Pattern.compile("[^-/_a-zA-Z0-9]");
	public static void register(Plugin plugin) {
		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new DynamicSuggestedStringArgument(() -> {return plugin.mRespawnManager.listStructures();}));
		CommandAPI.getInstance().register("activatespecialstructure",
		                                  CommandPermission.fromString("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      activate(sender, plugin, (String)args[0], null);
		                                  }
		);

		arguments.put("special_structure_path", new TextArgument());
		CommandAPI.getInstance().register("activatespecialstructure",
		                                  CommandPermission.fromString("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      activate(sender, plugin, (String)args[0], (String)args[1]);
		                                  }
		);
	}

	private static void activate(CommandSender sender, Plugin plugin, String label, String path) {
		if (path != null && INVALID_PATH_PATTERN.matcher(path).find()) {
			sender.sendMessage(ChatColor.RED + "Path contains illegal characters!");
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
