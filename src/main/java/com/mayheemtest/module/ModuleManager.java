package com.mayheemtest.module;

import com.mayheemtest.module.impl.HitboxModule;
import com.mayheemtest.module.impl.ReachModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Holds and provides access to every registered module.
 * Instantiated once by MayheemTestMod and referenced statically.
 */
public class ModuleManager {

    private static ModuleManager instance;

    private final List<AbstractModule> modules = new ArrayList<>();

    // Direct references for the mixins to access quickly
    public final ReachModule reach;
    public final HitboxModule hitbox;

    public ModuleManager() {
        instance = this;

        reach  = register(new ReachModule());
        hitbox = register(new HitboxModule());
    }

    private <T extends AbstractModule> T register(T module) {
        modules.add(module);
        return module;
    }

    public List<AbstractModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public static ModuleManager get() {
        return instance;
    }
}
