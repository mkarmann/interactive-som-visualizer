package utils;

import javafx.geometry.Point3D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
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
public class Som3dCanvasPane extends AnimatedCanvasPane {

    /**
     * Subclass for storing one 3d data point
     */
    private static class Color3dSample {
        public Color3dSample prevSampleX;
        public Color3dSample prevSampleY;
        public Color3dSample prevSampleXY;
        public Color3dSample prevSampleZ;
        public Point3D point3D;
        public Color color;
        public Color connectionsColor = Color.WHITE;
        public double pointWidth = 0.015;
        public double lineWidth;
        public Type type;

        private enum Type {
            POINT,
            LINE,
            RECT,
            CUBE,
        }

        // temp variables
        private static double[] xPoints = new double[4];
        private static double[] yPoints = new double[4];

        public Color3dSample(Point3D point3D, Color color, double pointWidth) {
            this.point3D = point3D;
            this.color = color;
            this.type = Type.POINT;
            this.pointWidth = pointWidth;
        }

        public Color3dSample(Color3dSample prevSample, Point3D point3D, Color color, double lineWidht) {
            this.prevSampleX = prevSample;
            this.point3D = point3D;
            this.color = color;
            this.connectionsColor = color;
            this.lineWidth = lineWidht;
            this.type = Type.LINE;
        }

        public Color3dSample(Color3dSample prevSampleX, Color3dSample prevSampleY, Point3D point3D, Color color, double lineWidth) {
            this.prevSampleX = prevSampleX;
            this.prevSampleY = prevSampleY;
            this.point3D = point3D;
            this.color = color;
            this.lineWidth = lineWidth;
            this.type = Type.RECT;
        }

        public void applDepthTransform(double zoomIn) {
            // apply camera z
            double z_scale = 3. / (3.0 - point3D.getZ() - zoomIn);
            point3D = new Point3D(point3D.getX() * z_scale, point3D.getY() * z_scale, -3.0 + point3D.getZ() + zoomIn);
            lineWidth *= z_scale;
            pointWidth *= z_scale;
        }

