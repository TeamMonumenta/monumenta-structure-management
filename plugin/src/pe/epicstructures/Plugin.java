package pe.epicstructures;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;

import pe.epicstructures.commands.LoadStructure;
import pe.epicstructures.commands.SaveStructure;
import pe.epicstructures.commands.LoadUserStructure;
import pe.epicstructures.managers.StructureManager;
import pe.epicstructures.managers.UserStructureManager;

public class Plugin extends JavaPlugin {
	public World mWorld;
	public StructureManager mStructureManager;
	public UserStructureManager mUserStructureManager;

	//	Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		mWorld = Bukkit.getWorlds().get(0);

		mStructureManager = new StructureManager(this, mWorld);
		mUserStructureManager = new UserStructureManager(this);

		getCommand("LoadStructure").setExecutor(new LoadStructure(this, mWorld));
		getCommand("SaveStructure").setExecutor(new SaveStructure(this, mWorld));
		getCommand("LoadUserStructure").setExecutor(new LoadUserStructure(this, mWorld));
	}

	//	Logic that is performed upon disabling the plugin.
	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);

		mUserStructureManager.unloadAll();
	}
}
