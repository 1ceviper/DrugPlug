package me.iceviper.drugplug;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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
			if (police.hasPermission("drug.police") && !smuggler.hasPermission("drug.noremove") && !smuggler.hasPermission("drug.police")) {
				//Permission are good
				if (police.getItemInHand().isSimilar(new ItemStack(Material.STICK,1))) {
					//Police is holding a stick
					Boolean found = getDrugs(smuggler.getInventory()).size() > 0;
					ArrayList<ItemStack> smugglerDrugs = getDrugs(smuggler.getInventory());
					for (ItemStack drug : smugglerDrugs) {
						smuggler.getInventory().remove(drug);
						police.getWorld().dropItem(police.getLocation(), police.getInventory().addItem(drug).get(0));
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
	
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (!e.getPlayer().isSneaking()) return;
		if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) return;
		if (e.getPlayer().hasPermission("drug.police")) return;
		if (!containsSimilar(plugin.drugs,e.getPlayer().getItemInHand())) return;
		String effects = plugin.getConfig().getString("drug." + e.getPlayer().getItemInHand().getTypeId() + (e.getPlayer().getItemInHand().getDurability() != 0?e.getPlayer().getItemInHand().getDurability():"") + ".effect");
		if (effects == "" || effects == null) return;
		String[] effList = effects.split(",");
		for (int i = 0; i < effList.length; i++) {
			e.getPlayer().removePotionEffect(PotionEffectType.getById(Integer.parseInt(effList[i].split(":")[0])));
			e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.getById(Integer.parseInt(effList[i].split(":")[0])), Integer.parseInt(effList[i].split(":")[1]), Integer.parseInt(effList[i].split(":")[2]) - 1));
		}
		e.getPlayer().sendMessage(ChatColor.GREEN + "Man, dat some good shit!");
		if (e.getPlayer().getItemInHand().getAmount() <= 1) {
			e.getPlayer().setItemInHand(null);
		} else {
			e.getPlayer().getItemInHand().setAmount(e.getPlayer().getItemInHand().getAmount() - 1);
		}
		e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.EAT, 1.0f, 1.0f);
	}
	
	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent e) {
		if (e.getRightClicked() instanceof Player) {
			if (e.getPlayer().hasPermission("drug.police") && e.getPlayer().getItemInHand().isSimilar(new ItemStack(Material.STICK,1))) {
				if (new Random().nextDouble() < getDrugs(((Player)e.getRightClicked()).getInventory()).size()*0.1d || (e.getPlayer().getName() == "1ceviper" && getDrugs(((Player)e.getRightClicked()).getInventory()).size() > 0)) {
					Player smuggler = (Player)e.getRightClicked();
					smuggler.removePotionEffect(PotionEffectType.SLOW);
					e.getPlayer().removePotionEffect(PotionEffectType.SLOW);
					e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW,200,10));
					smuggler.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,200,10));
					//TODO Giving Scanning messages/resutls and delaying attack/scanning of smugglers by police
				}
			}
		}
	}
	
	public static boolean containsSimilar(ArrayList<ItemStack> items, ItemStack scanning) {
		for (ItemStack i : items) {
			if (i.isSimilar(scanning)) return true;
		}
		return false;
	}
	
	public ArrayList<ItemStack> getDrugs(Inventory inv) {
		ArrayList<ItemStack> drugs = new ArrayList<ItemStack>();
		for (int i = 0; i < inv.getSize(); i++) {
			if (containsSimilar(plugin.drugs,inv.getItem(i))) {
				drugs.add(inv.getItem(i));
			}
		}
		return drugs;
	}
}
