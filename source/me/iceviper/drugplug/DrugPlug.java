package me.iceviper.drugplug;

import org.bukkit.plugin.java.JavaPlugin;

public class DrugPlug extends JavaPlugin{
	
	
	
	
	@Override
	public void onEnable() {
		getLogger().info("[DrugPlug] Has been enabled, have fun drugging!");
	}
	
	@Override
	public void onDisable() {
		getLogger().info("[DrugPlug] Has been disabled :(");
	}
}
