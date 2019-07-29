package generator;

import combinatorial.CTModel;
import combinatorial.TestSuite;
import handler.ConstraintHandler;
import handler.ConstraintHandler.Handlers;

import java.util.*;

import static handler.ConstraintHandler.Handlers.*;

/**
 * An implementation of the Particle Swarm Optimisation (PSO) algorithm, which uses the
 * One-Test-at-a-Time framework.
 */
public class PSO extends CAGenerator {

  class Particle {
    int dimension;
    int[] position;
    double[] velocity;
    double fitness;

    // the local best position and its fitness value
    int[] pbest;
    double pbest_fitness;

    Particle () {
      dimension = model.parameter;
      position = new int[dimension];
      velocity = new double[dimension];
      fitness = -1;
      pbest = new int[dimension];
      pbest_fitness = -1;
    }

    // initialise the particle to a valid position
    void init() {
      for (int x = 0 ; x < dimension ; x++)
        position[x] = -1;

      Collections.shuffle(parameterList);
      for (int e : parameterList) {
        velocity[e] = 2 * random.nextDouble() - 1;
        position[e] = random.nextInt(model.value[e]);
        boolean contain = model.constrainedParameters.contains(e);
        while (contain && (!handler.isValid(position) || handler.penaltyTerm(position) > 0))
          position[e] = random.nextInt(model.value[e]);
      }
    }

    // set current position as its local best position
    void setPBest() {
      System.arraycopy(position, 0, pbest, 0, dimension);
      pbest_fitness = fitness;
    }

    // update
    void update(final int[] gbest) {
      // ------------ velocity update ------------ //
      for (int i = 0; i < dimension; i++) {
        double r1 = random.nextDouble(), r2 = random.nextDouble();
        velocity[i] = WEIGHT * velocity[i] +
            FACTOR * r1 * (pbest[i] - position[i]) +
            FACTOR * r2 * (gbest[i] - position[i]);

        // apply maximum velocity limitation
        if (velocity[i] > model.value[i] / 2)
          velocity[i] = (double) model.value[i] / 2;
        if (velocity[i] < -model.value[i] / 2)
          velocity[i] = (double) -model.value[i] / 2;
      }

      // ------------ position update ------------ //
      for (int i = 0; i < dimension; i++) {
        position[i] = (int) (position[i] + velocity[i]);

        // apply absorbing walls
        if (position[i] >= model.value[i])
          position[i] = model.value[i] - 1;
        if (position[i] < 0)
          position[i] = 0;
      }

      // if move to an invalid position, re-initialise its position
      if (!handler.isValid(position)) {
        for (int x = 0 ; x < dimension ; x++)
          position[x] = -1;

        Collections.shuffle(parameterList);
        for (int e : parameterList) {
          position[e] = random.nextInt(model.value[e]);
          boolean contain = model.constrainedParameters.contains(e);
          while (contain && !handler.isValid(position))
            position[e] = random.nextInt(model.value[e]);
        }
      }
    }
  }

  private CTModel model;
  private ConstraintHandler handler;

  private Random random;
  private List<Integer> parameterList;
  private int EACH_COVER_MAX;

  private int SIZE = 80;
  private int ROUND = 250;
  private double WEIGHT = 0.9;
  private double FACTOR = 1.3;
  private double PENALTY = 0.5;

  public PSO() {
    supportedHandlers = new Handlers[]{Verify, Solver, Tolerate, Replace};
    random = new Random();
  }

  public void settings(int size, int round, double weight, double factor, double penalty) {
    this.SIZE = size;
    this.ROUND = round;
    this.WEIGHT = weight;
    this.FACTOR = factor;
    this.PENALTY = penalty;
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

    parameterList = new ArrayList<>();
    for (int i = 0 ; i < model.parameter ; i++)
      parameterList.add(i);

    EACH_COVER_MAX = model.PC_ALL.size();
    model.removeInvalidCombinations(handler);

    while (model.UNCOVERED() != 0) {
      // apply PSO to construct a test case
      int[] next = search();
      if (next != null) {
        model.coverUpdate(next);
        ts.suite.add(next);
        //System.out.println("# covered = " + covered);
      }
    }
  }

  /**
   * Evolve to find a test case that covers the most uncovered combinations.
   */
  private int[] search() {

    // initialise a random set of valid solutions ()
    List<Particle> swarm = new ArrayList<>(SIZE);
    for (int i = 0 ; i < SIZE ; i++) {
      Particle particle = new Particle();
      particle.init();
      swarm.add(particle);
    }

    // best solution (should always be valid)
    int[] gbest = new int[model.parameter];
    double best_fit = -1;

    int iteration = 1;
    int count = 0;
    while (true) {
      // evaluate the fitness value of each particle
      // note that every particle satisfies "isValid(position) == true"
      boolean update = false;
      for (Particle p : swarm) {
        // the larger fitness value, the better
        long covered =  model.coverNumber(p.position);
        long violations = handler.penaltyTerm(p.position);
        //System.out.println(covered + " " + violations);
        p.fitness = covered - PENALTY * violations;

        // update gbest
        if (p.fitness > best_fit && violations == 0) {
          System.arraycopy(p.position, 0, gbest, 0, model.parameter);
          if (p.fitness == EACH_COVER_MAX)
            return gbest;
          best_fit = p.fitness;
          update = true;
        }

        // update pbest
        if (p.fitness >= p.pbest_fitness) {
          p.setPBest();
        }
      }

      count = update ? 0 : count + 1;

      // if exceed the maximum round, or no improvement is observed in 20 rounds
      if (iteration >= ROUND || count > 20)
        break;

      // update position and velocity
      for (Particle p : swarm) {
        p.update(gbest);
      }

      iteration = iteration + 1;

    } // end while

    // if covers nothing, then ignore it
    // for Tolerate, if it cannot find a valid solution, then best_fit will be -1
    return best_fit > 0 ? gbest : null ;
  }

}
