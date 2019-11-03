package utils;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;

/**
 * Visualize the weights of the som in a canvas
 */
public class SomWeightsPane extends AnimatedCanvasPane {
    private volatile SelfOrganizingMap som;
    private final int PIXELS_WIDTH_PER_NEURON = 4;

    public SomWeightsPane(SelfOrganizingMap som, double width, double height) {
        super(width, height);
        this.som = som;
        updateCanvasSize();
    }

    @Override
    public void updateView() {
        updateCanvasSize();
        fillCanvas();
    }

    private void updateCanvasSize() {
        Canvas canvas = getCanvas();
        if (som.dimensions == 2) {
            canvas.setWidth(som.neuronPerDimension * PIXELS_WIDTH_PER_NEURON);
            canvas.setHeight(som.neuronPerDimension * PIXELS_WIDTH_PER_NEURON);
        }
        else {
            canvas.setHeight(PIXELS_WIDTH_PER_NEURON * 3);
        }
    }

    public SelfOrganizingMap getSom() {
        return som;
    }

    public void setSom(SelfOrganizingMap som) {
        this.som = som;
        updateCanvasSize();
    }

    public void fillCanvas() {
        Canvas canvas = this.getCanvas();
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        final PixelWriter pw = gc.getPixelWriter();

        int width = (int)canvas.getWidth();
        int height = (int)canvas.getHeight();
        double[] input = new double[som.dimensions];
        double[] output = new double[3];
        for(int y=0; y<height; y++){
            for (int x=0; x<width; x++){
                input[0] = (double) x / width;
                if (input.length == 2) {
                    input[1] = (double) y / height;
                }
                som.getNeuronWeightsFromGridPosition(input, output);
                Color color = new Color(Math.max(Math.min(output[0], 1.0), 0.0), Math.max(Math.min(output[1], 1.0), 0.0), Math.max(Math.min(output[2], 1.0), 0.0), 1.0);
                pw.setColor(x, y, new Color(Math.max(Math.min(output[0], 1.0), 0.0), Math.max(Math.min(output[1], 1.0), 0.0), Math.max(Math.min(output[2], 1.0), 0.0), 1.0));
            }
        }
    }

}
