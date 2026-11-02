// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/ArrayPool.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The C# desktop class is `unsafe`, but its only pointer member is `double*[] this.pD`; everything
// else is already managed arrays. this.pD is ported as the safe ArrayOffset<double> variant
// (ArrayOffsetDouble), same storage walk as the raw pointers (DECISIONS #4).
package rawr.models.mage;

import rawr.base.stats.Stats;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

public class ArraySet {
    public double[] sparseMatrixValue;
    public int[] sparseMatrixRow;
    public int[] sparseMatrixCol;
    public double[] sparseMatrixData; // still store the dense version, memory is cheap and it speeds some stuff up
    public int sparseMatrixMaxRows = 0;
    public int sparseMatrixMaxCols = 0;
    public int sparseMatrixSparseSize;

    public int lUetaMax;
    public int lUsizeMax;
    public int[] LU_P;
    public int[] LU_Q;
    public double[] lUsparseL; // packed non-zero elements of eta vectors
    public int[] lUsparseLI; // indices of non-zero elements
    public int[] lUsparseLstart; // start index of non-zero elements of eta vector i
    public int[] LU_LJ; // col/row eta index   Li = I + L[i*size:(i+1)*size-1] * e_LI[i]'
    public double[] lUcolumn;
    public double[] lUcolumn2;
    public double[] LU_U;

    public Map<String, Integer> extraConstraintMap = new LinkedHashMap<String, Integer>();
    public int[] extraReferenceCount;
    public double[] extraConstraints;
    public double[] d;
    public double[] x;
    public double[] w;
    public double[] ww;
    public double[] wd;
    public double[] qx;
    public double[] qv;
    public double[] vd;
    public double[] u;
    public double[] c;
    // C# desktop: double*[] (raw pointers into extraConstraints); index-based port, see header.
    public ArrayOffsetDouble[] pD;
    //internal static double[] _b;
    public double[] cost;
    public double[] spellDps;
    public double[] costWorking;
    //private static double[] _beta;
    //private static double[] _betaBackup;

    public int[] orderedSuperbasis;
    public int[] flags;
    public double[] lb;
    public double[] ub;
    public double[] mb;

    public double[] cg_p;
    public double[] cg_x;
    public double[] cg_r;
    public double[] cg_w;
    public double[] cg_rr;
    public double[] cg_ww;
    public double[] cg_qp;

    public int maxRows = 0;
    public int maxCols = 0;
    public int maxExtra = 0;
    public int maxExtraRows = 0;

    public int maxSolverRows = 0;
    public int maxSolverCols = 0;
    public double[] rowScale;
    public double[] columnScale;

    public Stats accumulator;

    public Spell[] spellPool = new Spell[64];
    public int spellIndex;

    public Spell newSpell() {
        if (spellIndex < spellPool.length) {
            Spell spell = spellPool[spellIndex];
            if (spell != null) {
                spellIndex++;
                return spell;
            }
        }
        return newSpellCold();
    }

    private Spell newSpellCold() {
        /*if (spellPool == null)
        {
            spellPool = new Spell[64];
        }
        else */if (spellIndex >= spellPool.length) {
            Spell[] arr = new Spell[spellPool.length * 2];
            System.arraycopy(spellPool, 0, arr, 0, spellPool.length);
            spellPool = arr;
        }
        Spell spell = spellPool[spellIndex];
        if (spell == null) {
            spell = new Spell();
            spellPool[spellIndex] = spell;
        }
        spellIndex++;
        return spell;
    }

    public Cycle[] cyclePool = new Cycle[64];
    public int cycleIndex;

    public Cycle newCycle() {
        if (cycleIndex < cyclePool.length) {
            Cycle cycle = cyclePool[cycleIndex];
            if (cycle != null) {
                cycleIndex++;
                return cycle;
            }
        }
        return newCycleCold();
    }

