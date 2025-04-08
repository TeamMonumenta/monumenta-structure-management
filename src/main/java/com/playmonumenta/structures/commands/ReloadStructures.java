package com.playmonumenta.structures.commands;

import com.playmonumenta.structures.StructuresPlugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ReloadStructures {
	public static void register(StructuresPlugin plugin) {
		new CommandAPICommand("reloadstructures")
			.withPermission(CommandPermission.fromString("monumenta.structures"))
			.executes((sender, args) -> {
				plugin.reloadConfig();
				sender.sendMessage(Component.text("Structures reloaded", NamedTextColor.GREEN));
			})
			.register();
	}
}
