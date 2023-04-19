package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import com.playmonumenta.structures.utils.CommandUtils;
import com.playmonumenta.structures.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class AddRespawningStructure {
	public static void register(Plugin plugin) {
		final String command = "addrespawningstructure";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label"))
			.withArguments(new TextArgument("path")) // TODO: Path arguments autocomplete?
			.withArguments(new LocationArgument("location"))
			.withArguments(new IntegerArgument("extraRadius", 0))
			.withArguments(new IntegerArgument("respawnTime", 20))
			.withArguments(new TextArgument("name"))
			.executes((sender, args) -> {
				add(sender, plugin, (String)args[0], (String)args[1], (Location)args[2], (Integer)args[3], (Integer)args[4], (String)args[5]);
			})
			.register();
	}

	public static void add(CommandSender sender, Plugin plugin, String label, String path, Location loc, int extraRadius, int respawnTime, String name) {
		try {
			CommandUtils.getAndValidateSchematicPath(plugin, path, true);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to add structure: " + e.getMessage());
			MessagingUtils.sendStackTrace(sender, e);
			return;
		}

		RespawnManager.getInstance().addStructure(extraRadius, label, name, path, loc, respawnTime).whenComplete((unused, ex) -> {
			if (ex != null) {
				sender.sendMessage(ChatColor.RED + "Failed to add structure: " + ex.getMessage());
			} else {
				sender.sendMessage("Structure added successfully");
			}
		});
	}
}