    private Cycle newCycleCold() {
        /*if (cyclePool == null)
        {
            cyclePool = new Cycle[64];
        }
        else */if (cycleIndex >= cyclePool.length) {
            Cycle[] arr = new Cycle[cyclePool.length * 2];
            System.arraycopy(cyclePool, 0, arr, 0, cyclePool.length);
            cyclePool = arr;
        }
        Cycle cycle = cyclePool[cycleIndex];
        if (cycle == null) {
            cycle = new Cycle();
            cyclePool[cycleIndex] = cycle;
        }
        cycleIndex++;
        return cycle;
    }

    public CastingState[] statePool = new CastingState[32];
    public int stateIndex;

    public CastingState newCastingState() {
        if (stateIndex < statePool.length) {
            CastingState state = statePool[stateIndex];
            if (state != null) {
                stateIndex++;
                return state;
            }
        }
        return newCastingStateCold();
    }

    private CastingState newCastingStateCold() {
        /*if (statePool == null)
        {
            statePool = new CastingState[32];
        }
        else */if (stateIndex >= statePool.length) {
            CastingState[] arr = new CastingState[statePool.length * 2];
            System.arraycopy(statePool, 0, arr, 0, statePool.length);
            statePool = arr;
        }
        CastingState state = statePool[stateIndex];
        if (state == null) {
            state = new CastingState();
            statePool[stateIndex] = state;
        }
        stateIndex++;
        return state;
    }

    public EffectCooldown[] effectPool = new EffectCooldown[32];
    public int effectIndex;

    public EffectCooldown newEffectCooldown() {
        if (effectIndex < effectPool.length) {
            EffectCooldown effect = effectPool[effectIndex];
            if (effect != null) {
                effectIndex++;
                return effect;
            }
        }
        return newEffectCooldownCold();
    }

    private EffectCooldown newEffectCooldownCold() {
        if (effectIndex >= effectPool.length) {
            EffectCooldown[] arr = new EffectCooldown[effectPool.length * 2];
            System.arraycopy(effectPool, 0, arr, 0, effectPool.length);
            effectPool = arr;
        }
        EffectCooldown effect = effectPool[effectIndex];
        if (effect == null) {
            effect = new EffectCooldown();
            effectPool[effectIndex] = effect;
        }
        effectIndex++;
        return effect;
    }

    public int getMaxSize() {
        return this.sparseMatrixMaxRows * this.sparseMatrixMaxCols;
    }

    public ArraySet() {
        this.sparseMatrixMaxRows = 200;
        this.sparseMatrixMaxCols = 200;
        recreateSparseMatrixArrays();

        this.lUsizeMax = 200;
        recreateLUArrays();

        maxRows = 200;
        maxCols = 200;
        recreateLPArrays();
        maxExtraRows = 32;
        extraConstraints = new double[maxExtraRows * maxCols];
        extraReferenceCount = new int[maxExtraRows];

        maxSolverRows = 200;
        maxSolverCols = 200;
        recreateSolverArrays();
    }

    public void recreateSparseMatrixArrays() {
        this.sparseMatrixSparseSize = Math.max(this.sparseMatrixSparseSize, (int) (this.sparseMatrixMaxRows * this.sparseMatrixMaxCols * 0.4));
        this.sparseMatrixValue = new double[this.sparseMatrixSparseSize];
        this.sparseMatrixRow = new int[this.sparseMatrixSparseSize];
        this.sparseMatrixCol = new int[this.sparseMatrixMaxCols + 1];
        this.sparseMatrixData = new double[this.sparseMatrixMaxRows * this.sparseMatrixMaxCols];
    }

    public void recreateLUArrays() {
        try {
            LU_U = new double[this.lUsizeMax * this.lUsizeMax];
            this.lUetaMax = Math.max(this.lUsizeMax + 100, 2 * this.lUsizeMax);
            LU_P = new int[this.lUsizeMax];
            LU_Q = new int[this.lUsizeMax];
            //_L = new double[etaMax * size];
            LU_LJ = new int[this.lUetaMax];
            this.lUsparseL = new double[this.lUetaMax * this.lUsizeMax];
            this.lUsparseLI = new int[this.lUetaMax * this.lUsizeMax];
            this.lUsparseLstart = new int[this.lUetaMax];
            this.lUcolumn = new double[this.lUsizeMax];
            this.lUcolumn2 = new double[this.lUsizeMax];
        } catch (OutOfMemoryError ex) {
            // C# catches OutOfMemoryException; Java throws OutOfMemoryError.
            this.lUsizeMax = 0;
            this.lUetaMax = 0;
            throw ex;
        }
    }

