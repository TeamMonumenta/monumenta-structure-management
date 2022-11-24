package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import com.playmonumenta.structures.managers.SpawnerBreakTrigger;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class SetSpawnerBreakTrigger {
	public static void register() {
		final String command = "setspawnerbreaktrigger";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").replaceSuggestions((info) -> RespawnManager.getInstance().listStructures()))
			.executes((sender, args) -> {
				setTrigger(sender, (String)args[0], 0, null);
			})
			.register();

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(new StringArgument("label").replaceSuggestions((info) -> RespawnManager.getInstance().listStructures()))
			.withArguments(new IntegerArgument("spawner_count", 0))
			.withArguments(new GreedyStringArgument("quest_component"))
			.executes((sender, args) -> {
				setTrigger(sender, (String)args[0], (Integer)args[1], (String)args[2]);
			})
			.register();
	}

	private static void setTrigger(CommandSender sender, String label, int spawnerCount, @Nullable String questComponentStr) {
		try {
			SpawnerBreakTrigger trigger = null;
			if (questComponentStr != null) {
				trigger = new SpawnerBreakTrigger(spawnerCount, spawnerCount, questComponentStr);
			}
			RespawnManager.getInstance().setSpawnerBreakTrigger(label, trigger);
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Got error while attempting to set spawner break trigger: " + e.getMessage());
			return;
		}

		if (questComponentStr != null) {
			sender.sendMessage("Successfully set spawner break trigger");
		} else {
			sender.sendMessage("Successfully unset spawner break trigger");
		}
	}
}
