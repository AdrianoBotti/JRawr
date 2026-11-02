// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Tree/CalculationOptionsTree.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port). getXml writes the
// XmlSerializer element order by hand (same approach as the other ported models); readXml parses
// the same shape back, including the nested Current SpellProfile.
package rawr.models.tree;

import rawr.base.calculations.ICalculationOptionBase;

import java.util.ArrayList;
import java.util.List;

public class CalculationOptionsTree implements ICalculationOptionBase {
    public static class SpellProfile {
        private String name;
        private int fightDuration;
        private int replenishmentUptime;
        private int wildGrowthPerMinute;
        private int innervates;
        private int swiftmendPerMinute;
        private int idleCastTimePercent;
        private int rejuvFrac, regrowthFrac, lifebloomFrac, nourishFrac;
        private int rejuvAmount, regrowthAmount, lifebloomStackAmount;
        private int lifebloomStackType;
        private int nourish1, nourish2, nourish3, nourish4;
        private int livingSeedEfficiency;
        private int revitalizePPM;

        private int adjTimeRejuv, adjTimeRegrowth, adjTimeNourish, adjTimeLifebloom;
        private int adjTimeSwiftmend, adjTimeWildGrowth, adjTimeIdle;
        private int adjTimeManagedRejuv, adjTimeManagedRegrowth, adjTimeManagerLifebloomStack;

        private int adjTimeRejuvOrder, adjTimeRegrowthOrder, adjTimeNourishOrder, adjTimeLifebloomOrder;
        private int adjTimeSwiftmendOrder, adjTimeWildGrowthOrder, adjTimeIdleOrder;
        private int adjTimeManagedRejuvOrder, adjTimeManagedRegrowthOrder, adjTimeManagedLifebloomStackOrder;

        private int reduceOOMRejuv, reduceOOMRegrowth, reduceOOMLifebloom, reduceOOMNourish, reduceOOMWildGrowth;
        private int reduceOOMRejuvOrder, reduceOOMRegrowthOrder, reduceOOMLifebloomOrder, reduceOOMNourishOrder, reduceOOMWildGrowthOrder;

        public SpellProfile() {
            name = "Custom";

            fightDuration = 300;
            replenishmentUptime = 80;
            innervates = 1;

            idleCastTimePercent = 0;

            rejuvFrac = 50;
            regrowthFrac = 0;
            lifebloomFrac = 10;
            nourishFrac = 40;
            lifebloomStackAmount = 0;
            rejuvAmount = 0;
            regrowthAmount = 0;
            lifebloomStackType = 2;
            nourish1 = 60;
            nourish2 = 10;
            nourish3 = 0;
            nourish4 = 0;
            wildGrowthPerMinute = 4;
            swiftmendPerMinute = 2;
            livingSeedEfficiency = 40;

            adjTimeRejuv = 100;
            adjTimeRegrowth = 100;
            adjTimeNourish = 100;
            adjTimeLifebloom = 100;
            adjTimeSwiftmend = 100;
            adjTimeWildGrowth = 100;
            adjTimeIdle = 100;
            adjTimeManagedRejuv = 100;
            adjTimeManagedRegrowth = 100;
            adjTimeManagerLifebloomStack = 100;

            adjTimeRejuvOrder = 0;
            adjTimeRegrowthOrder = 0;
            adjTimeNourishOrder = 0;
            adjTimeLifebloomOrder = 0;
            adjTimeSwiftmendOrder = 2;
            adjTimeWildGrowthOrder = 1;
            adjTimeIdleOrder = 4;
            adjTimeManagedRejuvOrder = 3;
            adjTimeManagedRegrowthOrder = 3;
            adjTimeManagedLifebloomStackOrder = 3;

            reduceOOMRejuv = 40;
            reduceOOMNourish = 80;
            reduceOOMLifebloom = 100;
            reduceOOMRegrowth = 100;
            reduceOOMWildGrowth = 40;

            reduceOOMNourishOrder = 0;
            reduceOOMLifebloomOrder = 1;
            reduceOOMRegrowthOrder = 2;
            reduceOOMWildGrowthOrder = 3;
            reduceOOMRejuvOrder = 4;

            revitalizePPM = 5;
        }

