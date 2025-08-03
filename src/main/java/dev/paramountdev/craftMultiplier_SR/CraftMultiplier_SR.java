package dev.paramountdev.craftMultiplier_SR;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.*;

import java.util.*;
import java.util.logging.Level;


public final class CraftMultiplier_SR extends JavaPlugin implements  CommandExecutor, TabCompleter {

    private FileConfiguration config;
    private int defaultMultiplier;
    private Map<Material, Integer> customMultipliers;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.config = getConfig();

        if (!config.getBoolean("enabled", true)) {
            getLogger().info("CraftMultiplier отключён в конфиге.");
            return;
        }

        loadMultipliers();
        Bukkit.getPluginManager().registerEvents(new CraftListener(this, defaultMultiplier, customMultipliers), this);
        getCommand("cm").setExecutor(this);
        getCommand("cm").setTabCompleter(this);
        Bukkit.getScheduler().runTaskLater(this, this::modifyRecipes, 1L);
        sendSignatureToConsole("enabled");
    }

    private void loadMultipliers() {
        defaultMultiplier = config.getInt("default-multiplier", 10);
        customMultipliers = new HashMap<>();

        if (config.isConfigurationSection("custom-multipliers")) {
            for (String key : config.getConfigurationSection("custom-multipliers").getKeys(false)) {
                try {
                    Material mat = Material.valueOf(key.toUpperCase());
                    int mult = config.getInt("custom-multipliers." + key);
                    customMultipliers.put(mat, Math.max(1, mult));
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Неверный материал в конфиге: " + key);
                }
            }
        }
    }

    private void modifyRecipes() {
        List<Recipe> recipes = new ArrayList<>();
        Bukkit.recipeIterator().forEachRemaining(recipes::add);

        for (Recipe recipe : recipes) {

            removeMatchingRecipes(recipe.getResult());

            if (recipe instanceof ShapedRecipe) {
                processShaped((ShapedRecipe) recipe);
            } else if (recipe instanceof ShapelessRecipe) {
                processShapeless((ShapelessRecipe) recipe);
            }
        }
    }


    private NamespacedKey getRecipeKey(Recipe recipe) {
        if (recipe instanceof Keyed) {
            return ((Keyed) recipe).getKey();
        } else {
            return null;
        }
    }

    private void removeMatchingRecipes(ItemStack result) {
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe recipe = it.next();
            if (recipe != null && recipe.getResult().isSimilar(result)) {
                it.remove();
            }
        }
    }



    private void processShaped(ShapedRecipe original) {
        NamespacedKey key = new NamespacedKey(this, UUID.randomUUID().toString());
        ShapedRecipe newRecipe = new ShapedRecipe(key, original.getResult());
        newRecipe.shape(original.getShape());

        for (Map.Entry<Character, RecipeChoice> entry : original.getChoiceMap().entrySet()) {
            RecipeChoice choice = entry.getValue();
            if (choice == null) continue;

            if (choice instanceof RecipeChoice.MaterialChoice) {
                Material mat = ((RecipeChoice.MaterialChoice) choice).getChoices().get(0);
                int multiplier = customMultipliers.getOrDefault(mat, defaultMultiplier);
                newRecipe.setIngredient(entry.getKey(), mat);
            } else {
                newRecipe.setIngredient(entry.getKey(), choice);
            }
        }

        Bukkit.addRecipe(newRecipe);
    }

    private void processShapeless(ShapelessRecipe original) {
        NamespacedKey key = new NamespacedKey(this, UUID.randomUUID().toString());
        ShapelessRecipe newRecipe = new ShapelessRecipe(key, original.getResult());

        int totalIngredients = 0;

        for (RecipeChoice choice : original.getChoiceList()) {
            if (choice == null) continue;

            if (choice instanceof RecipeChoice.MaterialChoice) {
                Material mat = ((RecipeChoice.MaterialChoice) choice).getChoices().get(0);
                int multiplier = customMultipliers.getOrDefault(mat, defaultMultiplier);

                for (int i = 0; i < multiplier; i++) {
                    newRecipe.addIngredient(new RecipeChoice.ExactChoice(new ItemStack(mat)));
                }

                totalIngredients += multiplier;
            } else {
                if (totalIngredients + 1 > 9) {
                    getLogger().warning("Shapeless-рецепт '" + original.getResult().getType() + "' пропущен — больше 9 ингредиентов.");
                    return;
                }

                newRecipe.addIngredient(choice);
                totalIngredients++;
            }
        }

        Bukkit.addRecipe(newRecipe);
    }

    // === Команды ===
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("craftmultiplier.use")) {
            sender.sendMessage(ChatColor.RED + "У тебя нет прав.");
            return true;
        }

        if (args.length == 0) {
            sendAuthorMessage((Player) sender);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            reloadConfig();
            this.config = getConfig();
            sender.sendMessage(ChatColor.GOLD + "[CraftMultiplier]" + ChatColor.GREEN  + " Конфиг и рецепты перезагружены.");
            return true;
        }

        if (args[0].equalsIgnoreCase("author")) {
            sendAuthorMessage((Player) sender);
            return true;
        }

        sendAuthorMessage((Player) sender);
        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("craftmultiplier.use")) return Collections.emptyList();

        if (args.length == 1) {
            List<String> suggestions = Arrays.asList("reload", "author");
            String current = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (String suggestion : suggestions) {
                if (suggestion.startsWith(current)) {
                    completions.add(suggestion);
                }
            }
            return completions;
        }

        return Collections.emptyList();
    }


    private void sendAuthorMessage(Player player) {
        player.sendMessage("");
        player.sendMessage("");
        player.sendMessage(ChatColor.DARK_GREEN + "=====[ " + ChatColor.GOLD + "CraftMultiplier" + ChatColor.DARK_GREEN + " ]=====");
        player.sendMessage("");
        player.sendMessage(ChatColor.YELLOW + "Автор: " + ChatColor.GREEN + "SteelRework");
        player.sendMessage("");




        TextComponent tg1Prefix = new TextComponent("• ");
        tg1Prefix.setColor(net.md_5.bungee.api.ChatColor.GOLD);

        TextComponent tg1Link = new TextComponent("Telegram: @steelrework");
        tg1Link.setColor(net.md_5.bungee.api.ChatColor.AQUA);
        tg1Link.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://t.me/steelrework"));
        tg1Link.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Открыть Telegram").create()));
        tg1Prefix.addExtra(tg1Link);
        player.spigot().sendMessage(tg1Prefix);

        player.sendMessage("");
        player.sendMessage(ChatColor.DARK_GREEN + "===============================");
        player.sendMessage("");
        player.sendMessage("");
    }


    private void sendSignatureToConsole(String pluginStatus) {
        getLogger().log(Level.INFO, "\n");
        getLogger().info("\u001B[35m!---------------CraftMultiplier Plugin " + pluginStatus + "---------------!\u001B[0m");
        getLogger().info("     \u001B[35m!---------------Made by SteelRework---------------!\u001B[0m");
        getLogger().info("     \u001B[35m!         Link: https://t.me/steelrework          !\u001B[0m");
        getLogger().log(Level.INFO, "\n");
    }
}
