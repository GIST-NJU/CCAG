package generator;

import combinatorial.CTModel;
import combinatorial.TestSuite;
import combinatorial.Tuple;
import handler.ConstraintHandler;
import handler.ConstraintHandler.Handlers;
import combinatorial.ALG;

import java.util.*;
import java.util.stream.Collectors;

import static handler.ConstraintHandler.Handlers.Replace;
import static handler.ConstraintHandler.Handlers.Solver;
import static handler.ConstraintHandler.Handlers.Verify;

/**
 * An implementation of the AETG algorithm, which uses the One-test-at-a-time framework.
 */
public class AETG extends CAGenerator {

  private class Pair implements Comparable<Pair> {
    int index;
    int number;

    private Pair(int i, int n) {
      index = i;
      number = n;
    }

    /*
     *  compareTo should return < 0 if this is supposed to be
     *  less than other, > 0 if this is supposed to be greater than
     *  other and 0 if they are supposed to be equal
     *
     *  find a descending order via collection.sort (collection.sort is
     *  ascending), and sorting is only based on the value of number
     */
    @Override
    public int compareTo(Pair B) {
      return -Integer.compare(this.number, B.number);
    }

    @Override
    public String toString() {
      return String.valueOf(index) + " (" + String.valueOf(number) + ")";
    }
  }

  private int CANDIDATE = 50;
  private int EACH_COVER_MAX ;
  private Random random;
  private CTModel model ;
  private ConstraintHandler handler ;

  public AETG() {
    supportedHandlers = new Handlers[]{Verify, Solver, Replace};
    random = new Random();
    model = null;
    handler = null;
  }

  /**
   * Set the number of candidates when generating each test case.
   * @param candidate number of candidates
   */
  public void setCANDIDATE(int candidate) {
    this.CANDIDATE = candidate;
  }

  /**
   * The particular generation algorithm. Use handler.isValid() and handler.penaltyTerm()
   * methods to deal with constraints encountered during the generation process.
   *
   * This implementation uses a lazy detection strategy.
   * @param model a combinatorial test model
   * @param handler the constraint handler
   * @param ts the generated test suite
   */
  @Override
  public void process(CTModel model, ConstraintHandler handler, TestSuite ts) {
    this.model = model;
    this.handler = handler;

    EACH_COVER_MAX = model.PC_ALL.size();
    ts.suite.clear();

    while (model.UNCOVERED() != 0) {
      int[] next = nextBestTestCase(CANDIDATE);
      // if there is no more combination that needs to cover
      if (next == null)
        break;
      ts.suite.add(next);
      model.coverUpdate(next);
    }
  }

  /**
   * Generate the next test case that covers the most uncovered combinations.
   * @param N number of candidates
   */
  private int[] nextBestTestCase(int N) {
    int[] best = nextTestCase();
    if (best == null || N == 1)
      return best;

    long covBest = model.coverNumber(best);
    for (int x = 1; x < N; x++) {
      int[] temp = nextTestCase();
      long covTemp = model.coverNumber(temp);

      if (covTemp == EACH_COVER_MAX) {
        return best;
      } else if (covTemp > covBest) {
        best = temp;
        covBest = covTemp;
      }
    }
    return best;
  }

  /**
   * Generate a new candidate test case.
   */
  private int[] nextTestCase() {
    // --------------------------------------------------------- //
    // Get a random uncovered t-way combination; if there does not
    // have such one, then all combinations have been covered
    //
    // Lazy Detection: if this t-way combination is invalid,
    // remove it from the set of combinations to be covered.
    // ---------------------------------------------------------- //
    Tuple tp = model.getRandomUncoveredTuple();
    while (tp != null && !handler.isValid(tp.test)) {
      // set this tuple as invalid (no need to cover) and find another one
      model.setInvalid(tp.position, tp.schema);
      tp = model.getRandomUncoveredTuple();
    }
    if (tp == null)
      return null;

    // assign the new combination in tc[]
    int[] tc = tp.test;

    // randomize a permutation of other parameters
    List<Integer> permutation = new ArrayList<>();
    for (int k = 0; k < model.parameter; k++) {
      if (tc[k] == -1)
        permutation.add(k);
    }
    Collections.shuffle(permutation);

    // for each of the remaining parameters
    int assigned = model.parameter - permutation.size();
    for (int par : permutation) {
      tc[par] = selectBestValue(tc, assigned, par);
      assigned++;
    }

    return tc;
  }

  /**
   * Given a partial test case and a unfixed parameter, determine the value assignment
   * that is the best in terms of coverage and at the same time constraint satisfied.
   * @param test a partial test case
   * @param par the index of a free parameter
   */
  private int selectBestValue(final int[] test, int assigned, int par) {
    // iterate all possible values
    ArrayList<Pair> vs = new ArrayList<>();
    for (int i = 0; i < model.value[par]; i++) {
      int num = coveredSchemaNumberFast(test, assigned, par, i);
      if (num != -1)
        vs.add(new Pair(i, num));
    }
    Collections.sort(vs);

    // apply tie-breaking
    int max = vs.get(0).number;
    List<Pair> filtered = vs.stream()
        .filter(p -> p.number == max)
        .collect(Collectors.toList());

    int r = random.nextInt(filtered.size());
    return filtered.get(r).index;
  }

