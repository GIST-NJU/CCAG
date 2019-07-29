package combinatorial;

/**
 * A position is a 2-test indicating the indexes of row and column
 * of a particular position in an array.
 */
public class Position {
  public int row ;
  public int column ;

  public Position(int row, int column) {
    this.row = row ;
    this.column = column ;
  }

  @Override
  public String toString() {
    return String.format("(%d, %d)", row, column);
  }

  @Override
  public int hashCode() {
    return 31 * (31 + row) + column ;
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof Position))
      return false;
    if (other == this)
      return true;

    Position o = (Position)other ;
    return o.row == row && o.column == column ;
  }
}
