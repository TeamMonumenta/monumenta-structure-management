package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.StructuresAPI;
import com.playmonumenta.structures.utils.MessagingUtils;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.wrappers.FunctionWrapper;

public class LoadStructure {
	public static void register() {
		final String command = "loadstructure";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new TextArgument("path"))
			.withArguments(new LocationArgument("position"))
			.executes((sender, args) -> {
				load(sender, (String)args[0], (Location)args[1], false, null);
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new TextArgument("path"))
			.withArguments(new LocationArgument("position"))
			.withArguments(new BooleanArgument("includeEntities"))
			.executes((sender, args) -> {
				load(sender, (String)args[0], (Location)args[1], (Boolean)args[2], null);
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new TextArgument("path"))
			.withArguments(new LocationArgument("position"))
			.withArguments(new BooleanArgument("includeEntities"))
			.withArguments(new FunctionArgument("postLoadFunction"))
			.executes((sender, args) -> {
				load(sender, (String)args[0], (Location)args[1], (Boolean)args[2], (FunctionWrapper[])args[3]);
			})
			.register();
	}

	private static void load(CommandSender sender, String path, Location loadLoc, boolean includeEntities, FunctionWrapper[] postFunc) {
		sender.sendMessage("Started loading structure '" + path + "' at (" + loadLoc.getBlockX() + " " + loadLoc.getBlockY() + " " + loadLoc.getBlockZ() + ")");

		StructuresAPI.loadAndPasteStructure(path, loadLoc, includeEntities).whenComplete((unused, ex) -> {
			if (ex != null) {
				if (sender != null) {
					sender.sendMessage(ChatColor.RED + "Failed to load structure: " + ex.getMessage());
					MessagingUtils.sendStackTrace(sender, ex);
				}
			} else {
				if (postFunc != null) {
					for (FunctionWrapper func : postFunc) {
						func.run();
					}
				}
				if (sender != null) {
					sender.sendMessage("Loaded structure '" + path + "' at (" + loadLoc.getBlockX() + " " + loadLoc.getBlockY() + " " + loadLoc.getBlockZ() + ")");
				}
			}
		});
	}
}
