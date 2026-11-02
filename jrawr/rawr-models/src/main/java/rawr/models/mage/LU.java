// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/LU.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The C# desktop class is `unsafe` with double*/int* fields and explicit pointer walks; ported
// with index-based arrays and int cursors, identical arithmetic and loop order (DECISIONS #4).
// The hand-optimized desktop FSolveU/FSolveL/BSolveL pointer walks are represented by the
// array-based bodies that visit the same elements in the same order (the private static Copy
// helper existed only for the desktop FSolveU and is therefore not needed).
package rawr.models.mage;

public class LU {
    private int size;
    public int etaSize;
    private ArraySet arraySet;

    // Ln...L0 B = P U Q
    // B = (Ln...L0)inv P U Q

    // Bx = b
    // (Ln...L0)inv P U Q x = b
    // P U Q x = (Ln...L0)b
    // U Q x = [P' (Ln...L0)b] = U z
    // Q x = z => x = Q' z

    // B~ = B + aj ep'
    // Ln...L0 B~ = P U~ Q = P U Q + Ln...L0 aj ep'
    // U~ = U + [P' Ln...L0 aj] ep' Q'

    // y B = z
    // y (Ln...L0)inv P U Q = z
    // [y (Ln...L0)inv P] U = z Q' = yy U
    // y (Ln...L0)inv P = yy
    // y = yy P' (Ln...L0)
    // y' = L0'...Ln' P yy'
    // Li' v = (I + eta ep')' v = v + ep (eta' v)

    // L1inv B = P1 U1 Q1
    // L1inv B = P2 U2 Q2
    // P2' L1inv B Q2' = U2
    // U3 = E U2 = E P2' L1inv B Q2'
    // [E P2'] L1inv B = [E U2] Q2 = U3 Q2 = U3 Q3
    // [P2 E P2'] L1inv B = P2 U3 Q3 = P3 U3 Q3
    // E = I + eta ep'
    // P2 E P2' = I + P2 eta ep' P2'
    // ep' P2' = (P2 ep)'

    // E = I + ep eta'
    // P E P' = I + P ep eta' P'
    // eta' P' = (P eta)'

    private double[] U;
    private double[] sL;
    private int[] P;
    private int[] Q;
    private int[] LJ;
    private int[] sLI;
    private int[] sLstart;
    private double[] c;
    private double[] c2;

    public void beginSafe() {
        this.U = arraySet.LU_U;
        this.sL = arraySet.lUsparseL;
        this.P = arraySet.LU_P;
        this.Q = arraySet.LU_Q;
        this.LJ = arraySet.LU_LJ;
        this.sLI = arraySet.lUsparseLI;
        this.sLstart = arraySet.lUsparseLstart;
        this.c = arraySet.lUcolumn;
        this.c2 = arraySet.lUcolumn2;
    }

    public void beginUnsafe(double[] U, double[] sL, int[] P, int[] Q, int[] LJ, int[] sLI, int[] sLstart, double[] c, double[] c2) {
        this.U = U;
        this.sL = sL;
        this.P = P;
        this.Q = Q;
        this.LJ = LJ;
        this.sLI = sLI;
        this.sLstart = sLstart;
        this.c = c;
        this.c2 = c2;
    }

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

    private boolean singular;

    public boolean getSingular() {
        return singular;
    }

    public void setSingular(boolean value) {
        singular = value;
    }

    private int rank;

    public int getRank() {
        return rank;
    }

    public void setRank(int value) {
        rank = value;
    }

    // data will be modified, if you need to retain it clean pass a clone
    public LU(int size, ArraySet arraySet) {
        initialize(size, arraySet);
    }

    public void initialize(int size, ArraySet arraySet) {
        this.size = size;
        this.arraySet = arraySet;
        etaSize = 0;
        if (size > arraySet.lUsizeMax) {
            arraySet.lUsizeMax = size;
            arraySet.recreateLUArrays();
        }
    }

    public void bSolve(double[] b) {
        bSolveU(b, c, 0.00000001);
        bSolveL(c, b, 0.00000001);
    }

    public void bSolve(double[] b, double epsZero) {
        bSolveU(b, c, epsZero);
        bSolveL(c, b, epsZero);
    }

