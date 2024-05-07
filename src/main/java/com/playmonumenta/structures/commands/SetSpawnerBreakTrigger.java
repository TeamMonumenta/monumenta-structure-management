package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.managers.RespawnManager;
import com.playmonumenta.structures.managers.SpawnerBreakTrigger;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class SetSpawnerBreakTrigger {
	public static void register() {
		final String command = "setspawnerbreaktrigger";
		final CommandPermission perms = CommandPermission.fromString("monumenta.structures");

		Argument<String> labelArg = new StringArgument("label").replaceSuggestions(RespawnManager.SUGGESTIONS_STRUCTURES);
		IntegerArgument spawnerCountArg = new IntegerArgument("spawner_count", 0);
		GreedyStringArgument questComponentArg = new GreedyStringArgument("quest_component");

		new CommandAPICommand(command)
			.withPermission(perms)
			.withArguments(labelArg)
			.withOptionalArguments(spawnerCountArg)
			.withOptionalArguments(questComponentArg)
			.executes((sender, args) -> {
				setTrigger(sender, args.getByArgument(labelArg), args.getByArgumentOrDefault(spawnerCountArg, 0), args.getByArgument(questComponentArg));
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
			sender.sendMessage(Component.text("Got error while attempting to set spawner break trigger: " + e.getMessage(), NamedTextColor.RED));
			return;
		}

		if (questComponentStr != null) {
			sender.sendMessage("Successfully set spawner break trigger");
		} else {
			sender.sendMessage("Successfully unset spawner break trigger");
		}
	}
}
