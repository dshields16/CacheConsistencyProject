import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/*
    Test the Zipf distribution function
 */
public class ZipfLawTest {

    @BeforeAll
    static void Init() {

        ZipfLaw.CalculateDenominator(10);
    }

    @Test
    void GetFrequencyForAllRanks() {

        double sum = 0;

        for(int i = 1; i <= 10; i++) {

            double freq = ZipfLaw.GetZipfFrequency(i);
            sum += freq;
            System.out.printf("Frequency for rank %d: %.2f%n", i, freq);
        }

        System.out.printf("Total Frequency: %.2f%n",  sum);
        assertTrue(Math.floor(sum) == 1);
    }
}