    public void bSolveU(double[] b, double[] c, double epsZero) {
        int i, k;
        for (i = 0; i < size; i++) {
            c[i] = b[Q[i]]; // shuffle Q
        }
        for (k = 0; k < size; k++) {
            if (U[k * size + k] != 0 && Math.abs(c[k]) > epsZero) {
                c[k] /= U[k * size + k];
                for (i = k + 1; i < size; i++) {
                    c[i] -= c[k] * U[k * size + i];
                }
            } else c[k] = 0; // value underspecified
        }
    }

    public void bSolveUnit(double[] b, int col) {
        bSolveUUnit(col, c);
        bSolveL(c, b, 0.00000001);
    }

    public void bSolveUUnit(int col, double[] c) {
        int i, k;
        LP.zero(c, size);
        for (i = 0; ; i++) {
            if (Q[i] == col) {
                col = i;
                break;
            }
        }
        //c[col] = 1.0;
        /*for (i = 0; i < size; i++)
        {
            c[i] = b[Q[i]]; // shuffle Q
        }*/

        if (U[col * size + col] != 0) {
            c[col] = 1.0 / U[col * size + col];
            for (i = col + 1; i < size; i++) {
                c[i] -= c[col] * U[col * size + i];
            }
        } else return;
        //else c[col] = 0; // value underspecified

        for (k = col + 1; k < size; k++) {
            if (U[k * size + k] != 0 && Math.abs(c[k]) > 0.000001) {
                c[k] /= U[k * size + k];
                for (i = k + 1; i < size; i++) {
                    c[i] -= c[k] * U[k * size + i];
                }
            } else c[k] = 0; // value underspecified
        }
    }

    public void bSolveL(double[] b, double[] c, double epsZero) {
        int i, j;
        for (i = 0; i < size; i++) {
            c[P[i]] = b[i];
        }
        for (j = etaSize - 1; j >= 0; j--) {
            if (j < size) {
                // eta columns from initial decomposition
                int row = LJ[j]; // we're updating row element
                // c~ = c + erow (eta' c)
                double f = 0.0;
                /*for (i = 0; i < size; i++)
                {
                    f += c[i] * L[j * size + i];
                }*/
                int maxi = sLstart[j + 1];
                for (i = sLstart[j]; i < maxi; i++) {
                    f += c[sLI[i]] * sL[i];
                }
                c[row] += f;
            } else {
                // eta rows from update
                int row = LJ[j]; // we're updating using row, if element is zero we can skip
                // c~ = c + eta (erow' c)
                double f = c[row];
                if (Math.abs(f) >= epsZero) {
                    int maxi = sLstart[j + 1];
                    for (i = sLstart[j]; i < maxi; i++) {
                        c[sLI[i]] += f * sL[i];
                    }
                }
            }
        }
    }

    public void fSolve(double[] b) {
        fSolveL(b, c, 0.00000001);
        fSolveU(c, b, 0.00000001);
    }

    public void fSolve(double[] b, double epsZero) {
        fSolveL(b, c, epsZero);
        fSolveU(c, b, epsZero);
    }

