// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Warlock/CalculationOptionsWarlock.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// PARTIAL: INotifyPropertyChanged is omitted (no UI consumers in the port); getXml writes the
// XmlSerializer element order by hand (same approach as CalculationOptionsRogue).
package rawr.models.warlock;

import rawr.base.calculations.ICalculationOptionBase;
import rawr.base.util.NetFormat;

import java.util.ArrayList;
import java.util.List;

public class CalculationOptionsWarlock implements ICalculationOptionBase {

    public static CalculationOptionsWarlock makeDefaultOptions() {
        CalculationOptionsWarlock options = new CalculationOptionsWarlock();
        options.pet = "None";
        options.targetLevel = 83;
        options.duration = 300;
        options.latency = .1f;
        options.rotations = new ArrayList<Rotation>();

        options.rotations.add(new Rotation(
                "Affliction",
                "Shadow Bolt",
                "Drain Soul",
                "Haunt",
                "Corruption",
                "Unstable Affliction",
                "Curse Of Agony"));
        options.rotations.add(new Rotation(
                "Demonology",
                "Shadow Bolt",
                "Soul Fire",
                "Immolation Aura",
                "Corruption",
                "Immolate",
                "Incinerate (Under Molten Core)",
                "Curse Of Agony"));
        options.rotations.add(new Rotation(
                "Destruction",
                "Incinerate",
                null,
                "Immolate",
                "Conflagrate",
                "Incinerate (Under Backdraft)",
                "Chaos Bolt",
                "Curse Of Doom"));

        return options;
    }

    private static final int[] hitRatesByLevelDifference =
            { 100 - 4, 100 - 5, 100 - 6, 100 - 17, 100 - 28, 100 - 39 };

    private String pet = "None";
    // default it here for backward compatibility w/ files from before it was added
    private String imbue = "Grand Spellstone";
    private boolean useInfernal = false;
    private int targetLevel = 83;
    private float duration = 300;
    private float latency = 200;
    // defaults for backward compatibility
    private float thirtyFive = 0.25f;
    private float twentyFive = 0.15f;
    private List<Rotation> rotations = new ArrayList<Rotation>();
    private int activeRotationIndex = 0;

    private float perSP = 1;
    private boolean convertTotem = false;
    // default for back. compat.
    private float perFlametongue = 500f;
    private float perMagicBuff = 0;
    private float perCritBuff = 0;
    private float perInt = 0;
    private float perSpi = 0;
    private float perHealth = 0;

    private boolean noProcs = false;

    public String getPet() {
        return pet;
    }

    public void setPet(String value) {
        pet = value;
    }

    public String getImbue() {
        return imbue;
    }

    public void setImbue(String value) {
        imbue = value;
    }

    public boolean getUseInfernal() {
        return useInfernal;
    }

    public void setUseInfernal(boolean value) {
        useInfernal = value;
    }

    public int getTargetLevel() {
        return targetLevel;
    }

    public void setTargetLevel(int value) {
        targetLevel = value;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float value) {
        duration = value;
    }

    public float getLatency() {
        return latency;
    }

    public void setLatency(float value) {
        latency = value;
    }

    public float getThirtyFive() {
        return thirtyFive;
    }

    public void setThirtyFive(float value) {
        thirtyFive = value;
    }

    public float getTwentyFive() {
        return twentyFive;
    }

    public void setTwentyFive(float value) {
        twentyFive = value;
    }

    public List<Rotation> getRotations() {
        return rotations;
    }

    public void setRotations(List<Rotation> value) {
        rotations = value;
    }

    public int getActiveRotationIndex() {
        return activeRotationIndex;
    }

    public void setActiveRotationIndex(int value) {
        activeRotationIndex = value;
    }

    public float getPerSP() {
        return perSP;
    }

    public void setPerSP(float value) {
        perSP = value;
    }

    public boolean getConvertTotem() {
        return convertTotem;
    }

    public void setConvertTotem(boolean value) {
        convertTotem = value;
    }

    public float getPerFlametongue() {
        return perFlametongue;
    }

    public void setPerFlametongue(float value) {
        perFlametongue = value;
    }

