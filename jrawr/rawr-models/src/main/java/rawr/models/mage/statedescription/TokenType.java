// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/StateDescriptionScanner.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage.statedescription;

public enum TokenType {

    //Non terminal tokens:
    _NONE_,
    _UNDETERMINED_,

    //Non terminal tokens:
    Start,
    UnionExpr,
    DiffExpr,
    IntExpr,
    CompExpr,
    Atom,

    //Terminal tokens:
    STATE,
    INTERSECTION,
    UNION,
    DIFFERENCE,
    COMPLEMENT,
    BROPEN,
    BRCLOSE,
    EOF
}
