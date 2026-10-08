package com.talesforge.nexusnpc.mixin;

import com.talesforge.nexusnpc.npc.runtime.NpcEditing;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * While an NPC is being edited it must stand still and not attack. {@code isImmobile()} is the
 * vanilla switch for exactly that (LivingEntity#aiStep skips the AI step when it is true), and it
 * works for every mob type, goal-driven or Brain-driven.
 * <p>
 * Injected into LivingEntity (not Mob) because Mob does not declare the method itself.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "isImmobile", at = @At("RETURN"), cancellable = true)
    private void nexusnpc$freezeWhileEdited(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && (Object) this instanceof Mob mob && NpcEditing.isBeingEdited(mob)) {
            cir.setReturnValue(true);
        }
    }
}
