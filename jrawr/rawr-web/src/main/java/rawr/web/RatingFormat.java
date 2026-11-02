package rawr.web;

import java.util.Locale;

/**
 * Single formatting rule for rating values on the result page; the web test formats the golden
 * value with this same method so page and expectation cannot drift apart.
 */
public final class RatingFormat {

    private RatingFormat() {
    }

    public static String format(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
