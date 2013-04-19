package me.iceviper.drugplug;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
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
	public void entityDamaged(EntityDamageByEntityEvent e) {
		Player police = null;
		Player smuggler = null;
		if (e.getDamager() instanceof Player) {
			police = (Player) e.getDamager();
		}
		if (e.getEntity() instanceof Player) {
			smuggler = (Player)e.getEntity();
		}
		if (police != null && smuggler != null) {
			if (police.hasPermission("drugplug.police") && !smuggler.hasPermission("drugplug.noremove") && !smuggler.hasPermission("drugplug.police")) {
				//Permission are good
				if (police.getItemInHand().isSimilar(new ItemStack(Material.STICK,1))) {
					//Police is holding a stick
					ItemStack scanning = null;
					Boolean found = false;
					for (int i = 0; i < 36; i++) {
						scanning = smuggler.getInventory().getItem(i);
						if(containsSimilar(plugin.drugs, scanning)) {
							smuggler.getInventory().remove(scanning);
							police.getWorld().dropItem(police.getLocation(), police.getInventory().addItem(scanning).get(0));
							found = true;
						}
					}
					if (found) {
						jail(smuggler,1000*60*5);
					} else {
						//Police punishment
					}
				}
			}
		}
	}
	
	public boolean containsSimilar(ArrayList<ItemStack> items, ItemStack scanning) {
		for (ItemStack i : items) {
			if (i.isSimilar(scanning)) return true;
		}
		return false;
	}
	
	public void jail (Player player, int time) {
		plugin.jailed.put(key, value);
	}
}
