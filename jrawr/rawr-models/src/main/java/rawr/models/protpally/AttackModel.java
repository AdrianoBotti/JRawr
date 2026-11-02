// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.ProtPaladin/AttackModel.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.protpally;

import rawr.base.character.Character;
import rawr.base.stats.Stats;

public class AttackModel {
    private Character character;
    private CalculationOptionsProtPaladin calcOpts;
    private Stats stats;
    private DefendTable defendTable;
    private ParryModel parryModel;

    public AbilityModelList abilities = new AbilityModelList();

    private AttackModelMode attackModelMode;
    public AttackModelMode getAttackModelMode() { return attackModelMode; }
    public void setAttackModelMode(AttackModelMode value) {
        attackModelMode = value;
        calculate();
    }

    public String name;
    public String description;
    public float threatPerSecond;
    public float damagePerSecond;
    public float attackerHitsPerSecond;

    private void calculate() {
        float modelLength = 0.0f;
        float modelThreat = 0.0f;
        float modelDamage = 0.0f;
        float modelCrits = 0.0f;

        switch (attackModelMode) {
            case BasicSoV: {
                // Basic Rotation (Assumes Judgement of Vengeance)
                name = "Basic + Seal of Vengeance";
                description = "9-6-9-6 Rotation";
                modelLength = 18.0f;
                modelThreat =
                        abilities.get(Ability.ShieldOfRighteousness).getThreat() * 3 +
                        abilities.get(Ability.HammerOfTheRighteous).getThreat() * 3 +
                        abilities.get(Ability.JudgementOfVengeance).getThreat() * 2 +
                        abilities.get(Ability.Consecration).getThreat() * 2;
                modelDamage =
                        abilities.get(Ability.ShieldOfRighteousness).getDamage() * 3 +
                        abilities.get(Ability.HammerOfTheRighteous).getDamage() * 3 +
                        abilities.get(Ability.JudgementOfVengeance).getDamage() * 2 +
                        abilities.get(Ability.Consecration).getDamage() * 2;
                modelCrits =
                        abilities.get(Ability.ShieldOfRighteousness).getCritPercentage() * 3 +
                        abilities.get(Ability.HammerOfTheRighteous).getCritPercentage() * 3 +
                        abilities.get(Ability.JudgementOfVengeance).getCritPercentage() * 2 +
                        abilities.get(Ability.Consecration).getCritPercentage() * 2;
                break;
            }
            case BasicSoR: {
                // Basic Rotation (Assumes Judgement of Righteousness)
                name = "Basic + Seal of Righteousness";
                description = "9-6-9-6 Rotation";
                modelLength = 18.0f;
                modelThreat =
                        abilities.get(Ability.ShieldOfRighteousness).getThreat() * 3 +
                        abilities.get(Ability.HammerOfTheRighteous).getThreat() * 3 +
                        abilities.get(Ability.JudgementOfRighteousness).getThreat() * 2 +
                        abilities.get(Ability.Consecration).getThreat() * 2;
                modelDamage =
                        abilities.get(Ability.ShieldOfRighteousness).getDamage() * 3 +
                        abilities.get(Ability.HammerOfTheRighteous).getDamage() * 3 +
                        abilities.get(Ability.JudgementOfRighteousness).getDamage() * 2 +
                        abilities.get(Ability.Consecration).getDamage() * 2;
                modelCrits =
                        abilities.get(Ability.ShieldOfRighteousness).getCritPercentage() * 3 +
                        abilities.get(Ability.HammerOfTheRighteous).getCritPercentage() * 3 +
                        abilities.get(Ability.JudgementOfRighteousness).getCritPercentage() * 2 +
                        abilities.get(Ability.Consecration).getCritPercentage() * 2;
                break;
            }
        }

        // White Damage
        float reckoningUptime = 1f - (float) Math.pow((1f - 0.02f * character.getPaladinTalents().getReckoning() * defendTable.anyHit()), (Math.min(8f, 4f * parryModel.weaponSpeed) / parryModel.bossAttackSpeed));
        float weaponHits = modelLength / parryModel.weaponSpeed / (1 - reckoningUptime); //Lookup.WeaponSpeed(Character, Stats);
        modelThreat += abilities.get(Ability.None).getThreat() * weaponHits;
        modelDamage += abilities.get(Ability.None).getDamage() * weaponHits;
        modelCrits += abilities.get(Ability.None).getCritPercentage() * weaponHits;

        // Seals
        weaponHits += modelLength / 6.0f; // Add Seal Damage from Hammer of the Righteous
        switch (calcOpts.getSealChoice()) {
            // Seal of Righteousness
            case "Seal of Righteousness":
                modelThreat += abilities.get(Ability.SealOfRighteousness).getThreat() * weaponHits;
                modelDamage += abilities.get(Ability.SealOfRighteousness).getDamage() * weaponHits;
                modelCrits += abilities.get(Ability.SealOfRighteousness).getCritPercentage() * weaponHits;
                break;
            //Seal of Vengeance Mode
            case "Seal of Vengeance":
                modelThreat += abilities.get(Ability.SealOfVengeance).getThreat() * weaponHits;
                modelDamage += abilities.get(Ability.SealOfVengeance).getDamage() * weaponHits;
                modelCrits += abilities.get(Ability.SealOfVengeance).getCritPercentage() * weaponHits;

                //Holy Vengeance (Seal of Vengeance DOT)
                modelThreat += abilities.get(Ability.HolyVengeance).getThreat();
                modelDamage += abilities.get(Ability.HolyVengeance).getDamage();
                break;
            default:
                break;
        }

        if (character.getPaladinTalents().getHolyShield() != 0) {
            // Holy Shield
            // TODO: Model Holy Shield Charges
            float attackerBlocks = defendTable.getBlock() * (modelLength / parryModel.bossAttackSpeed); //Options.BossAttackSpeed;
            modelThreat += abilities.get(Ability.HolyShield).getThreat() * attackerBlocks;
            modelDamage += abilities.get(Ability.HolyShield).getDamage() * attackerBlocks;
            modelCrits += abilities.get(Ability.HolyShield).getCritPercentage() * attackerBlocks;
        }

        float attackerHits = defendTable.anyHit() * (modelLength / parryModel.bossAttackSpeed); //Options.BossAttackSpeed;

        threatPerSecond = modelThreat / modelLength;
        damagePerSecond = modelDamage / modelLength;
        attackerHitsPerSecond = attackerHits / modelLength;
    }

