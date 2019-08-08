# Constrained Covering Array Generation (CCAG)

CCAG is a reference implementation for the design and evaluation of test suite generation algorithms for constrained combinatorial testing. It provides building blocks for creating (greedy and heuristic search-based) constrained covering array generation algorithms, which especially supports the substitution of different constraint handlers.

Currently, four constraint handling techniques, including `Verify`, `Solver`, `Replace`, and `Tolerate`, as well as six covering array generation algorithms, including `AETG`, `DDA`, `IPO`, `PSO`, `SA`, and `TS`, are implemented.

## Overview

The CCAG framework is as follows:

```python
PreProcess()
S = an initial solution
while (S is not a covering array):
  S = Next(S, isValid(), PenaltyTerm())
PostProcess()
```

Here, `PreProcess()`, `isValid()`, `PenaltyTerm()`, and `PostProcess()` are four constraint handler-related methods, where different procedures will be executed in different constraint handlers. As such, as long as a generation algorithm is implemented under the CCAG framework with these methods, it can be easily configured to work with any of the constraint handlers.

#### The Implementation

The `CAGenerator` class is an implementation of the above framework, and should be extended by any generation algorithm. The following code gives its methods and variables:

```java
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
  public void generation(CTModel model, Handlers handlerName, TestSuite ts) {...}

  /**
   * The particular generation algorithm. Use handler.isValid() and handler.penaltyTerm()
   * methods to deal with constraints encountered during the generation process.
   * @param model a combinatorial test model
   * @param handler the constraint handler
   * @param ts the generated test suite
   */
  abstract void process(CTModel model, ConstraintHandler handler, TestSuite ts);
}
```

With this framework, we can simply use the following code to initialise a particular instance of `CAGenerator`, and use it to construct covering arrays of the given test model. Specifically, we just need to pass a particular `handler` variable to the `generation()` method (for example, `Verify`) to  determine the choice of constraint handlers that will be used.

```java
CTModel model = new CTModel(parameter, value, tway, constraints);  // the test model
TestSuite ts = new TestSuite();                                    // the test suite

CAGenerator c = new AETG();
c.generation(model, Verify, ts);     // use AETG with Verify constraint handler
c.generation(model, Replace, ts);    // use AETG with Replace constraint handler
System.out.println(ts); 
```

Detailed explanations for implementing covering array generation algorithms and constraint handlers in CCAG can be found in the [wiki page](https://github.com/GIST-NJU/CCAG/wiki) of this project.

#### Executable JAR File

To generate a constrained covering array by a particular generation algorithm and constraint handler, use the following command:

```
java -jar CCAG-GEN.jar <ALGORITHM> <HANDLER> <MODEL_NAME>
```

## The Comparative Study

We have conducted an empirical comparative study to investigate the impact of different constraint handling techniques on the performance of covering array generation algorithms that use them.

#### Benchmark

We used a well-known benchmark (as reported in [a previous study](https://ieeexplore.ieee.org/document/4564473/)) of constrained covering array generation as subject test models. The `NAME.model` (parameters and values) and `NAME.constraints` (constraints) files can be found in the **benchmark** directory; an explanation of their formats can be found [here](https://cse.unl.edu/~citportal/) (the same format of CASA tool). 

In addition, in order to investigate the failure revelation ability of different algorithms, we constructed 100 k-way failure causing combinations at random for k = 3, 4, 5, and 6, respectively. These combinations can be found in the `NAME.corpus` file of each test model (each combination is represented as `#K, [INDEXES OF PARAMETERS], [VALUES OF PARAMETERS]`).

#### Algorithms

Our experiment includes 21 generation algorithms, each of which is a particular combination of a covering array generation algorithm and a constraint handler: three variants (*X+Verify*, *X+Solver*, *X+Replace*) for each of AETG, DDA and IPO (greedy algorithms), and four variants (*X+Verify*, *X+Solver*, *X+Tolerate*, *X+Replace*) for each of PSO, SA and TS (search-based algorithms).

#### Test Suites

We generated the most widely used 2-way constrained covering arrays. For each test model, the execution of each algorithm is repeated 30 times. The test suites obtained can be found in the **data** directory (namely, each `ALGORITHM/HANDLER_MODEL.txt` file contains 30 covering arrays).

The tables that show sizes of covering arrays, computational costs, and proportions of failures detected by variants of each generation algorithm can be found in the [wiki page](https://github.com/GIST-NJU/CCAG/wiki) of this project.
