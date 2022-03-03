import java.util.Random;

public class PoissonProcess {

    /*
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
