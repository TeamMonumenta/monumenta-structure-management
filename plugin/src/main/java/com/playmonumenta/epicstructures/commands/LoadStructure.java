package com.playmonumenta.epicstructures.commands;

import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.epicstructures.Plugin;
import com.playmonumenta.epicstructures.utils.MessagingUtils;
import com.playmonumenta.epicstructures.utils.StructureUtils;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.math.BlockVector3;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.BooleanArgument;
import io.github.jorelali.commandapi.api.arguments.LocationArgument;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

public class LoadStructure {
	private static final Pattern INVALID_PATH_PATTERN = Pattern.compile("[^-/_a-zA-Z0-9]");
	public static void register(Plugin plugin) {
		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("path", new TextArgument());
		arguments.put("position", new LocationArgument());

		CommandAPI.getInstance().register("loadstructure",
		                                  CommandPermission.fromString("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      load(sender, plugin, (String)args[0], (Location)args[1], false);
		                                  }
		);

		arguments.put("includeEntities", new BooleanArgument());

		CommandAPI.getInstance().register("loadstructure",
		                                  CommandPermission.fromString("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      load(sender, plugin, (String)args[0], (Location)args[1], (Boolean)args[2]);
		                                  }
		);
	}

	private static void load(CommandSender sender, Plugin plugin, String path, Location loadLoc, boolean includeEntities) {
		if (INVALID_PATH_PATTERN.matcher(path).find()) {
			sender.sendMessage(ChatColor.RED + "Path contains illegal characters!");
			return;
		}
		if (plugin.mStructureManager == null || plugin.mWorld == null) {
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
						StructureUtils.paste(plugin, clipboard, plugin.mWorld, loadPos, includeEntities);

						if (sender != null) {
							sender.sendMessage("Loaded structure '" + path + "' at " + loadPos);
						}
					}
				}.runTask(plugin);
			}
		}.runTaskAsynchronously(plugin);
	}
}
