package handler.common;

import combinatorial.ALG;
import combinatorial.CTModel;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.TimeoutException;
import org.sat4j.core.VecInt;
import org.sat4j.minisat.SolverFactory;
import org.sat4j.specs.IProblem;
import org.sat4j.specs.ISolver;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Vector;


/**
 * Use the sat4j solver (http://www.sat4j.org) to determine the validity.
 * Current version uses a basic SAT (boolean) encoding to model constraints.
 */
public class SAT4J implements CSolver {

  /*
   *  To use the sat4j solver, each parameter value must be mapped
   *  into an integer value (a boolean variable), which starts from 1.
   *
   *  For example, the mapping for CA(N;t,5,3) is as follows:
   *  p1  p2  p3  p4  p5
   *   1   4   7  10  13
   *   2   5   8  11  14
   *   3   6   9  12  15
   *
   *  A constraint is represented as a disjunction of literals.
   *  For example, the followings give the representations of
   *  two forbidden combinations.
   *  {0,  -1, 0, -1, -1} as [-1, -7]
   *  {-1, -1, 2,  0,  1} as [-9, -10, -14]
   */

  private int[][] relation;
  private Vector<int[]> basicConstraint;  // at-least & at-most constraints
  private Vector<int[]> hardConstraint;   // user specified constraints

  private ISolver solver;  // the SAT solver

  public SAT4J() {
    basicConstraint = new Vector<>();
    hardConstraint = new Vector<>();
  }

  /**
   * Initialize the sat4j solver.
   * @param model a test model of CT
   */
  public void init(CTModel model) {
    basicConstraint = new Vector<>();
    hardConstraint = new Vector<>();
    relation = model.relation;

    // set at-least constraint
    for (int i = 0; i < model.parameter; i++) {
      basicConstraint.add(copy(relation[i]));
    }

    // set at-most constraint
    for (int i = 0; i < model.parameter; i++) {
      for (int[] row : ALG.allCombination(model.value[i], 2)) {
        int[] tp = {0 - relation[i][row[0]], 0 - relation[i][row[1]]};
        basicConstraint.add(copy(tp));
      }
    }

    // set hard constraints
    if(model.constraint!=null)
      for (int[] x : model.constraint) {
        hardConstraint.add(copy(x));
      }

    // initialize solver
    int MAXVAR = relation[model.parameter-1][model.value[model.parameter-1]-1];
    int NBCLAUSES = basicConstraint.size() + hardConstraint.size();
    solver = SolverFactory.newDefault();
    solver.newVar(MAXVAR);
    solver.setExpectedNumberOfClauses(NBCLAUSES);

    try {
      for (int[] clause : basicConstraint)
        solver.addClause(new VecInt(clause));
      for (int[] clause : hardConstraint)
        solver.addClause(new VecInt(clause));
    } catch (ContradictionException e) {
      System.err.println("Solver Contradiction Error: " + e.getMessage());
    }
  }

  private int[] copy(final int[] clause) {
    int[] disjunction = new int[clause.length];
    System.arraycopy(clause, 0, disjunction, 0, clause.length);
    return disjunction;
  }

  /**
   * Determine whether a given complete or partial test case is constraints
   * satisfying. Any unfixed parameters are assigned to value -1.
   * @param test a complete or partial test case
   */
  public boolean satisfied(final int[] test) {
    if (hardConstraint.size() == 0)
      return true;

    // transfer test to clause representation
    ArrayList<Integer> list = new ArrayList<>();
    for (int i = 0; i < test.length; i++) {
      if (test[i] != -1)
        list.add(relation[i][test[i]]);
    }
    int[] clause = list.stream().mapToInt(i -> i).toArray();

    // determine validity
    boolean satisfiable = false;
    try {
      VecInt c = new VecInt(clause);
      IProblem problem = solver;
      satisfiable = problem.isSatisfiable(c);
    } catch (TimeoutException e) {
      System.err.println("Solver Timeout Error: " + e.getMessage());
    }
    return satisfiable;
  }

}
