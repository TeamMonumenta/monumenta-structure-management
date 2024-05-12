package com.playmonumenta.structures.commands;

import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.Location2DArgument;
import dev.jorel.commandapi.arguments.LocationType;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import dev.jorel.commandapi.wrappers.Location2D;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public class ForceloadLazy {
	public static void register() {
		Location2DArgument fromArg = new Location2DArgument("from", LocationType.BLOCK_POSITION);
		Location2DArgument toArg = new Location2DArgument("to", LocationType.BLOCK_POSITION);
		FunctionArgument callbackArg = new FunctionArgument("callback");

		new CommandTree("forceload")
			.then(new LiteralArgument("addlazy")
				.withPermission(CommandPermission.fromString("monumenta.structures.forceloadlazy"))

				// forceload addlazy from
				.then(fromArg
					.executes((sender, args) -> {
						Location2D from = args.getByArgument(fromArg);
						load(sender, from, from, null); // Intentionally both the same argument
					})

					// forceload addlazy from to
					.then(toArg
						.executes((sender, args) -> {
							load(sender, args.getByArgument(fromArg), args.getByArgument(toArg), null);
						})

						// forceload addlazy from to callback
						.then(callbackArg
							.executes((sender, args) -> {
								load(sender, args.getByArgument(fromArg), args.getByArgument(toArg), args.getByArgument(callbackArg));
							})))))
			.register();
	}

	private static void load(CommandSender sender, Location2D from, Location2D to, @Nullable FunctionWrapper[] callback) {
		final Set<BlockVector2> chunks = new CuboidRegion(BlockVector3.at(from.getBlockX(), 0, from.getBlockZ()),
				BlockVector3.at(to.getBlockX(), 255, to.getBlockZ())).getChunks();

		final AtomicInteger numRemaining = new AtomicInteger(chunks.size());

		StringBuilder msg = new StringBuilder(ChatColor.WHITE + "" + chunks.size() + " chunks have finished forceloading: ");
		boolean first = true;
		for (final BlockVector2 chunkCoords : chunks) {
			if (!first) {
				msg.append(", ");
			}
			msg.append("[").append(chunkCoords.getX()).append(", ").append(chunkCoords.getZ()).append("]");
			first = false;
		}
		final String sendMessage = msg.toString();

		final Consumer<Chunk> chunkConsumer = (final Chunk chunk) -> {
			chunk.setForceLoaded(true);
			int remaining = numRemaining.decrementAndGet();
			if (remaining == 0) {
				if (callback != null) {
					for (FunctionWrapper func : callback) {
						func.run(sender);
					}
				}
				sender.sendMessage(sendMessage);
			}
		};

		for (final BlockVector2 chunkCoords : chunks) {
			from.getWorld().getChunkAtAsync(chunkCoords.getX(), chunkCoords.getZ(), chunkConsumer);
		}
	}
}
