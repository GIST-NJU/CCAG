package utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Read the combinatorial test model files of CASA format. Note that in current
 * version, all constraints should be given in the form of forbidden tuples
 * (namely, in the conjunction form of "- A - B - C").
 */
public class CASAFileReader {

  public int parameter;
  public int[] value;
  public int t_way;
  public ArrayList<int[]> constraint;

  /**
   * Read the combinatorial test model. Note that the representation of boolean
   * variables (each parameter value) in the CASA format starts from 0, but the
   * representation used in our solvers starts from 1.
   * @param modelFile .model file
   * @param constraintFile .constraints file
   */
  public CASAFileReader(String modelFile, String constraintFile) {
    try {
      // read model file, which consists of three lines: t_way, parameter, value
      File file = new File(modelFile);
      BufferedReader reader = new BufferedReader(new java.io.FileReader(file));

      t_way = Integer.valueOf(reader.readLine().trim());
      parameter = Integer.valueOf(reader.readLine().trim());
      value = new int[parameter];
      int i = 0;
      for (String each : reader.readLine().trim().split(" ")) {
        value[i++] = Integer.valueOf(each);
      }
      reader.close();

      // read constraint file
      constraint = new ArrayList<>();
      if (constraintFile != null) {
        file = new File(constraintFile);
        reader = new BufferedReader(new java.io.FileReader(file));

        // line 1 : total number
        int total = Integer.valueOf(reader.readLine().trim());

        // for each constraint
        for (i = 0; i < total; i++) {
          int num = Integer.valueOf(reader.readLine().trim());
          int[] each = new int[num];

          String[] str = reader.readLine().trim().split(" ");
          for (int j = 0; j < 2 * num; j += 2) {
            // specific symbol, note that the representation in the file starts
            // from 0 but the representation in the solver starts from 1.
            int v = Integer.valueOf(str[j + 1]) +1;
            each[j / 2] = str[j].equals("-") ? -v : v;
          }
          constraint.add(each);
        }
      }
      reader.close();

    } catch (IOException e) {
      System.err.println("No such test model file: " + modelFile + " / " + constraintFile);
    }
  }
}
