package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CompassRespawn {
	public static void register() {
		final String command = "compassrespawn";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures.compassrespawn");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").replaceSuggestions((info) -> RespawnManager.getInstance().listStructures()))
			.executes((sender, args) -> {
				if (sender instanceof Player) {
					forceRespawn(sender, (String)args[0]);
				}
			})
			.register();
	}

	private static void forceRespawn(CommandSender sender, String label) {
		try {
			Player player = (Player) sender;
			RespawnManager.getInstance().compassRespawn(player, label);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to force respawn on structure: " + e.getMessage());
		}
	}
}
