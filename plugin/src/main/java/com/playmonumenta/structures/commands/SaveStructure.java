package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.StructuresPlugin;
import com.playmonumenta.structures.utils.MessagingUtils;
import com.sk89q.worldedit.math.BlockVector3;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class SaveStructure {
	public static void register(StructuresPlugin plugin) {
		new CommandAPICommand("savestructure")
			.withPermission(CommandPermission.fromString("monumenta.structures"))
			.withArguments(new TextArgument("path"))
			.withArguments(new LocationArgument("pos1"))
			.withArguments(new LocationArgument("pos2"))
			.executes((sender, args) -> {
				save(sender, plugin, (Location)args[1], (Location)args[2], (String)args[0]);
			})
			.register();
	}

	private static void save(CommandSender sender, StructuresPlugin plugin, Location loc1, Location loc2, String path) throws WrapperCommandSyntaxException {
		if (path.contains("..")) {
			CommandAPI.fail("Paths containing '..' are not allowed");
		}

		// Parse the coordinates of the structure to save
		BlockVector3 pos1 = BlockVector3.at(loc1.getBlockX(), loc1.getBlockY(), loc1.getBlockZ());
		BlockVector3 pos2 = BlockVector3.at(loc2.getBlockX(), loc2.getBlockY(), loc2.getBlockZ());

		BlockVector3 minpos = pos1.getMinimum(pos2);
		BlockVector3 maxpos = pos1.getMaximum(pos2);

		// Save it
		try {
			plugin.mStructureManager.saveSchematic(path, minpos, maxpos, loc1.getWorld());
		} catch (Exception e) {
			plugin.getLogger().severe("Caught exception saving structure: " + e.getMessage());
			e.printStackTrace();

			MessagingUtils.sendStackTrace(sender, e);
			CommandAPI.fail("Failed to save structure" + e.getMessage());
		}

		sender.sendMessage("Saved structure '" + path + "'");
	}
}
