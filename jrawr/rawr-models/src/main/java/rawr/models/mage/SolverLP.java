// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SolverLP.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The C# desktop class is `unsafe` with double* scaling/cost pointers; ported as index-based
// double[] arrays (the arrays are passed whole, no offsets), same arithmetic (DECISIONS #4).
package rawr.models.mage;

import java.util.Locale;

public final class SolverLP implements Comparable<SolverLP> {
    private int cRows;
    private int cCols;
    private Solver solver;
    private LP lp;
    private double[] compactSolution;
    private boolean needsDual;
    private boolean needsQuadratic;
    private int segments;
    public StringBuilder log;
    private int mpsRow;
    private int[] sort;
    private double qk;
    private int maximizeColumn;
    private double targetValue;
    //public int[] disabledHex;

    private double[] pRowScale;
    private double[] pColumnScale;
    private double[] pCost;

    private SparseMatrix sparseMatrix;

    public void beginSafe(double[] pRowScale, double[] pColumnScale, double[] pCost, double[] pData, double[] pValue, int[] pRow, int[] pCol) {
        this.pRowScale = pRowScale;
        this.pColumnScale = pColumnScale;
        this.pCost = pCost;
        sparseMatrix = lp.A;
        sparseMatrix.beginSafe(pData, pValue, pRow, pCol);
    }

    public void beginUnsafe(double[] pRowScale, double[] pColumnScale, double[] pCost, double[] pData, double[] pValue, int[] pRow, int[] pCol) {
        this.pRowScale = pRowScale;
        this.pColumnScale = pColumnScale;
        this.pCost = pCost;
        sparseMatrix = lp.A;
        sparseMatrix.beginUnsafe(pData, pValue, pRow, pCol);
    }

    public void endUnsafe() {
        sparseMatrix.endUnsafe();
        sparseMatrix = null;
        pRowScale = null;
        pCost = null;
    }

    public void endColumnConstruction() {
        lp.endColumnConstruction();
    }

    public void forceRecalculation(boolean dual) {
        compactSolution = null;
        needsDual = dual;
    }

    public void forceRecalculation() {
        compactSolution = null;
        needsDual = false;
    }

    public SolverLP clone() {
        //if (compactSolution != null && !allowReuse) throw new InvalidOperationException();
        SolverLP clone = new SolverLP();
        clone.cRows = cRows;
        clone.cCols = cCols;
        clone.solver = solver;
        clone.lp = lp.clone();
        clone.compactSolution = null;
        clone.needsDual = needsDual;
        clone.needsQuadratic = needsQuadratic;
        clone.segments = segments;
        clone.log = this.log;
        clone.mpsRow = mpsRow;
        clone.sort = sort;
        clone.qk = this.qk;
        clone.maximizeColumn = maximizeColumn;
        clone.targetValue = targetValue;
        clone.pRowScale = pRowScale;
        clone.pColumnScale = pColumnScale;
        clone.pCost = pCost;
        clone.sparseMatrix = sparseMatrix;
        clone.arraySet = arraySet;
        if (this.log != null) clone.log = new StringBuilder(this.log.toString());
        //if (disabledHex != null) clone.disabledHex = (int[])disabledHex.Clone();
        return clone;
    }

    public SolverLP() {
    }

    public SolverLP(ArraySet arraySet, int baseRows, int maximumColumns, Solver solver, int segments) {
        initialize(arraySet, baseRows, maximumColumns, solver, segments);
    }

    public void initialize(ArraySet arraySet, int baseRows, int maximumColumns, Solver solver, int segments) {
        this.arraySet = arraySet;
        if (baseRows > arraySet.maxSolverRows || maximumColumns > arraySet.maxSolverCols) {
            arraySet.maxSolverRows = Math.max(baseRows, arraySet.maxSolverRows);
            arraySet.maxSolverCols = Math.max(maximumColumns, arraySet.maxSolverCols);
            arraySet.recreateSolverArrays();
        }

        this.solver = solver;
        this.segments = segments;
        cRows = baseRows;
        cCols = 0;

        for (int i = 0; i <= cRows; i++) {
            arraySet.rowScale[i] = 1.0;
        }

        if (lp == null) {
            lp = new LP(cRows, maximumColumns, arraySet);
        } else {
            lp.initialize(cRows, maximumColumns, arraySet);
        }

        compactSolution = null;
        sort = null;
        needsDual = false;
        needsQuadratic = false;
        maximizeColumn = -1;
    }

