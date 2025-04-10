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

public class SetRespawnTimer {
	@SuppressWarnings("DataFlowIssue")
	public static void register() {
		Argument<String> labelArg = new StringArgument("label").replaceSuggestions(RespawnManager.SUGGESTIONS_STRUCTURES);
		IntegerArgument ticksArg = new IntegerArgument("ticks", 0);

		new CommandAPICommand("setrespawntimer")
				.withPermission(CommandPermission.fromString("monumenta.structures"))
				.withArguments(labelArg)
				.withArguments(ticksArg)
				.executes((sender, args) -> {
					setTimer(sender, args.getByArgument(labelArg), args.getByArgument(ticksArg));
				})
				.register();
	}

	private static void setTimer(CommandSender sender, String label, int ticks) {
		try {
			RespawnManager.getInstance().setTimerPeriod(label, ticks);
		} catch (Exception e) {
			sender.sendMessage(Component.text("Got error while attempting to set post respawn command: " + e.getMessage(), NamedTextColor.RED));
			return;
		}

		sender.sendMessage(Component.text("Successfully set respawn timer to '" + ticks + "'"));
	}
}
