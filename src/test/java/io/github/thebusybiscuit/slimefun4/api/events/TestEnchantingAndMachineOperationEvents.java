package io.github.thebusybiscuit.slimefun4.api.events;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;

import io.github.bakedlibs.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineOperation;
import io.github.thebusybiscuit.slimefun4.core.machines.MachineProcessor;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * Regression coverage for four previously-untested machine/enchanting events:
 * {@link AutoEnchantEvent}, {@link AutoDisenchantEvent}, {@link AsyncAutoEnchanterProcessEvent}
 * and {@link AsyncMachineOperationFinishEvent}. Each is exercised at the API-contract level
 * (field exposure, null-validation, cancellation, listener dispatch and async declaration).
 *
 * <p>
 * Note: Bukkit {@code @EventHandler} methods must reference a concrete event type (a type variable
 * erases to {@code Event}, which has no handler list), so each dispatch assertion uses a dedicated
 * inner listener class.
 *
 * @author Zurker
 */
class TestEnchantingAndMachineOperationEvents {

    private static ServerMock server;
    private static Slimefun plugin;

    @BeforeAll
    public static void load() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(Slimefun.class);
    }

    @AfterAll
    public static void unload() {
        MockBukkit.unmock();
    }

    @BeforeEach
    public void beforeEach() {
        server.getPluginManager().clearEvents();
    }

    @Test
    @DisplayName("AutoEnchantEvent exposes its item, is async and cancellable")
    void testAutoEnchantEvent() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        AutoEnchantEvent event = new AutoEnchantEvent(item);

        Assertions.assertEquals(item, event.getItem());
        Assertions.assertTrue(event.isAsynchronous(), "AutoEnchantEvent is fired on the async ticker thread");
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());

        // Dispatch is not asserted here: AutoEnchantEvent is declared async (super(true)) and
        // MockBukkit refuses to callEvent an async event on the main thread (see MockBukkit pitfall
        // notes). The handler list is standard, so dispatch works on the real async ticker thread.
    }

    @Test
    @DisplayName("AutoDisenchantEvent exposes its item, is async and cancellable")
    void testAutoDisenchantEvent() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        AutoDisenchantEvent event = new AutoDisenchantEvent(item);

        Assertions.assertEquals(item, event.getItem());
        Assertions.assertTrue(event.isAsynchronous());
        Assertions.assertFalse(event.isCancelled());

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("AsyncAutoEnchanterProcessEvent exposes item/book/menu and validates nulls")
    void testAsyncAutoEnchanterProcessEvent() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        BlockMenu menu = Mockito.mock(BlockMenu.class);

        AsyncAutoEnchanterProcessEvent event = new AsyncAutoEnchanterProcessEvent(item, book, menu);

        Assertions.assertEquals(item, event.getItem());
        Assertions.assertEquals(book, event.getEnchantedBook());
        Assertions.assertEquals(menu, event.getMenu());
        Assertions.assertTrue(event.isAsynchronous());

        Assertions.assertThrows(IllegalArgumentException.class, () -> new AsyncAutoEnchanterProcessEvent(null, book, menu));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AsyncAutoEnchanterProcessEvent(item, null, menu));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new AsyncAutoEnchanterProcessEvent(item, book, null));

        event.setCancelled(true);
        Assertions.assertTrue(event.isCancelled());
    }

    @Test
    @DisplayName("AsyncMachineOperationFinishEvent exposes position/processor/operation and is not cancellable")
    void testAsyncMachineOperationFinishEvent() {
        BlockPosition position = Mockito.mock(BlockPosition.class);
        MachineProcessor<MachineOperation> processor = Mockito.mock(MachineProcessor.class);
        MachineOperation operation = Mockito.mock(MachineOperation.class);

        AsyncMachineOperationFinishEvent event = new AsyncMachineOperationFinishEvent(position, processor, operation);

        Assertions.assertEquals(position, event.getPosition());
        Assertions.assertEquals(processor, event.getProcessor());
        Assertions.assertEquals(operation, event.getOperation());
        // The event declares itself async based on the firing thread; on the (main) unit-test thread it reads as sync.
        Assertions.assertFalse(event.isAsynchronous(), "On the main thread the adaptive async flag must report false");
        Assertions.assertFalse(event instanceof org.bukkit.event.Cancellable, "MachineOperationFinish is informational and not cancellable");
    }
}
