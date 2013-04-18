package me.iceviper.drugplug;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class DamageListener implements Listener {
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
					
				}
			}
		}
	}
}
