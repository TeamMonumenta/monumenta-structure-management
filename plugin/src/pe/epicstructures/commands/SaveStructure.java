package pe.epicstructures.commands;

import java.io.File;
import java.io.FileOutputStream;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import com.boydti.fawe.object.schematic.Schematic;
import com.boydti.fawe.util.EditSessionBuilder;

import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.WorldData;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.operation.Operations;

import pe.epicstructures.Plugin;
import pe.epicstructures.utils.CommandUtils;
import pe.epicstructures.utils.MessagingUtils;

public class SaveStructure implements CommandExecutor {
	Plugin mPlugin;
	org.bukkit.World mWorld;

	public SaveStructure(Plugin plugin, org.bukkit.World world) {
		mPlugin = plugin;
		mWorld = world;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length != 7) {
			sender.sendMessage(ChatColor.RED + "This command requires exactly seven arguments");
			return false;
		}

		// Parse the coordinates of the structure to save
		Vector minpos;
		Vector maxpos;
		try {
			Vector pos1;
			Vector pos2;
			pos1 = new Vector(CommandUtils.parseIntFromString(sender, arg3[1]),
			                  CommandUtils.parseIntFromString(sender, arg3[2]),
			                  CommandUtils.parseIntFromString(sender, arg3[3]));
			pos2 = new Vector(CommandUtils.parseIntFromString(sender, arg3[4]),
			                  CommandUtils.parseIntFromString(sender, arg3[5]),
			                  CommandUtils.parseIntFromString(sender, arg3[6]));

			minpos = Vector.getMinimum(pos1, pos2);
			maxpos = Vector.getMaximum(pos1, pos2);
		} catch (Exception e) {
			return false;
		}

		// Save it
		try {
			_saveSchematic(arg3[0], minpos, maxpos);
		} catch (Exception e) {
			mPlugin.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();

			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Failed to save structure");
				MessagingUtils.sendStackTrace(sender, e);
			}
			return false;
		}

		sender.sendMessage("Saved structure '" + arg3[0] + "'");

		return true;
	}

	// TODO: Probably need to update the cache here if present
	// This code adapted from forum post here: https://www.spigotmc.org/threads/saving-schematics-to-file-with-worldedit-api.276078/
	public void _saveSchematic(String baseName, Vector minpos, Vector maxpos) throws Exception {
		if (baseName == null || baseName.isEmpty()) {
			throw new Exception("Structure name is empty!");
		}

		final String fileName = mPlugin.getDataFolder() + File.separator + "structures" + File.separator + baseName + ".schematic";

        File file = new File(fileName);
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			file.createNewFile();
		}

        World world = new BukkitWorld(mWorld);
        WorldData worldData = world.getWorldData();
        CuboidRegion cReg = new CuboidRegion(world, minpos, maxpos);

		BlockArrayClipboard clipboard = new BlockArrayClipboard(cReg);
		Extent source = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1);
		Extent destination = clipboard;
		ForwardExtentCopy copy = new ForwardExtentCopy(source, cReg, clipboard.getOrigin(), destination, minpos);
		copy.setSourceMask(new ExistingBlockMask(source));

		// TODO: Make this run async (completeSmart)
		Operations.completeLegacy(copy);

		ClipboardFormat.SCHEMATIC.getWriter(new FileOutputStream(file)).write(clipboard, worldData);
    }
}