    public AttackModel(Character character, Stats stats, AttackModelMode attackModelMode, CalculationOptionsProtPaladin calcOpts) {
        this.character = character;
        this.calcOpts = calcOpts;
        this.stats = stats;
        defendTable = new DefendTable(character, stats, calcOpts, true);
        parryModel = new ParryModel(character, stats, calcOpts);
        this.attackModelMode = attackModelMode;

        abilities.add(Ability.None, character, stats, calcOpts);
        abilities.add(Ability.ShieldOfRighteousness, character, stats, calcOpts);
        abilities.add(Ability.HammerOfTheRighteous, character, stats, calcOpts);
        abilities.add(Ability.SealOfVengeance, character, stats, calcOpts);
        abilities.add(Ability.HolyVengeance, character, stats, calcOpts);
        abilities.add(Ability.JudgementOfVengeance, character, stats, calcOpts);
        abilities.add(Ability.SealOfRighteousness, character, stats, calcOpts);
        abilities.add(Ability.JudgementOfRighteousness, character, stats, calcOpts);
        abilities.add(Ability.Exorcism, character, stats, calcOpts);
        abilities.add(Ability.HammerOfWrath, character, stats, calcOpts);
        abilities.add(Ability.AvengersShield, character, stats, calcOpts);
        abilities.add(Ability.HolyShield, character, stats, calcOpts);
        abilities.add(Ability.RetributionAura, character, stats, calcOpts);
        abilities.add(Ability.HolyWrath, character, stats, calcOpts);
        abilities.add(Ability.Consecration, character, stats, calcOpts);

        calculate();
    }
}
