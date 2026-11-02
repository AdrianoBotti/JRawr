// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/LP.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The C# desktop class is `unsafe` with double*/int* fields and pointer walks; ported as
// index-based arrays plus int cursors (DECISIONS #4). The C# field `super` is renamed
// `superCount` because `super` is a Java keyword; it is private so no external contract changes.
// Desktop branch semantics are used wherever the SILVERLIGHT and desktop branches differ.
package rawr.models.mage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LP {
    SparseMatrix A;
    private int[] extraConstraintsUsed;
    private int numExtraConstraints;
    private int baseRows;
    private int rows;
    int cols;
    private int superCount;

    private int[] extraBasis;
    private int[] extraNonbasic;
    private int[] extraSlack;
    private LU lu;

    ArraySet arraySet;

    private boolean costWorkingDirty;
    private boolean shifted;
    private boolean osbDirty; // true if superbasis changed since last ordered superbasis update
    private int osbStructural; // number of structural variables in superbasis

    private int[] extraFlags;
    double[] extraLowerBounds;
    double[] extraUpperBounds;
    double[] extraMiddleBounds;

    private static final int flagNLB = 0x1; // variable nonbasic at lower bound
    private static final int flagNUB = 0x2; // variable nonbasic at upper bound
    private static final int flagNMid = 0x400; // variable nonbasic in middle (quadratic program only)
    private static final int flagN = flagNLB | flagNUB | flagNMid; // variable nonbasic
    private static final int flagB = 0x4; // variable basic
    private static final int flagDis = 0x8; // variable blacklisted
    private static final int flagFix = 0x10; // variable fixed
    private static final int flagLB = 0x20; // variable has finite lower bound
    private static final int flagUB = 0x40; // variable has finite upper bound
    private static final int flagPivot = 0x80; // variable is eligible for pivot
    private static final int flagFlip = 0x100; // variable bounds must be flipped
    private static final int flagPivot2 = 0x200; // variable is in reduced pivot candidate set
    private static final int flagSB = flagB | flagNMid;

    private boolean disabledDirty;

    // quadratic parameters
    private int mpsRow;
    private int[] sort;
    //private int[] sortinv;
    private double qk;

    // C# desktop: double*; C# SILVERLIGHT: double[]. Ported as double[] (DECISIONS #4).
    private double[] a;
    private double[] U;
    private double[] d;
    private double[] x;
    private double[] w;
    private double[] ww;
    private double[] wd;
    private double[] qx;
    private double[] qv;
    private double[] vd;
    private double[] c;
    private double[] u;
    private double[] cost;
    private double[] spellDps;
    private double[] sparseValue;
    private double[] D;
    // C# desktop: double** (into arraySet.extraConstraints); index-based port via ArrayOffsetDouble.
    private ArrayOffsetDouble[] pD;
    private int[] B;
    private int[] V;
    private int[] S;
    private int[] OSB;
    private int[] sparseRow;
    private int[] sparseCol;
    private int[] flags;
    private double[] lb;
    private double[] ub;
    private double[] mb;

    private double[] cg_p;
    private double[] cg_x;
    private double[] cg_w;
    private double[] cg_r;
    private double[] cg_ww;
    private double[] cg_rr;
    private double[] cg_qp;

    private static final double epsPrimal = 1.0e-7;
    private static final double epsPrimalLow = 1.0e-6;
    private static final double epsPrimalRel = 1.0e-9;
    private static final double epsDual = 1.0e-7;
    private static final double epsDualI = 1.0e-8;
    private static final double epsPivot = 1.0e-5;
    private static final double epsPivot2 = 1.0e-7;
    private static final double epsZero = 1.0e-12;
    private static final double epsDrop = 1.0e-14;
    private static final double epsCG = 1.0e-5;

    public static void copy(double[] dest, double[] source, int size) {
        System.arraycopy(source, 0, dest, 0, size);
    }

    public static void zero(double[] array, int size) {
        Arrays.fill(array, 0, size, 0.0);
    }

    private static int indexOf(int[] array, int value) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == value) return i;
        }
        return -1;
    }

    private static int indexOf(int[] array, int value, int start) {
        for (int i = start; i < array.length; i++) {
            if (array[i] == value) return i;
        }
        return -1;
    }

    public LP clone() {
        LP clone = new LP();
        // MemberwiseClone
        clone.A = A;
        clone.extraConstraintsUsed = extraConstraintsUsed;
        clone.numExtraConstraints = numExtraConstraints;
        clone.baseRows = baseRows;
        clone.rows = rows;
        clone.cols = cols;
        clone.superCount = superCount;
        clone.extraBasis = this.extraBasis;
        clone.extraNonbasic = this.extraNonbasic;
        clone.extraSlack = this.extraSlack;
        clone.lu = lu;
        clone.arraySet = arraySet;
        clone.costWorkingDirty = costWorkingDirty;
        clone.shifted = shifted;
        clone.osbDirty = osbDirty;
        clone.osbStructural = osbStructural;
        clone.extraFlags = this.extraFlags;
        clone.extraLowerBounds = this.extraLowerBounds;
        clone.extraUpperBounds = this.extraUpperBounds;
        clone.extraMiddleBounds = this.extraMiddleBounds;
        clone.disabledDirty = disabledDirty;
        clone.mpsRow = mpsRow;
        clone.sort = sort;
        clone.qk = qk;
        clone.a = a;
        clone.U = U;
        clone.d = d;
        clone.x = x;
        clone.w = w;
        clone.ww = ww;
        clone.wd = wd;
        clone.qx = qx;
        clone.qv = qv;
        clone.vd = vd;
        clone.c = c;
        clone.u = u;
        clone.cost = cost;
        clone.spellDps = spellDps;
        clone.sparseValue = sparseValue;
        clone.D = D;
        clone.pD = pD;
        clone.B = B;
        clone.V = V;
        clone.S = S;
        clone.OSB = OSB;
        clone.sparseRow = sparseRow;
        clone.sparseCol = sparseCol;
        clone.flags = flags;
        clone.lb = lb;
        clone.ub = ub;
        clone.mb = mb;
        clone.cg_p = cg_p;
        clone.cg_x = cg_x;
        clone.cg_w = cg_w;
        clone.cg_r = cg_r;
        clone.cg_ww = cg_ww;
        clone.cg_rr = cg_rr;
        clone.cg_qp = cg_qp;
        clone.maxSize = maxSize;
        clone.constructed = constructed;
        clone.coldStart = coldStart;
        clone.hardInfeasibility = hardInfeasibility;

        clone.extraBasis = this.extraBasis.clone();
        clone.extraNonbasic = this.extraNonbasic.clone();
        if (this.extraSlack != null) {
            clone.extraSlack = this.extraSlack.clone();
        }
        clone.extraFlags = this.extraFlags.clone();
        clone.extraLowerBounds = this.extraLowerBounds.clone();
        clone.extraUpperBounds = this.extraUpperBounds.clone();
        if (this.extraMiddleBounds != null) {
            clone.extraMiddleBounds = this.extraMiddleBounds.clone();
        }
        if (numExtraConstraints > 0) {
            clone.extraConstraintsUsed = extraConstraintsUsed.clone();
            // increase reference count
            for (int i = 0; i < numExtraConstraints; i++) {
                arraySet.extraReferenceCount[extraConstraintsUsed[i]]++;
            }
        }
        return clone;
    }

    public double get(int row, int col) {
        if (row > baseRows) throw new RuntimeException();
        else if (row == baseRows) return arraySet.cost[col];
        else if (col == cols) return -this.extraLowerBounds[cols + row];
        else return A.get(row, col);
    }

    public void set(int row, int col, double value) {
        A.set(row, col, value);
    }

    public void setCost(int col, double value) {
        arraySet.cost[col] = value;
    }

    public void setSpellDps(int col, double value) {
        arraySet.spellDps[col] = value;
    }

    private boolean hardInfeasibility = false;

    public boolean updateLowerBound(int col, double value) {
        if (value > this.extraLowerBounds[col] + epsZero) {
            setLowerBound(col, value);
            return true;
        } else {
            return false;
        }
    }

    public boolean updateUpperBound(int col, double value) {
        if (value < this.extraUpperBounds[col] - epsZero) {
            setUpperBound(col, value);
            return true;
        } else {
            return false;
        }
    }

    public double getLowerBound(int col) {
        return this.extraLowerBounds[col];
    }

    public void setLowerBound(int col, double value) {
        this.extraLowerBounds[col] = value;
        if (Double.isInfinite(value) && value < 0) {
            this.extraFlags[col] &= ~flagLB;
        } else {
            this.extraFlags[col] |= flagLB;
        }
        if (this.extraUpperBounds[col] - this.extraLowerBounds[col] < epsZero) {
            this.extraFlags[col] |= flagFix;
        } else {
            this.extraFlags[col] &= ~flagFix;
        }
        if (this.extraUpperBounds[col] < this.extraLowerBounds[col] - epsZero) hardInfeasibility = true;
    }

    public void setUpperBound(int col, double value) {
        this.extraUpperBounds[col] = value;
        if (Double.isInfinite(value) && value > 0) {
            this.extraFlags[col] &= ~flagUB;
        } else {
            this.extraFlags[col] |= flagUB;
        }
        if (this.extraUpperBounds[col] - this.extraLowerBounds[col] < epsZero) {
            this.extraFlags[col] |= flagFix;
        } else {
            this.extraFlags[col] &= ~flagFix;
        }
        if (this.extraUpperBounds[col] < this.extraLowerBounds[col] - epsZero) hardInfeasibility = true;
    }

    public double getRHS(int row) {
        //_b[row] = value;
        return -getLowerBound(cols + row);
    }

    public void setRHS(int row, double value) {
        //_b[row] = value;
        setLowerBound(cols + row, -value);
    }

    public void setLHS(int row, double value) {
        setUpperBound(cols + row, -value);
    }

    public double getConstraintElement(int index, int col) {
        return arraySet.extraConstraints[extraConstraintsUsed[index] * cols + col];
    }

    public void setConstraintElement(int index, int col, double value) {
        arraySet.extraConstraints[extraConstraintsUsed[index] * cols + col] = value;
    }

    public double getConstraintRHS(int index) {
        return -this.extraLowerBounds[cols + baseRows + index];
    }

    public void setConstraintRHS(int index, double value) {
        setLowerBound(cols + baseRows + index, -value);
    }

    public double getConstraintLHS(int index) {
        return -this.extraUpperBounds[cols + baseRows + index];
    }

    public void setConstraintLHS(int index, double value) {
        setUpperBound(cols + baseRows + index, -value);
    }

    public int getExtraConstraintCount() {
        return numExtraConstraints;
    }

    public int addColumn() {
        if (cols + rows + 100 >= maxSize) {
            maxSize = 2 * maxSize;
            extendInstanceArrays();
        }
        arraySet.cost[cols] = 0.0;
        this.extraLowerBounds[cols] = 0.0; // we can reuse dirty arrays, initialize only when needed
        this.extraUpperBounds[cols] = Double.POSITIVE_INFINITY;
        arraySet.spellDps[cols] = 0.0;
        this.extraFlags[cols] = flagNLB | flagLB;
        cols++;
        return A.addColumn();
    }

    private int maxSize;

    public void releaseConstraints() {
        for (int i = 0; i < numExtraConstraints; i++) {
            arraySet.extraReferenceCount[extraConstraintsUsed[i]]--;
        }
        numExtraConstraints = 0;
    }

    // should only add extra constraints that are tight when primal feasible
    public int addConstraint(String name, boolean[] newConstraint) {
        if (cols + rows >= maxSize) {
            maxSize += 100;
            extendInstanceArrays();
        }
        numExtraConstraints++;
        rows++;
        if (rows > arraySet.maxRows) {
            arraySet.maxRows = rows + 10;
            arraySet.extendLPArrays();
        }
        if (numExtraConstraints > arraySet.maxExtra) {
            arraySet.maxExtra += 32;
            arraySet.pD = new ArrayOffsetDouble[arraySet.maxExtra];
        }
        if (extraConstraintsUsed == null) extraConstraintsUsed = new int[32];
        if (numExtraConstraints > extraConstraintsUsed.length) {
            int[] newArray = new int[extraConstraintsUsed.length + 32];
            if (extraConstraintsUsed != null) System.arraycopy(extraConstraintsUsed, 0, newArray, 0, extraConstraintsUsed.length);
            extraConstraintsUsed = newArray;
        }
        int index = arraySet.getConstraint(name, cols, newConstraint);
        extraConstraintsUsed[numExtraConstraints - 1] = index;
        this.extraLowerBounds[cols + rows - 1] = 0.0; // don't forget to clear
        this.extraUpperBounds[cols + rows - 1] = Double.POSITIVE_INFINITY;
        this.extraFlags[cols + rows - 1] = flagLB | flagB;
        arraySet.cost[cols + rows - 1] = 0.0;
        int[] newB = new int[rows];
        System.arraycopy(this.extraBasis, 0, newB, 0, rows - 1);
        extraBasis = newB;
        //this.extraBasis[rows - 1] = cols + rows - 1;
        // extra constraints should be at start so they are not eliminated when patching singular basis
        this.extraBasis[rows - 1] = this.extraBasis[numExtraConstraints - 1];
        this.extraBasis[numExtraConstraints - 1] = cols + rows - 1;
        lu = new LU(rows, arraySet);
        return numExtraConstraints - 1;
    }

    private boolean constructed;

    public void endColumnConstruction() {
        if (constructed) return;
        if (cols + rows > maxSize + 100) {
            maxSize = cols + rows + 100;
            extendInstanceArrays();
        }
        A.endConstruction();
        extraBasis = new int[rows];
        extraNonbasic = new int[cols];
        for (int i = 0; i < rows; i++) {
            this.extraBasis[i] = cols + i;
            this.extraLowerBounds[cols + i] = 0.0; // don't forget to clear
            this.extraUpperBounds[cols + i] = Double.POSITIVE_INFINITY;
            this.extraFlags[cols + i] = flagB | flagLB;
            arraySet.cost[cols + i] = 0.0;
        }
        for (int j = 0; j < cols; j++) {
            this.extraNonbasic[j] = j;
        }
        if (arraySet.extraConstraints.length < arraySet.maxExtraRows * cols) {
            arraySet.extraConstraints = new double[arraySet.maxExtraRows * cols];
        }
        constructed = true;
    }

    private void extendInstanceArrays() {
        if (this.extraLowerBounds == null) {
            if (arraySet.lb != null && arraySet.lb.length >= maxSize) {
                extraLowerBounds = arraySet.lb;
                extraUpperBounds = arraySet.ub;
                extraMiddleBounds = arraySet.mb;
                extraFlags = arraySet.flags;
                // do not clear, initialize on each new column instead
                //Array.Clear(this.extraLowerBounds, 0, this.extraLowerBounds.Length);
                //Array.Clear(this.extraUpperBounds, 0, this.extraLowerBounds.Length);
                //Array.Clear(this.extraFlags, 0, this.extraLowerBounds.Length);
                maxSize = this.extraLowerBounds.length;
            } else {
                extraLowerBounds = new double[maxSize];
                extraUpperBounds = new double[maxSize];
                extraMiddleBounds = new double[maxSize];
                extraFlags = new int[maxSize];
                arraySet.lb = this.extraLowerBounds;
                arraySet.ub = this.extraUpperBounds;
                arraySet.mb = this.extraMiddleBounds;
                arraySet.flags = this.extraFlags;
            }
        } else {
            double[] newlb = new double[maxSize];
            System.arraycopy(this.extraLowerBounds, 0, newlb, 0, this.extraLowerBounds.length);
            double[] newub = new double[maxSize];
            System.arraycopy(this.extraUpperBounds, 0, newub, 0, this.extraUpperBounds.length);
            double[] newmb = new double[maxSize];
            System.arraycopy(this.extraMiddleBounds, 0, newmb, 0, this.extraMiddleBounds.length);
            int[] newflags = new int[maxSize];
            System.arraycopy(this.extraFlags, 0, newflags, 0, this.extraFlags.length);
            if (this.extraLowerBounds == arraySet.lb) {
                arraySet.lb = newlb;
                arraySet.ub = newub;
                arraySet.mb = newmb;
                arraySet.flags = newflags;
            }
            extraLowerBounds = newlb;
            extraUpperBounds = newub;
            extraMiddleBounds = newmb;
            extraFlags = newflags;
        }
    }

    public LP(int baseRows, int maxCols, ArraySet arraySet) {
        initialize(baseRows, maxCols, arraySet);
    }

    private LP() {
        // only used by clone(), mirroring MemberwiseClone semantics
    }

    public void initialize(int baseRows, int maxCols, ArraySet arraySet) {
        this.baseRows = baseRows;
        this.arraySet = arraySet;
        if (baseRows + 10 > arraySet.maxRows || maxCols + 10 > arraySet.maxCols) {
            arraySet.maxRows = Math.max(baseRows + 10, arraySet.maxRows);
            arraySet.maxCols = Math.max(maxCols + 10, arraySet.maxCols);
            arraySet.recreateLPArrays();
        }
        this.rows = baseRows;
        this.cols = 0;
        this.superCount = 0;
        maxSize = rows + 500;
        extraLowerBounds = null;
        extendInstanceArrays();
        if (arraySet.extraReferenceCount != null) Arrays.fill(arraySet.extraReferenceCount, 0, arraySet.extraReferenceCount.length, 0);
        arraySet.extraConstraintMap.clear();

        if (A == null) {
            A = new SparseMatrix(baseRows, maxCols, arraySet);
        } else {
            A.initialize(baseRows, maxCols, arraySet);
        }
        if (lu == null) {
            lu = new LU(rows, arraySet);
        } else {
            lu.initialize(rows, arraySet);
        }
        //extraConstraints = new double[cols + rows + 1];
        //extraConstraints[cols + rows - 1] = 1;
        //numExtraConstraints = 1;
        //Array.Clear(_b, 0, baseRows);

        coldStart = true;
        constructed = false;
        disabledDirty = false;
        numExtraConstraints = 0;
    }

    private boolean coldStart;

    private void setupExtraConstraints() {
        for (int i = 0; i < numExtraConstraints; i++) {
            pD[i] = new ArrayOffsetDouble(D, extraConstraintsUsed[i] * cols);
        }
    }

    public double[] solvePrimal(boolean prepareForDual) {
        if (hardInfeasibility) return new double[cols + 1];
        double[] ret = null;

        this.a = arraySet.sparseMatrixData;
        this.U = arraySet.LU_U;
        this.d = arraySet.d;
        this.x = arraySet.x;
        this.w = arraySet.w;
        this.ww = arraySet.ww;
        this.wd = arraySet.wd;
        this.c = arraySet.c;
        this.u = arraySet.u;
        this.cost = arraySet.cost;
        this.sparseValue = arraySet.sparseMatrixValue;
        this.D = arraySet.extraConstraints;
        this.B = this.extraBasis;
        this.V = this.extraNonbasic;
        this.sparseRow = arraySet.sparseMatrixRow;
        this.sparseCol = arraySet.sparseMatrixCol;
        this.flags = this.extraFlags;
        this.lb = this.extraLowerBounds;
        this.ub = this.extraUpperBounds;
        this.pD = arraySet.pD;

        setupExtraConstraints();

        lu.beginSafe();
        if (prepareForDual) {
            this.cost = arraySet.costWorking;
            loadCost();
            perturbCost();
        }
        ret = solvePrimalUnsafe(prepareForDual, prepareForDual, false, true);
        lu.endUnsafe();

        this.a = null;
        this.U = null;
        this.d = null;
        this.x = null;
        this.w = null;
        this.ww = null;
        this.wd = null;
        this.c = null;
        this.u = null;
        this.cost = null;
        this.sparseValue = null;
        this.D = null;
        this.pD = null;
        this.B = null;
        this.V = null;
        this.sparseRow = null;
        this.sparseCol = null;
        this.flags = null;
        this.lb = null;
        this.ub = null;

        return ret;
    }

    public void quadraticQ(double[] x, double[] Qx) {
        int sortj = 0;
        double xm = 0.0;
        for (int j = 0; j < cols; j++) {
            sortj = sort[j];
            xm += x[sortj] * a[sortj * baseRows + mpsRow];
            Qx[sortj] = spellDps[sortj] * xm;
        }
        double xe = x[sortj] * spellDps[sortj];
        for (int j = cols - 2; j >= 0; j--) {
            sortj = sort[j];
            Qx[sortj] += a[sortj * baseRows + mpsRow] * xe;
            xe += x[sortj] * spellDps[sortj];
        }
    }

    public void quadraticQOSB(double[] x, double[] Qx) {
        int sortj = 0;
        double xm = 0.0;
        for (int j = 0; j < osbStructural; j++) {
            sortj = OSB[j];
            xm += x[sortj] * a[sortj * baseRows + mpsRow];
            Qx[sortj] = spellDps[sortj] * xm;
        }
        double xe = x[sortj] * spellDps[sortj];
        for (int j = osbStructural - 2; j >= 0; j--) {
            sortj = OSB[j];
            Qx[sortj] += a[sortj * baseRows + mpsRow] * xe;
            xe += x[sortj] * spellDps[sortj];
        }
    }

    private void updateMB() {
        // TODO this should be updated instead of reset each time
        for (int i = 0; i < rows; i++) {
            int col = B[i];
            mb[col] = d[i];
        }
        for (int col = 0; col < cols + rows; col++) {
            if ((flags[col] & flagNLB) != 0) {
                mb[col] = lb[col];
            } else if ((flags[col] & flagNUB) != 0) {
                mb[col] = ub[col];
            }
        }
    }

    private void computeQx() {
        int sortj = 0;
        double xm = 0.0;
        for (int j = 0; j < cols; j++) {
            sortj = sort[j];
            xm += mb[sortj] * a[sortj * baseRows + mpsRow];
            qx[sortj] = spellDps[sortj] * xm;
        }
        double xe = mb[sortj] * spellDps[sortj];
        for (int j = cols - 2; j >= 0; j--) {
            sortj = sort[j];
            qx[sortj] += a[sortj * baseRows + mpsRow] * xe;
            xe += mb[sortj] * spellDps[sortj];
        }
    }

    public double computevQv(int incoming, double direction) {
        // v = 1 at incoming, 0 at all other nonbasic, -w at basic
        // expand v
        zero(vd, cols);
        for (int i = 0; i < rows; i++) {
            int col = B[i];
            vd[col] = -direction * w[i];
        }
        vd[V[incoming]] = direction;
        int sortj = 0;
        double xm = 0.0;
        for (int j = 0; j < cols; j++) {
            sortj = sort[j];
            xm += vd[sortj] * a[sortj * baseRows + mpsRow];
            qv[sortj] = spellDps[sortj] * xm;
        }
        double xe = vd[sortj] * spellDps[sortj];
        for (int j = cols - 2; j >= 0; j--) {
            sortj = sort[j];
            qv[sortj] += a[sortj * baseRows + mpsRow] * xe;
            xe += vd[sortj] * spellDps[sortj];
        }
        double v = 0.0;
        for (int j = 0; j < cols; j++) {
            v += qv[j] * vd[j];
        }
        return -v * qk;
    }

    public double[] solvePrimalQuadratic(int mpsRow, int[] sort, double k, boolean skipLinear, int maximizeColumn, double targetValue, int globalRestarts) {
        if (hardInfeasibility) return new double[cols + 1];
        double[] ret = null;
        double[] bestret = null;

        if (arraySet.cg_p == null || arraySet.cg_p.length < cols + rows) {
            arraySet.recreateCGArrays();
        }

        this.mpsRow = mpsRow;
        this.sort = sort;
        this.qk = k;
        if (this.extraSlack == null || this.extraSlack.length < cols) {
            extraSlack = new int[cols];
        }

        this.a = arraySet.sparseMatrixData;
        this.U = arraySet.LU_U;
        this.d = arraySet.d;
        this.x = arraySet.x;
        this.w = arraySet.w;
        this.ww = arraySet.ww;
        this.wd = arraySet.wd;
        this.c = arraySet.c;
        this.u = arraySet.u;
        this.cost = arraySet.cost;
        this.sparseValue = arraySet.sparseMatrixValue;
        this.D = arraySet.extraConstraints;
        this.B = this.extraBasis;
        this.V = this.extraNonbasic;
        this.S = this.extraSlack;
        this.OSB = arraySet.orderedSuperbasis;
        this.sparseRow = arraySet.sparseMatrixRow;
        this.sparseCol = arraySet.sparseMatrixCol;
        this.flags = this.extraFlags;
        this.lb = this.extraLowerBounds;
        this.ub = this.extraUpperBounds;
        this.mb = this.extraMiddleBounds;
        this.pD = arraySet.pD;
        this.qx = arraySet.qx;
        this.qv = arraySet.qv;
        this.vd = arraySet.vd;
        this.spellDps = arraySet.spellDps;
        this.cg_p = arraySet.cg_p;
        this.cg_x = arraySet.cg_x;
        this.cg_w = arraySet.cg_w;
        this.cg_r = arraySet.cg_r;
        this.cg_qp = arraySet.cg_qp;
        this.cg_rr = arraySet.cg_rr;
        this.cg_ww = arraySet.cg_ww;

        setupExtraConstraints();

        lu.beginSafe();

        int step = 10;
        double lastScore = Double.NEGATIVE_INFINITY;
        @SuppressWarnings("unchecked")
        List<Integer>[] perturbColumns = new List[1];

        globalRestart: for (;;) {
            if (maximizeColumn > -1) {
                ret = solvePrimalQuadraticCGReciprocalUnsafe(maximizeColumn, targetValue);
            } else {
                if (!skipLinear) {
                    // we have to fix flags in case we're doing MIP and we're resolving with linear
                    for (int i = 0; i < superCount; i++) {
                        int col = S[i];
                        if ((flags[col] & flagLB) != 0) {
                            flags[col] = (flags[col] & ~flagN) | flagNLB;
                        } else {
                            flags[col] = (flags[col] & ~flagN) | flagNUB;
                        }
                    }
                    superCount = 0;
                    ret = solvePrimalUnsafe(false, false, false, true);
                }
                ret = solvePrimalQuadraticCGUnsafe();
            }
            double score = ret[ret.length - 1];
            if (bestret == null || score > bestret[bestret.length - 1]) {
                bestret = ret;
            }
            if (globalRestarts > 0 && score != 0.0) {
                /*if (Math.Abs(score - lastScore) < epsPrimal)
                {
                    step *= 2;
                }*/
                lastScore = score;
                skipLinear = true;
                globalRestart2(step, perturbColumns);
                globalRestarts--;
                continue globalRestart;
            }
            break;
        }
        lu.endUnsafe();

        this.a = null;
        this.U = null;
        this.d = null;
        this.x = null;
        this.w = null;
        this.ww = null;
        this.wd = null;
        this.c = null;
        this.u = null;
        this.cost = null;
        this.sparseValue = null;
        this.D = null;
        this.pD = null;
        this.B = null;
        this.V = null;
        this.S = null;
        this.OSB = null;
        this.sparseRow = null;
        this.sparseCol = null;
        this.flags = null;
        this.lb = null;
        this.ub = null;
        this.mb = null;
        this.qx = null;
        this.qv = null;
        this.vd = null;
        this.spellDps = null;
        this.cg_p = null;
        this.cg_x = null;
        this.cg_w = null;
        this.cg_r = null;
        this.cg_qp = null;
        this.cg_rr = null;
        this.cg_ww = null;

        return bestret;
    }

    private void globalRestart2(int steps, List<Integer>[] perturbColumns) {
        NetRandom rnd = new NetRandom();

        if (perturbColumns[0] == null) {
            perturbColumns[0] = new ArrayList<Integer>();
            for (int col = 0; col < cols; col++) {
                if (spellDps[col] == 0.0 && a[col * baseRows + mpsRow] <= 0.0) {
                    perturbColumns[0].add(col);
                }
            }
        }

        while (steps > 0) {
            steps--;

            // pick a random nonbasic mana variable
            int col = perturbColumns[0].get(rnd.next(perturbColumns[0].size()));
            int maxj = indexOf(this.extraNonbasic, col);
            if (maxj == -1) {
                continue;
            }
            double direction;

            if ((flags[col] & flagNLB) != 0) {
                S[superCount] = col;
                superCount++;
                flags[col] = (flags[col] & ~flagN) | flagNMid;
                osbDirty = true;

                direction = 1.0;
            } else if ((flags[col] & flagNUB) != 0) {
                S[superCount] = col;
                superCount++;
                flags[col] = (flags[col] & ~flagN) | flagNMid;
                osbDirty = true;

                direction = -1.0;
            } else {
                if (rnd.next(2) == 0) {
                    direction = 1.0;
                } else {
                    direction = -1.0;
                }
            }

            // get blocking basic
            computeBasisStep(maxj);

            double dist;
            if (direction > 0.0) {
                dist = ub[col] - mb[col];
            } else {
                dist = mb[col] - lb[col];
            }
            if (dist > 100.0) {
                dist = 100.0;
            }
            double minr = dist * rnd.nextDouble();

            if (minr > 0.0) {
                minr *= direction;
                mb[col] += minr;
                for (int i = 0; i < rows; i++) {
                    mb[B[i]] -= minr * w[i];
                }
            }
        }
    }

    private void globalRestart(int steps) {
        NetRandom rnd = new NetRandom();

        decompose();

        if (lu.getSingular()) {
            patchSingularBasis();
            decompose();
        }

        computePrimalSolution(false);
        updateMB();

        while (steps > 0) {
            steps--;

            // pick a random nonbasic mana variable
            int maxj;
            int col;
            do {
                maxj = rnd.next(cols);
                col = V[maxj];
            } while (a[col * baseRows + mpsRow] > 0.0);
            double direction;
            if ((flags[col] & flagNLB) != 0) {
                direction = 1.0;
            } else if ((flags[col] & flagNUB) != 0) {
                direction = -1.0;
            } else {
                if (rnd.next(2) == 0) {
                    direction = 1.0;
                } else {
                    direction = -1.0;
                }
            }

            // get blocking basic
            computeBasisStep(maxj);

            int mini = -1;
            double minr = Double.POSITIVE_INFINITY;
            int bound = flagNMid;
            boolean changeBasis = false;

            int[] refMini = new int[] { mini };
            double[] refMinr = new double[] { minr };
            int[] refBound = new int[] { bound };
            if (!selectReducedGradientOutgoing(direction, epsPrimal, refMini, refMinr, refBound)) {
                return;
            }
            mini = refMini[0];
            minr = refMinr[0];
            bound = refBound[0];

            double dist;
            if (direction > 0.0) {
                dist = ub[col] - mb[col];
            } else {
                dist = mb[col] - lb[col];
            }
            if (Double.isInfinite(dist)) {
                continue;
            }
            if (minr >= dist - epsZero) {
                // just a bound swap
                minr = dist;
                if ((flags[col] & flagNMid) != 0) {
                    int s = indexOf(this.extraSlack, col);
                    S[s] = S[superCount - 1];
                    superCount--;
                    osbDirty = true;
                }
                if (direction > 0.0) {
                    flags[col] = (flags[col] & ~flagN) | flagNUB;
                } else {
                    flags[col] = (flags[col] & ~flagN) | flagNLB;
                }
            } else if (mini == -1) {
                return;
            } else {
                // we hit basic bound, update basis
                changeBasis = true;
                osbDirty = true;

                if ((flags[col] & flagNMid) != 0) {
                    int s = indexOf(this.extraSlack, col);
                    S[s] = S[superCount - 1];
                    superCount--;
                    osbDirty = true;
                }
            }

            if (minr > 0.0) {
                minr *= direction;
                mb[col] += minr;
                for (int i = 0; i < rows; i++) {
                    mb[B[i]] -= minr * w[i];
                }
            }

            // swap basis
            if (changeBasis) {
                double[] pivot = new double[1];
                lu.update(ww, mini, pivot);
                if (lu.getSingular() || Math.abs(w[mini] - pivot[0]) > 1.0e-6 * Math.abs(w[mini]) + epsZero) {
                    // we aren't making the swap
                    // make sure to properly add superbasic back to S if we cancelled swap
                    col = V[maxj];
                    if ((flags[col] & flagNMid) != 0) {
                        S[superCount] = col;
                        superCount++;
                    }

                    decompose();

                    if (lu.getSingular()) {
                        patchSingularBasis();
                        decompose();
                    }

                    computePrimalSolution(false);
                    updateMB();
                    continue;
                }

                int k = B[mini];
                B[mini] = V[maxj];
                V[maxj] = k;
                flags[B[mini]] = (flags[B[mini]] | flagB) & ~flagN;
                flags[V[maxj]] = (flags[V[maxj]] | bound) & ~flagB;

                if (steps > 0 && steps % 50 == 0) {
                    decompose();

                    if (lu.getSingular()) {
                        patchSingularBasis();
                        decompose();
                    }

                    computePrimalSolution(false);
                    updateMB();
                }
            }
        }
    }

    private void decompose() {
        int uj = 0;
        for (int j = 0; j < rows; j++, uj++) {
            int col = B[j];
            if (col < cols) {
                int ai = col * baseRows;
                int ui = uj;
                for (int i = 0; i < baseRows; i++, ai++, ui += rows) {
                    U[ui] = a[ai];
                }
                for (int k = 0; k < numExtraConstraints; k++, ui += rows) {
                    U[ui] = pD[k].get(col);
                }
            } else {
                for (int i = 0; i < rows; i++) {
                    U[i * rows + uj] = (i == col - cols) ? 1.0 : 0.0;
                }
            }
        }
        lu.decompose();
    }

    private void patchSingularBasis() {
        for (int j = lu.getRank(); j < rows; j++) {
            int singularColumn = arraySet.LU_Q[j];
            int singularRow = arraySet.LU_P[j];
            int slackColumn = cols + singularRow;
            int vindex = indexOf(this.extraNonbasic, slackColumn);
            V[vindex] = B[singularColumn];
            B[singularColumn] = slackColumn;
            if ((flags[V[vindex]] & flagLB) != 0) {
                flags[V[vindex]] = (flags[V[vindex]] | flagNLB) & ~flagB & ~flagDis;
            } else {
                flags[V[vindex]] = (flags[V[vindex]] | flagNUB) & ~flagB & ~flagDis;
            }
            if ((flags[B[singularColumn]] & flagNMid) != 0) {
                int s = indexOf(this.extraSlack, B[singularColumn]);
                S[s] = S[superCount - 1];
                superCount--;
            }
            flags[B[singularColumn]] = (flags[B[singularColumn]] | flagB) & ~flagN & ~flagDis;
            //ValidateSuperBasis();
        }
        osbDirty = true;
    }

    private double dualPhaseILowerBound(int col) {
        //if (col < cols && (flags[col] & flagLB) == 0) return -1.0;
        if ((flags[col] & flagLB) == 0) return -1.0;
        return 0.0;
    }

    private double dualPhaseIUpperBound(int col) {
        //if (col < cols && (flags[col] & flagUB) == 0) return 1.0;
        if ((flags[col] & flagUB) == 0) return 1.0;
        return 0.0;
    }

    private void computePrimalSolution(boolean dualPhaseI) {
        zero(d, rows);
        //int j = 0;
        //for (; j < baseRows; j++)
        //{
        //    d[j] = b[j];
        //}
        //for (int k = 0; k < numExtraConstraints; k++, j++)
        //{
        //    d[j] = D[k * (cols + 1) + cols];
        //}
        for (int col = 0; col < cols + rows; col++) {
            double v = 0.0;
            if (dualPhaseI) {
                if ((flags[col] & flagNLB) != 0) {
                    v = dualPhaseILowerBound(col);
                } else if ((flags[col] & flagNUB) != 0) {
                    v = dualPhaseIUpperBound(col);
                }
            } else {
                if ((flags[col] & flagNLB) != 0) {
                    v = lb[col];
                } else if ((flags[col] & flagNUB) != 0) {
                    v = ub[col];
                } else if ((flags[col] & flagNMid) != 0) {
                    v = mb[col];
                }
            }
            if (Math.abs(v) >= epsZero) {
                if (col < cols) {
                    int sCol1 = sparseCol[col];
                    int sCol2 = sparseCol[col + 1];
                    for (int i = sCol1; i < sCol2; i++) {
                        d[sparseRow[i]] -= sparseValue[i] * v;
                    }
                    for (int k = 0; k < numExtraConstraints; k++) {
                        d[baseRows + k] -= pD[k].get(col) * v;
                    }
                } else {
                    d[col - cols] -= v;
                }
            }
        }
        lu.fSolve(d);
    }

    private boolean isPrimalFeasible(double eps) {
        for (int i = 0; i < rows; i++) {
            int col = B[i];
            if (d[i] < lb[col] - Math.abs(lb[col]) * epsPrimalRel - eps || d[i] > ub[col] + Math.abs(ub[col]) * epsPrimalRel + eps) {
                return false;
            }
        }
        return true;
    }

    private boolean isPrimalQFeasible(double eps, boolean[] recalculate) {
        boolean feasible = true;
        recalculate[0] = false;
        for (int i = 0; i < rows + cols; i++) {
            if (mb[i] < lb[i] - Math.abs(lb[i]) * epsPrimalRel - eps) {
                if ((flags[i] & flagN) != 0) {
                    mb[i] = lb[i];
                    recalculate[0] = true;
                }
                feasible = false;
            } else if (mb[i] > ub[i] + Math.abs(ub[i]) * epsPrimalRel + eps) {
                if ((flags[i] & flagN) != 0) {
                    mb[i] = ub[i];
                    recalculate[0] = true;
                }
                feasible = false;
            }
        }
        return feasible;
    }

    private void computeReducedCosts() {
        for (int i = 0; i < rows; i++) {
            //if (B[i] < cols) u[i] = cost[B[i]];
            //else u[i] = 0.0;
            u[i] = cost[B[i]];
        }
        lu.bSolve(u);
        for (int j = 0; j < cols; j++) {
            int col = V[j];

            if (col < cols) {
                double costcol = cost[col];
                int sCol1 = sparseCol[col];
                int sCol2 = sparseCol[col + 1];
                for (int i = sCol1; i < sCol2; i++) {
                    costcol -= sparseValue[i] * u[sparseRow[i]];
                }
                for (int k = 0; k < numExtraConstraints; k++) {
                    costcol -= pD[k].get(col) * u[baseRows + k];
                }
                c[j] = costcol;
            } else {
                c[j] = cost[col] - u[col - cols];
            }
        }
    }

    private void computeReducedCostGradient() {
        computeQx();
        for (int i = 0; i < rows; i++) {
            //if (B[i] < cols) u[i] = cost[B[i]];
            //else u[i] = 0.0;
            int col = B[i];
            if (col < cols) {
                u[i] = cost[col] - qk * qx[col];
            } else {
                u[i] = cost[col];
            }
        }
        lu.bSolve(u);
        for (int j = 0; j < cols; j++) {
            int col = V[j];

            if (col < cols) {
                double costcol = cost[col] - qk * qx[col];
                int sCol1 = sparseCol[col];
                int sCol2 = sparseCol[col + 1];
                for (int i = sCol1; i < sCol2; i++) {
                    costcol -= sparseValue[i] * u[sparseRow[i]];
                }
                for (int k = 0; k < numExtraConstraints; k++) {
                    costcol -= pD[k].get(col) * u[baseRows + k];
                }
                c[j] = costcol;
            } else {
                c[j] = cost[col] - u[col - cols];
            }
        }
    }

    private void computePhaseIReducedCosts(double[] infeasibility, double eps) {
        infeasibility[0] = 0.0;
        for (int i = 0; i < rows; i++) {
            int col = B[i];
            if (d[i] < lb[col] - Math.abs(lb[col]) * epsPrimalRel - eps) {
                x[i] = 1.0;
                infeasibility[0] += lb[col] - d[i];
            } else if (d[i] > ub[col] + Math.abs(ub[col]) * epsPrimalRel + eps) {
                x[i] = -1.0;
                infeasibility[0] += d[i] - ub[col];
            } else x[i] = 0.0;
        }
        lu.bSolve(x);
        for (int j = 0; j < cols; j++) {
            int col = V[j];

            if (col < cols) {
                double costcol = 0;
                int sCol1 = sparseCol[col];
                int sCol2 = sparseCol[col + 1];
                for (int i = sCol1; i < sCol2; i++) {
                    costcol -= sparseValue[i] * x[sparseRow[i]];
                }
                for (int k = 0; k < numExtraConstraints; k++) {
                    costcol -= pD[k].get(col) * x[baseRows + k];
                }
                c[j] = costcol;
            } else {
                c[j] = -x[col - cols];
            }
        }
    }

    private void computePhaseIQReducedCosts(double[] infeasibility, double eps) {
        infeasibility[0] = 0.0;
        for (int i = 0; i < rows; i++) {
            int col = B[i];
            if (mb[col] < lb[col] - Math.abs(lb[col]) * epsPrimalRel - eps) {
                x[i] = 1.0;
                infeasibility[0] += lb[col] - mb[col];
            } else if (mb[col] > ub[col] + Math.abs(ub[col]) * epsPrimalRel + eps) {
                x[i] = -1.0;
                infeasibility[0] += mb[col] - ub[col];
            } else x[i] = 0.0;
        }
        lu.bSolve(x);
        for (int j = 0; j < cols; j++) {
            int col = V[j];

            if (col < cols) {
                double costcol = 0;
                int sCol1 = sparseCol[col];
                int sCol2 = sparseCol[col + 1];
                for (int i = sCol1; i < sCol2; i++) {
                    costcol -= sparseValue[i] * x[sparseRow[i]];
                }
                for (int k = 0; k < numExtraConstraints; k++) {
                    costcol -= pD[k].get(col) * x[baseRows + k];
                }
                c[j] = costcol;
            } else {
                c[j] = -x[col - cols];
            }
        }
    }

    private double[] computeReturnSolution() {
        double[] ret = new double[cols + 1];
        double value = 0.0;
        for (int i = 0; i < rows; i++) {
            if (B[i] < cols) {
                double di = d[i];
                if (Math.abs(di - lb[B[i]]) < Math.abs(lb[B[i]]) * epsPrimalRel + epsPrimalLow || di < lb[B[i]]) di = lb[B[i]];
                else if (Math.abs(di - ub[B[i]]) < Math.abs(ub[B[i]]) * epsPrimalRel + epsPrimalLow || di > ub[B[i]]) di = ub[B[i]];
                ret[B[i]] = di;
                value += cost[B[i]] * di;
            }
        }
        for (int i = 0; i < cols; i++) {
            if ((flags[i] & flagNLB) != 0) {
                ret[i] = lb[i];
                value += cost[i] * lb[i];
            } else if ((flags[i] & flagNUB) != 0) {
                ret[i] = ub[i];
                value += cost[i] * ub[i];
            } else if ((flags[i] & flagNMid) != 0) {
                ret[i] = mb[i];
                value += cost[i] * mb[i];
            }
        }
        ret[cols] = value;
        return ret;
    }

    private double[] computeReturnSolutionQuadratic() {
        double[] ret = new double[cols + 1];
        double value = 0.0;
        for (int col = 0; col < cols; col++) {
            if ((flags[col] & flagNLB) != 0) {
                ret[col] = lb[col];
                value += cost[col] * lb[col];
            } else if ((flags[col] & flagNUB) != 0) {
                ret[col] = ub[col];
                value += cost[col] * ub[col];
            } else {
                double di = mb[col];
                if (Math.abs(di - lb[col]) < Math.abs(lb[col]) * epsPrimalRel + epsPrimalLow || di < lb[col]) di = lb[col];
                else if (Math.abs(di - ub[col]) < Math.abs(ub[col]) * epsPrimalRel + epsPrimalLow || di > ub[col]) di = ub[col];
                ret[col] = di;
                value += cost[col] * di;
            }
        }
        computeQx();
        for (int i = 0; i < cols; i++) {
            value -= 0.5 * qk * qx[i] * mb[i];
        }
        ret[cols] = value;
        return ret;
    }

    private double computeValue() {
        double value = 0.0;
        for (int i = 0; i < rows; i++) {
            if (B[i] < cols) {
                value += cost[B[i]] * d[i];
            }
        }
        for (int i = 0; i < cols; i++) {
            if ((flags[i] & flagNLB) != 0) {
                value += cost[i] * lb[i];
            } else if ((flags[i] & flagNUB) != 0) {
                value += cost[i] * ub[i];
            } else if ((flags[i] & flagNMid) != 0) {
                value += cost[i] * mb[i];
            }
        }
        return value;
    }

    private double computeValueQuadratic() {
        double value = 0.0;
        for (int i = 0; i < cols; i++) {
            value += cost[i] * mb[i];
        }
        computeQx();
        for (int i = 0; i < cols; i++) {
            value -= 0.5 * qk * qx[i] * mb[i];
        }
        /*for (int i = 0; i < cols; i++)
        {
            value -= 0.5 * Qk * mb[i] * mb[i] * a[i * baseRows + mpsRow] * spellDps[i];
        }
        for (int i = 0; i < cols; i++)
        {
            for (int j = i + 1; j < cols; j++)
            {
                if (sortinv[i] < sortinv[j])
                {
                    value -= Qk * mb[i] * mb[j] * a[i * baseRows + mpsRow] * spellDps[j];
                }
                else
                {
                    value -= Qk * mb[i] * mb[j] * a[j * baseRows + mpsRow] * spellDps[i];
                }
            }
        }*/
        return value;
    }

    private double computeDualIValue() {
        double value = 0.0;
        for (int i = 0; i < rows; i++) {
            if (B[i] < cols) {
                value += cost[B[i]] * d[i];
            }
        }
        for (int i = 0; i < cols; i++) {
            if ((flags[i] & flagNLB) != 0) {
                value += cost[i] * dualPhaseILowerBound(i);
            } else if ((flags[i] & flagNUB) != 0) {
                value += cost[i] * dualPhaseIUpperBound(i);
            }
        }
        return value;
    }

    private int selectPrimalIncoming(double[] direction, boolean prepareForDual) {
        double maxc = (prepareForDual) ? epsDualI : epsDual;
        int maxj = -1;
        direction[0] = 0.0;
        double dir = 0.0;
        for (int j = 0; j < cols; j++) {
            int col = V[j];
            double costj = 0.0;
            if ((flags[col] & flagNLB) != 0) {
                costj = c[j];
                dir = 1.0;
            } else if ((flags[col] & flagNUB) != 0) {
                costj = -c[j];
                dir = -1.0;
            } else if ((flags[col] & flagNMid) != 0) {
                if (c[j] > maxc) {
                    costj = c[j];
                    dir = 1.0;
                } else {
                    costj = -c[j];
                    dir = -1.0;
                }
            }
            if (costj > maxc && (flags[col] & flagDis) == 0 && (flags[col] & flagFix) == 0) {
                maxc = costj;
                maxj = j;
                direction[0] = dir;
            }
        }
        return maxj;
    }

    private int selectPrimalIncomingBoundary(double[] direction, boolean prepareForDual) {
        double maxc = (prepareForDual) ? epsDualI : epsDual;
        int maxj = -1;
        direction[0] = 0.0;
        double dir = 0.0;
        for (int j = 0; j < cols; j++) {
            int col = V[j];
            double costj = 0.0;
            if ((flags[col] & flagNLB) != 0) {
                costj = c[j];
                dir = 1.0;
            } else if ((flags[col] & flagNUB) != 0) {
                costj = -c[j];
                dir = -1.0;
            } else if ((flags[col] & flagNMid) != 0) {
                continue;
            }
            if (costj > maxc && (flags[col] & flagDis) == 0 && (flags[col] & flagFix) == 0) {
                maxc = costj;
                maxj = j;
                direction[0] = dir;
            }
        }
        return maxj;
    }

    private int selectPrimalAny() {
        double maxc = Double.NEGATIVE_INFINITY;
        int maxj = -1;
        for (int j = 0; j < cols; j++) {
            int col = V[j];
            double costj = 0.0;
            if ((flags[col] & flagNLB) != 0) {
                costj = c[j];
            } else if ((flags[col] & flagNUB) != 0) {
                costj = -c[j];
            } else if ((flags[col] & flagNMid) != 0) {
                if (c[j] > maxc) {
                    costj = c[j];
                } else {
                    costj = -c[j];
                }
            }
            if (costj > maxc && (flags[col] & flagDis) == 0 && (flags[col] & flagFix) == 0) {
                maxc = costj;
                maxj = j;
            }
        }
        return maxj;
    }

    private int selectPrimalReciprocal(int maximizeColumn, double eps) {
        for (int i = 0; i < rows; i++) {
            int col = B[i];
            if (mb[col] < lb[col] + Math.abs(lb[col]) * epsPrimalRel + eps) {
                x[i] = 1.0;
            } else if (mb[col] > ub[col] - Math.abs(ub[col]) * epsPrimalRel - eps) {
                x[i] = -1.0;
            } else x[i] = 0.0;
        }
        lu.bSolve(x);

        double maxc = epsDual;
        int maxj = -1;

        for (int j = 0; j < cols; j++) {
            int col = V[j];
            if (col == maximizeColumn) {
                continue;
            }

            double costcol;
            if (col < cols) {
                costcol = 0;
                int sCol1 = sparseCol[col];
                int sCol2 = sparseCol[col + 1];
                for (int i = sCol1; i < sCol2; i++) {
                    costcol -= sparseValue[i] * x[sparseRow[i]];
                }
                for (int k = 0; k < numExtraConstraints; k++) {
                    costcol -= pD[k].get(col) * x[baseRows + k];
                }
            } else {
                costcol = -x[col - cols];
            }

            double costj;
            if ((flags[col] & flagNLB) != 0) {
                costj = costcol;
            } else if ((flags[col] & flagNUB) != 0) {
                costj = -costcol;
            } else/* if ((flags[col] & flagNMid) != 0)*/ {
                if (costcol > maxc) {
                    costj = costcol;
                } else {
                    costj = -costcol;
                }
            }
            if (costj > maxc && (flags[col] & flagDis) == 0 && (flags[col] & flagFix) == 0) {
                maxc = costj;
                maxj = j;
            }
        }

        return maxj;
    }

    private int checkTermination(double[] direction) {
        double dir = 0.0;
        for (int j = 0; j < cols; j++) {
            int col = V[j];
            double costj = 0.0;
            if ((flags[col] & flagNLB) != 0) {
                costj = c[j];
                dir = 1.0;
            } else if ((flags[col] & flagNUB) != 0) {
                costj = -c[j];
                dir = -1.0;
            } else if ((flags[col] & flagNMid) != 0) {
                if (c[j] > epsDual) {
                    costj = c[j];
                    dir = 1.0;
                } else {
                    costj = -c[j];
                    dir = -1.0;
                }
            }
            if (costj > epsDual && (flags[col] & flagDis) == 0 && (flags[col] & flagFix) == 0) {
                computeBasisStep(j);
                double t = computeQuadraticLimit(j, dir);

                if (0.5 * c[j] * dir * t > epsPrimal) {
                    direction[0] = dir;
                    return j;
                }
            }
        }
        direction[0] = 0.0;
        return -1;
    }

    private boolean selectCGOutgoing(double eps, int[] refcol, double[] refalpha, int[] refbound) {
        for (int col = 0; col < cols + rows; col++) {
            double difflb = mb[col] - lb[col];
            double diffub = mb[col] - ub[col];
            boolean ifeasiblelb = (difflb >= -Math.abs(lb[col]) * epsPrimalRel - eps);
            boolean ifeasibleub = (diffub <= +Math.abs(ub[col]) * epsPrimalRel + eps);
            boolean ifeasible = ifeasiblelb && ifeasibleub;
            if (!ifeasible) {
                return false;
            }

            if (Math.abs(cg_x[col]) > epsZero) {
                // mb[col] + alpha * cg_x[col]
                double limit;
                if (cg_x[col] > 0) {
                    if (mb[col] + refalpha[0] * cg_x[col] - ub[col] > +Math.abs(ub[col]) * epsPrimalRel + eps) {
                        limit = (ub[col] - mb[col]) / cg_x[col];
                        if (limit < refalpha[0]) {
                            refalpha[0] = limit;
                            refcol[0] = col;
                            refbound[0] = flagNUB;
                        }
                    }
                } else {
                    if (mb[col] + refalpha[0] * cg_x[col] - lb[col] < -Math.abs(lb[col]) * epsPrimalRel - eps) {
                        limit = (lb[col] - mb[col]) / cg_x[col];
                        if (limit < refalpha[0]) {
                            refalpha[0] = limit;
                            refcol[0] = col;
                            refbound[0] = flagNLB;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean selectPrimalOutgoing(double direction, boolean feasible, double eps, int[] refmini, double[] refminr, int[] refbound) {
        // min over i of d[i]/w[i] where w[i]>0
        double minrr = refminr[0];
        double minr = refminr[0];
        int mini = refmini[0];
        int bound = refbound[0];
        double minv = 0.0;
        //double reducedcost = c[incoming];
        for (int i = 0; i < rows; i++) {
            double wi = w[i];
            double v = Math.abs(wi);
            double widir = direction * wi;
            int col = B[i];
            double difflb = d[i] - lb[col];
            double diffub = d[i] - ub[col];
            boolean ifeasiblelb = (difflb >= -Math.abs(lb[col]) * epsPrimalRel - eps);
            boolean ifeasibleub = (diffub <= +Math.abs(ub[col]) * epsPrimalRel + eps);
            boolean ifeasible = ifeasiblelb && ifeasibleub;
            if (feasible && !ifeasible) {
                // we lost primal feasibility
                // fall back to phase I
                refmini[0] = mini;
                refminr[0] = minr;
                refbound[0] = bound;
                return false;
            }
            int branch = 0;
            if (feasible || ifeasible) {
                if (widir > epsPivot && (flags[col] & flagLB) != 0) {
                    branch = 1;
                } else if (widir < -epsPivot && (flags[col] & flagUB) != 0) {
                    branch = 2;
                }
            } else {
                if (!ifeasibleub && widir > epsPivot) {
                    branch = 2;
                } else if (!ifeasiblelb && widir < -epsPivot) {
                    branch = 1;
                }
            }
            if (branch == 1) {
                // LBCHECK:
                double r = difflb / widir;
                if (r < minrr + epsZero && (r < minrr || v > minv)) {
                    minrr = r;
                    minr = difflb / wi;
                    mini = i;
                    minv = v;
                    bound = flagNLB;
                }
            } else if (branch == 2) {
                // UBCHECK:
                double r = diffub / widir;
                if (r < minrr + epsZero && (r < minrr || v > minv)) {
                    minrr = r;
                    minr = diffub / wi;
                    mini = i;
                    minv = v;
                    bound = flagNUB;
                }
            }
        }
        refmini[0] = mini;
        refminr[0] = minr;
        refbound[0] = bound;
        return true;
    }

    private void selectPhaseIQOutgoing(double direction, double eps, int[] refmini, double[] refminr, int[] refbound) {
        // min over i of d[i]/w[i] where w[i]>0
        double minrr = refminr[0];
        double minr = refminr[0];
        int mini = refmini[0];
        int bound = refbound[0];
        double minv = 0.0;
        //double reducedcost = c[incoming];
        for (int i = 0; i < rows; i++) {
            double wi = w[i];
            double v = Math.abs(wi);
            double widir = direction * wi;
            int col = B[i];
            double difflb = mb[col] - lb[col];
            double diffub = mb[col] - ub[col];
            boolean ifeasiblelb = (difflb >= -Math.abs(lb[col]) * epsPrimalRel - eps);
            boolean ifeasibleub = (diffub <= +Math.abs(ub[col]) * epsPrimalRel + eps);
            boolean ifeasible = ifeasiblelb && ifeasibleub;
            int branch = 0;
            if (ifeasible) {
                if (widir > epsPivot && (flags[col] & flagLB) != 0) {
                    branch = 1;
                } else if (widir < -epsPivot && (flags[col] & flagUB) != 0) {
                    branch = 2;
                }
            } else {
                if (!ifeasibleub && widir > epsPivot) {
                    branch = 2;
                } else if (!ifeasiblelb && widir < -epsPivot) {
                    branch = 1;
                }
            }
            if (branch == 1) {
                // LBCHECK:
                double r = difflb / widir;
                if (r < minrr + epsZero && (r < minrr || v > minv)) {
                    minrr = r;
                    minr = difflb / wi;
                    mini = i;
                    minv = v;
                    bound = flagNLB;
                }
            } else if (branch == 2) {
                // UBCHECK:
                double r = diffub / widir;
                if (r < minrr + epsZero && (r < minrr || v > minv)) {
                    minrr = r;
                    minr = diffub / wi;
                    mini = i;
                    minv = v;
                    bound = flagNUB;
                }
            }
        }
        refmini[0] = mini;
        refminr[0] = minr;
        refbound[0] = bound;
    }

    private boolean selectReducedGradientOutgoing(double direction, double eps, int[] refmini, double[] refminr, int[] refbound) {
        // min over i of d[i]/w[i] where w[i]>0
        double minrr = refminr[0];
        int mini = refmini[0];
        int bound = refbound[0];
        double minv = 0.0;
        //double reducedcost = c[incoming];
        for (int i = 0; i < rows; i++) {
            double wi = w[i];
            double v = Math.abs(wi);
            double widir = direction * wi;
            int col = B[i];
            double difflb = mb[col] - lb[col];
            double diffub = mb[col] - ub[col];
            boolean ifeasiblelb = (difflb >= -Math.abs(lb[col]) * epsPrimalRel - eps);
            boolean ifeasibleub = (diffub <= +Math.abs(ub[col]) * epsPrimalRel + eps);
            boolean ifeasible = ifeasiblelb && ifeasibleub;
            if (!ifeasible) {
                // we lost feasibility
                return false;
            }
            int branch = 0;
            if (widir > epsPivot && (flags[col] & flagLB) != 0) {
                branch = 1;
            } else if (widir < -epsPivot && (flags[col] & flagUB) != 0) {
                branch = 2;
            }
            if (branch == 1) {
                // LBCHECK:
                double r = difflb / widir;
                if (r < minrr + epsZero && (r < minrr || v > minv)) {
                    minrr = r;
                    mini = i;
                    minv = v;
                    bound = flagNLB;
                }
            } else if (branch == 2) {
                // UBCHECK:
                double r = diffub / widir;
                if (r < minrr + epsZero && (r < minrr || v > minv)) {
                    minrr = r;
                    mini = i;
                    minv = v;
                    bound = flagNUB;
                }
            }
        }
        refmini[0] = mini;
        refminr[0] = minrr;
        refbound[0] = bound;
        return true;
    }

    private double computeQuadraticLimit(int incoming, double direction) {
        // feasible direction v, 1 in component incoming
        // [B N]*[vB vN] = 0
        // vB = -Binv * N*vN = -w
        // increase in value = t * vT * grad + t^2/2 * vT * Q * v
        // vT * grad = c[incoming]
        double vQv = computevQv(incoming, direction);
        // c[incoming] + t * vQv = 0
        if (vQv > -epsZero) {
            return Double.POSITIVE_INFINITY;
        }
        return -c[incoming] * direction / vQv;
    }

    private void computeBasisStep(int incoming) {
        // w = U \ (L \ A(:,j));
        int maxcol = V[incoming];
        if (maxcol < cols) {
            copy(w, a, maxcol * baseRows, baseRows);
            if (numExtraConstraints > 0) {
                for (int k = 0; k < numExtraConstraints; k++) {
                    w[baseRows + k] = pD[k].get(maxcol);
                }
            }
        } else {
            zero(w, rows);
            w[maxcol - cols] = 1.0;
            /*for (int i = 0; i < rows; i++)
            {
                w[i] = (i == maxcol - cols) ? 1.0 : 0.0;
            }*/
        }
        //lu.FSolve(w);
        lu.fSolveL(w, ww, 0.00000001);
        lu.fSolveU(ww, w, 0.00000001);
    }

    private static void copy(double[] dest, double[] source, int sourceOffset, int size) {
        System.arraycopy(source, sourceOffset, dest, 0, size);
    }

    private void computeBasisReplace(int incoming) {
        // w = U \ (L \ A(:,j));
        int maxcol = incoming;
        if (maxcol < cols) {
            copy(w, a, maxcol * baseRows, baseRows);
            if (numExtraConstraints > 0) {
                for (int k = 0; k < numExtraConstraints; k++) {
                    w[baseRows + k] = pD[k].get(maxcol);
                }
            }
        } else {
            zero(w, rows);
            w[maxcol - cols] = 1.0;
            /*for (int i = 0; i < rows; i++)
            {
                w[i] = (i == maxcol - cols) ? 1.0 : 0.0;
            }*/
        }
        lu.fSolveL(w, ww, 0.00000001);
    }

    private int selectDualOutgoing(boolean phaseI, double[] delta, int[] bound) {
        double mind = 0.0;
        int mini = -1;
        delta[0] = 0.0;
        bound[0] = 0;
        for (int i = 0; i < rows; i++) {
            int col = B[i];
            double lbcol, ubcol;
            if (phaseI) {
                lbcol = dualPhaseILowerBound(col);
                ubcol = dualPhaseIUpperBound(col);
            } else {
                lbcol = lb[col];
                ubcol = ub[col];
            }
            if ((flags[col] & flagDis) == 0) {
                if ((phaseI || (flags[col] & flagLB) != 0) && d[i] < lbcol - Math.abs(lbcol) * epsPrimalRel - epsPrimal) {
                    double dist = d[i] - lbcol;
                    if (dist < mind) {
                        mind = dist;
                        mini = i;
                        delta[0] = dist;
                        bound[0] = flagNLB;
                    }
                } else if ((phaseI || (flags[col] & flagUB) != 0) && d[i] > ubcol + Math.abs(ubcol) * epsPrimalRel + epsPrimal) {
                    double dist = ubcol - d[i];
                    if (dist < mind) {
                        mind = dist;
                        mini = i;
                        delta[0] = -dist;
                        bound[0] = flagNUB;
                    }
                }
            }
        }
        return mini;
    }

    private boolean computeDualPivotRow(boolean phaseI, int outgoing) {
        // x = z <- e_mini*A_B^-1
        /*for (int i = 0; i < rows; i++)
        {
            x[i] = ((i == outgoing) ? 1 : 0);
        }
        lu.BSolve(x); // TODO exploit nature of x*/
        lu.bSolveUnit(x, outgoing);

        double eps = phaseI ? epsDualI : epsDual;
        for (int j = 0; j < cols; j++) {
            int col = V[j];
            if (phaseI) {
                if ((flags[col] & flagUB) != 0 && (flags[col] & flagLB) != 0) continue;
            } else {
                if ((flags[col] & flagFix) != 0) continue;
            }
            if ((flags[col] & flagNLB) != 0 && c[j] > eps) {
                // we lost dual feasibility
                // creative use of shifting
                double shift = eps - c[j];
                if (Math.abs(shift) < 1.0e-5) {
                    c[j] += shift;
                    cost[col] += shift; // requires working copy for cost
                    costWorkingDirty = true;
                    shifted = true;
                } else {
                    // fall back to dual phase I
                    return false;
                }
            } else if ((flags[col] & flagNUB) != 0 && c[j] < -eps) {
                // we lost dual feasibility
                // creative use of shifting
                double shift = -eps - c[j];
                if (Math.abs(shift) < 1.0e-5) {
                    c[j] += shift;
                    cost[col] += shift; // requires working copy for cost
                    costWorkingDirty = true;
                    shifted = true;
                } else {
                    // fall back to dual phase I
                    return false;
                }
            }
            if (col < cols) {
                wd[j] = 0;
                int sCol1 = sparseCol[col];
                int sCol2 = sparseCol[col + 1];
                for (int i = sCol1; i < sCol2; i++) {
                    wd[j] += sparseValue[i] * x[sparseRow[i]];
                }
                for (int k = 0; k < numExtraConstraints; k++) {
                    wd[j] += pD[k].get(col) * x[baseRows + k];
                }
            } else {
                wd[j] = x[col - cols];
            }
        }
        return true;
    }

    private void selectDualIncoming(boolean phaseI, int outgoing, int[] minj, double[] minr) {
        double minrr = Double.POSITIVE_INFINITY;
        minr[0] = Double.POSITIVE_INFINITY;
        minj[0] = -1;
        double minv = 0.0;
        double eps = phaseI ? epsDualI : epsDual;
        double direction = 1.0;
        if (phaseI) {
            if (d[outgoing] > dualPhaseIUpperBound(B[outgoing])) {
                direction = -1.0;
            }
        } else {
            if (d[outgoing] > ub[B[outgoing]]) {
                direction = -1.0;
            }
        }
        for (int j = 0; j < cols; j++) {
            int col = V[j];
            if (phaseI) {
                if ((flags[col] & flagUB) != 0 && (flags[col] & flagLB) != 0) continue;
            } else {
                if ((flags[col] & flagFix) != 0) continue;
            }
            double v = Math.abs(wd[j]);
            double wdir = direction * wd[j];
            if (((flags[col] & flagNLB) != 0 && wdir < -epsPivot) || ((flags[col] & flagNUB) != 0 && wdir > epsPivot)) {
                double r = c[j] / wdir;
                if (r < minrr + epsZero && (r < minrr || v > minv)) {
                    minr[0] = c[j] / wd[j];
                    minrr = r;
                    minj[0] = j;
                    minv = v;
                }
            }
        }
    }

    private void selectDualIncomingWithBoundFlips(boolean phaseI, double delta, int[] minj, double[] minr) {
        minj[0] = -1;
        minr[0] = Double.POSITIVE_INFINITY;
        //double minv = 0.0;
        double eps = phaseI ? epsDualI : epsDual;
        double epsp = epsPivot;
        boolean positive = (delta > 0);
        if (!positive) delta = -delta;
        int available = 0;
        int allavailable = 0;
        boolean retried = false;

        double maxstep = Double.POSITIVE_INFINITY;
        retry: for (;;) {
            // mark eligible pivots and compute max step
            maxstep = Double.POSITIVE_INFINITY;
            for (int j = 0; j < cols; j++) {
                int col = V[j];
                flags[col] &= ~flagPivot2;
                if (phaseI) {
                    if ((flags[col] & flagUB) != 0 && (flags[col] & flagLB) != 0) {
                        flags[col] &= ~flagPivot;
                        continue;
                    }
                } else {
                    if ((flags[col] & flagFix) != 0) {
                        flags[col] &= ~flagPivot;
                        continue;
                    }
                }
                double wdir;
                if (positive)
                    wdir = -wd[j];
                else
                    wdir = wd[j];
                if (((flags[col] & flagNLB) != 0 && wdir < -epsp) || ((flags[col] & flagNUB) != 0 && wdir > epsp)) {
                    double r;
                    if (wdir > 0) {
                        r = (c[j] + eps) / wdir;
                    } else {
                        r = (c[j] - eps) / wdir;
                    }
                    if (r < maxstep) maxstep = r;
                    flags[col] |= flagPivot;
                    available++;
                    allavailable++;
                } else {
                    flags[col] &= ~flagPivot;
                }
            }
            if (available == 0 && Math.abs(delta) < epsPivot && !retried) {
                retried = true;
                epsp = 1.0e-7;
                continue retry;
            }
            break retry;
        }

        double deltamin = 0.0;
        double step = Math.max(maxstep * 10.0, eps);
        while (delta - deltamin > epsZero && allavailable > 0) {
            available = 0;
            delta -= deltamin;
            maxstep = Double.POSITIVE_INFINITY;
            deltamin = 0;
            for (int j = 0; j < cols; j++) {
                int col = V[j];
                flags[col] &= ~flagPivot2;
                if ((flags[col] & flagPivot) != 0) {
                    double wdir;
                    if (positive)
                        wdir = -wd[j];
                    else
                        wdir = wd[j];
                    double r;
                    double v = Math.abs(wd[j]);
                    if (wdir > 0) // upper bound
                    {
                        r = (c[j] + eps) / wdir;
                        if (c[j] - step * wdir < -eps) {
                            flags[col] &= ~flagPivot;
                            flags[col] |= flagPivot2;
                            available++;
                            allavailable--;
                            if (phaseI) {
                                deltamin += (dualPhaseIUpperBound(col) - dualPhaseILowerBound(col)) * v;
                            } else {
                                deltamin += (ub[col] - lb[col]) * v;
                            }
                        } else if (r < maxstep) maxstep = r;
                    } else // lower bound
                    {
                        r = (c[j] - eps) / wdir;
                        if (c[j] - step * wdir > eps) {
                            flags[col] &= ~flagPivot;
                            flags[col] |= flagPivot2;
                            available++;
                            allavailable--;
                            if (phaseI) {
                                deltamin += (dualPhaseIUpperBound(col) - dualPhaseILowerBound(col)) * v;
                            } else {
                                deltamin += (ub[col] - lb[col]) * v;
                            }
                        } else if (r < maxstep) maxstep = r;
                    }
                }
            }
            step = 2.0 * maxstep;
        }

        while (delta >= 0 && (available > 0 || delta > epsZero)) {
            maxstep = Double.POSITIVE_INFINITY;
            for (int j = 0; j < cols; j++) {
                int col = V[j];
                if ((flags[col] & flagPivot2) != 0) {
                    double wdir;
                    if (positive)
                        wdir = -wd[j];
                    else
                        wdir = wd[j];
                    double r;
                    if (wdir > 0) {
                        r = (c[j] + eps) / wdir;
                    } else {
                        r = (c[j] - eps) / wdir;
                    }
                    if (r < maxstep) maxstep = r;
                }
            }

            minj[0] = -1;
            double minv = 0.0;
            for (int j = 0; j < cols; j++) {
                int col = V[j];
                if ((flags[col] & flagPivot2) != 0) {
                    double wdir;
                    if (positive)
                        wdir = -wd[j];
                    else
                        wdir = wd[j];
                    double r = c[j] / wdir;
                    double v = Math.abs(wd[j]);
                    if (r <= maxstep) {
                        if (phaseI) {
                            delta -= (dualPhaseIUpperBound(col) - dualPhaseILowerBound(col)) * v;
                        } else {
                            delta -= (ub[col] - lb[col]) * v;
                        }
                        flags[col] &= ~flagPivot2;
                        available--;
                        if (v > minv) {
                            minv = v;
                            minj[0] = j;
                        }
                    }
                }
            }
            if (minj[0] == -1) return;
        }

        if (minj[0] != -1) {
            minr[0] = c[minj[0]] / wd[minj[0]];
        }
    }

    private void updatePrimal(double minr, int mini, int maxj) {
        // minr = primal step
        // rd = dual step

        double rd = c[maxj] / w[mini];

        // x = z <- e_mini*A_B^-1
        /*for (int i = 0; i < rows; i++)
        {
            x[i] = ((i == mini) ? 1.0 : 0.0);
        }
        lu.BSolve(x); // TODO exploit nature of x*/
        lu.bSolveUnit(x, mini);

        // update primal and dual
        if (Math.abs(minr) > epsZero) {
            for (int i = 0; i < rows; i++) {
                d[i] -= minr * w[i];
            }
        }
        if ((flags[V[maxj]] & flagNLB) != 0) {
            d[mini] = lb[V[maxj]] + minr;
        } else if ((flags[V[maxj]] & flagNUB) != 0) {
            d[mini] = ub[V[maxj]] + minr;
        }
        for (int j = 0; j < cols; j++) {
            int col = V[j];
            if (col < cols) {
                //double v = 0.0;
                int sCol1 = sparseCol[col];
                int sCol2 = sparseCol[col + 1];
                // TODO reinvestigate moving rd out of the loop once dual is more stable
                double sum = 0.0;
                for (int i = sCol1; i < sCol2; i++) {
                    sum += sparseValue[i] * x[sparseRow[i]];
                }
                for (int k = 0; k < numExtraConstraints; k++) {
                    sum += pD[k].get(col) * x[baseRows + k];
                }
                c[j] -= rd * sum;
            } else {
                c[j] -= rd * x[col - cols];
            }
        }
        c[maxj] = -rd;
    }

    private void updateDual(boolean phaseI, int minj, int mini, double minr, double delta, boolean updatec) {
        // w = U \ (L \ A(:,j));
        int mincol = V[minj];
        if (mincol < cols) {
            int ai = mincol * baseRows;
            int wi = 0;
            for (int i = 0; i < baseRows; i++, ai++, wi++) {
                w[wi] = a[ai];
            }
            for (int k = 0; k < numExtraConstraints; k++, wi++) {
                w[wi] = pD[k].get(mincol);
            }
        } else {
            //Array.Clear(_w, 0, rows);
            //w[mincol - cols] = 1.0;
            for (int i = 0; i < rows; i++) {
                w[i] = (i == mincol - cols) ? 1.0 : 0.0;
            }
        }
        //lu.FSolve(w);
        lu.fSolveL(w, ww, 0.00000001);
        lu.fSolveU(ww, w, 0.00000001);

        // -minr = dual step
        // rp = primal step

        //System.Diagnostics.Trace.WriteLine(w[mini] + " = " + wd[minj]);

        double l, u;
        if (phaseI) {
            u = dualPhaseIUpperBound(B[mini]);
            l = dualPhaseILowerBound(B[mini]);
        } else {
            u = ub[B[mini]];
            l = lb[B[mini]];
        }
        if (delta > 0) {
            delta = d[mini] - u;
        } else {
            delta = d[mini] - l;
        }
        double rp = delta / w[mini];

        // update primal and dual
        for (int i = 0; i < rows; i++) {
            d[i] -= rp * w[i];
        }
        if ((flags[mincol] & flagNLB) != 0) {
            d[mini] = (phaseI ? dualPhaseILowerBound(mincol) : lb[mincol]) + rp;
        } else if ((flags[mincol] & flagNUB) != 0) {
            d[mini] = (phaseI ? dualPhaseIUpperBound(mincol) : ub[mincol]) + rp;
        }
        if (updatec) {
            for (int j = 0; j < cols; j++) {
                c[j] -= minr * wd[j];
            }
            c[minj] = -minr;
        }
    }

    private void perturbCost() {
        NetRandom rnd = new NetRandom(0);
        for (int i = 0; i < cols; i++) {
            // usually fixed positions would not be perturbed, but in the context of MIP branching it's necessary to use the same perturbation
            // this wouldn't be an issue if the variable was nonbasic, but especially in MIP the fixed positions will
            // most often be on basic positions (for example when disabling a variable in a branch) and thus it's cost influences
            // reduced costs of all nonbasic variable which could lead to loss of feasibility
            //if ((flags[i] & flagFix) == 0)
            //{
            cost[i] -= 0.5 * (100.0 * epsDual + Math.abs(cost[i]) * 1.0e-5) * (1 + rnd.nextDouble()); // perturb in negative direction since all variables/slacks have lower bound
            //}
        }
        costWorkingDirty = true;
    }

    private void loadCost() {
        System.arraycopy(arraySet.cost, 0, arraySet.costWorking, 0, cols + rows);
        costWorkingDirty = false;
    }

    private boolean boundFlip(boolean phaseI, double dualStep, double eps, int incoming, boolean flipBounds) {
        boolean needDualI = false;
        boolean flipsDone = false;
        Arrays.fill(arraySet.x, 0, rows, 0.0);
        boolean modc = (Math.abs(dualStep) > epsZero && incoming != -1);
        for (int j = 0; j < cols; j++) {
            int col = V[j];
            if (!flipBounds) flags[col] &= ~flagFlip;
            if (j != incoming) {
                if ((flags[col] & flagFix) != 0) continue;
                if (modc) c[j] -= dualStep * wd[j];
                double v = 0.0;
                if ((flags[col] & flagNLB) != 0 && c[j] > eps) {
                    if (phaseI) {
                        // phase I flip
                        v = dualPhaseIUpperBound(col) - dualPhaseILowerBound(col);
                        if (flipBounds) flags[col] = (flags[col] | flagNUB) & ~flagNLB;
                        else flags[col] |= flagFlip;
                    } else if ((flags[col] & flagUB) != 0) {
                        // phase II flip
                        v = ub[col] - lb[col];
                        if (flipBounds) flags[col] = (flags[col] | flagNUB) & ~flagNLB;
                        else flags[col] |= flagFlip;
                    } else {
                        needDualI = true;
                    }
                } else if ((flags[col] & flagNUB) != 0 && c[j] < -eps) {
                    if (phaseI) {
                        // phase I flip
                        v = dualPhaseILowerBound(col) - dualPhaseIUpperBound(col);
                        if (flipBounds) flags[col] = (flags[col] | flagNLB) & ~flagNUB;
                        else flags[col] |= flagFlip;
                    } else if ((flags[col] & flagLB) != 0) {
                        // phase II flip
                        v = lb[col] - ub[col];
                        if (flipBounds) flags[col] = (flags[col] | flagNLB) & ~flagNUB;
                        else flags[col] |= flagFlip;
                    } else {
                        needDualI = true;
                    }
                }
                if (Math.abs(v) > epsZero) {
                    if (col < cols) {
                        int sCol1 = sparseCol[col];
                        int sCol2 = sparseCol[col + 1];
                        for (int i = sCol1; i < sCol2; i++) {
                            x[sparseRow[i]] += v * sparseValue[i];
                        }
                        for (int k = 0; k < numExtraConstraints; k++) {
                            x[baseRows + k] += v * pD[k].get(col);
                        }
                    } else {
                        x[col - cols] += v;
                    }
                    flipsDone = true;
                }
            }
        }
        if (incoming != -1) c[incoming] = -dualStep;
        if (flipsDone) {
            lu.fSolve(x);
            for (int i = 0; i < rows; i++) {
                d[i] -= x[i];
            }
        }
        return needDualI;
    }

    private void updateBounds() {
        for (int j = 0; j < cols; j++) {
            int col = V[j];
            if ((flags[col] & flagFlip) != 0) {
                //System.Diagnostics.Debug.WriteLine("Flipped " + col);
                if ((flags[col] & flagNLB) != 0) {
                    flags[col] = (flags[col] | flagNUB) & ~flagNLB;
                } else {
                    flags[col] = (flags[col] | flagNLB) & ~flagNUB;
                }
            }
        }
    }

    private boolean preprocessingSingletonConstraint() {
        boolean updated = false;
        // singleton row
        for (int i = 0; i < rows; i++) {
            //a[i + col * baseRows];
            double rhs = 0.0;
            int singlecol = -1;
            boolean issingleton = true;
            for (int col = 0; col < cols; col++) {
                double v;
                if (i < baseRows) {
                    v = a[i + col * baseRows];
                } else {
                    v = pD[i - baseRows].get(col);
                }
                if (Math.abs(v) > epsZero) {
                    if ((flags[col] & flagFix) != 0) {
                        rhs -= v * lb[col];
                    } else {
                        if (singlecol != -1) {
                            issingleton = false;
                            break;
                        } else {
                            singlecol = col;
                        }
                    }
                }
            }
            if (issingleton) {
                if (singlecol == -1) {
                    // all variables are fixed => row is fixed
                    updated |= updateLowerBound(cols + i, -rhs);
                    updated |= updateUpperBound(cols + i, -rhs);
                } else {
                    // update col bounds
                    // v * x + s = rhs
                    // lb <= s <= ub
                    // -ub <= -s <= -lb
                    // x = (rhs - s) / v
                    double v;
                    if (i < baseRows) {
                        v = a[i + singlecol * baseRows];
                    } else {
                        v = pD[i - baseRows].get(singlecol);
                    }
                    if (v > 0) {
                        // (rhs - ub) / v <= x = (rhs - s) / v <= (rhs - lb) / v
                        updated |= updateLowerBound(singlecol, (rhs - ub[cols + i]) / v);
                        updated |= updateUpperBound(singlecol, (rhs - lb[cols + i]) / v);
                    } else {
                        // (rhs - ub) / v >= x = (rhs - s) / v >= (rhs - lb) / v
                        updated |= updateUpperBound(singlecol, (rhs - ub[cols + i]) / v);
                        updated |= updateLowerBound(singlecol, (rhs - lb[cols + i]) / v);
                    }
                }
            }
        }
        return updated;
    }

    private boolean preprocessingTightenBounds() {
        boolean updated = false;
        // singleton row
        for (int i = 0; i < rows; i++) {
            //a[i + col * baseRows];
            double lbi = 0.0;
            double ubi = 0.0;
            for (int col = 0; col < cols; col++) {
                double v;
                if (i < baseRows) {
                    v = a[i + col * baseRows];
                } else {
                    v = pD[i - baseRows].get(col);
                }
                if (Math.abs(v) > epsZero) {
                    if (v > 0) {
                        lbi += v * lb[col];
                        ubi += v * ub[col];
                    } else {
                        lbi += v * ub[col];
                        ubi += v * lb[col];
                    }
                }
            }
            updated |= updateLowerBound(cols + i, -ubi);
            updated |= updateUpperBound(cols + i, -lbi);
            for (int col = 0; col < cols; col++) {
                double v;
                if (i < baseRows) {
                    v = a[i + col * baseRows];
                } else {
                    v = pD[i - baseRows].get(col);
                }
                if (Math.abs(v) > epsZero) {
                    if (v > 0) {
                        updated |= updateUpperBound(col, lb[col] - (lbi + lb[cols + i]) / v);
                        updated |= updateLowerBound(col, ub[col] - (ub[cols + i] + ubi) / v);
                    } else {
                        updated |= updateLowerBound(col, ub[col] - (lbi + lb[cols + i]) / v);
                        updated |= updateUpperBound(col, lb[col] - (ub[cols + i] + ubi) / v);
                    }
                }
            }
        }
        return updated;
    }

    private void preprocessing() {
        if (hardInfeasibility) return;
        boolean updated;
        do {
            updated = false;
            //updated |= PreprocessingSingletonConstraint();
            updated |= preprocessingTightenBounds();
        } while (updated && !hardInfeasibility);
    }

    /*
    private void InitializeDSEWeights(bool identity)
    {
        if (identity)
        {
            for (int i = 0; i < rows; i++)
            {
                beta[i] = 1.0;
            }
        }
        else
        {
            for (int j = 0; j < rows; j++)
            {
                for (int i = 0; i < rows; i++)
                {
                    x[i] = ((i == j) ? 1 : 0);
                }
                lu.BSolve(x); // TODO exploit nature of x
                beta[j] = 0.0;
                for (int i = 0; i < rows; i++)
                {
                    beta[j] += x[i] * x[i];
                }
            }
        }
    }*/

    private double[] solvePrimalUnsafe(boolean prepareForDual, boolean verifyRefactoredOptimality, boolean shortLimit, boolean highPrecision) {
        // LU = A_B
        // d = x_B <- A_B^-1*b ... primal solution
        // u = u <- c_B*A_B^-1 ... dual solution
        // w_N <- c_N - u*A_N  ... dual solution

        int limit = 5000;

        restart: for (;;) {
            int i, j, k;
            boolean feasible = false;
            int round = 0;
            int redecompose = 0;
            final int maxRedecompose = 50;
            int verificationAttempts = 0;
            double eps = highPrecision ? epsPrimal : epsPrimalLow;
            double lowestInfeasibility = Double.POSITIVE_INFINITY;
            int lastFeasible = 0;

            if (disabledDirty) {
                for (i = 0; i < cols + rows; i++) {
                    flags[i] &= ~flagDis;
                }
                disabledDirty = false;
            }

            iteration: do {
                // DECOMPOSE:
                if (redecompose <= 0) {
                    decompose();
                    redecompose = maxRedecompose; // decompose every 50 iterations to maintain stability
                    feasible = false; // when refactoring basis recompute the solution
                }
                if (lu.getSingular()) {
                    redecompose = 0;
                    patchSingularBasis();
                    continue iteration;
                }

                if (!feasible) {
                    computePrimalSolution(false);
                    feasible = isPrimalFeasible(eps);

                    if (feasible) {
                        lastFeasible = round;
                        // we have a feasible solution, initialize phase 2
                        computeReducedCosts();
                    } else if (shortLimit) {
                        // we're retuning dual optimality on unshifted/redecomposed basis
                        // if redecomposition caused loss of primal optimality then we probably
                        // won't be able to fix the optimality and doing so might just cause us problems
                        // in the dual
                        // abort and let dual handle the problems if we're also dual unfeasible
                        return null;
                    }
                }

                if (!feasible) {
                    double[] infeasibility = new double[1];
                    computePhaseIReducedCosts(infeasibility, eps);
                    if (infeasibility[0] < lowestInfeasibility) lowestInfeasibility = infeasibility[0];
                    else if (infeasibility[0] > lowestInfeasibility + eps) {
                        // we're not using a shifting strategy for primal so the only way to combat
                        // numerical cycling is to lower the tolerances
                        if (eps < 1.0) {
                            eps *= 10.0;
                            lowestInfeasibility = Double.POSITIVE_INFINITY;
                        }
                        if (redecompose < maxRedecompose) {
                            redecompose = 0;
                            continue iteration;
                        }
                    }
                    if (infeasibility[0] > 100.0 && !coldStart) {
                        // we're so far out of feasible region we're better off starting from scratch
                        for (i = 0; i < rows; i++) {
                            B[i] = cols + i;
                            flags[cols + i] = (flags[cols + i] | flagB) & ~flagN & ~flagDis;
                        }
                        for (j = 0; j < cols; j++) {
                            V[j] = j;
                            flags[j] = (flags[j] | flagNLB) & ~flagB & ~flagDis;
                        }
                        disabledDirty = false;
                        redecompose = 0;
                        coldStart = true;
                        continue iteration;
                    }
                }

                // MINISTEP:
                ministep: for (;;) {
                    double[] direction = new double[1];
                    int maxj = selectPrimalIncoming(direction, prepareForDual);

                    if (maxj == -1) {
                        if (feasible && verifyRefactoredOptimality && redecompose < maxRedecompose && verificationAttempts < 5) {
                            redecompose = 0;
                            feasible = false;
                            verificationAttempts++;
                            continue iteration;
                        }
                        /*if (blacklistAllowed)
                        {
                            bool retry = false;
                            if (disabledDirty)
                            {
                                for (i = 0; i < cols + rows; i++)
                                {
                                    if ((flags[i] & flagDis) != 0)
                                    {
                                        flags[i] &= ~flagDis;
                                        retry = true;
                                    }
                                }
                                disabledDirty = false;
                            }
                            if (retry)
                            {
                                blacklistAllowed = false;
                                redecompose = 0;
                                needsRecalc = true;
                                continue;
                            }
                        }*/

                        // rebuild solution so it's stable
                        //ComputePrimalSolution();

                        // optimum, return solution (or could be no feasible solution)
                        if (!feasible) return new double[cols + 1]; // if it's not feasible then return null solution
                        coldStart = false;
                        //System.Diagnostics.Trace.WriteLine("Primal optimality in round " + round);
                        return computeReturnSolution();
                    }

                    int mini = -1;
                    double minr = Double.POSITIVE_INFINITY;
                    int bound = 0;

                    computeBasisStep(maxj);

                    int[] refMini = new int[] { mini };
                    double[] refMinr = new double[] { minr };
                    int[] refBound = new int[] { bound };
                    if (!selectPrimalOutgoing(direction[0], feasible, eps, refMini, refMinr, refBound)) {
                        feasible = false;
                        lowestInfeasibility = Double.POSITIVE_INFINITY;
                        redecompose = 0;
                        continue iteration;
                    }
                    mini = refMini[0];
                    minr = refMinr[0];
                    bound = refBound[0];

                    if (mini == -1) {
                        // unbounded
                        if (!coldStart) {
                            // something went really horribly wrong, try from scratch
                            for (i = 0; i < rows; i++) {
                                B[i] = cols + i;
                                flags[cols + i] = (flags[cols + i] | flagB) & ~flagN & ~flagDis;
                            }
                            for (j = 0; j < cols; j++) {
                                V[j] = j;
                                flags[j] = (flags[j] | flagNLB) & ~flagB & ~flagDis;
                            }
                            redecompose = 0;
                            coldStart = true;
                            continue iteration;
                        }
                        // completely unstable, investigate
                        double[] ret = new double[cols + 1];
                        return ret;
                    }

                    // determine if we do a basis change or just a bound swap
                    int col = V[maxj];
                    if ((flags[col] & (flagLB | flagUB)) == (flagLB | flagUB) && Math.abs(minr) >= ub[col] - lb[col] - epsZero) {
                        // do bound swap
                        if (direction[0] > 0.0) {
                            flags[col] = (flags[col] | flagNUB) & ~flagNLB;
                        } else {
                            flags[col] = (flags[col] | flagNLB) & ~flagNUB;
                        }

                        if (!feasible) {
                            round++; // still increment to avoid infinite cycles
                            continue iteration;
                        }

                        minr = direction[0] * (ub[col] - lb[col]);
                        for (i = 0; i < rows; i++) {
                            d[i] -= minr * w[i];
                        }

                        continue ministep;
                    }

                    if (feasible) {
                        updatePrimal(minr, mini, maxj);
                    }

                    // swap base

                    redecompose--;
                    if (redecompose > 0) {
                        double[] pivot = new double[1];
                        lu.update(ww, mini, pivot);
                        if (lu.getSingular() || Math.abs(w[mini] - pivot[0]) > 1.0e-6 * Math.abs(w[mini]) + epsZero) {
                            if (redecompose == maxRedecompose - 1) {
                                redecompose = 0;
                                feasible = false; // forces recalc
                                flags[V[maxj]] |= flagDis;
                                disabledDirty = true;
                                continue iteration;
                            } else {
                                redecompose = 0;
                                feasible = false;
                                continue iteration;
                            }
                        }
                    }

                    k = B[mini];
                    B[mini] = V[maxj];
                    V[maxj] = k;
                    flags[B[mini]] = (flags[B[mini]] | flagB) & ~flagN;
                    flags[V[maxj]] = (flags[V[maxj]] | bound) & ~flagB;

                    round++;
                    break ministep;
                }
            } while ((round < limit && !shortLimit) || round < 100); // limit computation so we don't dead loop, if everything works it shouldn't take more than this
            // when tuning dual feasibility limit to 100 rounds, we don't want to spend too much time on it

            // just in case
            // if feasible return the best we got
            if (feasible) return computeReturnSolution();
            if (lastFeasible > 0 && !shortLimit) {
                limit = lastFeasible;
                for (i = 0; i < rows; i++) {
                    B[i] = cols + i;
                    flags[cols + i] = (flags[cols + i] | flagB) & ~flagN & ~flagDis;
                }
                for (j = 0; j < cols; j++) {
                    V[j] = j;
                    flags[j] = (flags[j] | flagNLB) & ~flagB & ~flagDis;
                }
                continue restart;
            }
            return new double[cols + 1];
        }
    }

    private double[] solvePrimalQuadraticUnsafe() {
        // LU = A_B
        // d = x_B <- A_B^-1*b ... primal solution
        // u = u <- c_B*A_B^-1 ... dual solution
        // w_N <- c_N - u*A_N  ... dual solution

        int limit = 5000;

        restart: for (;;) {
            int i, j, k;
            boolean feasible = false;
            int round = 0;
            int redecompose = 0;
            final int maxRedecompose = 50;
            double eps = epsPrimal;
            double lowestInfeasibility = Double.POSITIVE_INFINITY;
            int lastFeasible = 0;

            if (disabledDirty) {
                for (i = 0; i < cols + rows; i++) {
                    flags[i] &= ~flagDis;
                }
                disabledDirty = false;
            }

            iteration: do {
                // DECOMPOSE:
                if (redecompose <= 0) {
                    decompose();
                    redecompose = maxRedecompose; // decompose every 50 iterations to maintain stability
                    feasible = false; // when refactoring basis recompute the solution
                }
                if (lu.getSingular()) {
                    redecompose = 0;
                    patchSingularBasis();
                    continue iteration;
                }

                if (!feasible) {
                    computePrimalSolution(false);
                    feasible = isPrimalFeasible(eps);

                    if (feasible) {
                        lastFeasible = round;
                    }
                }

                if (!feasible) {
                    double[] infeasibility = new double[1];
                    computePhaseIReducedCosts(infeasibility, eps);
                    if (infeasibility[0] < lowestInfeasibility) lowestInfeasibility = infeasibility[0];
                    else if (infeasibility[0] > lowestInfeasibility + eps) {
                        // we're not using a shifting strategy for primal so the only way to combat
                        // numerical cycling is to lower the tolerances
                        if (eps < 1.0) {
                            eps *= 10.0;
                            lowestInfeasibility = Double.POSITIVE_INFINITY;
                        }
                        if (redecompose < maxRedecompose) {
                            redecompose = 0;
                            continue iteration;
                        }
                    }
                    if (infeasibility[0] > 100.0 && !coldStart) {
                        // we're so far out of feasible region we're better off starting from scratch
                        for (i = 0; i < rows; i++) {
                            B[i] = cols + i;
                            flags[cols + i] = (flags[cols + i] | flagB) & ~flagN & ~flagDis;
                        }
                        for (j = 0; j < cols; j++) {
                            V[j] = j;
                            flags[j] = (flags[j] | flagNLB) & ~flagB & ~flagDis;
                        }
                        disabledDirty = false;
                        redecompose = 0;
                        coldStart = true;
                        continue iteration;
                    }
                }

                // MINISTEP:
                ministep: for (;;) {
                    if (feasible) {
                        updateMB();
                        computeReducedCostGradient();
                    }

                    double[] direction = new double[1];
                    int maxj = selectPrimalIncoming(direction, false);

                    if (maxj == -1) {
                        // TODO verify optimality
                        // so far we only know it's a KKT point
                        /*if (feasible && verifyRefactoredOptimality && redecompose < maxRedecompose && verificationAttempts < 5)
                        {
                            redecompose = 0;
                            feasible = false;
                            verificationAttempts++;
                            continue;
                        }*/
                        /*if (blacklistAllowed)
                        {
                            bool retry = false;
                            if (disabledDirty)
                            {
                                for (i = 0; i < cols + rows; i++)
                                {
                                    if ((flags[i] & flagDis) != 0)
                                    {
                                        flags[i] &= ~flagDis;
                                        retry = true;
                                    }
                                }
                                disabledDirty = false;
                            }
                            if (retry)
                            {
                                blacklistAllowed = false;
                                redecompose = 0;
                                needsRecalc = true;
                                continue;
                            }
                        }*/

                        // rebuild solution so it's stable
                        //ComputePrimalSolution();

                        // optimum, return solution (or could be no feasible solution)
                        if (!feasible) return new double[cols + 1]; // if it's not feasible then return null solution
                        coldStart = false;
                        //System.Diagnostics.Trace.WriteLine("Primal optimality in round " + round);
                        return computeReturnSolutionQuadratic();
                    }

                    computeBasisStep(maxj);

                    int mini = -1;
                    double minr = computeQuadraticLimit(maxj, direction[0]);
                    int bound = flagNMid;

                    int[] refMini = new int[] { mini };
                    double[] refMinr = new double[] { minr };
                    int[] refBound = new int[] { bound };
                    if (!selectPrimalOutgoing(direction[0], feasible, eps, refMini, refMinr, refBound)) {
                        feasible = false;
                        lowestInfeasibility = Double.POSITIVE_INFINITY;
                        redecompose = 0;
                        continue iteration;
                    }
                    mini = refMini[0];
                    minr = refMinr[0];
                    bound = refBound[0];

                    /*if (mini == -1)
                    {
                        // unbounded
                        if (!ColdStart)
                        {
                            // something went really horribly wrong, try from scratch
                            for (i = 0; i < rows; i++)
                            {
                                B[i] = cols + i;
                                flags[cols + i] = (flags[cols + i] | flagB) & ~flagN & ~flagDis;
                            }
                            for (j = 0; j < cols; j++)
                            {
                                V[j] = j;
                                flags[j] = (flags[j] | flagNLB) & ~flagB & ~flagDis;
                            }
                            redecompose = 0;
                            ColdStart = true;
                            continue;
                        }
                        // completely unstable, investigate
                        double[] ret = new double[cols + 1];
                        return ret;
                    }*/

                    // determine if we do a basis change or just a bound swap
                    int col = V[maxj];
                    double dist;
                    if (direction[0] > 0.0) {
                        dist = ub[col] - mb[col];
                    } else {
                        dist = mb[col] - lb[col];
                    }
                    boolean isBound = false;
                    if (Math.abs(minr) >= dist - epsZero) {
                        if (direction[0] > 0.0) {
                            flags[col] = (flags[col] & ~flagN) | flagNUB;
                        } else {
                            flags[col] = (flags[col] & ~flagN) | flagNLB;
                        }
                        isBound = true;
                    } else if (mini == -1) {
                        flags[col] = (flags[col] & ~flagN) | flagNMid;
                        mb[col] = mb[col] + direction[0] * minr;
                        dist = minr;
                        isBound = true;
                    }
                    if (isBound) {
                        if (!feasible) {
                            round++; // still increment to avoid infinite cycles
                            continue iteration;
                        }

                        minr = direction[0] * dist;
                        for (i = 0; i < rows; i++) {
                            d[i] -= minr * w[i];
                        }

                        continue ministep;
                    }

                    // UPDATE:

                    if (feasible) {
                        updatePrimal(minr, mini, maxj);
                    }

                    // swap base

                    redecompose--;
                    if (redecompose > 0) {
                        double[] pivot = new double[1];
                        lu.update(ww, mini, pivot);
                        if (lu.getSingular() || Math.abs(w[mini] - pivot[0]) > 1.0e-6 * Math.abs(w[mini])) {
                            if (redecompose == maxRedecompose - 1) {
                                redecompose = 0;
                                feasible = false; // forces recalc
                                flags[V[maxj]] |= flagDis;
                                disabledDirty = true;
                                continue iteration;
                            } else {
                                redecompose = 0;
                                feasible = false;
                                continue iteration;
                            }
                        }
                    }

                    k = B[mini];
                    B[mini] = V[maxj];
                    V[maxj] = k;
                    flags[B[mini]] = (flags[B[mini]] | flagB) & ~flagN;
                    flags[V[maxj]] = (flags[V[maxj]] | bound) & ~flagB;

                    round++;
                    break ministep;
                }
            } while (round < limit); // limit computation so we don't dead loop, if everything works it shouldn't take more than this
            // when tuning dual feasibility limit to 100 rounds, we don't want to spend too much time on it

            // just in case
            // if feasible return the best we got
            if (feasible) return computeReturnSolution();
            if (lastFeasible > 0) {
                limit = lastFeasible;
                for (i = 0; i < rows; i++) {
                    B[i] = cols + i;
                    flags[cols + i] = (flags[cols + i] | flagB) & ~flagN & ~flagDis;
                }
                for (j = 0; j < cols; j++) {
                    V[j] = j;
                    flags[j] = (flags[j] | flagNLB) & ~flagB & ~flagDis;
                }
                continue restart;
            }
            return new double[cols + 1];
        }
    }

    private void nullSpaceReduce(double[] dest, double[] source) {
        //dest = Z*ZT*source

        // Z = [-Binv*S, I, 0]

        //Zero(dest, cols + rows);

        // ZT*source

        // (sourceS - (Binv*S)T*sourceB)
        // (sourceS - ST*BinvT*sourceB)
        // BinvT*sourceB = x
        // sourceBT*Binv = xT
        for (int i = 0; i < rows; i++) {
            int col = B[i];
            x[i] = source[col];
        }
        lu.bSolve(x, epsZero);

        for (int s = 0; s < superCount; s++) {
            int col = S[s];
            double v = source[col];

            if (col < cols) {
                int sCol1 = sparseCol[col];
                int sCol2 = sparseCol[col + 1];
                for (int i = sCol1; i < sCol2; i++) {
                    v -= sparseValue[i] * x[sparseRow[i]];
                }
                for (int k = 0; k < numExtraConstraints; k++) {
                    v -= pD[k].get(col) * x[baseRows + k];
                }
            } else {
                v -= x[col - cols];
            }

            dest[col] = v;
        }
    }

    private void nullSpaceExpand(double[] dest) {
        //dest = Z*ZT*source
        //destB = -Binv*S*destS

        // S*destS
        zero(x, rows);
        for (int s = 0; s < superCount; s++) {
            int col = S[s];
            double v = dest[col];

            if (col < cols) {
                int sCol1 = sparseCol[col];
                int sCol2 = sparseCol[col + 1];
                for (int i = sCol1; i < sCol2; i++) {
                    x[sparseRow[i]] += sparseValue[i] * v;
                }
                for (int k = 0; k < numExtraConstraints; k++) {
                    x[baseRows + k] += pD[k].get(col) * v;
                }
            } else {
                x[col - cols] += v;
            }
        }

        lu.fSolve(x, epsZero);

        for (int i = 0; i < rows; i++) {
            int col = B[i];
            dest[col] = -x[i];
        }
    }

    private void updateOSB() {
        int i = 0;
        for (int j = 0; j < cols; j++) {
            int sortj = sort[j];
            if ((flags[sortj] & flagSB) != 0) {
                OSB[i] = sortj;
                i++;
            }
        }
        osbStructural = i;
        for (int j = cols; j < cols + rows; j++) {
            if ((flags[j] & flagSB) != 0) {
                OSB[i] = j;
                i++;
            }
        }
        osbDirty = false;
    }

    private boolean computeCGStep(double eps, double[] maxAlpha, boolean useReducedGradient, boolean[] needsReducedGradientStep) {
        // compute CG step for superbasic variables
        // see ftp://ftp.numerical.rl.ac.uk/pub/reports/ghnRAL98069.ps.gz

        // INITIALIZE
        int hardLimit = superCount * 5;
        needsReducedGradientStep[0] = false;

        restart: for (;;) {

            //x = 0
            zero(cg_x, cols + rows);

            zero(cg_p, cols + rows);

            if (osbDirty) {
                updateOSB();
            }

            //r = g
            for (int i = 0; i < osbStructural; i++) {
                int col = OSB[i];
                cg_r[col] = cost[col] - qk * qx[col];
            }
            for (int i = osbStructural; i < superCount + rows; i++) {
                int col = OSB[i];
                cg_r[col] = cost[col];
            }
            // we only use r up to cols, and we only need the basic/superbasic components

            //w = Z*ZT*r
            if (useReducedGradient) {
                for (int i = 0; i < cols; i++) {
                    cg_w[V[i]] = c[i];
                }
            } else {
                nullSpaceReduce(cg_w, cg_r);
            }

            // verify gradient on boundary, if some superbasic is on boundary and has gradient in
            // opposite direction we'll want to remove it from superbasic, so that the truncate tests
            // are meaningful
            for (int i = 0; i < superCount; i++) {
                int col = S[i];

                if (ub[col] - mb[col] < Math.abs(ub[col]) * epsPrimalRel + epsPrimalLow) {
                    if (ub[col] - (mb[col] + cg_w[col]) < -Math.abs(ub[col]) * epsPrimalRel - epsPrimalLow) {
                        cg_x[col] = 1.0;
                        nullSpaceExpand(cg_x);
                        maxAlpha[0] = Double.POSITIVE_INFINITY;
                        return true;
                    }
                }
                if (mb[col] - lb[col] < Math.abs(lb[col]) * epsPrimalRel + epsPrimalLow) {
                    if ((mb[col] + cg_w[col]) - lb[col] < -Math.abs(lb[col]) * epsPrimalRel - epsPrimalLow) {
                        cg_x[col] = -1.0;
                        nullSpaceExpand(cg_x);
                        maxAlpha[0] = Double.POSITIVE_INFINITY;
                        return true;
                    }
                }
            }

            //p = w
            //Copy(cg_p, cg_w, cols + rows);
            for (int i = 0; i < superCount; i++) {
                int col = S[i];
                cg_p[col] = cg_w[col];
            }
            //ValidateCGFeasibility2();

            maxAlpha[0] = 1.0;

            // ITERATE

            double rtw = 0.0;
            /*for (int i = 0; i < cols; i++)
            {
                rtw += cg_r[i] * cg_w[i];
            }*/
            // r*w = rT*Z*ZT*r = (ZT*r)T * (ZT*r)
            // since Z=[-Binv*S, I, 0] we can just check the superbasic components of w
            // this'll be faster and also more accurate because for very small r
            // solving with basis might give bad results because of snapping to zero
            for (int i = 0; i < superCount; i++) {
                int col = S[i];
                rtw += cg_w[col] * cg_w[col];
            }

            double limit = eps * eps;

            if (rtw <= limit) {
                return false;
            }

            double minrtw = rtw;
            double startrtw = rtw;
            int minstep = -1;

            int step = 0;

            do {
                //alpha = -(rT * w) / (pT * Q * p)
                nullSpaceExpand(cg_p);
                quadraticQOSB(cg_p, cg_qp);
                double pqp = 0.0;
                for (int i = 0; i < osbStructural; i++) {
                    int col = OSB[i];
                    pqp += cg_qp[col] * cg_p[col];
                }
                pqp *= qk;
                if (pqp < limit) {
                    // we found a direction of negative curvature (positive in the maximization context)
                    // or possibly linear descent direction
                    // CG won't work in this case, i.e. it'll try to go opposite of gradient
                    // if we've done some steps before encountering negative curvature just use
                    // that as a good approximation
                    // otherwise return this feasible direction along which we'll have unbounded increase
                    if (pqp < -limit) {
                        // definitely negative curvature
                        // use this as direction for line search until we hit boundary
                        // determine direction based on sign of gradient along this direction
                        // normalize size
                        normalizeCGStep(cg_p);
                        double grad = computeGradient(cg_p);
                        if (grad < -limit) {
                            // negative gradient, we should use the opposite direction
                            for (int i = 0; i < rows + superCount; i++) {
                                int col = OSB[i];
                                cg_x[col] = -cg_p[col];
                            }
                        } else {
                            for (int i = 0; i < rows + superCount; i++) {
                                int col = OSB[i];
                                cg_x[col] = cg_p[col];
                            }
                        }
                        // verify that the selected direction has correct direction on boundary
                        // otherwise we're in danger of infinite loop
                        if (trunkateBoundary(cg_x)) {
                            // we have to reverify that this is still direction of negative gradient
                            quadraticQOSB(cg_x, cg_qp);
                            pqp = 0.0;
                            for (int i = 0; i < osbStructural; i++) {
                                int col = OSB[i];
                                pqp += cg_qp[col] * cg_x[col];
                            }
                            pqp *= qk;

                            grad = computeGradient(cg_x);

                            if (pqp > -limit || grad < -limit) {
                                // we have a negative descent, but we can't make it feasible easily
                                // in this case we have to fall back to reduced gradient step
                                // so the we move into interior away from boundary
                                needsReducedGradientStep[0] = true;
                                return false;
                            }
                        }
                        maxAlpha[0] = Double.POSITIVE_INFINITY;
                        return true;
                    } else {
                        // we have zero curvature
                        // we're either at optimum or we have unbounded linear descent
                        if (step == 0) {
                            // things depend on the gradient
                            if (rtw > 0.001) {
                                // gradient is significantly steep that we can treat this
                                // as unbounded linear descent
                                // this is in direction of gradient so we know it has to be a feasible direction
                                maxAlpha[0] = Double.POSITIVE_INFINITY;
                                for (int i = 0; i < rows + superCount; i++) {
                                    int col = OSB[i];
                                    cg_x[col] = cg_p[col];
                                }
                                return true;
                            } else {
                                // assume we're close to optimal that it won't matter
                                // if there are other optimal directions we'll get better gradient next time
                                needsReducedGradientStep[0] = true;
                                return false;
                            }
                        } else {
                            // we already computed some progress to be made, so use that
                            // if after making that we encounter linear descent we'll
                            // find this in subsequent step
                            return true;
                        }
                    }
                }
                double alpha = rtw / pqp;

                if (alpha > 100.0) {
                    // relative steepness is very small
                    // this could result in a very long step
                    // we try not to go too much out of feasible range, which is usually in the order of several hundred per variable
                    double infnorm = 0.0;
                    for (int i = 0; i < rows + superCount; i++) {
                        int col = OSB[i];
                        double v = Math.abs(cg_p[col]);
                        if (v > infnorm) {
                            infnorm = v;
                        }
                    }

                    // if we're too large cancel based on our prediction if we made any progress
                    if (alpha * infnorm > 1000.0) {
                        if (trunkateBoundary(cg_p)) {
                            quadraticQOSB(cg_p, cg_qp);
                            pqp = 0.0;
                            for (int i = 0; i < osbStructural; i++) {
                                int col = OSB[i];
                                pqp += cg_qp[col] * cg_p[col];
                            }
                            pqp *= qk;
                        }

                        // check if it's safe to treat this as linear descent
                        double grad = computeGradient(cg_p);
                        double t = 100 / infnorm;
                        if (Math.abs(grad) * t - 0.5 * t * t * pqp > 0) {
                            if (grad < 0) {
                                // negative gradient, we should use the opposite direction
                                for (int i = 0; i < rows + superCount; i++) {
                                    int col = OSB[i];
                                    cg_x[col] = -cg_p[col];
                                }
                            } else {
                                for (int i = 0; i < rows + superCount; i++) {
                                    int col = OSB[i];
                                    cg_x[col] = cg_p[col];
                                }
                            }
                            maxAlpha[0] = Double.POSITIVE_INFINITY;
                            return true;
                        }

                        if (rtw > startrtw) {
                            needsReducedGradientStep[0] = true;
                            return false;
                        } else {
                            return true;
                        }
                    }
                }

                //ValidateCGFeasibility();
                //ValidateCGFeasibility2();

                //x = x + alpha * p
                for (int i = 0; i < rows + superCount; i++) {
                    int col = OSB[i];
                    cg_x[col] += alpha * cg_p[col];
                }

                //ValidateCGFeasibility();

                //r~ = r + alpha * Q * p
                for (int i = 0; i < osbStructural; i++) {
                    int col = OSB[i];
                    cg_rr[col] = cg_r[col] - alpha * qk * cg_qp[col];
                }

                //w~ = Z*ZT*r~
                nullSpaceReduce(cg_ww, cg_rr);

                //beta = (r~T * w~) / (rT * w)
                double rtwnew = 0.0;
                /*for (int i = 0; i < cols; i++)
                {
                    rtwnew += cg_rr[i] * cg_ww[i];
                }*/
                for (int i = 0; i < superCount; i++) {
                    int col = S[i];
                    rtwnew += cg_ww[col] * cg_ww[col];
                }
                step++;
                if (rtwnew <= limit) {
                    return true;
                }
                if (step > hardLimit || (step >= superCount && rtwnew < 2 * minrtw)) {
                    if (rtwnew > startrtw) {
                        if (minrtw < 0.1 * startrtw) {
                            // we have a possibility of improving the value if we terminate early
                            hardLimit = minstep - 1;
                            continue restart;
                        }
                        needsReducedGradientStep[0] = true;
                        return false;
                    }
                    if (step > superCount) {
                        if (rtwnew < eps) {
                            return true;
                        }
                        // often when we come here we're dealing with numerical instabilities
                        // it can happen that some of superbasics that start on boundary will have
                        // their value in opposite direction of boundary despite gradient being in
                        // the right direction
                        // if this happens it's better to project that direction out since
                        // otherwise we can get in an endless loop of adding/removing that variable
                        // from superbasis
                        trunkateBoundary(cg_x);

                        // verify if generated direction has good gradient and curvature
                        quadraticQOSB(cg_x, cg_qp);
                        pqp = 0.0;
                        for (int i = 0; i < osbStructural; i++) {
                            int col = OSB[i];
                            pqp += cg_qp[col] * cg_x[col];
                        }
                        pqp *= qk;
                        double grad = 0.0;
                        for (int i = 0; i < osbStructural; i++) {
                            int col = OSB[i];
                            grad += (cost[col] - qk * qx[col]) * cg_x[col];
                        }
                        if (pqp < -limit) {
                            // negative curvature
                            if (grad < -limit) {
                                // negative gradient, we should use the opposite direction
                                for (int i = 0; i < rows + superCount; i++) {
                                    int col = OSB[i];
                                    cg_x[col] = -cg_x[col];
                                }
                            }
                            maxAlpha[0] = Double.POSITIVE_INFINITY;
                            return true;
                        } else {
                            // zero or positive curvature
                            // make sure gradient is ok
                            if (grad < -limit) {
                                needsReducedGradientStep[0] = true;
                                return false;
                            }
                            // make sure alpha = 1 is ok
                            // df = t * grad - 0.5 * t^2 * pqp
                            if (grad - 0.5 * pqp < -limit) {
                                needsReducedGradientStep[0] = true;
                                return false;
                            }
                            return true;
                        }
                    }
                    return true;
                }
                if (rtwnew < minrtw) {
                    minstep = step;
                    minrtw = rtwnew;
                }
                double beta = rtwnew / rtw;

                //p = w~ + beta*p
                /*for (int i = 0; i < cols + rows; i++)
                {
                    cg_p[i] = cg_ww[i] + beta * cg_p[i];
                }*/
                for (int i = 0; i < superCount; i++) {
                    int col = S[i];
                    cg_p[col] = cg_ww[col] + beta * cg_p[col];
                }
                //ValidateCGFeasibility2();

                //w = w~
                //r = r~
                rtw = rtwnew;
                double[] tmp = cg_w;
                cg_w = cg_ww;
                cg_ww = tmp;
                tmp = cg_r;
                cg_r = cg_rr;
                cg_rr = tmp;
            } while (true);
        }
    }

    private double computeGradient(double[] step) {
        double grad = 0.0;
        for (int i = 0; i < osbStructural; i++) {
            int col = OSB[i];
            grad += (cost[col] - qk * qx[col]) * step[col];
        }
        return grad;
    }

    private void normalizeCGStep(double[] step) {
        double infnorm = 0.0;
        for (int i = 0; i < rows + superCount; i++) {
            int col = OSB[i];
            double v = Math.abs(step[col]);
            if (v > infnorm) {
                infnorm = v;
            }
        }
        if (infnorm > 1000.0) {
            for (int i = 0; i < rows + superCount; i++) {
                int col = OSB[i];
                step[col] /= infnorm;
            }
        }
    }

    private boolean trunkateBoundary(double[] step) {
        boolean dirty = false;
        for (int i = 0; i < superCount; i++) {
            int col = S[i];

            if (ub[col] - mb[col] < Math.abs(ub[col]) * epsPrimalRel + epsPrimalLow) {
                if (ub[col] - (mb[col] + step[col]) < -Math.abs(ub[col]) * epsPrimalRel - epsPrimalLow) {
                    step[col] = 0;
                    dirty = true;
                }
            }
            if (mb[col] - lb[col] < Math.abs(lb[col]) * epsPrimalRel + epsPrimalLow) {
                if ((mb[col] + step[col]) - lb[col] < -Math.abs(lb[col]) * epsPrimalRel - epsPrimalLow) {
                    step[col] = 0;
                    dirty = true;
                }
            }
        }
        if (dirty) {
            nullSpaceExpand(step);
            return true;
        }
        return false;
    }

    private boolean copositivityDetection(double eps) {
        // verify second order optimality conditions
        // based on http://iwi.eldoc.ub.rug.nl/FILES/root/2008/LinAlgApplBundfuss/2008LinAlgApplBundfuss.pdf

        zero(cg_x, cols + rows);
        zero(cg_p, cols + rows);

        // add all zero reduced gradient nonbasics into superbasis
        // they'll form the basis of simplex

        computeQx();

        for (int j = 0; j < cols; j++) {
            int col = V[j];
            if (Math.abs(c[j]) < eps && (flags[col] & flagNMid) == 0) {
                S[superCount] = col;
                superCount++;
                flags[col] = (flags[col] & ~flagN) | flagNMid;
            }
        }

        int maxv = superCount * 2;
        int maxs = 10;
        double[] vector = new double[maxv * superCount]; // contains superbasic components of simplex vectors vi
        double[] cross = new double[maxv * maxv]; // contains products viT * Q * vj
        int[] simplex = new int[maxs * superCount]; // contains simplexes with oriented vector indexes

        updateOSB();

        // initialize root vertexes
        int vCount = 1;
        for (int i = 0; i < superCount; i++) {
            vector[vCount * superCount + i] = 1.0;
            vCount++;
        }

        // flag products as not computed
        for (int i = 1; i <= superCount; i++) {
            for (int j = i; j <= superCount; j++) {
                cross[(j - 1) * j / 2 + i] = Double.NaN;
            }
        }

        // initialize root simplexes
        int simCount = 0;
        int combinations = (1 << superCount);
        for (int i = 0; i < combinations; i++) {
            boolean valid = true;
            for (int j = 0; j < superCount; j++) {
                int col = S[j];
                if ((i & (1 << j)) != 0) {
                    if (mb[col] - lb[col] < Math.abs(lb[col]) * epsPrimalRel + epsPrimalLow) {
                        valid = false;
                        break;
                    }
                } else {
                    if (ub[col] - mb[col] < Math.abs(ub[col]) * epsPrimalRel + epsPrimalLow) {
                        valid = false;
                        break;
                    }
                }
            }
            if (valid) {
                if (simCount + 1 > maxs) {
                    maxs += 10;
                    simplex = Arrays.copyOf(simplex, maxs * superCount);
                }
                for (int j = 0; j < superCount; j++) {
                    if ((i & (1 << j)) != 0) {
                        simplex[simCount * superCount + j] = -(j + 1);
                    } else {
                        simplex[simCount * superCount + j] = (j + 1);
                    }
                }
                simCount++;
            }
        }

        while (simCount > 0) {
            int off = (simCount - 1) * superCount;

            final double limit = -1.0;
            double min = limit; // a rough detection, but we're not multiplying with Qk
            int mini = 0;
            int minj = 0;

            int vi;
            int vj;


            // find minimum product
            for (int i = 0; i < superCount; i++) {
                for (int j = i; j < superCount; j++) {
                    vi = simplex[off + i];
                    vj = simplex[off + j];

                    double sign = (vi * vj < 0) ? -1.0 : 1.0;

                    if (vi < 0) vi = -vi;
                    if (vj < 0) vj = -vj;
                    int index;
                    if (vi < vj) {
                        index = (vj - 1) * vj / 2 + vi;
                    } else {
                        index = (vi - 1) * vi / 2 + vj;
                    }

                    if (Double.isNaN(cross[index])) {
                        for (int s = 0; s < superCount; s++) {
                            int col = S[s];
                            cg_p[col] = vector[vi * superCount + s];
                        }
                        nullSpaceExpand(cg_p);
                        quadraticQOSB(cg_p, cg_qp);
                        for (int s = 0; s < superCount; s++) {
                            int col = S[s];
                            cg_p[col] = vector[vj * superCount + s];
                        }
                        nullSpaceExpand(cg_p);
                        double pqp = 0.0;
                        for (int k = 0; k < osbStructural; k++) {
                            int col = OSB[k];
                            pqp += cg_qp[col] * cg_p[col];
                        }
                        cross[index] = pqp;
                    }

                    double v = sign * cross[index];

                    if (i == j && v < -eps) {
                        for (int s = 0; s < superCount; s++) {
                            int col = S[s];
                            cg_x[col] = vector[vi * superCount + s];
                        }
                        nullSpaceExpand(cg_x);
                        return true;
                    }

                    if (v < min) {
                        min = v;
                        mini = i;
                        minj = j;
                    }
                }
            }

            if (min == limit) {
                // remove simplex
                simCount--;
                continue;
            }

            // select splitting point
            vi = simplex[off + mini];
            vj = simplex[off + minj];
            double signi = (vi < 0) ? -1.0 : 1.0;
            double signj = (vj < 0) ? -1.0 : 1.0;
            if (vi < 0) vi = -vi;
            if (vj < 0) vj = -vj;
            double alpha = cross[(vi - 1) * vi / 2 + vi];
            double beta = cross[(vj - 1) * vj / 2 + vj];
            double gamma = min;

            double k1 = gamma / (gamma - alpha);
            double k2 = beta / (beta - gamma);

            if (k1 > k2 + epsZero) {
                if (alpha < eps && beta < eps) {
                    for (int s = 0; s < superCount; s++) {
                        int col = S[s];
                        cg_x[col] = (signi * vector[vi * superCount + s] + signj * vector[vj * superCount + s]) / 2;
                    }
                    nullSpaceExpand(cg_x);
                    return true;
                } else if (alpha >= eps) {
                    for (int s = 0; s < superCount; s++) {
                        int col = S[s];
                        cg_x[col] = signj * vector[vj * superCount + s] - gamma / alpha * signi * vector[vi * superCount + s];
                    }
                    nullSpaceExpand(cg_x);
                    return true;
                } else {
                    for (int s = 0; s < superCount; s++) {
                        int col = S[s];
                        cg_x[col] = signi * vector[vi * superCount + s] - gamma / beta * signj * vector[vj * superCount + s];
                    }
                    nullSpaceExpand(cg_x);
                    return true;
                }
            }

            double lambda;

            /*if (k2 - k1 < 0.01)
            {
                // we're in dange of splitting a very small edge infinitely
                // instead split the longest edge in half
                int maxi = 0;
                int maxj = 0;
                double max = 0;
                for (int i = 0; i < super; i++)
                {
                    for (int j = i + 1; j < super; j++)
                    {
                        vi = simplex[off + i];
                        vj = simplex[off + j];
                        signi = (vi < 0) ? -1.0 : 1.0;
                        signj = (vj < 0) ? -1.0 : 1.0;
                        if (vi < 0) vi = -vi;
                        if (vj < 0) vj = -vj;
                        for (int s = 0; s < super; s++)
                        {
                            double diff = Math.Abs(signi * vector[vi * super + s] - signj * vector[vj * super + s]);
                            if (diff > max)
                            {
                                max = diff;
                                maxi = i;
                                maxj = j;
                            }
                        }
                    }
                }

                mini = maxi;
                minj = maxj;
                vi = simplex[off + maxi];
                vj = simplex[off + maxj];
                signi = (vi < 0) ? -1.0 : 1.0;
                signj = (vj < 0) ? -1.0 : 1.0;
                if (vi < 0) vi = -vi;
                if (vj < 0) vj = -vj;

                lambda = 0.5;
            }
            else*/
            {
                double k3 = (beta - gamma) / (alpha - 2 * gamma + beta);

                lambda = Math.max(k1, Math.min(k3, k2));
            }

            if (vCount == maxv) {
                maxv *= 2;
                vector = Arrays.copyOf(vector, maxv * superCount);
                cross = Arrays.copyOf(cross, maxv * maxv);
            }
            // add the splitting point
            for (int s = 0; s < superCount; s++) {
                vector[vCount * superCount + s] = lambda * signi * vector[vi * superCount + s] + (1.0 - lambda) * signj * vector[vj * superCount + s];
            }
            // flag products as not computed
            for (int i = 1; i <= vCount; i++) {
                cross[(vCount - 1) * vCount / 2 + i] = Double.NaN;
            }
            vCount++;

            // remove old simplex
            simCount--;

            if (simCount + 2 > maxs) {
                maxs += 10;
                simplex = Arrays.copyOf(simplex, maxs * superCount);
            }
            // add new simplex
            for (int s = 0; s < superCount; s++) {
                simplex[(simCount + 1) * superCount + s] = simplex[simCount * superCount + s];
            }
            simplex[simCount * superCount + mini] = vCount - 1;
            simplex[(simCount + 1) * superCount + minj] = vCount - 1;
            simCount += 2;
        }

        return false;
    }

    private boolean copositivityDetectionLight(double eps) {
        // verify second order optimality conditions
        // based on http://iwi.eldoc.ub.rug.nl/FILES/root/2008/LinAlgApplBundfuss/2008LinAlgApplBundfuss.pdf

        zero(cg_x, cols + rows);
        zero(cg_p, cols + rows);

        // add all zero reduced gradient nonbasics into superbasis
        // they'll form the basis of simplex

        computeQx();

        int col;

        for (int j = 0; j < cols; j++) {
            col = V[j];
            if (Math.abs(c[j]) < eps && (flags[col] & flagNMid) == 0) {
                S[superCount] = col;
                superCount++;
                flags[col] = (flags[col] & ~flagN) | flagNMid;
            }
        }

        updateOSB();

        double[] vqv = new double[superCount];
        boolean[] up = new boolean[superCount];
        boolean[] down = new boolean[superCount];

        for (int i = 0; i < superCount; i++) {
            for (int s = 0; s < superCount; s++) {
                col = S[s];
                cg_x[col] = (i == s) ? 1.0 : 0.0;
            }
            nullSpaceExpand(cg_x);
            up[i] = true;
            down[i] = true;
            for (int j = 0; j < superCount + rows; j++) {
                col = OSB[j];
                if (Math.abs(cg_x[col]) > epsZero) {
                    if (ub[col] - mb[col] < Math.abs(ub[col]) * epsPrimalRel + epsPrimalLow) {
                        if (cg_x[col] > 0) {
                            up[i] = false;
                        } else {
                            down[i] = false;
                        }
                    }
                    if (mb[col] - lb[col] < Math.abs(lb[col]) * epsPrimalRel + epsPrimalLow) {
                        if (cg_x[col] < 0) {
                            up[i] = false;
                        } else {
                            down[i] = false;
                        }
                    }
                }
            }
            quadraticQOSB(cg_x, cg_qp);
            double pqp = 0.0;
            for (int k = 0; k < osbStructural; k++) {
                col = OSB[k];
                pqp += cg_qp[col] * cg_x[col];
            }
            vqv[i] = pqp;

            if (pqp < -eps) {
                if (up[i]) {
                    return true;
                } else if (down[i]) {
                    for (int k = 0; i < rows + superCount; i++) {
                        col = OSB[k];
                        cg_x[col] = -cg_x[col];
                    }
                }
            }
        }

        // find minimum product
        for (int i = 0; i < superCount; i++) {
            for (int s = 0; s < superCount; s++) {
                col = S[s];
                cg_p[col] = (i == s) ? 1.0 : 0.0;
            }
            nullSpaceExpand(cg_p);
            quadraticQOSB(cg_p, cg_qp);
            for (int j = i + 1; j < superCount; j++) {
                for (int s = 0; s < superCount; s++) {
                    col = S[s];
                    cg_p[col] = (j == s) ? 1.0 : 0.0;
                }
                nullSpaceExpand(cg_p);
                double pqp = 0.0;
                for (int k = 0; k < osbStructural; k++) {
                    col = OSB[k];
                    pqp += cg_qp[col] * cg_p[col];
                }

                double signi = 1.0;
                double signj = 1.0;

                if (!up[i]) {
                    signi = -1.0;
                    if (!down[i]) {
                        continue;
                    }
                }

                if (!up[j]) {
                    signj = -1.0;
                    if (!down[j]) {
                        continue;
                    }
                }

                if (Math.abs(pqp) > eps) {
                    if (pqp > 0) {
                        if (signi == 1.0 && down[i]) {
                            signi = -1.0;
                            pqp = -pqp;
                        }

                        if (pqp > 0) {
                            if (signj == 1.0 && down[j]) {
                                signj = -1.0;
                                pqp = -pqp;
                            } else {
                                continue;
                            }
                        }
                    }
                    double alpha = vqv[i];
                    double beta = vqv[j];
                    double gamma = pqp;

                    double k1 = gamma / (gamma - alpha);
                    double k2 = beta / (beta - gamma);

                    if (k1 > k2 + epsZero) {
                        if (alpha < eps && beta < eps) {
                            for (int s = 0; s < superCount; s++) {
                                col = S[s];
                                cg_x[col] = (((i == s) ? signi : 0.0) + ((j == s) ? signj : 0.0)) / 2;
                            }
                            nullSpaceExpand(cg_x);
                            return true;
                        } else if (alpha >= eps) {
                            for (int s = 0; s < superCount; s++) {
                                col = S[s];
                                cg_x[col] = ((j == s) ? signj : 0.0) - gamma / alpha * ((i == s) ? signi : 0.0);
                            }
                            nullSpaceExpand(cg_x);
                            return true;
                        } else {
                            for (int s = 0; s < superCount; s++) {
                                col = S[s];
                                cg_x[col] = ((i == s) ? signi : 0.0) - gamma / beta * ((j == s) ? signj : 0.0);
                            }
                            nullSpaceExpand(cg_x);
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private double[] solvePrimalQuadraticCGUnsafe() {
        int limit = 5000;

        //RESTART:
        int i, k;
        int round = 0;
        int updateCount = 0;
        int redecompose = 0;
        int maxj = 0, mini = 0;
        final int maxRedecompose = 50;
        boolean changeBasis;
        double direction;
        boolean checkTermination;
        boolean needsReducedGradientStep = false;
        osbDirty = true;

        //int verificationAttempts = 0;

        boolean feasible = false;
        double lowestInfeasibility = Double.POSITIVE_INFINITY;

        double eps = epsPrimal;

        if (disabledDirty) {
            for (i = 0; i < cols + rows; i++) {
                flags[i] &= ~flagDis;
            }
            disabledDirty = false;
        }

        // in MIP a superbasic may become fixed, if this happens remove it from superbasis
        for (i = 0; i < superCount; i++) {
            int col = S[i];
            if ((flags[col] & flagFix) != 0) {
                flags[col] = (flags[col] & ~flagN) | flagNLB;
                S[i] = S[superCount - 1];
                superCount--;
                i--;
            }
        }

        iteration: do {
            // DECOMPOSE:

            if (redecompose <= 0) {
                decompose();
                redecompose = maxRedecompose; // decompose every 50 iterations to maintain stability
                feasible = false; // when refactoring basis recompute the solution
            }
            if (lu.getSingular()) {
                redecompose = 0;
                patchSingularBasis();
                //ValidateSuperBasis();
                continue iteration;
            }

            if (!feasible) {
                boolean[] recalculate = new boolean[1];
                do {
                    computePrimalSolution(false);
                    updateMB();
                    feasible = isPrimalQFeasible(eps, recalculate);
                } while (recalculate[0]);
                updateCount = 0;
            }

            int bound = flagNMid;
            boolean updateFromPhaseI = false;

            if (!feasible) {
                round++;
                if (round > limit) {
                    break iteration;
                }

                double[] infeasibility = new double[1];
                computePhaseIQReducedCosts(infeasibility, eps);
                if (infeasibility[0] < lowestInfeasibility) lowestInfeasibility = infeasibility[0];
                else if (infeasibility[0] > lowestInfeasibility + eps) {
                    // we're not using a shifting strategy for primal so the only way to combat
                    // numerical cycling is to lower the tolerances
                    if (eps < 1.0) {
                        eps *= 10.0;
                        lowestInfeasibility = Double.POSITIVE_INFINITY;
                    }
                    if (redecompose < maxRedecompose) {
                        redecompose = 0;
                        continue iteration;
                    }
                }
                // for establishing feasibility we don't need quadratic part, just do a simplex step
                int[] maxjOut = new int[1];
                int[] miniOut = new int[1];
                int[] boundOut = new int[] { bound };
                boolean[] changeBasisOut = new boolean[1];
                phaseIQStep(maxjOut, eps, miniOut, boundOut, changeBasisOut);
                maxj = maxjOut[0];
                mini = miniOut[0];
                bound = boundOut[0];
                changeBasis = changeBasisOut[0];
                updateCount++;

                if (maxj == -1) {
                    // if it's not feasible then return null solution
                    if (!feasible) return new double[cols + 1];
                }

                if (changeBasis) {
                    if ((flags[V[maxj]] & flagNMid) != 0) {
                        int s = indexOf(this.extraSlack, V[maxj]);
                        S[s] = S[superCount - 1];
                        superCount--;
                    }

                    updateFromPhaseI = true; // goto UPDATE (falls through to update section)
                } else {
                    continue iteration;
                }
            }

            {
                // MINISTEP:
                ministep: for (;;) {
                    int col;
                    double alpha;
                    ministepBody: {
                        if (!updateFromPhaseI) {
                        if (updateCount > 50) {
                            feasible = false;
                            continue iteration;
                        }

                        round++;
                        if (round > limit) {
                            break iteration;
                        }

                        col = -1;
                        alpha = 1.0;

                        if (superCount > 0) {
                            computeQx();
                            double[] alphaOut = new double[] { alpha };
                            boolean[] nrgsOut = new boolean[1];
                            if (computeCGStep(eps, alphaOut, false, nrgsOut)) {
                                alpha = alphaOut[0];

                                int[] refCol = new int[] { col };
                                double[] refAlpha = new double[] { alpha };
                                int[] refBound = new int[] { bound };
                                if (!selectCGOutgoing(eps, refCol, refAlpha, refBound)) {
                                    // we lost feasibility, go to phase I
                                    feasible = false;
                                    lowestInfeasibility = Double.POSITIVE_INFINITY;
                                    redecompose = 0;
                                    continue iteration;
                                }
                                col = refCol[0];
                                alpha = refAlpha[0];
                                bound = refBound[0];

                                if (col == -1) {
                                    for (i = 0; i < cols + rows; i++) {
                                        mb[i] += cg_x[i];
                                    }
                                    updateCount++;
                                    //ValidateFeasibility();
                                    //ValidateFlags();
                                }
                                /*else if ((flags[col] & flagNMid) == 0)
                                {
                                    System.Diagnostics.Trace.Assert(Array.IndexOf(this.extraBasis, col) != -1, "Bad outgoing selection!");
                                }*/
                            }
                        }

                        if (col == -1) {
                            // no bound hit, alpha = 1 is valid step
                            // determine if we can free some variable

                            computeReducedCostGradient();

                            double[] directionOut = new double[1];
                            maxj = selectPrimalIncoming(directionOut, false);
                            direction = directionOut[0];

                            if (maxj == -1) {
                                // we might have disabled some variables, unflag them and recheck
                                boolean retry = false;
                                if (disabledDirty) {
                                    for (i = 0; i < cols + rows; i++) {
                                        if ((flags[i] & flagDis) != 0) {
                                            flags[i] &= ~flagDis;
                                            retry = true;
                                        }
                                    }
                                    disabledDirty = false;
                                }
                                if (retry) {
                                    maxj = selectPrimalIncoming(directionOut, false);
                                    direction = directionOut[0];
                                }

                                if (maxj == -1) {
                                    // goto TERMINATION
                                    return computeReturnSolutionQuadratic();
                                }
                            }

                            // freeing V[maxj] will result in improved solution

                            if (superCount == 0 || needsReducedGradientStep || (flags[V[maxj]] & flagNMid) != 0) {
                                // special case the one free variable case
                                // in this situation the CG step is overkill
                                // we can use straight reduced gradient approach

                                // we also use this for superbasics when we're so close to optimal that
                                // CG decides it's not safe numerically to continue
                                // in that case the quadratic limit will most likely be very small
                                // but since we're working with unit step in reduced space the solving with
                                // basis shouldn't give too many problems due to zero snapping

                                updateCount++;
                                int[] miniOut = new int[1];
                                int[] boundOut = new int[] { bound };
                                boolean[] changeBasisOut = new boolean[1];
                                boolean[] checkTerminationOut = new boolean[1];
                                if (!reducedGradientStep(maxj, direction, eps, miniOut, boundOut, changeBasisOut, checkTerminationOut)) {
                                    // we lost feasibility, go to phase I
                                    feasible = false;
                                    lowestInfeasibility = Double.POSITIVE_INFINITY;
                                    redecompose = 0;
                                    continue iteration;
                                }
                                mini = miniOut[0];
                                bound = boundOut[0];
                                changeBasis = changeBasisOut[0];
                                checkTermination = checkTerminationOut[0];

                                if (checkTermination) {
                                    computeReducedCostGradient();
                                    maxj = checkTermination(directionOut);
                                    direction = directionOut[0];
                                    if (maxj == -1) {
                                        // goto TERMINATION
                                        return computeReturnSolutionQuadratic();
                                    }
                                    updateCount++;
                                    miniOut = new int[1];
                                    boundOut = new int[] { bound };
                                    changeBasisOut = new boolean[1];
                                    checkTerminationOut = new boolean[1];
                                    if (!reducedGradientStep(maxj, direction, eps, miniOut, boundOut, changeBasisOut, checkTerminationOut)) {
                                        // we lost feasibility, go to phase I
                                        feasible = false;
                                        lowestInfeasibility = Double.POSITIVE_INFINITY;
                                        redecompose = 0;
                                        continue iteration;
                                    }
                                    mini = miniOut[0];
                                    bound = boundOut[0];
                                    changeBasis = changeBasisOut[0];
                                    checkTermination = checkTerminationOut[0];
                                }

                                if (changeBasis) {
                                    if ((flags[V[maxj]] & flagNMid) != 0) {
                                        int s = indexOf(this.extraSlack, V[maxj]);
                                        S[s] = S[superCount - 1];
                                        superCount--;
                                    }

                                    break ministepBody; // goto UPDATE
                                }

                                if (superCount == 1) {
                                    // we already updated superbasics to optimal
                                    // so we know CG will return zero step
                                    // just skip ahead and add another free variable

                                    computeReducedCostGradient();

                                    maxj = selectPrimalIncoming(directionOut, false);
                                    direction = directionOut[0];

                                    if (maxj == -1) {
                                        // goto TERMINATION
                                        return computeReturnSolutionQuadratic();
                                    }

                                    col = V[maxj];
                                    if ((flags[col] & flagNMid) == 0) {
                                        S[superCount] = col;
                                        superCount++;
                                        flags[col] = (flags[col] & ~flagN) | flagNMid;
                                        osbDirty = true;
                                        //ValidateSuperBasis();
                                    }
                                }

                                continue ministep;
                            }

                            col = V[maxj];
                            S[superCount] = col;
                            superCount++;
                            flags[col] = (flags[col] & ~flagN) | flagNMid;
                            osbDirty = true;
                            //ValidateSuperBasis();

                            continue ministep;
                        }

                        //CGSTEP:
                        // we hit a bound
                        // if it's superbasic just fix it
                        // otherwise we have to do a basis update

                        // move for alpha
                        if (alpha > epsZero) {
                            for (i = 0; i < cols + rows; i++) {
                                mb[i] += alpha * cg_x[i];
                            }
                            updateCount++;
                        }
                        //ValidateFeasibility();
                        /*ValidateFlags();
                        if ((bound & flagNUB) != 0)
                        {
                            System.Diagnostics.Trace.Assert(Math.Abs(mb[col] - ub[col]) < Math.Abs(ub[col]) * epsPrimalRel + epsPrimalLow, "Flags corruption");
                        }
                        if ((bound & flagNLB) != 0)
                        {
                            System.Diagnostics.Trace.Assert(Math.Abs(mb[col] - lb[col]) < Math.Abs(lb[col]) * epsPrimalRel + epsPrimalLow, "Flags corruption");
                        }*/


                        if ((flags[col] & flagNMid) != 0) {
                            // fix superbasic
                            flags[col] = (flags[col] & ~flagN) | bound;
                            i = indexOf(this.extraSlack, col);
                            S[i] = S[superCount - 1];
                            superCount--;
                            osbDirty = true;
                            //ValidateSuperBasis();

                            continue ministep;
                        }

                        //System.Diagnostics.Trace.Assert(Array.IndexOf(this.extraBasis, col) != -1, "Bad outgoing selection!");

                        // select superbasic to enter basis
                        // avoid selecting those flagged as disabled because they'll likely cause
                        // basis singularity again

                        // try to replace col with a combination of superbasics
                        // perform this by repeated updates
                        mini = indexOf(this.extraBasis, col);
                        int inc = -1;
                        boolean numericCheck = true;
                        i = superCount - 1;
                        //SUPERBASICUPDATE:
                        for (; i >= 0; i--) {
                            inc = S[i];
                            // skip if we disabled for numerical stability
                            if (numericCheck && (flags[inc] & flagDis) != 0) {
                                continue;
                            }
                            computeBasisReplace(inc);
                            // numerical stability verification only makes sense while we're still nonsinguler
                            // once we get singular further solver with U are undefined
                            if (numericCheck) {
                                lu.fSolveU(ww, w, 0.00000001);
                            }

                            double[] pivot = new double[1];
                            if (lu.update(ww, mini, pivot)) {
                                redecompose--;
                                if (numericCheck && Math.abs(w[mini] - pivot[0]) > 1.0e-6 * Math.abs(w[mini]) + epsZero) {
                                    // numerical errors accumulated are high
                                    if (redecompose == maxRedecompose - 1) {
                                        redecompose = 0;
                                        feasible = false; // forces recalc
                                        flags[inc] |= flagDis;
                                        disabledDirty = true;
                                        continue iteration;
                                    } else {
                                        redecompose = 0;
                                        feasible = false;
                                        continue iteration;
                                    }
                                }
                                if (lu.getSingular()) {
                                    // we need more than one superbasic to complete the replace
                                    numericCheck = false;
                                    //goto SUPERBASICUPDATE;
                                    continue;
                                } else {
                                    // replace successful, final column is inc
                                    S[i] = S[superCount - 1];
                                    superCount--;

                                    maxj = indexOf(this.extraNonbasic, inc);

                                    k = B[mini];
                                    B[mini] = V[maxj];
                                    V[maxj] = k;
                                    flags[B[mini]] = (flags[B[mini]] | flagB) & ~flagN;
                                    flags[V[maxj]] = (flags[V[maxj]] | bound) & ~flagB;
                                    osbDirty = true;
                                    //ValidateSuperBasis();
                                    //ValidateFlags();

                                    continue iteration;
                                }
                            }
                        }
                        if (lu.getSingular()) {
                            decompose();
                            redecompose = maxRedecompose;
                        }
                        if (lu.getSingular()) {
                            feasible = false;
                            redecompose = 0;
                            patchSingularBasis();
                            //ValidateSuperBasis();
                            continue iteration;
                        }

                        /*int inc = S[super - 1];
                        if ((flags[inc] & flagDis) == 0)
                        {
                            super--;
                        }
                        else
                        {
                            bool found = false;
                            // check if there is some other superbasic that is not flagged disabled
                            for (i = super - 2; i >= 0; i--)
                            {
                                inc = S[i];
                                if ((flags[inc] & flagDis) == 0)
                                {
                                    found = true;
                                    S[i] = S[super - 1];
                                    super--;
                                    break;
                                }
                            }
                            if (!found)
                            {*/
                        // all superbasics were flagged, so we can't actually do the swap
                        // try to do a reduced gradient and see if it makes sense to free
                        // another variable so that we can use it to enter basis

                        // we're not actually at optimality point for superbasics
                        // so whatever we get here will be good for reduced gradient only
                        // CG step might want us to go in the opposite direction actually
                        // but we can't allow that because we can't fix basis in that direction
                        // so pretend superbasics are fixed and do a single direction change
                        computeReducedCostGradient();

                        double[] directionOut = new double[1];
                        maxj = selectPrimalIncomingBoundary(directionOut, false);
                        direction = directionOut[0];

                        if (maxj == -1) {
                            // we might have disabled some variables, unflag them and recheck
                            boolean retry = false;
                            if (disabledDirty) {
                                for (i = 0; i < cols + rows; i++) {
                                    if ((flags[i] & flagDis) != 0) {
                                        flags[i] &= ~flagDis;
                                        retry = true;
                                    }
                                }
                                disabledDirty = false;
                            } else {
                                retry = true;
                            }
                            if (retry) {
                                maxj = selectPrimalIncoming(directionOut, false);
                                direction = directionOut[0];
                            }

                            if (maxj == -1) {
                                // goto TERMINATION
                                return computeReturnSolutionQuadratic();
                            }
                        }

                        updateCount++;
                        int[] miniOut2 = new int[1];
                        int[] boundOut2 = new int[] { bound };
                        boolean[] changeBasisOut2 = new boolean[1];
                        boolean[] checkTerminationOut2 = new boolean[1];
                        if (!reducedGradientStep(maxj, direction, eps, miniOut2, boundOut2, changeBasisOut2, checkTerminationOut2)) {
                            // we lost feasibility, go to phase I
                            feasible = false;
                            lowestInfeasibility = Double.POSITIVE_INFINITY;
                            redecompose = 0;
                            continue iteration;
                        }
                        mini = miniOut2[0];
                        bound = boundOut2[0];
                        changeBasis = changeBasisOut2[0];
                        checkTermination = checkTerminationOut2[0];

                        if (checkTermination) {
                            computeReducedCostGradient();
                            maxj = checkTermination(directionOut);
                            direction = directionOut[0];
                            if (maxj == -1) {
                                // goto TERMINATION
                                return computeReturnSolutionQuadratic();
                            }
                            updateCount++;
                            int[] miniOut3 = new int[1];
                            int[] boundOut3 = new int[] { bound };
                            boolean[] changeBasisOut3 = new boolean[1];
                            boolean[] checkTerminationOut3 = new boolean[1];
                            if (!reducedGradientStep(maxj, direction, eps, miniOut3, boundOut3, changeBasisOut3, checkTerminationOut3)) {
                                // we lost feasibility, go to phase I
                                feasible = false;
                                lowestInfeasibility = Double.POSITIVE_INFINITY;
                                redecompose = 0;
                                continue iteration;
                            }
                            mini = miniOut3[0];
                            bound = boundOut3[0];
                            changeBasis = changeBasisOut3[0];
                            checkTermination = checkTerminationOut3[0];
                        }

                        if (changeBasis) {
                            if ((flags[V[maxj]] & flagNMid) != 0) {
                                int s = indexOf(this.extraSlack, V[maxj]);
                                S[s] = S[superCount - 1];
                                superCount--;
                            }

                            break ministepBody; // goto UPDATE
                        }

                        //ValidateSuperBasis();
                        continue ministep;
                        }
                    /*}
                }
                ValidateSuperBasis();

                // swap base
                maxj = Array.IndexOf(this.extraNonbasic, inc);

                ComputeBasisStep(maxj);*/
                    }
                    // UPDATE:

                    redecompose--;
                    if (redecompose > 0) {
                        double[] pivot = new double[1];
                        lu.update(ww, mini, pivot);
                        if (lu.getSingular() || Math.abs(w[mini] - pivot[0]) > 1.0e-6 * Math.abs(w[mini]) + epsZero) {
                            // we aren't making the swap
                            // make sure to properly add superbasic back to S if we cancelled swap
                            col = V[maxj];
                            if ((flags[col] & flagNMid) != 0) {
                                S[superCount] = col;
                                superCount++;
                                //ValidateSuperBasis();
                            }
                            if (redecompose == maxRedecompose - 1) {
                                redecompose = 0;
                                feasible = false; // forces recalc
                                flags[col] |= flagDis;
                                disabledDirty = true;
                                continue iteration;
                            } else {
                                redecompose = 0;
                                feasible = false;
                                continue iteration;
                            }
                        }
                    }

                    k = B[mini];
                    B[mini] = V[maxj];
                    V[maxj] = k;
                    flags[B[mini]] = (flags[B[mini]] | flagB) & ~flagN;
                    flags[V[maxj]] = (flags[V[maxj]] | bound) & ~flagB;
                    //ValidateSuperBasis();
                    //ValidateFlags();
                    continue iteration; // goto DECOMPOSE
                }
            }

        } while (round < limit); // limit computation so we don't dead loop, if everything works it shouldn't take more than this
        // when tuning dual feasibility limit to 100 rounds, we don't want to spend too much time on it

        // just in case
        // if feasible return the best we got
        if (feasible) return computeReturnSolutionQuadratic();
        return new double[cols + 1];
    }

    private double[] solvePrimalQuadraticCGReciprocalUnsafe(int maximizeColumn, double targetValue) {
        int limit = 5000;

        //RESTART:
        int i, k;
        int round = 0;
        int updateCount = 0;
        int redecompose = 0;
        int maxj = 0, mini = 0;
        final int maxRedecompose = 50;
        boolean changeBasis;
        double direction;
        osbDirty = true;
        boolean needsReducedGradientStep = false;

        //int verificationAttempts = 0;

        boolean feasible = false;
        double lowestInfeasibility = Double.POSITIVE_INFINITY;

        double eps = epsPrimal;

        if (disabledDirty) {
            for (i = 0; i < cols + rows; i++) {
                flags[i] &= ~flagDis;
            }
            disabledDirty = false;
        }

        iteration: do {
            // DECOMPOSE:

            if (redecompose <= 0) {
                decompose();
                redecompose = maxRedecompose; // decompose every 50 iterations to maintain stability
                feasible = false; // when refactoring basis recompute the solution
            }
            if (lu.getSingular()) {
                redecompose = 0;
                patchSingularBasis();
                //ValidateSuperBasis();
                continue iteration;
            }

            if (!feasible) {
                boolean[] recalculate = new boolean[1];
                do {
                    computePrimalSolution(false);
                    updateMB();
                    feasible = isPrimalQFeasible(eps, recalculate);
                } while (recalculate[0]);
                updateCount = 0;
            }

            int bound = flagNMid;
            boolean updateFromPhaseI = false;

            if (!feasible) {
                round++;
                if (round > limit) {
                    break iteration;
                }

                double[] infeasibility = new double[1];
                computePhaseIQReducedCosts(infeasibility, eps);
                if (infeasibility[0] < lowestInfeasibility) lowestInfeasibility = infeasibility[0];
                else if (infeasibility[0] > lowestInfeasibility + eps) {
                    // we're not using a shifting strategy for primal so the only way to combat
                    // numerical cycling is to lower the tolerances
                    if (eps < 1.0) {
                        eps *= 10.0;
                        lowestInfeasibility = Double.POSITIVE_INFINITY;
                    }
                    if (redecompose < maxRedecompose) {
                        redecompose = 0;
                        continue iteration;
                    }
                }
                // for establishing feasibility we don't need quadratic part, just do a simplex step
                int[] maxjOut = new int[1];
                int[] miniOut = new int[1];
                int[] boundOut = new int[] { bound };
                boolean[] changeBasisOut = new boolean[1];
                phaseIQStep(maxjOut, eps, miniOut, boundOut, changeBasisOut);
                maxj = maxjOut[0];
                mini = miniOut[0];
                bound = boundOut[0];
                changeBasis = changeBasisOut[0];
                updateCount++;

                if (maxj == -1) {
                    // if it's not feasible then return null solution
                    if (!feasible) return new double[cols + 1];
                }

                if (changeBasis) {
                    if ((flags[V[maxj]] & flagNMid) != 0) {
                        int s = indexOf(this.extraSlack, V[maxj]);
                        S[s] = S[superCount - 1];
                        superCount--;
                    }

                    updateFromPhaseI = true; // goto UPDATE (falls through to update section)
                } else {
                    continue iteration;
                }
            }

            {
                // MINISTEP:
                ministep: for (;;) {
                    int col;
                    double alpha;
                    ministepBody: {
                        if (!updateFromPhaseI) {
                        round++;
                        if (round > limit) {
                            break iteration;
                        }

                        double value = computeValueQuadratic();
                        if (value > targetValue + epsZero) {
                            if ((flags[maximizeColumn] & flagB) != 0) {
                                lb[maximizeColumn] = mb[maximizeColumn];
                                computeReducedCostGradient();
                                maxj = selectPrimalAny();
                                if (maxj != -1) {
                                    mini = indexOf(this.extraBasis, maximizeColumn);
                                    bound = flagNLB;
                                    computeBasisStep(maxj);
                                    break ministepBody; // goto UPDATE
                                }
                            } else {
                                if ((flags[maximizeColumn] & flagNMid) != 0) {
                                    int s = indexOf(this.extraSlack, maximizeColumn);
                                    S[s] = S[superCount - 1];
                                    superCount--;
                                    lb[maximizeColumn] = mb[maximizeColumn];
                                    flags[maximizeColumn] = (flags[maximizeColumn] & ~flagN) | flagNLB;
                                }
                                // we're on lower bound
                                // increase lower bound while value remains above target
                                computeReducedCostGradient(); // don't need the whole gradient, change this
                                maxj = indexOf(this.extraNonbasic, maximizeColumn);
                                computeBasisStep(maxj);
                                direction = 1.0;
                                double vQv = computevQv(maxj, direction);
                                // df = t * c[maximizeColumn] + 0.5 * t^2 * vQv = targetValue - value
                                // 0.5*A*t^2 + B*t + C = 0
                                // A = vQv, B = c[maximizeColumn], C = value - targetValue
                                double b = c[maxj];
                                double C = value - targetValue;
                                // t = [-B +- sqrt(B*B - 2*A*C)]/A
                                double t;
                                if (Math.abs(vQv) < epsZero) {
                                    if (Math.abs(b) < epsZero) {
                                        t = Double.POSITIVE_INFINITY;
                                    } else {
                                        t = -C / b;
                                    }
                                } else {
                                    double sq = Math.sqrt(b * b - 2 * vQv * C);
                                    double t1 = (-b + sq) / vQv;
                                    double t2 = (-b - sq) / vQv;
                                    if (t1 >= 0) {
                                        if (t2 >= 0 && t2 < t1) {
                                            t = t2;
                                        } else {
                                            t = t1;
                                        }
                                    } else {
                                        t = t2;
                                    }
                                }

                                if (t > epsZero) {
                                    mini = -1;
                                    double minr = t;
                                    bound = flagNMid;

                                    int[] refMini = new int[] { mini };
                                    double[] refMinr = new double[] { minr };
                                    int[] refBound = new int[] { bound };
                                    if (!selectReducedGradientOutgoing(direction, eps, refMini, refMinr, refBound)) {
                                        // we lost feasibility, go to phase I
                                        feasible = false;
                                        lowestInfeasibility = Double.POSITIVE_INFINITY;
                                        redecompose = 0;
                                        continue iteration;
                                    }
                                    mini = refMini[0];
                                    minr = refMinr[0];
                                    bound = refBound[0];

                                    mb[maximizeColumn] += minr;
                                    lb[maximizeColumn] = mb[maximizeColumn];
                                    for (i = 0; i < rows; i++) {
                                        mb[B[i]] -= minr * w[i];
                                    }

                                    if (mini != -1) {
                                        // we need to replace this with something that has negative gradient!!!
                                        maxj = selectPrimalReciprocal(maximizeColumn, eps);
                                        if (maxj != -1) {
                                            computeBasisStep(maxj);
                                            break ministepBody; // goto UPDATE
                                        }
                                    }
                                }
                            }
                        }

                        col = -1;
                        alpha = 1.0;

                        if (superCount > 0) {
                            computeQx();
                            double[] alphaOut = new double[] { alpha };
                            boolean[] nrgsOut = new boolean[1];
                            if (computeCGStep(eps, alphaOut, false, nrgsOut)) {
                                alpha = alphaOut[0];

                                int[] refCol = new int[] { col };
                                double[] refAlpha = new double[] { alpha };
                                int[] refBound = new int[] { bound };
                                if (!selectCGOutgoing(eps, refCol, refAlpha, refBound)) {
                                    // we lost feasibility, go to phase I
                                    feasible = false;
                                    lowestInfeasibility = Double.POSITIVE_INFINITY;
                                    redecompose = 0;
                                    continue iteration;
                                }
                                col = refCol[0];
                                alpha = refAlpha[0];
                                bound = refBound[0];

                                if (col == -1) {
                                    for (i = 0; i < cols + rows; i++) {
                                        mb[i] += cg_x[i];
                                    }
                                    updateCount++;
                                    //ValidateFeasibility();
                                    //ValidateFlags();
                                }
                                /*else if ((flags[col] & flagNMid) == 0)
                                {
                                    System.Diagnostics.Trace.Assert(Array.IndexOf(this.extraBasis, col) != -1, "Bad outgoing selection!");
                                }*/
                            }
                        }

                        if (col == -1) {
                            // no bound hit, alpha = 1 is valid step
                            // determine if we can free some variable

                            computeReducedCostGradient();

                            double[] directionOut = new double[1];
                            maxj = selectPrimalIncoming(directionOut, false);
                            direction = directionOut[0];

                            if (maxj == -1) {
                                // we might have disabled some variables, unflag them and recheck
                                boolean retry = false;
                                if (disabledDirty) {
                                    for (i = 0; i < cols + rows; i++) {
                                        if ((flags[i] & flagDis) != 0) {
                                            flags[i] &= ~flagDis;
                                            retry = true;
                                        }
                                    }
                                    disabledDirty = false;
                                }
                                if (retry) {
                                    maxj = selectPrimalIncoming(directionOut, false);
                                    direction = directionOut[0];
                                }

                                if (maxj == -1) {
                                    // goto TERMINATION
                                    double[] ret = computeReturnSolutionQuadratic();
                                    ret[ret.length - 1] = lb[maximizeColumn];
                                    return ret;
                                }
                            }

                            // freeing V[maxj] will result in improved solution

                            if (superCount == 0 || (flags[V[maxj]] & flagNMid) != 0) {
                                // special case the one free variable case
                                // in this situation the CG step is overkill
                                // we can use straight reduced gradient approach

                                // we also use this for superbasics when we're so close to optimal that
                                // CG decides it's not safe numerically to continue
                                // in that case the quadratic limit will most likely be very small
                                // but since we're working with unit step in reduced space the solving with
                                // basis shouldn't give too many problems due to zero snapping

                                updateCount++;
                                boolean checkTermination;
                                int[] miniOut = new int[1];
                                int[] boundOut = new int[] { bound };
                                boolean[] changeBasisOut = new boolean[1];
                                boolean[] checkTerminationOut = new boolean[1];
                                if (!reducedGradientStep(maxj, direction, eps, miniOut, boundOut, changeBasisOut, checkTerminationOut)) {
                                    // we lost feasibility, go to phase I
                                    feasible = false;
                                    lowestInfeasibility = Double.POSITIVE_INFINITY;
                                    redecompose = 0;
                                    continue iteration;
                                }
                                mini = miniOut[0];
                                bound = boundOut[0];
                                changeBasis = changeBasisOut[0];
                                checkTermination = checkTerminationOut[0];

                                if (checkTermination) {
                                    computeReducedCostGradient();
                                    maxj = checkTermination(directionOut);
                                    direction = directionOut[0];
                                    if (maxj == -1) {
                                        // goto TERMINATION
                                        double[] ret = computeReturnSolutionQuadratic();
                                        ret[ret.length - 1] = lb[maximizeColumn];
                                        return ret;
                                    }
                                    updateCount++;
                                    int[] miniOut2 = new int[1];
                                    int[] boundOut2 = new int[] { bound };
                                    boolean[] changeBasisOut2 = new boolean[1];
                                    boolean[] checkTerminationOut2 = new boolean[1];
                                    if (!reducedGradientStep(maxj, direction, eps, miniOut2, boundOut2, changeBasisOut2, checkTerminationOut2)) {
                                        // we lost feasibility, go to phase I
                                        feasible = false;
                                        lowestInfeasibility = Double.POSITIVE_INFINITY;
                                        redecompose = 0;
                                        continue iteration;
                                    }
                                    mini = miniOut2[0];
                                    bound = boundOut2[0];
                                    changeBasis = changeBasisOut2[0];
                                    checkTermination = checkTerminationOut2[0];
                                }

                                if (changeBasis) {
                                    if ((flags[V[maxj]] & flagNMid) != 0) {
                                        int s = indexOf(this.extraSlack, V[maxj]);
                                        S[s] = S[superCount - 1];
                                        superCount--;
                                    }

                                    break ministepBody; // goto UPDATE
                                }

                                if (superCount == 1) {
                                    // we already updated superbasics to optimal
                                    // so we know CG will return zero step
                                    // just skip ahead and add another free variable

                                    computeReducedCostGradient();

                                    maxj = selectPrimalIncoming(directionOut, false);
                                    direction = directionOut[0];

                                    if (maxj == -1) {
                                        // goto TERMINATION
                                        double[] ret = computeReturnSolutionQuadratic();
                                        ret[ret.length - 1] = lb[maximizeColumn];
                                        return ret;
                                    }

                                    col = V[maxj];
                                    if ((flags[col] & flagNMid) == 0) {
                                        S[superCount] = col;
                                        superCount++;
                                        flags[col] = (flags[col] & ~flagN) | flagNMid;
                                        osbDirty = true;
                                        //ValidateSuperBasis();
                                    }
                                }

                                continue ministep;
                            }

                            col = V[maxj];
                            S[superCount] = col;
                            superCount++;
                            flags[col] = (flags[col] & ~flagN) | flagNMid;
                            osbDirty = true;
                            //ValidateSuperBasis();

                            continue ministep;
                        }

                        //CGSTEP:
                        // we hit a bound
                        // if it's superbasic just fix it
                        // otherwise we have to do a basis update

                        // move for alpha
                        if (alpha > epsZero) {
                            for (i = 0; i < cols + rows; i++) {
                                mb[i] += alpha * cg_x[i];
                            }
                            updateCount++;
                        }
                        //ValidateFeasibility();
                        /*ValidateFlags();
                        if ((bound & flagNUB) != 0)
                        {
                            System.Diagnostics.Trace.Assert(Math.Abs(mb[col] - ub[col]) < Math.Abs(ub[col]) * epsPrimalRel + epsPrimalLow, "Flags corruption");
                        }
                        if ((bound & flagNLB) != 0)
                        {
                            System.Diagnostics.Trace.Assert(Math.Abs(mb[col] - lb[col]) < Math.Abs(lb[col]) * epsPrimalRel + epsPrimalLow, "Flags corruption");
                        }*/


                        if ((flags[col] & flagNMid) != 0) {
                            // fix superbasic
                            flags[col] = (flags[col] & ~flagN) | bound;
                            i = indexOf(this.extraSlack, col);
                            S[i] = S[superCount - 1];
                            superCount--;
                            osbDirty = true;
                            //ValidateSuperBasis();

                            continue ministep;
                        }

                        //System.Diagnostics.Trace.Assert(Array.IndexOf(this.extraBasis, col) != -1, "Bad outgoing selection!");

                        // select superbasic to enter basis
                        // avoid selecting those flagged as disabled because they'll likely cause
                        // basis singularity again

                        // try to replace col with a combination of superbasics
                        // perform this by repeated updates
                        mini = indexOf(this.extraBasis, col);
                        int inc = -1;
                        boolean numericCheck = true;
                        superbasicUpdate: for (;;) {
                        for (i = superCount - 1; i >= 0; i--) {
                            inc = S[i];
                            // skip if we disabled for numerical stability
                            if ((flags[col] & flagDis) != 0) {
                                continue;
                            }
                            computeBasisReplace(inc);
                            // numerical stability verification only makes sense while we're still nonsinguler
                            // once we get singular further solver with U are undefined
                            if (numericCheck) {
                                lu.fSolveU(ww, w, 0.00000001);
                            }

                            double[] pivot = new double[1];
                            if (lu.update(ww, mini, pivot)) {
                                redecompose--;
                                if (numericCheck && Math.abs(w[mini] - pivot[0]) > 1.0e-6 * Math.abs(w[mini]) + epsZero) {
                                    // numerical errors accumulated are high
                                    if (redecompose == maxRedecompose - 1) {
                                        redecompose = 0;
                                        feasible = false; // forces recalc
                                        flags[col] |= flagDis;
                                        disabledDirty = true;
                                        continue iteration;
                                    } else {
                                        redecompose = 0;
                                        feasible = false;
                                        continue iteration;
                                    }
                                }
                                if (lu.getSingular()) {
                                    // we need more than one superbasic to complete the replace
                                    numericCheck = false;
                                    continue superbasicUpdate;
                                } else {
                                    // replace successful, final column is inc
                                    S[i] = S[superCount - 1];
                                    superCount--;

                                    maxj = indexOf(this.extraNonbasic, inc);

                                    k = B[mini];
                                    B[mini] = V[maxj];
                                    V[maxj] = k;
                                    flags[B[mini]] = (flags[B[mini]] | flagB) & ~flagN;
                                    flags[V[maxj]] = (flags[V[maxj]] | bound) & ~flagB;
                                    osbDirty = true;
                                    //ValidateSuperBasis();
                                    //ValidateFlags();

                                    continue iteration;
                                }
                            }
                        }
                        break superbasicUpdate;
                        }
                        if (lu.getSingular()) {
                            decompose();
                            redecompose = maxRedecompose;
                        }

                        /*int inc = S[super - 1];
                        if ((flags[inc] & flagDis) == 0)
                        {
                            super--;
                        }
                        else
                        {
                            bool found = false;
                            // check if there is some other superbasic that is not flagged disabled
                            for (i = super - 2; i >= 0; i--)
                            {
                                inc = S[i];
                                if ((flags[inc] & flagDis) == 0)
                                {
                                    found = true;
                                    S[i] = S[super - 1];
                                    super--;
                                    break;
                                }
                            }
                            if (!found)
                            {*/
                        // all superbasics were flagged, so we can't actually do the swap
                        // try to do a reduced gradient and see if it makes sense to free
                        // another variable so that we can use it to enter basis

                        // we're not actually at optimality point for superbasics
                        // so whatever we get here will be good for reduced gradient only
                        // CG step might want us to go in the opposite direction actually
                        // but we can't allow that because we can't fix basis in that direction
                        // so pretend superbasics are fixed and do a single direction change
                        computeReducedCostGradient();

                        double[] directionOut = new double[1];
                        maxj = selectPrimalIncoming(directionOut, false);
                        direction = directionOut[0];

                        if (maxj == -1) {
                            // we might have disabled some variables, unflag them and recheck
                            boolean retry = false;
                            if (disabledDirty) {
                                for (i = 0; i < cols + rows; i++) {
                                    if ((flags[i] & flagDis) != 0) {
                                        flags[i] &= ~flagDis;
                                        retry = true;
                                    }
                                }
                                disabledDirty = false;
                            }
                            if (retry) {
                                maxj = selectPrimalIncoming(directionOut, false);
                                direction = directionOut[0];
                            }

                            if (maxj == -1) {
                                // goto TERMINATION
                                double[] ret = computeReturnSolutionQuadratic();
                                ret[ret.length - 1] = lb[maximizeColumn];
                                return ret;
                            }
                        }

                        updateCount++;
                        int[] miniOut = new int[1];
                        int[] boundOut = new int[] { bound };
                        boolean[] changeBasisOut = new boolean[1];
                        if (!reducedGradientStep(maxj, direction, eps, miniOut, boundOut, changeBasisOut)) {
                            // we lost feasibility, go to phase I
                            feasible = false;
                            lowestInfeasibility = Double.POSITIVE_INFINITY;
                            redecompose = 0;
                            continue iteration;
                        }
                        mini = miniOut[0];
                        bound = boundOut[0];
                        changeBasis = changeBasisOut[0];

                        if (changeBasis) {
                            if ((flags[V[maxj]] & flagNMid) != 0) {
                                int s = indexOf(this.extraSlack, V[maxj]);
                                S[s] = S[superCount - 1];
                                superCount--;
                            }

                            break ministepBody; // goto UPDATE
                        }

                        //ValidateSuperBasis();
                        continue ministep;
                        }
                    /*}
                }
                //ValidateSuperBasis();

                // swap base
                maxj = Array.IndexOf(this.extraNonbasic, inc);

                ComputeBasisStep(maxj);*/
                    }
                    // UPDATE:

                    redecompose--;
                    if (redecompose > 0) {
                        double[] pivot = new double[1];
                        lu.update(ww, mini, pivot);
                        if (lu.getSingular() || Math.abs(w[mini] - pivot[0]) > 1.0e-6 * Math.abs(w[mini]) + epsZero) {
                            // we aren't making the swap
                            // make sure to properly add superbasic back to S if we cancelled swap
                            col = V[maxj];
                            if ((flags[col] & flagNMid) != 0) {
                                S[superCount] = col;
                                superCount++;
                                //ValidateSuperBasis();
                            }
                            if (redecompose == maxRedecompose - 1) {
                                redecompose = 0;
                                feasible = false; // forces recalc
                                flags[col] |= flagDis;
                                disabledDirty = true;
                                continue iteration;
                            } else {
                                redecompose = 0;
                                feasible = false;
                                continue iteration;
                            }
                        }
                    }

                    k = B[mini];
                    B[mini] = V[maxj];
                    V[maxj] = k;
                    flags[B[mini]] = (flags[B[mini]] | flagB) & ~flagN;
                    flags[V[maxj]] = (flags[V[maxj]] | bound) & ~flagB;
                    //ValidateSuperBasis();
                    //ValidateFlags();
                    continue iteration; // goto DECOMPOSE
                }
            }

        } while (round < limit); // limit computation so we don't dead loop, if everything works it shouldn't take more than this
        // when tuning dual feasibility limit to 100 rounds, we don't want to spend too much time on it

        // just in case
        // if feasible return the best we got
        if (feasible) return computeReturnSolutionQuadratic();
        return new double[cols + 1];
    }

    private double[] solvePrimalQuadraticCGUnsafe2() {
        int limit = 5000;

        //RESTART:
        int i, k;
        int round = 0;
        int updateCount = 0;
        int redecompose = 0;
        int maxj = 0, mini = 0;
        final int maxRedecompose = 50;
        boolean changeBasis;
        double direction;
        osbDirty = true;
        boolean needsReducedGradientStep = false;

        //int verificationAttempts = 0;

        boolean feasible = false;
        double lowestInfeasibility = Double.POSITIVE_INFINITY;

        double eps = epsPrimal;

        if (disabledDirty) {
            for (i = 0; i < cols + rows; i++) {
                flags[i] &= ~flagDis;
            }
            disabledDirty = false;
        }

        iteration: do {
            // DECOMPOSE:

            if (redecompose <= 0) {
                decompose();
                redecompose = maxRedecompose; // decompose every 50 iterations to maintain stability
                feasible = false; // when refactoring basis recompute the solution
            }
            if (lu.getSingular()) {
                redecompose = 0;
                patchSingularBasis();
                //ValidateSuperBasis();
                continue iteration;
            }

            if (!feasible) {
                boolean[] recalculate = new boolean[1];
                do {
                    computePrimalSolution(false);
                    updateMB();
                    feasible = isPrimalQFeasible(eps, recalculate);
                } while (recalculate[0]);
                updateCount = 0;
            }

            int bound = flagNMid;
            boolean updateFromPhaseI = false;

            if (!feasible) {
                round++;
                if (round > limit) {
                    break iteration;
                }

                double[] infeasibility = new double[1];
                computePhaseIQReducedCosts(infeasibility, eps);
                if (infeasibility[0] < lowestInfeasibility) lowestInfeasibility = infeasibility[0];
                else if (infeasibility[0] > lowestInfeasibility + eps) {
                    // we're not using a shifting strategy for primal so the only way to combat
                    // numerical cycling is to lower the tolerances
                    if (eps < 1.0) {
                        eps *= 10.0;
                        lowestInfeasibility = Double.POSITIVE_INFINITY;
                    }
                    if (redecompose < maxRedecompose) {
                        redecompose = 0;
                        continue iteration;
                    }
                }
                // for establishing feasibility we don't need quadratic part, just do a simplex step
                int[] maxjOut = new int[1];
                int[] miniOut = new int[1];
                int[] boundOut = new int[] { bound };
                boolean[] changeBasisOut = new boolean[1];
                phaseIQStep(maxjOut, eps, miniOut, boundOut, changeBasisOut);
                maxj = maxjOut[0];
                mini = miniOut[0];
                bound = boundOut[0];
                changeBasis = changeBasisOut[0];
                updateCount++;

                if (maxj == -1) {
                    // if it's not feasible then return null solution
                    if (!feasible) return new double[cols + 1];
                }

                if (changeBasis) {
                    if ((flags[V[maxj]] & flagNMid) != 0) {
                        int s = indexOf(this.extraSlack, V[maxj]);
                        S[s] = S[superCount - 1];
                        superCount--;
                    }

                    updateFromPhaseI = true; // goto UPDATE (falls through to update section)
                } else {
                    continue iteration;
                }
            }

            {
                // MINISTEP:
                ministep: for (;;) {
                    int col;
                    double alpha;
                    ministepBody: {
                        if (!updateFromPhaseI) {
                        round++;
                        if (round > limit) {
                            break iteration;
                        }

                        computeReducedCostGradient();

                        double[] directionOut = new double[1];
                        maxj = selectPrimalIncoming(directionOut, false);
                        direction = directionOut[0];

                        if (maxj == -1) {
                            // we might have disabled some variables, unflag them and recheck
                            boolean retry = false;
                            if (disabledDirty) {
                                for (i = 0; i < cols + rows; i++) {
                                    if ((flags[i] & flagDis) != 0) {
                                        flags[i] &= ~flagDis;
                                        retry = true;
                                    }
                                }
                                disabledDirty = false;
                            }
                            if (retry) {
                                maxj = selectPrimalIncoming(directionOut, false);
                                direction = directionOut[0];
                            }

                            if (maxj == -1) {
                                // goto TERMINATION
                                return computeReturnSolutionQuadratic();
                            }
                        }

                        col = V[maxj];

                        if ((flags[col] & flagNMid) != 0) {
                            col = -1;
                            alpha = 1.0;

                            double[] alphaOut = new double[] { alpha };
                            boolean[] nrgsOut = new boolean[1];
                            if (computeCGStep(eps, alphaOut, true, nrgsOut)) {
                                alpha = alphaOut[0];

                                int[] refCol = new int[] { col };
                                double[] refAlpha = new double[] { alpha };
                                int[] refBound = new int[] { bound };
                                if (!selectCGOutgoing(eps, refCol, refAlpha, refBound)) {
                                    // we lost feasibility, go to phase I
                                    feasible = false;
                                    lowestInfeasibility = Double.POSITIVE_INFINITY;
                                    redecompose = 0;
                                    continue iteration;
                                }
                                col = refCol[0];
                                alpha = refAlpha[0];
                                bound = refBound[0];

                                if (col == -1) {
                                    for (i = 0; i < cols + rows; i++) {
                                        mb[i] += cg_x[i];
                                    }
                                    updateCount++;
                                    //ValidateFeasibility();
                                    //ValidateFlags();

                                    continue ministep;
                                } else {
                                    // we hit a bound
                                    // if it's superbasic just fix it
                                    // otherwise we have to do a basis update

                                    // move for alpha
                                    if (alpha > epsZero) {
                                        for (i = 0; i < cols + rows; i++) {
                                            mb[i] += alpha * cg_x[i];
                                        }
                                        updateCount++;
                                    }
                                    //ValidateFeasibility();
                                    /*ValidateFlags();
                                    if ((bound & flagNUB) != 0)
                                    {
                                        System.Diagnostics.Trace.Assert(Math.Abs(mb[col] - ub[col]) < Math.Abs(ub[col]) * epsPrimalRel + epsPrimalLow, "Flags corruption");
                                    }
                                    if ((bound & flagNLB) != 0)
                                    {
                                        System.Diagnostics.Trace.Assert(Math.Abs(mb[col] - lb[col]) < Math.Abs(lb[col]) * epsPrimalRel + epsPrimalLow, "Flags corruption");
                                    }*/


                                    if ((flags[col] & flagNMid) != 0) {
                                        // fix superbasic
                                        flags[col] = (flags[col] & ~flagN) | bound;
                                        i = indexOf(this.extraSlack, col);
                                        S[i] = S[superCount - 1];
                                        superCount--;
                                        osbDirty = true;
                                        //ValidateSuperBasis();

                                        continue ministep;
                                    }

                                    //System.Diagnostics.Trace.Assert(Array.IndexOf(this.extraBasis, col) != -1, "Bad outgoing selection!");

                                    // select superbasic to enter basis
                                    // avoid selecting those flagged as disabled because they'll likely cause
                                    // basis singularity again

                                    // try to replace col with a combination of superbasics
                                    // perform this by repeated updates
                                    mini = indexOf(this.extraBasis, col);
                                    int inc = -1;
                                    boolean numericCheck = true;
                                    superbasicUpdate: for (;;) {
                                    for (i = superCount - 1; i >= 0; i--) {
                                        inc = S[i];
                                        // skip if we disabled for numerical stability
                                        if ((flags[col] & flagDis) != 0) {
                                            continue;
                                        }
                                        computeBasisReplace(inc);
                                        // numerical stability verification only makes sense while we're still nonsinguler
                                        // once we get singular further solver with U are undefined
                                        if (numericCheck) {
                                            lu.fSolveU(ww, w, 0.00000001);
                                        }

                                        double[] pivot = new double[1];
                                        if (lu.update(ww, mini, pivot)) {
                                            redecompose--;
                                            if (numericCheck && Math.abs(w[mini] - pivot[0]) > 1.0e-6 * Math.abs(w[mini]) + epsZero) {
                                                // numerical errors accumulated are high
                                                if (redecompose == maxRedecompose - 1) {
                                                    redecompose = 0;
                                                    feasible = false; // forces recalc
                                                    flags[col] |= flagDis;
                                                    disabledDirty = true;
                                                    continue iteration;
                                                } else {
                                                    redecompose = 0;
                                                    feasible = false;
                                                    continue iteration;
                                                }
                                            }
                                            if (lu.getSingular()) {
                                                // we need more than one superbasic to complete the replace
                                                numericCheck = false;
                                                continue superbasicUpdate;
                                            } else {
                                                // replace successful, final column is inc
                                                S[i] = S[superCount - 1];
                                                superCount--;

                                                maxj = indexOf(this.extraNonbasic, inc);

                                                k = B[mini];
                                                B[mini] = V[maxj];
                                                V[maxj] = k;
                                                flags[B[mini]] = (flags[B[mini]] | flagB) & ~flagN;
                                                flags[V[maxj]] = (flags[V[maxj]] | bound) & ~flagB;
                                                osbDirty = true;
                                                //ValidateSuperBasis();
                                                //ValidateFlags();

                                                continue iteration;
                                            }
                                        }
                                    }
                                    break superbasicUpdate;
                                    }
                                    if (lu.getSingular()) {
                                        decompose();
                                        redecompose = maxRedecompose;
                                    }

                                    /*int inc = S[super - 1];
                                    if ((flags[inc] & flagDis) == 0)
                                    {
                                        super--;
                                    }
                                    else
                                    {
                                        bool found = false;
                                        // check if there is some other superbasic that is not flagged disabled
                                        for (i = super - 2; i >= 0; i--)
                                        {
                                            inc = S[i];
                                            if ((flags[inc] & flagDis) == 0)
                                            {
                                                found = true;
                                                S[i] = S[super - 1];
                                                super--;
                                                break;
                                            }
                                        }
                                        if (!found)
                                        {*/
                                    // all superbasics were flagged, so we can't actually do the swap
                                    // try to do a reduced gradient and see if it makes sense to free
                                    // another variable so that we can use it to enter basis

                                    // we're not actually at optimality point for superbasics
                                    // so whatever we get here will be good for reduced gradient only
                                    // CG step might want us to go in the opposite direction actually
                                    // but we can't allow that because we can't fix basis in that direction
                                    // so pretend superbasics are fixed and do a single direction change
                                    computeReducedCostGradient();

                                    maxj = selectPrimalIncoming(directionOut, false);
                                    direction = directionOut[0];

                                    if (maxj == -1) {
                                        // we might have disabled some variables, unflag them and recheck
                                        boolean retry = false;
                                        if (disabledDirty) {
                                            for (i = 0; i < cols + rows; i++) {
                                                if ((flags[i] & flagDis) != 0) {
                                                    flags[i] &= ~flagDis;
                                                    retry = true;
                                                }
                                            }
                                            disabledDirty = false;
                                        }
                                        if (retry) {
                                            maxj = selectPrimalIncoming(directionOut, false);
                                            direction = directionOut[0];
                                        }

                                        if (maxj == -1) {
                                            // goto TERMINATION
                                            return computeReturnSolutionQuadratic();
                                        }
                                    }

                                    updateCount++;
                                    int[] miniOut = new int[1];
                                    int[] boundOut = new int[] { bound };
                                    boolean[] changeBasisOut = new boolean[1];
                                    if (!reducedGradientStep(maxj, direction, eps, miniOut, boundOut, changeBasisOut)) {
                                        // we lost feasibility, go to phase I
                                        feasible = false;
                                        lowestInfeasibility = Double.POSITIVE_INFINITY;
                                        redecompose = 0;
                                        continue iteration;
                                    }
                                    mini = miniOut[0];
                                    bound = boundOut[0];
                                    changeBasis = changeBasisOut[0];

                                    if (changeBasis) {
                                        if ((flags[V[maxj]] & flagNMid) != 0) {
                                            int s = indexOf(this.extraSlack, V[maxj]);
                                            S[s] = S[superCount - 1];
                                            superCount--;
                                        }

                                        break ministepBody; // goto UPDATE
                                    }

                                    //ValidateSuperBasis();
                                    continue ministep;
                                }
                            }
                        }

                        updateCount++;
                        int[] miniOut2 = new int[1];
                        int[] boundOut2 = new int[] { bound };
                        boolean[] changeBasisOut2 = new boolean[1];
                        if (!reducedGradientStep(maxj, direction, eps, miniOut2, boundOut2, changeBasisOut2)) {
                            // we lost feasibility, go to phase I
                            feasible = false;
                            lowestInfeasibility = Double.POSITIVE_INFINITY;
                            redecompose = 0;
                            continue iteration;
                        }
                        mini = miniOut2[0];
                        bound = boundOut2[0];
                        changeBasis = changeBasisOut2[0];

                        if (changeBasis) {
                            if ((flags[V[maxj]] & flagNMid) != 0) {
                                int s = indexOf(this.extraSlack, V[maxj]);
                                S[s] = S[superCount - 1];
                                superCount--;
                            }

                            break ministepBody; // goto UPDATE
                        }

                        continue ministep;
                        }
                    }
                    // UPDATE:

                    redecompose--;
                    if (redecompose > 0) {
                        double[] pivot = new double[1];
                        lu.update(ww, mini, pivot);
                        if (lu.getSingular() || Math.abs(w[mini] - pivot[0]) > 1.0e-6 * Math.abs(w[mini]) + epsZero) {
                            // we aren't making the swap
                            // make sure to properly add superbasic back to S if we cancelled swap
                            col = V[maxj];
                            if ((flags[col] & flagNMid) != 0) {
                                S[superCount] = col;
                                superCount++;
                                //ValidateSuperBasis();
                            }
                            if (redecompose == maxRedecompose - 1) {
                                redecompose = 0;
                                feasible = false; // forces recalc
                                flags[col] |= flagDis;
                                disabledDirty = true;
                                continue iteration;
                            } else {
                                redecompose = 0;
                                feasible = false;
                                continue iteration;
                            }
                        }
                    }

                    k = B[mini];
                    B[mini] = V[maxj];
                    V[maxj] = k;
                    flags[B[mini]] = (flags[B[mini]] | flagB) & ~flagN;
                    flags[V[maxj]] = (flags[V[maxj]] | bound) & ~flagB;
                    //ValidateSuperBasis();
                    //ValidateFlags();
                    continue iteration; // goto DECOMPOSE
                }
            }

        } while (round < limit); // limit computation so we don't dead loop, if everything works it shouldn't take more than this
        // when tuning dual feasibility limit to 100 rounds, we don't want to spend too much time on it

        // just in case
        // if feasible return the best we got
        if (feasible) return computeReturnSolutionQuadratic();
        return new double[cols + 1];
    }

    private void phaseIQStep(int[] maxj, double eps, int[] mini, int[] bound, boolean[] changeBasis) {
        double[] direction = new double[1];
        maxj[0] = selectPrimalIncoming(direction, false);


        mini[0] = -1;
        double minr = Double.POSITIVE_INFINITY;
        bound[0] = flagNMid;
        changeBasis[0] = false;

        if (maxj[0] == -1) {
            // not feasible (can happen as part of MIP)
            return;
        }

        computeBasisStep(maxj[0]);
        int[] refMini = new int[] { mini[0] };
        double[] refMinr = new double[] { minr };
        int[] refBound = new int[] { bound[0] };
        selectPhaseIQOutgoing(direction[0], eps, refMini, refMinr, refBound);
        mini[0] = refMini[0];
        minr = refMinr[0];
        bound[0] = refBound[0];

        int col = V[maxj[0]];
        double dist;
        if (direction[0] > 0.0) {
            dist = ub[col] - mb[col];
        } else {
            dist = mb[col] - lb[col];
        }
        if (Math.abs(minr) >= dist - epsZero) {
            // just a bound swap
            if ((flags[col] & flagNMid) != 0) {
                int s = indexOf(this.extraSlack, col);
                S[s] = S[superCount - 1];
                superCount--;
            }
            if (direction[0] > 0.0) {
                flags[col] = (flags[col] & ~flagN) | flagNUB;
            } else {
                flags[col] = (flags[col] & ~flagN) | flagNLB;
            }
            //ValidateSuperBasis();
        } else {
            // we hit basic bound, update basis
            changeBasis[0] = true;
            dist = minr;
        }
        osbDirty = true;

        minr = direction[0] * dist;
        mb[col] += minr;
        for (int i = 0; i < rows; i++) {
            mb[B[i]] -= minr * w[i];
        }
        /*if (!changeBasis)
        {
            ValidateFlags();
        }
        else
        {
            col = B[mini];
            if ((bound & flagNUB) != 0)
            {
                System.Diagnostics.Trace.Assert(Math.Abs(mb[col] - ub[col]) < Math.Abs(ub[col]) * epsPrimalRel + epsPrimalLow, "Flags corruption");
            }
            if ((bound & flagNLB) != 0)
            {
                System.Diagnostics.Trace.Assert(Math.Abs(mb[col] - lb[col]) < Math.Abs(lb[col]) * epsPrimalRel + epsPrimalLow, "Flags corruption");
            }
        }*/
    }

    private boolean reducedGradientStep(int maxj, double direction, double eps, int[] mini, int[] bound, boolean[] changeBasis) {
        boolean[] checkTermination = new boolean[1];
        return reducedGradientStep(maxj, direction, eps, mini, bound, changeBasis, checkTermination);
    }

    private boolean reducedGradientStep(int maxj, double direction, double eps, int[] mini, int[] bound, boolean[] changeBasis, boolean[] checkTermination) {
        computeBasisStep(maxj);

        mini[0] = -1;
        double minr = computeQuadraticLimit(maxj, direction);
        bound[0] = flagNMid;
        changeBasis[0] = false;
        checkTermination[0] = false;

        int[] refMini = new int[] { mini[0] };
        double[] refMinr = new double[] { minr };
        int[] refBound = new int[] { bound[0] };
        if (!selectReducedGradientOutgoing(direction, eps, refMini, refMinr, refBound)) {
            return false;
        }
        mini[0] = refMini[0];
        minr = refMinr[0];
        bound[0] = refBound[0];

        int col = V[maxj];
        double dist;
        if (direction > 0.0) {
            dist = ub[col] - mb[col];
        } else {
            dist = mb[col] - lb[col];
        }
        if (minr >= dist - epsZero) {
            // just a bound swap
            minr = dist;
            if ((flags[col] & flagNMid) != 0) {
                int s = indexOf(this.extraSlack, col);
                S[s] = S[superCount - 1];
                superCount--;
                osbDirty = true;
            }
            if (direction > 0.0) {
                flags[col] = (flags[col] & ~flagN) | flagNUB;
            } else {
                flags[col] = (flags[col] & ~flagN) | flagNLB;
            }
            //ValidateSuperBasis();
        } else if (mini[0] == -1) {
            // no bound hit, add to superbasic
            if ((flags[col] & flagNMid) == 0) {
                flags[col] = (flags[col] & ~flagN) | flagNMid;
                S[superCount] = col;
                superCount++;
                osbDirty = true;
            } else {
                // we were already superbasic
                // check if we're making significant progress, if we're below threshold
                // we should check if we're already as close to optimal as we'll be able to get
                // due to numerical precision
                if (0.5 * c[maxj] * direction * minr < eps) {
                    checkTermination[0] = true;
                }
            }
            //ValidateSuperBasis();
        } else {
            // we hit basic bound, update basis
            changeBasis[0] = true;
            osbDirty = true;
        }

        if (minr > 0.0) {
            minr *= direction;
            mb[col] += minr;
            for (int i = 0; i < rows; i++) {
                mb[B[i]] -= minr * w[i];
            }
        }
        //ValidateFeasibility();
        /*if (!changeBasis)
        {
            ValidateFlags();
        }
        else
        {
            col = B[mini];
            if ((bound & flagNUB) != 0)
            {
                System.Diagnostics.Trace.Assert(Math.Abs(mb[col] - ub[col]) < Math.Abs(ub[col]) * epsPrimalRel + epsPrimalLow, "Flags corruption");
            }
            if ((bound & flagNLB) != 0)
            {
                System.Diagnostics.Trace.Assert(Math.Abs(mb[col] - lb[col]) < Math.Abs(lb[col]) * epsPrimalRel + epsPrimalLow, "Flags corruption");
            }
        }*/
        return true;
    }

    private static void traceAssert(boolean condition, String message) {
        // C# System.Diagnostics.Trace.Assert; the default listener aborts the process on failure
        if (!condition) throw new RuntimeException(message);
    }

    public void validateBaseIntegrity() {
        for (int i = 0; i < cols + rows; i++) {
            if ((this.extraFlags[i] & flagN) != 0) {
                traceAssert(indexOf(this.extraNonbasic, i) != -1, "Basis corruption");
                traceAssert(indexOf(this.extraBasis, i) == -1, "Basis corruption");
            }
            if ((this.extraFlags[i] & flagB) != 0) {
                traceAssert(indexOf(this.extraNonbasic, i) == -1, "Basis corruption");
                traceAssert(indexOf(this.extraBasis, i) != -1, "Basis corruption");
            }
        }
    }

    public void validateFeasibility() {
        for (int row = 0; row < baseRows; row++) {
            double sum = 0.0;
            for (int i = 0; i < cols; i++) {
                sum += mb[i] * a[row + i * baseRows];
            }
            sum += mb[cols + row];
            traceAssert(Math.abs(sum) < 0.00001, "Feasibility lost");
        }
        for (int i = 0; i < cols + rows; i++) {
            double difflb = mb[i] - lb[i];
            double diffub = mb[i] - ub[i];
            boolean ifeasiblelb = (difflb >= -Math.abs(lb[i]) * epsPrimalRel - epsPrimal);
            boolean ifeasibleub = (diffub <= +Math.abs(ub[i]) * epsPrimalRel + epsPrimal);
            traceAssert(ifeasiblelb && ifeasibleub, "Feasibility lost");
        }
    }

    public void validateSuperBasis() {
        for (int s = 0; s < superCount - 1; s++) {
            int col = S[s];
            int i = indexOf(this.extraSlack, col, s + 1);
            traceAssert(i == -1 || i >= superCount, "Superbasis corruption");
        }
        for (int s = 0; s < superCount; s++) {
            int col = S[s];
            int i = indexOf(this.extraNonbasic, col);
            traceAssert(i != -1, "Superbasis corruption");
            traceAssert((flags[col] & flagNMid) != 0, "Superbasis corruption");
        }
    }

    public void validateFlags() {
        for (int col = 0; col < cols + rows; col++) {
            if ((flags[col] & flagNUB) != 0) {
                traceAssert(indexOf(this.extraNonbasic, col) != -1, "Flags corruption");
                traceAssert(Math.abs(mb[col] - ub[col]) < Math.abs(ub[col]) * epsPrimalRel + epsPrimalLow, "Flags corruption");
            }
            if ((flags[col] & flagNLB) != 0) {
                traceAssert(indexOf(this.extraNonbasic, col) != -1, "Flags corruption");
                traceAssert(Math.abs(mb[col] - lb[col]) < Math.abs(lb[col]) * epsPrimalRel + epsPrimalLow, "Flags corruption");
            }
            if ((flags[col] & flagNMid) != 0) {
                traceAssert(indexOf(this.extraSlack, col) != -1, "Flags corruption");
                traceAssert(indexOf(this.extraNonbasic, col) != -1, "Flags corruption");
            }
            if ((flags[col] & flagB) != 0) {
                traceAssert(indexOf(this.extraBasis, col) != -1, "Flags corruption");
            }
        }
    }

    public void validateCGFeasibility() {
        for (int row = 0; row < baseRows; row++) {
            double sum = 0.0;
            for (int i = 0; i < cols; i++) {
                sum += cg_x[i] * a[row + i * baseRows];
            }
            sum += cg_x[cols + row];
            traceAssert(Math.abs(sum) < epsPrimal, "CG Feasibility lost");
        }
    }

    public void validateCGFeasibility2() {
        for (int row = 0; row < baseRows; row++) {
            double sum = 0.0;
            for (int i = 0; i < cols; i++) {
                sum += cg_p[i] * a[row + i * baseRows];
            }
            sum += cg_p[cols + row];
            traceAssert(Math.abs(sum) < epsPrimal, "CG Feasibility lost");
        }
    }

    public double[] solveDual(boolean startWithPhaseI) {
        if (hardInfeasibility) return new double[cols + 1];
        double[] ret = null;

        this.a = arraySet.sparseMatrixData;
        this.U = arraySet.LU_U;
        this.d = arraySet.d;
        this.x = arraySet.x;
        this.w = arraySet.w;
        this.ww = arraySet.ww;
        this.wd = arraySet.wd;
        this.c = arraySet.c;
        this.u = arraySet.u;
        this.cost = arraySet.costWorking;
        this.sparseValue = arraySet.sparseMatrixValue;
        this.D = arraySet.extraConstraints;
        this.B = this.extraBasis;
        this.V = this.extraNonbasic;
        this.sparseRow = arraySet.sparseMatrixRow;
        this.sparseCol = arraySet.sparseMatrixCol;
        this.flags = this.extraFlags;
        this.lb = this.extraLowerBounds;
        this.ub = this.extraUpperBounds;
        this.pD = arraySet.pD;

        setupExtraConstraints();

        lu.beginSafe();
        loadCost();
        perturbCost();
        boolean[] dualUnbounded = new boolean[1];
        shifted = false;
        //Preprocessing();
        ret = solveDualUnsafe(startWithPhaseI, dualUnbounded);
        if (!dualUnbounded[0]) {
            // unshifted might no longer be dual feasible for our tolerances
            // reoptimize it with primal, dual phase II tolerances are ok
            if (shifted) {
                loadCost();
                perturbCost();
            }
            ret = solvePrimalUnsafe(false, true, true, true);
        }
        if ((costWorkingDirty && !dualUnbounded[0]) || ret == null) {
            if (costWorkingDirty) loadCost();
            int[] _Bcopy = null;
            int[] _Vcopy = null;
            int[] _flagscopy = null;
            if (!dualUnbounded[0]) {
                // we're just reoptimizing for changes in cost vector
                // since we'll be reusing this basis for dual later
                // we should store the basis, because the basis on unperturbed problem
                // very likely is not dual feasible on perturbed system
                // since we're using deterministic perturbation the likelihood of remaining
                // dual feasible is much higher since changes due to shifting are very small
                _Bcopy = this.extraBasis.clone();
                _Vcopy = this.extraNonbasic.clone();
                _flagscopy = this.extraFlags.clone();
            }
            ret = solvePrimalUnsafe(false, false, false, false);
            if (!dualUnbounded[0]) {
                extraBasis = _Bcopy;
                extraNonbasic = _Vcopy;
                extraFlags = _flagscopy;
            }
        }
        lu.endUnsafe();

        this.a = null;
        this.U = null;
        this.d = null;
        this.x = null;
        this.w = null;
        this.ww = null;
        this.wd = null;
        this.c = null;
        this.u = null;
        this.cost = null;
        this.sparseValue = null;
        this.pD = null;
        this.D = null;
        this.B = null;
        this.V = null;
        this.sparseRow = null;
        this.sparseCol = null;
        this.flags = null;
        this.lb = null;
        this.ub = null;

        return ret;
    }

    public double[] solveDualUnsafe(boolean startWithPhaseI, boolean[] dualUnbounded) {
        // LU = A_B
        // d = x_B <- A_B^-1*b ... primal solution
        // u = u <- c_B*A_B^-1 ... dual solution
        // w_N <- c_N - u*A_N  ... dual solution

        int i, k;
        int round = 0;
        boolean needsRecalc = true;
        int redecompose = 0;
        final int maxRedecompose = 50;
        dualUnbounded[0] = false;

        if (disabledDirty) {
            for (i = 0; i < cols + rows; i++) {
                flags[i] &= ~flagDis;
            }
            disabledDirty = false;
        }

        boolean blacklistAllowed = true;

        boolean phaseI = false;
        if (startWithPhaseI) {
            decompose();
            if (lu.getSingular()) {
                patchSingularBasis();
                decompose();
            }
            redecompose = maxRedecompose;
            computePrimalSolution(phaseI);
            computeReducedCosts();
            phaseI = boundFlip(phaseI, 0.0, epsDualI, -1, true);
            if (phaseI) {
                computePrimalSolution(phaseI);
                boundFlip(phaseI, 0.0, epsDualI, -1, true);
            }
            needsRecalc = false;
        }

        do {
            // DECOMPOSE:
            if (redecompose <= 0) {
                decompose();
                redecompose = maxRedecompose; // decompose each time to see where the instability comes from
                needsRecalc = true; // for long dual phases refresh the reduced costs
            }

            if (lu.getSingular()) {
                //System.Diagnostics.Trace.WriteLine("Basis singular in " + round);
                // try to patch the basis by replacing singular columns with corresponding slacks of singular rows
                redecompose = 0;
                patchSingularBasis();
                continue;
            }

            if (needsRecalc) {
                computePrimalSolution(phaseI);
                computeReducedCosts();
                needsRecalc = false;
            }

            double[] deltaOut = new double[1];
            int[] boundOut = new int[1];

            int mini = selectDualOutgoing(phaseI, deltaOut, boundOut);
            double delta = deltaOut[0];
            int bound = boundOut[0];

            if (mini == -1) {
                // if we blacklisted some variables give it another try
                if (disabledDirty) {
                    for (i = 0; i < cols + rows; i++) {
                        flags[i] &= ~flagDis;
                    }
                    disabledDirty = false;
                    blacklistAllowed = false;
                    redecompose = 0;
                    needsRecalc = true;
                    continue;
                }
                if (phaseI) {
                    // if a variable is on a nonexisting bound then we must have an unrecoverable dual unfeasibility, shouldn't happen
                    for (int col = 0; col < cols + rows; col++) {
                        if ((flags[col] & flagNLB) != 0 && (flags[col] & flagLB) == 0) return new double[cols + 1];
                        else if ((flags[col] & flagNUB) != 0 && (flags[col] & flagUB) == 0) return new double[cols + 1];
                    }
                    phaseI = false;
                    computePrimalSolution(phaseI);
                    continue;
                }
                // refine solution
                //ComputePrimalSolution();
                coldStart = false;
                //System.Diagnostics.Trace.WriteLine("Dual optimal in " + round);
                return computeReturnSolution();
            }

            int minj;
            double minr;

            if (!computeDualPivotRow(phaseI, mini)) {
                decompose();
                //System.Diagnostics.Trace.WriteLine("Feasibility lost in " + round + ", conditioning " + U[rows * rows - 1]);
                if (lu.getSingular()) {
                    patchSingularBasis();
                    decompose();
                }
                redecompose = maxRedecompose;
                computeReducedCosts();
                phaseI = boundFlip(phaseI, 0.0, epsDualI, -1, true);
                if (phaseI) {
                    computePrimalSolution(phaseI);
                    boundFlip(phaseI, 0.0, epsDualI, -1, true);
                }
                continue;
            }

            //SelectDualIncoming(phaseI, mini, out minj, out minr);
            int[] minjOut = new int[1];
            double[] minrOut = new double[1];
            selectDualIncomingWithBoundFlips(phaseI, delta, minjOut, minrOut);
            minj = minjOut[0];
            minr = minrOut[0];

            if (minj == -1) {
                if (redecompose == maxRedecompose) {
                    // unfeasible, return null solution, don't pursue this branch because no solution exists here
                    double[] ret = new double[cols + 1];
                    dualUnbounded[0] = true;
                    // debug measure, verify primal infeasibility by solving the primal
                    //ret = SolvePrimalUnsafe(true, false, false);
                    //System.Diagnostics.Trace.WriteLine("Dual unfeasible after " + round);
                    return ret;
                } else {
                    // make a clean factorization to make sure we're really unbounded
                    redecompose = 0;
                    needsRecalc = true;
                    continue;
                }
            }

            // shifting to prevent numerical cycling
            if (minr * delta > 0) {
                //System.Diagnostics.Debug.WriteLine("Value shifted in round " + round);
                if (delta > 0) {
                    minr = -1.0e-12;
                } else {
                    minr = 1.0e-12;
                }
                double shift = minr * wd[minj] - c[minj];
                c[minj] += shift;
                cost[V[minj]] += shift; // requires working copy for cost
                costWorkingDirty = true;
                shifted = true;
                // NLB: *cj < eps, NUB: *cj > -eps
                // update goes *cj -= minr * *wdj
                for (int j = 0; j < cols; j++) {
                    if (j != minj) {
                        int col = V[j];
                        if ((flags[col] & flagNLB) != 0) {
                            if (c[j] - minr * wd[j] > epsDual) {
                                shift = epsDual - c[j] + minr * wd[j];
                                c[j] += shift;
                                cost[col] += shift; // requires working copy for cost
                            }
                        } else if ((flags[col] & flagNUB) != 0) {
                            if (c[j] - minr * wd[j] < -epsDual) {
                                shift = -epsDual - c[j] + minr * wd[j];
                                c[j] += shift;
                                cost[col] += shift; // requires working copy for cost
                            }
                        }
                    }
                }
            }

            boundFlip(phaseI, minr, phaseI ? epsDualI : epsDual, minj, false);
            updateDual(phaseI, minj, mini, minr, delta, false); // use true if using simple ratio test

            redecompose--;
            if (redecompose > 0) {
                double[] pivot = new double[1];
                lu.update(ww, mini, pivot);
                if (lu.getSingular() || Math.abs(wd[minj] - pivot[0]) > 1.0e-6 * Math.abs(wd[minj]) + epsZero) {
                    if (redecompose == maxRedecompose - 1) {
                        // if we don't allow blacklisting we'll either go on if not singular or patch the basis
                        if (blacklistAllowed) {
                            flags[B[mini]] |= flagDis;
                            disabledDirty = true;
                            redecompose = 0;
                            needsRecalc = true;
                            continue;
                        }
                    } else {
                        redecompose = 0;
                        needsRecalc = true;
                        continue;
                    }
                }
            }

            //System.Diagnostics.Debug.WriteLine("Round " + round + ", swap " + B[mini] + " out, " + V[minj] + " in, value change estimate " + delta * minr);

            // swap base
            updateBounds();
            k = B[mini];
            B[mini] = V[minj];
            V[minj] = k;
            flags[B[mini]] = (flags[B[mini]] | flagB) & ~flagN;
            flags[V[minj]] = (flags[V[minj]] | bound) & ~flagB;

            round++;
        } while (round < 5000);
        // just in case
        return new double[cols + 1];
    }

    // Faithful port of .NET Framework System.Random (subtractive generator), needed because
    // PerturbCost/GlobalRestart use deterministic Random(0) perturbation.
    private static final class NetRandom {
        private static final int MBIG = Integer.MAX_VALUE;
        private static final int MSEED = 161803398;
        private final int[] SeedArray = new int[56];
        private int inext;
        private int inextp;

        NetRandom() {
            this((int) (System.nanoTime() & 0xFFFFFFFFL));
        }

        NetRandom(int seed) {
            int ii;
            int mj, mk;
            int subtraction = (seed == Integer.MIN_VALUE) ? Integer.MAX_VALUE : Math.abs(seed);
            mj = MSEED - subtraction;
            SeedArray[55] = mj;
            mk = 1;
            for (int i = 1; i < 55; i++) {
                ii = (21 * i) % 55;
                SeedArray[ii] = mk;
                mk = mj - mk;
                if (mk < 0) mk += MBIG;
                mj = SeedArray[ii];
            }
            for (int k = 1; k < 5; k++) {
                for (int i = 1; i < 56; i++) {
                    SeedArray[i] -= SeedArray[1 + (i + 30) % 55];
                    if (SeedArray[i] < 0) SeedArray[i] += MBIG;
                }
            }
            inext = 0;
            inextp = 21;
        }

        private int internalSample() {
            int retVal;
            int locINext = inext;
            int locINextp = inextp;
            if (++locINext >= 56) locINext = 1;
            if (++locINextp >= 56) locINextp = 1;
            retVal = SeedArray[locINext] - SeedArray[locINextp];
            if (retVal == MBIG) retVal--;
            if (retVal < 0) retVal += MBIG;
            SeedArray[locINext] = retVal;
            inext = locINext;
            inextp = locINextp;
            return retVal;
        }

        public double nextDouble() {
            return internalSample() * (1.0 / MBIG);
        }

        public int next(int maxValue) {
            if (maxValue < 0) throw new RuntimeException("Argument out of range");
            return (int) (internalSample() * (1.0 / MBIG) * maxValue);
        }
    }
}
