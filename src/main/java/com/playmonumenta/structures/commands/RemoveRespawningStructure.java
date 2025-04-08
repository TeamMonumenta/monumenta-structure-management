package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class RemoveRespawningStructure {
	public static void register() {
		Argument<String> labelArg = new StringArgument("label").replaceSuggestions(RespawnManager.SUGGESTIONS_STRUCTURES);

		new CommandAPICommand("removerespawningstructure")
			.withPermission(CommandPermission.fromString("monumenta.structures"))
			.withArguments(labelArg)
			.executes((sender, args) -> {
				remove(sender, args.getByArgument(labelArg));
			})
			.register();
	}

	private static void remove(CommandSender sender, String label) {
		try {
			RespawnManager.getInstance().removeStructure(label);
		} catch (Exception e) {
			sender.sendMessage(Component.text("Failed to remove structure: " + e.getMessage(), NamedTextColor.RED));
			return;
		}

		sender.sendMessage("Structure '" + label + "' no longer respawns automatically");
	}
}
