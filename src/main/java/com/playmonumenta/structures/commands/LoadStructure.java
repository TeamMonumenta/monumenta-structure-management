package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.StructuresAPI;
import com.playmonumenta.structures.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public class LoadStructure {
	public static void register() {
		final String command = "loadstructure";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new TextArgument("path"))
			.withArguments(new LocationArgument("position"))
			.executes((sender, args) -> {
				load(sender, (String)args[0], (Location)args[1], false, false, null);
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new TextArgument("path"))
			.withArguments(new LocationArgument("position"))
			.withArguments(new BooleanArgument("includeEntities"))
			.executes((sender, args) -> {
				load(sender, (String)args[0], (Location)args[1], (Boolean)args[2], false, null);
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new TextArgument("path"))
			.withArguments(new LocationArgument("position"))
			.withArguments(new BooleanArgument("includeEntities"))
			.withArguments(new BooleanArgument("includeBiomes"))
			.executes((sender, args) -> {
				load(sender, (String)args[0], (Location)args[1], (Boolean)args[2], (Boolean)args[3], null);
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new TextArgument("path"))
			.withArguments(new LocationArgument("position"))
			.withArguments(new BooleanArgument("includeEntities"))
			.withArguments(new FunctionArgument("postLoadFunction"))
			.executes((sender, args) -> {
				load(sender, (String)args[0], (Location)args[1], (Boolean)args[2], false, (FunctionWrapper[])args[3]);
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new TextArgument("path"))
			.withArguments(new LocationArgument("position"))
			.withArguments(new BooleanArgument("includeEntities"))
			.withArguments(new BooleanArgument("includeBiomes"))
			.withArguments(new FunctionArgument("postLoadFunction"))
			.executes((sender, args) -> {
				load(sender, (String)args[0], (Location)args[1], (Boolean)args[2], (Boolean)args[3], (FunctionWrapper[])args[4]);
			})
			.register();
	}

	private static void load(CommandSender sender, String path, Location loadLoc, boolean includeEntities, boolean includeBiomes, @Nullable FunctionWrapper[] postFunc) {
		sender.sendMessage("Started loading structure '" + path + "' at (" + loadLoc.getBlockX() + " " + loadLoc.getBlockY() + " " + loadLoc.getBlockZ() + ")");

		StructuresAPI.loadAndPasteStructure(path, loadLoc, includeEntities, includeBiomes).whenComplete((unused, ex) -> {
			if (ex != null) {
				boolean senderLoaded;
				if (sender instanceof Entity entity) {
					senderLoaded = entity.isValid();
				} else if (sender instanceof BlockCommandSender blockSender) {
					senderLoaded = blockSender.getBlock().getLocation().isChunkLoaded();
				} else {
					senderLoaded = true;
				}
				if (senderLoaded) {
					sender.sendMessage(ChatColor.RED + "Failed to load structure: " + ex.getMessage());
					ex.printStackTrace();
					MessagingUtils.sendStackTrace(sender, ex);
				}
			} else {
				if (postFunc != null) {
					for (FunctionWrapper func : postFunc) {
						func.run();
					}
				}
				boolean senderLoaded;
				if (sender instanceof Entity entity) {
					senderLoaded = entity.isValid();
				} else if (sender instanceof BlockCommandSender blockSender) {
					senderLoaded = blockSender.getBlock().getLocation().isChunkLoaded();
				} else {
					senderLoaded = true;
				}
				if (senderLoaded) {
					sender.sendMessage("Loaded structure '" + path + "' at (" + loadLoc.getBlockX() + " " + loadLoc.getBlockY() + " " + loadLoc.getBlockZ() + ")");
				}
			}
		});
	}
}
