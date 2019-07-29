package generator;

import static handler.ConstraintHandler.Handlers.Replace;
import static handler.ConstraintHandler.Handlers.Solver;
import static handler.ConstraintHandler.Handlers.Tolerate;
import static handler.ConstraintHandler.Handlers.Verify;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import combinatorial.CTModel;
import combinatorial.TestSuite;
import combinatorial.Tuple;
import handler.ConstraintHandler;
import handler.ConstraintHandler.Handlers;

/**
 * An implementation of the Tabu Search (TS) algorithm, which uses the Evolving-Test-Suite framework.
 */
public class TS extends CAGenerator {

  private CTModel model;
  private ConstraintHandler handler;
  private Random random;

  // the tabu list
  private TabuList tabuList;

  // record
  private int[][] cover;

  // parameter settings
  private int LENGTH = 1000;     // the length of tabu list
  private int ROUND = 80000;
  private double WEIGHT = 4.0;

  public TS() {
    supportedHandlers = new Handlers[]{Verify, Solver, Tolerate, Replace};
    random = new Random();
  }

  public void settings(int round, int length, double weight) {
    this.ROUND = round;
    this.LENGTH = length;
    this.WEIGHT = weight;
  }

  /**
   * The particular generation algorithm. Use handler.isValid() and handler.penaltyTerm()
   * methods to deal with constraints encountered during the generation process.
   *
   * @param model   a combinatorial test model
   * @param handler the constraint handler
   * @param ts      the generated test suite
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
      upper = upper * 2;
      A1 = outerSearch(lower, upper);
    }

    ts.suite = A1;
    if (ts.suite.size() == 0)
      System.out.println("* Do not find a covering array till the end of TS for " + model.name);
  }

  /**
   * The outer search of TS. A binary search like strategy is used to determine the size of arrays
   * that the TS algorithm will try to construct.
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
   * The inner search of TS. The algorithm will try to evolve an array of the given size to cover
   * all the required t-way combinations (namely, search for a covering array).
   * @param N the size of the target array
   * @return test suite
   */
  private List<int[]> innerSearch(int N) {
    // re-initialise
    model.resetCoverMatrix(cover);

    // create the tabu list
    int[] val = model.value.clone();
    Arrays.sort(val);
    LENGTH = N * model.parameter * val[val.length - 1] / 10 < 5000 ? N * model.parameter * val[val.length - 1] / 10 : 5000;
    tabuList = new TabuList(N);

    // initialise a set of valid test suite at random
    List<int[]> A = new ArrayList<>();
    long coveredNum = 0;
    for (int i = 0; i < N; i++) {
      int[] tc = getMaxHamDistRow(A, i);
      A.add(tc);
      coveredNum += model.coverUpdate(tc);
    }

    long uncovered = model.SPACE_ALL - coveredNum;  // number of uncovered combinations
    long violation = handler.penaltyTerm(A);       // number of constraints violations
    //System.out.println("try to find an array of size " + N + " | uncovered = " + uncovered + ", violations = " + violation);

    if (uncovered == 0 && violation == 0)
      return A;

    int round = 0;
    Instant start = Instant.now();
    while (round < ROUND) {
      Element move = null;
      List<Tuple> listTp = new ArrayList<>();
      List<Integer> listRow = new ArrayList<>();

      if (violation > 0) {
        // get an invalid tuple that is covered by test suit at random
        model.getCoveredConstraints(new TestSuite(A), listTp, listRow);
        if (listTp.size() > 0) {
          int index = random.nextInt(listTp.size());
          Tuple tp = listTp.get(index);
          int row = listRow.get(index);
          // the move to break this tuple
          move = violateInvalidTupleMove(A, row, tp.position, tp.schema);
        }
      }
      if (listTp.size() == 0) {
        // if there are uncovered tuple, try to cover it
        if (uncovered > 0) {
          Tuple tp = model.getRandomUncoveredTuple();
          if (tp == null) {
            System.err.println("ERROR (TS): uncovered has occurred error!");
            System.exit(0);
          }
          // the move to cover this tuple
          move = coverMissingTupleMove(A, tp.position, tp.schema);
          if (move == null || move.delta_uncovered == 0)
            move = randomMove(A);
        }
        else {
          // conduct a random move
          move = randomMove(A);
        }
      }

      // if there is no move
      if (move == null) {
        round++;
        continue;
      }

      // the change
      int[] candidate = new int[model.parameter];
      System.arraycopy(A.get(move.row), 0, candidate, 0, model.parameter);
      candidate[move.column] = move.value;

      // update
      long fit = model.fitnessDelta(A.get(move.row), candidate);
      tabuList.updateUndoList(move.row, move.column, A.get(move.row)[move.column]);
      if (fit != move.delta_uncovered) {
        System.err.println("ERROR (TS): fit != move.delta_uncovered");
        System.exit(0);
      }

      // apply the new move, A = A1
      A.get(move.row)[move.column] = move.value;
      uncovered = uncovered + move.delta_uncovered;
      violation = violation + move.delta_violation;

      // if get a covering array
      if (uncovered == 0 && violation == 0) {
        return A;
      }

      round += 1;
      Instant end = Instant.now();
      if (Duration.between(start, end).toMillis() / 1000 > 600)
        break;
    }

    return new ArrayList<>();
  }

