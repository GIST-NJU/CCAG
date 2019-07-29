package generator;

import combinatorial.ALG;
import combinatorial.CTModel;
import combinatorial.TestSuite;
import combinatorial.Tuple;
import handler.ConstraintHandler;
import handler.ConstraintHandler.Handlers;
import java.util.*;
import java.util.stream.Collectors;

import static handler.ConstraintHandler.Handlers.*;

/**
 * An implementation of the DDA algorithm, which uses the One-test-at-a-time framework.
 */
public class DDA extends CAGenerator {

  private class Pair implements Comparable<Pair> {
    int index;
    double number;

    private Pair(int i, double n) {
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
      return -Double.compare(this.number, B.number);
    }

    @Override
    public String toString() {
      return String.valueOf(index) + " (" + String.valueOf(number) + ")";
    }
  }

  private int CANDIDATE = 5;
  private int EACH_COVER_MAX ;
  private Random random;
  private CTModel model ;
  private ConstraintHandler handler ;

  public DDA() {
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

    // for each of the remaining parameters
    int assigned = model.parameter - permutation.size();  // # fixed parameters
    int unassigned =permutation.size();                   // # unfixed parameters

    // reordering based on density
    permutation = computerDensity(permutation, tc, assigned, unassigned);
    for (int par : permutation) {
      tc[par] = selectBestValue2(tc, assigned,unassigned,par);
      assigned++;
      unassigned--;
    }
    return tc;
  }

  /**
   * Get the order of parameters based on their densities.
   */
  private List<Integer> computerDensity(List<Integer> permutation, int[] tc, int assigned, int unassigned) {
    List<Integer> newPer = new ArrayList<>(); // default order
    if (permutation.size() > 1) {
      int[] k = new int[tc.length]; // record the maximum density
      ArrayList<Pair> vs = new ArrayList<>();
      for (int par : permutation) {
        double num = 0;
        double Max = 0;
        k[par] = 0;
        for (int i = 0; i < model.value[par]; i++) {
          // compute density
          double temp = computerDensity(tc, assigned, unassigned, par, i);
          if (temp > Max)
            k[par] = i;
          if (temp != -1.0)
            num += temp;
        }
        vs.add(new Pair(par, num / model.value[par]));
      }

      Collections.sort(vs);
      while (vs.size() > 0) {
        double max = vs.get(0).number;
        List<Pair> filtered = vs.stream().filter(p -> p.number == max).collect(Collectors.toList());
        int r = random.nextInt(filtered.size());
        newPer.add(vs.get(r).index);
        vs.remove(r);
      }
    } else {
      return permutation;
    }
    return newPer;
  }

  /**
   * Given a partial test case and a unfixed parameter, determine the value assignment
   * that is the best in terms of coverage and at the same time constraint satisfied.
   * @param test a partial test case
   * @param par the index of a free parameter
   */
  private int selectBestValue2(final int[] test, int assigned,int unassigned, int par) {
    // iterate all possible values
    ArrayList<Pair> vs = new ArrayList<>();
    for (int i = 0; i < model.value[par]; i++) {
      double num = computerDensity(test, assigned,unassigned, par, i);//计算密度
      if (num != -1.0)
        vs.add(new Pair(i, num));
    }
    Collections.sort(vs);

    // apply tie-breaking
    double max = vs.get(0).number;
    List<Pair> filtered = vs.stream()
      .filter(p -> p.number == max)
      .collect(Collectors.toList());

    int r = random.nextInt(filtered.size());
    return filtered.get(r).index;
  }

