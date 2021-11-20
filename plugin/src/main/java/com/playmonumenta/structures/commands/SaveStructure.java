package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.StructuresAPI;
import com.playmonumenta.structures.StructuresPlugin;
import com.playmonumenta.structures.utils.MessagingUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.TextArgument;

public class SaveStructure {
	public static void register() {
		new CommandAPICommand("savestructure")
			.withPermission(CommandPermission.fromString("monumenta.structures"))
			.withArguments(new TextArgument("path"))
			.withArguments(new LocationArgument("pos1"))
			.withArguments(new LocationArgument("pos2"))
			.executes((sender, args) -> {
				save(sender, (Location)args[1], (Location)args[2], (String)args[0]);
			})
			.register();
	}

	private static void save(CommandSender sender, Location loc1, Location loc2, String path) {
		sender.sendMessage("Started saving structure '" + path + "'");
		Bukkit.getScheduler().runTaskAsynchronously(StructuresPlugin.getInstance(), () -> {
			try {
				StructuresAPI.copyAreaAndSaveStructure(path, loc1, loc2).get();
				Bukkit.getScheduler().runTask(StructuresPlugin.getInstance(), () -> {
					sender.sendMessage("Saved structure '" + path + "'");
				});
			} catch (Exception e) {
				Bukkit.getScheduler().runTask(StructuresPlugin.getInstance(), () -> {
					sender.sendMessage("Failed to save structure" + e.getMessage());
					StructuresPlugin.getInstance().getLogger().severe("Caught exception saving structure: " + e.getMessage());
					e.printStackTrace();
					MessagingUtils.sendStackTrace(sender, e);
				});
			}
		});
	}
}