    public void setRowScale(int row, double value) {
        if (row == -1) return;
        arraySet.rowScale[row] = value;
    }

    public void setRowScaleUnsafe(int row, double value) {
        if (row == -1) return;
        pRowScale[row] = value;
    }

    public void setColumnScale(int col, double value) {
        arraySet.columnScale[col] = value;
    }

    public void setColumnScaleUnsafe(int col, double value) {
        pColumnScale[col] = value;
    }

    public int getColumns() {
        return cCols;
    }

    public int addColumn() {
        arraySet.columnScale[cCols] = 1.0;
        cCols++;
        return lp.addColumn();
    }

    public int addColumnUnsafe() {
        pColumnScale[cCols] = 1.0;
        cCols++;
        return lp.addColumn();
    }

    public double get(int row, int col) {
        if (row == -1 || col == -1) return 0;
        return lp.get(row, col) / arraySet.rowScale[row] / arraySet.columnScale[col];
    }

    public void set(int row, int col, double value) {
        if (row == -1 || col == -1) return;
        lp.set(row, col, value * arraySet.rowScale[row] * arraySet.columnScale[col]);
        compactSolution = null;
    }

    public void setElement(int row, int col, double value) {
        this.set(row, col, value);
    }

    public void setElementUnsafe(int row, int col, double value) {
        if (row == -1) return;
        sparseMatrix.setElementUnsafe(row, col, value * pRowScale[row] * pColumnScale[col]);
    }

    public void setRHS(int row, double value) {
        if (row == -1) return;
        lp.setRHS(row, value * arraySet.rowScale[row]);
    }

    public double getRHS(int row) {
        if (row == -1) return Double.POSITIVE_INFINITY;
        return lp.getRHS(row) / arraySet.rowScale[row];
    }

    public void setLHS(int row, double value) {
        if (row == -1) return;
        lp.setLHS(row, value * arraySet.rowScale[row]);
    }

    public void setRHSUnsafe(int row, double value) {
        if (row == -1) return;
        lp.setRHS(row, value * pRowScale[row]);
    }

    public void setLHSUnsafe(int row, double value) {
        if (row == -1) return;
        lp.setLHS(row, value * pRowScale[row]);
    }

    public void setCost(int col, double value) {
        if (col == -1) return;
        lp.setCost(col, value * arraySet.rowScale[cRows] * arraySet.columnScale[col]);
    }

    public void setCostUnsafe(int col, double value) {
        pCost[col] = value * pRowScale[cRows] * pColumnScale[col];
    }

    public void setSpellDpsUnsafe(int col, double value) {
        arraySet.spellDps[col] = value * pRowScale[cRows] * pColumnScale[col];
    }

    public void setColumnLowerBound(int col, double value) {
        lp.setLowerBound(col, value / arraySet.columnScale[col]);
    }

    public void setColumnUpperBound(int col, double value) {
        lp.setUpperBound(col, value / arraySet.columnScale[col]);
    }

    //private int rowMaximizeSegment = -1;
    //private int rowColdsnap = -1;

    public void eraseColumn(int col) {
        if (col == -1) return;
        lp.setUpperBound(col, 0.0);
        compactSolution = null;
        needsDual = true;
    }

    /*public void UpdateMaximizeSegmentColumn(int col)
    {
        if (col == -1) return;
        if (rowMaximizeSegment == -1) rowMaximizeSegment = lp.AddConstraint();
        lp.SetConstraintElement(rowMaximizeSegment, col, lp.GetConstraintElement(rowMaximizeSegment, col) - 1.0 * columnScale[col]);
        compactSolution = null;
        needsDual = true;
    }

    public void UpdateMaximizeSegmentDuration(double value)
    {
        if (rowMaximizeSegment == -1) rowMaximizeSegment = lp.AddConstraint();
        lp.SetConstraintRHS(rowMaximizeSegment, lp.GetConstraintRHS(rowMaximizeSegment) - value);
        compactSolution = null;
        needsDual = true;
    }

    public bool HasColdsnapConstraints
    {
        get
        {
            return rowColdsnap != -1;
        }
    }*/

    public int addConstraint() {
        boolean[] newConstraint = new boolean[1];
        return lp.addConstraint(null, newConstraint);
    }

    public int addConstraint(String name, boolean[] newConstraint) {
        return lp.addConstraint(name, newConstraint);
    }

    public void releaseConstraints() {
        lp.releaseConstraints();
    }

    public void setConstraintRHS(int index, double value) {
        lp.setConstraintRHS(index, value);
    }

