package pe.epicstructures;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import pe.epicstructures.commands.SaveStructure;
import pe.epicstructures.commands.LoadStructure;

public class Plugin extends JavaPlugin {
	public World mWorld;

	//	Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		PluginManager manager = getServer().getPluginManager();

		mWorld = Bukkit.getWorlds().get(0);

		getCommand("LoadStructure").setExecutor(new LoadStructure(this, mWorld));
		getCommand("SaveStructure").setExecutor(new SaveStructure(this, mWorld));
	}

	//	Logic that is performed upon disabling the plugin.
	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
	}
}
