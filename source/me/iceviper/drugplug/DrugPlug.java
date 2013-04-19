package me.iceviper.drugplug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class DrugPlug extends JavaPlugin{
	
	public ArrayList<ItemStack> drugs = new ArrayList<ItemStack>();
	public HashMap<Integer,Player> jailed = new HashMap<Integer,Player>();
	
	@Override
	public void onEnable() {
		getLogger().info("[DrugPlug] Has been enabled, have fun drugging!");
		getServer().getPluginManager().registerEvents(new DamageListener(this),this);
		this.saveDefaultConfig();
		reloadDrugConfig();
		this.getCommand("drug").setExecutor(new DrugCommandExecutor(this));
	}
	
	@Override
	public void onDisable() {
		getLogger().info("[DrugPlug] Has been disabled :(");
	}
	
	public void reloadDrugConfig() {
		drugs.clear();
		reloadConfig();
		List<String> illegals = this.getConfig().getStringList("drugs");
		for (String drug : illegals) {
			if (drug.contains(":")) {
				ItemStack drug2add = new ItemStack(Material.getMaterial(Integer.getInteger(drug.split(":")[0])),1);
				drug2add.setDurability(Short.parseShort(drug.split(":")[1]));
				drugs.add(drug2add);
			} else {
				drugs.add(new ItemStack(Material.getMaterial(Integer.getInteger(drug)),1));
			}
		}
	}
}
