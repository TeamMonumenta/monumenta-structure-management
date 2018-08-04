package pe.epicstructures.managers;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Paths;
import java.util.HashMap;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.Extent;
import com.sk89q.worldedit.function.mask.ExistingBlockMask;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldedit.world.registry.WorldData;

import com.boydti.fawe.object.schematic.Schematic;

import pe.epicstructures.Plugin;

public class RespawnManager {
	Plugin mPlugin;

	List<RespawningStructure> mRespawns = new ArrayList<RespawningStructure>();

	public RespawnManager(Plugin plugin, CONFIG) {
		mPlugin = plugin;

		// load config
		// create sync task that counts down and loads
	}
}