  /**
   * Generate 10 moves at random, and select the best one as the final move.
   * @param A the candidate array
   */
  private Element randomMove(List<int[]> A) {
    Element bestMove = null;
    ArrayList<Element> bestMoveSet = new ArrayList<>();

    double effect, bestEffect = 0;
    for (int count = 0 ; count < 10 ; count++ ) {
      Element move = new Element();
      move.row = random.nextInt(A.size());
      move.column = random.nextInt(model.parameter);
      move.value = random.nextInt(model.value[move.column]);

      // the change
      int[] candidate = new int[model.parameter];
      System.arraycopy(A.get(move.row), 0, candidate, 0, model.parameter);
      candidate[move.column] = move.value;

      // if the change violates constraints, or tabu, ignore this move
      boolean tabu = tabuList.isTabu(A, move.row, move.column, move.value);
      if (move.value == A.get(move.row)[move.column] || !handler.isValid(candidate) || tabu)
        continue;

      move.delta_uncovered = model.fitnessDeltaEvaluation(A.get(move.row), candidate);                //  U' - U
      move.delta_violation = handler.penaltyTerm(candidate) - handler.penaltyTerm(A.get(move.row)); //  V' - V
      effect = move.delta_uncovered + move.delta_violation * WEIGHT;

      if (bestEffect >= effect) {
        if (bestEffect > effect) {
          bestMoveSet.clear();
          bestEffect = effect;
        }
        bestMoveSet.add(move);
      }
    }

    if (bestMoveSet.size() > 0) {
      int p = random.nextInt(bestMoveSet.size());
      bestMove = bestMoveSet.get(p);
    }

    return bestMove;
  }

  /**
   * Generate a move to cover a currently uncovered combination.
   * @param A the candidate array
   * @param position position of the t-way combination
   * @param schema schema of the t-way combination
   */
  private Element coverMissingTupleMove(List<int[]> A, int[] position, int[] schema) {
    Element bestMove = null;
    ArrayList<Element> bestMoveSet = new ArrayList<>();

    double effect, bestEffect = 0;
    boolean isFirstMove = true;
    // iterate each row in the array
    for (int z = 0; z < A.size(); z++) {
      int difCount = 0;   // the number of differences
      Element move = new Element();
      for (int m = 0; m < model.t_way; m++) {
        if (A.get(z)[position[m]] != schema[m]) {
          if (difCount == 0) {
            move.row = z;
            move.column = position[m];
            move.value = schema[m];
          }
          difCount++;
        }
        if (difCount >= 2)    // only change a position
          break;
      }
      if (difCount >= 2)
        continue;

      // the change
      int[] candidate = new int[model.parameter];
      System.arraycopy(A.get(move.row), 0, candidate, 0, model.parameter);
      candidate[move.column] = move.value;

      // if the change violates constraints, or is tabu, ignore this move
      if (!handler.isValid(candidate) || tabuList.isTabu(A, z, move.column, move.value))
        continue;

      move.delta_uncovered = model.fitnessDeltaEvaluation(A.get(move.row), candidate);                //  U' - U
      move.delta_violation = handler.penaltyTerm(candidate) - handler.penaltyTerm(A.get(move.row)); //  V' - V
      effect = move.delta_uncovered + move.delta_violation * WEIGHT;

      if (isFirstMove || bestEffect >= effect) {
        if (isFirstMove || bestEffect > effect) {
          bestMoveSet.clear();
          bestEffect = effect;
          if (isFirstMove)
            isFirstMove = false;
        }
        bestMoveSet.add(move);
      }
    }

    // select the best move
    if (bestMoveSet.size() > 0) {
      int p = random.nextInt(bestMoveSet.size());
      bestMove = bestMoveSet.get(p);
    }
    return bestMove;
  }

