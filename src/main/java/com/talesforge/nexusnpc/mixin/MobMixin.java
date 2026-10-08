package com.talesforge.nexusnpc.mixin;

import com.talesforge.nexusnpc.npc.runtime.NpcHost;
import com.talesforge.nexusnpc.npc.runtime.NpcRuntime;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Makes every {@link Mob} an {@link NpcHost}: one lazily-allocated transient state object plus
 * access to the (protected) goal selectors. Deliberately contains NO behavior changes — all NPC
 * logic is in regular classes and is only ever reached for mobs that carry NPC data.
 */
@Mixin(Mob.class)
public abstract class MobMixin implements NpcHost {
    @Shadow @Final protected GoalSelector goalSelector;
    @Shadow @Final protected GoalSelector targetSelector;

    @Unique private NpcRuntime nexusnpc$runtime;

    @Override
    public NpcRuntime nexusnpc$getRuntime() {
        if (nexusnpc$runtime == null) nexusnpc$runtime = new NpcRuntime();
        return nexusnpc$runtime;
    }

    @Override
    public NpcRuntime nexusnpc$peekRuntime() { return nexusnpc$runtime; }

    @Override
    public GoalSelector nexusnpc$goalSelector() { return goalSelector; }

    @Override
    public GoalSelector nexusnpc$targetSelector() { return targetSelector; }
}
