package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class CompassRespawn {
	public static void register() {
		final String command = "compassrespawn";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures.compassrespawn");

		Argument<String> labelArg = new StringArgument("label").replaceSuggestions(RespawnManager.SUGGESTIONS_STRUCTURES);

		new CommandAPICommand(command)
				.withPermission(perms)
				.withArguments(labelArg)
				.executes((sender, args) -> {
					if (sender instanceof Player player) {
						forceRespawn(player, args.getByArgument(labelArg));
					}
				})
				.register();
	}

	private static void forceRespawn(Player player, String label) {
		try {
			RespawnManager.getInstance().compassRespawn(player, label);
		} catch (Exception e) {
			player.sendMessage(Component.text("Got error while attempting to force respawn on structure: " + e.getMessage(), NamedTextColor.RED));
		}
	}
}