    public void fSolveU(double[] b, double[] c, double epsZero) {
        int i, k;
        for (i = 0; i < size; i++) {
            c2[i] = b[i];
        }
        for (k = size - 1; k >= 0; k--) {
            if (U[k * size + k] != 0 && Math.abs(c2[k]) > epsZero) {
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

    public void fSolveL(double[] b, double[] c, double epsZero) {
        // perform all eta operations and finally apply row permutation P
        int i, j;
        for (j = 0; j < etaSize; j++) {
            if (j < size) {
                // eta columns from initial decomposition
                int row = LJ[j]; // we're updating using row, if element is zero we can skip
                // b~ = b + eta (erow' b)
                double f = b[row];
                if (Math.abs(f) >= epsZero) {
                    /*for (i = 0; i < size; i++)
                    {
                        b[i] += f * L[j * size + i];
                    }*/
                    int maxi = sLstart[j + 1];
                    for (i = sLstart[j]; i < maxi; i++) {
                        b[sLI[i]] += f * sL[i];
                    }
                }
            } else {
                // eta rows from updates
                // b~ = b + erow (eta' b)
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

    // Performance Log, start, load Kavan, repeat dps time = 1
    // Primal=6.08148534180949E-05, Decompose=3.38125119182951E-05 implementation LU
    // Primal=7.7155968256824E-05, Decompose=4.80555577214333E-05  initial implementation LU2 with column pivoting
    // Primal=4.44975972839637E-05, Decompose=1.34763462504299E-05 LU2 no column pivoting
    // Primal=4.10642857990147E-05, Decompose=1.28461907883656E-05 FSolveL => sparse
    // Primal=3.96870122800117E-05, Decompose=1.36131169065829E-05 BSolveL => sparse
    // Primal=2.73956138996725E-05, Decompose=4.29207679646327E-06 update rule

    // SMP
    // Primal=0.000248019081795013, Decompose=7.31092948026856E-05 LU2 no column pivoting
    // Primal=0.000217759250424469, Decompose=7.53546090376799E-05 sparse L
    // Primal=0.00013135285568704, Decompose=1.44506264553419E-05 update rule

    public void decompose() {
        etaSize = 0; // reset eta file
        //Array.Clear(_L, 0, _L.Length);
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
                for (k = j; k < size; k++) // columns before j have zeros in these rows
                {
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

            if (etaSize >= arraySet.lUetaMax) throw new RuntimeException();

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
                    //L[etaSize * size + pi] = f; // eta element that eliminates element in row i
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
            LJ[etaSize] = P[j]; // yes I had to guess, but I used unit test to make sure it works :)
            etaSize++;
        }
    }

    // B~ = B + aj ecol'
    // Ln...L0 B~ = P U~ Q = P U Q + Ln...L0 aj ecol'
    // U~ = U + [P' Ln...L0 aj] ecol' Q'
    // a = [P' Ln...L0 aj]

    // replace column col in basis B with aj
    public boolean update(double[] a, int col, double[] pivot) {
        int i, j, k;
        int[] Q = this.Q;
        double[] U = this.U;
        for (j = 0; ; j++) {
            if (Q[j] == col) break;
        }
        col = j;

        double ujj = U[j * size + j];

        // place in column col = j
        // rotate columns to get upper hessenberg form (col...lastnz)
        int lastnz = size - 1;
        for (; lastnz >= 0; lastnz--) {
            if (Math.abs(a[lastnz]) > 0.000001) break;
        }

        if (lastnz < col) {
            pivot[0] = 0.0;
            return false;
        }

        // XXaXXXX    XXXXXaX
        //  XaXXXX     XXXXaX
        //   aXXXX      XXXaX   <--- col
        //   aXXXX  =>  XXXaX
        //   a XXX       XXaX
        //   a  XX        XaX   <--- lastnz
        //       X          X

        int Qcol = Q[col];
        for (j = col; j < lastnz; j++) {
            Q[j] = Q[j + 1];
        }
        Q[lastnz] = Qcol;

        // index translation of the C# pointer walk over a (a++ per row):
        // rows 0..col shift columns [col, lastnz), rows col+1..lastnz shift columns [row - 1, lastnz),
        // then the inserted column is a[0..lastnz].
        int ai = 0;
        for (i = 0; i <= col; i++) {
            for (j = col; j < lastnz; j++) {
                U[i * size + j] = U[i * size + j + 1];
            }
            U[i * size + lastnz] = a[ai];
            ai++;
        }
        for (i = col + 1; i <= lastnz; i++) {
            for (j = i - 1; j < lastnz; j++) {
                U[i * size + j] = U[i * size + j + 1];
            }
            U[i * size + lastnz] = a[ai];
            ai++;
        }

        // eliminate row at index col up to lastnz using rows below it
        // we're eliminating on previous diagonals, so we know it is safe (we shouldn't update on a singular basis)

        // E = I + ep eta'
        // P E P' = I + P ep eta' P'
        // eta' P' = (P eta)'

        if (etaSize >= arraySet.lUetaMax) throw new RuntimeException();

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

        // XXXXXXX    XXXXXXX
        //  XXXXXX     XXXXXX
        //      XX      XXXXX   <--- col
        //   XXXXX  =>   XXXX
        //    XXXX        XXX
        //     XXX         XX   <--- lastnz
        //       X          X

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
        pivot[0] = c[lastnz] / ujj;
        // C# desktop branch semantics (the Silverlight/Base variant also sets Singular=true on the
        // lastnz<col early return and leaves it untouched otherwise).
        if (Math.abs(U[lastnz * size + lastnz]) < 0.000001) {
            singular = true;
        } else {
            singular = false;
        }
        return true;
    }
}
