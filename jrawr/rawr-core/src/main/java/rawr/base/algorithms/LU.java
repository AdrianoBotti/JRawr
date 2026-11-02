// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Base/Algorithms/LU.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The C# desktop class is `unsafe` (double*/int* fields, `fixed` pointers); ported with index-based
// arrays and int cursors, identical arithmetic and loop order (DECISIONS #4/#59). The C# `Update`
// has an `out double pivot`; the Java port returns the pivot. `InvalidOperationException` (eta file
// exhausted) maps to `IllegalStateException`.
package rawr.base.algorithms;

/**
 * A sparse LU factorization of a square matrix with partial pivoting and an "eta" update capability,
 * used to solve systems of linear equations {@code B * x = b}.
 *
 * <p>An LU factorization writes a square matrix as the product of a lower-triangular matrix and an
 * upper-triangular matrix. Once factored, solving for a new right-hand side costs only two cheap
 * triangular substitutions instead of a full factorization. This implementation does not store the
 * factors as dense matrices: each elimination step is kept as a sparse <i>eta vector</i> (the
 * multipliers that zero one column), while the upper factor {@code U} stays a dense row-major
 * {@code double[size * size]} array. The sparse eta files matter because the systems that models
 * build (rotation state spaces, linear programs) are large but extremely sparse.</p>
 *
 * <h2>Storage layout ({@link ArraySet})</h2>
 *
 * <p>All arrays live in an {@link ArraySet}, normally borrowed from {@link ArrayPool} and used by one
 * {@code LU} at a time:</p>
 * <ul>
 *   <li>{@code luU} &mdash; dense row-major upper factor; element {@code (row, column)} is at
 *       {@code row * size + column}.</li>
 *   <li>{@code luSparseL} / {@code luSparseLI} / {@code luSparseLstart} &mdash; the packed non-zero
 *       entries of the eta vectors: vector {@code j} occupies positions
 *       {@code [luSparseLstart[j], luSparseLstart[j + 1])} of the value and index arrays.</li>
 *   <li>{@code luLj} &mdash; for each eta vector, the column/row it eliminates.</li>
 *   <li>{@code luP} / {@code luQ} &mdash; the row and column permutations produced by pivoting.</li>
 *   <li>{@code luColumn} / {@code luColumn2} &mdash; scratch vectors for the two solve directions.</li>
 * </ul>
 *
 * <p>The C# original is an {@code unsafe} class holding raw pointers into those arrays; this port
 * keeps index-based arrays and integer cursors with identical arithmetic and loop order
 * ({@code docs/dev/DECISIONS.md} #4 and #59). The original's {@code out double pivot} parameter of
 * {@link #update(double[], int)} is returned instead, and its {@code InvalidOperationException}
 * (eta file exhausted) maps to {@link IllegalStateException}.</p>
 *
 * <h2>Lifecycle</h2>
 *
 * <p>There are two ways to bind the arrays. The normal path constructs the object with an
 * {@code ArraySet} and calls {@link #beginSafe()}. The hot path used by {@link MarkovProcess} calls
 * {@link #beginUnsafe(double[], double[], int[], int[], int[], int[], int[], double[], double[])}
 * with the pool's raw arrays, avoiding any copying, and must pair it with {@link #endUnsafe()}
 * before releasing the set. After {@link #decompose()} the factorization can absorb single-column
 * changes through {@link #update(double[], int)} without refactoring; each update appends one more
 * eta vector.</p>
 *
 * <h2>Solving directions</h2>
 *
 * <p>With the product form {@code (Ln ... L0) * B = P * U * Q} ({@code L} the eta transforms,
 * {@code P}/{@code Q} the permutations), the two public solves mean:</p>
 * <ul>
 *   <li>{@link #fSolve(double[])} solves {@code B * x = b} in place. It applies the eta transforms
 *       forward (in the order they were recorded), applies {@code P}, back-substitutes the upper
 *       triangular factor and finally undoes {@code Q}.</li>
 *   <li>{@link #bSolve(double[])} solves the dual system {@code y * B = b} (a row vector times the
 *       matrix). It starts by undoing {@code Q}, forward-substitutes, then applies the eta transforms
 *       in reverse order and ends with {@code P}. This is the direction used to recover pivot
 *       information.</li>
 *   <li>The {@code fSolveL}/{@code fSolveU} and {@code bSolveL}/{@code bSolveU} halves are public
 *       because callers combine a solve with their own intermediate vector.</li>
 * </ul>
 *
 * <p>Fidelity notes: the pivot zero tests use the original's absolute thresholds
 * ({@code 0.1}, {@code 1e-6}, {@code 1e-8}) and missing pivots set entries to zero rather than
 * throwing, so the solver degrades gracefully on singular systems and reports them through
 * {@link #singular} and {@link #rank}. Do not "clean up" these constants or the loop order.</p>
 *
 * @see ArrayPool
 * @see MarkovProcess
 */
