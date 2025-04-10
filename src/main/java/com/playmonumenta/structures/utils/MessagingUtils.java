package com.playmonumenta.structures.utils;

import com.playmonumenta.structures.StructuresPlugin;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.command.CommandSender;

public class MessagingUtils {
	public static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();
	public static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

	public static String plainText(Component formattedText) {
		// This is only legacy text because we have a bunch of section symbols lying around that need to be updated.
		String legacyText = PLAIN_SERIALIZER.serialize(formattedText);
		return plainFromLegacy(legacyText);
	}

	public static String plainFromLegacy(String legacyText) {
		return PLAIN_SERIALIZER.serialize(LEGACY_SERIALIZER.deserialize(legacyText));
	}

	public static void sendStackTrace(CommandSender sender, Throwable e) {
		String errorMessage = e.getLocalizedMessage();

		// Get the first 300 characters of the stacktrace and send them to the player
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String rawStackTrace = sw.toString();
		String sStackTrace = rawStackTrace.substring(0, Math.min(rawStackTrace.length(), 300));

		final var formattedMessage = Component.text(
				Objects.requireNonNullElse(errorMessage, "An error occurred without a set message. Hover for stack trace."),
				NamedTextColor.RED
		).hoverEvent(Component.text(sStackTrace, NamedTextColor.RED).asHoverEvent());

		sender.sendMessage(formattedMessage);
		StructuresPlugin.getInstance().getLogger().warning(errorMessage + "\n" + rawStackTrace);
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
