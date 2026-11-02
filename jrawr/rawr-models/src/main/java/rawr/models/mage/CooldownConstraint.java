// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SequenceItem.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CooldownConstraint {
    public SequenceGroup group;
    public double cooldown;
    public double duration;
    public boolean coldSnap;
    public EffectCooldown effectCooldown;

    public SequenceGroup getGroup() {
        return group;
    }

    public void setGroup(SequenceGroup group) {
        this.group = group;
    }

    public double getCooldown() {
        return cooldown;
    }

    public void setCooldown(double cooldown) {
        this.cooldown = cooldown;
    }

    public double getDuration() {
        return duration;
    }

    public void setDuration(double duration) {
        this.duration = duration;
    }

    public boolean getColdSnap() {
        return coldSnap;
    }

    public void setColdSnap(boolean coldSnap) {
        this.coldSnap = coldSnap;
    }

    public EffectCooldown getEffectCooldown() {
        return effectCooldown;
    }

    public void setEffectCooldown(EffectCooldown effectCooldown) {
        this.effectCooldown = effectCooldown;
    }
}
