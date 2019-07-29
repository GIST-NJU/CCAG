package handler;

import combinatorial.CTModel;
import combinatorial.TestSuite;
import handler.common.MFT;

import java.util.HashMap;

/**
 * An implementation of the Verify constraint handler (MFT based).
 */
public class HandlerVerify implements ConstraintHandler {

  private MFT mft ;

  public HandlerVerify() {
    mft = new MFT();
  }

  /**
   * The process executed before test suite generation.
   * [Verify]: calculate the set of MFT
   * @param model the test model of CT
   */
  public void pre(CTModel model) {
    mft.calculateMFT(model);
  }

  /**
   * The process executed after test suite generation.
   * [Verify]: do nothing
   * @param model the test model of CT
   * @param ts the generated test suite
   */
  public void post(CTModel model, TestSuite ts) {}

  /**
   * Determine whether a given complete or partial test case is constraints
   * satisfying. Any unfixed parameter should be assigned to value -1.
   * [Verify]: check against the set of minimum forbidden tuple
   * @param test a complete or partial test case
   * @return validity of the test case
   */
  public boolean isValid(final int[] test) {
    return mft.satisfied(test);
  }

  /**
   * Calculate the number of violations in the candidate solution. Any unfixed
   * parameter should be assigned to value -1.
   * [Verify]: returns zero to avoid to affect the fitness value
   * @param test a complete or partial test case
   * @return number of constraints violations
   */
  public long penaltyTerm(final int[] test) {
    return 0;
  }

}
