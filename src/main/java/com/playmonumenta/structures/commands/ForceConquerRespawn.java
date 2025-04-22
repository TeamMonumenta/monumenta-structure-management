package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import com.playmonumenta.structures.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class ForceConquerRespawn {
	public static void register() {
		final String command = "forceconquerrespawn";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures.forceconquerrespawn");

		Argument<String> labelArg = new StringArgument("label").replaceSuggestions(RespawnManager.SUGGESTIONS_STRUCTURES);
		EntitySelectorArgument.OnePlayer playerArg = new EntitySelectorArgument.OnePlayer("player");

		new CommandAPICommand(command)
				.withPermission(perms)
				.withArguments(labelArg)
				.executesPlayer((sender, args) -> {
					forceRespawn(sender, args.getByArgument(labelArg));
				})
				.register();

		new CommandAPICommand(command)
				.withPermission(perms)
				.withArguments(labelArg)
				.withArguments(playerArg)
				.executes((sender, args) -> {
					forceRespawn(args.getByArgument(playerArg), args.getByArgument(labelArg));
				})
				.register();
	}

	private static void forceRespawn(Player sender, String label) {
		try {
			RespawnManager.getInstance().forceConquerRespawn(label);
		} catch (Exception e) {
			sender.sendMessage(Component.text("Got error while attempting to force respawn on structure: " + e.getMessage(), NamedTextColor.RED));
			MessagingUtils.sendStackTrace(sender, e);
		}
	}
}
