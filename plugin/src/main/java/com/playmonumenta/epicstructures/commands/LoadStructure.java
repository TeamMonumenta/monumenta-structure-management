package com.playmonumenta.epicstructures.commands;

import com.playmonumenta.epicstructures.Plugin;
import com.playmonumenta.epicstructures.utils.CommandUtils;
import com.playmonumenta.epicstructures.utils.MessagingUtils;
import com.playmonumenta.epicstructures.utils.StructureUtils;

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.Vector;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.LocationArgument;
import io.github.jorelali.commandapi.api.arguments.TextArgument;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;

public class LoadStructure {
	private static final Pattern INVALID_PATH_PATTERN = Pattern.compile("[^-/_a-zA-Z0-9]");
	public static void register(Plugin plugin, World world) {
		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("path", new TextArgument());
		arguments.put("position", new LocationArgument());

		CommandAPI.getInstance().register("loadstructure",
		                                  CommandPermission.fromString("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      load(sender, plugin, world, (String)args[0], (Location)args[1]);
		                                  }
		);
	}

	private static void load(CommandSender sender, Plugin plugin, World world,
	                         String path, Location loadLoc) {
		if (INVALID_PATH_PATTERN.matcher(path).find()) {
			sender.sendMessage(ChatColor.RED + "Path contains illegal characters!");
			return;
		}

		Vector loadPos = new Vector(loadLoc.getBlockX(), loadLoc.getBlockY(), loadLoc.getBlockZ());

		BlockArrayClipboard clipboard;
		try {
			clipboard = plugin.mStructureManager.loadSchematic(path);
		} catch (Exception e) {
			plugin.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();

			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Failed to load structure");
				MessagingUtils.sendStackTrace(sender, e);
			}
			return;
		}

		StructureUtils.paste(plugin, clipboard, world, loadPos);

		sender.sendMessage("Loaded structure '" + path + "' at " + loadPos);
	}
}
