package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class SetPostRespawnCommand {
	public static void register() {
		final String command = "setpostrespawncommand";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		Argument<String> labelArg = new StringArgument("label").replaceSuggestions(RespawnManager.SUGGESTIONS_STRUCTURES);
		TextArgument commandArg = new TextArgument("command");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(labelArg)
			.withOptionalArguments(commandArg)
			.executes((sender, args) -> {
				setCommand(sender, args.getByArgument(labelArg), args.getByArgument(commandArg));
			})
			.register();
	}

	private static void setCommand(CommandSender sender, String label, @Nullable String command) {
		if (command != null && command.startsWith("/")) {
			command = command.substring(1);
		}

		try {
			RespawnManager.getInstance().setPostRespawnCommand(label, command);
		} catch (Exception e) {
			sender.sendMessage(Component.text("Got error while attempting to set post respawn command: " + e.getMessage(), NamedTextColor.RED));
			return;
		}

		if (command != null) {
			sender.sendMessage("Successfully set post_respawn_command to '" + command + "'");
		} else {
			sender.sendMessage("Successfully unset post_respawn_command");
		}
	}
}
