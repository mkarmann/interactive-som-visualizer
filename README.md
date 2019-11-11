# Interactive som visualizer
![som map iamge](_images/ball_volume.png)

This example application visualizes a Self Organizing Map (SOM) training process in 3d and lets you modify its parameters during training.

- [More about this vsiualizer](#installation)
- [Installation](#Installation)
- [User interface](#getting-started)
- [Parameter overview](#communication)
    - [Input](#the-team)
    - [Map dimensions](#the-team)
    - [Learning rate](#releases-and-contributing)
    - [Neighbourhood](#the-team)
    - [Neighbourhood](#the-team)

## About
A self organizing map is a type of artificial neural network which is used for dimensional reduction. The way it does that is by having a fixed map structure (like a 2d map of 10 by 10 neurons) and fitting it into the input distribution.

For example: If we take the 3 dimensional color space (rgb) and map it to a 10 by 10 map it looks something like this (each pixel is one neuron):

![som map iamge](_images/colorspace_to_2d_map.png)

In 3d the input space looks just like a cube in rgb space:

![som map iamge](_images/colorspace_input.png)

Now we can visualize the 2d map in 3d by using the rgb information of each neuron (each vertex is one neuron):

![som map iamge](_images/colorspace_with_2d_map.png)

The entire application is written in pure Java 1.8. There are no additional libraries required. The training process of the som runs in a second thread. This way the visual updates do not slow down the training. Especially the 2d som takes a lot of time to visualize with big neuron counts.

## Installation
Clone the repository with a simple clone command:
```
git clone https://github.com/mkarmann/interactive-som-visualizer
```

Then open it in any Java editor of your choice or run it directly from the console.
The main class is in [src/main/java/MainApp.java](src/main/java/MainApp.java)

## User interface
![som map iamge](_images/gui.png)

## Parameters
Self organizing maps are very stable, so feel free to just play around with each slider!

To get more into details. Here is a detailed description of all the parameters:

### Input
In this dropbox you can select a distribution you want the som to learn.
| input                | description                                                       |
|----------------------|-------------------------------------------------------------------|
| Full Space           | Full rgb color space                                              |
| Ball (Volume)        | The volume of a ball                                              |
| Sphere (Surface)     | The surface of a ball                                             |
| Peanut volume        | The volume of 2 intersecting balls (same density in intersection) |
| 1 area distribution  | One dense point in the center                                     |
| 2 areas distribution | Two dense points                                                  |
| 3 areas distribution | Two dense points with the last one streched                       |
| Plane                | A simple plane along the x and y axis                             |
| Mandelbrot set       | The 2d mandelbrot set along the x and y axis                      |
| Mandelbrot outline   | Outline of the 2d mandelbrot set                                  |

### Neuron connections
Here you can select to which internal structure the som should have.
If you want a dimensional reduction from 3d to 2d, then simply select 2d. If you want 3d to 1d ... you get the idea.

### Learning rate
The learningrate determines how big the changes of one iteration should be applied to the map.
Big steps help to find a good starting map (used at the beginning).
Small steps help to find a fine and smooth map (used at the end).

### Distance function
If one neuron gets updated, how much should its neighbours follow?
The more you slide the slider to the right, the more neurons will follow each other and therefore stay in the internal shape.
Is the slider to the left, each neuron can move freely without influencing the other ones.

### Number of neurons
More neurons means more information means more detailed representation.
If you select a small number of neurons, the computation time will be much faster and the shape is easier to hold.
If you select a large number of neurons, the computation time will slow down and the map gets more detailed and complex.