    public float getPerMagicBuff() {
        return perMagicBuff;
    }

    public void setPerMagicBuff(float value) {
        perMagicBuff = value;
    }

    public float getPerCritBuff() {
        return perCritBuff;
    }

    public void setPerCritBuff(float value) {
        perCritBuff = value;
    }

    public float getPerInt() {
        return perInt;
    }

    public void setPerInt(float value) {
        perInt = value;
    }

    public float getPerSpi() {
        return perSpi;
    }

    public void setPerSpi(float value) {
        perSpi = value;
    }

    public float getPerHealth() {
        return perHealth;
    }

    public void setPerHealth(float value) {
        perHealth = value;
    }

    public boolean getNoProcs() {
        return noProcs;
    }

    public void setNoProcs(boolean value) {
        noProcs = value;
    }

    public Rotation getActiveRotation() {
        return (rotations.size() > 0 ? rotations.get(activeRotationIndex) : new Rotation());
    }

    public void removeActiveRotation() {
        rotations.remove(activeRotationIndex);
        activeRotationIndex = Math.min(activeRotationIndex, rotations.size() - 1);
    }

    public int getBaseHitRate() {
        if (targetLevel < 80) targetLevel = 80;
        else if (targetLevel > 83) targetLevel = 83;
        return hitRatesByLevelDifference[targetLevel - 80];
    }

    @Override
    public String getXml() {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"utf-16\"?>\r\n");
        xml.append("<CalculationOptionsWarlock xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n");
        element(xml, "Pet", pet);
        element(xml, "Imbue", imbue);
        element(xml, "UseInfernal", Boolean.toString(useInfernal));
        element(xml, "TargetLevel", Integer.toString(targetLevel));
        element(xml, "Duration", NetFormat.floatR(duration));
        element(xml, "Latency", NetFormat.floatR(latency));
        element(xml, "ThirtyFive", NetFormat.floatR(thirtyFive));
        element(xml, "TwentyFive", NetFormat.floatR(twentyFive));
        if (rotations == null || rotations.isEmpty()) {
            xml.append("  <Rotations />\r\n");
        } else {
            xml.append("  <Rotations>\r\n");
            for (Rotation rotation : rotations) {
                xml.append("    <Rotation>\r\n");
                element(xml, "      ", "Name", rotation.name);
                element(xml, "      ", "Filler", rotation.filler);
                if (rotation.execute != null) {
                    element(xml, "      ", "Execute", rotation.execute);
                }
                if (rotation.spellPriority == null || rotation.spellPriority.isEmpty()) {
                    xml.append("      <SpellPriority />\r\n");
                } else {
                    xml.append("      <SpellPriority>\r\n");
                    for (String spell : rotation.spellPriority) {
                        xml.append("        <string>").append(spell).append("</string>\r\n");
                    }
                    xml.append("      </SpellPriority>\r\n");
                }
                xml.append("    </Rotation>\r\n");
            }
            xml.append("  </Rotations>\r\n");
        }
        element(xml, "ActiveRotationIndex", Integer.toString(activeRotationIndex));
        element(xml, "PerSP", NetFormat.floatR(perSP));
        element(xml, "ConvertTotem", Boolean.toString(convertTotem));
        element(xml, "PerFlametongue", NetFormat.floatR(perFlametongue));
        element(xml, "PerMagicBuff", NetFormat.floatR(perMagicBuff));
        element(xml, "PerCritBuff", NetFormat.floatR(perCritBuff));
        element(xml, "PerInt", NetFormat.floatR(perInt));
        element(xml, "PerSpi", NetFormat.floatR(perSpi));
        element(xml, "PerHealth", NetFormat.floatR(perHealth));
        element(xml, "NoProcs", Boolean.toString(noProcs));
        xml.append("</CalculationOptionsWarlock>");
        return xml.toString();
    }

    private static void element(StringBuilder xml, String name, String value) {
        element(xml, "  ", name, value);
    }

    private static void element(StringBuilder xml, String indent, String name, String value) {
        xml.append(indent).append('<').append(name).append('>').append(escape(value))
                .append("</").append(name).append(">\r\n");
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