    public void setConstraintLHS(int index, double value) {
        lp.setConstraintLHS(index, value);
    }

    public void setConstraintElement(int index, int col, double value) {
        lp.setConstraintElement(index, col, arraySet.columnScale[col] * value);
    }

    /*public void AddColdsnapConstraints(int segments)
    {
        if (rowColdsnap == -1)
        {
            rowColdsnap = lp.AddConstraint();
            for (int i = 1; i < segments; i++) lp.AddConstraint();
        }
    }

    public void UpdateColdsnapColumn(int index, int col)
    {
        compactSolution = null;
        needsDual = true;
        if (col == -1) return;
        lp.SetConstraintElement(rowColdsnap + index, col, 1.0 * columnScale[col]);
    }

    public void UpdateColdsnapDuration(int index, double value)
    {
        compactSolution = null;
        needsDual = true;
        lp.SetConstraintRHS(rowColdsnap + index, value);
    }*/

    private ArraySet arraySet;

    public ArraySet getArraySet() {
        return arraySet;
    }

    private void solveInternal() {
        if (compactSolution == null) {
            if (needsQuadratic) {
                compactSolution = lp.solvePrimalQuadratic(mpsRow, sort, this.qk, needsDual, maximizeColumn, targetValue, solver.getCalculationOptions().getGlobalRestarts());
            } else if (needsDual) {
                //System.Diagnostics.Trace.WriteLine("Solving " + Log.ToString());
                //System.Diagnostics.Debug.WriteLine("Solving H=" + HeroismHash.ToString("X") + ", AP=" + APHash.ToString("X") + ", IV=" + IVHash.ToString("X"));
                compactSolution = lp.solveDual(false);
            } else {
                compactSolution = lp.solvePrimal(false);
                //compactSolution = lp.SolveDual(true);
            }
            unscaleSolution();
        }
    }

    private void unscaleSolution() {
        if (maximizeColumn == -1) {
            compactSolution[compactSolution.length - 1] /= arraySet.rowScale[cRows];
        }
        for (int i = 0; i < compactSolution.length - 1; i++) {
            compactSolution[i] *= arraySet.columnScale[i];
        }
    }

    public double[] solve() {
        solveInternal();
        return compactSolution;
    }

    public void solvePrimalDual() {
        lp.solvePrimal(true);
        //lp.SolveDual(true);
        compactSolution = lp.solveDual(true);
        unscaleSolution();
    }

    public void solvePrimalQuadratic(int mpsRow, int[] sort, double k, boolean skipLinear, int maximizeColumn, double targetValue) {
        this.mpsRow = mpsRow;
        this.sort = sort;
        this.qk = k;
        this.maximizeColumn = maximizeColumn;
        this.targetValue = targetValue * arraySet.rowScale[cRows];
        compactSolution = lp.solvePrimalQuadratic(mpsRow, sort, k, skipLinear, maximizeColumn, this.targetValue, solver.getCalculationOptions().getGlobalRestarts());
        unscaleSolution();
        needsQuadratic = true;
    }

    public double getValue() {
        solveInternal();
        return compactSolution[compactSolution.length - 1];
    }

    @Override
    public int compareTo(SolverLP other) {
        double a = this.getValue();
        double b = other.getValue();
        if (a < b) return -1;
        if (a > b) return 1;
        if (a == b) return 0;
        if (Double.isNaN(a)) return Double.isNaN(b) ? 0 : -1;
        return 1;
    }

    @Override
    public String toString() {
        if (compactSolution == null) return super.toString();
        return "LP: " + compactSolution[compactSolution.length - 1];
    }

    public String getListing() {
        if (compactSolution == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < solver.getSolutionVariable().size(); i++) {
            if (compactSolution[i] > 0.01) {
                if (solver.getSolutionVariable().get(i).type == VariableType.Spell) {
                    sb.append(String.format(Locale.ROOT, "%3$s %1$s: %2$.2f", solver.getSolutionVariable().get(i).state.getBuffLabel() + "+" + solver.getSolutionVariable().get(i).cycle.name, compactSolution[i], solver.getSolutionVariable().get(i).segment));
                    sb.append(System.lineSeparator());
                } else {
                    sb.append(String.format(Locale.ROOT, "%3$s %1$s: %2$.2f", solver.getSolutionVariable().get(i).type, compactSolution[i], solver.getSolutionVariable().get(i).segment));
                    sb.append(System.lineSeparator());
                }
            }
        }
        return sb.toString();
    }
}
