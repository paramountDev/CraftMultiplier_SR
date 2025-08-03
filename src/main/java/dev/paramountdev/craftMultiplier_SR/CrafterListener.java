package dev.paramountdev.craftMultiplier_SR;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Crafter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class CrafterListener implements Listener {

    private final int defaultMultiplier;
    private final Map<Material, Integer> customMultipliers;

    public CrafterListener(int defaultMultiplier, Map<Material, Integer> customMultipliers) {
        this.defaultMultiplier = defaultMultiplier;
        this.customMultipliers = customMultipliers;
    }

    @EventHandler
    public void onCrafterCraft(CrafterCraftEvent event) {
        Block block = event.getBlock();
        BlockState state = block.getState();

        if (!(state instanceof Crafter)) {
            return;
        }

        Crafter crafter = (Crafter) state;
        Inventory inv = crafter.getInventory();

        ItemStack[] matrix = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            matrix[i] = inv.getItem(i);
        }

        int minCraftsPossible = Integer.MAX_VALUE;

        for (ItemStack item : matrix) {
            if (item == null || item.getType().isAir()) continue;

            int required = customMultipliers.getOrDefault(item.getType(), defaultMultiplier);
            int available = item.getAmount();

            int crafts = available / required;
            if (crafts == 0) {
                event.setCancelled(true);
                return;
            }

            minCraftsPossible = Math.min(minCraftsPossible, crafts);
        }

        ItemStack result = event.getRecipe().getResult().clone();
        result.setAmount(result.getAmount() * minCraftsPossible);
        event.setResult(result);

        for (int i = 0; i < 9; i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType().isAir()) continue;

            int required = customMultipliers.getOrDefault(item.getType(), defaultMultiplier);
            int newAmount = item.getAmount() - (required * minCraftsPossible);

            if (newAmount > 0) {
                item.setAmount(newAmount);
                inv.setItem(i, item);
            } else {
                inv.setItem(i, null);
            }
        }
    }
}