public class LU {
    /**
     * The reusable array bundle a factorization works on: one dense upper factor plus the sparse eta
     * file, permutations and scratch columns. Sizes start at 200 rows; constructing an {@link LU} for
     * a larger matrix calls {@link #recreateArrays()} with the new size, discarding the old arrays
     * (a borrower must not keep references across that call). {@code luEtaMax} is
     * {@code max(luSizeMax + 100, 2 * luSizeMax)}: the number of column updates the factorization
     * can absorb before the eta file is full.
     */
    public static class ArraySet {
        public int luEtaMax;
        public int luSizeMax;
        public int[] luP;
        public int[] luQ;
        public double[] luSparseL; // packed non-zero elements of eta vectors
        public int[] luSparseLI; // indices of non-zero elements
        public int[] luSparseLstart; // start index of non-zero elements of eta vector i
        public int[] luLj; // col/row eta index   Li = I + L[i*size:(i+1)*size-1] * e_LI[i]'
        public double[] luColumn;
        public double[] luColumn2;
        public double[] luU;

        public ArraySet() {
            luSizeMax = 200;
            recreateArrays();
        }

        /**
         * Reallocates every array for the current {@code luSizeMax}. A factorization that still
         * references the old arrays becomes invalid; borrowers must not hold references across this
         * call.
         */
        public void recreateArrays() {
            luU = new double[luSizeMax * luSizeMax];
            luEtaMax = Math.max(luSizeMax + 100, 2 * luSizeMax);
            luP = new int[luSizeMax];
            luQ = new int[luSizeMax];
            luLj = new int[luEtaMax];
            luSparseL = new double[luEtaMax * luSizeMax];
            luSparseLI = new int[luEtaMax * luSizeMax];
            luSparseLstart = new int[luEtaMax];
            luColumn = new double[luSizeMax];
            luColumn2 = new double[luSizeMax];
        }
    }

    private int size;
    public int etaSize;
    private ArraySet arraySet;

    private double[] U;
    private double[] sL;
    private int[] P;
    private int[] Q;
    private int[] LJ;
    private int[] sLI;
    private int[] sLstart;
    private double[] c;
    private double[] c2;

    /**
     * Binds the solve fields to this factorization's {@link ArraySet} arrays. The safe counterpart of
     * {@link #beginUnsafe(double[], double[], int[], int[], int[], int[], int[], double[], double[])}.
     */
    public void beginSafe() {
        this.U = arraySet.luU;
        this.sL = arraySet.luSparseL;
        this.P = arraySet.luP;
        this.Q = arraySet.luQ;
        this.LJ = arraySet.luLj;
        this.sLI = arraySet.luSparseLI;
        this.sLstart = arraySet.luSparseLstart;
        this.c = arraySet.luColumn;
        this.c2 = arraySet.luColumn2;
    }

    /**
     * Binds the solve fields to caller-provided arrays, in the exact order of the {@link ArraySet}
     * fields ({@code luU}, {@code luSparseL}, {@code luP}, {@code luQ}, {@code luLj},
     * {@code luSparseLI}, {@code luSparseLstart}, {@code luColumn}, {@code luColumn2}). No copies are
     * made; pair this with {@link #endUnsafe()} before returning the arrays to their owner. This is
     * the port of the C# {@code fixed} pointer path used for zero-allocation solves.
     */
    public void beginUnsafe(double[] u, double[] sl, int[] p, int[] q, int[] lj, int[] sli, int[] slstart, double[] cc, double[] cc2) {
        this.U = u;
        this.sL = sl;
        this.P = p;
        this.Q = q;
        this.LJ = lj;
        this.sLI = sli;
        this.sLstart = slstart;
        this.c = cc;
        this.c2 = cc2;
    }

