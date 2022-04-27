/*
    Utility class with methods for printing arrays
 */
public class Utils {

    /*
        bytes - array to be printed

        Prints a byte array
     */
    public static void PrintByteArray(byte[] bytes) {

        String message = "";
        for(int i = 0; i < bytes.length; i++){
            message += String.format("%d", bytes[i]);
            if(i < bytes.length - 1) {
                message += String.format(", ", bytes[i]);
            }
        }
        System.out.println("Message: " + message);
    }

    /*
        shorts - array to be printed
        message - message to be printed before the array

        Prints a short array
     */
    public static void PrintShortArray(short[] shorts, String message) {

        String out = "";
        for(int i = 0; i < shorts.length; i++){
            out += String.format("%d", shorts[i]);
            if(i < shorts.length - 1) {
                out += String.format(", ", shorts[i]);
            }
        }
        System.out.println(message + ": " + out);
    }
}
