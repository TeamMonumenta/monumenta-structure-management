package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class ListRespawningStructures {
	public static void register() {
		final String command = "listrespawningstructures";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		Argument<String> labelArg = new StringArgument("label").replaceSuggestions(RespawnManager.SUGGESTIONS_STRUCTURES);

		new CommandAPICommand(command)
			.withPermission(perms)
			.withOptionalArguments(labelArg)
			.executes((sender, args) -> {
				list(sender, args.getByArgument(labelArg));
			})
			.register();
	}

	private static void list(CommandSender sender, @Nullable String label) {
		if (label == null) {
			RespawnManager.getInstance().listStructures(sender);
		} else {
			try {
				RespawnManager.getInstance().structureInfo(sender, label);
			} catch (Exception e) {
				sender.sendMessage(Component.text("Got error while attempting to get structure info: " + e.getMessage(), NamedTextColor.RED));
			}
		}
	}
}
