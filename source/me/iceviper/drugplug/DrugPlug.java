package me.iceviper.drugplug;

import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class DrugPlug extends JavaPlugin{
	
	public ArrayList<ItemStack> drugs = new ArrayList<ItemStack>();
	public HashMap<Integer,String> jailed = new HashMap<Integer,String>();
	public HashMap<Integer, Long> jails = new HashMap<Integer, Long>();
	public HashMap<String, Integer> policeWarning = new HashMap<String, Integer>();
	public HashMap<String,Long> lastArrest = new HashMap<String,Long>();
	public HashMap<String,Long> lastScan = new HashMap<String,Long>();
	public int jailAmount = 0;
	
	@Override
	public void onEnable() {
		getLogger().info("Has been enabled, have fun drugging!");
		getServer().getPluginManager().registerEvents(new DamageListener(this),this);
		this.saveDefaultConfig();
		reloadDrugConfig();
		reloadJails();
	}
	
	@Override
	public void onDisable() {
		getLogger().info("Has been disabled :(");
		releaseEveryone();
	}
	
	public void reloadDrugConfig() {
		drugs.clear();
		reloadConfig();
		String illegals = this.getConfig().getString("drugs");
		for (int i = 0; i < illegals.split(",").length; i++) {
			String drug = illegals.split(",")[i];
			if (drug.contains(":")) {
				ItemStack drug2add = new ItemStack(Material.getMaterial(Integer.parseInt(drug.split(":")[0])),1);
				drug2add.setDurability(Short.parseShort(drug.split(":")[1]));
				drugs.add(drug2add);
				getLogger().info("[DEBUG]Adding drug: " + drug);
			} else {
				if (drug == "" || drug == " ") continue;
				else {
					getLogger().info("[DEBUG]Adding drug: " + drug);
					drugs.add(new ItemStack(Material.getMaterial(Integer.parseInt(drug)),1));
				}
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
			if (getConfig().getInt("drug.free.y",-1) == -1) {
				getServer().getPlayer(releasing).performCommand("spawn");
				getConfig().set("drug.free.x", 0);
				getConfig().set("drug.free.y", -1);
				getConfig().set("drug.free.z", 0);
			} else {
				getServer().getPlayer(releasing).teleport(new Location(getServer().getPlayer(releasing).getWorld(),getConfig().getInt("drug.free.x"),getConfig().getInt("drug.free.y"),getConfig().getInt("drug.free.z")));
			}
			getServer().getPlayer(releasing).sendMessage(ChatColor.GREEN + "You have been released!");
		}
	}
	
	public void jail (final Player player, int time) {
		player.sendMessage(ChatColor.GOLD + "You have been jailed! You will be released in " + (time/20) + " seconds.");
		if (jailed.containsValue(player.getName())) {
			return;
		}
		boolean found = false;
		int i = 0;
		while (!found) {
			i++;
			if (jailed.get(i) == "" || jailed.get(i) == null || jailed.get(i) == "null") {
				found = true;
			} else {
				if (i >= jailAmount) {
					break;
				}
			}
		}
		if (!found) {
			i = releaseLongestIn();
		}
		int x = getConfig().getInt("jails."+i+".x");
		int y = getConfig().getInt("jails."+i+".y");
		int z = getConfig().getInt("jails."+i+".z");
		player.teleport(new Location(player.getWorld(),x,y,z));
		jailed.put(i, player.getName());
		jails.put(i, System.currentTimeMillis());
		getLogger().info("Jailing player: " + player.getName());
		getServer().getScheduler().runTaskLater(this, new Runnable(){
			public void run() {
				unJail(player.getName());
				getLogger().info("Releasing player from jail: " + player.getName());
			}
		}, time);
	}
	
	public boolean onCommand(CommandSender cmdSender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("drug")) {
			if (args.length == 0) {
				help(cmdSender);
				return true;
			} else {
				if (args[0].equalsIgnoreCase("add")) {
					if (!cmdSender.hasPermission("drug.add")) {
						cmdSender.sendMessage(ChatColor.RED + "You do not have permission to add drugs to the config, this is Admin Only!");
						return true;
					}
					if (args.length != 2) {
						cmdSender.sendMessage(ChatColor.RED + "'/drug add <id[:data]>' only accepts 2 arguments! You put in " + args.length);
					} else {
						getConfig().set("drugs", getConfig().getString("drugs") + "," + args[1]);
						saveConfig();
						reloadDrugConfig();
					}
					return true;
				} else if (args[0].equalsIgnoreCase("addeffect")) {
					if (!cmdSender.hasPermission("drug.add")) {
						cmdSender.sendMessage(ChatColor.RED + "You do not have permission to add drug effects to the config, this is Admin Only!");
						return true;
					}
					if (args.length != 5) {
						cmdSender.sendMessage(ChatColor.RED + "'/drug addeffect <id[:data]> <potionEffectId> <strength> <time (seconds)>' only accepts 5 arguments! You put in " + args.length);
					} else {
						String drug = args[1];
						int effect = Integer.parseInt(args[2]);
						int strength = Integer.parseInt(args[3]);
						int time = Integer.parseInt(args[4])*20;
						if (drug.contains(":")) {
							ItemStack drugItem = new ItemStack(Integer.parseInt(drug.split(":")[0]),1);
							drugItem.setDurability((short) Integer.parseInt(drug.split(":")[1]));
							if (!DamageListener.containsSimilar(drugs, drugItem)) {
								cmdSender.sendMessage(ChatColor.RED + "Cannot add effects to non-existent drugs. Unknown drug: " + drug);
								return true;
							}
						} else {
							if (!DamageListener.containsSimilar(drugs, new ItemStack(Integer.parseInt(drug),1))) {
								cmdSender.sendMessage(ChatColor.RED + "Cannot add effects to non-existent drugs. Unknown drug: " + drug);
								return true;
							}
						}
						String addStr = "" + effect + ":" + time + ":" + strength;
						if (getConfig().getString("drug." + drug + ".effect","") != "") {
							addStr = "," + addStr;
						}
						getConfig().set("drug." + drug + ".effect", getConfig().getString("drug." + drug + ".effect","") + addStr);
						saveConfig();
					}
					return true;
				} else if (args[0].equalsIgnoreCase("reload")) {
					if (!cmdSender.hasPermission("drug.reload")) {
						cmdSender.sendMessage(ChatColor.RED + "You do not have permission to reload the config, this is Admin Only!");
						return true;
					}
					reloadDrugConfig();
					return true;
				} else if (args[0].equalsIgnoreCase("newjail")) {
					if (!cmdSender.hasPermission("drug.newjail") || !(cmdSender instanceof Player)) {
						cmdSender.sendMessage(ChatColor.RED + "You do not have permission to create new jails, this is Admin Only!");
						return true;
					}
					int jailnum = getConfig().getInt("jails.amount") + 1;
					getConfig().set("jails.amount", jailnum);
					getConfig().set("jails." + jailnum + ".x", ((Player) cmdSender).getLocation().getBlockX());
					getConfig().set("jails." + jailnum + ".y", ((Player) cmdSender).getLocation().getBlockY());
					getConfig().set("jails." + jailnum + ".z", ((Player) cmdSender).getLocation().getBlockZ());
					saveConfig();
					cmdSender.sendMessage("Created a jail at your position!");
					return true;
				} else if (args[0].equalsIgnoreCase("jail")) {
					if (!cmdSender.hasPermission("drug.jail")) {
						cmdSender.sendMessage(ChatColor.RED + "You do not have permission to jail people, this is Admin Only!");
						return true;
					}
					if (args.length != 3) {
						cmdSender.sendMessage(ChatColor.RED + "'/drug jail <player> <time in seconds>' only accepts 3 arguments! You put in " + args.length);
						return true;
					}
					jail(getServer().getPlayer(args[1]), Integer.parseInt(args[2])*20);
					return true;
				} else if (args[0].equalsIgnoreCase("unjail")) {
					if (!cmdSender.hasPermission("drug.jail")) {
						cmdSender.sendMessage(ChatColor.RED + "You do not have permission to unjail people, this is Admin Only!");
						return true;
					}
					if (args.length != 2) {
						cmdSender.sendMessage(ChatColor.RED + "'/drug unjail <player>' only accepts 2 arguments! You put in " + args.length);
						return true;
					}
					if (!jailed.containsValue(getServer().getPlayer(args[1]).getName())) {
						cmdSender.sendMessage(ChatColor.RED + getServer().getPlayer(args[1]).getName() + " is not in jail.");
						return true;
					}
					unJail(getServer().getPlayer(args[1]).getName());
					return true;
				} else if (args[0].equalsIgnoreCase("help")) {
					help(cmdSender);
					return true;
				} else if (args[0].equalsIgnoreCase("makewarrant")) {
					if (!cmdSender.isOp()) {
						cmdSender.sendMessage(ChatColor.RED + "You need to be an op to perform this command");
						return true;
					}
					if (cmdSender instanceof Player) {
						ItemStack warrant = new ItemStack(Material.PAPER,1);
						ItemMeta meta = warrant.getItemMeta();
						meta.setDisplayName("Warrant");
						ArrayList<String> durability = new ArrayList<String>();
						durability.add("Uses: 10/10");
						meta.setLore(durability);
						warrant.setItemMeta(meta);
						((Player) cmdSender).getInventory().addItem(warrant);
					}
					return true;
				} else {
					cmdSender.sendMessage(ChatColor.RED + "Unknown argument: " + args[0] + ". Type '/drug help' for all available commands.");
					return true;
				}
			}
		}
		return false;
	}
	
	public void help(CommandSender cmdSender) {
		cmdSender.sendMessage(ChatColor.DARK_GREEN + "-=- Drug Plug help -=-");
		cmdSender.sendMessage(ChatColor.AQUA + "/drug add <id[:data]> " + ChatColor.GREEN + "to add an item as a drug " + ChatColor.RED + "(Admin only)");
		cmdSender.sendMessage(ChatColor.AQUA + "/drug addeffect <id[:data]> <effectId> <time (seconds)>" + ChatColor.GREEN + "to add an effect to a drug " + ChatColor.RED + "(Admin only)");
		cmdSender.sendMessage(ChatColor.AQUA + "/drug reload " + ChatColor.GREEN + "to reload the config " + ChatColor.RED + "(Admin only)");
		cmdSender.sendMessage(ChatColor.AQUA + "/drug newjail " + ChatColor.GREEN + "to set a jail point " + ChatColor.RED + "(Admin only)");
		cmdSender.sendMessage(ChatColor.AQUA + "/drug jail <player> <time in seconds>" + ChatColor.GREEN + "to jail someone " + ChatColor.RED + "(Admin only)");
		cmdSender.sendMessage(ChatColor.AQUA + "/drug unjail <player>" + ChatColor.GREEN + "to unjail someone " + ChatColor.RED + "(Admin only)");
	}
}
