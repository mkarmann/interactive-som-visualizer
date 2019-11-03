package utils;

/**
 * This class enables creating n-dimensional Self Organizing Maps
 * with a n-dimensional cube structure.
 */
public class SelfOrganizingMap {
    public final double INFLUENCE_THRESHOLD = 0.001;    // Threshold for the neighbourhood calculation (for performance)
    public final int numNeurons;                        // Number of neurons
    public final int dimensions;                        // Number of dimensions
    public final int neuronPerDimension;                // Number of neurons for each dimension
    public final int inputSize;                         // Input size of one sample
    public final double weights[];                      // Stores weights for all neurons
    public final double neuronGridPositions[];          // Stores n-dimensional grid positions
    public final int neuronGridIndices[];               // Stores n-dimensional grid index

    // variables for calculation
    private final int[] tmpNeuronPositions;
    private final int[] tmpIterators;

    // distance function
    public double phi = 0.2;                            // Variable for the distance function
                                                        // (bigger => stronger influence falloff)

    /**
     * Initialize Self Organizing Map (SOM).
     *
     * @param inputSize The number of parameters each training sample has.
     * @param dimensions The number of dimensions for the neurons gird (inner shape representation).
     * @param neuronPerDimension Number of neurons for each dimension.
     */
    public SelfOrganizingMap(int inputSize, int dimensions, int neuronPerDimension) {
        this.dimensions = dimensions;
        this.neuronPerDimension = neuronPerDimension;
        this.inputSize = inputSize;
        this.numNeurons = getNumNeurons();
        this.weights = new double[numNeurons * inputSize];
        this.neuronGridPositions = new double[numNeurons * dimensions];
        this.neuronGridIndices = new int[numNeurons * dimensions];

        // Calculate and store each grid position
        for (int i=0; i<numNeurons; i++) {
            setupGridPameters(i);
        }

        // initialize weights
        for (int i=0; i<weights.length; i++){
            weights[i] = Math.random() * 0.01;
        }

        // for faster calculations
        tmpNeuronPositions = new int[dimensions];
        tmpIterators = new int[dimensions];
    }

    public SelfOrganizingMap(int inputSize, int dimensions, int neuronPerDimension, SelfOrganizingMap map) {
        this(inputSize, dimensions, neuronPerDimension);

        if (map != null && map.dimensions == dimensions && map.inputSize == inputSize) {

            // using nearest neighbour interpolation with some random shift
            double tmpMapPositions[] = new double[dimensions];
            double randomShift[] = new double[dimensions];
            for (int d=0; d<dimensions; d++) {
                randomShift[d] = Math.random() * 0.5 - 0.25;
            }
            for (int i=0; i<numNeurons; i++) {
                for (int d=0; d<dimensions; d++) {
                    double position = neuronGridPositions[i * dimensions + d];
                    tmpMapPositions[d] = position / (neuronPerDimension) + (0.5 + randomShift[d]) / map.neuronPerDimension;
                }

                // copy position of nearest neighbour in the grid
                int nearestNeighbourIndex = map.getNeuronIndexByGridPosition(tmpMapPositions);
                for (int j=0; j<inputSize; j++) {
                    weights[i * inputSize + j] = map.weights[nearestNeighbourIndex * map.inputSize + j];
                }
            }
        }
    }

    /**
     * Get the number of neurons
     *
     * @return Number of neurons
     */
    private int getNumNeurons() {
        int product = 1;
        for (int i=0; i< dimensions; i++){
            product *= neuronPerDimension;
        }

        return product;
    }

    /**
     * Get the index of the closest neuron to the input sample
     *
     * @param input An input sample (length should be bigger or equal to the inputSize of the som)
     * @return single index of the closest neuron
     */
    public int getClosestNeuronIndex(double input[]) {
        // calculate each distance
        double shortestDistance = Double.MAX_VALUE;
        int winnerIndex = 0;
        for (int n=0; n<numNeurons; n++) {

            // use euclidean
            double distance = 0;
            for (int i = 0; i< inputSize; i++) {
                double delta = input[i] - weights[i + n * inputSize];
                distance += delta * delta;

                if (distance > shortestDistance) {
                    break;
                }
            }

            if (distance < shortestDistance) {
                winnerIndex = n;
                shortestDistance = distance;
            }
        }

        // return the prototype (winner)
        return winnerIndex;
    }

    /**
     * Get the weights of a neuron by its grid position
     *
     * @param gridPosition N-dimensional position in the grid (length should equal to som dimensions)
     * @param outNeuronWeights Output array which the values will be stored in (length should equal to som dimensions)
     */
    public void getNeuronWeightsFromGridPosition(double gridPosition[], double outNeuronWeights[]) {
        int winnerIndex = getNeuronIndexByGridPosition(gridPosition);
        for (int i = 0; i< inputSize; i++) {
            outNeuronWeights[i] = weights[winnerIndex * inputSize + i];
        }
    }

