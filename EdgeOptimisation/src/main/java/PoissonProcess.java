import java.util.Random;

/*
    Class used to generate Poisson random values which estimates
    the amount of traffic arrival during a time frame
 */
public class PoissonProcess {

    /*
        mean - an average value to distribute around
        r - a random number generator using a set seed

        Knuth algorithm
        Given a mean, generate a Poisson-distributed variable
     */
    public static int GetPoissonRandom(double mean, Random r) {
        //init
        double L = Math.exp(-mean); //e^-lambda
        int k = 0;
        double p = 1.0;

        do {
            p = p * r.nextDouble();
            k++;
        } while (p > L);
        return k - 1;
    }
}