  /**
   * Calculate the density of the given parameter value
   * @param test a candidate test case
   * @param assigned number of fixed parameters
   * @param unassigned number of unfixed parameters
   * @param par the parameter to be accounted
   * @param val the value to be accounted
   */
  private double computerDensity(int[] test, int assigned, int unassigned, int par, int val) {

    // whether it is constraints satisfying
    int[] new_test = new int[model.parameter];
    System.arraycopy(test, 0, new_test, 0, model.parameter);
    new_test[par] = val;
    if (!handler.isValid(new_test))
      return -1;

    // 1-restrict, need to select t - 1 parameters
    int required = model.t_way - 1;
    // fixed parameter values
    int[] fp = new int[assigned];
    int[] fv = new int[assigned];
    // unfixed parameter values
    int[] freep = new int[unassigned];
    int[] freev = new int[unassigned];
    for (int i = 0, j = 0; i < model.parameter; i++) {
      if (new_test[i] != -1 && i != par) {
        fp[j] = i;
        fv[j++] = new_test[i];
      }
    }

    // if there is unfixed positions
    // unassigned == 1 indicates that every other positions have been fixed (only case 1)
    if (unassigned > 1) {
      freep = new int[unassigned - 1];
      freev = new int[unassigned - 1];
      for (int i = 0, j = 0; i < model.parameter; i++) {
        if (new_test[i] == -1 && i != par) {
          freep[j] = i;
          freev[j++] = new_test[i];
        }
      }
    }

    // the density
    double denSum = 0;

    // case 1: t-1 fixed positions + par
    // case 2: t-2 fixed positions + 1 unfixed position + par
    // ------------------------------------------------------------------------------
    // for example, new_test = [-1, 0, 1, -1, -1, 0], t-way = 3, par = 0, val = 1
    // then we have fixed pos = [1, 2, 4]; sch = [0, 1, 0]
    // for case 1, select two parameters from pos
    // for case 2, select one parameter from pos, another one from unfixed positions
    // ------------------------------------------------------------------------------

    // case 1
    for (int[] each : ALG.allCombination(assigned, required)) {
      int[] position = new int[model.t_way];
      int[] schema = new int[model.t_way];
      int x, y;
      for (x = 0, y = 0; y < required && fp[each[y]] < par; x++, y++) {
        position[x] = fp[each[y]];
        schema[x] = fv[each[y]];
      }
      position[x] = par;
      schema[x] = val;
      if (y < required) {
        for (x = x + 1; y < required; x++, y++) {
          position[x] = fp[each[y]];
          schema[x] = fv[each[y]];
        }
      }
      // determine whether this t-way combination is covered or not
      if (model.coverState(position, schema) == 0)
        denSum += 1;
    }

    // case 2
    int flag; // the unfixed position
    if (unassigned > 1) {
      for (int[] each : ALG.allCombination(assigned, required - 1)) {
        for (int k = 0; k < freep.length; k++) {
          int[] position = new int[model.t_way];
          int[] schema = new int[model.t_way];
          int x, y;
          // ensure the order of positions
          if (freep[k] <= par) {
            for (x = 0, y = 0; y < required - 1 && fp[each[y]] < freep[k]; x++, y++) {
              position[x] = fp[each[y]];
              schema[x] = fv[each[y]];
            }
            position[x] = freep[k];
            schema[x] = freev[k];
            flag = x;
            for (x = x + 1; y < required - 1 && fp[each[y]] < par; x++, y++) {
              position[x] = fp[each[y]];
              schema[x] = fv[each[y]];
            }
            position[x] = par;
            schema[x] = val;
            if (y < required - 1) {
              for (x = x + 1; y < required - 1; x++, y++) {
                position[x] = fp[each[y]];
                schema[x] = fv[each[y]];
              }
            }
          } else {
            for (x = 0, y = 0; y < required - 1 && fp[each[y]] < par; x++, y++) {
              position[x] = fp[each[y]];
              schema[x] = fv[each[y]];
            }
            position[x] = par;
            schema[x] = val;
            for (x = x + 1; y < required - 1 && fp[each[y]] < freep[k]; x++, y++) {
              position[x] = fp[each[y]];
              schema[x] = fv[each[y]];
            }
            position[x] = freep[k];
            schema[x] = freev[k];
            flag = x;
            if (y < required - 1) {
              for (x = x + 1; y < required - 1; x++, y++) {
                position[x] = fp[each[y]];
                schema[x] = fv[each[y]];
              }
            }
          }
          // compute density
          int xy = model.value[freep[k]];
          for (int a = 0; a < model.value[freep[k]]; a++) {
            schema[flag] = a;
            // if covered, or invalid
            if (model.coverState(position, schema) != 0)
              xy--;
          }
          denSum += (double) xy / (double) model.value[freep[k]];
        }
      }
    } // end if unassigned > 1

    return denSum;
  }
}
