package combinatorial;

import java.util.*;

public class ALG {

  /**
   * Calculate binomial coefficient c(n, m), where c(n, 0) = 1.
   *
   * @param n number of parameters
   * @param m number of chosen parameters
   * @return C(n, m)
   */
  public static int combine(int n, int m) {
    int ret = 1;
    int p = n;
    for (int x = 1; x <= m; x++, p--) {
      ret = ret * p;
      ret = ret / x;
    }
    return ret;
  }

  /**
   * Calculate the number of all possible value combinations
   * among a given parameter set.
   *
   * @param position indexes of chosen parameters
   * @param value    number of values of all parameters
   * @return number of value combinations among parameters
   */
  public static int combineValue(final int[] position, final int[] value) {
    int comb = 1;
    for (int k = 0; k < position.length; k++)
      comb = comb * value[position[k]];
    return comb;
  }

  /**
   * Calculate the index of a parameter combination in all possible
   * parameter combinations of c(n, m), where index starts at 0.
   *
   * combine2num({1, 2}, 4, 2) = 3,
   * because c(4,2) is as 0 1 , 0 2 , 0 3 , 1 2 , 1 3 , 2 3
   *
   * @param c a parameter combination
   * @param n number of parameters
   * @param m number of chosen parameters
   * @return index of c
   */
  public static int combine2num(final int[] c, int n, int m) {
    int ret = combine(n, m);
    for (int i = 0; i < m; i++) {
      ret -= combine(n - c[i] - 1, m - i);
    }
    return ret - 1;
  }

  /**
   * Calculate the t-th parameter combination of c(n, m),
   * where index starts at 0.
   *
   * num2combine(2, 4, 2) = {0, 3}
   * because c(4,2) is as 0 1 , 0 2 , 0 3 , 1 2 , 1 3 , 2 3
   *
   * @param t index of required parameter combination
   * @param n number of parameters
   * @param m number of chosen parameters
   * @return the t-th parameter combination
   */
  public static int[] num2combine(int t, int n, int m) {
    int[] ret = new int[m];
    t = t + 1;
    int j = 1, k;
    for (int i = 0; i < m; ret[i++] = j++) {
      for (; t > (k = combine(n - j, m - i - 1)); t -= k, j++) ;
    }
    for (int p = 0; p < m; p++)
      ret[p] = ret[p] - 1;
    return ret;
  }

  /**
   * Calculate all parameter combinations of c(n, m).
   * allCombination(4, 2) = {{0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3}}
   *
   * @param n number of parameters
   * @param m number of chosen parameters
   * @return all parameter combinations, each in a row
   */
  public static List<int[]> allCombination(int n, int m) {
    List<int[]> data = new ArrayList<>();
    dfs(data, new int[m], m, 1, n - m + 1, 0);
    return data;
  }

  private static void dfs(List<int[]> data, int[] list, int k_left, int from, int to, int index) {
    if (k_left == 0) {
      data.add(list.clone());
      return;
    }
    for (int i = from; i <= to; ++i ) {
      list[index++] = i - 1;
      dfs(data, list, k_left - 1, i + 1, to + 1, index);
      index -= 1;
    }
  }

  /**
   * Calculate the index of a t-way value combination among given parameters (index starts from 0).
   *
   * val2num({0, 1}, {1, 2}, 2, {3, 3, 3, 3}) = 5, as the orders of all 3^2 value combinations among
   * parameters {0, 1} are 0 0, 0 1, 0 2, 1 0, 1 1, 1 2, 2 0, 2 1, 2 2
   *
   * @param pos indexes of chosen parameters
   * @param sch a value combination
   * @param t number of chosen parameters
   * @param value number of values of all parameters
   * @return index of the value combination
   */
  public static int val2num(final int[] pos, final int[] sch, int t, final int[] value) {
    int ret = 0;
    int com = 1;
    for (int k = t - 1; k >= 0; k--) {
      ret += com * sch[k];
      com = com * value[pos[k]];
    }
    return ret;
  }

  /**
   * Calculate the index of a t-way value combination among given parameters (index starts from 0),
   * where the concrete value combination is given in a test case.
   *
   * val2num({0, 1}, {1, 2, 0, 0}, 2, {3, 3, 3, 3}) = 5, as the orders of all 3^2 value combinations
   * among parameters {0, 1} are 0 0, 0 1, 0 2, 1 0, 1 1, 1 2, 2 0, 2 1, 2 2
   *
   * @param pos indexes of chosen parameters
   * @param test the test case that gives the value combination
   * @param t number of chosen parameters
   * @param value number of values of all parameters
   * @return index of the value combination (-1 if it is not a tuple)
   */
  public static int valtest2num(final int[] pos, final int[] test, int t, final int[] value) {
    int ret = 0;
    int com = 1;
    for (int k = t - 1; k >= 0; k--) {
      if (test[pos[k]] == -1)
        return -1;
      ret += com * test[pos[k]];
      com = com * value[pos[k]];
    }
    return ret;
  }



