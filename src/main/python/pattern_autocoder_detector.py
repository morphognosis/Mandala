# Pattern autocoder detector.
# Use an autocoder model to detect patterns in input.
# ref: https://blog.keras.io/building-autoencoders-in-keras.html

import keras
from keras import layers
from keras import regularizers
import numpy as np
import random
import sys, getopt

# Patterns.
pattern_dim = 8
pattern_idxs = [[1, 4], [3]]

# Signal quantizer.
signal_quantizer_min = .5
signal_quantizer_incr = .1

# Noise probability.
noise_probability = .1

# Dataset size.
dataset_size = 20

# Hidden layer dimensions.
hidden_dim = 32

# Training epochs.
epochs = 100

# Random seed.
random_seed = 4517

# Get options
usage = 'pattern_autocoder_detector.py [-d <pattern_dimensions>] [ -i <pattern_indexes> ::= <pattern>;<pattern>;... where <pattern> ::= <index>,<index>,...] [-q <signal_quantizer> ::= <minimum>,<increment>] [-p <noise_probability>] [-n <dataset_size>] [-h <hidden_neuron_dimensions>] [-e <epochs>] [-r <random seed>]'
try:
  opts, args = getopt.getopt(sys.argv[1:],"d:i:q:p:n:h:e:r:?:",["pattern_dimensions=","pattern_indexes=","signal_quantizer=","noise_probability=","dataset_size=","hidden_neuron_dimensions=","epochs=","random_seed","help="])
except getopt.GetoptError:
  print(usage)
  sys.exit(1)
for opt, arg in opts:
  if opt in ("-?", "--help"):
     print(usage)
     sys.exit(0)
  if opt in ("-d", "--pattern_dimensions"):
     pattern_dim = int(arg)
  elif opt in ("-i", "--pattern_indexes"):
     pattern_idxs = []
     patterns = arg.split(';')
     for pattern in patterns:
         pattern_idxs.append(pattern.split(','))
  elif opt in ("-q", "--signal_quantizer"):
     quantizers = arg.split(',')
     if len(quantizers) != 2:
        print('invalid signal_quantizer')
        print(usage)
        sys.exit(1)
     signal_quantizer_min = float(quantizers[0])
     signal_quantizer_incr = float(quantizers[1])
     if signal_quantizer_incr <= 0.0:
         print('signal_quantizer increment must be > 0')
         sys.exit(1)
  elif opt in ("-p", "--noise_probability"):
     noise_probability = float(arg)
  elif opt in ("-n", "--dataset_size"):
     dataset_size = int(arg)
  elif opt in ("-h", "--hidden_neuron_dimensions"):
     hidden_dim = int(arg)
  elif opt in ("-e", "--epochs"):
     epochs = int(arg)
  elif opt in ("-r", "--random_seed"):
     random_seed = int(arg)
  else:
     print(usage)
     sys.exit(1)
     
# Seed random numbers.
random.seed(random_seed)

# Input layer.
input_layer = keras.Input(shape=(pattern_dim,))

# Hidden layer with an L1 activity regularizer.
hidden_layer = layers.Dense(hidden_dim, activation='relu',
                activity_regularizer=regularizers.l1(10e-5))(input_layer)

# Output layer.
output_layer = layers.Dense(pattern_dim, activation='sigmoid')(hidden_layer)

# Pattern model maps input to output.
pattern_model = keras.Model(input_layer, output_layer)

# Compile model with a mean squared error loss and Adam optimizer.
pattern_model.compile(optimizer='adam', loss='mse')

# Generate pattern dataset.
# off=0.0, on=1.0
pattern_data = np.zeros((dataset_size, pattern_dim))
for i in range(dataset_size):
    for j in range(pattern_dim):
        if random.random() < noise_probability:
              pattern_data[i][j] = 1
    if len(pattern_idxs) > 0:
        idx = random.randint(0,len(pattern_idxs)  - 1)
        for j in pattern_idxs[idx]:
            pattern_data[i][int(j)] = 1
#print(pattern_data)

# Train pattern model.
pattern_model.fit(pattern_data, pattern_data,
                epochs=epochs,
                batch_size=256,
                shuffle=True)

# Extract patterns.
print('patterns:')
for i in range(dataset_size):
    print('pattern #',i,':',sep='')
    threshold = signal_quantizer_min
    while threshold <= 1.0:
        print('signal threshold=',threshold,sep='')
        input_pattern = np.array([pattern_data[i]])
        predicted_pattern = pattern_model.predict(input_pattern)
        print('input:', input_pattern[0])
        print('prediction:', predicted_pattern[0])
        print('pattern: [', end='')
        for j in range(pattern_dim):
            if predicted_pattern[0][j] >= threshold:
                print('1', end='')
            else:
                print('0', end='')
            if j < pattern_dim - 1:
                print(', ', end='')
        print(']')
        threshold += signal_quantizer_incr

exit(0)

