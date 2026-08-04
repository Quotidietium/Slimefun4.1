package io.github.thebusybiscuit.slimefun4.core.attributes;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * This is an {@link ItemAttribute} that marks a {@link SlimefunItem} as <b>edible food</b>.
 *
 * <p>
 * Attaching this marker lets other add-ons recognise consumable items, for example via
 * {@code SlimefunItem.getByItem(stack) instanceof SlimefunFood} or inside a
 * {@link io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemConsumeEvent} handler
 * (see {@link #isFood()} on that event).
 * </p>
 *
 * <p>
 * It carries no behaviour of its own; per-food effects are expressed through the
 * {@link io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler}, while
 * cross-cutting dietary effects are expressed through
 * {@link io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemConsumeEvent}.
 * </p>
 *
 * <p>
 * Built-in foods such as {@link io.github.thebusybiscuit.slimefun4.implementation.items.food.Juice},
 * {@code DietCookie}, {@code FortuneCookie}, {@code MonsterJerky} and {@code MeatJerky} already
 * implement this interface.
 * </p>
 *
 * @author Zurker
 *
 * @see io.github.thebusybiscuit.slimefun4.api.events.SlimefunItemConsumeEvent
 * @see io.github.thebusybiscuit.slimefun4.core.handlers.ItemConsumptionHandler
 *
 */
public interface SlimefunFood extends ItemAttribute {
}
