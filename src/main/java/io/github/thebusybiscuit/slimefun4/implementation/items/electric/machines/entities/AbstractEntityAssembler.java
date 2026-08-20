package io.github.thebusybiscuit.slimefun4.implementation.items.electric.machines.entities;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.items.CustomItemStack;
import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.events.AsyncEntityAssembleEvent;
import io.github.thebusybiscuit.slimefun4.api.events.BlockPlacerPlaceEvent;
import io.github.thebusybiscuit.slimefun4.api.events.EntityAssemblerToggleEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.items.SimpleSlimefunItem;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.thebusybiscuit.slimefun4.utils.NumberUtils;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;

import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.inventory.DirtyChestMenu;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;

/**
 * This is an abstract super class for Entity Assemblers.
 *
 * @author TheBusyBiscuit
 *
 * @see WitherAssembler
 * @see IronGolemAssembler
 *
 */
public abstract class AbstractEntityAssembler<T extends Entity> extends SimpleSlimefunItem<BlockTicker> implements EnergyNetComponent {

    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_OFFSET = "offset";

    private final int[] border = { 0, 2, 3, 4, 5, 6, 8, 12, 14, 21, 23, 30, 32, 39, 40, 41 };
    private final int[] inputSlots = { 19, 28, 25, 34 };

    private final int[] headSlots = { 19, 28 };
    private final int[] headBorder = { 9, 10, 11, 18, 20, 27, 29, 36, 37, 38 };

    private final int[] bodySlots = { 25, 34 };
    private final int[] bodyBorder = { 15, 16, 17, 24, 26, 33, 35, 42, 43, 44 };

    private int lifetime = 0;

    @ParametersAreNonnullByDefault
    protected AbstractEntityAssembler(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);

        new BlockMenuPreset(getId(), item.getItemMetaSnapshot().getDisplayName().orElse("Entity Assembler")) {

            @Override
            public void init() {
                drawBackground(border);
                drawBackground(CustomItemStack.create(getHeadBorder(), " "), headBorder);
                drawBackground(CustomItemStack.create(getBodyBorder(), " "), bodyBorder);

                constructMenu(this);
            }

            @Override
            public void newInstance(BlockMenu menu, Block b) {
                updateBlockInventory(menu, b);
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                return p.hasPermission("slimefun.inventory.bypass") || Slimefun.getProtectionManager().hasPermission(p, b.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.INSERT) {
                    return inputSlots;
                } else {
                    return new int[0];
                }
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(DirtyChestMenu menu, ItemTransportFlow flow, ItemStack item) {
                if (flow == ItemTransportFlow.INSERT && item != null) {
                    if (item.getType() == getBody().getType()) {
                        return bodySlots;
                    }

                    if (item.getType() == getHead().getType()) {
                        return headSlots;
                    }
                }

                return new int[0];
            }
        };

        addItemHandler(onPlace(), onBreak());
    }

