package generator;

import combinatorial.CTModel;
import combinatorial.TestSuite;
import handler.ConstraintHandler;
import handler.ConstraintHandler.Handlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static handler.ConstraintHandler.Handlers.*;

/**
 * An implementation of the Simulated Annealing (SA) algorithm, which uses the Evolving-Test-Suite framework.
 */
public class SA extends CAGenerator {

  private CTModel model;
  private ConstraintHandler handler;
  private Random random;

  // record
  private int[][] cover;

  // parameter settings
  private double TEMPERATURE = 0.2;
  private int ROUND = 200000;
  private double WEIGHT = 4.0;

  public SA() {
    supportedHandlers = new Handlers[]{Verify, Solver, Tolerate, Replace};
    random = new Random();
  }

  public void settings(int round, double temperature, double weight) {
    this.ROUND = round;
    this.TEMPERATURE = temperature;
    this.WEIGHT = weight;
  }

  /**
   * The particular generation algorithm. Use handler.isValid() and handler.penaltyTerm()
   * methods to deal with constraints encountered during the generation process.
   * @param model a combinatorial test model
   * @param handler the constraint handler
   * @param ts the generated test suite
   */
  @Override
  public void process(CTModel model, ConstraintHandler handler, TestSuite ts) {
    this.model = model;
    this.handler = handler;

    // only valid combinations will be accounted
    model.removeInvalidCombinations(handler);

    // record the cover matrix (to avoid multiple re-initialisations in the inner search)
    cover = new int[model.PC_NUM][];
    model.copyCoverMatrix(cover);

    // calculate the lower[0] and upper[1] bounds
    int[] bounds = bound();
    int lower = bounds[0];
    int upper = bounds[1];

    // conduct the outer search
    List<int[]> A1 = outerSearch(lower, upper);

    if (A1.size() == 0) {
      //System.out.println("increase upper bound, new bounds = " + lower + " / " + upper);
      upper = upper * 2;
      A1 = outerSearch(lower, upper);
    }

    while (A1.size() == lower) {
      upper = lower - 1;
      lower = lower > 5 ? lower - 5 : lower / 2;
      //System.out.println("achieve lower bound, new bounds = " + lower + " / " + upper);

      List<int[]> A2 = outerSearch(lower, upper);
      if (A2.size() > 0)
        A1 = A2;
    }

    ts.suite = A1 ;
    if (ts.suite.size() == 0)
      System.out.println("* Do not find a covering array till the end of SA for " + model.name);
  }

  /**
   * The outer search of SA. A binary search like strategy is used to determine the size of arrays
   * that the SA algorithm will try to construct.
   * @param lower lower bound
   * @param upper upper bound
   * @return test suite
   */
  private List<int[]> outerSearch(int lower, int upper) {
    List<int[]> A = new ArrayList<>();
    int N = (lower + 2 * upper) / 3;
    while (upper >= lower) {
      List<int[]> A1 = innerSearch(N);
      // if a solution is found
      if (A1.size() != 0) {
        A = A1;
        upper = N - 1;
      } else {
        lower = N + 1;
      }
      N = (lower + 2 * upper) / 3;
    }
    return A;
  }

  /**
   * The inner search of SA. The algorithm will try to evolve an array of the given size to cover
   * all the required t-way combinations (namely, search for a covering array).
   * @param N the size of the target array
   * @return test suite
   */
  private List<int[]> innerSearch(int N) {
    // initialise a set of valid test suite at random
    List<int[]> A = new ArrayList<>();
    for (int i = 0; i < N; i++) {
      int[] test = sample();
      while (!handler.isValid(test))
        test = sample();
      A.add(test);
    }

    // re-initialise
    model.resetCoverMatrix(cover);

    long covered = 0 ;
    for (int[] test : A)
      covered += model.coverUpdate(test);

    long uncovered = model.SPACE_ALL - covered;   // number of uncovered combinations
    long violation = handler.penaltyTerm(A);     // number of constraints violations
    //System.out.println("try to find an array of size " + N + " | uncovered = " + uncovered + ", violations = " + violation);

    double temperature = TEMPERATURE;
    int round = 0;
    while (round < ROUND) {
      // get a random position and a random value in test suite
      int row = random.nextInt(N);
      int column = random.nextInt(model.parameter);
      int symbol = random.nextInt(model.value[column]);

      // the change
      int[] candidate = new int[model.parameter];
      System.arraycopy(A.get(row), 0, candidate, 0, model.parameter);
      candidate[column] = symbol;

      // if there is no change, or the change violates constraints, ignore this move
      if (symbol == A.get(row)[column] || !handler.isValid(candidate))
        continue;

      // ----------------------------------------------------- //
      // delta = (U' + 4 * V') - (U + 4 * V)
      // where U = number of yet-to-be covered combinations
      //       V = number of constraints violations
      // delta < 0 indicates a better solution
      // ----------------------------------------------------- //
      long fit = model.fitnessDelta(A.get(row), candidate); //  U' - U
      long violationA = handler.penaltyTerm(A.get(row));      //  V
      long violationA1 = handler.penaltyTerm(candidate);      //  V'

      // fitness(A1) - fitness(A)
      double delta = fit + WEIGHT * (violationA1 - violationA);

      if (delta <= 0 || random.nextDouble() < Math.pow(Math.E, - (delta / temperature))) {
        // apply the new move, A = A1
        A.get(row)[column] = symbol;
        uncovered = uncovered + fit;
        violation = violation + (violationA1 - violationA);
      }
      else {
        // reset the cover matrix
        model.fitnessDelta(candidate, A.get(row));
      }

      // if get a covering array
      if (uncovered == 0 && violation == 0) {
        return A;
      }

      round += 1;
      temperature = cool(temperature, round);
    }

    return new ArrayList<>();
  }

  /**
   * Determine the initial lower and upper bounds.
   * @return int[lower bound, upper bound]
   */
  private int[] bound() {
    int[] bound = new int[]{1, 5};
    // get the ascending order
    int[] val = model.value.clone();
    Arrays.sort(val);

    // lower bound
    for (int k = val.length - 1 ; k >= val.length - model.t_way ; k--)
      bound[0] = bound[0] * val[k] ;

    // upper bound
    for (int k = 0 ; k < model.t_way ; k++)
      bound[1] = bound[1] * val[val.length - 1];

    return bound;
  }

  /**
   * Get a candidate test case at random.
   */
  private int[] sample() {
    int[] tc = new int[model.parameter];
    for (int i = 0; i < model.parameter; i++)
      tc[i] = random.nextInt(model.value[i]);
    return tc;
  }

  /**
   * Cooling function: temperature * 0.999999 every 10 iteration
   */
  private double cool(double temperature, int round) {
    if (round % 10 != 0)
      return temperature;
    return temperature * 0.999999;
  }

}