    /**
     * Train the som with a single training sample
     *
     * @param input one input sample (length equal to the som dimensions)
     * @param eta learning rate. It should stay in the range [0.0 ; 1.0] to be stable.
     */
    public void train(double input[], double eta) {
        int winnerNeuron = getClosestNeuronIndex(input);

        // clear temp variables
        for (int i=0; i<dimensions; i++) {
            tmpIterators[i] = 0;
            tmpNeuronPositions[i] = 0;
        }


        // Treat the n-dimensional grid indices as if they were a number and count through them.
        // starting with dimension 0
        while (true) {
            // calculate distance and phi
            double distanceToNeuronSq = 0;
            for (int i = 0; i < dimensions; i++) {
                double delta = tmpIterators[i];
                distanceToNeuronSq += delta * delta;
            }

            double phi = distanceFunction(distanceToNeuronSq);
            boolean influenceToSmall = phi < INFLUENCE_THRESHOLD;

            if (!influenceToSmall) {
                //counter++;
                doNeuronUpdateRecursivelyByDelta(eta, phi, input, tmpNeuronPositions, winnerNeuron, tmpIterators, 0);
            }


            // update the grid indices
            int currentDimension = 0;
            while (true) {
                if (tmpIterators[currentDimension] == neuronPerDimension - 1 | influenceToSmall) {

                    // ensure to only change the first dimension by to small influence
                    influenceToSmall = false;

                    // next dimension
                    tmpIterators[currentDimension] = 0;
                    currentDimension++;

                    if (currentDimension == dimensions) {
                        break;
                    }
                } else {

                    // increment this one
                    tmpIterators[currentDimension]++;

                    // done
                    break;
                }
            }

            // grid indices counter
            if (currentDimension == dimensions) {
                break;
            }
        }
    }

    /**
     * Method for calculating the weight updates in a recursive way. Each call gets one dimension deeper.
     *
     * In the n-dimensional grid, each winning neuron has 2^n neurons with the same distance and therefore the same phi
     * influence. This method removes duplicated phi calculation by applying it directly to all the neurons with the
     * same distance.
     *
     * @param eta Learning rate
     * @param phi Neighbourhood factor
     * @param input Input sample
     * @param position The neurons position
     * @param winnerNeuronIndex Index of the neuron, that is the closest to the input sample
     * @param indexDelta Delta in the indices
     * @param depth Current n-dimension depth in the recursive method
     */
    private void doNeuronUpdateRecursivelyByDelta(double eta, double phi, double[] input, int[] position, int winnerNeuronIndex, int[] indexDelta, int depth) {
        if (depth == dimensions) {

            // end of position
            int neuronIndex = getNeuronIndexByGridIndices(position);

            for (int i = 0; i< inputSize; i++) {
                int weightIndex = neuronIndex * inputSize + i;
                weights[weightIndex] += eta * phi * (input[i] - weights[weightIndex]);
            }
        }
        else {

            // in between
            int winnerPos = neuronGridIndices[winnerNeuronIndex * dimensions + depth];
            int delta = indexDelta[depth];
            int winnPositive = winnerPos + delta;
            int winnNegative = winnerPos - delta;

            if (winnPositive < neuronPerDimension) {
                position[depth] = winnPositive;
                doNeuronUpdateRecursivelyByDelta(eta, phi, input, position, winnerNeuronIndex, indexDelta, depth + 1);
            }

            if (delta != 0 && winnNegative >= 0) {
                position[depth] = winnNegative;
                doNeuronUpdateRecursivelyByDelta(eta, phi, input, position, winnerNeuronIndex, indexDelta, depth + 1);
            }
        }
    }

    /**
     * Setup the grid position and indices for one neuron
     * @param index Index of the neuron
     */
    private void setupGridPameters(int index) {
        int product = 1;
        for (int d=0; d<dimensions; d++){
            int nextProduct = product * neuronPerDimension;
            neuronGridIndices[index * dimensions + d] = ((index / product) % neuronPerDimension);
            neuronGridPositions[index * dimensions + d] = neuronGridIndices[index * dimensions + d];
            product = nextProduct;
        }
    }

    /**
     * Get the neuron index by the n-dimension grid position
     *
     * Important, this method accesses the neurons in the range 0. to 1.
     *
     * @param gridPosition N-dimensional grid position with values from 0. to 1.
     * @return
     */
    public int getNeuronIndexByGridPosition(double[] gridPosition) {
        int index = 0;
        int product = 1;
        for (int d=0; d<dimensions; d++) {
            index += product * Math.min(neuronPerDimension - 1, Math.max(0, (int)(gridPosition[d] * neuronPerDimension)));
            product *= neuronPerDimension;
        }

        return index;
    }

    /**
     * Get the neuron index by the n-dimensional grid indices
     * @param gridIndices N-dimensional grid indices
     * @return
     */
    public int getNeuronIndexByGridIndices(int [] gridIndices) {
        int index = 0;
        int product = 1;
        for (int d=0; d<dimensions; d++) {
            index += product * gridIndices[d];
            product *= neuronPerDimension;
        }

        return index;
    }

    /**
     * The distance function of the som
     *
     * @param distanceSq Squared euclidean distance in the grid
     * @return influence factor for the neuron which has the distanceSq
     */
    public double distanceFunction(double distanceSq) {
        return Math.exp(-distanceSq * phi);
    }
}
