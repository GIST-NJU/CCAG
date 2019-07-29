package combinatorial;

import java.util.Random;

/**
 * A two dimensional array for representing the t-way combination coverage, where
 *  a) value "-1" indicates that the t-tuple is an invalid combination;
 *  b) value "0" indicates that the t-tuple is uncovered;
 *  c) a "positive value" indicates the number of times the t-tuple is covered.
 */
public class BArray {

  public int[][] cover;
  public long zeros;
  private Random random = new Random();

  // zero positions: each zero position is represented as a pair <row, column>
  //public int[] z_row;
  //public int[] z_column;
  //public int z_total;

  public BArray(int row) {
    cover = new int[row][];
    //z_total = 0;
    zeros = 0;
    for (int i = 0; i < row; i++) {
      cover[i] = null;
    }
  }

  public BArray(int row, int column) {
    cover = new int[row][column];
    zeros = row * column;
    //z_total = row * column;
    //z_row = new int[z_total];
    //z_column = new int[z_total];
    int index = 0;
    for (int i = 0; i < row; i++) {
      for (int j = 0; j < column; j++) {
        cover[i][j] = 0;
        //z_row[index] = i;
        //z_column[index] = j;
        index += 1;
      }
    }
  }

  public int[][] getCover() {
    return cover;
  }


  /**
   * Copy current cover matrix into array.
   * @param array
   */
  public void copyCover(int[][] array) {
    for (int i = 0 ; i < array.length ; i++) {
      array[i] = new int[cover[i].length];
      System.arraycopy(cover[i], 0, array[i], 0, array[i].length);
    }
  }

  /**
   * Initialize a single row by zero.
   * @param index  index of row
   * @param column number of elements in that row
   */
  public void initializeRow(int index, int column) {
    if (cover[index] == null) {
      cover[index] = new int[column];
      for (int j = 0; j < column; j++)
        cover[index][j] = 0;
      //z_total += column;
      zeros += column;
    }
  }

  /**
   * If the matrix is initialized by row, then this method should be
   * used to initialize the zero positions.
   */
  public void initializeZeros() {
    //z_row = new int[z_total];
    //z_column = new int[z_total];
    int index = 0;
    for (int i = 0 ; i < cover.length ; i++ ) {
      for (int j = 0 ; j < cover[i].length ; j++) {
        //z_row[index] = i;
        //z_column[index] = j;
        index += 1;
      }
    }
  }

  /**
   * Return the index of row and column of a random zero position.
   * The zero_* variables will only be maintained in this method.
   */
  public Position getRandomZeroPosition2() {
    if (zeros > 0) {
      int index = random.nextInt((int) zeros);
      int count = -1;
      for (int row = 0; row < cover.length; row++) {
        for (int column = 0; column < cover[row].length; column++) {
          if (cover[row][column] == 0) {
            count++;
            if (count == index)
              return new Position(row, column);
          }
        }
      }
    }
    return null;
  }

  /*
  public Position getRandomZeroPosition() {
    while (z_total > 0) {
      int index = random.nextInt(z_total);
      int row = z_row[index];
      int column = z_column[index];
      if (cover[row][column] != 0) {
        // move the pair to the last position (i.e. index = zero_total - 1)
        int right = z_total - 1;
        z_row[index] = z_row[right];
        z_column[index] = z_column[right];
        z_total -= 1;
      }
      else {
        return new Position(row, column);
      }
    }
    return null;
  }
  */

  /**
   * Get the number of zero elements in the cover matrix.
   * @return the number of zeros
   */
  public long getZeros() {
    return zeros;
  }

  /**
   * Set the number of zero elements in the cover matrix.
   * @param zeros the number of zeros
   */
  public void setZeros(long zeros) {
    this.zeros = zeros;
  }

  /**
   * Get the element in row i and column j of Cover.
   * @param i index of row
   * @param j index of column
   */
  public int getElement(int i, int j) {
    return cover[i][j];
  }

  /**
   * Set the element in row i and column j of Cover to true or false.
   * @param i index of row
   * @param j index of column
   * @param value new value
   */
  public void setElement(int i, int j, int value) {
    if (cover[i][j] == 0 && value != 0)
      zeros--;
    else if (cover[i][j] != 0 && value ==0)
      zeros++;

    cover[i][j] = value;
  }

  /**
   * Let cover[i][j] = over[i][j] + 1
   * @param i index of row
   * @param j index of column
   */
  public void elementIncrease(int i, int j) {
    if (cover[i][j] == 0)
      zeros--;

    cover[i][j]++;
  }

  /**
   * Let cover[i][j] = over[i][j] - 1
   * @param i index of row
   * @param j index of column
   */
  public void elementDecrease(int i, int j) {
    if (cover[i][j] == 1)
      zeros++;

    cover[i][j]--;
  }

  /**
   * Convert the specified row of cover matrix into its string representation.
   * @param index the index of row
   */
  public String coverMatrixRow(int index) {
    StringBuilder sb = new StringBuilder();
    for (int e : cover[index])
      sb.append(e == -1 ? "-" : e).append(" ");
    return sb.toString();
  }

}

