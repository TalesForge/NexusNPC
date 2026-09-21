package com.talesforge.masternpc.api;

import com.google.common.base.Supplier;
import com.talesforge.masternpc.entity.custom.NpcEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

public final class NpcTypeProperties {
    // Fields without `private`: they are read by `MasterNpcApi` from the same package.
    MobCategory category = MobCategory.MISC;
    float width = 0.6F, height = 1.8F, eyeHeight = 1.62F;
    Supplier<AttributeSupplier.Builder> attributes = NpcEntity::createAttributes;
    boolean defaultRenderer = true;

    private NpcTypeProperties() {}

    public static NpcTypeProperties create() { return new NpcTypeProperties(); }

    public NpcTypeProperties category(MobCategory category) { this.category = category; return this; }
    public NpcTypeProperties size(float width, float height) { this.width = width; this.height = height; return this; }
    public NpcTypeProperties eyeHeight(float eyeHeight) { this.eyeHeight = eyeHeight; return this; }
    public NpcTypeProperties attributes(Supplier<AttributeSupplier.Builder> attributes) { this.attributes = attributes; return this; }
    /** The mod registers its own renderer; the standard one is not needed. */
    public NpcTypeProperties customRenderer() { this.defaultRenderer = false; return this; }
}
