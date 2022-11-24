package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class SetPostRespawnCommand {
	public static void register() {
		final String command = "setpostrespawncommand";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").replaceSuggestions((info) -> RespawnManager.getInstance().listStructures()))
			.executes((sender, args) -> {
				setCommand(sender, (String)args[0], null);
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").replaceSuggestions((info) -> RespawnManager.getInstance().listStructures()))
			.withArguments(new TextArgument("command"))
			.executes((sender, args) -> {
				setCommand(sender, (String)args[0], (String)args[1]);
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
			sender.sendMessage(ChatColor.RED + "Got error while attempting to set post respawn command: " + e.getMessage());
			return;
		}

		if (command != null) {
			sender.sendMessage("Successfully set post_respawn_command to '" + command + "'");
		} else {
			sender.sendMessage("Successfully unset post_respawn_command");
		}
	}
}