  /**
   * Calculate the i-th t-way value combination among a given
   * parameter set, where index starts at 0.
   *
   * num2val(4, {1, 2}, 2, {3, 3, 3, 3}) = {1, 1}
   *
   * @param i     index of required value combination
   * @param pos   indexes of chosen parameters
   * @param t     number of chosen parameters
   * @param value number of values of all parameters
   * @return the i-th value combination
   */
  public static int[] num2val(int i, final int[] pos, int t, final int[] value) {
    int[] ret = new int[t];

    int div = 1;
    for (int k = t - 1; k > 0; k--)
      div = div * value[pos[k]];

    for (int k = 0; k < t - 1; k++) {
      ret[k] = i / div;
      i = i - ret[k] * div;
      div = div / value[pos[k + 1]];
    }
    ret[t - 1] = i / div;
    return ret;
  }

  /**
   * Calculate all t-way value combinations among a given parameter set.
   *
   * allV({0, 1}, 2, {3, 3, 3, 3}) =
   * {{0, 0}, {0, 1}, {0, 2}, {1, 0}, {1, 1}, {1, 2}, {2, 0}, {2, 1}, {2, 2}}
   *
   * @param pos   indexes of chosen parameters
   * @param t     number of chosen parameters
   * @param value number of values of all parameters
   * @return all value combinations among pos
   */
  public static int[][] allV(final int[] pos, int t, final int[] value) {
    if (t == 0 )
      return new int[1][0];

    int[] counter = new int[t];         // current combination
    int[] counter_max = new int[t];     // the maximum value of each element
    int comb = 1;
    for (int k = 0; k < t; k++) {
      counter[k] = 0;
      counter_max[k] = value[pos[k]] - 1;
      comb = comb * value[pos[k]];
    }
    int end = t - 1;

    int[][] data = new int[comb][t];
    for (int i = 0; i < comb; i++) {
      // assign data[i]
      data[i] = counter.clone();

      // move counter to the next one
      counter[end] = counter[end] + 1;
      int ptr = end;
      while (ptr > 0) {
        if (counter[ptr] > counter_max[ptr]) {
          counter[ptr] = 0;
          counter[--ptr] += 1;
        } else
          break;
      }
    }
    return data;
  }

  /**
   * Calculate the factorial of a non-negative integer.
   *
   * @param t input integer
   * @return t!
   */
  public static int cal_factorial(int t) {
    int n = 1;
    for (int i = 2; i <= t; i++)
      n = n * i;
    return n;
  }

  /**
   * Calculate all permutations of t relations {0, 1, 2, ..., t-1}
   * [1] The Countdown QuickPerm Algorithm, http://www.quickperm.org/
   *
   * @param t the number of relations
   * @return all permutations
   */
  public static Map<ArrayList<Integer>, Integer> cal_permutation(int t) {
    Map<ArrayList<Integer>, Integer> permutation = new HashMap<>();
    int count = 0;

    Integer[] v = new Integer[t];
    int[] p = new int[t + 1];
    for (int i = 1; i < t + 1; i++) {
      v[i - 1] = i - 1;
      p[i] = i;
    }

    permutation.put(new ArrayList<>(Arrays.asList(v)), count);
    count = count + 1;

    int i = 1;
    while (i < t) {
      p[i] = p[i] - 1;
      int j = i % 2 * p[i];   // if i is odd then j = p[i] otherwise j = 0
      Integer temp = v[i];    // swap(arr[i], arr[j])
      v[i] = v[j];
      v[j] = temp;

      // display new sequence
      permutation.put(new ArrayList<>(Arrays.asList(v)), count);
      count = count + 1;

      i = 1;
      while (p[i] == 0) {
        p[i] = i;
        i = i + 1;
      }
    }
    return permutation;
  }

  /**
   * Conduct selection sort for A and swap corresponding elements in B simultaneously.
   *
   * @param a the primary array
   * @param b the additional array
   * @param ascending true (ascending) or false (descending)
   */
  public static void sortArray(int[] a, int[] b, boolean ascending) {
    int i, j, idx;
    for (i = 0; i < a.length - 1; i++) {
      idx = i;
      for (j = i + 1; j < a.length; j++) {
        if (ascending && a[j] < a[idx])
          idx = j;
        else if (a[j] > a[idx])
          idx = j;
      }
      // swap A
      int temp = a[idx];
      a[idx] = a[i];
      a[i] = temp;
      // swap B
      temp = b[idx];
      b[idx] = b[i];
      b[i] = temp;
    }
  }

}

