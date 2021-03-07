package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.StructuresAPI;
import com.playmonumenta.structures.StructuresPlugin;
import com.playmonumenta.structures.utils.MessagingUtils;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

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

	private static void save(CommandSender sender, Location loc1, Location loc2, String path) throws WrapperCommandSyntaxException {
		try {
			StructuresAPI.save(path, loc1, loc2);
			sender.sendMessage("Saved structure '" + path + "'");
		} catch (Exception e) {
			StructuresPlugin.getInstance().getLogger().severe("Caught exception saving structure: " + e.getMessage());
			e.printStackTrace();

			MessagingUtils.sendStackTrace(sender, e);
			CommandAPI.fail("Failed to save structure" + e.getMessage());
		}
	}
}
