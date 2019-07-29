package handler.common;

import combinatorial.CTModel;

/**
 * The interface of constraint satisfaction solver.
 */
public interface CSolver {

  /**
   * Initialize the sat4j solver.
   * @param model a test model of CT
   */
  void init(CTModel model);

  /**
   * Determine whether a given complete or partial test case is constraints
   * satisfying. Any unfixed parameters are assigned to value -1.
   * @param test a complete or partial test case
   */
  boolean satisfied(final int[] test);

}
