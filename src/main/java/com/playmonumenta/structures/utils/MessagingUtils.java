package com.playmonumenta.structures.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;

public class MessagingUtils {
	public static void sendStackTrace(CommandSender sender, Throwable e) {
		TextComponent formattedMessage;
		String errorMessage = e.getLocalizedMessage();
		formattedMessage = new TextComponent(Objects.requireNonNullElse(errorMessage, "An error occurred without a set message. Hover for stack trace."));
		formattedMessage.setColor(ChatColor.RED);

		// Get the first 300 characters of the stacktrace and send them to the player
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String sStackTrace = sw.toString();
		sStackTrace = sStackTrace.substring(0, Math.min(sStackTrace.length(), 300));

		BaseComponent[] textStackTrace = new ComponentBuilder(sStackTrace.replace("\t",
			"  ")).color(ChatColor.RED).create();
		formattedMessage.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, textStackTrace));
		sender.spigot().sendMessage(formattedMessage);
	}

	public static String durationToString(int ticks) {
		if (ticks <= 0) {
			return "0 seconds";
		}

		int minutes = ticks / (60 * 20);
		int seconds = (ticks / 20) % 60;

		StringBuilder text = new StringBuilder();
		if (minutes > 1) {
			text.append(minutes).append(" minutes");
		} else if (minutes == 1) {
			text.append("1 minute");
		}

		if (minutes > 0 && seconds > 0) {
			text.append(" and ");
		}

		if (seconds > 1) {
			text.append(seconds).append(" seconds");
		} else if (seconds == 1) {
			text.append("1 second");
		}

		return text.toString();
	}

}
