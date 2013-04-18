package me.iceviper.drugplug;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.plugin.java.JavaPlugin;

public class DrugPlug extends JavaPlugin{
	
	ArrayList<Material> drugs = new ArrayList<Material>();
	
	
	@Override
	public void onEnable() {
		getLogger().info("[DrugPlug] Has been enabled, have fun drugging!");
		
	}
	
	@Override
	public void onDisable() {
		getLogger().info("[DrugPlug] Has been disabled :(");
	}
}
