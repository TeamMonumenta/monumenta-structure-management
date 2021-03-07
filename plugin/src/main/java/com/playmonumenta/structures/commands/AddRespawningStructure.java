package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import com.playmonumenta.structures.utils.CommandUtils;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

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

	public static void add(CommandSender sender, Plugin plugin, String label, String path, Location loc, int extraRadius, int respawnTime, String name) throws WrapperCommandSyntaxException {
		CommandUtils.getAndValidateSchematicPath(plugin, path, true);

		try {
			RespawnManager.getInstance().addStructure(extraRadius, label, name, path, loc.toVector(), respawnTime);
		} catch (Exception e) {
			CommandAPI.fail(ChatColor.RED + "Failed to add structure: " + e.getMessage());
		}

		sender.sendMessage("Structure added successfully");
	}
}
