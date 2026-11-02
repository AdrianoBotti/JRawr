// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/HunterEnums.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.hunter;

public final class HunterEnums {
    private HunterEnums() {
    }

    public enum Aspect {
        Beast,
        Hawk,
        Viper,
        Monkey,
        Dragonhawk,
        None
    }

    public enum ShotRotation {
        AutoShotOnly,
        OneToOne,
        ThreeToTwo
    }

    public enum Shots {
        None,
        ExplosiveShot,
        ChimearaShot_Serpent,
        SerpentSting,
        SteadyShot,
        AimedShot,
        MultiShot,
        ArcaneShot,
        BlackArrow,
        KillShot,
        SilencingShot,
        ChimearaShot,
        ScorpidSting,
        ViperSting,
        RapidFire,
        ImmolationTrap,
        ExplosiveTrap,
        FreezingTrap,
        FrostTrap,
        Volley,
        Readiness,
        BestialWrath
    }

    public enum PetFamily {
        Bat(24),
        BirdOfPrey(26),
        Chimaera(38),
        Dragonhawk(30),
        NetherRay(34),
        Ravager(31),
        Serpent(35),
        Silithid(41),
        Spider(3),
        SporeBat(33),
        WindSerpent(27),

        Bear(4),
        Boar(5),
        Crab(8),
        Crocolisk(6),
        Gorilla(9),
        Rhino(43),
        Scorpid(20),
        Turtle(21),
        WarpStalker(32),
        Worm(42),

        CarrionBird(7),
        Cat(2),
        CoreHound(45),
        Devilsaur(39),
        Hyena(25),
        Moth(37),
        Raptor(11),
        SpiritBeast(46),
        Tallstrider(12),
        Wasp(44),
        Wolf(1),

        None(0);

        public final int id;

        PetFamily(int id) {
            this.id = id;
        }
    }

    public enum PetFamilyTree {
        Cunning,
        Ferocity,
        Tenacity,
        None
    }

    public enum Faction {
        None,
        Aldor,
        Scryer
    }

    public enum PetAttacks {
        AcidSpit,
        BadAttitude,
        Bite,
        Bullheaded,
        CallOfTheWild,
        Charge,
        Claw,
        Cower,
        Dash,
        Dive,
        DemoralizingScreech,
        DustCloud,
        FireBreath,
        FroststormBreath,
        FuriousHowl,
        Gore,
        Growl,
        LastStand,
        LavaBreath,
        LickYourWounds,
        LightningBreath,
        MonstrousBite,
        NetherShock,
        None,
        Pin,
        PoisonSpit,
        Prowl,
        Pummel,
        Rabid,
        Rake,
        Ravage,
        RoarOfRecovery,
        RoarOfSacrifice,
        SavageRend,
        ScorpidPoison,
        SerenityDust,
        ShellShield,
        Smack,
        Snatch,
        SonicBlast,
        SpiritStrike,
        SporeCloud,
        Stampede,
        Sting,
        Swipe,
        Swoop,
        Taunt,
        TendonRip,
        Thunderstomp,
        VenomWebSpray,
        Warp,
        Web,
        WolverineBite
    }

    public enum PetSkillType {
        FocusDump,
        NonDamaging,
        SpecialMelee,
        SpecialSpell,
        SpecialUnique,
        Unimplemented
    }

    public enum AspectUsage {
        None,
        ViperToOOM,
        ViperRegen
    }

    public enum PetHappiness {
        Happy,
        Content,
        Unhappy
    }
}
