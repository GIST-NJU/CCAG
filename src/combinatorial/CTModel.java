package combinatorial;

import handler.ConstraintHandler;

import java.util.*;

import static java.lang.Math.abs;

/**
 * The test model of combinatorial testing.
 */
public class CTModel {

  public int parameter;
  public int[] value;
  public int t_way;
  public String name;

  // a list of constraints, each is represented by a conjunction of literals
  public List<int[]> constraint;

  // relation indicates the index of a given parameter value (starts from 1),
  // namely, each parameter value is represented by a boolean variable
  // For example, the mapping for CA(N;t,5,3) is as follows:
  //  p1  p2  p3  p4  p5
  //   1   4   7  10  13
  //   2   5   8  11  14
  //   3   6   9  12  15
  public int[][] relation;

  // the list of parameter combinations, and its size
  public List<int[]> PC_ALL;
  public int PC_NUM;

  // the set of parameters that are involved in constraints (index starts from 0)
  public HashSet<Integer> constrainedParameters;

  // the cover matrix of all t-way combinations
  private BArray combination;

  // statistics
  public long SPACE_RAW;       // the number of all possible combinations
  public long SPACE_ALL;       // the number of all valid combinations to be covered

  private Random random = new Random();

  public CTModel(String name, int parameter, final int[] value, int t_way, List<int[]> constraint) {
    this(parameter, value, t_way, constraint);
    this.name = name;
  }

  public CTModel(int parameter, final int[] value, int t_way, List<int[]> constraint) {
    this.parameter = parameter;
    this.value = value;
    this.t_way = t_way;
    this.constraint = constraint;
    this.name = "model";

    // determine constrained parameters
    constrainedParameters = new HashSet<>();
    HashSet<Integer> temp = new HashSet<>();
    if (constraint != null) {
      for (int[] c : constraint) {
        for (int v : c) {
          temp.add(abs(v));
        }
      }
    }

    // set mapping relationship
    relation = new int[parameter][];
    int start = 1;
    for (int i = 0; i < parameter; i++) {
      relation[i] = new int[value[i]];
      for (int j = 0; j < value[i]; j++, start++) {
        relation[i][j] = start;
        if (temp.contains(start))
          constrainedParameters.add(i);
      }
    }
  }

  /**
   * Initialise the set of all possible t-way combinations that needs to be covered.
   * This is generally the first step in a covering array generation algorithm.
   */
  public void initialization() {
    SPACE_RAW = SPACE_ALL = 0;
    PC_ALL = ALG.allCombination(parameter, t_way);
    PC_NUM = PC_ALL.size();

    // assign C(n, t) rows
    int row = ALG.combine(parameter, t_way);
    combination = new BArray(row);

    // enumerate every t parameters to calculate the number of combinations to be covered
    int i = 0;
    for (int[] pos : PC_ALL) {
      int cc = ALG.combineValue(pos, value);
      combination.initializeRow(i++, cc);
      SPACE_RAW += cc;
      SPACE_ALL += cc;
    }
    combination.initializeZeros();
  }

  /**
   * Remove invalid t-way combinations from the set of need-to-be-covered combinations.
   * Each invalid combination is either explicit or implicit constraint, which will be
   * identified by the isValid() method of the ConstraintHandler.
   * This process is typically needed after initialization(), unless a lazy detection
   * strategy is used in the algorithm.
   * @param handler the constraint handler to check validity (note that the handler
   *                should have been initialised)
   */
  public void removeInvalidCombinations(ConstraintHandler handler) {
    int row = 0;
    for (int[] pos : PC_ALL) {
      int column = 0;
      for (int[] sch : ALG.allV(pos, t_way, value)) {
        // for each t-way combination representing as pos[] and sch[]
        Tuple tuple = new Tuple(pos, sch, parameter);
        if (!handler.isValid(tuple.test))
          setCoverValue(row, column, -1);
        column += 1;
      }
      row += 1;
    }
  }

