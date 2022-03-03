public class ZipfLaw {

    private static double denominator = 0;
    private static final float s = 1f;

    /*
        Calculate the bottom part of zipf's law equation

        N - number of elements

     */
    public static void CalculateDenominator(int N) {

        double sum = 0;

        for(int n = 1; n <= N; n++) {
            sum += 1.0 / (Math.pow(n, s));
        }

        denominator = sum;
    }

    /*
           Calculate Zipf frequency

           k - rank of the element

     */
    public static double GetZipfFrequency(int k) {

        return (1.0 / (Math.pow(k, s))) / denominator;
    }
}