        public String getName() {
            return name;
        }

        public void setName(String value) {
            name = value;
        }

        public int getFightDuration() {
            return fightDuration;
        }

        public void setFightDuration(int value) {
            fightDuration = value;
        }

        public int getReplenishmentUptime() {
            return replenishmentUptime;
        }

        public void setReplenishmentUptime(int value) {
            replenishmentUptime = value;
        }

        public int getWildGrowthPerMinute() {
            return wildGrowthPerMinute;
        }

        public void setWildGrowthPerMinute(int value) {
            wildGrowthPerMinute = value;
        }

        public int getInnervates() {
            return innervates;
        }

        public void setInnervates(int value) {
            innervates = value;
        }

        public int getSwiftmendPerMinute() {
            return swiftmendPerMinute;
        }

        public void setSwiftmendPerMinute(int value) {
            swiftmendPerMinute = value;
        }

        public int getIdleCastTimePercent() {
            return idleCastTimePercent;
        }

        public void setIdleCastTimePercent(int value) {
            idleCastTimePercent = value;
        }

        public int getRejuvFrac() {
            return rejuvFrac;
        }

        public void setRejuvFrac(int value) {
            rejuvFrac = value;
        }

        public int getRegrowthFrac() {
            return regrowthFrac;
        }

        public void setRegrowthFrac(int value) {
            regrowthFrac = value;
        }

        public int getLifebloomFrac() {
            return lifebloomFrac;
        }

        public void setLifebloomFrac(int value) {
            lifebloomFrac = value;
        }

        public int getNourishFrac() {
            return nourishFrac;
        }

        public void setNourishFrac(int value) {
            nourishFrac = value;
        }

        public int getLifebloomStackAmount() {
            return lifebloomStackAmount;
        }

        public void setLifebloomStackAmount(int value) {
            lifebloomStackAmount = value;
        }

        public int getRejuvAmount() {
            return rejuvAmount;
        }

        public void setRejuvAmount(int value) {
            rejuvAmount = value;
        }

        public int getRegrowthAmount() {
            return regrowthAmount;
        }

        public void setRegrowthAmount(int value) {
            regrowthAmount = value;
        }

        public int getLifebloomStackType() {
            return lifebloomStackType;
        }

        public void setLifebloomStackType(int value) {
            lifebloomStackType = value;
        }

        public int getNourish1() {
            return nourish1;
        }

        public void setNourish1(int value) {
            nourish1 = value;
        }

        public int getNourish2() {
            return nourish2;
        }

        public void setNourish2(int value) {
            nourish2 = value;
        }

        public int getNourish3() {
            return nourish3;
        }

        public void setNourish3(int value) {
            nourish3 = value;
        }

        public int getNourish4() {
            return nourish4;
        }

        public void setNourish4(int value) {
            nourish4 = value;
        }

        public int getAdjustTimeRejuv() {
            return adjTimeRejuv;
        }

        public void setAdjustTimeRejuv(int value) {
            adjTimeRejuv = value;
        }

        public int getAdjustTimeRegrowth() {
            return adjTimeRegrowth;
        }

        public void setAdjustTimeRegrowth(int value) {
            adjTimeRegrowth = value;
        }

        public int getAdjustTimeNourish() {
            return adjTimeNourish;
        }

        public void setAdjustTimeNourish(int value) {
            adjTimeNourish = value;
        }

        public int getAdjustTimeLifebloom() {
            return adjTimeLifebloom;
        }

        public void setAdjustTimeLifebloom(int value) {
            adjTimeLifebloom = value;
        }

        public int getAdjustTimeSwiftmend() {
            return adjTimeSwiftmend;
        }

        public void setAdjustTimeSwiftmend(int value) {
            adjTimeSwiftmend = value;
        }