    /**
     * Drops the array references bound by {@code beginUnsafe}. The arrays themselves are neither
     * modified nor freed; the factorization cannot be used until the fields are bound again.
     */
    public void endUnsafe() {
        this.U = null;
        this.sL = null;
        this.P = null;
        this.Q = null;
        this.LJ = null;
        this.sLI = null;
        this.sLstart = null;
        this.c = null;
        this.c2 = null;
    }

    public boolean singular;

    public int rank;

    /**
     * Prepares a factorization for a matrix of {@code size} rows and columns. No matrix data has to
     * exist yet: fill the upper-factor array and call {@link #decompose()}. If {@code size} exceeds
     * the set's capacity, the set is enlarged with {@link ArraySet#recreateArrays()}.
     */
    // data will be modified, if you need to retain it clean pass a clone
    public LU(int size, ArraySet arraySet) {
        this.size = size;
        this.arraySet = arraySet;
        etaSize = 0;
        if (size > arraySet.luSizeMax) {
            arraySet.luSizeMax = size;
            arraySet.recreateArrays();
        }
    }

    /**
     * Solves the dual system {@code y * B = b} in place: {@code b} is replaced by the solution.
     */
    public void bSolve(double[] b) {
        bSolveU(b, c);
        bSolveL(c, b);
    }

    /**
     * Half of {@link #bSolve(double[])}: undoes {@code Q} and forward-substitutes the upper
     * triangular factor, writing the result into {@code c}. Entries whose diagonal is near zero are
     * treated as underspecified and set to 0 instead of producing infinities.
     */
    public void bSolveU(double[] b, double[] c) {
        int i, k;
        for (i = 0; i < size; i++) {
            c[i] = b[Q[i]]; // shuffle Q
        }
        for (k = 0; k < size; k++) {
            if (U[k * size + k] != 0 && Math.abs(c[k]) > 0.000001) {
                c[k] /= U[k * size + k];
                for (i = k + 1; i < size; i++) {
                    c[i] -= c[k] * U[k * size + i];
                }
            } else c[k] = 0; // value underspecified
        }
    }

    /**
     * Half of {@link #bSolve(double[])}: applies the eta transforms in reverse creation order and
     * finally the row permutation {@code P}, writing the result into {@code c}. Eta vectors created
     * by {@link #update(double[], int)} are told apart from the initial ones by their position
     * ({@code j < size}).
     */
    public void bSolveL(double[] b, double[] c) {
        int i, j;
        for (i = 0; i < size; i++) {
            c[P[i]] = b[i];
        }
        for (j = etaSize - 1; j >= 0; j--) {
            if (j < size) {
                // eta columns from initial decomposition
                int row = LJ[j]; // we're updating row element
                double f = 0.0;
                int maxi = sLstart[j + 1];
                for (i = sLstart[j]; i < maxi; i++) {
                    f += c[sLI[i]] * sL[i];
                }
                c[row] += f;
            } else {
                // eta rows from update
                int row = LJ[j]; // we're updating using row, if element is zero we can skip
                double f = c[row];
                if (Math.abs(f) >= 0.00000001) {
                    int maxi = sLstart[j + 1];
                    for (i = sLstart[j]; i < maxi; i++) {
                        c[sLI[i]] += f * sL[i];
                    }
                }
            }
        }
    }

    /**
     * Solves {@code B * x = b} in place: {@code b} is replaced by the solution. This is the primal
     * direction used by {@link MarkovProcess} and the one to reach for in general.
     */
    public void fSolve(double[] b) {
        fSolveL(b, c);
        fSolveU(c, b);
    }

    /**
     * Half of {@link #fSolve(double[])}: back-substitutes the upper triangular factor into {@code c}
     * and undoes {@code Q}, writing the final solution into {@code c}. Underspecified entries are set
     * to 0.
     */
    public void fSolveU(double[] b, double[] c) {
        int i, k;
        for (i = 0; i < size; i++) {
            c2[i] = b[i];
        }
        for (k = size - 1; k >= 0; k--) {
            if (U[k * size + k] != 0 && Math.abs(c2[k]) > 0.000001) {
                c2[k] /= U[k * size + k];
                for (i = 0; i < k; i++) {
                    c2[i] -= c2[k] * U[i * size + k];
                }
            } else c2[k] = 0; // value underspecified
        }
        // shuffle Q
        for (i = 0; i < size; i++) {
            c[Q[i]] = c2[i];
        }
    }

