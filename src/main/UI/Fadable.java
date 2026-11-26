package main.UI;

// A screen whose elements (buttons, text, pictures - not its background) can fade in and out.
public interface Fadable {
    boolean isFadeEnabled();

    float getContentAlpha();

    void setContentAlpha(float alpha);

    // Smoothly changes the elements' opacity to {@code target} over {@code ms} milliseconds, then runs {@code done}.
    void fadeTo(float target, int ms, Runnable done);
}
