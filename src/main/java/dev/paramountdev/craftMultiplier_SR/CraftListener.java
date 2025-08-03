package dev.paramountdev.craftMultiplier_SR;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class CraftListener implements Listener {

    private final JavaPlugin plugin;
    private final int defaultMultiplier;
    private final Map<Material, Integer> customMultipliers;

    public CraftListener(JavaPlugin plugin, int defaultMultiplier, Map<Material, Integer> customMultipliers) {
        this.plugin = plugin;
        this.defaultMultiplier = defaultMultiplier;
        this.customMultipliers = customMultipliers;
    }

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inv = event.getInventory();
        ItemStack[] matrix = inv.getMatrix();

        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) continue;

            int required = customMultipliers.getOrDefault(item.getType(), defaultMultiplier);
            if (item.getAmount() < required) {
                inv.setResult(null);
                return;
            }
        }
    }

    @EventHandler
    public void onCraft(CraftItemEvent event) {
        CraftingInventory inv = (CraftingInventory) event.getInventory();
        ItemStack[] matrix = inv.getMatrix();
        boolean shiftClick = event.isShiftClick();

        int minCraftsPossible = Integer.MAX_VALUE;

        Map<Integer, Integer> requiredPerSlot = new HashMap<>();

        for (int i = 0; i < matrix.length; i++) {
            ItemStack item = matrix[i];
            if (item == null || item.getType() == Material.AIR) continue;

            int required = customMultipliers.getOrDefault(item.getType(), defaultMultiplier);
            int available = item.getAmount();

            int crafts = available / required;
            if (crafts == 0) {
                event.setCancelled(true);
                return;
            }

            requiredPerSlot.put(i, required);

            if (crafts < minCraftsPossible) {
                minCraftsPossible = crafts;
            }
        }

        int timesToCraft = shiftClick ? minCraftsPossible : 1;


        event.setCancelled(true);


        ItemStack result = event.getRecipe().getResult().clone();
        result.setAmount(result.getAmount() * timesToCraft);
        event.getWhoClicked().getInventory().addItem(result);


        ItemStack[] newMatrix = matrix.clone();
        for (Map.Entry<Integer, Integer> entry : requiredPerSlot.entrySet()) {
            int slot = entry.getKey();
            int required = entry.getValue() * timesToCraft;

            ItemStack item = newMatrix[slot];
            item.setAmount(item.getAmount() - required);
            newMatrix[slot] = item.getAmount() > 0 ? item : null;
        }

        inv.setMatrix(newMatrix);
        inv.setResult(null);
    }
}
