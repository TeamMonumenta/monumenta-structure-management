package com.playmonumenta.epicstructures.commands;

import java.util.logging.Level;

import com.playmonumenta.epicstructures.Plugin;
import com.playmonumenta.epicstructures.utils.CommandUtils;
import com.playmonumenta.epicstructures.utils.MessagingUtils;
import com.playmonumenta.epicstructures.utils.StructureUtils;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.math.BlockVector3;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class LoadStructure {
	public static void register(Plugin plugin) {
		final String command = "loadstructure";
		final CommandPermission perms = CommandPermission.fromString("epicstructures");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new TextArgument("path"))
			.withArguments(new LocationArgument("position"))
			.executes((sender, args) -> {
				load(sender, plugin, (String)args[0], (Location)args[1], false);
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new TextArgument("path"))
			.withArguments(new LocationArgument("position"))
			.withArguments(new BooleanArgument("includeEntities"))
			.executes((sender, args) -> {
				load(sender, plugin, (String)args[0], (Location)args[1], (Boolean)args[2]);
			})
			.register();
	}

	private static void load(CommandSender sender, Plugin plugin, String path, Location loadLoc, boolean includeEntities) throws WrapperCommandSyntaxException {
		CommandUtils.getAndValidateSchematicPath(plugin, path, true);

		if (plugin.mStructureManager == null) {
			return;
		}

		BlockVector3 loadPos = BlockVector3.at(loadLoc.getBlockX(), loadLoc.getBlockY(), loadLoc.getBlockZ());

		// Load the schematic asynchronously (this might access the disk!)
		// Then switch back to the main thread to initiate pasting
		new BukkitRunnable() {
			@Override
			public void run() {
				final BlockArrayClipboard clipboard;

				try {
					clipboard = plugin.mStructureManager.loadSchematic(path);
				} catch (Exception e) {
					plugin.asyncLog(Level.SEVERE, "Failed to load schematic '" + path + "'", e);

					if (sender != null) {
						sender.sendMessage(ChatColor.RED + "Failed to load structure");
						MessagingUtils.sendStackTrace(sender, e);
					}
					return;
				}

				/* Once the schematic is loaded, this task is used to paste it */
				new BukkitRunnable() {
					@Override
					public void run() {
						StructureUtils.paste(plugin, clipboard, loadLoc.getWorld(), loadPos, includeEntities);

						if (sender != null) {
							sender.sendMessage("Loaded structure '" + path + "' at " + loadPos);
						}
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}
}
