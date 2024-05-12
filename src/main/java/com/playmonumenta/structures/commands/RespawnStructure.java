package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class RespawnStructure {
	public static void register() {
		final String command = "respawnstructure";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		Argument<String> labelArg = new StringArgument("label").replaceSuggestions(RespawnManager.SUGGESTIONS_STRUCTURES);
		IntegerArgument ticksUntilRespawnArg = new IntegerArgument("ticks_until_respawn", 0);

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(labelArg)
			.withOptionalArguments(ticksUntilRespawnArg)
			.executes((sender, args) -> {
				respawn(sender, args.getByArgument(labelArg), args.getByArgumentOrDefault(ticksUntilRespawnArg, 600));
			})
			.register();
	}

	private static void respawn(CommandSender sender, String label, Integer ticksUntilRespawn) {
		try {
			RespawnManager.getInstance().setTimer(label, ticksUntilRespawn);
		} catch (Exception e) {
			sender.sendMessage(Component.text("Got error while attempting to respawn structure: " + e.getMessage(), NamedTextColor.RED));
		}
	}
}
