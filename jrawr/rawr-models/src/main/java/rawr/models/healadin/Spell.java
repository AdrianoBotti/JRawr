// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Healadin/Spell.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// (the Spell hierarchy lives in Spell.cs in the original).
package rawr.models.healadin;

import rawr.base.util.NetFormat;
import rawr.base.talents.PaladinTalents;
import rawr.base.stats.Stats;

public abstract class Spell {
    private Rotation rotation;

    public Rotation getRotation() {
        return this.rotation;
    }

    public void setRotation(Rotation value) {
        this.rotation = value;
        this.stats = this.rotation.getStats();
        this.talents = this.rotation.getTalents();
    }

    private Stats stats;

    protected Stats getStats() {
        return this.stats;
    }

    private PaladinTalents talents;

    protected PaladinTalents getTalents() {
        return this.talents;
    }

    public Spell(Rotation rotation) {
        setRotation(rotation);
    }

    public float uptime;

    public float getUptime() {
        return uptime;
    }

    public void setUptime(float value) {
        uptime = value;
    }

    public float duration;

    public float getDuration() {
        return duration;
    }

    public void setDuration(float value) {
        duration = value;
    }

    public float baseCost;

    public float getBaseCost() {
        return baseCost;
    }

    public void setBaseCost(float value) {
        baseCost = value;
    }

    public float cost() {
        return (baseCost - getStats().getSpellsManaReduction()) * (1f - .02f * getTalents().getBenediction());
    }

    public float casts() {
        return (float) Math.ceil(uptime / duration);
    }

    public float castTime() {
        return (float) Math.max(1f, 1.5f / (1f + getStats().getSpellHaste()));
    }

    public float time() {
        return casts() * castTime();
    }

    public float usage() {
        return casts() * cost();
    }
}

class SacredShield extends Spell {
    public SacredShield(Rotation rotation) {
        super(rotation);
        duration = 30f + getTalents().getDivineGuardian() * 15f;
        uptime = getRotation().fightLength * getRotation().getCalcOpts().getSSUptime();
        baseCost = 527f;
    }

    public float icd() {
        return 6f - getStats().getSacredShieldICDReduction();
    }

    public float procAbsorb() {
        return (500f + getStats().getSpellPower() * (getStats().getSacredShieldICDReduction() > 0 ? 0.85f : 0.75f)) * (1f + getTalents().getDivineGuardian() * .1f);
    }

    public float totalAborb() {
        return uptime / icd() * procAbsorb();
    }

    public float castAborb() {
        return duration / icd() * procAbsorb();
    }

    public float hpm() {
        return procAbsorb() * duration / icd() / cost();
    }

    public float hps() {
        return totalAborb() / time();
    }

    @Override
    public String toString() {
        return "Proc Absorb: " + NetFormat.grouped(procAbsorb())
                + "\nCast Absorb: " + NetFormat.grouped(castAborb())
                + "\nAverage Cost: " + NetFormat.grouped(cost())
                + "\nHPS: " + NetFormat.grouped(hps())
                + "\nHPM: " + NetFormat.grouped(hpm(), 2) + "\n";
    }
}

class BeaconOfLight extends Spell {
    public BeaconOfLight(Rotation rotation) {
        super(rotation);
        duration = 60f + (getTalents().isGlyphOfBeaconOfLight() ? 30f : 0f);
        uptime = getRotation().fightLength * getRotation().getCalcOpts().getBoLUp();
        baseCost = 1537f;
    }

    public float healingDone(float procableHealing) {
        return procableHealing * getRotation().getCalcOpts().getBoLUp();
    }
}

class JudgementsOfThePure extends Spell {
    public JudgementsOfThePure(Rotation rotation, boolean maintainDebuff) {
        super(rotation);
        duration = maintainDebuff ? (20f + getStats().getBonusJudgementDuration()) : 60f;
        uptime = getRotation().getCalcOpts().getJotP() ? getRotation().fightLength : 0f;
        baseCost = 219f;
    }

    @Override
    public float castTime() {
        return 1.5f;
    }
}

class DivineIllumination extends Spell {
    public HolyLight hl_DI;

    public DivineIllumination(Rotation rotation) {
        super(rotation);
        duration = 180f;
        uptime = getRotation().fightLength;
        baseCost = 0f;
        hl_DI = new HolyLight(rotation);
        hl_DI.setDivineIllumination(true);
    }

    public float healed() {
        return hl_DI.hps() * time();
    }

    @Override
    public float time() {
        return 15f * casts() * getRotation().getCalcOpts().getActivity();
    }

    @Override
    public float usage() {
        return time() * hl_DI.mps();
    }
}

class DivineFavor extends Spell {
    public HolyLight hl_DF;

    public DivineFavor(Rotation rotation) {
        super(rotation);
        duration = 120f;
        uptime = getRotation().fightLength;
        baseCost = 130f;
        hl_DF = new HolyLight(rotation);
        hl_DF.setExtraCritChance(1f);
    }

    public float healed() {
        return hl_DF.averageHealed() * casts();
    }

    @Override
    public float time() {
        return hl_DF.castTime() * casts();
    }

    @Override
    public float usage() {
        return casts() * (cost() + hl_DF.averageCost());
    }
}
