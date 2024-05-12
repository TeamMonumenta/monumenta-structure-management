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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public class LoadStructure {
	public static void register() {
		final String command = "loadstructure";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		TextArgument pathArg = new TextArgument("path");
		LocationArgument positionArg = new LocationArgument("position");
		BooleanArgument includeEntitiesArg = new BooleanArgument("includeEntities");
		BooleanArgument includeBiomesArg = new BooleanArgument("includeBiomes");
		FunctionArgument functionArg = new FunctionArgument("postLoadFunction");

		// Skips biomeArg
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(pathArg)
			.withArguments(positionArg)
			.withArguments(includeEntitiesArg)
			.withArguments(functionArg)
			.executes((sender, args) -> {
				load(sender, args.getByArgument(pathArg), args.getByArgument(positionArg), args.getByArgumentOrDefault(includeEntitiesArg, false), false, args.getByArgument(functionArg));
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(pathArg)
			.withArguments(positionArg)
			.withOptionalArguments(includeEntitiesArg)
			.withOptionalArguments(includeBiomesArg)
			.withOptionalArguments(functionArg)
			.executes((sender, args) -> {
				load(sender, args.getByArgument(pathArg), args.getByArgument(positionArg), args.getByArgumentOrDefault(includeEntitiesArg, false), args.getByArgumentOrDefault(includeBiomesArg, false), args.getByArgument(functionArg));
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
					sender.sendMessage(Component.text("Failed to load structure: " + ex.getMessage(), NamedTextColor.RED));
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
