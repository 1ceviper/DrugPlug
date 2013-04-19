package me.iceviper.drugplug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class DrugPlug extends JavaPlugin{
	
	public ArrayList<ItemStack> drugs = new ArrayList<ItemStack>();
	public HashMap<Integer,String> jailed = new HashMap<Integer,String>();
	public HashMap<Integer, Long> jails = new HashMap<Integer, Long>();
	public HashMap<String, Integer> policeWarning = new HashMap<String, Integer>();
	public int jailAmount = 0;
	
	@Override
	public void onEnable() {
		getLogger().info("[DrugPlug] Has been enabled, have fun drugging!");
		getServer().getPluginManager().registerEvents(new DamageListener(this),this);
		this.saveDefaultConfig();
		reloadDrugConfig();
		reloadJails();
		this.getCommand("drug").setExecutor(new DrugCommandExecutor(this));
	}
	
	@Override
	public void onDisable() {
		getLogger().info("[DrugPlug] Has been disabled :(");
		releaseEveryone();
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
	
	public void reloadJails() {
		jailed.clear();
		jails.clear();
		jailAmount = getConfig().getInt("jails.amount");
		for (int i = 1; i <= jailAmount ; i++) {
			jailed.put(i, "");
			jails.put(i, (long) 0);
		}
	}
	
	public void releaseEveryone() {
		for (int i = 1; i <= jailAmount; i++) {
			if (jailed.get(i) != "") {
				String releasing = jailed.get(i);
				jailed.remove(i);
				jailed.put(i, "");
				jails.remove(i);
				jails.put(i, (long) 0);
				getServer().getPlayer(releasing).performCommand("spawn");
				getServer().getPlayer(releasing).sendMessage(ChatColor.GREEN + "You have been released!");
			}
		}
	}
	
	public int releaseLongestIn() {
		int longestIn = 0;
		long longestInTime = 0; 
		for (int i = 1; i <= jailAmount; i++) {
			if (longestIn == 0 || (jails.get(i) < longestInTime && jailed.get(i) != "")) {
				longestIn = i;
				longestInTime = jails.get(i);
			}
		}
		String releasing = jailed.get(longestIn);
		jailed.remove(longestIn);
		jailed.put(longestIn, "");
		jails.remove(longestIn);
		jails.put(longestIn, (long) 0);
		getServer().getPlayer(releasing).performCommand("spawn");
		getServer().getPlayer(releasing).sendMessage(ChatColor.GREEN + "You have been released!");
		return longestIn;
	}
	
	public void unJail(String player) {
		int j = 0;
		for (int i = 1; i <= jailAmount; i++) {
			if (jailed.get(i) == player) {
				j = i;
				break;
			}
		}
		if (j != 0) {
			String releasing = jailed.get(j);
			jailed.remove(j);
			jailed.put(j, "");
			jails.remove(j);
			jails.put(j, (long) 0);
			getServer().getPlayer(releasing).performCommand("spawn");
			getServer().getPlayer(releasing).sendMessage(ChatColor.GREEN + "You have been released!");
		}
	}
	
	public void jail (final Player player, int time) {
		boolean found = false;
		int i = 0;
		while (!found) {
			i++;
			if (jailed.get(i) == "") {
				found = true;
			} else if (i >= jailAmount) {
				break;
			}
		}
		if (!found) {
			i = releaseLongestIn();
		}
		jailed.put(i, player.getName());
		jails.put(i, System.currentTimeMillis());
		getServer().getScheduler().runTaskLater(this, new Runnable(){
			public void run() {
				unJail(player.getName());
			}
		}, time);
	}
}
