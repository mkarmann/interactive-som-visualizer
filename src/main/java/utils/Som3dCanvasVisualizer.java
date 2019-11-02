package utils;

import javafx.geometry.Point3D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.transform.Affine;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Visualize the som network in 3d
 */
public class Som3dCanvasVisualizer extends AnimatedCanvasPane {

    /**
     * Subclass for storing one 3d data point
     */
    private static class Color3dSample {
        public Color3dSample prevSampleX;
        public Color3dSample prevSampleY;
        public Color3dSample prevSampleXY;
        public Point3D point3D;
        public Color color;
        public double lineWidth;

        public Color3dSample(Color3dSample prevSampleX, Point3D point3D, Color color, double lineWidht) {
            this.prevSampleX = prevSampleX;
            this.point3D = point3D;
            this.color = color;
            this.lineWidth = lineWidht;
        }

        public Color3dSample(Color3dSample prevSampleX, Color3dSample prevSampleY, Point3D point3D, Color color, double lineWidth) {
            this.prevSampleX = prevSampleX;
            this.prevSampleY = prevSampleY;
            this.point3D = point3D;
            this.color = color;
            this.lineWidth = lineWidth;
        }
    }

    public SelfOrganizingMap som;       // som to visualize
    public double rotationX = 0.2;      // camera rotation x
    public double rotationY = -0.2;     // comaera rotation y
    private double lastDragX = 0;       // last cursor drag position x
    private double lastDragY = 0;       // last cursor drag position y

    public Som3dCanvasVisualizer(SelfOrganizingMap som, double width, double height) {
        this(width, height);
        this.som = som;
    }

