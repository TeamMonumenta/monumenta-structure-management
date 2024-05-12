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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

public class AddRespawningStructure {
	public static void register(Plugin plugin) {
		final String command = "addrespawningstructure";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		StringArgument labelArg = new StringArgument("label");
		TextArgument pathArg = new TextArgument("path"); // TODO: Path arguments autocomplete?
		LocationArgument locationArg = new LocationArgument("location");
		IntegerArgument radiusArg = new IntegerArgument("extraRadius", 0);
		IntegerArgument respawnTimeArg = new IntegerArgument("respawnTime", 20);
		TextArgument nameArg = new TextArgument("name");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(labelArg)
			.withArguments(pathArg)
			.withArguments(locationArg)
			.withArguments(radiusArg)
			.withArguments(respawnTimeArg)
			.withArguments(nameArg)
			.executes((sender, args) -> {
				add(sender, plugin, args.getByArgument(labelArg), args.getByArgument(pathArg), args.getByArgument(locationArg), args.getByArgument(radiusArg), args.getByArgument(respawnTimeArg), args.getByArgument(nameArg));
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

		RespawnManager.getInstance().addStructure(extraRadius, label, name, path, loc.toVector(), respawnTime).whenComplete((unused, ex) -> {
			if (ex != null) {
				sender.sendMessage(Component.text("Failed to add structure: " + ex.getMessage(), NamedTextColor.RED));
			} else {
				sender.sendMessage("Structure added successfully");
			}
		});
	}
}