    @Nonnull
    private BlockPlaceHandler onPlace() {
        return new BlockPlaceHandler(true) {

            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                onPlace(e);
            }

            @Override
            public void onBlockPlacerPlace(BlockPlacerPlaceEvent e) {
                onPlace(e);
            }

            private void onPlace(BlockEvent e) {
                BlockStorage.addBlockInfo(e.getBlock(), KEY_OFFSET, "3.0");
                BlockStorage.addBlockInfo(e.getBlock(), KEY_ENABLED, String.valueOf(false));
            }
        };
    }

    @Nonnull
    private BlockBreakHandler onBreak() {
        return new BlockBreakHandler(false, false) {

            @Override
            public void onPlayerBreak(BlockBreakEvent e, ItemStack item, List<ItemStack> drops) {
                Block b = e.getBlock();
                BlockMenu inv = BlockStorage.getInventory(b);

                if (inv != null) {
                    inv.dropItems(b.getLocation(), headSlots);
                    inv.dropItems(b.getLocation(), bodySlots);
                }
            }
        };
    }

    private void updateBlockInventory(BlockMenu menu, Block b) {
        if (!BlockStorage.hasBlockInfo(b) || BlockStorage.getLocationInfo(b.getLocation(), KEY_ENABLED) == null || BlockStorage.getLocationInfo(b.getLocation(), KEY_ENABLED).equals(String.valueOf(false))) {
            menu.replaceExistingItem(22, CustomItemStack.create(Material.GUNPOWDER, "&7已启用： &4\u2718", "", "&e> 点击开启此机器"));
            menu.addMenuClickHandler(22, (p, slot, item, action) -> {
                toggleEnabled(p, menu, b, true);
                return false;
            });
        } else {
            menu.replaceExistingItem(22, CustomItemStack.create(Material.REDSTONE, "&7已启用： &2\u2714", "", "&e> 点击关闭此机器"));
            menu.addMenuClickHandler(22, (p, slot, item, action) -> {
                toggleEnabled(p, menu, b, false);
                return false;
            });
        }

        double offset = getSpawnOffset(b);

        menu.replaceExistingItem(31, CustomItemStack.create(Material.PISTON, "&7偏移： &3" + offset + " Block(s)", "", "&f左键： &7+0.1", "&f右键： &7-0.1"));
        menu.addMenuClickHandler(31, (p, slot, item, action) -> {
            double offsetv = NumberUtils.reparseDouble(getSpawnOffset(b) + (action.isRightClicked() ? -0.1F : 0.1F));
            BlockStorage.addBlockInfo(b, KEY_OFFSET, String.valueOf(offsetv));
            updateBlockInventory(menu, b);
            return false;
        });
    }

    /**
     * Reads the spawn offset of the given assembler {@link Block} from
     * {@link BlockStorage}, defaulting to {@code 3.0} when the value is missing
     * or corrupted. A non-numeric stored value previously crashed the menu and
     * threw an NPE in the click handler.
     */
    private static double getSpawnOffset(@Nonnull Block b) {
        if (!BlockStorage.hasBlockInfo(b)) {
            return 3.0F;
        }

        String offsetData = BlockStorage.getLocationInfo(b.getLocation(), KEY_OFFSET);

        if (offsetData == null) {
            return 3.0F;
        }

        try {
            return Double.parseDouble(offsetData);
        } catch (NumberFormatException x) {
            return 3.0F;
        }
    }

    /**
     * This toggles the enabled state of this assembler after the {@link Player}
     * clicked the toggle button.
     * <p>
     * Package-private so that regression tests can drive the toggle directly;
     * reaching it through the menu would require simulating a click in a
     * {@link BlockMenuPreset}-bound inventory, which MockBukkit cannot provide.
     *
     * @param p
     *            The {@link Player} who clicked the button
     * @param menu
     *            The {@link BlockMenu} to redraw after the toggle
     * @param b
     *            The assembler {@link Block}
     * @param enable
     *            The state the clicked button applies
     */
    void toggleEnabled(@Nonnull Player p, @Nonnull BlockMenu menu, @Nonnull Block b, boolean enable) {
        if (EntityAssemblerToggleEvent.getHandlerList().getRegisteredListeners().length > 0) {
            EntityAssemblerToggleEvent event = new EntityAssemblerToggleEvent(p, this, b, enable);
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                // An addon vetoed the toggle; the state stays and the menu is left as-is.
                return;
            }
        }

        BlockStorage.addBlockInfo(b, KEY_ENABLED, String.valueOf(enable));
        updateBlockInventory(menu, b);
    }

    @Override
    public BlockTicker getItemHandler() {
        return new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem sf, Config data) {
                if ("false".equals(BlockStorage.getLocationInfo(b.getLocation(), KEY_ENABLED))) {
                    return;
                }

                if (lifetime % 60 == 0 && getCharge(b.getLocation(), data) >= getEnergyConsumption()) {
                    BlockMenu menu = BlockStorage.getInventory(b);

                    if (menu == null) {
                        return;
                    }

                    boolean hasBody = findResource(menu, getBody(), bodySlots);
                    boolean hasHead = findResource(menu, getHead(), headSlots);

                    if (hasBody && hasHead) {
                        Location spawnLocation = null;

                        if (AsyncEntityAssembleEvent.getHandlerList().getRegisteredListeners().length > 0) {
                            AsyncEntityAssembleEvent event = new AsyncEntityAssembleEvent(AbstractEntityAssembler.this, b);
                            Bukkit.getPluginManager().callEvent(event);

                            if (event.isCancelled()) {
                                return;
                            }

                            // An addon may have redirected the spawn
                            spawnLocation = event.getSpawnLocation();
                        }

                        String offsetData = BlockStorage.getLocationInfo(b.getLocation(), KEY_OFFSET);
                        double offset;

                        try {
                            offset = Double.parseDouble(offsetData);
                        } catch (NullPointerException | NumberFormatException x) {
                            /*
                             * Missing or corrupted offset data: skip this assembly.
                             * This must happen BEFORE any resources are consumed,
                             * otherwise every failed tick would eat a full set of
                             * materials and energy, and repeated failures would let
                             * the TickerTask destroy this machine.
                             */
                            return;
                        }

                        consumeResources(menu);

                        removeCharge(b.getLocation(), getEnergyConsumption());

                        Location customLocation = spawnLocation;
                        Slimefun.runSync(() -> {
                            Location loc = customLocation != null ? customLocation : new Location(b.getWorld(), b.getX() + 0.5D, b.getY() + offset, b.getZ() + 0.5D);
                            spawnEntity(loc);

                            // BlockData is the primary data type of this effect
                            Material headMaterial = getHead().getType();
                            Object particleData = headMaterial.isBlock() ? headMaterial.createBlockData() : headMaterial;
                            b.getWorld().playEffect(b.getLocation(), Effect.STEP_SOUND, particleData);
                        });
                    }
                }
            }

            @Override
            public void uniqueTick() {
                lifetime++;
            }

            @Override
            public boolean isSynchronized() {
                return false;
            }
        };
    }

    private boolean findResource(BlockMenu menu, ItemStack item, int[] slots) {
        int found = 0;

        for (int slot : slots) {
            if (SlimefunUtils.isItemSimilar(menu.getItemInSlot(slot), item, true, false)) {
                found += menu.getItemInSlot(slot).getAmount();

                if (found >= item.getAmount()) {
                    return true;
                }
            }
        }

        return false;
    }

    private void consumeResources(BlockMenu inv) {
        int bodyCount = getBody().getAmount();
        int headCount = getHead().getAmount();

        for (int slot : bodySlots) {
            if (SlimefunUtils.isItemSimilar(inv.getItemInSlot(slot), getBody(), true, false)) {
                int amount = inv.getItemInSlot(slot).getAmount();

                if (amount >= bodyCount) {
                    inv.consumeItem(slot, bodyCount);
                    break;
                } else {
                    bodyCount -= amount;
                    inv.replaceExistingItem(slot, null);
                }
            }
        }

        for (int slot : headSlots) {
            if (SlimefunUtils.isItemSimilar(inv.getItemInSlot(slot), getHead(), true, false)) {
                int amount = inv.getItemInSlot(slot).getAmount();

                if (amount >= headCount) {
                    inv.consumeItem(slot, headCount);
                    break;
                } else {
                    headCount -= amount;
                    inv.replaceExistingItem(slot, null);
                }
            }
        }
    }

    protected void constructMenu(BlockMenuPreset preset) {
        preset.addItem(1, CustomItemStack.create(getHead(), "&7头部槽位", "", "&f此槽位接受对应的头部物品"), ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(7, CustomItemStack.create(getBody(), "&7身体槽位", "", "&f此槽位接受对应的身体物品"), ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(13, CustomItemStack.create(Material.CLOCK, "&7冷却： &b30 秒", "", "&f此机器运作一次最多需要半分钟", "&f请给它一点时间！"), ChestMenuUtils.getEmptyClickHandler());
    }

    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    public abstract int getEnergyConsumption();

    public abstract ItemStack getHead();

    public abstract ItemStack getBody();

    public abstract Material getHeadBorder();

    public abstract Material getBodyBorder();

    public abstract T spawnEntity(Location l);

}
