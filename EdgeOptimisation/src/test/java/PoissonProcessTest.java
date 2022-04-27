import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertTrue;

/*
    Test the Poisson process function
 */
public class PoissonProcessTest {

    @Test
    void PoissonTest() {
        double sum = 0;
        Random r = new Random();

        for(int i = 0; i < 100; i++) {
            sum += PoissonProcess.GetPoissonRandom(10, r);
        }

        sum = sum/100;
        System.out.printf("Poisson: %.2f%n", sum);
        assertTrue(Math.abs(10-sum) < 1);   //check mean over 100 runs is close to 10
    }
}
