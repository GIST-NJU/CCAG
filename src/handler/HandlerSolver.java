package handler;

import combinatorial.CTModel;
import combinatorial.TestSuite;
import handler.common.CSolver;
import handler.common.SAT4J;

/**
 * An implementation of the Solver constraint handler, which uses SAT4J as
 * the constraint solver (by default).
 */
public class HandlerSolver implements ConstraintHandler {

  private CSolver c_solver ;

  public HandlerSolver() {
    c_solver = new SAT4J();
  }

  /**
   * The process executed before test suite generation.
   * [Solver]: initialise the constraint solver
   * @param model the test model of CT
   */
  public void pre(CTModel model) {
    c_solver.init(model);
  }

  /**
   * The process executed after test suite generation.
   * [Solver]: do nothing
   * @param ts the generated test suite
   */
  public void post(CTModel model, TestSuite ts) {}

  /**
   * Determine whether a given complete or partial test case is constraints
   * satisfying. Any unfixed parameters should be assigned to value -1.
   * [Solver]: solve the constraint satisfaction problem
   * @param test a complete or partial test case
   * @return validity of the test case
   */
  public boolean isValid(final int[] test) {
    return c_solver.satisfied(test);
  }

  /**
   * Calculate the number of violations in the candidate solution. Any unfixed
   * parameter should be assigned to value -1.
   * [Solver]: returns zero to avoid to affect the fitness value
   * @param test a complete or partial test case
   * @return number of constraints violations
   */
  public long penaltyTerm(final int[] test) {
    return 0;
  }

}
