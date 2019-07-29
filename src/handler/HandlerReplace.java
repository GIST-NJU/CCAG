package handler;

import combinatorial.ALG;
import combinatorial.CTModel;
import combinatorial.TestSuite;

import java.util.ArrayList;
import java.util.Collections;


/**
 * An implementation of the Replace constraint handler (MFT based).
 */
public class HandlerReplace implements ConstraintHandler {

  public HandlerReplace() {
  }

  /**
   * The process executed before test suite generation.
   * [Replace]: do nothing (assume an unconstrained test model).
   *
   * @param model the test model of CT
   */
  public void pre(CTModel model) {
  }

  /**
   * The process executed after test suite generation.
   * [Replace]: remove invalid test cases, and then generate new test cases
   * to cover the uncovered combinations.
   *
   * @param model the test model of CT
   * @param ts    the generated test suite
   */
  public void post(CTModel model, TestSuite ts) {
    // the new test suite
    TestSuite ts_new = new TestSuite();

    // create a Verify constraint handler
    ConstraintHandler verify = new HandlerVerify();
    verify.pre(model);

    // re-initialise the model
    model.initialization();
    model.removeInvalidCombinations(verify);

    // update Cover Matrix
    for (int[] tc : ts.suite)
      model.coverUpdate(tc);

    // calculate the set of yet-to-be covered combinations
    ArrayList<int[]> R = new ArrayList<>();
    for (int[] tc : ts.suite) {
      if (verify.isValid(tc)) {
        ts_new.suite.add(tc);
        continue;
      }

      int row = 0;
      for (int[] pos : model.PC_ALL) {
        int column = ALG.valtest2num(pos, tc, model.t_way, model.value);
        int cov = model.coverState(row, column);

        // if it is covered
        if (cov >= 1) {
          // if it is only covered in this invalid test case
          if (cov == 1)
            R.add(extractTuple(tc, pos));
          model.setCoverValue(row, column, cov - 1);
        } else if (cov == 0) {
          System.err.println("[Replace] the input is not a covering array!");
        }
        row += 1;
      }
    }

    //System.out.println("Size of valid test suite         = " + ts_new.suite.size());
    //System.out.println("Number of uncovered combinations = " + R.size());

    // generate new test cases to cover tuple in R
    //Collections.shuffle(R);
    if (R.size() > 0) {
      ArrayList<int[]> mergeTS = new ArrayList<>();
      mergeTS.add(R.get(0));

      // merge tuple in R into existing compatible test cases in mergeTS,
      // if it is not possible, add a new test case
      for (int i = 1; i < R.size(); i++) {
        int[] t1 = R.get(i);
        int maxMatchLine = -1;
        int maxMatch = -1;
        for (int x = 0; x < mergeTS.size(); x++) {
          int match = match(mergeTS.get(x), t1, verify);
          if (match != -1 && match > maxMatch) {
            maxMatchLine = x;
            maxMatch = match;
          }
        }
        if (maxMatchLine == -1) {
          mergeTS.add(t1);
        } else {
          mergeTest(mergeTS.get(maxMatchLine), t1);
        }
      }

      // assign unfixed parameters in mergeTS, and add them into ts_new
      ts_new.suite.addAll(mergeTS);

    }

    // the new test suite
    ts.suite.clear();
    ts.suite = ts_new.suite;
    ts.assignUnfixedValues(model, verify);
  }


  /**
   * Get the specified t-way tuple in the test case.
   *
   * @param test     a test case
   * @param position the indexes of parameters
   * @return the tuple (unfixed parameters are assigned to -1)
   */
  private int[] extractTuple(final int[] test, final int[] position) {
    int[] tc = new int[test.length];
    for (int k = 0; k < test.length; k++)
      tc[k] = -1;
    for (int e : position)
      tc[e] = test[e];
    return tc;
  }

  /**
   * Calculate the number of matched parameters between two test cases.
   * Return -1 when the two test cases are incompatible, or the combined
   * test case is invalid.
   */
  private int match(final int[] a, final int[] b, ConstraintHandler handler) {
    int[] tc = new int[a.length];
    int match = 0;
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i] && a[i] != -1 && b[i] != -1)
        return -1;
      if (a[i] != -1)
        tc[i] = a[i];
      else
        tc[i] = b[i];
      if (a[i] == b[i] && a[i] != -1)
        match++;
    }
    if (!handler.isValid(tc))
      return -1;

    return match;
  }

  /**
   * Merge test case B into test case A.
   */
  private void mergeTest(int[] a, final int[] b) {
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i] && a[i] != -1 && b[i] != -1)
        System.err.println("[Replace] merge incompatible test cases!");
      if (a[i] == -1)
        a[i] = b[i];
    }
  }

  /**
   * Determine whether a given complete or partial test case is constraints
   * satisfying. Any unfixed parameter should be assigned to value -1.
   * [Replace]: always returns true (assume an unconstrained test model).
   *
   * @param test a complete or partial test case
   * @return validity of the test case
   */
  public boolean isValid(final int[] test) {
    return true;
  }

  /**
   * Calculate the number of violations in the candidate solution. Any unfixed
   * parameter should be assigned to value -1.
   * [Replace]: returns zero to avoid to affect the fitness value
   *
   * @param test a complete or partial test case
   * @return number of constraints violations
   */
  public long penaltyTerm(final int[] test) {
    return 0;
  }

}
