/*
    Used to generate randomly generated values with a popularity skew
    using a Zipf distribution
 */
public class ZipfLaw {

    private static double denominator = 0;
    private static final float s = 2f;

    /*
        N - number of data elements

        Calculate the bottom part of zipf's law equation
     */
    public static void CalculateDenominator(int N) {

        double sum = 0;

        for(int n = 1; n <= N; n++) {
            sum += 1.0 / (Math.pow(n, s));
        }

        denominator = sum;
    }

    /*
           k - rank of the element

           Calculate Zipf frequency
     */
    public static double GetZipfFrequency(int k) {

        return (1.0 / (Math.pow(k, s))) / denominator;
    }
}
