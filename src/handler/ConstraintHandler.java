package handler;

import combinatorial.CTModel;
import combinatorial.TestSuite;

import java.util.List;


/**
 * The interface of constraint handler in the CCAG framework.
 */
public interface ConstraintHandler {

  enum Handlers {
    Verify, Solver, Replace, Tolerate
  }

  /**
   * The process executed before test suite generation.
   * @param model the test model of CT
   */
  void pre(CTModel model);

  /**
   * The process executed after test suite generation.
   * @param model the test model of CT
   * @param ts the generated test suite
   */
  void post(CTModel model, TestSuite ts);

  /**
   * Determine whether a given complete or partial test case is constraints
   * satisfying. Any unfixed parameter should be assigned to value -1.
   * @param test a complete or partial test case
   * @return validity of the test case
   */
  boolean isValid(final int[] test);

  /**
   * Calculate the number of violations in the candidate solution. Any unfixed
   * parameter should be assigned to value -1.
   * @param test a complete or partial test case
   * @return number of constraints violations
   */
  long penaltyTerm(final int[] test);

  /**
   * Determine whether a given test suite is constraints satisfying.
   * @param suite a test suite
   * @return validity of the test suite
   */
  default boolean isValid(final List<int[]> suite) {
    for (int[] each : suite) {
      if (!isValid(each))
        return false;
    }
    return true;
  }

  /**
   * Calculate the number of violations in the given test suite.
   * @param suite a test suite
   * @return number of constraints violations
   */
  default long penaltyTerm(final List<int[]> suite) {
    int sum = 0 ;
    for (int[] each : suite)
      sum += penaltyTerm(each);
    return sum;
  }

}