  /**
   *
   * @param A
   * @param row
   * @param position
   * @param schema
   * @return
   */
  private Element violateInvalidTupleMove(List<int[]> A, int row, int[] position, int[] schema) {
    Element bestMove = null;
    ArrayList<Element> bestMoveSet = new ArrayList<>();
    double effect, bestEffect = 0;
    boolean isFirstMove = true;
    for (int i = 0; i < position.length; i++) {
      //计算有多少个不同
      int difCount = 0;

      //去除原来值，每个参数遍历其它所有取值
      for (int value = 0; value < model.value[position[i]]; value++) {
        if (value == schema[i])
          continue;

        Element move = new Element(row, position[i], value);

        // the change
        int[] candidate = new int[model.parameter];
        System.arraycopy(A.get(move.row), 0, candidate, 0, model.parameter);
        candidate[move.column] = move.value;

        // if the change violates constraints, or is tabu, ignore this move
        if (!handler.isValid(candidate) || tabuList.isTabu(A, row, move.column, move.value))
          continue;
        move.delta_uncovered = model.fitnessDeltaEvaluation(A.get(move.row), candidate);    //  U' - U
        move.delta_violation = handler.penaltyTerm(candidate) - handler.penaltyTerm(A.get(move.row)); //V'-V

        //移动后与移动前总变化
        effect = move.delta_uncovered + move.delta_violation * WEIGHT;

        if (isFirstMove || bestEffect >= effect) {
          if (isFirstMove || bestEffect > effect) {
            bestMoveSet.clear();
            bestEffect = effect;
            if (isFirstMove)
              isFirstMove = false;
          }
          bestMoveSet.add(move);
        }
      }
    }
    //存在可行移动，从bestMoveSet随机选择一个
    if (bestMoveSet.size() > 0) {
      int p = random.nextInt(bestMoveSet.size());
      bestMove = bestMoveSet.get(p);
    }
    return bestMove;
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
    for (int k = val.length - 1; k >= val.length - model.t_way; k--)
      bound[0] = bound[0] * val[k];

    // upper bound
    for (int k = 0; k < model.t_way; k++)
      bound[1] = bound[1] * val[val.length - 1];

    return bound;
  }

  /**
   * Get a candidate constraints satisfying test case at random.
   */
  private int[] sample2() {
    int[] tc = new int[model.parameter];
    for (int i = 0; i < model.parameter; i++)
      tc[i] = -1;
    for (int i = 0; i < model.parameter; i++) {
      tc[i] = random.nextInt(model.value[i]);
      boolean contain = model.constrainedParameters.contains(i);
      while (contain && !handler.isValid(tc))
        tc[i] = random.nextInt(model.value[i]);
    }
    return tc;
  }


