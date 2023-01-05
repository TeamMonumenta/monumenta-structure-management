package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class RemoveRespawningStructure {
	public static void register() {
		new CommandAPICommand("removerespawningstructure")
			.withPermission(CommandPermission.fromString("monumenta.structures"))
			.withArguments(new StringArgument("label").replaceSuggestions(RespawnManager.SUGGESTIONS_STRUCTURES))
			.executes((sender, args) -> {
				remove(sender, (String)args[0]);
			})
			.register();
	}

	private static void remove(CommandSender sender, String label) {
		try {
			RespawnManager.getInstance().removeStructure(label);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Failed to remove structure: " + e.getMessage());
			return;
		}

		sender.sendMessage("Structure '" + label + "' no longer respawns automatically");
	}
}
