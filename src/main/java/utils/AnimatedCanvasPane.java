package utils;

import javafx.animation.AnimationTimer;

/**
 * Resizable canvas with animation loop
 */
public abstract class AnimatedCanvasPane extends CanvasPane {
    private AnimationTimer timer;

    public AnimatedCanvasPane(double width, double height) {
        super(width, height);
        startAnimation();
    }

    public void startAnimation() {
        if (timer == null) {
            timer = new AnimationTimer()
            {
                public void handle(long currentNanoTime)
                {
                    updateView();
                }
            };
            timer.start();
        }
    }

    public void stopAnimation() {
        timer.stop();
    }

    public abstract void updateView();
}
