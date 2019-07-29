package handler.common;

import combinatorial.CTModel;

import java.util.*;

/**
 * Use the minimum forbidden tuple (MFT) as the validity checker. Given a test model,
 * it firstly calculates the MFS, then each solution can be verified against the MFT.
 */
public class MFT {


  private int[][] relation;
  public Vector<int[]> mft;

  public MFT() {
    mft = new Vector<>();
  }

  /**
   * Calculate minimum forbidden tuple.
   * @param model an object of CT model
   */
  public void calculateMFT(CTModel model) {
    if (model.constraint == null)
      return;

    relation = model.relation;
    Vector<int[]> original = new Vector<>();
    for (int[] x : model.constraint)
      original.add(sortedArray(x));
    minimise(original);

    // calculate implicit parameters: all values of an implicit parameter appear in constraints
    Vector<Integer> implicitPars = new Vector<>();
    for (int i = 0; i < relation.length; i++) {
      boolean isImplicit = true;
      for (int j = 0; j < relation[i].length; j++) {
        // whether this value choice is in a constraint
        boolean inCons = false;
        for (int[] cons : original) {
          for (int cc : cons) {
            if (relation[i][j] == Math.abs(cc)) {
              inCons = true;
              break;
            }
          }
          if (inCons)
            break;
        }
        // if there is a value that is not in any constraint, then it is not implicit
        if (!inCons) {
          isImplicit = false;
          break;
        }
      }
      if (isImplicit)
        implicitPars.add(i);
    }

    Vector<Integer> imParameter1 = new Vector<>(implicitPars);
    Vector<int[]> newConstraint = new Vector<>();

    // while there is an implicit parameter
    while (implicitPars.size() != 0) {
      Vector<Integer> tempPar = new Vector<>();
      newConstraint.clear();

      // for every implicit parameter, calculate the valid Cartesian product of all tuples
      for (Integer ip : implicitPars) {
        Vector<int[]> cartesian = new Vector<>();

        // for each value of an implicit parameter
        for (int ii = 0; ii < relation[ip].length; ii++) {

          // get all constraints that contain this value (this value is not included in tmpCons)
          Vector<int[]> tmpCons = new Vector<>();
          for (int[] cons : original) {
            for (int k = 0; k < cons.length; k++)
              // get a matched constraint
              if (Math.abs(cons[k]) == relation[ip][ii]) {
                int[] tmpArray = new int[cons.length - 1];
                int tmpI = 0, tmp1 = 0;
                while (tmpI < cons.length) {
                  if (tmpI != k) {
                    tmpArray[tmp1] = cons[tmpI];
                    tmp1++;
                  }
                  tmpI++;
                }
                tmpCons.add(tmpArray);
                break;
              }
          }

          // if this is the first value of the implicit parameter
          if (ii == 0) {
            cartesian.addAll(tmpCons);
          }
          // calculate Cartesian product of tuples in current cartesian set and the obtained tmpCons set
          else {
            Vector<int[]> tmp = new Vector<>(cartesian);
            cartesian.clear();

            for (int[] cons1 : tmp)
              for (int[] cons2 : tmpCons) {
                // combine the two tuples
                int[] mergeCons = combineTuple(cons1, cons2);

                // check the validity of this merged tuple
                boolean drop = false;
                for (int x = 0, y = 0; x < mergeCons.length - 1; x++) {
                  while (Math.abs(mergeCons[x]) > relation[y][relation[y].length - 1])
                    y++;
                  // get there are two different values for a same parameter
                  if (Math.abs(mergeCons[x + 1]) <= relation[y][relation[y].length - 1] && mergeCons[x] != mergeCons[x + 1]) {
                    drop = true;
                    break;
                  }
                }

                // if this tuple is valid and not duplicated, and not in cartesian, keep it
                if (!drop) {
                  List<Integer> list = new ArrayList<>();
                  for (int mc : mergeCons) {
                    if (!list.contains(mc))
                      list.add(mc);
                  }
                  int[] dc = list.stream().mapToInt(x -> x).toArray();
                  if (!included(cartesian, dc))
                    cartesian.add(dc);
                }

              }
          } // end else
        } // end for each value

        newConstraint.addAll(cartesian);
      }  // end for every implicit parameter

      // store new constraints and calculate implicit parameters for the next round
      for (int[] consN : newConstraint) {
        // check whether the constraint consN is duplicated
        boolean isNew = true;
        for (int[] consB : original) {
          if (isParent(consB, consN)) {
            isNew = false;
            break;
          }
        }

        if (isNew) {
          original.add(consN);
          for (int y = 0, k = 0; y < consN.length; y++) {
            // if a new constraint is added, and it contains an implicit parameter
            // then include this parameter in next round
            while (Math.abs(consN[y]) > relation[k][relation[k].length - 1])
              k++;
            if (imParameter1.contains(k) && !tempPar.contains(k))
              tempPar.add(k);
          }
        }
      }

      // update the new implicit parameters
      implicitPars.clear();
      implicitPars.addAll(tempPar);

      // minimise constraints
      minimise(original);

    } // end the outer while loop

    mft = original;

  }

