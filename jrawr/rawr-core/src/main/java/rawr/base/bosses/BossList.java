// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Bosses/BossList.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.base.bosses;

import rawr.base.character.TargetGroup;
import rawr.base.items.ItemDamageType;

import rawr.base.bosses.AlgalontheObserver;
import rawr.base.bosses.AnubRekhan;
import rawr.base.bosses.Anubarak;
import rawr.base.bosses.ArchavonTheStoneWatcher;
import rawr.base.bosses.AssemblyofIron;
import rawr.base.bosses.Auriaya;
import rawr.base.bosses.Baltharus;
import rawr.base.bosses.BloodPrinceCouncil;
import rawr.base.bosses.BloodQueenLanathel;
import rawr.base.bosses.DeathbringerSaurfang;
import rawr.base.bosses.EmalonTheStormWatcher;
import rawr.base.bosses.FactionChampions;
import rawr.base.bosses.Festergut;
import rawr.base.bosses.FourHorsemen;
import rawr.base.bosses.Freya;
import rawr.base.bosses.GeneralVezax;
import rawr.base.bosses.GeneralZarithrian;
import rawr.base.bosses.Gluth;
import rawr.base.bosses.GothiktheHarvester;
import rawr.base.bosses.GrandWidowFaerlina;
import rawr.base.bosses.Grobbulus;
import rawr.base.bosses.Halion;
import rawr.base.bosses.HeigantheUnclean;
import rawr.base.bosses.Hodir;
import rawr.base.bosses.IgnistheFurnaceMaster;
import rawr.base.bosses.InstructorRazuvious;
import rawr.base.bosses.KelThuzad;
import rawr.base.bosses.Kologarn;
import rawr.base.bosses.KoralonTheFlameWatcher;
import rawr.base.bosses.LadyDeathwhisper;
import rawr.base.bosses.Loatheb;
import rawr.base.bosses.LordJaraxxus;
import rawr.base.bosses.LordMarrowgar;
import rawr.base.bosses.Maexxna;
import rawr.base.bosses.Malygos;
import rawr.base.bosses.Mimiron;
import rawr.base.bosses.NorthrendBeasts;
import rawr.base.bosses.NoththePlaguebringer;
import rawr.base.bosses.Patchwerk;
import rawr.base.bosses.ProfessorPutricide;
import rawr.base.bosses.Razorscale;
import rawr.base.bosses.Rotface;
import rawr.base.bosses.Sapphiron;
import rawr.base.bosses.Sartharion;
import rawr.base.bosses.SavianaRagefire;
import rawr.base.bosses.Shadron;
import rawr.base.bosses.Sindragosa;
import rawr.base.bosses.Tenebron;
import rawr.base.bosses.Thaddius;
import rawr.base.bosses.TheLichKing;
import rawr.base.bosses.Thorim;
import rawr.base.bosses.ToravonTheIceWatcher;
import rawr.base.bosses.TwinValkyr;
import rawr.base.bosses.ValathriaDreamwalker;
import rawr.base.bosses.Vesperon;
import rawr.base.bosses.XT002Deconstructor;
import rawr.base.bosses.YoggSaron;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * The encounter database: every boss known to the engine, collected and filtered for the UI.
 *
 * <p>The constructor instantiates each boss class from {@link rawr.base.bosses} once and flattens
 * the chosen variants into {@link #list}. Earlier tiers contribute two entries per boss (10- and
 * 25-player normal); later tiers contribute four (10/25 normal and heroic). The list is the
 * lookup table behind the boss picker: {@link #genCalledList(FilterType, String)} narrows
 * {@link #calledList} by content tier, instance, version or name, and
 * {@link #getBossFromName(String)} / {@link #getBossFromBetterName(String)} resolve a saved
 * name back to a canonical definition (the "better" form is the display string
 * {@code content : instance (version) name}).</p>
 *
 * <p>{@link #theEzModeBoss}, {@link #theAvgBoss} and {@link #theHardestBoss} are synthetic
 * encounters aggregated from the current selection by {@code genTheEZModeBoss},
 * {@code genTheAvgBoss} and {@code genTheHardestBoss}: the easiest takes the smallest
 * health/armor/level and the longest timers, the average takes arithmetic means, the hardest the
 * opposites. Users pick them to optimize against "typical" or "worst case" content. Their
 * {@code _Called} counterparts track the filtered list.</p>
 *
 * <p>Bosses are identified across save files by name (see {@link BossOptions#getBossName()}), so
 * names must stay stable even where the original contains misspellings.</p>
 */
public class BossList {
    public static final int NormCharLevel = 80;

    public BossHandler[] list;
    public BossHandler[] calledList;
    private ItemDamageType[] dts = null;

    private ItemDamageType[] getDamageTypes() {
        if (dts == null) {
            dts = new ItemDamageType[] {
                ItemDamageType.Physical,
                ItemDamageType.Frost,
                ItemDamageType.Fire,
                ItemDamageType.Nature,
                ItemDamageType.Arcane,
                ItemDamageType.Shadow,
                ItemDamageType.Holy,
            };
        }
        return dts;
    }

    public BossHandler theEzModeBoss;
    public BossHandler theAvgBoss;
    public BossHandler theHardestBoss;
    public BossHandler theEzModeBoss_Called;
    public BossHandler theAvgBoss_Called;
    public BossHandler theHardestBoss_Called;

    public enum FilterType {
        Content(0), Instance(1), Version(2), Name(3);

        private final int value;

        FilterType(int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public BossList() {
        AnubRekhan anubrekhan = new AnubRekhan();
        GrandWidowFaerlina grandwidow = new GrandWidowFaerlina();
        Maexxna maexxna = new Maexxna();
        NoththePlaguebringer noth = new NoththePlaguebringer();
        HeigantheUnclean heigantheUnclean = new HeigantheUnclean();
        Loatheb loatheb = new Loatheb();
        InstructorRazuvious instructorRazuvious = new InstructorRazuvious();
        GothiktheHarvester gothiktheHarvester = new GothiktheHarvester();
        FourHorsemen fourHorsemen = new FourHorsemen();
        Patchwerk patchwerk = new Patchwerk();
        Grobbulus grobbulus = new Grobbulus();
        Gluth gluth = new Gluth();
        Thaddius thaddius = new Thaddius();
        Sapphiron sapphiron = new Sapphiron();
        KelThuzad kelThuzad = new KelThuzad();
        Shadron shadron = new Shadron();
        Tenebron tenebron = new Tenebron();
        Vesperon vesperon = new Vesperon();
        Sartharion sartharion = new Sartharion();
        ArchavonTheStoneWatcher archavon = new ArchavonTheStoneWatcher();
        Malygos malygos = new Malygos();
        IgnistheFurnaceMaster ignistheFurnaceMaster = new IgnistheFurnaceMaster();
        Razorscale razorscale = new Razorscale();
        XT002Deconstructor xt002Deconstructor = new XT002Deconstructor();
        AssemblyofIron assemblyofIron = new AssemblyofIron();
        Kologarn kologarn = new Kologarn();
        Auriaya auriaya = new Auriaya();
        Mimiron mimiron = new Mimiron();
        Freya freya = new Freya();
        Thorim thorim = new Thorim();
        Hodir hodir = new Hodir();
        GeneralVezax generalVezax = new GeneralVezax();
        YoggSaron yoggSaron = new YoggSaron();
        AlgalontheObserver algalontheObserver = new AlgalontheObserver();
        EmalonTheStormWatcher emalonTheStormWatcher = new EmalonTheStormWatcher();
        KoralonTheFlameWatcher koralonTheFlameWatcher = new KoralonTheFlameWatcher();
        NorthrendBeasts northrendBeasts = new NorthrendBeasts();
        LordJaraxxus lordJaraxxus = new LordJaraxxus();
        FactionChampions factionChampions = new FactionChampions();
        TwinValkyr twinValkyr = new TwinValkyr();
        Anubarak anubarak = new Anubarak();
        ToravonTheIceWatcher toravonTheIceWatcher = new ToravonTheIceWatcher();
        LordMarrowgar lordMarrowgar = new LordMarrowgar();
        LadyDeathwhisper ladyDeathwhisper = new LadyDeathwhisper();
        DeathbringerSaurfang deathbringerSaurfang = new DeathbringerSaurfang();
        Festergut festergut = new Festergut();
        Rotface rotface = new Rotface();
        ProfessorPutricide professorPutricide = new ProfessorPutricide();
        BloodPrinceCouncil bloodPrinceCouncil = new BloodPrinceCouncil();
        BloodQueenLanathel bloodQueenLanathel = new BloodQueenLanathel();
        ValathriaDreamwalker valathriaDreamwalker = new ValathriaDreamwalker();
        Sindragosa sindragosa = new Sindragosa();
        TheLichKing theLichKing = new TheLichKing();
        Baltharus baltharus = new Baltharus();
        SavianaRagefire savianaRagefire = new SavianaRagefire();
        GeneralZarithrian generalZarithrian = new GeneralZarithrian();
        Halion halion = new Halion();
        list = new BossHandler[] {
            anubrekhan.get(0), anubrekhan.get(1),
            grandwidow.get(0), grandwidow.get(1),
            maexxna.get(0), maexxna.get(1),
            noth.get(0), noth.get(1),
            heigantheUnclean.get(0), heigantheUnclean.get(1),
            loatheb.get(0), loatheb.get(1),
            instructorRazuvious.get(0), instructorRazuvious.get(1),
            gothiktheHarvester.get(0), gothiktheHarvester.get(1),
            fourHorsemen.get(0), fourHorsemen.get(1),
            patchwerk.get(0), patchwerk.get(1),
            grobbulus.get(0), grobbulus.get(1),
            gluth.get(0), gluth.get(1),
            thaddius.get(0), thaddius.get(1),
            sapphiron.get(0), sapphiron.get(1),
            kelThuzad.get(0), kelThuzad.get(1),
            shadron.get(0), shadron.get(1),
            tenebron.get(0), tenebron.get(1),
            vesperon.get(0), vesperon.get(1),
            sartharion.get(0), sartharion.get(1),
            archavon.get(0), archavon.get(1),
            malygos.get(0), malygos.get(1),
            ignistheFurnaceMaster.get(0), ignistheFurnaceMaster.get(1),
            razorscale.get(0), razorscale.get(1),
            xt002Deconstructor.get(0), xt002Deconstructor.get(1),
            assemblyofIron.get(0), assemblyofIron.get(1),
            kologarn.get(0), kologarn.get(1),
            auriaya.get(0), auriaya.get(1),
            mimiron.get(0), mimiron.get(1),
            freya.get(0), freya.get(1),
            thorim.get(0), thorim.get(1),
            hodir.get(0), hodir.get(1),
            generalVezax.get(0), generalVezax.get(1),
            yoggSaron.get(0), yoggSaron.get(1),
            algalontheObserver.get(0), algalontheObserver.get(1),
            emalonTheStormWatcher.get(0), emalonTheStormWatcher.get(1),
            koralonTheFlameWatcher.get(0), koralonTheFlameWatcher.get(1),
            northrendBeasts.get(0), northrendBeasts.get(1), northrendBeasts.get(2), northrendBeasts.get(3),
            lordJaraxxus.get(0), lordJaraxxus.get(1), lordJaraxxus.get(2), lordJaraxxus.get(3),
            factionChampions.get(0), factionChampions.get(1), factionChampions.get(2), factionChampions.get(3),
            twinValkyr.get(0), twinValkyr.get(1), twinValkyr.get(2), twinValkyr.get(3),
            anubarak.get(0), anubarak.get(1), anubarak.get(2), anubarak.get(3),
            toravonTheIceWatcher.get(0), toravonTheIceWatcher.get(1),
            lordMarrowgar.get(0), lordMarrowgar.get(1), lordMarrowgar.get(2), lordMarrowgar.get(3),
            ladyDeathwhisper.get(0), ladyDeathwhisper.get(1), ladyDeathwhisper.get(2), ladyDeathwhisper.get(3),
            deathbringerSaurfang.get(0), deathbringerSaurfang.get(1), deathbringerSaurfang.get(2), deathbringerSaurfang.get(3),
            festergut.get(0), festergut.get(1), festergut.get(2), festergut.get(3),
            rotface.get(0), rotface.get(1), rotface.get(2), rotface.get(3),
            professorPutricide.get(0), professorPutricide.get(1), professorPutricide.get(2), professorPutricide.get(3),
            bloodPrinceCouncil.get(0), bloodPrinceCouncil.get(1), bloodPrinceCouncil.get(2), bloodPrinceCouncil.get(3),
            bloodQueenLanathel.get(0), bloodQueenLanathel.get(1), bloodQueenLanathel.get(2), bloodQueenLanathel.get(3),
            valathriaDreamwalker.get(0), valathriaDreamwalker.get(1), valathriaDreamwalker.get(2), valathriaDreamwalker.get(3),
            sindragosa.get(0), sindragosa.get(1), sindragosa.get(2), sindragosa.get(3),
            theLichKing.get(0), theLichKing.get(1), theLichKing.get(2), theLichKing.get(3),
            baltharus.get(0), baltharus.get(1), baltharus.get(2), baltharus.get(3),
            savianaRagefire.get(0), savianaRagefire.get(1), savianaRagefire.get(2), savianaRagefire.get(3),
            generalZarithrian.get(0), generalZarithrian.get(1), generalZarithrian.get(2), generalZarithrian.get(3),
            halion.get(0), halion.get(1), halion.get(2), halion.get(3),
        };
        theEzModeBoss = genTheEZModeBoss(list);
        theAvgBoss = genTheAvgBoss(list);
        theHardestBoss = genTheHardestBoss(list);
        calledList = genCalledList(FilterType.Content, "All");
    }

    /**
     * Rebuilds {@link #calledList} from {@link #list} keeping only bosses matching the filter, and
     * recomputes the three synthetic aggregate bosses for that subset. The literal filter
     * {@code "All"} restores the full list; otherwise the filter is compared ignoring case against
     * the field selected by {@link FilterType}. The special aggregate bosses are always kept.
     */
    public BossHandler[] genCalledList(FilterType ftype, String filter) {
        if (filter.equalsIgnoreCase("All")) {
            theEzModeBoss_Called = theEzModeBoss;
            theAvgBoss_Called = theAvgBoss;
            theHardestBoss_Called = theHardestBoss;
            calledList = list;
            return calledList;
        }
        List<BossHandler> retList = new ArrayList<BossHandler>();
        for (BossHandler boss : list) {
            switch (ftype) {
                case Content:  { if (boss.getContentString().equalsIgnoreCase(filter)) { retList.add(boss); } break; }
                case Instance: { if (boss.getInstance().equalsIgnoreCase(filter)) { retList.add(boss); } break; }
                case Version:  { if (boss.getVersionString().equalsIgnoreCase(filter)) { retList.add(boss); } break; }
                case Name:     { if (boss.getName().equalsIgnoreCase(filter)) { retList.add(boss); } break; }
                default: { break; }
            }
        }
        BossHandler[] retList2 = retList.toArray(new BossHandler[0]);
        theEzModeBoss_Called = genTheEZModeBoss(retList2);
        theAvgBoss_Called = genTheAvgBoss(retList2);
        theHardestBoss_Called = genTheHardestBoss(retList2);
        calledList = retList2;
        return calledList;
    }

    public List<String> getBossNames() {
        List<String> names = new ArrayList<String>();
        names.add(theEzModeBoss.getName());
        names.add(theAvgBoss.getName());
        names.add(theHardestBoss.getName());
        for (BossHandler boss : calledList) {
            names.add(boss.getName());
        }
        return names;
    }

    public String[] getBossNamesAsArray() {
        return getBossNames().toArray(new String[0]);
    }

    public List<String> getBetterBossNames() {
        List<String> names = new ArrayList<String>();
        names.add(theEzModeBoss.getName());
        names.add(theAvgBoss.getName());
        names.add(theHardestBoss.getName());
        for (BossHandler boss : calledList) {
            String name = boss.getContentString() + " : " + boss.getInstance() + " (" + boss.getVersionString() + ") " + boss.getName();
            names.add(name);
        }
        return names;
    }

    public String[] getBetterBossNamesAsArray() {
        return getBetterBossNames().toArray(new String[0]);
    }

    public BossHandler getBossFromName(String name) {
        BossHandler retBoss = new BossHandler();
        if (Objects.equals(theEzModeBoss_Called.getName(), name)) {
            retBoss = theEzModeBoss_Called;
        } else if (Objects.equals(theAvgBoss_Called.getName(), name)) {
            retBoss = theAvgBoss_Called;
        } else if (Objects.equals(theHardestBoss_Called.getName(), name)) {
            retBoss = theHardestBoss_Called;
        } else {
            for (BossHandler boss : calledList) {
                if (Objects.equals(boss.getName(), name)) {
                    retBoss = boss;
                    break;
                }
            }
        }
        return retBoss;
    }

    public BossHandler getBossFromBetterName(String name) {
        BossHandler retBoss = new BossHandler();
        if (Objects.equals(theEzModeBoss_Called.getName(), name)) {
            retBoss = theEzModeBoss_Called;
        } else if (Objects.equals(theAvgBoss_Called.getName(), name)) {
            retBoss = theAvgBoss_Called;
        } else if (Objects.equals(theHardestBoss_Called.getName(), name)) {
            retBoss = theHardestBoss_Called;
        } else {
            for (BossHandler boss : calledList) {
                String checkName = boss.getContentString() + " : " + boss.getInstance() + " (" + boss.getVersionString() + ") " + boss.getName();
                if (Objects.equals(checkName, name)) {
                    retBoss = boss;
                    break;
                }
            }
        }
        return retBoss;
    }

    public List<String> getFilterList(FilterType ftype) {
        List<String> names = new ArrayList<String>();
        switch (ftype) {
            case Content: {
                if (!names.contains(theEzModeBoss_Called.getContentString())) { names.add(theEzModeBoss_Called.getContentString()); }
                if (!names.contains(theAvgBoss_Called.getContentString())) { names.add(theAvgBoss_Called.getContentString()); }
                if (!names.contains(theHardestBoss_Called.getContentString())) { names.add(theHardestBoss_Called.getContentString()); }
                for (BossHandler boss : calledList) { if (!names.contains(boss.getContentString())) { names.add(boss.getContentString()); } }
                break;
            }
            case Instance: {
                if (!names.contains(theEzModeBoss_Called.getInstance())) { names.add(theEzModeBoss_Called.getInstance()); }
                if (!names.contains(theAvgBoss_Called.getInstance())) { names.add(theAvgBoss_Called.getInstance()); }
                if (!names.contains(theHardestBoss_Called.getInstance())) { names.add(theHardestBoss_Called.getInstance()); }
                for (BossHandler boss : calledList) { if (!names.contains(boss.getInstance())) { names.add(boss.getInstance()); } }
                break;
            }
            case Name: {
                if (!names.contains(theEzModeBoss_Called.getName())) { names.add(theEzModeBoss_Called.getName()); }
                if (!names.contains(theAvgBoss_Called.getName())) { names.add(theAvgBoss_Called.getName()); }
                if (!names.contains(theHardestBoss_Called.getName())) { names.add(theHardestBoss_Called.getName()); }
                for (BossHandler boss : calledList) { if (!names.contains(boss.getName())) { names.add(boss.getName()); } }
                break;
            }
            case Version: {
                if (!names.contains(theEzModeBoss_Called.getVersionString())) { names.add(theEzModeBoss_Called.getVersionString()); }
                if (!names.contains(theAvgBoss_Called.getVersionString())) { names.add(theAvgBoss_Called.getVersionString()); }
                if (!names.contains(theHardestBoss_Called.getVersionString())) { names.add(theHardestBoss_Called.getVersionString()); }
                for (BossHandler boss : calledList) { if (!names.contains(boss.getVersionString())) { names.add(boss.getVersionString()); } }
                break;
            }
            default: { break; }
        }
        return names;
    }

    public String[] getFilterListAsArray(FilterType ftype) {
        return getFilterList(ftype).toArray(new String[0]);
    }

    public String genInfoString(String bossName, boolean useBettername) {
        BossHandler boss = useBettername ? getBossFromBetterName(bossName) : getBossFromName(bossName);
        String retVal = boss.genInfoString();
        return retVal;
    }

    private boolean useGoodBoyAvg = true;











    private BossHandler genTheEZModeBoss(BossHandler[] passedList) {
        useGoodBoyAvg = true;
        BossHandler retboss = new BossHandler();
        if (passedList.length < 1) {
            return retboss;
        }
        float value = 0f;
        retboss.setName("The Easiest Boss");
        value = BossHandler.TierLevels.T10_9.value();
        for (BossHandler boss : passedList) { value = Math.min(value, boss.getContent().value()); }
        retboss.setContent(BossHandler.TierLevels.of((int) Math.floor(value)));
        value = passedList[0].getLevel();
        for (BossHandler boss : passedList) { value = Math.min(value, boss.getLevel()); }
        retboss.setLevel((int) value);
        value = passedList[0].getArmor();
        for (BossHandler boss : passedList) { value = Math.min(value, boss.getArmor()); }
        retboss.setArmor((int) value);
        value = passedList[0].getHealth();
        for (BossHandler boss : passedList) { value = Math.min(value, boss.getHealth()); }
        retboss.setHealth((int) value);
        value = 1f;
        for (BossHandler boss : passedList) { value = Math.max(value, boss.getBerserkTimer()); }
        retboss.setBerserkTimer((int) Math.ceil(value));
        value = 1f;
        for (BossHandler boss : passedList) { value = Math.max(value, boss.getSpeedKillTimer()); }
        retboss.setSpeedKillTimer((int) Math.ceil(value));
        value = 0f;
        for (BossHandler boss : passedList) { value = Math.max(value, (float) boss.getInBackPercMelee()); }
        retboss.setInBackPercMelee(value);
        value = 0f;
        for (BossHandler boss : passedList) { value = Math.max(value, (float) boss.getInBackPercRanged()); }
        retboss.setInBackPercRanged(value);
        boolean is25 = false;
        for (BossHandler boss : passedList) {
            if (boss.getVersion() == BossHandler.Versions.V_25N || boss.getVersion() == BossHandler.Versions.V_25H) {
                is25 = true;
                break;
            }
        }
        retboss.setMaxPlayers(is25 ? 25 : 10);
        value = 0f;
        for (BossHandler boss : passedList) { value = Math.min(value, boss.getMinTanks()); }
        retboss.setMinTanks((int) Math.ceil(value));
        value = 0f;
        for (BossHandler boss : passedList) { value = Math.min(value, boss.getMinHealers()); }
        retboss.setMinHealers((int) Math.ceil(value));
        {   // Multi-targs
            float f = -1, d = -1, n = 1;
            value = 0;
            for (BossHandler boss : passedList) {
                if (boss.getMultiTargsFreq() <= 0) { value = 0f; break; } else { value = Math.max(value, boss.getMultiTargsFreq()); }
            }
            f = (value >= retboss.getBerserkTimer() || value <= 0 ? 0000 : value);
            value = 5000;
            for (BossHandler boss : passedList) { value = Math.min(value, boss.getMultiTargsDur()); }
            d = (value >= retboss.getBerserkTimer() || value <= 0 ? 5000 : value);
            value = 0;
            for (BossHandler boss : passedList) { value = Math.min(value, (float) boss.getMultiTargsNum()); }
            n = value;
            if (!(f <= 0 || d <= 0 || n <= 0)) {
                TargetGroup tg = new TargetGroup();
                tg.frequency = f;
                tg.duration = d;
                tg.chance = 1f;
                tg.numTargs = n;
                tg.nearBoss = true;
                retboss.targets.add(tg);
            }
        }
        {   // Attacks
            {
                List<Attack> attacks = new ArrayList<Attack>();
                for (BossHandler boss : passedList) {
                    attacks.addAll(boss.getFilteredAttackList(AttackTypes.AT_MELEE));
                }
                float perhit = 500000, numtrg = retboss.getMaxPlayers(), atkspd = 0f;
                for (Attack a : attacks) {
                    if (!"Invalid".equals(a.name)) {
                        perhit = Math.min(perhit, a.getDamagePerHit());
                        numtrg = Math.min(numtrg, a.maxNumTargets);
                        atkspd = Math.max(atkspd, a.attackSpeed);
                    }
                }
                Attack atk = new Attack();
                atk.name = "Avg Melee";
                atk.attackType = AttackTypes.AT_MELEE;
                atk.damageType = ItemDamageType.Physical;
                atk.setDamagePerHit(perhit);
                atk.maxNumTargets = numtrg;
                atk.attackSpeed = atkspd;
                atk.isTheDefaultMelee = true;
                retboss.getAttacks().add(atk);
            }
            {
                List<Attack> attacks = new ArrayList<Attack>();
                for (BossHandler boss : passedList) {
                    attacks.addAll(boss.getFilteredAttackList(AttackTypes.AT_RANGED));
                }
                float perhit = 500000, numtrg = retboss.getMaxPlayers(), atkspd = 0f;
                for (Attack a : attacks) {
                    if (!"Invalid".equals(a.name)) {
                        perhit = Math.min(perhit, a.getDamagePerHit());
                        numtrg = Math.min(numtrg, a.maxNumTargets);
                        atkspd = Math.max(atkspd, a.attackSpeed);
                    }
                }
                Attack atk = new Attack();
                atk.name = "Avg Ranged";
                atk.attackType = AttackTypes.AT_RANGED;
                atk.damageType = ItemDamageType.Physical;
                atk.setDamagePerHit(perhit);
                atk.maxNumTargets = numtrg;
                atk.attackSpeed = atkspd;
                retboss.getAttacks().add(atk);
            }
            {
                List<Attack> attacks = new ArrayList<Attack>();
                for (BossHandler boss : passedList) {
                    attacks.addAll(boss.getFilteredAttackList(AttackTypes.AT_AOE));
                }
                float perhit = 500000, numtrg = retboss.getMaxPlayers(), atkspd = 0f;
                for (Attack a : attacks) {
                    if (!"Invalid".equals(a.name)) {
                        perhit = Math.min(perhit, a.getDamagePerHit());
                        numtrg = Math.min(numtrg, a.maxNumTargets);
                        atkspd = Math.max(atkspd, a.attackSpeed);
                    }
                }
                Attack atk = new Attack();
                atk.name = "Avg AoE";
                atk.attackType = AttackTypes.AT_AOE;
                atk.damageType = ItemDamageType.Arcane;
                atk.setDamagePerHit(perhit);
                atk.maxNumTargets = numtrg;
                atk.attackSpeed = atkspd;
                retboss.getAttacks().add(atk);
            }
        }
        for (ItemDamageType t : getDamageTypes()) {
            value = (float) passedList[0].getResistance(t);
            for (BossHandler boss : passedList) {
                value = (float) Math.min(value, boss.getResistance(t));
            }
            retboss.setResistance(t, value);
        }
        {   // Move
            float f = -1, d = -1;
            value = 0;
            for (BossHandler boss : passedList) {
                if (boss.getMovingTargsFreq() <= 0) { value = 0f; break; } else { value = Math.max(value, boss.getMovingTargsFreq()); }
            }
            f = (value >= retboss.getBerserkTimer() || value <= 0 ? 0000 : value);
            value = 5000;
            for (BossHandler boss : passedList) { value = Math.min(value, boss.getMovingTargsDur()); }
            d = (value >= retboss.getBerserkTimer() || value <= 0 ? 5000 : value);
            if (!(f <= 0 || d <= 0)) {
                Impedance imp = new Impedance();
                imp.frequency = f;
                imp.duration = d;
                imp.chance = 1f;
                imp.breakable = true;
                retboss.moves.add(imp);
            }
        }
        {   // Stun
            float f = -1, d = -1;
            value = 0;
            for (BossHandler boss : passedList) {
                if (boss.getStunningTargsFreq() <= 0) { value = 0f; break; } else { value = Math.max(value, boss.getStunningTargsFreq()); }
            }
            f = (value >= retboss.getBerserkTimer() || value <= 0 ? 0000 : value);
            value = 10 * 1000;
            for (BossHandler boss : passedList) { value = Math.min(value, boss.getStunningTargsDur()); }
            d = (value >= retboss.getBerserkTimer() || value <= 0 ? 5000 : value);
            if (!(f <= 0 || d <= 0)) {
                Impedance imp = new Impedance();
                imp.frequency = f;
                imp.duration = d;
                imp.chance = 1f;
                imp.breakable = true;
                retboss.stuns.add(imp);
            }
        }
        {   // Fear
            float f = -1, d = -1;
            value = 0;
            for (BossHandler boss : passedList) {
                if (boss.getFearingTargsFreq() <= 0) { value = 0f; break; } else { value = Math.max(value, boss.getFearingTargsFreq()); }
            }
            f = (value >= retboss.getBerserkTimer() || value <= 0 ? 0000 : value);
            value = 5000;
            for (BossHandler boss : passedList) { value = Math.min(value, boss.getFearingTargsDur()); }
            d = (value >= retboss.getBerserkTimer() || value <= 0 ? 5000 : value);
            if (!(f <= 0 || d <= 0)) {
                Impedance imp = new Impedance();
                imp.frequency = f;
                imp.duration = d;
                imp.chance = 1f;
                imp.breakable = true;
                retboss.fears.add(imp);
            }
        }
        {   // Root
            float f = -1, d = -1;
            value = 0;
            for (BossHandler boss : passedList) {
                if (boss.getRootingTargsFreq() <= 0) { value = 0f; break; } else { value = Math.max(value, boss.getRootingTargsFreq()); }
            }
            f = (value >= retboss.getBerserkTimer() || value <= 0 ? 0000 : value);
            value = 5000;
            for (BossHandler boss : passedList) { value = Math.min(value, boss.getRootingTargsDur()); }
            d = (value >= retboss.getBerserkTimer() || value <= 0 ? 5000 : value);
            if (!(f <= 0 || d <= 0)) {
                Impedance imp = new Impedance();
                imp.frequency = f;
                imp.duration = d;
                imp.chance = 1f;
                imp.breakable = true;
                retboss.roots.add(imp);
            }
        }
        {   // Disarm
            float f = -1, d = -1;
            value = 0;
            for (BossHandler boss : passedList) {
                if (boss.getDisarmingTargsFreq() <= 0) { value = 0f; break; } else { value = Math.max(value, boss.getDisarmingTargsFreq()); }
            }
            f = (value >= retboss.getBerserkTimer() || value <= 0 ? 0000 : value);
            value = 5000;
            for (BossHandler boss : passedList) { value = Math.min(value, boss.getDisarmingTargsDur()); }
            d = (value >= retboss.getBerserkTimer() || value <= 0 ? 5000 : value);
            if (!(f <= 0 || d <= 0)) {
                Impedance imp = new Impedance();
                imp.frequency = f;
                imp.duration = d;
                imp.chance = 1f;
                imp.breakable = true;
                retboss.disarms.add(imp);
            }
        }
        return retboss;
    }

    private BossHandler genTheAvgBoss(BossHandler[] passedList) {
        useGoodBoyAvg = true;
        BossHandler retboss = new BossHandler();
        if (passedList.length < 1) {
            return retboss;
        }
        float value = 0f;
        retboss.setName("The Average Boss");
        value = BossHandler.TierLevels.T7_0.value();
        for (BossHandler boss : passedList) { value += boss.getContent().value(); }
        retboss.setContent(BossHandler.TierLevels.of((int) Math.floor(value / (float) passedList.length)));
        value = 0f;
        for (BossHandler boss : passedList) { value += boss.getLevel(); }
        value /= passedList.length;
        retboss.setLevel((int) value);
        value = 0f;
        for (BossHandler boss : passedList) { value += boss.getArmor(); }
        value /= passedList.length;
        retboss.setArmor((int) value);
        value = 0f;
        for (BossHandler boss : passedList) { value += boss.getBerserkTimer(); }
        value /= passedList.length;
        retboss.setBerserkTimer((int) Math.floor(value));
        value = 0f;
        for (BossHandler boss : passedList) { value += boss.getSpeedKillTimer(); }
        value /= passedList.length;
        retboss.setSpeedKillTimer((int) Math.floor(value));
        value = 0f;
        for (BossHandler boss : passedList) { value += boss.getHealth(); }
        value /= passedList.length;
        retboss.setHealth(value);
        value = 0f;
        for (BossHandler boss : passedList) { value += (float) boss.getInBackPercMelee(); }
        value /= passedList.length;
        retboss.setInBackPercMelee(value);
        value = 0f;
        for (BossHandler boss : passedList) { value += (float) boss.getInBackPercRanged(); }
        value /= passedList.length;
        retboss.setInBackPercRanged(value);
        boolean is25 = false;
        for (BossHandler boss : passedList) {
            if (boss.getVersion() == BossHandler.Versions.V_25N || boss.getVersion() == BossHandler.Versions.V_25H) {
                is25 = true;
                break;
            }
        }
        retboss.setMaxPlayers(is25 ? 25 : 10);
        value = 0f;
        for (BossHandler boss : passedList) { value += boss.getMinTanks(); }
        value /= passedList.length;
        retboss.setMinTanks((int) Math.floor(value));
        value = 0f;
        for (BossHandler boss : passedList) { value += boss.getMinHealers(); }
        value /= passedList.length;
        retboss.setMinHealers((int) Math.floor(value));
        {   // Multi-targs
            float f = -1, d = -1, n = 1;
            value = 0f;
            for (BossHandler boss : passedList) {
                value += (boss.getMultiTargsFreq() > 0 && boss.getMultiTargsFreq() < boss.getBerserkTimer()) ? boss.getMultiTargsFreq() : retboss.getBerserkTimer();
            }
            value /= passedList.length;
            f = value;
            value = 0f;
            for (BossHandler boss : passedList) { value += boss.getMultiTargsDur(); }
            value /= passedList.length;
            d = value;
            value = 0f;
            for (BossHandler boss : passedList) { value += (float) boss.getMultiTargsNum(); }
            value /= passedList.length;
            n = value;
            if (f <= 0 || d == 0) {
            } else {
                TargetGroup tg = new TargetGroup();
                tg.frequency = f;
                tg.duration = d;
                tg.chance = 1f;
                tg.numTargs = n;
                tg.nearBoss = true;
                retboss.targets.add(tg);
            }
        }
        {   // Attacks
            {
                List<Attack> attacks = new ArrayList<Attack>();
                for (BossHandler boss : passedList) {
                    attacks.addAll(boss.getFilteredAttackList(AttackTypes.AT_MELEE));
                }
                if (attacks.size() > 0) {
                    float perhit = 0f, numtrg = 0f, atkspd = 0f;
                    for (Attack a : attacks) {
                        if (!"Invalid".equals(a.name)) {
                            perhit += a.getDamagePerHit();
                            numtrg += a.maxNumTargets;
                            atkspd += a.attackSpeed;
                        }
                    }
                    perhit /= (float) attacks.size();
                    numtrg /= (float) attacks.size();
                    atkspd /= (float) attacks.size();
                    Attack atk = new Attack();
                    atk.name = "Avg Melee";
                    atk.attackType = AttackTypes.AT_MELEE;
                    atk.damageType = ItemDamageType.Physical;
                    atk.setDamagePerHit(perhit);
                    atk.maxNumTargets = numtrg;
                    atk.attackSpeed = atkspd;
                    atk.isTheDefaultMelee = true;
                    retboss.getAttacks().add(atk);
                }
            }
            {
                List<Attack> attacks = new ArrayList<Attack>();
                for (BossHandler boss : passedList) {
                    attacks.addAll(boss.getFilteredAttackList(AttackTypes.AT_RANGED));
                }
                if (attacks.size() > 0) {
                    float perhit = 0f, numtrg = 0f, atkspd = 0f;
                    for (Attack a : attacks) {
                        if (!"Invalid".equals(a.name)) {
                            perhit += a.getDamagePerHit();
                            numtrg += a.maxNumTargets;
                            atkspd += a.attackSpeed;
                        }
                    }
                    perhit /= (float) attacks.size();
                    numtrg /= (float) attacks.size();
                    atkspd /= (float) attacks.size();
                    Attack atk = new Attack();
                    atk.name = "Avg Ranged";
                    atk.attackType = AttackTypes.AT_RANGED;
                    atk.damageType = ItemDamageType.Physical;
                    atk.setDamagePerHit(perhit);
                    atk.maxNumTargets = numtrg;
                    atk.attackSpeed = atkspd;
                    retboss.getAttacks().add(atk);
                }
            }
            {
                List<Attack> attacks = new ArrayList<Attack>();
                for (BossHandler boss : passedList) {
                    attacks.addAll(boss.getFilteredAttackList(AttackTypes.AT_AOE));
                }
                if (attacks.size() > 0) {
                    float perhit = 0f, numtrg = 0f, atkspd = 0f;
                    for (Attack a : attacks) {
                        if (!"Invalid".equals(a.name)) {
                            perhit += a.getDamagePerHit();
                            numtrg += a.maxNumTargets;
                            atkspd += a.attackSpeed;
                        }
                    }
                    perhit /= (float) attacks.size();
                    numtrg /= (float) attacks.size();
                    atkspd /= (float) attacks.size();
                    Attack atk = new Attack();
                    atk.name = "Avg AoE";
                    atk.attackType = AttackTypes.AT_AOE;
                    atk.damageType = ItemDamageType.Arcane;
                    atk.setDamagePerHit(perhit);
                    atk.maxNumTargets = numtrg;
                    atk.attackSpeed = atkspd;
                    retboss.getAttacks().add(atk);
                }
            }
        }
        for (ItemDamageType t : getDamageTypes()) {
            value = 0f;
            for (BossHandler boss : passedList) {
                value += (float) boss.getResistance(t);
            }
            value /= passedList.length;
            retboss.setResistance(t, value);
        }
        {   // Move
            float f = -1, d = -1;
            value = 0f;
            for (BossHandler boss : passedList) {
                value += (boss.getMovingTargsFreq() > 0 && boss.getMovingTargsFreq() < boss.getBerserkTimer()) ? boss.getMovingTargsFreq() : retboss.getBerserkTimer();
            }
            value /= passedList.length;
            f = value;
            value = 0f;
            for (BossHandler boss : passedList) { value += boss.getMovingTargsDur(); }
            value /= passedList.length;
            d = value;
            if (f <= 0 || d == 0) {
            } else {
                Impedance imp = new Impedance();
                imp.frequency = f;
                imp.duration = d;
                imp.chance = 1f;
                imp.breakable = true;
                retboss.moves.add(imp);
            }
        }
        {   // Stun
            float f = -1, d = -1;
            value = 0f;
            for (BossHandler boss : passedList) {
                value += (boss.getStunningTargsFreq() > 0 && boss.getStunningTargsFreq() < boss.getBerserkTimer()) ? boss.getStunningTargsFreq() : retboss.getBerserkTimer();
            }
            value /= passedList.length;
            f = value;
            value = 0f;
            for (BossHandler boss : passedList) { value += boss.getStunningTargsDur(); }
            value /= passedList.length;
            d = value;
            if (f <= 0 || d == 0) {
            } else {
                Impedance imp = new Impedance();
                imp.frequency = f;
                imp.duration = d;
                imp.chance = 1f;
                imp.breakable = true;
                retboss.stuns.add(imp);
            }
        }
        {   // Fear
            float f = -1, d = -1;
            value = 0f;
            for (BossHandler boss : passedList) {
                value += (boss.getFearingTargsFreq() > 0 && boss.getFearingTargsFreq() < boss.getBerserkTimer()) ? boss.getFearingTargsFreq() : retboss.getBerserkTimer();
            }
            value /= passedList.length;
            f = value;
            value = 0f;
            for (BossHandler boss : passedList) { value += boss.getFearingTargsDur(); }
            value /= passedList.length;
            d = value;
            if (f <= 0 || d == 0) {
            } else {
                Impedance imp = new Impedance();
                imp.frequency = f;
                imp.duration = d;
                imp.chance = 1f;
                imp.breakable = true;
                retboss.fears.add(imp);
            }
        }
        {   // Root
            float f = -1, d = -1;
            value = 0f;
            for (BossHandler boss : passedList) {
                value += (boss.getRootingTargsFreq() > 0 && boss.getRootingTargsFreq() < boss.getBerserkTimer()) ? boss.getRootingTargsFreq() : retboss.getBerserkTimer();
            }
            value /= passedList.length;
            f = value;
            value = 0f;
            for (BossHandler boss : passedList) { value += boss.getRootingTargsDur(); }
            value /= passedList.length;
            d = value;
            if (f <= 0 || d == 0) {
            } else {
                Impedance imp = new Impedance();
                imp.frequency = f;
                imp.duration = d;
                imp.chance = 1f;
                imp.breakable = true;
                retboss.roots.add(imp);
            }
        }
        {   // Disarm
            float f = -1, d = -1;
            value = 0f;
            for (BossHandler boss : passedList) {
                value += (boss.getDisarmingTargsFreq() > 0 && boss.getDisarmingTargsFreq() < boss.getBerserkTimer()) ? boss.getDisarmingTargsFreq() : retboss.getBerserkTimer();
            }
            value /= passedList.length;
            f = value;
            value = 0f;
            for (BossHandler boss : passedList) { value += boss.getDisarmingTargsDur(); }
            value /= passedList.length;
            d = value;
            if (f <= 0 || d == 0) {
            } else {
                Impedance imp = new Impedance();
                imp.frequency = f;
                imp.duration = d;
                imp.chance = 1f;
                imp.breakable = true;
                retboss.disarms.add(imp);
            }
        }
        return retboss;
    }

    private BossHandler genTheHardestBoss(BossHandler[] passedList) {
        useGoodBoyAvg = false;
        BossHandler retboss = new BossHandler();
        if (passedList.length < 1) {
            return retboss;
        }
        float value = 0f;
        retboss.setName("The Hardest Boss");
        value = BossHandler.TierLevels.T7_0.value();
        for (BossHandler boss : passedList) { value = Math.max(value, boss.getContent().value()); }
        retboss.setContent(BossHandler.TierLevels.of((int) Math.ceil(value)));
        value = 0f;
        for (BossHandler boss : passedList) { value = Math.max(value, boss.getLevel()); }
        retboss.setLevel((int) value);
        value = 0f;
        for (BossHandler boss : passedList) { value = Math.max(value, boss.getArmor()); }
        retboss.setArmor((int) value);
        value = 0f;
        for (BossHandler boss : passedList) { value = Math.max(value, boss.getHealth()); }
        retboss.setHealth(value);
        value = passedList[0].getBerserkTimer();
        for (BossHandler boss : passedList) { value = Math.min(value, boss.getBerserkTimer()); }
        retboss.setBerserkTimer((int) Math.floor(value));
        value = passedList[0].getSpeedKillTimer();
        for (BossHandler boss : passedList) { value = Math.min(value, boss.getSpeedKillTimer()); }
        retboss.setSpeedKillTimer((int) Math.floor(value));
        value = 1.00f;
        for (BossHandler boss : passedList) { value = (float) Math.min(value, boss.getInBackPercMelee()); }
        retboss.setInBackPercMelee(value);
        value = 1.00f;
        for (BossHandler boss : passedList) { value = (float) Math.min(value, boss.getInBackPercRanged()); }
        retboss.setInBackPercRanged(value);
        boolean is25 = false;
        for (BossHandler boss : passedList) {
            if (boss.getVersion() == BossHandler.Versions.V_25N || boss.getVersion() == BossHandler.Versions.V_25H) {
                is25 = true;
                break;
            }
        }
        retboss.setMaxPlayers(is25 ? 25 : 10);
        value = 0;
        for (BossHandler boss : passedList) { value = Math.max(value, boss.getMinTanks()); }
        retboss.setMinTanks((int) value);
        value = 0;
        for (BossHandler boss : passedList) { value = Math.max(value, boss.getMinHealers()); }
        retboss.setMinHealers((int) value);
        {   // Multi-targs
            float f = -1, d = -1, n = 1;
            value = 0;
            for (BossHandler boss : passedList) {
                value = Math.min(value, boss.getMultiTargsFreq() > 0 ? boss.getMultiTargsFreq() : value);
            }
            f = value;
            value = 0;
            for (BossHandler boss : passedList) { value = Math.max(value, boss.getMultiTargsDur()); }
            d = value;
            value = 0;
            for (BossHandler boss : passedList) { value = Math.max(value, (float) boss.getMultiTargsNum()); }
            n = value;
            if (!(f == 0 || d == 0)) {
                TargetGroup tg = new TargetGroup();
                tg.frequency = f;
                tg.duration = d;
                tg.chance = 1f;
                tg.numTargs = n;
                tg.nearBoss = false;
                retboss.targets.add(tg);
            }
        }
        {   // Attacks
            {
                List<Attack> attacks = new ArrayList<Attack>();
                for (BossHandler boss : passedList) {
                    attacks.addAll(boss.getFilteredAttackList(AttackTypes.AT_MELEE));
                }
                float perhit = 0f, numtrg = 0f, atkspd = 45f;
                for (Attack a : attacks) {
                    if (!"Invalid".equals(a.name)) {
                        perhit = Math.max(perhit, a.getDamagePerHit());
                        numtrg = Math.max(numtrg, a.maxNumTargets);
                        atkspd = Math.min(atkspd, a.attackSpeed);
                    }
                }
                Attack atk = new Attack();
                atk.name = "Avg Melee";
                atk.attackType = AttackTypes.AT_MELEE;
                atk.damageType = ItemDamageType.Physical;
                atk.setDamagePerHit(perhit);
                atk.maxNumTargets = numtrg;
                atk.attackSpeed = atkspd;
                atk.isTheDefaultMelee = true;
                retboss.getAttacks().add(atk);
            }
            {
                List<Attack> attacks = new ArrayList<Attack>();
                for (BossHandler boss : passedList) {
                    attacks.addAll(boss.getFilteredAttackList(AttackTypes.AT_RANGED));
                }
                float perhit = 0f, numtrg = 0f, atkspd = 45f;
                for (Attack a : attacks) {
                    if (!"Invalid".equals(a.name)) {
                        perhit = Math.max(perhit, a.getDamagePerHit());
                        numtrg = Math.max(numtrg, a.maxNumTargets);
                        atkspd = Math.min(atkspd, a.attackSpeed);
                    }
                }
                Attack atk = new Attack();
                atk.name = "Avg Ranged";
                atk.attackType = AttackTypes.AT_RANGED;
                atk.damageType = ItemDamageType.Physical;
                atk.setDamagePerHit(perhit);
                atk.maxNumTargets = numtrg;
                atk.attackSpeed = atkspd;
                retboss.getAttacks().add(atk);
            }
            {
                List<Attack> attacks = new ArrayList<Attack>();
                for (BossHandler boss : passedList) {
                    attacks.addAll(boss.getFilteredAttackList(AttackTypes.AT_AOE));
                }
                float perhit = 0f, numtrg = 0f, atkspd = 45f;
                for (Attack a : attacks) {
                    if (!"Invalid".equals(a.name)) {
                        perhit = Math.max(perhit, a.getDamagePerHit());
                        numtrg = Math.max(numtrg, a.maxNumTargets);
                        atkspd = Math.min(atkspd, a.attackSpeed);
                    }
                }
                Attack atk = new Attack();
                atk.name = "Avg AoE";
                atk.attackType = AttackTypes.AT_AOE;
                atk.damageType = ItemDamageType.Arcane;
                atk.setDamagePerHit(perhit);
                atk.maxNumTargets = numtrg;
                atk.attackSpeed = atkspd;
                retboss.getAttacks().add(atk);
            }
        }
        for (ItemDamageType t : getDamageTypes()) {
            value = 0f;
            for (BossHandler boss : passedList) { value = (float) Math.max(value, boss.getResistance(t)); }
            retboss.setResistance(t, value);
        }
        {   // Move
            float f = -1, d = -1;
            value = 19 * 20;
            for (BossHandler boss : passedList) { value = Math.min(value, boss.getMovingTargsFreq() > 0 ? boss.getMovingTargsFreq() : value); }
            f = Math.max(20, value);
            value = 0f;
            for (BossHandler boss : passedList) { value = Math.max(value, boss.getMovingTargsDur()); }
            d = Math.min(10 * 1000, value);
            if (f == 0 || d == 0) {
            } else {
                Impedance imp = new Impedance();
                imp.frequency = f;
                imp.duration = d;
                imp.chance = 1f;
                imp.breakable = true;
                retboss.moves.add(imp);
            }
        }
        {   // Stun
            float f = -1, d = -1;
            value = 19 * 20;
            for (BossHandler boss : passedList) { value = Math.min(value, boss.getStunningTargsFreq() > 0 ? boss.getStunningTargsFreq() : value); }
            f = Math.max(20, value);
            value = 0f;
            for (BossHandler boss : passedList) { value = Math.max(value, boss.getStunningTargsDur()); }
            d = Math.min(10 * 1000, value);
            if (f == 0 || d == 0) {
            } else {
                Impedance imp = new Impedance();
                imp.frequency = f;
                imp.duration = d;
                imp.chance = 1f;
                imp.breakable = true;
                retboss.stuns.add(imp);
            }
        }
        {   // Fear
            float f = -1, d = -1;
            value = 19 * 20;
            for (BossHandler boss : passedList) { value = Math.min(value, boss.getFearingTargsFreq() > 0 ? boss.getFearingTargsFreq() : value); }
            f = Math.max(20, value);
            value = 0f;
            for (BossHandler boss : passedList) { value = Math.max(value, boss.getFearingTargsDur()); }
            d = Math.min(10 * 1000, value);
            if (f == 0 || d == 0) {
            } else {
                Impedance imp = new Impedance();
                imp.frequency = f;
                imp.duration = d;
                imp.chance = 1f;
                imp.breakable = true;
                retboss.fears.add(imp);
            }
        }
        {   // Root
            float f = -1, d = -1;
            value = 19 * 20;
            for (BossHandler boss : passedList) { value = Math.min(value, boss.getRootingTargsFreq() > 0 ? boss.getRootingTargsFreq() : value); }
            f = Math.max(20, value);
            value = 0f;
            for (BossHandler boss : passedList) { value = Math.max(value, boss.getRootingTargsDur()); }
            d = Math.min(10 * 1000, value);
            if (f == 0 || d == 0) {
            } else {
                Impedance imp = new Impedance();
                imp.frequency = f;
                imp.duration = d;
                imp.chance = 1f;
                imp.breakable = true;
                retboss.roots.add(imp);
            }
        }
        {   // Disarm
            float f = -1, d = -1;
            value = 19 * 20;
            for (BossHandler boss : passedList) { value = Math.min(value, boss.getDisarmingTargsFreq() < 0 ? boss.getDisarmingTargsFreq() : value); }
            f = Math.max(20, value);
            value = 0f;
            for (BossHandler boss : passedList) { value = Math.max(value, boss.getDisarmingTargsDur()); }
            d = Math.min(10 * 1000, value);
            if (f == 0 || d == 0) {
            } else {
                Impedance imp = new Impedance();
                imp.frequency = f;
                imp.duration = d;
                imp.chance = 1f;
                imp.breakable = true;
                retboss.disarms.add(imp);
            }
        }
        return retboss;
    }
}