    public Som3dCanvasVisualizer(double width, double height) {
        super(width, height);

        // Store drag start positions
        getCanvas().addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            lastDragX = e.getX();
            lastDragY = e.getY();
        });

        // Listen for mouse drag events
        getCanvas().addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double factor = 1 / Math.min(getWidth(), getHeight()) * 2 * Math.PI;
            rotationY += (e.getX() - lastDragX) * factor;
            rotationX += (e.getY() - lastDragY) * factor;
            lastDragX = e.getX();
            lastDragY = e.getY();
        });
    }

    public void updateView() {
        if (som != null) {
            int numSamples = som.neuronPerDimension;
            fillCanvas1dto3dGraph(som, getCanvas(), numSamples, rotationY, rotationX);
        }
    }

    public static boolean fillCanvas1dto3dGraph(SelfOrganizingMap som, Canvas canvas, int numSamples, double rotationY, double rotationX) {

        boolean is1dInput = som.dimensions == 1;
        double w = canvas.getWidth();
        double h = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0,0,w,h);

        gc.save();
        double scale = Math.min(w,h) * 0.5 / Math.sqrt(3);
        gc.transform(new Affine(new Translate(w * 0.5, h * 0.5)));
        gc.transform(new Affine(new Scale(scale, scale)));
        Color3dSample samples[] = new Color3dSample[(is1dInput ? numSamples : numSamples * numSamples) + 3];
        Color3dSample centerSample;
        double[] inputs = new double[is1dInput ? 1 : 2];
        double[] outputs = new double[3];
        double delta = 1.0 / (numSamples - 1);

        // stroke coord system
        centerSample = new Color3dSample(null, new Point3D(0,0,0), Color.BLACK, 1.0 / scale);
        samples[samples.length - 1] = new Color3dSample(centerSample, new Point3D(1,0,0), Color.rgb(255,0,0,0.25), 5.0 / scale);
        samples[samples.length - 2] = new Color3dSample(centerSample, new Point3D(0,-1,0), Color.rgb(0,128,0,0.25), 5.0 / scale);
        samples[samples.length - 3] = new Color3dSample(centerSample, new Point3D(0,0,1), Color.rgb(0,0,255,0.25), 5.0 / scale);

        if (is1dInput) {
            // collect 1d network output
            Color3dSample lastSample = null;
            for (int i = 0; i < numSamples; i++) {
                inputs[0] = i * delta;
                som.getNeuronWeightsFromGridPosition(inputs, outputs);
                samples[i] = new Color3dSample(
                        lastSample,
                        new Point3D(outputs[0], -outputs[1], outputs[2]),
                        Color.rgb(
                                Math.min(255, Math.max(0, (int) (255 * outputs[0]))),
                                Math.min(255, Math.max(0, (int) (255 * outputs[1]))),
                                Math.min(255, Math.max(0, (int) (255 * outputs[2])))
                        ),
                        3 / scale);
                lastSample = samples[i];
            }
        }
        else {
            // collect 2d network output
            for (int x=0; x<numSamples; x++) {
                for (int y=0; y<numSamples; y++) {
                    int index = x + y * numSamples;
                    inputs[0] = x * delta;
                    inputs[1] = y * delta;
                    som.getNeuronWeightsFromGridPosition(inputs, outputs);
                    Color3dSample sample = new Color3dSample(
                            null,
                            new Point3D(outputs[0], -outputs[1], outputs[2]),
                            Color.rgb(
                                    Math.min(255, Math.max(0, (int) (255 * outputs[0]))),
                                    Math.min(255, Math.max(0, (int) (255 * outputs[1]))),
                                    Math.min(255, Math.max(0, (int) (255 * outputs[2])))
                            ),
                            1 / scale);

                    if (x != 0) {
                        sample.prevSampleX = samples[(x - 1) + y * numSamples];
                    }
                    if (y != 0) {
                        sample.prevSampleY = samples[x + (y - 1) * numSamples];
                    }
                    if (x != 0 && y != 0) {
                        sample.prevSampleXY = samples[(x - 1) + (y - 1) * numSamples];
                    }

                    samples[index] = sample;
                }
            }
        }

        Rotate rotation = new Rotate(Math.toDegrees(rotationY), 0,0,0,new Point3D(0,1,0));
        Rotate rotation2 = new Rotate(Math.toDegrees(-rotationX), 0,0,0,new Point3D(1,0,0));
        for (Color3dSample sample : samples) {
            sample.point3D = rotation2.transform(rotation.transform(sample.point3D));

            // apply camera z
            double z_scale = 4. / (4.0 - sample.point3D.getZ());
            sample.point3D = new Point3D(sample.point3D.getX() * z_scale, sample.point3D.getY() * z_scale, sample.point3D.getZ());
        }

        // sort by z coord
        Arrays.sort(samples, new Comparator<Color3dSample>() {
            @Override
            public int compare(Color3dSample o1, Color3dSample o2) {
                return (int)(Math.signum(o1.point3D.getZ() - o2.point3D.getZ()));
            }
        });

        // draw the samples
        double[] xPoints = new double[4];
        double[] yPoints = new double[4];
        for (Color3dSample sample : samples){
            if (sample.prevSampleX != null) {
                gc.setStroke(sample.color);
                gc.setLineWidth(sample.lineWidth);
                gc.strokeLine(sample.prevSampleX.point3D.getX(), sample.prevSampleX.point3D.getY(), sample.point3D.getX(), sample.point3D.getY());
            }

            if (sample.prevSampleY != null) {
                gc.setStroke(sample.color);
                gc.setLineWidth(sample.lineWidth);
                gc.strokeLine(sample.prevSampleY.point3D.getX(), sample.prevSampleY.point3D.getY(), sample.point3D.getX(), sample.point3D.getY());
            }

            if (sample.prevSampleX != null && sample.prevSampleY != null && sample.prevSampleXY != null) {
                gc.setFill(new Color(sample.color.getRed(), sample.color.getGreen(), sample.color.getBlue(), 0.66));
                gc.strokeLine(sample.prevSampleY.point3D.getX(), sample.prevSampleY.point3D.getY(), sample.point3D.getX(), sample.point3D.getY());
                xPoints[0] = sample.point3D.getX();
                xPoints[1] = sample.prevSampleX.point3D.getX();
                xPoints[2] = sample.prevSampleXY.point3D.getX();
                xPoints[3] = sample.prevSampleY.point3D.getX();
                yPoints[0] = sample.point3D.getY();
                yPoints[1] = sample.prevSampleX.point3D.getY();
                yPoints[2] = sample.prevSampleXY.point3D.getY();
                yPoints[3] = sample.prevSampleY.point3D.getY();
                gc.fillPolygon(xPoints, yPoints, 4);
            }
        }

        gc.restore();

        return true;
    }
}
