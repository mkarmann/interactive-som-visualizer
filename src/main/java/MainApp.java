import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.PixelWriter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import utils.CanvasPane;
import utils.SelfOrganizingMap;
import utils.Som3dCanvasVisualizer;
import utils.SomWeightsVisualizer;

public class MainApp extends Application {
    public Som3dCanvasVisualizer threeDVisualizer;              // 3d visualizer
    public volatile SomWeightsVisualizer weightsVisualizer;     // Weights visualizer
    public AnimationTimer animationTimer;                       // Timer for animating the iterations
    public volatile SelfOrganizingMap som;                      // Som instance
    public TrainingThread trainingThread;                       // Thread for training the som in parallel
    public Canvas distanceCanvas;                               // Canvas used to visualize the distance function

    public volatile double eta = 0.02;                          // Learning rate
    public volatile int datasetIndex = 0;                       // Index of the training dataset
    public volatile int neuronsPerDimension = 25;               // Number of som neurons per dimension
    public volatile double phi = 0.5;                           // Neighbourhood function variable
    public volatile int dimensions = 2;                         // Number of som dimensions
    public volatile long iteration = 0;                         // Current som training iteration
    public Text iterationInfo;                                  // Label for the iteration information

    /**
     * Class for the training thread. It takes the som of the app and its input generation function to
     * train the som.
     */
    public static class TrainingThread extends Thread {
        private volatile boolean stop = false;
        public final MainApp app;

        public TrainingThread(MainApp app) {
            this.app = app;
        }

        @Override
        public void run() {
            super.run();
            double inputs[] = new double[app.som.inputSize];
            System.out.println("Start training Som");
            while (!stop) {
                app.fillInputData(inputs);
                app.som.train(inputs, app.eta);
                app.iteration++;
            }

            System.out.println("Stop training Som");
        }

        public void finishTraining() {
            this.stop = true;
        }
    }

    /**
     * Main method of the application
     * @param primaryStage
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {

        // create start som
        som = new SelfOrganizingMap(3, dimensions, neuronsPerDimension);

        // initialize gui
        BorderPane rootPane = new BorderPane();
        rootPane.setCenter(create3dView());
        rootPane.setBottom(createDataInfo());
        rootPane.setTop(createNumericInfo());
        rootPane.setLeft(createControlMenu());
        Scene scene = new Scene(rootPane);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Interactive SOM visualizer");
        primaryStage.setWidth(1000);
        primaryStage.setHeight(700);
        primaryStage.setOnCloseRequest(event -> {
            stopTraining();
            stopAnimation();
        });

        primaryStage.show();

        // start the training and visualization
        startTraining();
        startAnimation();
    }

    /**
     * Creates the 3d view
     *
     * @return 3d view node
     */
    public CanvasPane create3dView() {
        threeDVisualizer = new Som3dCanvasVisualizer(som, 400,400);
        return threeDVisualizer;
    }

    /**
     * Creates the iteration information view
     * @return iteration information node.
     */
    public Node createNumericInfo() {
        // text
        iterationInfo = new Text("Iteration: ");
        HBox hBox=new HBox();
        hBox.setPadding(new Insets(10));
        hBox.setAlignment(Pos.BASELINE_RIGHT);
        hBox.getChildren().add(iterationInfo);

        return hBox;
    }

    /**
     * Creates the weights visualizer
     * @return Data information node.
     */
    public Node createDataInfo() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(2);
        gridPane.setVgap(2);
        gridPane.setPadding(new Insets(10));
        int rowIndex = 0;

        GridPane canvasGrid = new GridPane();
        gridPane.addRow(rowIndex++, new Label("Neuron values (in color):"));

        // canvas
        weightsVisualizer = new SomWeightsVisualizer(som, 10,10);
        GridPane.setHgrow(weightsVisualizer, Priority.ALWAYS);
        GridPane.setHgrow(canvasGrid, Priority.ALWAYS);
        canvasGrid.addRow(0, weightsVisualizer);
        gridPane.addRow(rowIndex++, canvasGrid);

