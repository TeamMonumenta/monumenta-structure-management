package pe.epicstructures.managers;

import com.boydti.fawe.object.schematic.Schematic;

import com.sk89q.worldedit.Vector;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import pe.epicstructures.Plugin;
import pe.epicstructures.utils.MessagingUtils;
import pe.epicstructures.utils.StructureUtils;

public class RespawningStructure {
	public class StructureBounds {
		public Vector mLowerCorner;
		public Vector mUpperCorner;

		public AreaBounds(String name, Vector lowerCorner, Vector upperCorner) {
			mInnerLowerCorner = Vector.getMinimum(lowerCorner, upperCorner);
			mInnerUpperCorner = Vector.getMaximum(lowerCorner, upperCorner);
		}

		public boolean within(Vector vec) {
			return vec.x >= mInnerLowerCorner.x && vec.x <= mInnerUpperCorner.x &&
			       vec.y >= mInnerLowerCorner.y && vec.y <= mInnerUpperCorner.y &&
			       vec.z >= mInnerLowerCorner.z && vec.z <= mInnerUpperCorner.z;
		}
	}

	private Plugin mPlugin;
	private World mWorld;
	private Clipboard mClipboard; // The structure itself
	private StructureBounds mInnerBounds; // The bounding box for the structure itself
	private StructureBounds mOuterBounds; // The bounding box for the nearby area around the structure
	private int mTicksLeft; // How many ticks remaining until respawn
	private int mRespawnTime; // How many ticks between respawns

	public RespawningStructure(Plugin plugin, World world, CONFIG) throws Exception {
		mPlugin = plugin;
		mWorld = world;

		//TODO parse load coord from string
		Location loadLoc = CommandUtils.parseLocationFromString(sender, mWorld, arg3[1], arg3[2], arg3[3]);
		loadPos = new Vector(loadLoc.getBlockX(), loadLoc.getBlockY(), loadLoc.getBlockZ());

		//TODO: Load structure w/ debug
		try {
			schem = mPlugin.mStructureManager.loadSchematic("structures", arg3[0]);
		} catch (Exception e) {
			mPlugin.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();

			if (sender != null) {
				sender.sendMessage(ChatColor.RED + "Failed to load structure");
				MessagingUtils.sendStackTrace(sender, e);
			}
			return false;
		}

		// TODO parse structure dimensions from structure

		// TODO create bounding box
		mInnerBounds = new StructureBounds(POS, POS);
		mOuterBounds = new StructureBounds(mInnerBounds.mLowerCorner.clone().subtract(RADIUS, RADIUS, RADIUS),
		                                   mInnerBounds.mUpperCorner.clone().add(RADIUS, RADIUS, RADIUS));
	}


	public void Respawn() {
		StructureUtils.paste(mClipboard, mWorld, mPos);
		mTicksLeft = mRespawnTime;
	}

	public void TellRespawnTime(Player player) {
		//TODO
		player.sendMessage(STUFF);
	}

	public void TellRespawnTimeIfNearby(Player player) {
		if (mOuterBounds.within(player.getLocation().toVector())) {
			TellRespawnTime(player);
		}
	}

	public void tick(int ticks) {
		if (((mTicksLeft >= 2400) && ((mTicksLeft - ticks) < 2400)) ||
		    ((mTicksLeft >= 600) && ((mTicksLeft - ticks) < 600))) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (mOuterBounds.within(player.getLocation().toVector())) {
					TellRespawnTime(player);
				}
			}
		}

		mTicksLeft -= ticks;

		if (mTicksLeft < 0) {
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (mOuterBounds.within(player.getLocation().toVector())) {
					Respawn();
					break;
				}
			}
		}
	}
}
