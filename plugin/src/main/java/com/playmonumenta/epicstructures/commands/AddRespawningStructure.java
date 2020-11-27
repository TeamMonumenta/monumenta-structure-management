package com.playmonumenta.epicstructures.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.epicstructures.Plugin;
import com.playmonumenta.epicstructures.utils.CommandUtils;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class AddRespawningStructure {
	public static void register(Plugin plugin) {
		final String command = "addrespawningstructure";
		final CommandPermission perms = CommandPermission.fromString("epicstructures");

		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("label", new StringArgument());
		arguments.put("path", new TextArgument()); // TODO: Path arguments autocomplete?
		arguments.put("location", new LocationArgument());
		arguments.put("extraRadius", new IntegerArgument(0));
		arguments.put("respawnTime", new IntegerArgument(20));
		arguments.put("name", new TextArgument());
		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				add(sender, plugin, (String)args[0], (String)args[1], (Location)args[2], (Integer)args[3], (Integer)args[4], (String)args[5]);
			})
			.register();
	}

	public static void add(CommandSender sender, Plugin plugin, String label, String path, Location loc, int extraRadius, int respawnTime, String name) throws WrapperCommandSyntaxException {
		CommandUtils.getAndValidateSchematicPath(plugin, path, true);

		try {
			plugin.mRespawnManager.addStructure(extraRadius, label, name, path, loc.toVector(), respawnTime);
		} catch (Exception e) {
			CommandAPI.fail(ChatColor.RED + "Failed to add structure: " + e.getMessage());
		}

		sender.sendMessage("Structure added successfully");
	}
}
