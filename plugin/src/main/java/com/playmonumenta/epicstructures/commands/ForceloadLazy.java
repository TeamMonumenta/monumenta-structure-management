package com.playmonumenta.epicstructures.commands;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.playmonumenta.epicstructures.Plugin;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;

import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Location2DArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.wrappers.Location2D;
import net.md_5.bungee.api.ChatColor;

public class ForceloadLazy {
	public static void register(Plugin plugin) {
		final CommandPermission perms = CommandPermission.fromString("epicstructures.forceloadlazy");

		/* First one of these includes coordinate arguments */
		new CommandAPICommand("forceload")
			.withPermission(perms)
			.withArguments(new MultiLiteralArgument("addlazy"))
			.withArguments(new Location2DArgument("from", LocationType.BLOCK_POSITION))
			.executes((sender, args) -> {
				load(sender, plugin, (Location2D)args[1], (Location2D)args[1]); // Intentionally both the same argument
			})
			.register();

		new CommandAPICommand("forceload")
			.withPermission(perms)
			.withArguments(new MultiLiteralArgument("addlazy"))
			.withArguments(new Location2DArgument("from", LocationType.BLOCK_POSITION))
			.withArguments(new Location2DArgument("to", LocationType.BLOCK_POSITION))
			.executes((sender, args) -> {
				load(sender, plugin, (Location2D)args[1], (Location2D)args[2]);
			})
			.register();
	}

	private static void load(CommandSender sender, Plugin plugin, Location2D from, Location2D to) throws WrapperCommandSyntaxException {
		CuboidRegion region = new CuboidRegion(BlockVector3.at(from.getBlockX(), 0, from.getBlockZ()), BlockVector3.at(to.getBlockX(), 255, to.getBlockZ()));
		final Set<BlockVector2> chunks = region.getChunks();

		final AtomicInteger numRemaining = new AtomicInteger(chunks.size());

		String msg = ChatColor.WHITE + "" + chunks.size() + " chunks have finished forceloading: ";
		boolean first = true;
		for (final BlockVector2 chunkCoords : chunks) {
			if (!first) {
				msg += ", ";
			}
			msg += "[" + chunkCoords.getX() + ", " + chunkCoords.getZ() + "]";
			first = false;
		}
		final String sendMessage = msg;

		final Consumer<Chunk> chunkConsumer = (final Chunk chunk) -> {
			chunk.setForceLoaded(true);
			int remaining = numRemaining.decrementAndGet();
			if (remaining == 0) {
				sender.sendMessage(sendMessage);
			}
		};

		for (final BlockVector2 chunkCoords : chunks) {
			from.getWorld().getChunkAtAsync(chunkCoords.getX(), chunkCoords.getZ(), chunkConsumer);
		}
	}
}
