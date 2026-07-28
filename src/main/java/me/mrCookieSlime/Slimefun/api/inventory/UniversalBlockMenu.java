package me.mrCookieSlime.Slimefun.api.inventory;

import java.io.File;

import io.github.bakedlibs.dough.config.Config;

// This class will be deprecated, relocated and rewritten in a future version.
public class UniversalBlockMenu extends DirtyChestMenu {

    public UniversalBlockMenu(BlockMenuPreset preset) {
        super(preset);

        preset.clone(this);

        save();
    }

    public UniversalBlockMenu(BlockMenuPreset preset, Config cfg) {
        super(preset);

        for (int i = 0; i < 54; i++) {
            if (cfg.contains(String.valueOf(i))) {
                addItem(i, cfg.getItem(String.valueOf(i)));
            }
        }

        preset.clone(this);

        if (preset.getSize() > -1 && !preset.getPresetSlots().contains(preset.getSize() - 1) && cfg.contains(String.valueOf(preset.getSize() - 1))) {
            addItem(preset.getSize() - 1, cfg.getItem(String.valueOf(preset.getSize() - 1)));
        }

        this.getContents();
    }

    public void save() {
        int unsavedChanges = changes.get();

        if (unsavedChanges <= 0) {
            return;
        }

        // To force CS-CoreLib to build the Inventory
        this.getContents();

        File file = new File("data-storage/Slimefun/universal-inventories/" + preset.getID() + ".sfi");
        Config cfg = new Config(file);
        cfg.setValue("preset", preset.getID());

        for (int slot : preset.getInventorySlots()) {
            cfg.setValue(String.valueOf(slot), getItemInSlot(slot));
        }

        if (saveAtomically(cfg)) {
            // Only subtract what was written - changes made during the write stay pending
            changes.addAndGet(-unsavedChanges);
        }
    }

}
