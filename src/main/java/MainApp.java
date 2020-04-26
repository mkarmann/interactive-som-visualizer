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
import javafx.scene.control.*;
import javafx.scene.image.PixelWriter;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import utils.SelfOrganizingMap;
import utils.Som3dCanvasPane;
import utils.SomWeightsPane;

public class MainApp extends Application {
    public Som3dCanvasPane threeDVisualizer;                    // 3d visualizer
    public volatile SomWeightsPane weightsVisualizer;           // Weights visualizer
    public AnimationTimer animationTimer;                       // Timer for animating the iterations
    public volatile SelfOrganizingMap som;                      // Som instance
    public TrainingThread trainingThread;                       // Thread for training the som in parallel
    public Canvas distanceCanvas;                               // Canvas used to visualize the distance function

    public volatile double eta = 0.01;                          // Learning rate
    public volatile int datasetIndex = 0;                       // Index of the training dataset
    public volatile int numberOfNeurons = 400;                  // Wanted number of neurons for the som
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
     *
     * @param primaryStage
     * @throws Exception
     */
    @Override
    public void start(Stage primaryStage) throws Exception {

        // create start som
        som = new SelfOrganizingMap(3, dimensions, 10);

        // initialize gui
        BorderPane rootPane = new BorderPane();
        rootPane.setCenter(create3dView());
        rootPane.setBottom(createDataInfo());
        rootPane.setTop(createNumericInfo());
        rootPane.setLeft(createControlMenu());
        Scene scene = new Scene(rootPane);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Interactive SOM visualizer");
        primaryStage.setWidth(1005);
        primaryStage.setHeight(750);
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
    public BorderPane create3dView() {
        BorderPane borderPane = new BorderPane();
        borderPane.setPadding(new Insets(10));
        threeDVisualizer = new Som3dCanvasPane(som, 400,400);

        VBox vBox = new VBox();

        CheckBox renderAxis = new CheckBox("Display axis");
        CheckBox renderData = new CheckBox("Display input");
        CheckBox renderSom = new CheckBox("Display map");

        renderAxis.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                threeDVisualizer.renderAxis = newValue;
            }
        });

        renderData.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                threeDVisualizer.renderDataPoints = newValue;
            }
        });

        renderSom.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                threeDVisualizer.renderSom = newValue;
            }
        });

        renderAxis.setSelected(threeDVisualizer.renderAxis);
        renderData.setSelected(threeDVisualizer.renderDataPoints);
        renderSom.setSelected(threeDVisualizer.renderSom);

        vBox.getChildren().addAll(renderAxis, renderData, renderSom);
        borderPane.setRight(vBox);
        borderPane.setCenter(threeDVisualizer);

        return borderPane;
    }

    /**
     * Creates the iteration information view
     *
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
     *
     * @return Data information node.
     */
    public Node createDataInfo() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(2);
        gridPane.setVgap(2);
        gridPane.setPadding(new Insets(10));
        int rowIndex = 0;

        GridPane canvasGrid = new GridPane();
        gridPane.addRow(rowIndex++, new Label("Neuron weights (each neuron/pixel has rgb weights):"));

        // canvas
        weightsVisualizer = new SomWeightsPane(som, 10,10);
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
        Insets basicInset = new Insets(0,0,15,0);

        // input combobox
        Label inputLabel = new Label("Input data: ");
        ObservableList<String> datasetOptions =
                FXCollections.observableArrayList(
                        "Full Space",
                        "Ball (volume)",
                        "Sphere (surface)",
                        "Peanut volume",
                        "1 area distribution",
                        "2 area distribution",
                        "3 area distribution",
                        "Plane",
                        "Mandelbrot set",
                        "Mandelbrot outline"
                );
        final ComboBox inputCombobox = new ComboBox(datasetOptions);
        inputCombobox.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue ov, String t, String t1) {
                datasetIndex = datasetOptions.indexOf(t1);

                // reset data distplay
                int n = 10000;
                threeDVisualizer.dataPoints = new double[n * 3];
                double datap[] = new double[3];
                for (int i=0; i<threeDVisualizer.dataPoints.length / 3; i++) {
                    fillInputData(datap);
                    threeDVisualizer.dataPoints[i * 3] = datap[0];
                    threeDVisualizer.dataPoints[i * 3 + 1] = datap[1];
                    threeDVisualizer.dataPoints[i * 3 + 2] = datap[2];
                }
            }
        });
        inputCombobox.setValue(datasetOptions.get(0));

        // eta slider
        Label etaLabel = new Label("Learning rate (eta: )");
        Slider etaSlider = new Slider(0, 3, 2);
        etaSlider.setPadding(basicInset);
        etaSlider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {
                eta = Math.pow(10, new_val.doubleValue() - etaSlider.getMax());
                etaLabel.setText("Learning rate ( eta: " + String.format("%.3f", eta) + " )");
            }});
        etaSlider.setValue(Math.log10(eta) + etaSlider.getMax());

        // phi slider
        Label phiLabel = new Label("distance function (phi: )");
        distanceCanvas = new Canvas(250,100);
        Slider phiSlider = new Slider(0.5, 10, 1);
        phiSlider.setPadding(basicInset);
        phiSlider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {
                double inv = 1 / new_val.doubleValue();
                MainApp.this.phi = inv * inv;
                phiLabel.setText("distance function ( phi: " + String.format("%.3f", MainApp.this.phi) + " )");
                som.phi = MainApp.this.phi;
                updateDistanceFunctionCanvas();
            }});
        phiSlider.setValue(2);

        // num neurons slider
        Label numNeuronsLabel = new Label("neurons: ");
        Slider neuronsSlider = new Slider(1000., 4000, numberOfNeurons - 1);
        neuronsSlider.setPadding(basicInset);
        neuronsSlider.valueProperty().addListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                                Number old_val, Number new_val) {
                numberOfNeurons = (int)Math.round(Math.pow(10.0, new_val.doubleValue() / 1000.));
                updateNumNeruonsLabel(numNeuronsLabel);
                resetSom(true);
            }});
        neuronsSlider.setValue(Math.log10(numberOfNeurons) * 1000);

        // dimension radio buttons
        Label dimensionLabel = new Label("Neuron connections " + dimensions + " dimensional:");
        final ToggleGroup dimensionsGroup = new ToggleGroup();
        GridPane dimensionsPane = new GridPane();

        RadioButton rb1 = new RadioButton("1d         ");
        rb1.setUserData(1);
        rb1.setToggleGroup(dimensionsGroup);
        rb1.setSelected(true);

        RadioButton rb2 = new RadioButton("2d         ");
        rb2.setUserData(2);
        rb2.setSelected(true);
        rb2.setToggleGroup(dimensionsGroup);

        RadioButton rb3 = new RadioButton("3d");
        rb3.setUserData(3);
        rb3.setToggleGroup(dimensionsGroup);
        dimensionsPane.addRow(0, rb1, rb2, rb3);
        dimensionsPane.setPadding(basicInset);

        dimensionsGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>(){
            public void changed(ObservableValue<? extends Toggle> ov,
                                Toggle old_toggle, Toggle new_toggle) {
                dimensions = (int)new_toggle.getUserData();
                dimensionLabel.setText("Neruon connections " + dimensions + " dimensional:");
                updateNumNeruonsLabel(numNeuronsLabel);
                resetSom(false);
            }
        });

        // reset button
        Button resetButton = new Button("Reset neurons");
        resetButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override public void handle(ActionEvent e) {
                resetSom(false);
            }
        });

        gridPane.addRow(rowIndex++, inputLabel);
        gridPane.addRow(rowIndex++, inputCombobox);
        gridPane.addRow(rowIndex++, new Label(""));
        gridPane.addRow(rowIndex++, dimensionLabel);
        gridPane.addRow(rowIndex++, dimensionsPane);
        gridPane.addRow(rowIndex++, etaLabel);
        gridPane.addRow(rowIndex++, etaSlider);
        gridPane.addRow(rowIndex++, phiLabel);
        gridPane.addRow(rowIndex++, distanceCanvas);
        gridPane.addRow(rowIndex++, phiSlider);
        gridPane.addRow(rowIndex++, numNeuronsLabel);
        gridPane.addRow(rowIndex++, neuronsSlider);
        gridPane.addRow(rowIndex++, resetButton);

        return gridPane;
    }

    private void updateNumNeruonsLabel(Label numNeuronsLabel) {
        if (dimensions == 1) {
            numNeuronsLabel.setText("Number of neurons: " + numberOfNeurons);
        }
        else if (dimensions == 2){
            int neuronsPerDimension = (int)Math.round(Math.sqrt(numberOfNeurons));
            numNeuronsLabel.setText("Number of neurons: " + neuronsPerDimension * neuronsPerDimension + " ( " + neuronsPerDimension + "x" + neuronsPerDimension + " )");
        }
        else {
            int neuronsPerDimension = (int)Math.round(Math.pow(numberOfNeurons, 1. / 3.));
            numNeuronsLabel.setText("Number of neurons: " + neuronsPerDimension * neuronsPerDimension * neuronsPerDimension + " ( " + neuronsPerDimension + "x" + neuronsPerDimension + "x" + neuronsPerDimension + " )");
        }
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
    public void resetSom(boolean tryKeepProgress) {
        int neuronPerDim = 0;
        if (dimensions == 1) {
            neuronPerDim = numberOfNeurons;
        }
        else if (dimensions == 2){
            neuronPerDim = (int)Math.round(Math.sqrt(numberOfNeurons));
        }
        else {
            neuronPerDim = (int)Math.round(Math.pow(numberOfNeurons, 1. / 3.));
        }

        if (tryKeepProgress) {
            if (som.dimensions != dimensions || som.neuronPerDimension != neuronPerDim)
            som = new SelfOrganizingMap(3, dimensions, neuronPerDim, som);
        }
        else {
            som = new SelfOrganizingMap(3, dimensions, neuronPerDim);
            iteration = 0;
        }
        som.phi = phi;
        threeDVisualizer.som = som;
        weightsVisualizer.setSom(som);
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
                // sphere volume
                do {
                    distanceSq = 0;
                    for (int i = 0; i < input.length; i++) {
                        input[i] = Math.random() * 2 - 1;
                        distanceSq += input[i] * input[i];
                    }
                } while (distanceSq > 1);
                break;

            case 2:
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

            case 3:
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

            case 4:
            case 5:
            case 6:
                // n-point density
                int index = (int)(Math.random() * (datasetIndex - 3));

                switch (index) {
                    case 0:
                        setDistributionPoint(input, 0, 0, 0);
                        break;
                    case 1:
                        setDistributionPoint(input, 0.5, Math.random() * 0.1 + 0.1, Math.random() * 0.2 + 0.2);
                        break;

                    default:
                        double f = Math.random();
                        double sX = Math.sin(f) * 0.3;
                        double sY = Math.cos(f) * 0.5;
                        setDistributionPoint(input, -0.5 + sX, 1. - sY, -0.8);
                }
                break;

            case 7:
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

            case 8:
                // mandelbrot set
                while (true) {
                    double ci = Math.random() * 2 - 1.;
                    double cr = Math.random() * 2 - 1.5;
                    double z0 = Math.random() * 2 - 1.;
                    if (checkInMandelbrotSet(ci, cr, 0)) {
                        input[0] = cr + 0.5;
                        input[1] = ci;
                        input[2] = 0;
                        break;
                    }
                }
                break;

            case 9:
                // mandelbrot outline
                while (true) {
                    double ci = Math.random() * 2 - 1.;
                    double cr = Math.random() * 2 - 1.5;
                    double z0 = Math.random() * 2 - 1.;
                    if (checkInMandelbrotOutline(ci, cr, 0)) {
                        input[0] = cr + 0.5;
                        input[1] = ci;
                        input[2] = 0;
                        break;
                    }
                }
                break;

        }
    }

    public void setDistributionPoint(double input[], double x, double y, double z) {
        while (true) {
            double dx = Math.random() * 4. - 2.;
            double dy = Math.random() * 4. - 2.;
            double dz = Math.random() * 4. - 2.;

            double dSq = dx * dx + dy * dy + dz * dz;
            dSq = dSq * dSq;
            dSq = dSq * dSq;
            dSq = dSq * dSq;
            dSq = dSq * dSq;
            dSq = (dSq * dSq + 1.0) * 0.1;

            dx *= dSq;
            dy *= dSq;
            dz *= dSq;

            dx += x;
            dy += y;
            dz += z;

            if (
                    dx >= -1 && dx <= 1. &&
                    dy >= -1 && dy <= 1. &&
                    dz >= -1 && dz <= 1.) {
                input[0] = dx;
                input[1] = dy;
                input[2] = dz;
                break;
            }
        }
    }

    // Mandelbrot set implementation based on
    // https://www.hameister.org/projects_fractal.html
    public boolean checkInMandelbrotSet(double ci, double c, double z0) {
        double zi = 0;
        double z = z0;
        for (int i = 0; i < 50; i++) {
            double ziT = 2 * (z * zi);
            double zT = z * z - (zi * zi);
            z = zT + c;
            zi = ziT + ci;
            if (z * z + zi * zi >= 4.0) {
                return false;
            }
        }
        return true;
    }

    public boolean checkInMandelbrotOutline(double ci, double c, double z0) {
        double zi = 0;
        double z = z0;
        for (int i = 0; i < 50; i++) {
            double ziT = 2 * (z * zi);
            double zT = z * z - (zi * zi);
            z = zT + c;
            zi = ziT + ci;
            if (z * z + zi * zi >= 4.0) {
                return i > 10;
            }
        }
        return false;
    }


}
