package generator;

import combinatorial.CTModel;
import combinatorial.TestSuite;
import combinatorial.Tuple;
import handler.ConstraintHandler;
import handler.ConstraintHandler.Handlers;
import combinatorial.ALG;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static handler.ConstraintHandler.Handlers.Replace;
import static handler.ConstraintHandler.Handlers.Solver;
import static handler.ConstraintHandler.Handlers.Verify;

/**
 * An implementation of the IPO algorithm, which uses the In-Parameter-Order framework.
 */
public class IPO extends CAGenerator {

  /*
   * The best assignment for a parameter value.
   */
  private class Choice {
    int valueBest;
    int coverBest;
    List<Tuple> coveredTuple;

    Choice(int value, int cover, List<Tuple> tuple) {
      valueBest = value;
      coverBest = cover;
      coveredTuple = tuple;
    }
  }

  private CTModel model;
  private ConstraintHandler handler ;

  public IPO() {
    supportedHandlers = new Handlers[]{Verify, Solver, Replace};
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

    // sort parameters in a non-increasing order of their values
    List<Integer> order = sortParameter();
    //System.out.println("order = " + order);

    // the ordered list of fixed parameters
    List<Integer> fixed = new ArrayList<>();
    for (int k = 0 ; k < model.t_way ; k++)
      fixed.add(order.get(k));
    Collections.sort(fixed);

    // whether there have constrained parameters in the fixed set
    long constrained = fixed.stream().filter(x -> model.constrainedParameters.contains(x)).count();

    // the test cases to cover all valid t-way combinations of the first t parameters
    int[] pos = fixed.stream().mapToInt(x -> x).toArray();
    for(int[] sch : ALG.allV(pos, model.t_way, model.value)) {
      Tuple tuple = new Tuple(pos, sch, model.parameter);
      if (constrained == 0 || handler.isValid(tuple.test)) {
        ts.suite.add(tuple.test);
        model.coverUpdate(tuple.position, tuple.schema);
      } else {
        model.setInvalid(tuple.position, tuple.schema);
      }
    }

    // for each of the remaining parameters
    int index = model.t_way ;
    while (index < model.parameter) {
      // now, consider the pid-th parameter
      int pid = order.get(index);

      // ----- horizontal extension ----- //
      for (int[] each : ts.suite) {
        // assign the best value and update combination coverage
        Choice candidate = selectBestValue(each, pid);
        if (candidate.coverBest > 0) {
          each[pid] = candidate.valueBest;
          candidate.coveredTuple.forEach(x -> model.coverUpdate(x.position, x.schema));
        }
      }

      fixed.add(pid);
      if (model.constrainedParameters.contains(pid))
        constrained += 1 ;

      // calculate the set of uncovered combinations involving the pid-th parameter
      List<Tuple> uncovered_tuple = new ArrayList<>();
      for (int[] e : ALG.allCombination(index, model.t_way - 1)) {
        pos = new int[model.t_way];
        int x , y;
        for (x = 0 , y = 0 ; y < model.t_way - 1 && fixed.get(e[y]) < pid ; x++, y++)
          pos[x] = fixed.get(e[y]);
        pos[x] = pid;
        if (y < model.t_way - 1) {
          for (x = x + 1; y < model.t_way - 1; x++, y++)
            pos[x] = fixed.get(e[y]);
        }

        for (Tuple tp : model.getUncoveredTuple(pos)) {
          if (constrained == 0 || handler.isValid(tp.test))
            uncovered_tuple.add(tp);
          else
            model.setInvalid(tp.position, tp.schema);
        }
      }

      // now, a new parameter has been fixed
      Collections.sort(fixed);
      index += 1;
      //constrained = fixed.stream().filter(x -> model.constrainedParameters.contains(x)).count();

      //System.out.println("remain " + uncovered_tuple.size() + " tuple after horizontally extending parameter " + pid);
      //for(Tuple tp : uncovered_tuple)
      //  System.out.println(tp);

      // ----- vertical extension ----- //
      while (uncovered_tuple.size() > 0) {
        Tuple tuple = uncovered_tuple.remove(0);

        // whether the tuple can be merged into existing test cases
        boolean merged = false;
        for( int[] test : ts.suite ) {
          if (merge(test, tuple.test)) {
            merged = true;
            break;
          }
        }
        // if the tuple cannot be merged, add a new test case to cover it
        if (!merged)
          ts.suite.add(tuple.test);
      }
    }  // end for extending each parameter

    // assign unfixed parameters to their first valid choice
    ts.assignUnfixedValues(model, handler);

  }

