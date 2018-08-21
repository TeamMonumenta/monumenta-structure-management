package pe.epicstructures;

import java.io.File;
import java.io.IOException;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.World;

import pe.epicstructures.commands.LoadStructure;
import pe.epicstructures.commands.SaveStructure;
import pe.epicstructures.managers.RespawnManager;
import pe.epicstructures.managers.StructureManager;

public class Plugin extends JavaPlugin {
	public World mWorld;
	public StructureManager mStructureManager;

	public RespawnManager mRespawnManager = null;

	private File mConfigFile;
	private YamlConfiguration mConfig;

	//	Logic that is performed upon enabling the plugin.
	@Override
	public void onEnable() {
		mWorld = Bukkit.getWorlds().get(0);

		mStructureManager = new StructureManager(this, mWorld);

		getCommand("LoadStructure").setExecutor(new LoadStructure(this, mWorld));
		getCommand("SaveStructure").setExecutor(new SaveStructure(this, mWorld));
		//TODO: Command for force-loading structure
		//TODO: Command to add a structure to the config
		//TODO: Command to reload structures from config file
		//TODO: Command to get structure debug info

		//TODO: Compass listener for telling players how long is left
	}

	//	Logic that is performed upon disabling the plugin.
	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
	}

	private void _loadConfig() {
		if (mConfigFile == null) {
			mConfigFile = new File(getDataFolder(), "config.yml");
		}

		// TODO: Check file exists, if not create default one

		mConfig = YamlConfiguration.loadConfiguration(mConfigFile);

		mRespawnManager = new RespawnManager(this, mWorld, mConfig);
	}

	private void _saveConfig() {
		try {
			mConfig.save(mConfigFile);
		} catch (IOException ex) {
			getLogger().log(Level.SEVERE, "Could not save config to " + mConfigFile, ex);
		}
	}
}
