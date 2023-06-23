# Signal detector.
# Use an autocoder model to detect signals in noise.
# ref: https://blog.keras.io/building-autoencoders-in-keras.html

import keras
from keras import layers
from keras import regularizers
import numpy as np
import random
import sys, getopt

# Signals.
signal_dim = 8
signal_idxs = [ 1, 4 ]

# Signal threshold.
signal_threshold = .5

# Noise probability.
noise_probability = .1

# Dataset size.
dataset_size = 12

# Dimensions of hidden layer.
hidden_dim = 32

# Training epochs.
epochs = 100

# Get options
usage = 'signal_detector.py [-d <signal_dimensions>] [ -s <signal_indexes>] [-t <signal_threshold>] [-p <noise_probability>] [-n <dataset_size>] [-h <hidden_neuron_dimensions>] [-e <epochs>]'
try:
  opts, args = getopt.getopt(sys.argv[1:],"d:s:t:p:n:h:e:?:",["signal_dimensions=","signal_indexes=","signal_threshold=","noise_probability=","dataset_size=","hidden_neuron_dimensions=","epochs=","help="])
except getopt.GetoptError:
  print(usage)
  sys.exit(1)
for opt, arg in opts:
  if opt in ("-?", "--help"):
     print(usage)
     sys.exit(0)
  if opt in ("-d", "--signal_dimensions"):
     signal_dim = int(arg)
  elif opt in ("-s", "--signal_indexes"):
     idxs = arg.split(',')
     signal_idxs = []
     for idx in idxs:
         signal_idxs.append(int(idx))
  elif opt in ("-t", "--signal_threshold"):
     signal_threshold = float(arg)
  elif opt in ("-p", "--noise_probability"):
     noise_probability = float(arg)
  elif opt in ("-n", "--dataset_size"):
     dataset_size = int(arg)
  elif opt in ("-h", "--hidden_neuron_dimensions"):
     hidden_dim = int(arg)
  elif opt in ("-e", "--epochs"):
     epochs = int(arg)
  else:
     print(usage)
     sys.exit(1)

# Input layer.
input_layer = keras.Input(shape=(signal_dim,))

# Hidden layer with an L1 activity regularizer.
hidden_layer = layers.Dense(hidden_dim, activation='relu',
                activity_regularizer=regularizers.l1(10e-5))(input_layer)

# Output layer.
output_layer = layers.Dense(signal_dim, activation='sigmoid')(hidden_layer)

# Signal model maps input to output.
signal_model = keras.Model(input_layer, output_layer)

# Compile model with a mean squared error loss and Adam optimizer.
signal_model.compile(optimizer='adam', loss='mse')

# Generate signal dataset.
# off=0.0, on=1.0
signal_data = np.zeros((dataset_size, signal_dim))
for i in range(dataset_size):
    for j in range(signal_dim):
        if random.random() < noise_probability:
              signal_data[i][j] = 1
    for j in signal_idxs:
        signal_data[i][j] = 1
print(signal_data)

# Train signal model.
signal_model.fit(signal_data, signal_data,
                epochs=epochs,
                batch_size=256,
                shuffle=True)

# Predict outputs from inputs.
print('predictions:')
for i in range(dataset_size):
    print('signal=',i,':',sep='')
    input_signals = np.array([signal_data[i]])
    predicted_signals = signal_model.predict(input_signals)
    signals = []
    for j in range(signal_dim):
        if predicted_signals[0][j] > signal_threshold:
            signals.append(j)
    print('input:', input_signals[0])
    print('prediction:', predicted_signals[0])
    print('signals:', signals)
    
exit(0)

