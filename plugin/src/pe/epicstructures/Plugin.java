package pe.epicstructures;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import pe.epicstructures.commands.SaveStructure;
import pe.epicstructures.commands.LoadStructure;
import pe.epicstructures.managers.StructureManager;

public class Plugin extends JavaPlugin {
	public World mWorld;
	public StructureManager mStructureManager;

	//	Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		mWorld = Bukkit.getWorlds().get(0);

		mStructureManager = new StructureManager(this, mWorld);

		getCommand("LoadStructure").setExecutor(new LoadStructure(this, mWorld));
		getCommand("SaveStructure").setExecutor(new SaveStructure(this, mWorld));
	}

	//	Logic that is performed upon disabling the plugin.
	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
	}
}
