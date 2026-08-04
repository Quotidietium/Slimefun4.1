package io.github.thebusybiscuit.slimefun4.core.attributes;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * This is an {@link ItemAttribute} that marks a {@link SlimefunItem} as a <b>weapon</b>.
 *
 * <p>
 * Attaching this marker to a custom item lets other add-ons (and Slimefun itself) recognise
 * the item as a weapon - for example via {@code SlimefunItem.getByItem(stack) instanceof SlimefunWeapon}.
 * It carries no behaviour of its own; all combat effects are expressed through the combat
 * handlers ({@link io.github.thebusybiscuit.slimefun4.core.handlers.WeaponUseHandler},
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.BowShootHandler}) and events
 * ({@link io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemDamageEvent}).
 * </p>
 *
 * <p>
 * Built-in weapons such as {@link io.github.thebusybiscuit.slimefun4.implementation.items.weapons.SlimefunBow SlimefunBow},
 * {@code VampireBlade}, {@code SeismicAxe} and {@code SwordOfBeheading} already implement this
 * interface, so {@code instanceof SlimefunWeapon} works out of the box for them.
 * </p>
 *
 * @author Zurker
 *
 * @see io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemDamageEvent
 * @see io.github.thebusybiscuit.slimefun4.api.events.SlimefunBowShootEvent
 *
 */
public interface SlimefunWeapon extends ItemAttribute {
}
