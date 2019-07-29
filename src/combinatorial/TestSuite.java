package combinatorial;

import handler.ConstraintHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * A basic data structure of CT test suite.
 */
public class TestSuite {

  public List<int[]> suite;

  public TestSuite() {
    this.suite = new ArrayList<>();
  }

  public TestSuite(List<int[]> suite) {
    this.suite = suite;
  }

  public int size() {
    return suite.size() ;
  }

  /**
   * Assign unfixed parameters at random.
   * @param model the test model
   * @param handler the constraint handler
   */
  public void assignUnfixedValues(CTModel model, ConstraintHandler handler) {
    Random random = new Random();
    for (int[] each : suite) {
      for (int i = 0; i < each.length; i++) {
        if (each[i] == -1) {
          each[i] = random.nextInt(model.value[i]);
          while (!handler.isValid(each))
            each[i] = random.nextInt(model.value[i]);
        }
      }
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (int[] x : suite) {
      for (int val : x)
        sb.append(val).append(" ");
      sb.append("\n");
    }
    return sb.toString();
  }


}
