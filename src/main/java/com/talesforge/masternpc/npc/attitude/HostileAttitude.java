package com.talesforge.masternpc.npc.attitude;

import com.talesforge.masternpc.config.Config;
import com.talesforge.masternpc.entity.custom.NpcEntity;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

/** It attacks players on its own. */
public class HostileAttitude extends NeutralAttitude {
    @Override
    public void createTargetGoals(NpcEntity npc, GoalSelector targets) {
        super.createTargetGoals(npc, targets);
        targets.addGoal(2, new NearestAttackableTargetGoal<>(npc, Player.class, true));
    }

    @Override
    public boolean isEnabled() { return Config.ALLOW_HOSTILE.get(); }
}
