package com.talesforge.masternpc.block;

import com.talesforge.masternpc.MasterNPC;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MasterNPC.MOD_ID);

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }



    // ===== BLOCKS =====
    // Creates a new Block with the id "masternpc:example_block"
    public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .destroyTime(-1)
                    .sound(SoundType.STONE)
    ));

}