  /**
   * Reorder parameters in a non-increasing order of their values.
   */
  private List<Integer> sortParameter() {
    int[] val = new int[model.parameter];
    System.arraycopy(model.value, 0, val, 0, model.parameter);

    int[] od = new int[model.parameter];
    for (int i = 0 ; i < model.parameter ; i++)
      od[i] = i ;

    ALG.sortArray(val, od, false);
    return IntStream.of(od).boxed().collect(Collectors.toList());
  }

  /**
   * Determine the best value assignment of parameter pid for the candidate test case.
   * @param test the current test case (-1 for unfixed parameters)
   * @param pid the index of target parameter
   * @return a Choice object indicating the best value, and the set of newly covered tuples
   */
  private Choice selectBestValue(final int[] test, int pid) {
    int valueBest = -1;
    int coverBest = -1;
    List<Tuple> coveredTuple = null;

    // note that even a parameter is extended, it might remain unfixed in some test cases
    int assigned = 0;
    int[] new_test = new int[model.parameter];
    for (int i = 0; i < model.parameter; i++) {
      new_test[i] = test[i];
      if (test[i] != -1)
        assigned++;
    }

    // for each possible value
    boolean constrained = model.constrainedParameters.contains(pid);
    for (int eid = 0; eid < model.value[pid]; eid++) {
      // assign this value
      new_test[pid] = eid ;

      // if the assignment is invalid, skip this one
      // otherwise, all t-way combinations accounted in the followings are valid
      if (constrained && !handler.isValid(new_test))
        continue;

      // calculate the number of combinations that can be covered: only consider the
      // combination between X and the assigned values:
      // 1 1 1 0   X        - - - - -
      // --------  -        ---------
      // assigned  pid-eid  unassigned

      // determine the indexes of fixed parameters
      int[] fixed_pos = new int[assigned];
      for (int i = 0, j = 0; i < model.parameter ; i++ ) {
        if (new_test[i] != -1 && i != pid)
          fixed_pos[j++] = i;
      }

      // for each possible (t-1)-way parameter combinations among fixed_pos[]
      int[] tp = new int[model.parameter];
      int fit = 0;
      List<Tuple> tuples = new ArrayList<>();
      for (int[] comb : ALG.allCombination(assigned, model.t_way - 1)) {
        // construct a candidate t-way tuple
        for (int k = 0 ; k < tp.length ; k++)
          tp[k] = -1 ;
        for (int k : comb)
          tp[fixed_pos[k]] = new_test[fixed_pos[k]];
        tp[pid] = eid;
        //System.out.println("comb = " + Arrays.toString(comb) + ", get tp = " + Arrays.toString(tp));

        // determine whether this t-way tuple is covered or not
        Tuple c = new Tuple(tp);
        if (model.coverState(c.position, c.schema) == 0) {
          fit++;
          tuples.add(c);
        }
      }

      // select the best one (if there are more, select the first one)
      if (fit > coverBest) {
        coverBest = fit;
        valueBest = eid;
        coveredTuple = tuples;
      }
      //System.out.println("pid = " + pid + ", eid = " + eid + ", cover = " + fit);
    }

    return new Choice(valueBest, coverBest, coveredTuple);
  }


  /**
   * Determine whether it is possible, and valid, to merge the test2 into the test1.
   * If it is possible, then test1 will be updated.
   * @param test1 the first test case (candidate test case)
   * @param test2 the second test case (candidate t-way tuple)
   */
  private boolean merge(int[] test1, final int[] test2) {
    int[] temp = new int[test1.length];
    for (int i = 0; i < test1.length; i++) {
      if (test1[i] != test2[i] && test1[i] != -1 && test2[i] != -1)
        return false;

      if (test1[i] != -1)
        temp[i] = test1[i];
      else
        temp[i] = test2[i];
    }

    if (!handler.isValid(temp))
      return false ;

    System.arraycopy(temp, 0, test1, 0, test1.length);
    return true;
  }


}