        return gridPane;
    }

    /**
     * Create the menu for controlling the som parameters.
     *
     * @return The menu node.
     */
    public Node createControlMenu() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(2);
        gridPane.setVgap(2);
        gridPane.setPadding(new Insets(10));
        int rowIndex = 0;
        Insets basicInset = new Insets(0,0,10,0);

        // input combobox
        ObservableList<String> options =
                FXCollections.observableArrayList(
                        "Full Space",
                        "Color space",
                        "Sphere volume",
                        "Sphere surface",
                        "Peanut volume",
                        "Plane"
                );
        final ComboBox comboBox = new ComboBox(options);
        comboBox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue ov, String t, String t1) {
                datasetIndex = options.indexOf(t1);
            }
        });
        comboBox.setValue(options.get(0));
        GridPane inputGrid = new GridPane();
        inputGrid.addRow(0, new Label("Input: "), comboBox);
        gridPane.addRow(rowIndex++, inputGrid);
        gridPane.addRow(rowIndex++, new Text(""));

        // eta slider
        Label etaLabel = new Label("Learning rate (eta: )");
        gridPane.addRow(rowIndex++, etaLabel);
        Slider etaSlider = new Slider(0, 3, 2);
        etaSlider.setPadding(basicInset);
        etaSlider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {
                eta = Math.pow(10, new_val.doubleValue() - etaSlider.getMax());
                etaLabel.setText("Learning rate ( eta: " + String.format("%.3f", eta) + " )");
            }});
        etaSlider.setValue(Math.log10(eta) + etaSlider.getMax());
        gridPane.addRow(rowIndex++, etaSlider);

        // phi slider
        Label psiLabel = new Label("distance function (phi: )");
        gridPane.addRow(rowIndex++, psiLabel);
        distanceCanvas = new Canvas(200,100);
        gridPane.addRow(rowIndex++, distanceCanvas);
        Slider psiSlider = new Slider(0.5, 6, 1);
        psiSlider.setPadding(basicInset);
        psiSlider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {
                double inv = 1 / new_val.doubleValue();
                phi = inv * inv;
                psiLabel.setText("distance function ( phi: " + String.format("%.3f", phi) + " )");
                som.phi = phi;
                updateDistanceFunctionCanvas();
            }});
        psiSlider.setValue(2);
        gridPane.addRow(rowIndex++, psiSlider);
        gridPane.addRow(rowIndex++, new Text(""));

        // num neurons slider
        Label neuronDensityLabel = new Label("neurons: ");
        Slider neuronsSlider = new Slider(2, 128, neuronsPerDimension - 1);
        neuronsSlider.setPadding(basicInset);
        neuronsSlider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {
                neuronsPerDimension = new_val.intValue();
                if (dimensions == 1) {
                    neuronDensityLabel.setText("Number of neurons: " + neuronsPerDimension * neuronsPerDimension + " ( = " + neuronsPerDimension + " * " + neuronsPerDimension + " )");
                }
                else {
                    neuronDensityLabel.setText("Number of neurons: " + neuronsPerDimension * neuronsPerDimension + " ( " + neuronsPerDimension + "x" + neuronsPerDimension + " )");
                }
                resetSom();
            }});
        neuronsSlider.setValue(neuronsPerDimension);

        // dimension slider
        Label dimensionLabel = new Label("Neuron connections " + dimensions + " dimensional:");
        Slider dimensionSlider = new Slider(1, 2, dimensions);
        dimensionSlider.setPadding(basicInset);
        dimensionSlider.setSnapToTicks(true);
        dimensionSlider.setMajorTickUnit(1);
        dimensionSlider.setMinorTickCount(1);
        dimensionSlider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {
                if (old_val.intValue() != new_val.intValue()) {
                    dimensions = new_val.intValue();
                    dimensionLabel.setText("Neruon connections " + dimensions + " dimensional:");
                    if (dimensions == 1) {
                        neuronDensityLabel.setText("Number of neurons: " + neuronsPerDimension * neuronsPerDimension + " ( = " + neuronsPerDimension + " * " + neuronsPerDimension + " )");
                    }
                    else {
                        neuronDensityLabel.setText("Number of neurons: " + neuronsPerDimension * neuronsPerDimension + " ( " + neuronsPerDimension + "x" + neuronsPerDimension + " )");
                    }
                    resetSom();
                }
            }});

        // reset button
        Button resetButton = new Button("Reset neurons");
        resetButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                resetSom();
            }
        });

        gridPane.addRow(rowIndex++, dimensionLabel);
        gridPane.addRow(rowIndex++, dimensionSlider);
        gridPane.addRow(rowIndex++, neuronDensityLabel);
        gridPane.addRow(rowIndex++, neuronsSlider);
        gridPane.addRow(rowIndex++, resetButton);

        return gridPane;
    }

    /**
     * Update the distance function visualization canvas.
     */
    public void updateDistanceFunctionCanvas() {
        int width = (int)distanceCanvas.getWidth();
        int height = (int)distanceCanvas.getHeight();
        int scaleFactor = 6;
        PixelWriter pixelWriter = distanceCanvas.getGraphicsContext2D().getPixelWriter();

        for (int x=0; x<width; x++) {
            double influence = som.distanceFunction(x / scaleFactor);
            int black = height - (int)(influence * height);
            for (int y=0; y<height; y++) {
                if (y < black) {
                    if (y + 1 < black) {
                        pixelWriter.setColor(x, y, Color.BLACK);
                    }
                    else {
                        pixelWriter.setColor(x, y, Color.WHITE);
                    }
                }
                else {
                    if (x % scaleFactor == scaleFactor - 1) {
                        pixelWriter.setColor(x, y, Color.DARKGREY);
                    }
                    else {
                        pixelWriter.setColor(x, y, Color.GRAY);
                    }
                }
            }
        }
    }

    /**
     * Reset the som
     */
    public void resetSom() {
        int neuronPerDim = neuronsPerDimension;
        if (dimensions == 1) {
            neuronPerDim *= neuronsPerDimension;
        }

        som = new SelfOrganizingMap(3, dimensions, neuronPerDim);
        som.phi = phi;
        threeDVisualizer.som = som;
        weightsVisualizer.setSom(som);
        iteration = 0;
    }

    /**
     * Start this apps animation
     */
    public void startAnimation() {
        animationTimer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                updateVisuals(now);
            }
        };

        animationTimer.start();
    }

    /**
     * Stop this apps animation
     */
    public void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    /**
     * Start the training thread
     */
    public void startTraining() {
        trainingThread = new TrainingThread(this);
        trainingThread.start();
    }

    /**
     * Stop the training thread
     */
    public void stopTraining() {
        if (trainingThread != null) {
            trainingThread.finishTraining();
            trainingThread = null;
        }
    }

    /**
     * Update this apps visuals.
     *
     * The 3d and weights visualizer do this on their own.
     *
     * @param now
     */
    public void updateVisuals(long now) {
        iterationInfo.setText("Iteration: " + iteration);
    }

    /**
     * This method generates the input data for the som training thread.
     *
     * @param input Input array which will be set to the new input values.
     */
    public void fillInputData(double[] input) {
        double distanceSq = 0;
        double distance = 0;
        switch (datasetIndex) {
            case 0:
                // full axis
                for (int i=0; i<input.length; i++) {
                    input[i] = Math.random() * 2 - 1;
                }
                break;

            case 1:
                // color space
                for (int i=0; i<input.length; i++) {
                    input[i] = Math.random();
                }
                break;

            case 2:
                // sphere volume
                do {
                    distanceSq = 0;
                    for (int i = 0; i < input.length; i++) {
                        input[i] = Math.random() * 2 - 1;
                        distanceSq += input[i] * input[i];
                    }
                } while (distanceSq > 1);
                break;

            case 3:
                // sphere surface
                do {
                    distanceSq = 0;
                    for (int i = 0; i < input.length; i++) {
                        input[i] = Math.random() * 2 - 1;
                        distanceSq += input[i] * input[i];
                    }
                } while (distanceSq > 1 && distanceSq <= 0.000001);

                distance = Math.sqrt(distanceSq);
                for (int i=0; i<input.length; i++) {
                    input[i] /= distance;
                }

                break;

            case 4:
                // peanut
            {
                double spheresRadius = 0.6;
                spheresRadius *= spheresRadius;
                double distanceSq2 = 0;
                do {
                    distanceSq = 0;
                    distanceSq2 = 0;
                    for (int i = 0; i < input.length; i++) {
                        input[i] = Math.random() * 2 - 1;
                        distanceSq += (input[i] + 0.25) * (input[i] + 0.25);
                        distanceSq2 += (input[i] - 0.25) * (input[i] - 0.25);
                    }
                } while (distanceSq > spheresRadius && distanceSq2 > spheresRadius);
            }
            break;


            case 5:
                // plane
                for (int i = 0; i < input.length; i++) {
                    if (i < 2) {
                        input[i] = Math.random() * 2 - 1;
                    }
                    else {
                        input[i] = 0;
                    }
                }
                break;

        }
    }
}