  /**
   * Copy the cover matrix to avoid multiple initialisations (for example, re-determining
   * the validity of all t-way combinations).
   * @param array the recorded cover matrix
   */
  public void copyCoverMatrix(int[][] array) {
    for (int i = 0 ; i < array.length ; i++) {
      array[i] = new int[combination.cover[i].length];
      System.arraycopy(combination.cover[i], 0, array[i], 0, array[i].length);
    }
  }

  /**
   * Reset the cover matrix to avoid multiple initialisations (for example, re-determining
   * the validity of all t-way combinations).
   * @param array the recorded cover matrix
   */
  public void resetCoverMatrix(final int[][] array) {
    SPACE_ALL = 0;
    for (int i = 0 ; i < array.length ; i++) {
      for (int j = 0 ; j < array[i].length ; j++) {
        combination.setElement(i, j, array[i][j]);
        if (array[i][j] == 0)
          SPACE_ALL++ ;
        else if (array[i][j] == 1)
          System.err.println("Reset cover matrix with value 1 (covered combinations).");
      }
    }
    combination.setZeros(SPACE_ALL);
  }





  /**
   * Get the number of uncovered combinations (namely, the number of zeros in cover matrix).
   */
  public long UNCOVERED() {
    return combination.getZeros();
  }

  /**
   * Get a currently uncovered t-way combination at random.
   */
  public Tuple getRandomUncoveredTuple() {
    Position e = combination.getRandomZeroPosition2();
    if (e == null)
      return null;

    int[] pos = PC_ALL.get(e.row);
    int[] sch = ALG.num2val(e.column, pos, t_way, value);
    return new Tuple(pos, sch, parameter);
  }

  /**
   * Get the set of all uncovered and valid tuples in a particular row (t parameters) of
   * cover matrix.
   * @param position the indexes of parameters
   */
  public List<Tuple> getUncoveredTuple(int[] position) {
    List<Tuple> results = new ArrayList<>();
    int row = ALG.combine2num(position, parameter, t_way);
    for (int j = 0 ; j < combination.cover[row].length ; j++) {
      if (combination.cover[row][j] == 0) {
        int[] sch = ALG.num2val(j, position, t_way, value);
        results.add(new Tuple(position, sch, parameter));
      }
    }
    return results;
  }


  /**
   * Get all constraints that are covered by the test suite at random. The results
   * will be added into the last two lists.
   * @param ts a test suite
   * @param cons the covered constraints in the suite
   * @param rows the corresponding indexes of rows
   */
  public void getCoveredConstraints(TestSuite ts, List<Tuple> cons, List<Integer> rows) {
    for (int k = 0 ; k < ts.suite.size() ; k++) {
      int[] test = ts.suite.get(k);
      // verify each test case against the list of this.constraint
      for (int[] cs : this.constraint) {
        int i, j ;
        int[] pos = new int[cs.length];
        int[] sch = new int[cs.length];
        for (i = 0, j = 0 ; i < test.length ; i++) {
          if (relation[i][test[i]] == -cs[j]) {
            pos[j] = i ;
            sch[j] = test[i];
            j++;
          }
          if (j == cs.length || relation[i][test[i]] > -cs[j])
            break;
        }
        // if this constraint is covered
        if (j == cs.length) {
          cons.add(new Tuple(pos, sch, parameter));
          rows.add(k);
          break;
        }
      }
    }

  }



  /**
   * Assign any value to the particular position of cover matrix. Assign -1 to mark
   * an invalid t-way combination.
   * @param row the row of cover matrix
   * @param column the column of cover matrix
   * @param value the new value
   */
  public void setCoverValue(int row, int column, int value) {
    if (value == -1) {
      if (combination.getElement(row, column) != -1) {
        combination.setElement(row, column, -1);
        SPACE_ALL--;
      }
    } else {
      combination.setElement(row, column, value);
    }
  }

