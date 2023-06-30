# Pattern detector.
# Detect patterns in input.
# ref: "How to measure importance of inputs" by Warren S. Sarle, SAS Institute Inc., Cary, NC, USA 
#      ftp://ftp.sas.com/pub/neural/importance.html

import keras
from keras import layers
from keras import regularizers
import numpy as np
import random
import sys, getopt

# Dimensions.
input_dim = 8
output_dim = 4
hidden_dim = 32

# Patterns.
input_idxs = [[1, 4], [3]]
output_idxs = [0, 2]

# Signal quantizer.
signal_quantizer_min = .5
signal_quantizer_incr = .6

# Noise probability.
noise_probability = .05

# Dataset size.
dataset_size = 20

# Training epochs.
epochs = 100

# Random seed.
random_seed = 4517

# Get options
usage = 'pattern_detector.py [-d <input_dimension>,<hidden_dimension>,<output_dimension>] [ -i <input_pattern_indexes> ::= <pattern>;<pattern>;... where <pattern> ::= <index>,<index>,...] [ -o <output_pattern_indexes> ::= <index>,<index>,...] [-q <signal_quantizer> ::= <minimum>,<increment>] [-p <noise_probability>] [-n <dataset_size>] [-e <epochs>] [-r <random seed>]'
try:
  opts, args = getopt.getopt(sys.argv[1:],"d:i:o:q:p:n:e:r:?:",["pattern_dimensions=","input_pattern_indexes=","output_pattern_indexes=","signal_quantizer=","noise_probability=","dataset_size=","epochs=","random_seed","help="])
except getopt.GetoptError:
  print(usage)
  sys.exit(1)
for opt, arg in opts:
  if opt in ("-?", "--help"):
     print(usage)
     sys.exit(0)
  if opt in ("-d", "--pattern_dimensions"):
     dimensions = arg.split(',')
     if len(dimensions) != 3:
        print('invalid pattern_dimensions')
        print(usage)
        sys.exit(1)
     input_dim = int(dimensions[0])
     if input_dim <= 0:
        print('invalid input dimension')
        sys.exit(1)
     hidden_dim = int(dimensions[1])
     if hidden_dim <= 0:
        print('invalid hidden dimension')
        sys.exit(1)
     output_dim = int(dimensions[2])
     if output_dim <= 0:
        print('invalid output dimension')
        sys.exit(1)
  elif opt in ("-i", "--input_pattern_indexes"):
     input_idxs = []
     patterns = arg.split(';')
     for pattern in patterns:
         idxs = pattern.split(',')
         idxs = [int(i) for i in idxs]
         input_idxs.append(idxs)
  elif opt in ("-o", "--output_pattern_indexes"):
     output_idxs = arg.split(',')
     output_idxs = [int(i) for i in output_idxs]
  elif opt in ("-q", "--signal_quantizer"):
     quantizers = arg.split(',')
     if len(quantizers) != 2:
        print('invalid signal_quantizer')
        print(usage)
        sys.exit(1)
     signal_quantizer_min = float(quantizers[0])
     if signal_quantizer_min < 0.0 or signal_quantizer_min > 1.0:
        print('invalid signal_quantizer_min')
        sys.exit(1)
     signal_quantizer_incr = float(quantizers[1])
     if signal_quantizer_incr <= 0.0:
         print('signal_quantizer increment must be > 0')
         sys.exit(1)
  elif opt in ("-p", "--noise_probability"):
     noise_probability = float(arg)
     if noise_probability < 0.0 or noise_probability > 1.0:
        print('invalid noise_probability')
        sys.exit(1)
  elif opt in ("-n", "--dataset_size"):
     dataset_size = int(arg)
     if dataset_size < 0:
        print('invalid dataset_size')
        sys.exit(1)
  elif opt in ("-e", "--epochs"):
     epochs = int(arg)
     if epochs < 0:
        print('invalid epochs')
        sys.exit(1)
  elif opt in ("-r", "--random_seed"):
     random_seed = int(arg)
  else:
     print(usage)
     sys.exit(1)
  if len(input_idxs) != len(output_idxs):
     print('input and output index lengths must be equal')
     print(usage)
     sys.exit(1)

# Seed random numbers.
random.seed(random_seed)

# Input layer.
input_layer = keras.Input(shape=(input_dim,))

# Hidden layer with an L1 activity regularizer.
hidden_layer = layers.Dense(hidden_dim, activation='relu',
                activity_regularizer=regularizers.l1(10e-5))(input_layer)

# Output layer.
output_layer = layers.Dense(output_dim, activation='sigmoid')(hidden_layer)

# Pattern model maps input to output.
pattern_model = keras.Model(input_layer, output_layer)

# Compile model with a mean squared error loss and Adam optimizer.
pattern_model.compile(optimizer='adam', loss='mse')

# Generate pattern dataset.
# off=0.0, on=1.0
input_data = np.zeros((dataset_size, input_dim))
output_data = np.zeros((dataset_size, output_dim))
for i in range(dataset_size):
    for j in range(input_dim):
        if random.random() < noise_probability:
              input_data[i][j] = 1
    idx = random.randint(0,len(input_idxs)  - 1)
    for j in input_idxs[idx]:
        input_data[i][j] = 1
    output_data[i][output_idxs[idx]] = 1
#print(input_data)
#print(output_data)

# Train pattern model.
pattern_model.fit(input_data, output_data,
                epochs=epochs,
                batch_size=256,
                shuffle=True)

# Detect patterns.
print('patterns:')
for i in range(dataset_size):
    print('pattern #',i,':',sep='')
    threshold = signal_quantizer_min
    while threshold <= 1.0:
        print('signal threshold=',threshold,sep='')
        input_pattern = np.array([input_data[i]])
        predicted_pattern = pattern_model.predict(input_pattern)
        print('input:', input_pattern[0])
        print('prediction:', predicted_pattern[0])
        print('pattern: [', end='')
        for j in range(output_dim):
            if predicted_pattern[0][j] >= threshold:
                print('1', end='')
            else:
                print('0', end='')
            if j < output_dim - 1:
                print(', ', end='')
        print(']')
        print('input importance detection:')
        idx = 0
        max = predicted_pattern[0][0]
        for j in range(output_dim):
            if predicted_pattern[0][j] > max:
                idx = j
                max = predicted_pattern[0][j]
        for j in range(input_dim):
            if input_pattern[0][j] == 1:
                print('input idx=',j,sep='')
                input_pattern[0][j] = 0
                predicted_pattern = pattern_model.predict(input_pattern)
                print('input:', input_pattern[0])
                print('prediction:', predicted_pattern[0])
                print('delta=',(max - predicted_pattern[0][idx]),sep='')
                input_pattern[0][j] = 1
        threshold += signal_quantizer_incr

exit(0)

