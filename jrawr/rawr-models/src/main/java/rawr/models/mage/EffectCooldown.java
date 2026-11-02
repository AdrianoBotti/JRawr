// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/Solver.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import rawr.base.effects.SpecialEffect;

import java.awt.Color;
import java.util.List;

public class EffectCooldown {
    public int mask;

    public int getMask() {
        return this.mask;
    }

    public void setMask(int value) {
        this.mask = value;
    }

    public boolean itemBased;

    public boolean getItemBased() {
        return this.itemBased;
    }

    public void setItemBased(boolean value) {
        this.itemBased = value;
    }

    // C# StandardEffect is a [Flags] enum; this port uses int constants in StandardEffect.
    public int standardEffect;

    public int getStandardEffect() {
        return this.standardEffect;
    }

    public void setStandardEffect(int value) {
        this.standardEffect = value;
    }

    public SpecialEffect specialEffect;

    public SpecialEffect getSpecialEffect() {
        return this.specialEffect;
    }

    public void setSpecialEffect(SpecialEffect value) {
        this.specialEffect = value;
    }

    public float cooldown;

    public float getCooldown() {
        return this.cooldown;
    }

    public void setCooldown(float value) {
        this.cooldown = value;
    }

    public float duration;

    public float getDuration() {
        return this.duration;
    }

    public void setDuration(float value) {
        this.duration = value;
    }

    public boolean hasteEffect;

    public boolean getHasteEffect() {
        return this.hasteEffect;
    }

    public void setHasteEffect(boolean value) {
        this.hasteEffect = value;
    }

    public int row;

    public int getRow() {
        return this.row;
    }

    public void setRow(int value) {
        this.row = value;
    }

    public List<SegmentConstraint> segmentConstraints;

    public List<SegmentConstraint> getSegmentConstraints() {
        return this.segmentConstraints;
    }

    public void setSegmentConstraints(List<SegmentConstraint> value) {
        this.segmentConstraints = value;
    }

    public String name;

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public float maximumDuration;

    public float getMaximumDuration() {
        return this.maximumDuration;
    }

    public void setMaximumDuration(float value) {
        this.maximumDuration = value;
    }

    public boolean automaticConstraints;

    public boolean getAutomaticConstraints() {
        return this.automaticConstraints;
    }

    public void setAutomaticConstraints(boolean value) {
        this.automaticConstraints = value;
    }

    public boolean automaticStackingConstraints;

    public boolean getAutomaticStackingConstraints() {
        return this.automaticStackingConstraints;
    }

    public void setAutomaticStackingConstraints(boolean value) {
        this.automaticStackingConstraints = value;
    }

    public Color color;

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color value) {
        this.color = value;
    }

    public void clear() {
        // Row is always initialized in ConstructRows
        // MaximumDuration is initialized in InitializeEffectCooldowns or in ConstructRows if AutomaticConstraints is true
        if (this.segmentConstraints != null) {
            this.segmentConstraints.clear();
        }
    }

    public EffectCooldown clone() {
        EffectCooldown clone = new EffectCooldown();
        clone.mask = this.mask;
        clone.itemBased = this.itemBased;
        clone.standardEffect = this.standardEffect;
        clone.specialEffect = this.specialEffect;
        clone.cooldown = this.cooldown;
        clone.duration = this.duration;
        clone.hasteEffect = this.hasteEffect;
        clone.row = this.row;
        clone.segmentConstraints = null;
        clone.name = this.name;
        clone.maximumDuration = this.maximumDuration;
        clone.automaticConstraints = this.automaticConstraints;
        clone.automaticStackingConstraints = this.automaticStackingConstraints;
        clone.color = this.color;
        return clone;
    }
}
