// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Hunter/Pet/PetSkill.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// FRAMEWORK ADAPTATION: the C# Debug.WriteLine debug block in PetSkillPriorityRotation.calculateDPS
// is omitted (debug output only).
package rawr.models.hunter;

import rawr.base.character.Character;
import rawr.models.hunter.HunterEnums.PetAttacks;
import rawr.models.hunter.HunterEnums.PetSkillType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PetSkill {
    public boolean canCrit;
    public float cooldown;
    public int focus;
    public PetSkillType type;
    public float average;

    public PetSkill(boolean canCrit, float cooldown, int focus, PetSkillType type) {
        this.canCrit = canCrit;
        this.cooldown = cooldown;
        this.focus = focus;
        this.type = type;
        this.average = 0;
    }

    public PetSkill(boolean canCrit, float cooldown, int focus, PetSkillType type, int min, int max) {
        this.canCrit = canCrit;
        this.cooldown = cooldown;
        this.focus = focus;
        this.type = type;
        this.average = (min + max) / 2;
    }
}
