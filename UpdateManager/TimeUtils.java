public class TimeUtils {
    public static double nowUtcSeconds() {
        return System.currentTimeMillis() / 1000.0;
    }

    public static String decimal(double number) {
        if (number == (long) number) {
            return String.format("%d", (long) number);
        }
        return String.format("%.8f", number).replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
