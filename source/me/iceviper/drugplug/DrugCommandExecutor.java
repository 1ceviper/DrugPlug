package me.iceviper.drugplug;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DrugCommandExecutor implements CommandExecutor{
	
	private DrugPlug plugin;
	
	public DrugCommandExecutor(DrugPlug plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender cmdSender, Command cmd, String label, String[] args) {
		if (args.length == 0) {
			help(cmdSender);
			return true;
		} else {
			if (args[0] == "add") {
				if (!cmdSender.hasPermission("drug.add")) {
					cmdSender.sendMessage(ChatColor.RED + "You do not have permission to add drugs to the config, this is Admin Only!");
					return true;
				}
				if (args.length != 2) {
					cmdSender.sendMessage(ChatColor.RED + "'/drug add <id[:data]>' only accepts 2 arguments! You put in " + args.length);
				} else {
					plugin.getConfig().set("drugs", plugin.getConfig().getStringList("drugs").add(args[1]));
					plugin.saveConfig();
					plugin.reloadDrugConfig();
				}
				return true;
			} else if (args[0] == "reload") {
				if (!cmdSender.hasPermission("drug.reload")) {
					cmdSender.sendMessage(ChatColor.RED + "You do not have permission to reload the config, this is Admin Only!");
					return true;
				}
				plugin.reloadDrugConfig();
				return true;
			} else if (args[0] == "newjail") {
				if (!cmdSender.hasPermission("drug.newjail") || !(cmdSender instanceof Player)) {
					cmdSender.sendMessage(ChatColor.RED + "You do not have permission to create new jails, this is Admin Only!");
					return true;
				}
				int jailnum = plugin.getConfig().getInt("jails.amount") + 1;
				plugin.getConfig().set("jails.amount", jailnum);
				plugin.getConfig().set("jails." + jailnum + ".x", ((Player) cmdSender).getLocation().getBlockX());
				plugin.getConfig().set("jails." + jailnum + ".y", ((Player) cmdSender).getLocation().getBlockY());
				plugin.getConfig().set("jails." + jailnum + ".z", ((Player) cmdSender).getLocation().getBlockZ());
				plugin.saveConfig();
				return true;
			} else if (args[0] == "jail") {
				if (!cmdSender.hasPermission("drug.jail")) {
					cmdSender.sendMessage(ChatColor.RED + "You do not have permission to jail people, this is Admin Only!");
					return true;
				}
				if (args.length != 3) {
					cmdSender.sendMessage(ChatColor.RED + "'/drug jail <player> <time in seconds>' only accepts 3 arguments! You put in " + args.length);
					return true;
				}
				plugin.jail(plugin.getServer().getPlayer(args[1]), Integer.parseInt(args[2])*1000);
				return true;
			}
			else {
				cmdSender.sendMessage(ChatColor.RED + "Unknown argument: " + args[0] + ". Type '/drug help' for all available commands.");
				return true;
			}
		}
	}
	
	public void help(CommandSender cmdSender) {
		cmdSender.sendMessage(ChatColor.DARK_GREEN + "-=- Drug Plug help -=-");
		cmdSender.sendMessage(ChatColor.AQUA + "/drug add <id[:data]> " + ChatColor.GREEN + "to add an item as a drug " + ChatColor.RED + "(Admin only)");
		cmdSender.sendMessage(ChatColor.AQUA + "/drug reload " + ChatColor.GREEN + "to reload the config " + ChatColor.RED + "(Admin only)");
		cmdSender.sendMessage(ChatColor.AQUA + "/drug newjail " + ChatColor.GREEN + "to set a jail point " + ChatColor.RED + "(Admin only)");
	}
	
}