        public int getAdjustTimeWildGrowth() {
            return adjTimeWildGrowth;
        }

        public void setAdjustTimeWildGrowth(int value) {
            adjTimeWildGrowth = value;
        }

        public int getAdjustTimeIdle() {
            return adjTimeIdle;
        }

        public void setAdjustTimeIdle(int value) {
            adjTimeIdle = value;
        }

        public int getAdjustTimeManagedRejuv() {
            return adjTimeManagedRejuv;
        }

        public void setAdjustTimeManagedRejuv(int value) {
            adjTimeManagedRejuv = value;
        }

        public int getAdjustTimeManagedRegrowth() {
            return adjTimeManagedRegrowth;
        }

        public void setAdjustTimeManagedRegrowth(int value) {
            adjTimeManagedRegrowth = value;
        }

        public int getAdjustTimeManagedLifebloomStack() {
            return adjTimeManagerLifebloomStack;
        }

        public void setAdjustTimeManagedLifebloomStack(int value) {
            adjTimeManagerLifebloomStack = value;
        }

        public int getAdjustTimeRejuvOrder() {
            return adjTimeRejuvOrder;
        }

        public void setAdjustTimeRejuvOrder(int value) {
            adjTimeRejuvOrder = value;
        }

        public int getAdjustTimeRegrowthOrder() {
            return adjTimeRegrowthOrder;
        }

        public void setAdjustTimeRegrowthOrder(int value) {
            adjTimeRegrowthOrder = value;
        }

        public int getAdjustTimeNourishOrder() {
            return adjTimeNourishOrder;
        }

        public void setAdjustTimeNourishOrder(int value) {
            adjTimeNourishOrder = value;
        }

        public int getAdjustTimeLifebloomOrder() {
            return adjTimeLifebloomOrder;
        }

        public void setAdjustTimeLifebloomOrder(int value) {
            adjTimeLifebloomOrder = value;
        }

        public int getAdjustTimeSwiftmendOrder() {
            return adjTimeSwiftmendOrder;
        }

        public void setAdjustTimeSwiftmendOrder(int value) {
            adjTimeSwiftmendOrder = value;
        }

        public int getAdjustTimeWildGrowthOrder() {
            return adjTimeWildGrowthOrder;
        }

        public void setAdjustTimeWildGrowthOrder(int value) {
            adjTimeWildGrowthOrder = value;
        }

        public int getAdjustTimeIdleOrder() {
            return adjTimeIdleOrder;
        }

        public void setAdjustTimeIdleOrder(int value) {
            adjTimeIdleOrder = value;
        }

        public int getAdjustTimeManagedRejuvOrder() {
            return adjTimeManagedRejuvOrder;
        }

        public void setAdjustTimeManagedRejuvOrder(int value) {
            adjTimeManagedRejuvOrder = value;
        }

        public int getAdjustTimeManagedRegrowthOrder() {
            return adjTimeManagedRegrowthOrder;
        }

        public void setAdjustTimeManagedRegrowthOrder(int value) {
            adjTimeManagedRegrowthOrder = value;
        }

        public int getAdjustTimeManagedLifebloomStackOrder() {
            return adjTimeManagedLifebloomStackOrder;
        }

        public void setAdjustTimeManagedLifebloomStackOrder(int value) {
            adjTimeManagedLifebloomStackOrder = value;
        }

        public int getReduceOOMRejuv() {
            return reduceOOMRejuv;
        }

        public void setReduceOOMRejuv(int value) {
            reduceOOMRejuv = value;
        }

        public int getReduceOOMRegrowth() {
            return reduceOOMRegrowth;
        }

        public void setReduceOOMRegrowth(int value) {
            reduceOOMRegrowth = value;
        }

        public int getReduceOOMLifebloom() {
            return reduceOOMLifebloom;
        }

        public void setReduceOOMLifebloom(int value) {
            reduceOOMLifebloom = value;
        }

        public int getReduceOOMNourish() {
            return reduceOOMNourish;
        }

