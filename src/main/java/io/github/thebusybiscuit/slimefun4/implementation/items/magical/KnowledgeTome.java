package io.github.thebusybiscuit.slimefun4.implementation.items.magical;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import io.github.bakedlibs.dough.common.ChatColors;
import io.github.bakedlibs.dough.items.ItemUtils;
import io.github.thebusybiscuit.slimefun4.api.events.KnowledgeTomeBindEvent;
import io.github.thebusybiscuit.slimefun4.api.events.KnowledgeTomeShareEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.player.PlayerProfile;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.api.researches.Research;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;

/**
 * The {@link KnowledgeTome} allows you to copy every unlocked {@link Research}
 * from one {@link Player} to another.
 *
 * @author TheBusyBiscuit
 *
 */
public class KnowledgeTome extends SimpleSlimefunItem<ItemUseHandler> {

    @ParametersAreNonnullByDefault
    public KnowledgeTome(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @Override
    public @Nonnull ItemUseHandler getItemHandler() {
        return e -> {
            Player p = e.getPlayer();
            ItemStack item = e.getItem();

            e.setUseBlock(Result.DENY);

            ItemMeta im = item.getItemMeta();

            // A Knowledge Tome must carry at least two lore lines (owner + uuid).
            // Abort gracefully if the lore was stripped/edited instead of throwing NPE/IOOBE.
            if (im == null || !im.hasLore()) {
                return;
            }

            List<String> lore = im.getLore();

            if (lore.size() < 2) {
                return;
            }

            if (lore.get(1).isEmpty()) {
                if (KnowledgeTomeBindEvent.getHandlerList().getRegisteredListeners().length > 0) {
                    KnowledgeTomeBindEvent event = new KnowledgeTomeBindEvent(p, this, item);
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        // An addon vetoed the binding; the tome stays unbound.
                        return;
                    }
                }

                lore.set(0, ChatColors.color("&7Owner: &b" + p.getName()));
                lore.set(1, ChatColor.BLACK + "" + p.getUniqueId());
                im.setLore(lore);
                item.setItemMeta(im);
                SoundEffect.TOME_OF_KNOWLEDGE_USE_SOUND.playFor(p);
            } else {
                UUID uuid;

                try {
                    uuid = UUID.fromString(ChatColor.stripColor(lore.get(1)));
                } catch (IllegalArgumentException x) {
                    return;
                }

                if (p.getUniqueId().equals(uuid)) {
                    Slimefun.getLocalization().sendMessage(p, "messages.no-tome-yourself");
                    return;
                }

                if (KnowledgeTomeShareEvent.getHandlerList().getRegisteredListeners().length > 0) {
                    KnowledgeTomeShareEvent event = new KnowledgeTomeShareEvent(p, this, item, uuid);
                    Bukkit.getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        // An addon vetoed the sharing; no researches are copied and the tome is kept.
                        return;
                    }

                    // An addon may have redirected the research source
                    uuid = event.getOwner();
                }

                final UUID sourceUuid = uuid;
                PlayerProfile.get(p, profile -> PlayerProfile.fromUUID(sourceUuid, owner -> {
                    for (Research research : owner.getResearches()) {
                        research.unlock(p, true);
                    }
                }));

                if (p.getGameMode() != GameMode.CREATIVE) {
                    ItemUtils.consumeItem(item, false);
                }
            }
        };
    }
}
