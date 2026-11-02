// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Moonkin/SpellRotation.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.moonkin;

public class RotationData {
    public float burstDPS = 0.0f;
    public float dps = 0.0f;
    public float dpm = 0.0f;
    public float manaUsed = 0.0f;
    public float manaGained = 0.0f;
    // C# TimeToOOM is a System.TimeSpan built as new TimeSpan(0, (int)(timeToOOM / 60), (int)(timeToOOM % 60)).
    // It is only used for the "Selected/Burst Time To OOM" display, so the port keeps the normalized
    // total seconds (same value the TimeSpan holds) and derives Minutes/Seconds like TimeSpan does.
    public int timeToOOM = 0;

    public int timeToOOMMinutes() {
        return (timeToOOM / 60) % 60;
    }

    public int timeToOOMSeconds() {
        return timeToOOM % 60;
    }
}