        public void setReduceOOMNourish(int value) {
            reduceOOMNourish = value;
        }

        public int getReduceOOMWildGrowth() {
            return reduceOOMWildGrowth;
        }

        public void setReduceOOMWildGrowth(int value) {
            reduceOOMWildGrowth = value;
        }

        public int getReduceOOMRejuvOrder() {
            return reduceOOMRejuvOrder;
        }

        public void setReduceOOMRejuvOrder(int value) {
            reduceOOMRejuvOrder = value;
        }

        public int getReduceOOMRegrowthOrder() {
            return reduceOOMRegrowthOrder;
        }

        public void setReduceOOMRegrowthOrder(int value) {
            reduceOOMRegrowthOrder = value;
        }

        public int getReduceOOMLifebloomOrder() {
            return reduceOOMLifebloomOrder;
        }

        public void setReduceOOMLifebloomOrder(int value) {
            reduceOOMLifebloomOrder = value;
        }

        public int getReduceOOMNourishOrder() {
            return reduceOOMNourishOrder;
        }

        public void setReduceOOMNourishOrder(int value) {
            reduceOOMNourishOrder = value;
        }

        public int getReduceOOMWildGrowthOrder() {
            return reduceOOMWildGrowthOrder;
        }

        public void setReduceOOMWildGrowthOrder(int value) {
            reduceOOMWildGrowthOrder = value;
        }

        public int getLivingSeedEfficiency() {
            return livingSeedEfficiency;
        }

        public void setLivingSeedEfficiency(int value) {
            livingSeedEfficiency = value;
        }

        public int getRevitalizePPM() {
            return revitalizePPM;
        }

