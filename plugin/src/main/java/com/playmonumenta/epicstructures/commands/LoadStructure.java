package com.playmonumenta.epicstructures.commands;

import com.playmonumenta.epicstructures.Plugin;
import com.playmonumenta.epicstructures.utils.CommandUtils;
import com.playmonumenta.epicstructures.utils.MessagingUtils;
import com.playmonumenta.epicstructures.utils.StructureUtils;

import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.Vector;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.LocationArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.LinkedHashMap;

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
	public static void register(Plugin plugin, World world) {
		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("path", new StringArgument());
		arguments.put("position", new LocationArgument());

		CommandAPI.getInstance().register("loadstructure",
		                                  new CommandPermission("epicstructures"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      load(sender, plugin, world, (String)args[0], (Location)args[1]);
		                                  }
		);
	}

	private static void load(CommandSender sender, Plugin plugin, World world,
	                         String path, Location loadLoc) {
		if (path.contains("..")) {
			sender.sendMessage(ChatColor.RED + "Paths containing '..' are not allowed");
			return;
		}

		Vector loadPos = new Vector(loadLoc.getBlockX(), loadLoc.getBlockY(), loadLoc.getBlockZ());

		BlockArrayClipboard clipboard;
		try {
			clipboard = plugin.mStructureManager.loadSchematicClipboard("structures", path);
		} catch (Exception e) {
			plugin.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();

			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Failed to load structure");
				MessagingUtils.sendStackTrace(sender, e);
			}
			return;
		}

		StructureUtils.paste(clipboard, world, loadPos);

		sender.sendMessage("Loaded structure '" + path + "' at " + loadPos);
	}
}