    /**
     * Half of {@link #fSolve(double[])}: applies all eta transforms in creation order and finally
     * the row permutation {@code P}, writing the result into {@code c}; {@code b} is scratch space.
     */
    public void fSolveL(double[] b, double[] c) {
        // perform all eta operations and finally apply row permutation P
        int i, j;
        for (j = 0; j < etaSize; j++) {
            if (j < size) {
                // eta columns from initial decomposition
                int row = LJ[j]; // we're updating using row, if element is zero we can skip
                double f = b[row];
                if (Math.abs(f) >= 0.00000001) {
                    int maxi = sLstart[j + 1];
                    for (i = sLstart[j]; i < maxi; i++) {
                        b[sLI[i]] += f * sL[i];
                    }
                }
            } else {
                // eta rows from updates
                int row = LJ[j]; // we're updating row element
                double f = 0.0;
                int maxi = sLstart[j + 1];
                for (i = sLstart[j]; i < maxi; i++) {
                    f += b[sLI[i]] * sL[i];
                }
                b[row] += f;
            }
        }
        for (i = 0; i < size; i++) {
            c[i] = b[P[i]];
        }
    }

    /**
     * Factors the matrix currently stored in the upper-factor array and fills the eta file from
     * scratch ({@code etaSize} is reset to 0).
     *
     * <p>Columns are eliminated left to right. For each column the algorithm selects the
     * largest-magnitude entry at or below the diagonal (row pivoting); if even that is small (below
     * 0.1) it widens the search to later columns, so it can swap in another column rather than accept
     * a zero pivot (column pivoting costs more, but the original prefers it over a zero). Row and
     * column swaps are recorded in {@code P} and {@code Q}. The multipliers that zero the column
     * below the diagonal are stored sparsely as one eta vector and immediately applied to the
     * remaining rows of the upper factor.</p>
     *
     * <p>If the best available pivot is below {@code 1e-6} in absolute value, {@link #singular}
     * becomes true and {@link #rank} records the first rank-deficient column; elimination still
     * proceeds so partial results remain usable.</p>
     *
     * @throws IllegalStateException if the eta file cannot hold one vector per column
     */
    public void decompose() {
        etaSize = 0; // reset eta file
        singular = false;
        rank = size;

        int i, j, k, pivi, pivj;
        // init P, Q
        for (i = 0; i < size; i++) {
            P[i] = i;
            Q[i] = i;
        }

        // eliminate by columns
        for (j = 0; j < size; j++) {
            // select pivot
            pivi = j;
            pivj = j;
            double max = Math.abs(U[pivi * size + pivj]);
            double newmax;

            for (i = j; i < size; i++) {
                if ((newmax = Math.abs(U[i * size + j])) > max) {
                    pivi = i;
                    pivj = j;
                    max = newmax;
                }
            }

            if (max < 0.1) {
                // don't allow a 0 if you can help it, even if it costs more to pivot columns
                for (k = j + 1; k < size; k++) {
                    for (i = j; i < size; i++) {
                        if ((newmax = Math.abs(U[i * size + k])) > max) {
                            pivi = i;
                            pivj = k;
                            max = newmax;
                        }
                    }
                }
            }

            if (pivi != j) {
                for (k = j; k < size; k++) { // columns before j have zeros in these rows
                    double t = U[pivi * size + k];
                    U[pivi * size + k] = U[j * size + k];
                    U[j * size + k] = t;
                }

                int tmp = P[pivi];
                P[pivi] = P[j];
                P[j] = tmp;
            }

            if (pivj != j) {
                for (k = 0; k < size; k++) {
                    double t = U[k * size + pivj];
                    U[k * size + pivj] = U[k * size + j];
                    U[k * size + j] = t;
                }

                int tmp = Q[pivj];
                Q[pivj] = Q[j];
                Q[j] = tmp;
            }

            // eliminate, construct eta vector

            if (etaSize >= arraySet.luEtaMax) throw new IllegalStateException();

            if (!singular && Math.abs(max) < 0.000001) {
                singular = true;
                rank = j;
            }

            sLstart[etaSize + 1] = sLstart[etaSize];
            double a = 1 / U[j * size + j];
            for (i = j + 1; i < size; i++) {
                int pi = P[i];
                double f = -U[i * size + j] * a;
                U[i * size + j] = 0;
                if (Math.abs(f) > 0.00000001) {
                    // sparse L construction
                    sL[sLstart[etaSize + 1]] = f;
                    sLI[sLstart[etaSize + 1]] = pi;
                    sLstart[etaSize + 1]++;
                    // update U
                    for (k = j + 1; k < size; k++) {
                        U[i * size + k] += f * U[j * size + k];
                    }
                }
            }
            LJ[etaSize] = P[j];
            etaSize++;
        }
    }