  /**
   * Combine two tuples into a new tuple.
   */
  private int[] combineTuple(final int[] a, final int[] b) {
    int[] merge = new int[a.length + b.length];
    int x = 0, y = 0, z = 0;
    while (x < a.length || y < b.length) {
      if (x >= a.length) {
        merge[z] = b[y];
        y++;
      } else if (y >= b.length) {
        merge[z] = a[x];
        x++;
      } else if (a[x] > b[y]) {
        merge[z] = a[x];
        x++;
      } else {
        merge[z] = b[y];
        y++;
      }
      z++;
    }
    return merge;
  }

  /**
   * Reorder the array to its non-increasing order.
   */
  private int[] sortedArray(final int[] a) {
    int[] array = new int[a.length];
    System.arraycopy(a, 0, array, 0, a.length);
    Arrays.sort(array);

    // reverse order
    for (int i = 0; i < array.length / 2; i++) {
      int temp = array[i];
      array[i] = array[array.length - 1 - i];
      array[array.length - 1 - i] = temp;
    }
    return array;
  }

  /**
   * Determine whether a tuple A is included in the set V.
   * @param A a constraint
   * @param V a set of constraints
   */
  private boolean included(Vector<int[]> V, int[] A) {
    for (int[] b : V) {
      if (b.length == A.length) {
        boolean re = true;
        for (int j = 0; j < b.length; j++) {
          if (b[j] != A[j]) {
            re = false;
            break;
          }
        }
        if (re)
          return true;
      }
    }
    return false;
  }

  /**
   * Minimise the given set of forbidden tuples, so that only keeping the tuples
   * of the minimum size. Namely, if there are a parent tuple and a child tuple,
   * the parent tuple will be removed.
   * @param constraintSet the candidate set of forbidden tuple
   */
  private void minimise(Vector<int[]> constraintSet) {
    // the indexes of to-be-removed constraints
    ArrayList<Integer> removeList = new ArrayList<>();

    for (int i = 0; i < constraintSet.size() - 1; i++) {
      for (int j = i + 1; j < constraintSet.size(); j++) {
        int[] c1 = constraintSet.get(i), c2 = constraintSet.get(j);
        // if the two constraints are involved in each other, then remove the large one
        if (check(c1, c2)) {
          if (c1.length <= c2.length) {
            if (!removeList.contains(j))
              removeList.add(j);
          } else {
            if (!removeList.contains(i))
              removeList.add(i);
          }
        }
      }
    }

    removeList.sort(Collections.reverseOrder());
    for (Integer each : removeList)
      constraintSet.removeElementAt(each);
  }

  /**
   * Given two forbidden tuples, determine whether one is the parent tuple of the
   * another.
   */
  private boolean check(final int[] A, final int[] B) {
    // ensure tht |X| <= |Y|
    int[] X = B;
    int[] Y = A;
    if (A.length <= B.length) {
      X = A;
      Y = B;
    }
    return isParent(X, Y);
  }

  /**
   * Determine whether B is a parent tuple of A, namely, whether each and every
   * element in A is also in B.
   */
  private boolean isParent(final int[] A, final int[] B){
    if (B.length < A.length)
      return false;

    for (int a : A) {
      int j;
      for (j = 0; j < B.length; j++) {
        if (B[j] == a)
          break;
      }
      // cannot find the element till the end
      if (j == B.length && a != B[j - 1])
        return false;
    }
    return true;
  }

  /**
   * Determine whether a given complete or partial test case is constraints
   * satisfying. Any unfixed parameter is assigned to value -1.
   * @param test a complete or partial test case
   */
  public boolean satisfied(final int[] test) {
    return validity(test, false) == 0 ;
  }

  /**
   * Calculate the concrete number of forbidden tuples that are covered by the
   * given test case. Any unfixed parameter is assigned to value -1.
   * @param test a complete or partial test case
   * @return number of violations
   */
  public long violations(final int[] test) {
    return validity(test, true);
  }

  /**
   * The internal validity checker. Use flag variable count to switch between
   * the following two modes:
   * 1) count = true, return the concrete number of forbidden tuple that are
   *                  covered by the given test;
   * 2) count = false, return 0 and 1 to indicate the given test is, and is not,
   *                   constraints satisfying, respectively.
   */
  private long validity(final int[] test, boolean count) {
    if (mft.size() == 0)
      return 0;

    // convert the test case into disjunction form
    // for example: [0, 0, -1] => [1, 3, -1] when value = [2, 2, 2]
    int[] tc = new int[test.length];
    for (int i = 0 ; i < test.length ; i++)
      tc[i] = test[i] == -1 ? -1 : relation[i][test[i]];

    // calculate the number of constraints that tc contains
    long violationCount = 0;
    for (int[] cons : mft) {
      boolean matched = true;
      int k = 0;
      int j = 0;
      while (j < cons.length) {
        while (k < tc.length && (tc[k] == -1 || Math.abs(cons[j]) > tc[k])) {
          k++;
          if (k == tc.length)
            matched = false;
        }
        if (!matched)
          break;
        if (Math.abs(cons[j]) == tc[k]) {
          k++;
          j++;
        } else {
          matched = false;
          break;
        }
      }
      if (matched) {
        // violates a single constraint simply indicates this test is invalid
        if (!count) {
          return 1;
        }
        violationCount++;
      }
    }
    return violationCount;
  }


}