        public void render(GraphicsContext gc) {

            if (point3D.getZ() < -0.01) {
                // *******************
                // stroke connections
                // *******************

                if (prevSampleX != null) {
                    gc.setStroke(connectionsColor);
                    gc.setLineWidth(lineWidth);
                    gc.strokeLine(prevSampleX.point3D.getX(), prevSampleX.point3D.getY(), point3D.getX(), point3D.getY());
                }

                if (prevSampleY != null) {
                    gc.setStroke(connectionsColor);
                    gc.setLineWidth(lineWidth);
                    gc.strokeLine(prevSampleY.point3D.getX(), prevSampleY.point3D.getY(), point3D.getX(), point3D.getY());
                }

                if (prevSampleZ != null) {
                    gc.setStroke(connectionsColor);
                    gc.setLineWidth(lineWidth);
                    gc.strokeLine(prevSampleZ.point3D.getX(), prevSampleZ.point3D.getY(), point3D.getX(), point3D.getY());
                }

                // Draw point
                if (this.type == Type.POINT) {
                    gc.setFill(color);
                    gc.fillOval(point3D.getX() - pointWidth * 0.5, point3D.getY() - pointWidth * 0.5, pointWidth, pointWidth);
                }

                // Fill rect
                if (this.type == Type.RECT && prevSampleX != null && prevSampleY != null && prevSampleXY != null) {
                    gc.setFill(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0.75));
                    xPoints[0] = point3D.getX();
                    xPoints[1] = prevSampleX.point3D.getX();
                    xPoints[2] = prevSampleXY.point3D.getX();
                    xPoints[3] = prevSampleY.point3D.getX();
                    yPoints[0] = point3D.getY();
                    yPoints[1] = prevSampleX.point3D.getY();
                    yPoints[2] = prevSampleXY.point3D.getY();
                    yPoints[3] = prevSampleY.point3D.getY();
                    gc.fillPolygon(xPoints, yPoints, 4);
                }
            }
        }
    }

    public SelfOrganizingMap som;           // som to visualize
    public double zoomIn = 0.;              // zoom in
    public double animatedZoomIn = -500.;   // animated zoom in which reaches zoom in after time
    public double rotationX = 0.5;          // camera rotation x
    public double rotationY = -0.4;         // comaera rotation y
    private double lastDragX = 0;           // last cursor drag position x
    private double lastDragY = 0;           // last cursor drag position y
    public double dataPoints[];             // 3d data points for data preview
    public boolean renderSom = true;        // display the som map
    public boolean renderDataPoints = true; // display the data points
    public boolean renderAxis = true;       // display the coordinate system axis.

    public Som3dCanvasPane(SelfOrganizingMap som, double width, double height) {
        this(width, height);
        this.som = som;
    }

    public Som3dCanvasPane(double width, double height) {
        super(width, height);

        // Store drag start positions
        getCanvas().addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            lastDragX = e.getX();
            lastDragY = e.getY();
        });

        // Listen for mouse drag events
        getCanvas().addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            double factor = 1 / Math.min(getWidth(), getHeight()) * 2 * Math.PI;
            rotationY +=(e.getX() - lastDragX) * factor;
            rotationX += (e.getY() - lastDragY) * factor;
            lastDragX = e.getX();
            lastDragY = e.getY();

            rotationX =  Math.min(0.5 * Math.PI, Math.max(-0.5 * Math.PI, rotationX));
        });

        // Listen for mouse scroll
        getCanvas().addEventHandler(ScrollEvent.ANY, e -> {
            zoomIn += 0.005 * e.getDeltaY();
            zoomIn = Math.min(1.5, zoomIn);
        });
    }

    public void updateView() {
        animatedZoomIn = 0.75 * animatedZoomIn + 0.25 * zoomIn;
        if (som != null) {
            int numSamples = som.neuronPerDimension;
            fillCanvas1dto3dGraph(som, getCanvas(), dataPoints, rotationY, rotationX);
        }
    }

    public boolean fillCanvas1dto3dGraph(SelfOrganizingMap som, Canvas canvas, double[] trainingData, double rotationY, double rotationX) {

        int numDataSamples = renderDataPoints && trainingData != null ? trainingData.length / 3 : 0;
        int numSomSamples = 0;
        int numAxisSamples = renderAxis ? 3 : 0;

        if (renderSom) {
            switch (som.dimensions) {
                case 1:
                    numSomSamples = som.neuronPerDimension;
                    break;

                case 2:
                    numSomSamples = som.neuronPerDimension * som.neuronPerDimension;
                    break;

                case 3:
                    numSomSamples = som.neuronPerDimension * som.neuronPerDimension * som.neuronPerDimension;
            }
        }

        double w = canvas.getWidth();
        double h = canvas.getHeight();
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.clearRect(0,0,w,h);

        gc.save();
        double scale = Math.min(w,h) * 0.5 / Math.sqrt(3);
        gc.transform(new Affine(new Translate(w * 0.5, h * 0.5)));
        gc.transform(new Affine(new Scale(scale, scale)));
        Color3dSample samples[] = new Color3dSample[numSomSamples + numAxisSamples + numDataSamples];
        Color3dSample centerSample;
        double[] inputs = new double[som.dimensions];
        double[] outputs = new double[3];
        double delta = 1.0 / (som.neuronPerDimension - 1);


        int samplesIndex = 0;

        // stroke coord system
        if (renderAxis) {
            centerSample = new Color3dSample(null, new Point3D(0, 0, 0), Color.BLACK, 1.0 / scale);
            samples[samplesIndex++] = new Color3dSample(centerSample, new Point3D(1, 0, 0), Color.rgb(255, 0, 0, 0.25), 5.0 / scale);
            samples[samplesIndex++] = new Color3dSample(centerSample, new Point3D(0, -1, 0), Color.rgb(0, 128, 0, 0.25), 5.0 / scale);
            samples[samplesIndex++] = new Color3dSample(centerSample, new Point3D(0, 0, 1), Color.rgb(0, 0, 255, 0.25), 5.0 / scale);
        }


        // ******************
        // Add training data
        // ******************
        if (renderDataPoints && trainingData != null) {
            for (int i=0; i<trainingData.length / 3; i++) {
                samples[samplesIndex++] = new Color3dSample(
                        new Point3D(
                                trainingData[i * 3],
                                -trainingData[i * 3 + 1],
                                trainingData[i * 3 + 2] ),
                        Color.rgb(200,200,200, .33),
                        0.015);
            }
        }


        // *************
        // Add som data
        // *************
        int somDataStartIndex = samplesIndex;
        if (renderSom) {
            if (som.dimensions == 1) {
                // collect 1d network output
                Color3dSample lastSample = null;
                for (int i = 0; i < som.neuronPerDimension; i++) {
                    inputs[0] = i * delta;
                    som.getNeuronWeightsFromGridPosition(inputs, outputs);
                    int r = Math.min(255, Math.max(0, (int) (127.5 * (outputs[0] + 1))));
                    int g = Math.min(255, Math.max(0, (int) (127.5 * (outputs[1] + 1))));
                    int b = Math.min(255, Math.max(0, (int) (127.5 * (outputs[2] + 1))));
                    samples[somDataStartIndex + i] = new Color3dSample(
                            lastSample,
                            new Point3D(outputs[0], -outputs[1], outputs[2]),
                            Color.rgb(r, g, b),
                            3 / scale);
                    lastSample = samples[somDataStartIndex + i];
                }
            } else if (som.dimensions == 2){
                // collect 2d network output
                for (int x = 0; x < som.neuronPerDimension; x++) {
                    for (int y = 0; y < som.neuronPerDimension; y++) {
                        int index = somDataStartIndex + x + y * som.neuronPerDimension;
                        inputs[0] = x * delta;
                        inputs[1] = y * delta;
                        som.getNeuronWeightsFromGridPosition(inputs, outputs);
                        int r = Math.min(255, Math.max(0, (int) (127.5 * (outputs[0] + 1))));
                        int g = Math.min(255, Math.max(0, (int) (127.5 * (outputs[1] + 1))));
                        int b = Math.min(255, Math.max(0, (int) (127.5 * (outputs[2] + 1))));
                        Color3dSample sample = new Color3dSample(
                                null,
                                new Point3D(outputs[0], -outputs[1], outputs[2]),
                                Color.rgb(r, g, b),
                                1 / scale);
                        sample.type = Color3dSample.Type.RECT;
                        sample.connectionsColor = Color.rgb((255 * 3 + r) / 4, (255 * 3 + g) / 4, (b + 255 * 3) / 4);
                        if (x != 0) {
                            sample.prevSampleX = samples[somDataStartIndex + (x - 1) + y * som.neuronPerDimension];
                        }
                        if (y != 0) {
                            sample.prevSampleY = samples[somDataStartIndex + x + (y - 1) * som.neuronPerDimension];
                        }
                        if (x != 0 && y != 0) {
                            sample.prevSampleXY = samples[somDataStartIndex + (x - 1) + (y - 1) * som.neuronPerDimension];
                        }

                        samples[index] = sample;
                    }
                }
            }
            else if (som.dimensions == 3) {
                // collect 3d network output
                for (int x = 0; x < som.neuronPerDimension; x++) {
                    for (int y = 0; y < som.neuronPerDimension; y++) {
                        for (int z = 0; z < som.neuronPerDimension; z++) {
                            int index = somDataStartIndex + x + y * som.neuronPerDimension + z * som.neuronPerDimension * som.neuronPerDimension;
                            inputs[0] = x * delta;
                            inputs[1] = y * delta;
                            inputs[2] = z * delta;
                            som.getNeuronWeightsFromGridPosition(inputs, outputs);
                            int r = Math.min(255, Math.max(0, (int) (127.5 * (outputs[0] + 1))));
                            int g = Math.min(255, Math.max(0, (int) (127.5 * (outputs[1] + 1))));
                            int b = Math.min(255, Math.max(0, (int) (127.5 * (outputs[2] + 1))));
                            Color3dSample sample = new Color3dSample(
                                    null,
                                    new Point3D(outputs[0], -outputs[1], outputs[2]),
                                    Color.rgb(r, g, b),
                                    1 / scale);
                            sample.pointWidth = 0.02;
                            sample.type = Color3dSample.Type.POINT;
                            if (x != 0) {
                                sample.prevSampleX = samples[index - 1];
                            }
                            if (y != 0) {
                                sample.prevSampleY = samples[index - som.neuronPerDimension];
                            }
                            if (z != 0) {
                                sample.prevSampleZ = samples[index - som.neuronPerDimension * som.neuronPerDimension];
                            }

                            samples[index] = sample;
                        }
                    }
                }
            }
        }

        Rotate rotation = new Rotate(Math.toDegrees(rotationY), 0,0,0,new Point3D(0,1,0));
        Rotate rotation2 = new Rotate(Math.toDegrees(-rotationX), 0,0,0,new Point3D(1,0,0));
        for (Color3dSample sample : samples) {
            sample.point3D = rotation2.transform(rotation.transform(sample.point3D));
            sample.applDepthTransform(animatedZoomIn);
        }

        // sort by z coord
        Arrays.sort(samples, new Comparator<Color3dSample>() {
            @Override
            public int compare(Color3dSample o1, Color3dSample o2) {
                return (int)(Math.signum(o1.point3D.getZ() - o2.point3D.getZ()));
            }
        });

        // draw the samples
        for (Color3dSample sample : samples){
            sample.render(gc);
        }

        gc.restore();

        return true;
    }
}
