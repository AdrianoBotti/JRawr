// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/StateDescriptionParseTree.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
package rawr.models.mage.statedescription;

import rawr.models.mage.StandardEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ParseNode {
    protected String text;
    public List<ParseNode> nodes;

    public ParseNode parent;
    public Token token; // the token/rule

    public String getText() {
        return text;
    }

    public void setText(String value) {
        text = value;
    }

    public ParseNode createNode(Token token, String text) {
        ParseNode node = new ParseNode(token, text);
        node.parent = this;
        return node;
    }

    protected ParseNode(Token token, String text) {
        this.token = token;
        this.text = text;
        this.nodes = new ArrayList<ParseNode>();
    }

    public StateDescriptionDelegate compile(ParseTree tree) {
        StateDescriptionDelegate Value = null;

        switch (token.type) {
            case Start:
                Value = EvalStart(tree);
                break;
            case UnionExpr:
                Value = EvalUnionExpr(tree);
                break;
            case DiffExpr:
                Value = EvalDiffExpr(tree);
                break;
            case IntExpr:
                Value = EvalIntExpr(tree);
                break;
            case CompExpr:
                Value = EvalCompExpr(tree);
                break;
            case Atom:
                Value = EvalAtom(tree);
                break;
            default:
                Value = effects -> true;
                break;
        }
        return Value;
    }

    public boolean interpret(ParseTree tree, int effects) {
        boolean Value = true;

        switch (token.type) {
            case Start:
                Value = EvalStart(tree, effects);
                break;
            case UnionExpr:
                Value = EvalUnionExpr(tree, effects);
                break;
            case DiffExpr:
                Value = EvalDiffExpr(tree, effects);
                break;
            case IntExpr:
                Value = EvalIntExpr(tree, effects);
                break;
            case CompExpr:
                Value = EvalCompExpr(tree, effects);
                break;
            case Atom:
                Value = EvalAtom(tree, effects);
                break;
            default:
                Value = true;
                break;
        }
        return Value;
    }

    protected boolean EvalStart(ParseTree tree, int effects) {
        for (ParseNode node : nodes) {
            if (node.token.type == TokenType.UnionExpr) {
                return node.interpret(tree, effects);
            }
        }
        return true;
    }

    protected boolean EvalUnionExpr(ParseTree tree, int effects) {
        for (ParseNode node : nodes) {
            if (node.token.type == TokenType.DiffExpr) {
                boolean value = node.interpret(tree, effects);
                if (value) return true;
            }
        }
        return false;
    }

    protected boolean EvalDiffExpr(ParseTree tree, int effects) {
        int i = 0;
        for (; i < nodes.size(); i++) {
            ParseNode node = nodes.get(i);
            if (node.token.type == TokenType.IntExpr) {
                boolean value = node.interpret(tree, effects);
                if (!value) return false;
                i++;
                break;
            }
        }
        for (; i < nodes.size(); i++) {
            ParseNode node = nodes.get(i);
            if (node.token.type == TokenType.IntExpr) {
                return !node.interpret(tree, effects);
            }
        }
        return true;
    }

    protected boolean EvalIntExpr(ParseTree tree, int effects) {
        for (ParseNode node : nodes) {
            if (node.token.type == TokenType.CompExpr) {
                boolean value = node.interpret(tree, effects);
                if (!value) return false;
            }
        }
        return true;
    }

    protected boolean EvalCompExpr(ParseTree tree, int effects) {
        boolean complement = false;
        for (int i = 0; i < nodes.size(); i++) {
            ParseNode node = nodes.get(i);
            if (node.token.type == TokenType.COMPLEMENT) {
                complement = true;
            } else if (node.token.type == TokenType.Atom) {
                return node.interpret(tree, effects) ^ complement;
            }
        }
        return true;
    }

    private int EvalState(ParseNode node) {
        String text = node.token.getText().replace(" ", "");
        if (text.toUpperCase(Locale.ROOT).equals("ANY")) {
            return 0;
        } else {
            return parseStandardEffect(text);
        }
    }

    // C# Enum.Parse(typeof(StandardEffect), text, true)
    private static int parseStandardEffect(String text) {
        switch (text.toUpperCase(Locale.ROOT)) {
            case "NONE": return 0;
            case "WATERELEMENTAL": return StandardEffect.WaterElemental;
            case "POWERINFUSION": return StandardEffect.PowerInfusion;
            case "POTIONOFSPEED": return StandardEffect.PotionOfSpeed;
            case "ARCANEPOWER": return StandardEffect.ArcanePower;
            case "COMBUSTION": return StandardEffect.Combustion;
            case "POTIONOFWILDMAGIC": return StandardEffect.PotionOfWildMagic;
            case "BERSERKING": return StandardEffect.Berserking;
            case "FLAMECAP": return StandardEffect.FlameCap;
            case "HEROISM": return StandardEffect.Heroism;
            case "ICYVEINS": return StandardEffect.IcyVeins;
            case "MOLTENFURY": return StandardEffect.MoltenFury;
            case "EVOCATION": return StandardEffect.Evocation;
            case "MANAGEMEFFECT": return StandardEffect.ManaGemEffect;
            case "MIRRORIMAGE": return StandardEffect.MirrorImage;
            default: throw new RuntimeException("Invalid StandardEffect: " + text);
        }
    }

    protected boolean EvalAtom(ParseTree tree, int effects) {
        for (ParseNode node : nodes) {
            if (node.token.type == TokenType.UnionExpr) {
                return node.interpret(tree, effects);
            } else if (node.token.type == TokenType.STATE) {
                int stateEffects = EvalState(node);
                return (effects & stateEffects) == stateEffects;
            }
        }
        return true;
    }

    protected StateDescriptionDelegate EvalStart(ParseTree tree) {
        for (ParseNode node : nodes) {
            if (node.token.type == TokenType.UnionExpr) {
                return node.compile(tree);
            }
        }
        return effects -> true;
    }

    protected StateDescriptionDelegate EvalUnionExpr(ParseTree tree) {
        List<StateDescriptionDelegate> nodelist = new ArrayList<StateDescriptionDelegate>();
        for (ParseNode node : nodes) {
            if (node.token.type == TokenType.DiffExpr) {
                nodelist.add(node.compile(tree));
            }
        }
        StateDescriptionDelegate[] list = nodelist.toArray(new StateDescriptionDelegate[0]);
        if (list.length == 1) return list[0];
        return effects -> {
            for (int i = 0; i < list.length; i++) {
                if (list[i].test(effects)) return true;
            }
            return false;
        };
    }

    protected StateDescriptionDelegate EvalDiffExpr(ParseTree tree) {
        StateDescriptionDelegate a = null;
        StateDescriptionDelegate b = null;
        int i = 0;
        for (; i < nodes.size(); i++) {
            ParseNode node = nodes.get(i);
            if (node.token.type == TokenType.IntExpr) {
                a = node.compile(tree);
                i++;
                break;
            }
        }
        for (; i < nodes.size(); i++) {
            ParseNode node = nodes.get(i);
            if (node.token.type == TokenType.IntExpr) {
                b = node.compile(tree);
                break;
            }
        }
        if (b != null) {
            StateDescriptionDelegate aa = a;
            StateDescriptionDelegate bb = b;
            return effects -> aa.test(effects) && !bb.test(effects);
        } else {
            return a;
        }
    }

    protected StateDescriptionDelegate EvalIntExpr(ParseTree tree) {
        boolean allAtom = true;
        List<StateDescriptionDelegate> nodelist = new ArrayList<StateDescriptionDelegate>();
        int stateEffects = 0;
        for (ParseNode node : nodes) {
            if (node.token.type == TokenType.CompExpr) {
                if (node.nodes.get(0).token.type == TokenType.Atom && node.nodes.get(0).nodes.get(0).token.type == TokenType.STATE) {
                    stateEffects = stateEffects | EvalState(node.nodes.get(0).nodes.get(0));
                } else {
                    allAtom = false;
                }
                nodelist.add(node.compile(tree));
            }
        }
        if (allAtom) {
            int se = stateEffects;
            return effects -> (effects & se) == se;
        } else {
            StateDescriptionDelegate[] list = nodelist.toArray(new StateDescriptionDelegate[0]);
            if (list.length == 1) return list[0];
            return effects -> {
                for (int i = 0; i < list.length; i++) {
                    if (!list[i].test(effects)) return false;
                }
                return true;
            };
        }
    }

    protected StateDescriptionDelegate EvalCompExpr(ParseTree tree) {
        boolean complement = false;
        for (int i = 0; i < nodes.size(); i++) {
            ParseNode node = nodes.get(i);
            if (node.token.type == TokenType.COMPLEMENT) {
                complement = true;
            } else if (node.token.type == TokenType.Atom) {
                StateDescriptionDelegate value = node.compile(tree);
                if (complement) {
                    return effects -> !value.test(effects);
                } else {
                    return value;
                }
            }
        }
        return null;
    }

    protected StateDescriptionDelegate EvalAtom(ParseTree tree) {
        for (ParseNode node : nodes) {
            if (node.token.type == TokenType.UnionExpr) {
                return node.compile(tree);
            } else if (node.token.type == TokenType.STATE) {
                int stateEffects = EvalState(node);
                return effects -> (effects & stateEffects) == stateEffects;
            }
        }
        return null;
    }
}