    /**
     * Replaces one matrix column (the one whose original index is {@code col}) with the vector
     * {@code a} and folds the change into the factorization by appending a single eta vector, without
     * refactoring. This is the basis-update operation of the simplex method, where only one column
     * changes between pivots.
     *
     * <p>The method locates the column in the permuted ordering, rotates the new column into an
     * upper-Hessenberg shape, eliminates the sub-diagonal entries using the rows below (recording the
     * multipliers as the new eta vector), and rotates rows back to triangular form. If the new
     * diagonal is near zero ({@code 1e-6}), {@link #singular} is set.</p>
     *
     * @return the pivot value {@code c[lastnz] / ujj}: the replaced column's element moved onto the
     *         new diagonal divided by the previous diagonal element at the column's old position.
     *         Callers use it for their own objective bookkeeping.
     * @throws IllegalStateException if the eta file is full
     */
    // B~ = B + aj ecol'
    // replace column col in basis B with aj
    public double update(double[] a, int col) {
        int i, j, k;
        for (j = 0; j < size; j++) {
            if (Q[j] == col) break;
        }
        col = j;

        double ujj = U[j * size + j];

        // place in column col = j
        // rotate columns to get upper hessenberg form (col...lastnz)
        int lastnz = -1;
        for (i = 0; i < size; i++) {
            if (Math.abs(a[i]) > 0.000001) lastnz = i;
        }

        int Qcol = Q[col];
        for (j = col; j < lastnz; j++) {
            Q[j] = Q[j + 1];
            for (i = 0; i <= j + 1; i++) {
                U[i * size + j] = U[i * size + j + 1];
            }
        }
        Q[lastnz] = Qcol;
        for (i = 0; i <= lastnz; i++) {
            U[i * size + lastnz] = a[i];
        }

        // eliminate row at index col up to lastnz using rows below it

        if (etaSize >= arraySet.luEtaMax) throw new IllegalStateException();

        sLstart[etaSize + 1] = sLstart[etaSize];

        for (j = col; j < lastnz; j++) {
            int pj = P[j + 1];
            double f = -U[col * size + j] / U[(j + 1) * size + j];
            U[col * size + j] = 0;
            if (Math.abs(f) > 0.00000001) {
                // sparse L construction
                sL[sLstart[etaSize + 1]] = f;
                sLI[sLstart[etaSize + 1]] = pj;
                sLstart[etaSize + 1]++;
                // update U
                for (k = j + 1; k < size; k++) {
                    U[col * size + k] += f * U[(j + 1) * size + k];
                }
            }
        }
        LJ[etaSize] = P[col];
        etaSize++;

        // rotate rows to get upper triangular form (col...lastnz)

        // store col in temp
        int Pcol = P[col];
        for (j = lastnz; j < size; j++) {
            c[j] = U[col * size + j];
        }
        for (i = col; i < lastnz; i++) {
            P[i] = P[i + 1];
            for (j = (i == 0 ? 0 : i - 1); j < size; j++) {
                U[i * size + j] = U[(i + 1) * size + j];
            }
        }
        P[lastnz] = Pcol;
        if (lastnz > 0) U[lastnz * size + lastnz - 1] = 0.0;
        for (j = lastnz; j < size; j++) {
            U[lastnz * size + j] = c[j];
        }
        double pivot = c[lastnz] / ujj;
        if (Math.abs(U[lastnz * size + lastnz]) < 0.000001) {
            singular = true;
        }
        return pivot;
    }
}