        public void setRevitalizePPM(int value) {
            revitalizePPM = value;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    private int singleTarget;
    private int sustainedTarget;
    private int survValuePer100;
    private int singleTargetRotation;
    private boolean ignoreNaturesGrace;
    private int procType;

    private List<SpellProfile> profiles;
    private SpellProfile current;

    public CalculationOptionsTree() {
        setSurvValuePer100(1);

        singleTarget = 9000;
        sustainedTarget = 8500;

        singleTargetRotation = 0;

        profiles = new ArrayList<SpellProfile>();
        current = new SpellProfile();

        ignoreNaturesGrace = true;
        procType = 0;
    }

    public int getSingleTarget() {
        return singleTarget;
    }

    public void setSingleTarget(int value) {
        singleTarget = value;
    }

    public int getSustainedTarget() {
        return sustainedTarget;
    }

    public void setSustainedTarget(int value) {
        sustainedTarget = value;
    }

    public int getSurvValuePer100() {
        return survValuePer100;
    }

    public void setSurvValuePer100(int value) {
        survValuePer100 = value;
    }

    public List<SpellProfile> getProfiles() {
        return profiles;
    }

    public void setProfiles(List<SpellProfile> value) {
        profiles = value;
    }

    public SpellProfile getCurrent() {
        return current;
    }

    public void setCurrent(SpellProfile value) {
        current = value;
    }

    public int getSingleTargetRotation() {
        return singleTargetRotation;
    }

    public void setSingleTargetRotation(int value) {
        singleTargetRotation = value;
    }

    public boolean getIgnoreNaturesGrace() {
        return ignoreNaturesGrace;
    }

    public void setIgnoreNaturesGrace(boolean value) {
        ignoreNaturesGrace = value;
    }

    public int getProcType() {
        return procType;
    }

    public void setProcType(int value) {
        procType = value;
    }

    // C# CalculationOptionsTree.GetXml (XmlSerializer on the public properties, declaration order).
    @Override
    public String getXml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>");
        sb.append("<CalculationOptionsTree xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">");
        sb.append("<SingleTarget>").append(singleTarget).append("</SingleTarget>");
        sb.append("<SustainedTarget>").append(sustainedTarget).append("</SustainedTarget>");
        sb.append("<SurvValuePer100>").append(survValuePer100).append("</SurvValuePer100>");
        if (profiles == null || profiles.isEmpty()) {
            sb.append("<Profiles />");
        } else {
            sb.append("<Profiles>");
            for (SpellProfile profile : profiles) {
                sb.append("<SpellProfile>");
                appendProfileXml(sb, profile);
                sb.append("</SpellProfile>");
            }
            sb.append("</Profiles>");
        }
        sb.append("<Current>");
        if (current != null) appendProfileXml(sb, current);
        sb.append("</Current>");
        sb.append("<SingleTargetRotation>").append(singleTargetRotation).append("</SingleTargetRotation>");
        sb.append("<IgnoreNaturesGrace>").append(ignoreNaturesGrace ? "true" : "false").append("</IgnoreNaturesGrace>");
        sb.append("<ProcType>").append(procType).append("</ProcType>");
        sb.append("</CalculationOptionsTree>");
        return sb.toString();
    }

    private static void appendProfileXml(StringBuilder sb, SpellProfile p) {
        sb.append("<Name>").append(p.getName() == null ? "" : escapeXml(p.getName())).append("</Name>");
        sb.append("<FightDuration>").append(p.getFightDuration()).append("</FightDuration>");
        sb.append("<ReplenishmentUptime>").append(p.getReplenishmentUptime()).append("</ReplenishmentUptime>");
        sb.append("<WildGrowthPerMinute>").append(p.getWildGrowthPerMinute()).append("</WildGrowthPerMinute>");
        sb.append("<Innervates>").append(p.getInnervates()).append("</Innervates>");
        sb.append("<SwiftmendPerMinute>").append(p.getSwiftmendPerMinute()).append("</SwiftmendPerMinute>");
        sb.append("<IdleCastTimePercent>").append(p.getIdleCastTimePercent()).append("</IdleCastTimePercent>");
        sb.append("<RejuvFrac>").append(p.getRejuvFrac()).append("</RejuvFrac>");
        sb.append("<RegrowthFrac>").append(p.getRegrowthFrac()).append("</RegrowthFrac>");
        sb.append("<LifebloomFrac>").append(p.getLifebloomFrac()).append("</LifebloomFrac>");
        sb.append("<NourishFrac>").append(p.getNourishFrac()).append("</NourishFrac>");
        sb.append("<LifebloomStackAmount>").append(p.getLifebloomStackAmount()).append("</LifebloomStackAmount>");
        sb.append("<RejuvAmount>").append(p.getRejuvAmount()).append("</RejuvAmount>");
        sb.append("<RegrowthAmount>").append(p.getRegrowthAmount()).append("</RegrowthAmount>");
        sb.append("<LifebloomStackType>").append(p.getLifebloomStackType()).append("</LifebloomStackType>");
        sb.append("<Nourish1>").append(p.getNourish1()).append("</Nourish1>");
        sb.append("<Nourish2>").append(p.getNourish2()).append("</Nourish2>");
        sb.append("<Nourish3>").append(p.getNourish3()).append("</Nourish3>");
        sb.append("<Nourish4>").append(p.getNourish4()).append("</Nourish4>");
        sb.append("<AdjustTimeRejuv>").append(p.getAdjustTimeRejuv()).append("</AdjustTimeRejuv>");
        sb.append("<AdjustTimeRegrowth>").append(p.getAdjustTimeRegrowth()).append("</AdjustTimeRegrowth>");
        sb.append("<AdjustTimeNourish>").append(p.getAdjustTimeNourish()).append("</AdjustTimeNourish>");
        sb.append("<AdjustTimeLifebloom>").append(p.getAdjustTimeLifebloom()).append("</AdjustTimeLifebloom>");
        sb.append("<AdjustTimeSwiftmend>").append(p.getAdjustTimeSwiftmend()).append("</AdjustTimeSwiftmend>");
        sb.append("<AdjustTimeWildGrowth>").append(p.getAdjustTimeWildGrowth()).append("</AdjustTimeWildGrowth>");
        sb.append("<AdjustTimeIdle>").append(p.getAdjustTimeIdle()).append("</AdjustTimeIdle>");
        sb.append("<AdjustTimeManagedRejuv>").append(p.getAdjustTimeManagedRejuv()).append("</AdjustTimeManagedRejuv>");
        sb.append("<AdjustTimeManagedRegrowth>").append(p.getAdjustTimeManagedRegrowth()).append("</AdjustTimeManagedRegrowth>");
        sb.append("<AdjustTimeManagedLifebloomStack>").append(p.getAdjustTimeManagedLifebloomStack()).append("</AdjustTimeManagedLifebloomStack>");
        sb.append("<AdjustTimeRejuvOrder>").append(p.getAdjustTimeRejuvOrder()).append("</AdjustTimeRejuvOrder>");
        sb.append("<AdjustTimeRegrowthOrder>").append(p.getAdjustTimeRegrowthOrder()).append("</AdjustTimeRegrowthOrder>");
        sb.append("<AdjustTimeNourishOrder>").append(p.getAdjustTimeNourishOrder()).append("</AdjustTimeNourishOrder>");
        sb.append("<AdjustTimeLifebloomOrder>").append(p.getAdjustTimeLifebloomOrder()).append("</AdjustTimeLifebloomOrder>");
        sb.append("<AdjustTimeSwiftmendOrder>").append(p.getAdjustTimeSwiftmendOrder()).append("</AdjustTimeSwiftmendOrder>");
        sb.append("<AdjustTimeWildGrowthOrder>").append(p.getAdjustTimeWildGrowthOrder()).append("</AdjustTimeWildGrowthOrder>");
        sb.append("<AdjustTimeIdleOrder>").append(p.getAdjustTimeIdleOrder()).append("</AdjustTimeIdleOrder>");
        sb.append("<AdjustTimeManagedRejuvOrder>").append(p.getAdjustTimeManagedRejuvOrder()).append("</AdjustTimeManagedRejuvOrder>");
        sb.append("<AdjustTimeManagedRegrowthOrder>").append(p.getAdjustTimeManagedRegrowthOrder()).append("</AdjustTimeManagedRegrowthOrder>");
        sb.append("<AdjustTimeManagedLifebloomStackOrder>").append(p.getAdjustTimeManagedLifebloomStackOrder()).append("</AdjustTimeManagedLifebloomStackOrder>");
        sb.append("<ReduceOOMRejuv>").append(p.getReduceOOMRejuv()).append("</ReduceOOMRejuv>");
        sb.append("<ReduceOOMRegrowth>").append(p.getReduceOOMRegrowth()).append("</ReduceOOMRegrowth>");
        sb.append("<ReduceOOMLifebloom>").append(p.getReduceOOMLifebloom()).append("</ReduceOOMLifebloom>");
        sb.append("<ReduceOOMNourish>").append(p.getReduceOOMNourish()).append("</ReduceOOMNourish>");
        sb.append("<ReduceOOMWildGrowth>").append(p.getReduceOOMWildGrowth()).append("</ReduceOOMWildGrowth>");
        sb.append("<ReduceOOMRejuvOrder>").append(p.getReduceOOMRejuvOrder()).append("</ReduceOOMRejuvOrder>");
        sb.append("<ReduceOOMRegrowthOrder>").append(p.getReduceOOMRegrowthOrder()).append("</ReduceOOMRegrowthOrder>");
        sb.append("<ReduceOOMLifebloomOrder>").append(p.getReduceOOMLifebloomOrder()).append("</ReduceOOMLifebloomOrder>");
        sb.append("<ReduceOOMNourishOrder>").append(p.getReduceOOMNourishOrder()).append("</ReduceOOMNourishOrder>");
        sb.append("<ReduceOOMWildGrowthOrder>").append(p.getReduceOOMWildGrowthOrder()).append("</ReduceOOMWildGrowthOrder>");
        sb.append("<LivingSeedEfficiency>").append(p.getLivingSeedEfficiency()).append("</LivingSeedEfficiency>");
        sb.append("<RevitalizePPM>").append(p.getRevitalizePPM()).append("</RevitalizePPM>");
    }

    private static String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /**
     * Parses the XmlSerializer shape written by {@link #getXml()} (and by the original app).
     * Unknown elements are ignored; missing ones keep the constructor defaults.
     */
    public static CalculationOptionsTree readXml(org.w3c.dom.Element root) {
        CalculationOptionsTree options = new CalculationOptionsTree();
        for (org.w3c.dom.Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
            org.w3c.dom.Element child = (org.w3c.dom.Element) node;
            String name = child.getTagName();
            switch (name) {
                case "SingleTarget":
                    options.setSingleTarget(readInt(child));
                    break;
                case "SustainedTarget":
                    options.setSustainedTarget(readInt(child));
                    break;
                case "SurvValuePer100":
                    options.setSurvValuePer100(readInt(child));
                    break;
                case "Profiles": {
                    List<SpellProfile> list = new ArrayList<SpellProfile>();
                    for (org.w3c.dom.Node p = child.getFirstChild(); p != null; p = p.getNextSibling()) {
                        if (p.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                        list.add(readProfile((org.w3c.dom.Element) p));
                    }
                    options.setProfiles(list);
                    break;
                }
                case "Current": {
                    SpellProfile profile = new SpellProfile();
                    for (org.w3c.dom.Node p = child.getFirstChild(); p != null; p = p.getNextSibling()) {
                        if (p.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
                        readProfileProperty(profile, (org.w3c.dom.Element) p);
                    }
                    options.setCurrent(profile);
                    break;
                }
                case "SingleTargetRotation":
                    options.setSingleTargetRotation(readInt(child));
                    break;
                case "IgnoreNaturesGrace":
                    options.setIgnoreNaturesGrace(Boolean.parseBoolean(child.getTextContent().trim()));
                    break;
                case "ProcType":
                    options.setProcType(readInt(child));
                    break;
                default:
                    break;
            }
        }
        return options;
    }

    private static SpellProfile readProfile(org.w3c.dom.Element element) {
        SpellProfile profile = new SpellProfile();
        for (org.w3c.dom.Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;
            readProfileProperty(profile, (org.w3c.dom.Element) node);
        }
        return profile;
    }

    private static void readProfileProperty(SpellProfile profile, org.w3c.dom.Element element) {
        String name = element.getTagName();
        switch (name) {
            case "Name": profile.setName(element.getTextContent().trim()); break;
            case "FightDuration": profile.setFightDuration(readInt(element)); break;
            case "ReplenishmentUptime": profile.setReplenishmentUptime(readInt(element)); break;
            case "WildGrowthPerMinute": profile.setWildGrowthPerMinute(readInt(element)); break;
            case "Innervates": profile.setInnervates(readInt(element)); break;
            case "SwiftmendPerMinute": profile.setSwiftmendPerMinute(readInt(element)); break;
            case "IdleCastTimePercent": profile.setIdleCastTimePercent(readInt(element)); break;
            case "RejuvFrac": profile.setRejuvFrac(readInt(element)); break;
            case "RegrowthFrac": profile.setRegrowthFrac(readInt(element)); break;
            case "LifebloomFrac": profile.setLifebloomFrac(readInt(element)); break;
            case "NourishFrac": profile.setNourishFrac(readInt(element)); break;
            case "LifebloomStackAmount": profile.setLifebloomStackAmount(readInt(element)); break;
            case "RejuvAmount": profile.setRejuvAmount(readInt(element)); break;
            case "RegrowthAmount": profile.setRegrowthAmount(readInt(element)); break;
            case "LifebloomStackType": profile.setLifebloomStackType(readInt(element)); break;
            case "Nourish1": profile.setNourish1(readInt(element)); break;
            case "Nourish2": profile.setNourish2(readInt(element)); break;
            case "Nourish3": profile.setNourish3(readInt(element)); break;
            case "Nourish4": profile.setNourish4(readInt(element)); break;
            case "AdjustTimeRejuv": profile.setAdjustTimeRejuv(readInt(element)); break;
            case "AdjustTimeRegrowth": profile.setAdjustTimeRegrowth(readInt(element)); break;
            case "AdjustTimeNourish": profile.setAdjustTimeNourish(readInt(element)); break;
            case "AdjustTimeLifebloom": profile.setAdjustTimeLifebloom(readInt(element)); break;
            case "AdjustTimeSwiftmend": profile.setAdjustTimeSwiftmend(readInt(element)); break;
            case "AdjustTimeWildGrowth": profile.setAdjustTimeWildGrowth(readInt(element)); break;
            case "AdjustTimeIdle": profile.setAdjustTimeIdle(readInt(element)); break;
            case "AdjustTimeManagedRejuv": profile.setAdjustTimeManagedRejuv(readInt(element)); break;
            case "AdjustTimeManagedRegrowth": profile.setAdjustTimeManagedRegrowth(readInt(element)); break;
            case "AdjustTimeManagedLifebloomStack": profile.setAdjustTimeManagedLifebloomStack(readInt(element)); break;
            case "AdjustTimeRejuvOrder": profile.setAdjustTimeRejuvOrder(readInt(element)); break;
            case "AdjustTimeRegrowthOrder": profile.setAdjustTimeRegrowthOrder(readInt(element)); break;
            case "AdjustTimeNourishOrder": profile.setAdjustTimeNourishOrder(readInt(element)); break;
            case "AdjustTimeLifebloomOrder": profile.setAdjustTimeLifebloomOrder(readInt(element)); break;
            case "AdjustTimeSwiftmendOrder": profile.setAdjustTimeSwiftmendOrder(readInt(element)); break;
            case "AdjustTimeWildGrowthOrder": profile.setAdjustTimeWildGrowthOrder(readInt(element)); break;
            case "AdjustTimeIdleOrder": profile.setAdjustTimeIdleOrder(readInt(element)); break;
            case "AdjustTimeManagedRejuvOrder": profile.setAdjustTimeManagedRejuvOrder(readInt(element)); break;
            case "AdjustTimeManagedRegrowthOrder": profile.setAdjustTimeManagedRegrowthOrder(readInt(element)); break;
            case "AdjustTimeManagedLifebloomStackOrder": profile.setAdjustTimeManagedLifebloomStackOrder(readInt(element)); break;
            case "ReduceOOMRejuv": profile.setReduceOOMRejuv(readInt(element)); break;
            case "ReduceOOMRegrowth": profile.setReduceOOMRegrowth(readInt(element)); break;
            case "ReduceOOMLifebloom": profile.setReduceOOMLifebloom(readInt(element)); break;
            case "ReduceOOMNourish": profile.setReduceOOMNourish(readInt(element)); break;
            case "ReduceOOMWildGrowth": profile.setReduceOOMWildGrowth(readInt(element)); break;
            case "ReduceOOMRejuvOrder": profile.setReduceOOMRejuvOrder(readInt(element)); break;
            case "ReduceOOMRegrowthOrder": profile.setReduceOOMRegrowthOrder(readInt(element)); break;
            case "ReduceOOMLifebloomOrder": profile.setReduceOOMLifebloomOrder(readInt(element)); break;
            case "ReduceOOMNourishOrder": profile.setReduceOOMNourishOrder(readInt(element)); break;
            case "ReduceOOMWildGrowthOrder": profile.setReduceOOMWildGrowthOrder(readInt(element)); break;
            case "LivingSeedEfficiency": profile.setLivingSeedEfficiency(readInt(element)); break;
            case "RevitalizePPM": profile.setRevitalizePPM(readInt(element)); break;
            default: break;
        }
    }

    private static int readInt(org.w3c.dom.Element element) {
        String text = element.getTextContent().trim();
        return text.isEmpty() ? 0 : Integer.parseInt(text);
    }
}