  /**
   * Set the given t-way combination as invalid (namely, -1 in the cover matrix).
   * @param position the indexes of parameters
   * @param schema the corresponding parameter values
   */
  public void setInvalid(final int[] position, final int[] schema) {
    int i = ALG.combine2num(position, parameter, t_way);
    int j = ALG.val2num(position, schema, t_way, value);

    if (combination.getElement(i, j) != -1) {
      combination.setElement(i, j, -1);
      SPACE_ALL--;
    }
  }





  /**
   * Set the currently uncovered t-way combinations involving in the given test case
   * as covered (for example, when this test case is added into the test suite), and
   * returns the size of these combinations.
   * Note that this method does not account for the validity of the test case. Namely,
   * it only consider valid t-way combinations, and any invalid t-way combination will
   * be simply ignored.
   * @param test a test case
   */
  public long coverUpdate(final int[] test) {
    int num = 0;
    int row = 0;
    for (int[] position : PC_ALL) {
      int column = ALG.valtest2num(position, test, t_way, value);
      // if it is not a t-way combination, then ignore it
      if (column != -1 && cover(row, column, 1) == 0)
        num++;
      row += 1;
    }
    return num;
  }

  /**
   * Set the given t-way combinations as covered (basically, cover[i][j]++).
   * @param position the indexes of parameters
   * @param schema the corresponding parameter values
   */
  public void coverUpdate(int[] position, int[] schema) {
    cover(position, schema, 1);
  }

  /**
   * Set the given t-way combinations as covered (basically, cover[i][j]++).
   * @param row row of the cover matrix
   * @param column column of the cover matrix
   */
  public void coverUpdate(int row, int column) {
    cover(row, column, 1);
  }





  /**
   * Calculate the number of uncovered combinations that can be covered by a given
   * test case.
   * Note that this method does not account for the validity of the test case. Namely,
   * it only consider valid t-way combinations, and any invalid t-way combination will
   * be simply ignored.
   * @param test a test case
   */
  public long coverNumber(final int[] test) {
    int num = 0;
    int row = 0;
    for (int[] position : PC_ALL) {
      int column = ALG.valtest2num(position, test, t_way, value);
      // if it is not a t-way combination, then ignore it
      if (column != -1 && cover(row, column, 0) == 0)
        num++;
      row += 1;
    }
    return num;
  }

  /**
   * Get the coverage state of the given t-way combination (-1 for invalid tuple,
   * 0 for uncovered tuple, and positive value indicates number of times that it
   * is covered).
   * @param position the indexes of parameters
   * @param schema the corresponding parameter values
   */
  public int coverState(int[] position, int[] schema) {
    return cover(position, schema, 0);
  }

  /**
   * Get the coverage state of the given t-way combination (-1 for invalid tuple,
   * 0 for uncovered tuple, and positive value indicates number of times that it
   * is covered).
   * @param row row of the cover matrix
   * @param column column of the cover matrix
   */
  public int coverState(int row, int column) {
    return cover(row, column, 0);
  }

  /**
   * Calculate fitness(A') - fitness(A), where fitness() is defined as the number of
   * yet-to-be covered combinations (delta < 0 indicates a better solution). There is
   * only one test case that has different values between A (a1) and A' (a2).
   * Note that the cover matrix will be modified accordingly when using this method, but
   * you can simply swap a1 and a2 to reset the changes.
   * @param a1 the test case in A
   * @param a2 the test case in A'
   */
  public int fitnessDelta(final int[] a1, final int[] a2) {
    int removal = 0, addition = 0;

    int row = 0;
    for (int[] pos : PC_ALL) {
      int column = ALG.valtest2num(pos, a1, t_way, value);
      int state = combination.getElement(row, column);
      // if this t-way combination is valid
      if (state != -1) {
        if (state - 1 == 0) // the combination is now uncovered in the test suite
          removal++;
        combination.elementDecrease(row, column);
      }
      row += 1 ;
    }

    row = 0;
    for (int[] pos : PC_ALL) {
      int column = ALG.valtest2num(pos, a2, t_way, value);
      int state = combination.getElement(row, column);
      // if this t-way combination is valid
      if (state != -1) {
        if (state == 0)  // the combination is now covered in the test suite
          addition++;
        combination.elementIncrease(row, column);
      }
      row += 1 ;
    }

    return removal - addition;
  }

