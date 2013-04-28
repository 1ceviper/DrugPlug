package me.iceviper.drugplug;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
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
import org.bukkit.inventory.meta.ItemMeta;
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
				if (!plugin.lastArrest.containsKey(police.getName())) plugin.lastArrest.put(police.getName(), 0l);
				if (police.getItemInHand().isSimilar(new ItemStack(Material.STICK,1))) {
					if (System.currentTimeMillis() - plugin.lastArrest.get(police.getName()) > 20*1000) {
						plugin.lastArrest.remove(police.getName());
						plugin.lastArrest.put(police.getName(), System.currentTimeMillis());
						Boolean found = getDrugs(smuggler.getInventory()).size() > 0;
						ArrayList<ItemStack> smugglerDrugs = getDrugs(smuggler.getInventory());
						for (ItemStack drug : smugglerDrugs) {
							HashMap<Integer,ItemStack> noFit = police.getInventory().addItem(drug);
							if (noFit.size() > 0) police.getWorld().dropItem(police.getLocation(), noFit.get(0));
						}
						for (ItemStack drug : smugglerDrugs) {
							smuggler.getInventory().remove(drug.getType());
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
					} else {
						police.sendMessage(ChatColor.RED + "You have to wait " + Math.ceil(20.0d-(System.currentTimeMillis() - plugin.lastArrest.get(police.getName()))/1000d) + " more seconds before you can arrest someone!");
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
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent e) {
		if (!e.getPlayer().isSneaking()) return;
		if (e.getAction() == Action.LEFT_CLICK_AIR || e.getAction() == Action.LEFT_CLICK_BLOCK) return;
		if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
			//Chest Searching and door opening with a warrant
			if (e.getPlayer().getItemInHand() != null) {
				if (e.getPlayer().getItemInHand().getItemMeta() != null) {
					if (e.getPlayer().getItemInHand().getItemMeta().hasLore()) {
						if (e.getPlayer().getItemInHand().getTypeId() == Material.PAPER.getId() && e.getPlayer().getItemInHand().getItemMeta().getDisplayName() == "Warrant" && e.getPlayer().getItemInHand().getItemMeta().getLore().get(0).contains("Uses:") && e.getPlayer().hasPermission("drug.police")) {
							if (e.getClickedBlock().getType() == Material.CHEST) {
								e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.CHEST_OPEN, 1.0f, 1.0f);
								Chest chest = (Chest) e.getClickedBlock().getState();
								ArrayList<ItemStack> drugs = getDrugs(chest.getBlockInventory());
								if (drugs.size() > 0) {
									for (int i = 0; i < drugs.size(); i++) {
										chest.getBlockInventory().remove(drugs.get(i));
										HashMap<Integer,ItemStack> leftOver = e.getPlayer().getInventory().addItem(drugs.get(i));
										if (leftOver.size() > 0) {
											e.getPlayer().getWorld().dropItem(e.getPlayer().getLocation(), drugs.get(0));
										}
										chest.update();
										e.getPlayer().updateInventory();
									}
									e.getPlayer().sendMessage(ChatColor.GREEN + "You found drugs in this chest and used your warrant to confiscate them.");
									warrantUse(e.getPlayer().getItemInHand());
								} else {
									e.getPlayer().sendMessage(ChatColor.RED + "You find nothing suspicious in this chest.");
								}
							} else if (e.getClickedBlock().getType() == Material.WOODEN_DOOR || e.getClickedBlock().getType() == Material.IRON_DOOR_BLOCK){
								e.getClickedBlock().setData((byte) ((e.getClickedBlock().getData() & 0x4) == 0x4? e.getClickedBlock().getData() & ~0x4 : e.getClickedBlock().getData() | 0x4));
								Block other = e.getClickedBlock().getRelative((e.getClickedBlock().getData() & 0x8) == 0x8?BlockFace.DOWN:BlockFace.UP);
								other.setData((byte) ((other.getData() & 0x4) == 0x4? other.getData() & ~0x4 : other.getData() | 0x4));
								warrantUse(e.getPlayer().getItemInHand());
								e.getPlayer().sendMessage(ChatColor.GREEN + "You used your warrant to persuade the door to open.");
								e.getPlayer().playSound(e.getPlayer().getLocation(), Sound.ZOMBIE_WOOD, 1.0f, 1.0f);
							}
							if (e.getPlayer().getItemInHand().isSimilar(new ItemStack(Material.PAPER))) e.getPlayer().sendMessage(ChatColor.GOLD + "Your warrant has been used up, and is now worth no more then a normal piece of paper.");
						}
					}
				}
			}
		}
		if (!e.getPlayer().hasPermission("drug.police") && containsSimilar(plugin.drugs,e.getPlayer().getItemInHand())) {
			//Drug Consuming
			String effects = plugin.getConfig().getString("drug." + e.getPlayer().getItemInHand().getTypeId() + (e.getPlayer().getItemInHand().getDurability() != 0?e.getPlayer().getItemInHand().getDurability():"") + ".effect");
			if (!(effects == "" || effects == null)) { 
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
		}
	}
	
	@EventHandler
	public void onPlayerInteractEntity(final PlayerInteractEntityEvent e) {
		if (e.getRightClicked() instanceof Player) {
			final Player smuggler = (Player)e.getRightClicked();
			if (e.getPlayer().hasPermission("drug.police") && !smuggler.hasPermission("drug.police") && !smuggler.hasPermission("drug.noremove")) {
				//Player inventory scanning
				if (!plugin.lastScan.containsKey(e.getPlayer().getName())) plugin.lastScan.put(e.getPlayer().getName(), 0l);
				if (e.getPlayer().getItemInHand().isSimilar(new ItemStack(Material.STICK,1))) {
					if (System.currentTimeMillis() - plugin.lastScan.get(e.getPlayer().getName()) > 20*1000) {
						plugin.lastScan.remove(e.getPlayer().getName());
						plugin.lastScan.put(e.getPlayer().getName(), System.currentTimeMillis());
						plugin.getLogger().info("Updated lastscan of player " + e.getPlayer().getName() + " to " + plugin.lastScan.get(e.getPlayer().getName()));
						smuggler.removePotionEffect(PotionEffectType.SLOW);
						e.getPlayer().removePotionEffect(PotionEffectType.SLOW);
						e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SLOW,200,10));
						smuggler.addPotionEffect(new PotionEffect(PotionEffectType.SLOW,200,10));
						e.getPlayer().sendMessage(ChatColor.GREEN + "Scanning smuggler inventory...");
						smuggler.sendMessage(ChatColor.RED + "A law enforcer is scanning your inventory...");
						if (new Random().nextDouble() < getDrugs(((Player)e.getRightClicked()).getInventory()).size()*0.1d) {
							plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable(){
								public void run() {
									e.getPlayer().sendMessage(ChatColor.GREEN + "This persons has drugs on him! Better arrest him!");
									smuggler.sendMessage(ChatColor.RED + "He found your drugs! Run!");
								}
							}, 200);
						} else {
							plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable(){
								public void run() {
									e.getPlayer().sendMessage(ChatColor.RED + "You didn't find anything fishy on this guy...");
									smuggler.sendMessage(ChatColor.GREEN + "He didn't find anything on you. You're safe... for now.");
								}
							}, 200);
						}
					} else {
						if (plugin.lastScan == null) plugin.getLogger().info("wutwut");
						e.getPlayer().sendMessage(ChatColor.RED + "You have to wait " + Math.ceil(20.0d-(System.currentTimeMillis() - plugin.lastScan.get(e.getPlayer().getName()))/1000d) + " more seconds before you can scan someone!");
					}
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
	
	public void warrantUse(ItemStack warrant) {
		if (warrant.getDurability() >= 10) {
			warrant.setItemMeta(null);
		} else {
			warrant.setDurability((short) (warrant.getDurability() + 1));
			ArrayList<String> duraStr = new ArrayList<String>();
			duraStr.add("Uses: " + (10-warrant.getDurability()) + "/10");
			ItemMeta meta = warrant.getItemMeta();
			meta.setLore(duraStr);
			warrant.setItemMeta(meta);
		}
	}
}
