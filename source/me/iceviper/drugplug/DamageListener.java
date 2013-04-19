package me.iceviper.drugplug;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

public class DamageListener implements Listener {
	
	/*
	 * Note: This was once created as a entityDamaged Listener
	 * 		 but now listens to more events
	 */
	
	private DrugPlug plugin;
	
	public DamageListener(DrugPlug plugin) {
		this.plugin = plugin;
	}	
	
	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
		Player police = null;
		Player smuggler = null;
		if (e.getDamager() instanceof Player) {
			police = (Player) e.getDamager();
		}
		if (e.getEntity() instanceof Player) {
			smuggler = (Player)e.getEntity();
		}
		if (police != null && smuggler != null) {
			if (police.hasPermission("drug.police") && !smuggler.hasPermission("drug.noremove") && !smuggler.hasPermission("drugplug.police")) {
				//Permission are good
				if (police.getItemInHand().isSimilar(new ItemStack(Material.STICK,1))) {
					//Police is holding a stick
					ItemStack scanning = null;
					Boolean found = false;
					for (int i = 0; i < 36; i++) {
						scanning = smuggler.getInventory().getItem(i);
						if(containsSimilar(plugin.drugs, scanning)) {
							smuggler.getInventory().remove(scanning);
							ItemStack leftover = police.getInventory().addItem(scanning).get(0);
							if(leftover != null) police.getWorld().dropItem(police.getLocation(),leftover);
							found = true;
						}
					}
					if (found) {
						plugin.jail(smuggler,20*60*5);
						smuggler.sendMessage(ChatColor.RED + "You have been caught with drugs by " + police.getDisplayName());
						police.sendMessage(ChatColor.GREEN + "You found drugs on this player and arrested him, his items are now yours!");
					} else {
						int warnings = 1;
						if (plugin.policeWarning.containsKey(police.getName())) {
							warnings = plugin.policeWarning.get(police.getName()) + 1;
							plugin.policeWarning.remove(police.getName());
						}
						if (warnings > plugin.getConfig().getInt("maxPoliceFails",5)) {
							plugin.jail(police, 20*60*5);
							warnings = 0;
						}
						police.sendMessage(ChatColor.RED + "You tried to arrest someone without drugs! Warning " + warnings + "/" + plugin.getConfig().getInt("maxPoliceFails",5));
						plugin.policeWarning.put(police.getName(), warnings);
					}
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent e) {
		if (plugin.jailed.containsValue(e.getPlayer().getName())) {
			e.setCancelled(true);
		}
	}
	
	public boolean containsSimilar(ArrayList<ItemStack> items, ItemStack scanning) {
		for (ItemStack i : items) {
			if (i.isSimilar(scanning)) return true;
		}
		return false;
	}
}