  /**
   * Calculate fitness(A') - fitness(A), where fitness() is defined as the number of
   * yet-to-be covered combinations (delta < 0 indicates a better solution). There is
   * only one test case that has different values between A (a1) and A' (a2).
   * In this version, the cover matrix will not be modified.
   * @param a1 the test case in A
   * @param a2 the test case in A'
   */
  public int fitnessDeltaEvaluation(final int[] a1, final int[] a2) {
    int removal = 0, addition = 0;

    int[] index_column = new int[PC_NUM];
    int[] index_state = new int[PC_NUM];

    int row = 0;
    for (int[] pos : PC_ALL) {
      int column = ALG.valtest2num(pos, a1, t_way, value);
      int state = combination.getElement(row, column);
      index_column[row] = column;
      index_state[row] = state;
      // if this t-way combination is valid
      if (state != -1) {
        if (state - 1 == 0) // the combination is now uncovered in the test suite
          removal++;
        index_state[row]--;
      }
      row += 1 ;
    }

    row = 0;
    for (int[] pos : PC_ALL) {
      int column = ALG.valtest2num(pos, a2, t_way, value);
      int state = index_column[row] == column ? index_state[row] : combination.getElement(row, column);
      // if this t-way combination is valid
      if (state != -1) {
        if (state == 0)  // the combination is now covered in the test suite
          addition++;
      }
      row += 1 ;
    }

    return removal - addition;
  }





  /**
   * Determine the state of the given k-way combination (invalid, uncovered, or covered).
   * @param position the indexes of parameters
   * @param schema the corresponding parameter values
   * @param FLAG FLAG = 0, the combination will not be updated
   *             FLAG = 1, the combination will be updated accordingly
   * @return  -1  if it is an invalid combination
   *          0   if it is an uncovered combination
   *          > 0 if it is a covered combination (number of times)
   */
  private int cover(int[] position, int[] schema, int FLAG) {
    int row = ALG.combine2num(position, parameter, t_way);
    int column = ALG.val2num(position, schema, t_way, value);
    return cover(row, column, FLAG);
  }

  /**
   * Determine the state of the given k-way combination (invalid, uncovered, or covered).
   * @param row row of the cover matrix
   * @param column column of the matrix
   * @param FLAG FLAG = 0, the combination will not be updated
   *             FLAG = 1, the combination will be updated accordingly
   * @return  -1  if it is an invalid combination
   *          0   if it is an uncovered combination
   *          > 0 if it is a covered combination (number of times)
   */
  private int cover(int row, int column, int FLAG) {
    int cov = combination.getElement(row, column) ;
    // if valid, and flag = 1, cover[row][column]++
    if (FLAG == 1 && cov >= 0) {
      combination.elementIncrease(row, column);
    }
    return cov ;
  }

  /**
   * Display.
   */
  public void show() {
    System.out.println("parameter = " + parameter);
    System.out.println("value = " + Arrays.toString(value));
    System.out.println("t-way = " + t_way);
    System.out.println("size of original constraints = " + constraint.size());
    constraint.forEach(x -> System.out.println(Arrays.toString(x)));
    System.out.println("constrained parameters = " + constrainedParameters);
    System.out.println("raw space = " + SPACE_RAW + ", valid combinations = " + SPACE_ALL);
  }

  /**
   * Display cover matrix, where "-" indicates invalid combination, and integer value
   * indicates the number of times each combination is covered.
   */
  public void showCoverMatrix() {
    System.out.println("---------- Cover Matrix ----------");
    int index = 0;
    for (int[] pos : PC_ALL)
      System.out.println(Arrays.toString(pos) + ": " + combination.coverMatrixRow(index++));
  }

}