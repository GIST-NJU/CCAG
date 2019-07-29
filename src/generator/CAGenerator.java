package generator;

import combinatorial.CTModel;
import combinatorial.TestSuite;
import handler.*;
import handler.ConstraintHandler.Handlers;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;


/**
 * The CCAG framework of constrained covering array generation.
 */
public abstract class CAGenerator {

  /**
   * The constraint handlers that is supported in the generation algorithm (for example,
   * Tolerate can only be used when a fitness function is involved).
   */
  public Handlers[] supportedHandlers = null;

  /**
   * The size of test suite and the computational cost (ms) obtained.
   */
  public int size = -1 ;
  public long time = -1 ;

  /**
   * The method to construct a t-way constrained covering array for a given test model.
   * @param model a combinatorial test model
   * @param handlerName the constraint handler to be used
   * @param ts the generated test suite
   */
  public void generation(CTModel model, Handlers handlerName, TestSuite ts) {
    if (supportedHandlers == null || Arrays.stream(supportedHandlers).noneMatch(x -> x == handlerName)) {
      System.err.println("The handler is not supported in this generator.");
      return;
    }

    // initialise the specified constraint handler
    ConstraintHandler handler = null ;
    switch (handlerName) {
      case Verify:
        handler = new HandlerVerify();
        break;
      case Solver:
        handler = new HandlerSolver();
        break;
      case Replace:
        handler = new HandlerReplace();
        break;
      case Tolerate:
        handler = new HandlerTolerate();
        break;
    }

    Instant start = Instant.now();

    // initialisation (this should be invoked before pre-processing of handler)
    model.initialization();
    ts.suite.clear();

    // execute the pre-processing procedure of the constraint handler
    handler.pre(model);

    // execute the particular generation process
    process(model, handler, ts);

    // execute the post-processing procedure of the constraint handler
    handler.post(model, ts);

    Instant end = Instant.now();

    size = ts.size();
    time = Duration.between(start, end).toMillis();
  }

  /**
   * The particular generation algorithm. Use handler.isValid() and handler.penaltyTerm()
   * methods to deal with constraints encountered during the generation process.
   * @param model a combinatorial test model
   * @param handler the constraint handler
   * @param ts the generated test suite
   */
  abstract void process(CTModel model, ConstraintHandler handler, TestSuite ts);

}
