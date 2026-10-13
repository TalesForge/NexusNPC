package com.talesforge.nexusnpc.npc.model;

/**
 * How much longer (or shorter) than normal each part of a humanoid body is, in MODEL PIXELS (1 px = 1/16 block
 * before the model's render scale). Negative = shorter. The head, hands and feet are never changed.
 * <p>
 * This is how NPCs of different heights are drawn WITHOUT scaling: instead of stretching the model (which makes
 * pixels bigger, or rectangular, and limbs spindly) the limbs and torso get extra length, and the extra length is
 * filled by repeating a band of the texture — so every texel stays square and the same size, on every NPC.
 * Real people of different heights differ in the same way: much the same head, longer legs and torso.
 * <p>
 * Pure maths, no client classes: the server never needs it, but it lives next to {@link NpcModelType} because the
 * two together define what "height" means for a model.
 */
public record HeightProfile(float legs, float torso, float arms) {
    public static final HeightProfile NONE = new HeightProfile(0, 0, 0);

    /**
     * How the extra height is shared out. Only legs + torso raise the head, so together they carry ALL of it
     * (shares sum to 1); the arms are not part of the total and simply stay in proportion: half the extra of the
     * body, since an arm is about half as long as legs + torso.
     */
    private static final float LEGS_SHARE = 0.60F;
    private static final float TORSO_SHARE = 0.40F;
    private static final float ARMS_SHARE = 0.50F;

    /** Rounding step in px. Fine enough to be invisible, coarse enough that models can be cached per height. */
    private static final float STEP = 0.25F;

    /** The profile of an NPC of {@code height} blocks. */
    public static HeightProfile of(NpcModelType model, float height) {
        float px = (model.clampHeight(height) - model.height()) * 16.0F / model.renderScale();
        return new HeightProfile(snap(px * LEGS_SHARE), snap(px * TORSO_SHARE), snap(px * ARMS_SHARE));
    }

    private static float snap(float value) {
        return Math.round(value / STEP) * STEP;
    }

    public boolean isNone() {
        return legs == 0 && torso == 0 && arms == 0;
    }
}
