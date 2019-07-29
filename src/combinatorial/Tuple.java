package combinatorial;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Representation of a particular t-way combination.
 */
public class Tuple {

  public int[] test;     // test case representation, -1 indicates unfixed values
  public int[] position; // indexes of each parameter
  public int[] schema;   // corresponding values
  public int length;     // number of fixed values

  /* ------------------------------------- //
   * An example of a 2-way combination
   * test     = [0, -1, -1, 1, -1]
   * position = [0, 3]
   * schema   = [0, 1]
   * length   = 2
   * ------------------------------------- */

  public Tuple(final int[] t) {
    test = t.clone();
    length = 0;
    ArrayList<Integer> pos = new ArrayList<>();
    ArrayList<Integer> sch = new ArrayList<>();
    for (int k = 0; k < t.length; k++) {
      if (t[k] != -1) {
        pos.add(k);
        sch.add(t[k]);
        length++;
      }
    }
    position = pos.stream().mapToInt(i -> i).toArray();
    schema = sch.stream().mapToInt(i -> i).toArray();
  }

  public Tuple(final int[] pos, final int[] sch, int full_length) {
    position = pos.clone();
    schema = sch.clone();
    length = pos.length;

    test = new int[full_length];
    for (int i = 0, j = 0; i < full_length; i++) {
      if (i > position[length - 1])
        test[i] = -1;
      else if (i == position[j])
        test[i] = schema[j++];
      else
        test[i] = -1;
    }
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder();
    for (int i : test)
      str.append(i == -1 ? "- " : String.valueOf(i) + " ");
    return str.toString();
  }

  @Override
  public int hashCode() {
    return toString().hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == null || !(other instanceof Tuple))
      return false;
    if (other == this)
      return true;

    Tuple otherTuple = (Tuple) other;
    return Arrays.equals(test, otherTuple.test);
  }

}
