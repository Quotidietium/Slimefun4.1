package io.github.thebusybiscuit.slimefun4.api.researches;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.apache.commons.lang.Validate;
import org.bukkit.NamespacedKey;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;

/**
 * A fluent builder for creating and registering {@link Research Researches}.
 *
 * <p>
 * This is a convenience API layered on top of the existing {@link Research} constructor
 * (which remains fully supported). It is especially handy when wiring up
 * {@link #addDependency(Research) prerequisite researches} or binding multiple items.
 * </p>
 *
 * <pre>
 * Research advanced = new ResearchBuilder()
 *     .key(new NamespacedKey(plugin, "advanced_smelting"))
 *     .name("Advanced Smelting")
 *     .cost(18)
 *     .addItem(SlimefunItems.ELECTRIC_FURNACE)
 *     .addDependency(basicSmeltingResearch)
 *     .register();
 * </pre>
 *
 * <p>
 * Either call {@link #build()} to obtain an unregistered {@link Research} (and
 * {@link Research#register()} it yourself), or call {@link #register()} to do both in one
 * step. A {@link NamespacedKey} and a name are always required.
 * </p>
 *
 * @author Zurker
 *
 * @see Research
 * @see Research#addDependency(Research)
 *
 */
public class ResearchBuilder {

    private NamespacedKey key;
    private int id = 0;
    private String name;
    private int cost = 1;
    private final Set<SlimefunItem> items = new LinkedHashSet<>();
    private final Set<Research> dependencies = new LinkedHashSet<>();

    /**
     * Sets the unique {@link NamespacedKey} identifying this {@link Research}.
     *
     * @param key
     *            A unique identifier for this {@link Research}
     *
     * @return This {@link ResearchBuilder}, for chaining
     */
    @Nonnull
    public ResearchBuilder key(@Nonnull NamespacedKey key) {
        Validate.notNull(key, "A NamespacedKey must be provided");
        this.key = key;
        return this;
    }

    /**
     * Sets the legacy numeric id of this {@link Research}.
     *
     * @param id
     *            The legacy numeric id (defaults to {@code 0})
     *
     * @return This {@link ResearchBuilder}, for chaining
     */
    @Nonnull
    public ResearchBuilder id(int id) {
        this.id = id;
        return this;
    }

    /**
     * Sets the displayed name of this {@link Research}.
     *
     * @param name
     *            The displayed name of this {@link Research}
     *
     * @return This {@link ResearchBuilder}, for chaining
     */
    @Nonnull
    public ResearchBuilder name(@Nonnull String name) {
        Validate.notNull(name, "A name must be specified");
        this.name = name;
        return this;
    }

    /**
     * Sets the XP-level cost to unlock this {@link Research}.
     *
     * @param cost
     *            The cost in XP levels (must be {@code >= 0}, defaults to {@code 1})
     *
     * @return This {@link ResearchBuilder}, for chaining
     */
    @Nonnull
    public ResearchBuilder cost(int cost) {
        if (cost < 0) {
            throw new IllegalArgumentException("Research cost must be zero or greater!");
        }

        this.cost = cost;
        return this;
    }

    /**
     * Binds the specified {@link SlimefunItem} to this {@link Research}.
     *
     * @param item
     *            The {@link SlimefunItem} to bind
     *
     * @return This {@link ResearchBuilder}, for chaining
     */
    @Nonnull
    public ResearchBuilder addItem(@Nonnull SlimefunItem item) {
        Validate.notNull(item, "The SlimefunItem cannot be null");
        this.items.add(item);
        return this;
    }

    /**
     * Adds a prerequisite {@link Research} that must be unlocked before this one.
     *
     * @param prerequisite
     *            The {@link Research} that must be unlocked first
     *
     * @return This {@link ResearchBuilder}, for chaining
     *
     * @see Research#addDependency(Research)
     */
    @Nonnull
    public ResearchBuilder addDependency(@Nonnull Research prerequisite) {
        Validate.notNull(prerequisite, "The prerequisite Research cannot be null");
        this.dependencies.add(prerequisite);
        return this;
    }

    /**
     * Builds the {@link Research} without registering it.
     *
     * @return The newly created (unregistered) {@link Research}
     */
    @Nonnull
    public Research build() {
        Validate.notNull(key, "A NamespacedKey must be provided (use ResearchBuilder#key)");
        Validate.notNull(name, "A name must be specified (use ResearchBuilder#name)");

        Research research = new Research(key, id, name, cost);

        for (SlimefunItem item : items) {
            research.addItems(item);
        }

        for (Research dep : dependencies) {
            research.addDependency(dep);
        }

        return research;
    }

    /**
     * Builds and registers the {@link Research}.
     *
     * @return The newly created and registered {@link Research}
     */
    @Nonnull
    public Research register() {
        Research research = build();
        research.register();
        return research;
    }
}
