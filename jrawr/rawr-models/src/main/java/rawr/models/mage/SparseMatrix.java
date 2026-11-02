// Ported from legacy/RAWR_WotLK_3.3.5a/Rawr.Mage/SparseMatrix.cs @ 46f190bee32964cc8df5fe853d95b8a343f5aa23
// The C# desktop class is `unsafe` with double*/int* fields; ported as index-based arrays
// (DECISIONS #4). The two C# compilation branches (BeginSafe/BeginUnsafe, array vs pointer)
// collapse into the same array signatures.
package rawr.models.mage;

import java.util.Arrays;

// only one should exist at a time, otherwise behavior unspecified
public class SparseMatrix {
    private double[] pData;
    double[] pValue;
    private int[] pRow;
    private int[] pCol;

    public void beginSafe(double[] pData, double[] pValue, int[] pRow, int[] pCol) {
        this.pData = pData;
        this.pValue = pValue;
        this.pRow = pRow;
        this.pCol = pCol;
    }

    public void beginUnsafe(double[] pData, double[] pValue, int[] pRow, int[] pCol) {
        this.pData = pData;
        this.pValue = pValue;
        this.pRow = pRow;
        this.pCol = pCol;
    }

    public void endUnsafe() {
        pData = null;
        pValue = null;
        pRow = null;
        pCol = null;
    }

    private int cols;
    private int rows;
    private int sparseIndex;
    private int lastCol;
    private ArraySet arraySet;

    private boolean finalized;

    public boolean getReadOnly() {
        return finalized;
    }

    public int getColumns() {
        return cols;
    }

    public int getRows() {
        return rows;
    }

    public SparseMatrix(int rows, int maxCols, ArraySet arraySet) {
        initialize(rows, maxCols, arraySet);
    }

    public void initialize(int rows, int maxCols, ArraySet arraySet) {
        this.arraySet = arraySet;
        if (rows > arraySet.sparseMatrixMaxRows || maxCols + 20 > arraySet.sparseMatrixMaxCols) {
            arraySet.sparseMatrixMaxRows = Math.max(rows, arraySet.sparseMatrixMaxRows);
            arraySet.sparseMatrixMaxCols = Math.max(maxCols + 20, arraySet.sparseMatrixMaxCols); // give some room for AddColumn
            arraySet.recreateSparseMatrixArrays();
        }
        this.rows = rows;
        this.cols = 0;
        //Array.Clear(data, 0, rows * cols); // only need to clear what will be used

        sparseIndex = 0;
        lastCol = 0;
        finalized = false;
    }

    public double get(int row, int col) {
        return arraySet.sparseMatrixData[col * rows + row];
    }

    public void set(int row, int col, double value) {
        if (finalized) throw new RuntimeException();
        if (col < lastCol) throw new RuntimeException();
        if (value == 0.0) return;
        if (col > lastCol) {
            for (int c = lastCol + 1; c <= col; c++) {
                arraySet.sparseMatrixCol[c] = sparseIndex;
            }
        }
        if (sparseIndex >= arraySet.sparseMatrixSparseSize) {
            arraySet.sparseMatrixSparseSize += (int) (rows * cols * 0.1);
            int[] newRow = new int[arraySet.sparseMatrixSparseSize];
            System.arraycopy(arraySet.sparseMatrixRow, 0, newRow, 0, arraySet.sparseMatrixRow.length);
            arraySet.sparseMatrixRow = newRow;
            double[] newValue = new double[arraySet.sparseMatrixSparseSize];
            System.arraycopy(arraySet.sparseMatrixValue, 0, newValue, 0, arraySet.sparseMatrixValue.length);
            arraySet.sparseMatrixValue = newValue;
        }
        arraySet.sparseMatrixRow[sparseIndex] = row;
        arraySet.sparseMatrixValue[sparseIndex] = value;
        arraySet.sparseMatrixData[col * rows + row] = value; // store data by columns, we always access by column so it will have better locality, we also never increase number of rows, only columns, that way we don't have to reposition data
        lastCol = col;
        sparseIndex++;
    }

    public void setElementUnsafe(int row, int col, double value) {
        if (pValue == null) {
            set(row, col, value);
            return;
        }
        if (value == 0.0) return;
        if (col > lastCol) {
            for (int c = lastCol + 1; c <= col; c++) {
                pCol[c] = sparseIndex;
            }
        }
        if (sparseIndex >= arraySet.sparseMatrixSparseSize) {
            setElementSafeFromUnsafe(row, col, value);
            return;
        }
        pRow[sparseIndex] = row;
        pValue[sparseIndex] = value;
        pData[col * rows + row] = value; // store data by columns, we always access by column so it will have better locality, we also never increase number of rows, only columns, that way we don't have to reposition data
        lastCol = col;
        sparseIndex++;
    }

    private void setElementSafeFromUnsafe(int row, int col, double value) {
        // C# does not allow to change a pinned reference, if we have to recreate arrays we have to move to safe code
        endUnsafe();
        arraySet.sparseMatrixSparseSize += (int) (rows * cols * 0.1);
        int[] newRow = new int[arraySet.sparseMatrixSparseSize];
        System.arraycopy(arraySet.sparseMatrixRow, 0, newRow, 0, arraySet.sparseMatrixRow.length);
        arraySet.sparseMatrixRow = newRow;
        double[] newValue = new double[arraySet.sparseMatrixSparseSize];
        System.arraycopy(arraySet.sparseMatrixValue, 0, newValue, 0, arraySet.sparseMatrixValue.length);
        arraySet.sparseMatrixValue = newValue;
        arraySet.sparseMatrixRow[sparseIndex] = row;
        arraySet.sparseMatrixValue[sparseIndex] = value;
        arraySet.sparseMatrixData[col * rows + row] = value; // store data by columns, we always access by column so it will have better locality, we also never increase number of rows, only columns, that way we don't have to reposition data
        lastCol = col;
        sparseIndex++;
    }

    public int addColumn() {
        finalized = false;
        cols++;
        if (cols > arraySet.sparseMatrixMaxCols) {
            throw new RuntimeException();
        }
        Arrays.fill(arraySet.sparseMatrixData, rows * (cols - 1), rows * (cols - 1) + rows, 0.0); // only need to clear what will be used
        return cols - 1;
    }

    public void endConstruction() {
        for (int c = lastCol + 1; c <= cols; c++) {
            arraySet.sparseMatrixCol[c] = sparseIndex;
        }
        finalized = true;
    }
}
