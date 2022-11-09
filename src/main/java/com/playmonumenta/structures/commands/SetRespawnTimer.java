package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class SetRespawnTimer {
	public static void register() {
		new CommandAPICommand("setrespawntimer")
			.withPermission(CommandPermission.fromString("monumenta.structures"))
			.withArguments(new StringArgument("label").replaceSuggestions((info) -> RespawnManager.getInstance().listStructures()))
			.withArguments(new IntegerArgument("ticks", 0))
			.executes((sender, args) -> {
				setTimer(sender, (String)args[0], (Integer)args[1]);
			})
			.register();
	}

	private static void setTimer(CommandSender sender, String label, int ticks) {
		try {
			RespawnManager.getInstance().setTimerPeriod(label, ticks);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to set post respawn command: " + e.getMessage());
			return;
		}

		sender.sendMessage("Successfully set respawn timer to '" + ticks + "'");
	}
}
