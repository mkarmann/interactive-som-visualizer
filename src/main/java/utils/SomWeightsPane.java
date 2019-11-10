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
    private final int CANVAS_HEIGHT_1D = 20;
    private final int CANVAS_SIZE_2D = 150;
    private final int CANVAS_SIZE_3D = 44;

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
        if (som.dimensions == 3) {
            canvas.setWidth(CANVAS_SIZE_3D * som.neuronPerDimension);
            canvas.setHeight(CANVAS_SIZE_3D);
        }
        else if (som.dimensions == 2) {
            canvas.setWidth(CANVAS_SIZE_2D);
            canvas.setHeight(CANVAS_SIZE_2D);
        }
        else {
            canvas.setHeight(CANVAS_HEIGHT_1D);
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
                if (input.length != 3) {
                    input[0] = (double) x / width;
                }
                else {
                    input[0] = (double)((x * som.neuronPerDimension) % width) / width;
                    input[2] = (double)((x * som.neuronPerDimension) / width) / som.neuronPerDimension;
                }
                if (input.length > 1) {
                    input[1] = (double) y / height;
                }
                som.getNeuronWeightsFromGridPosition(input, output);
                Color color = new Color(
                        Math.max(Math.min(output[0] * 0.5 + 0.5, 1.0), 0.0),
                        Math.max(Math.min(output[1] * 0.5 + 0.5, 1.0), 0.0),
                        Math.max(Math.min(output[2] * 0.5 + 0.5, 1.0), 0.0), 1.0);
                pw.setColor(x, y, color);
            }
        }
    }

}
