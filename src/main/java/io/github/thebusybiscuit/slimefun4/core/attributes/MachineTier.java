package io.github.thebusybiscuit.slimefun4.core.attributes;

import javax.annotation.Nonnull;

public enum MachineTier {

    BASIC("&e基础"),
    AVERAGE("&6标准"),
    MEDIUM("&a中级"),
    GOOD("&2良好"),
    ADVANCED("&6高级"),
    END_GAME("&4终局");

    private final String prefix;

    MachineTier(@Nonnull String prefix) {
        this.prefix = prefix;
    }

    @Override
    public String toString() {
        return prefix;
    }

}