  /**
   * Get initial solutions: generate 10 random test cases at random, and the select the one
   * that has the largest hamming distance.
   * @param ca  the candidate test set
   * @param row the number of rows
   */
  private int[] getMaxHamDistRow(List<int[]> ca, int row) {
    int[][] cdt = new int[10][];
    int[] hamdis = new int[10];
    int i, j, k;

    if (ca.size() == 0) {
      cdt[0] = sample2();
      return cdt[0];
    }

    for (i = 0; i < 10; i++)
      cdt[i] = sample2();

    for (k = 0; k < 10; k++) {
      int mindis = model.parameter + 1;
      for (i = 0; i < row; i++) {
        int dis = 0;
        for (j = 0; j < model.parameter; j++)
          if (ca.get(i)[j] != cdt[k][j])
            dis++;
        if (dis < mindis)
          mindis = dis;
      }
      hamdis[k] = mindis;
    }

    int maxIndex = 0;
    for (k = 1; k < 10; k++)
      if (hamdis[maxIndex] < hamdis[k])
        maxIndex = k;

    return cdt[maxIndex];
  }


  /**
   * The object of Tabu list.
   */
  class TabuList {

    private Element[] modifiedList;
    private int front;     // head of the queue
    private int rear;      // rear of the queue
    private int rowNum;    // size of covering array

    TabuList(int rowNum) {
      this.rowNum = rowNum;
      // use a circular queue
      modifiedList = new Element[LENGTH + 1];
      for (int i = 0; i < LENGTH + 1; i++)
        modifiedList[i] = new Element();
      front = rear = 0;
    }

    // get a specified element
    Element getElement(int p) {
      int k = (front + p) % (LENGTH + 1);
      return modifiedList[k];
    }

    // get the number of elements
    int getLength() {
      return (rear - front + LENGTH + 1) % (LENGTH + 1);
    }

    // clear the queue
    void clear() {
      front = rear = 0;
    }

    // update the queue
    void updateUndoList(int row, int column, int value) {
      // if not exceed the maximum length, directly append at the end of the queue
      if ((rear + 1) % (LENGTH + 1) != front) {
        modifiedList[rear].row = row;
        modifiedList[rear].column = column;
        modifiedList[rear].value = value;
        rear = (rear + 1) % (LENGTH + 1);
      } else {
        // else, remove the first element of the queue
        front = (front + 1) % (LENGTH + 1);
        modifiedList[rear].row = row;
        modifiedList[rear].column = column;
        modifiedList[rear].value = value;
        rear = (rear + 1) % (LENGTH + 1);
      }
    }

    // determine whether a move (element) is tabu
    boolean isTabu(List<int[]> A, int modiRow, int modiColumn, int modiValue) {
      int i, j, row, column, value, length;
      // the difference between current CA and a previous CA, -1 indicates the same
      int[][] diffPosition = new int[rowNum][model.parameter];

      length = getLength();
      if (length == 0)
        return false;

      // the number of different elements
      int stateCount = 0;

      for (i = 0; i < rowNum; i++)
        for (j = 0; j < model.parameter; j++)
          diffPosition[i][j] = -1;

      // transfer the undoList into tabuMoves
      for (i = length - 1; i >= 0; i--) {
        row = getElement(i).row;
        column = getElement(i).column;
        value = getElement(i).value;

        if (A.get(row)[column] == value) {
          diffPosition[row][column] = -1;
          stateCount -= 1;
        } else {
          if (diffPosition[row][column] == -1)
            stateCount += 1;
          diffPosition[row][column] = value;
        }

        if (stateCount == 1 && diffPosition[modiRow][modiColumn] == modiValue)
          return true;
      }
      return false;
    }
  }

  /**
   * The object to represent an element in the covering array.
   */
  class Element {

    int row;
    int column;
    int value;             // the new value
    long delta_uncovered;  // the addition of number of covered combinations
    long delta_violation;  // the addition of number of constraints violations

    Element() {}

    Element(int row, int column, int value) {
      this.row = row;
      this.column = column;
      this.value = value;
    }

    Element(int row, int column, int value, long delta_uncovered, long delta_violation) {
      this.row = row;
      this.column = column;
      this.value = value;
      this.delta_uncovered = delta_uncovered;
      this.delta_violation = delta_violation;
    }
  }
}
