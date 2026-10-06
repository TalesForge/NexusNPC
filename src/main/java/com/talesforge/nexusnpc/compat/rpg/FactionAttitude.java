package com.talesforge.nexusnpc.compat.rpg;

import com.talesforge.nexusnpc.config.Config;
import com.talesforge.nexusnpc.entity.custom.NpcEntity;
import com.talesforge.nexusnpc.npc.attitude.NeutralAttitude;
import com.talesforge.nexusrpg.api.NexusRPGApi;
import com.talesforge.nexusrpg.api.faction.Relation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;

/**
 * Attitude driven by NexusRPG relations: it fights back when hit (like {@link NeutralAttitude}) and also
 * attacks anything the NPC's faction regards as an ENEMY (members of one team count as allies). The relation
 * is looked up live, so changing factions or relations never requires rebuilding the AI.
 * Like the hostile attitude it can make NPCs attack players, so the same config switch applies.
 */
public class FactionAttitude extends NeutralAttitude {
    @Override
    public void createTargetGoals(NpcEntity npc, GoalSelector targets) {
        super.createTargetGoals(npc, targets);
        targets.addGoal(2, new NearestAttackableTargetGoal<>(npc, LivingEntity.class, 10, true, false,
                target -> target != npc && NexusRPGApi.profiles().relation(npc, target) == Relation.ENEMY));
    }

    @Override
    public boolean isEnabled() { return Config.ALLOW_HOSTILE.get(); }
}
