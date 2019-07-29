package handler;

import combinatorial.ALG;
import combinatorial.CTModel;
import combinatorial.TestSuite;
import combinatorial.Tuple;
import handler.common.MFT;

/**
 * An implementation of the Tolerate constraint handler (MFT based).
 */
public class HandlerTolerate implements ConstraintHandler {

  private MFT mft ;

  public HandlerTolerate() {
    mft = new MFT();
  }

  /**
   * The process executed before test suite generation.
   * [Tolerate]: calculate the set of MFT, and mark all invalid combinations
   * @param model the test model of CT
   */
  public void pre(CTModel model) {
    mft.calculateMFT(model);

    // Tolerate needs to mark all invalid combinations in the pre-process,
    // because it is need to tell whether a candidate test suite covers all
    // valid combinations (note that the isValid() method will always returns
    // true in Tolerate).
    for (int[] pos : model.PC_ALL) {
      for (int[] sch : ALG.allV(pos, model.t_way, model.value)) {
        Tuple tuple = new Tuple(pos, sch, model.parameter);
        if (!mft.satisfied(tuple.test))
          model.setInvalid(pos, sch);
      }
    }
  }

  /**
   * The process executed after test suite generation.
   * [Tolerate]: do nothing
   * @param model the test model of CT
   * @param ts the generated test suite
   */
  public void post(CTModel model, TestSuite ts) {}

  /**
   * Determine whether a given complete or partial test case is constraints
   * satisfying. Any unfixed parameter should be assigned to value -1.
   * [Tolerate]: always returns true (allow invalid intermediate solutions, but the final
   *             solution should be verified by penaltyTerm() method).
   * @param test a complete or partial test case
   * @return validity of the test case
   */
  public boolean isValid(final int[] test) {
    return true;
  }

  /**
   * Calculate the number of violations in the candidate solution. Any unfixed
   * parameter should be assigned to value -1.
   * [Tolerate]: calculate the number of forbidden tuples covered by the test
   * @param test a complete or partial test case
   * @return number of constraints violations
   */
  public long penaltyTerm(final int[] test) {
    return mft.violations(test);
  }

}
