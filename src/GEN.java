import combinatorial.CTModel;
import combinatorial.TestSuite;
import generator.*;
import handler.ConstraintHandler.Handlers;
import utility.CASAFileReader;

import java.util.Arrays;

public class GEN {

  private static String M_PATH = "benchmark/%s.model";
  private static String C_PATH = "benchmark/%s.constraints";

  /**
   * Demo: use a particular combination of ALGORITHM and HANDLER to generate a covering array.
   * @param args the input parameters
   *             args[0] = algorithm
   *             args[1] = constraint handler
   *             args[2] = test model
   */
  public static void main(String[] args) {
    System.out.println("--------------------------------------------");
    System.out.println("     CA Generators of the CCAG Framework    ");
    System.out.println("--------------------------------------------");

    if (args.length != 3) {
      System.out.println("Please check your inputs:\njava -jar CCAG-GEN.jar ALGORITHM HANDLER MODEL");
      return;
    }

    String algorithm = args[0];
    CAGenerator gen = getGenerator(algorithm);
    if (gen == null) {
      System.out.println("Cannot find the specified generation algorithm.");
      return;
    }

    if (Arrays.stream(gen.supportedHandlers).noneMatch(x -> x.toString().equals(args[1]))) {
      System.out.println("The specified constraint handler is not supported.");
      return;
    }
    Handlers handler = Handlers.valueOf(args[1]);
    String model = args[2];

    System.out.println("Algorithm          : " + algorithm);
    System.out.println("Constraint Handler : " + handler);
    System.out.println("Test Model         : " + model);
    System.out.println("......");

    CASAFileReader fr = new CASAFileReader(String.format(M_PATH, model), String.format(C_PATH, model));
    CTModel md = new CTModel(model, fr.parameter, fr.value, fr.t_way, fr.constraint);
    TestSuite ts = new TestSuite();

    gen.generation(md, handler, ts);
    System.out.println(ts);
    System.out.println("Size = " + gen.size + ", Time = " + gen.time + " (ms)");
  }


  private static CAGenerator getGenerator(String name) {
    switch (name) {
      case "AETG":
        return new AETG();
      case "IPO":
        return new IPO();
      case "SA":
        return new SA();
      case "PSO":
        return new PSO();
      case "DDA":
        return new DDA();
      case "TS":
        return new TS();
    }
    return null;
  }

}
