package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.StructuresAPI;
import com.playmonumenta.structures.StructuresPlugin;
import com.playmonumenta.structures.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

public class SaveStructure {
	public static void register() {
		TextArgument pathArg = new TextArgument("path");
		LocationArgument pos1Arg = new LocationArgument("pos1");
		LocationArgument pos2Arg = new LocationArgument("pos2");

		new CommandAPICommand("savestructure")
				.withPermission(CommandPermission.fromString("monumenta.structures"))
				.withArguments(pathArg)
				.withArguments(pos1Arg)
				.withArguments(pos2Arg)
				.executes((sender, args) -> {
					save(sender, args.getByArgument(pos1Arg), args.getByArgument(pos2Arg), args.getByArgument(pathArg));
				})
				.register();
	}

	private static void save(CommandSender sender, Location loc1, Location loc2, String path) {
		sender.sendMessage(Component.text("Started saving structure '" + path + "'"));
		Bukkit.getScheduler().runTaskAsynchronously(StructuresPlugin.getInstance(), () -> {
			try {
				StructuresAPI.copyAreaAndSaveStructure(path, loc1, loc2).get();
				Bukkit.getScheduler().runTask(StructuresPlugin.getInstance(), () -> sender.sendMessage(Component.text("Saved structure '" + path + "'")));
			} catch (Exception e) {
				Bukkit.getScheduler().runTask(StructuresPlugin.getInstance(), () -> {
					sender.sendMessage(Component.text("Failed to save structure" + e.getMessage()));
					StructuresPlugin.getInstance().getLogger().severe("Caught exception saving structure: " + e.getMessage());
					MessagingUtils.sendStackTrace(sender, e);
				});
			}
		});
	}
}