  /**
   * Given a parameter and its corresponding value, return the number of uncovered
   * combinations that can be covered by assigning this parameter value (fast version).
   * @param test current test case before assigning
   * @param par index of parameter to be assigned
   * @param val value to be assigned to the parameter
   * @return number of uncovered combinations (-1 if the assignment is invalid)
   */
  private int coveredSchemaNumberFast(final int[] test, int assigned, int par, int val) {

    // Only consider the combination between X and the assigned values:
    // iterate all (t-1)-way value combinations among all assigned values
    // to compute the number of uncovered combinations that can be covered
    // by assigning X.
    //
    // 1 1 1 0   X        - - - - -
    // --------  -        ---------
    // assigned  par-val  unassigned

    int fit = 0;
    int[] new_test = new int[model.parameter];
    System.arraycopy(test, 0, new_test, 0, model.parameter);
    new_test[par] = val;

    if (!handler.isValid(new_test))
      return -1 ;

    // number of required parameters to form a t-way combination
    int required = model.t_way - 1;

    /*
    // get fixed part, not including newly assigned one
    List<Pair> fixed = new ArrayList<>();
    for (int i = 0; i < model.parameter; i++) {
      if (new_test[i] != -1 && i != par)
        fixed.add(new Pair(new_test[i], i));
    }

    // for each possible r-way parameter combinations among fp[]
    for (int[] each : ALG.allCombination(assigned, required)) {
      // construct a temp t-way combination
      List<Pair> tempTuple = new ArrayList<>();
      for (int k = 0; k < required; k++)
        tempTuple.add(fixed.get(each[k]));
      tempTuple.add(new Pair(val, par));
      Collections.sort(tempTuple);

      int[] sch = new int[model.t_way];
      int[] pos = new int[model.t_way];
      for (int k = 0 ; k < model.t_way ; k++) {
        sch[k] = tempTuple.get(model.t_way - k - 1).index;
        pos[k] = tempTuple.get(model.t_way - k - 1).number;
      }

      // determine whether this t-way combination is covered or not
      if (model.cover(pos, sch, 0) == 0)
        fit++;
    }*/

    int[] fp = new int[assigned];
    int[] fv = new int[assigned];
    for (int i = 0, j = 0; i < model.parameter; i++) {
      if (new_test[i] != -1 && i != par) {
        fp[j] = i;
        fv[j++] = new_test[i];
      }
    }

    // newly assigned one
    int[] pp = {par};
    int[] vv = {val};

    // for each possible r-way parameter combinations among fp[]
    for (int[] each : ALG.allCombination(assigned, required)) {
      // construct a temp t-way combination
      int[] position = new int[model.t_way];
      int[] schema = new int[model.t_way];
      int x , y;
      for (x = 0 , y = 0 ; y < required && fp[each[y]] < par ; x++, y++) {
        position[x] = fp[each[y]];
        schema[x] = fv[each[y]];
      }
      position[x] = par;
      schema[x] = val;
      if (y < required) {
        for (x = x + 1 ; y < required ; x++, y++) {
          position[x] = fp[each[y]];
          schema[x] = fv[each[y]];
        }
      }

      /*
      int[] pos = new int[required];
      int[] sch = new int[required];
      for (int k = 0; k < required; k++) {
        pos[k] = fp[each[k]];
        sch[k] = fv[each[k]];
      }

      // construct a temp t-way combination
      int[] position = new int[model.t_way];
      int[] schema = new int[model.t_way];
      mergeArray(pos, sch, pp, vv, position, schema);
      */

      // determine whether this t-way combination is covered or not
      if (model.coverState(position, schema) == 0)
        fit++;
    }
    return fit;
  }

  /*
   * Merge two sorted arrays into a new sorted array. The ordering is
   * conducted on primary arrays (parameter array), while values in
   * additional arrays (value array) will be exchanged at the same time.
   */
  private static void mergeArray(int[] p1, int[] v1, int[] p2, int[] v2, int[] pos, int[] sch) {
    int i, j, k;
    for (i = 0, j = 0, k = 0; i < p1.length && j < p2.length; ) {
      if (p1[i] < p2[j]) {
        pos[k] = p1[i];
        sch[k++] = v1[i++];
      } else {
        pos[k] = p2[j];
        sch[k++] = v2[j++];
      }
    }
    if (i < p1.length) {
      for (; i < p1.length; i++, k++) {
        pos[k] = p1[i];
        sch[k] = v1[i];
      }
    }
    if (j < p2.length) {
      for (; j < p2.length; j++, k++) {
        pos[k] = p2[j];
        sch[k] = v2[j];
      }
    }
  }

}