    public void recreateLPArrays() {
        this.d = new double[maxRows];
        this.x = new double[maxRows];
        this.w = new double[maxRows];
        this.ww = new double[maxRows];
        this.wd = new double[maxCols];
        this.qx = new double[maxCols];
        this.qv = new double[maxCols];
        this.vd = new double[maxCols];
        this.u = new double[maxRows];
        this.c = new double[maxCols];
        this.spellDps = new double[maxCols];
        this.cost = new double[maxCols + maxRows];
        this.costWorking = new double[maxCols + maxRows];
        this.orderedSuperbasis = new int[maxCols + maxRows];
        //_b = new double[maxRows];
        //_beta = new double[maxRows];
        //_betaBackup = new double[maxRows];
    }

    public void recreateCGArrays() {
        cg_p = new double[maxRows + maxCols];
        cg_r = new double[maxRows + maxCols];
        cg_x = new double[maxRows + maxCols];
        cg_w = new double[maxRows + maxCols];
        cg_rr = new double[maxRows + maxCols];
        cg_ww = new double[maxRows + maxCols];
        cg_qp = new double[maxRows + maxCols];
    }

    public void extendLPArrays() {
        this.d = new double[maxRows];
        this.x = new double[maxRows];
        this.w = new double[maxRows];
        this.ww = new double[maxRows];
        this.qx = new double[maxCols];
        this.qv = new double[maxCols];
        this.vd = new double[maxCols];
        this.u = new double[maxRows];
        this.c = new double[maxCols];
        this.orderedSuperbasis = new int[maxCols + maxRows];
        double[] tmp = new double[maxCols + maxRows];
        System.arraycopy(this.cost, 0, tmp, 0, this.cost.length);
        this.cost = tmp;
        tmp = new double[maxCols + maxRows];
        System.arraycopy(this.costWorking, 0, tmp, 0, this.costWorking.length);
        this.costWorking = tmp;
    }

    public void recreateSolverArrays() {
        rowScale = new double[maxSolverRows];
        columnScale = new double[maxSolverCols];
    }

    public int getConstraint(String name, int cols, boolean[] newConstraint) {
        int index;
        Integer mapped = (name == null) ? null : extraConstraintMap.get(name);
        if (mapped == null) {
            // first check if we have any free constraints
            for (index = 0; index < maxExtraRows; index++) {
                if (extraReferenceCount[index] == 0) {
                    // if it was used by a named constraint we have to invalidate it
                    String key = null;
                    for (Map.Entry<String, Integer> kvp : extraConstraintMap.entrySet()) {
                        if (kvp.getValue() == index) {
                            key = kvp.getKey();
                            break;
                        }
                    }
                    if (key != null) {
                        extraConstraintMap.remove(key);
                    }
                    // clean it
                    Arrays.fill(extraConstraints, cols * index, cols * index + cols, 0.0);
                    break;
                }
            }
            if (index == maxExtraRows) {
                maxExtraRows += 32;
                if (extraConstraints.length < cols * maxExtraRows) {
                    double[] newArray = new double[cols * maxExtraRows];
                    if (extraConstraints != null) System.arraycopy(extraConstraints, 0, newArray, 0, extraConstraints.length);
                    extraConstraints = newArray;
                }
                int[] newRefCount = new int[maxExtraRows];
                if (extraReferenceCount != null) System.arraycopy(extraReferenceCount, 0, newRefCount, 0, extraReferenceCount.length);
                extraReferenceCount = newRefCount;
            }
            if (name != null) extraConstraintMap.put(name, index);
            newConstraint[0] = true;
        } else {
            index = mapped;
            newConstraint[0] = false;
        }
        extraReferenceCount[index]++;
        return index;
    }
}
