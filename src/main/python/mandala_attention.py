# For conditions of distribution and use, see copyright notice in LICENSE.txt

# Mandala Attention.
# A Transformer-style causal self-attention analog of mandala_rnn.py: same dataset,
# CLI, and results format, but predicts each path step using self-attention over
# the preceding steps instead of an LSTM's recurrent state.
# imports mandala_rnn_dataset.py
# results written to mandala_attention_results.json

import logging, os
logging.disable(logging.WARNING)
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"

# Default parameters.
n_features = 3
n_neurons = '128'
n_heads = 4
n_epochs = 500
results_filename = 'mandala_attention_results.json'
verbose = True

# Prediction significance threshold
threshold = 0.5

# Get options.
import getopt
import sys
usage = 'usage: python mandala_attention.py [--features <number of features> (default=' + str(n_features) + ')] [--neurons <number of neurons> (default=' + n_neurons + ', comma-separated list of feed-forward dimensions per attention block; the first value also sets the model/embedding dimension)] [--heads <number of attention heads> (default=' + str(n_heads) + ')] [--epochs <number of epochs> (default=' + str(n_epochs) + ')] [--results_filename <filename> (default=' + results_filename + ')] [--quiet (quiet)]'
try:
  opts, args = getopt.getopt(sys.argv[1:],"hf:n:a:e:r:q",["help","features=","neurons=","heads=","epochs=","results_filename=","quiet"])
except getopt.GetoptError:
  print(usage, sep='')
  sys.exit(1)
for opt, arg in opts:
  if opt in ("-h", "--help"):
     print(usage, sep='')
     sys.exit(0)
  if opt in ("-f", "--features"):
     n_features = int(arg)
  elif opt in ["-n", "--neurons"]:
     n_neurons = arg
  elif opt in ["-a", "--heads"]:
     n_heads = int(arg)
  elif opt in ["-e", "--epochs"]:
     n_epochs = int(arg)
  elif opt in ["-r", "--results_filename"]:
     results_filename = arg
  elif opt in ["-q", "--quiet"]:
     verbose = False
  else:
     print(usage, sep='')
     sys.exit(1)
if n_features < 1:
    print(usage, sep='')
    sys.exit(1)
n_list = n_neurons.split(",")
if len(n_list) == 0:
    print(usage, sep='')
    sys.exit(1)
n_hidden = []
for i in n_list:
    if i.isnumeric() == False:
        print(usage, sep='')
        sys.exit(1)
    if int(i) < 1:
        print(usage, sep='')
        sys.exit(1)
    n_hidden.append(int(i))
if n_heads < 1:
    print(usage, sep='')
    sys.exit(1)
if n_epochs < 0:
    print(usage, sep='')
    sys.exit(1)

# Load dataset (same tiered/sequential dataset as mandala_rnn.py).
from mandala_rnn_dataset import X_train_shape, y_train_shape, X_train, y_train, X_test_shape, y_test_shape, X_test, y_test, y_test_predictable
if X_train_shape[0] == 0:
    print('Empty train dataset')
    sys.exit(1)
if X_test_shape[0] == 0:
    print('Empty test dataset')
    sys.exit(1)

# Create model: a causal self-attention (Transformer encoder) stack.
# Each path step attends only to itself and prior steps (causal mask), analogous
# to an LSTM's forward-only recurrence, but without the fixed-state bottleneck.
import tensorflow as tf
from tensorflow.keras import layers, models
import warnings
warnings.filterwarnings("ignore", category=UserWarning, message="Do not pass an `input_shape`/`input_dim` argument to a layer.*")

seq_len  = X_train_shape[1]
n_inputs = X_train_shape[2]
d_model  = n_hidden[0]

inputs = layers.Input(shape=(seq_len, n_inputs))
x = layers.Dense(d_model)(inputs)
positions = tf.range(start=0, limit=seq_len, delta=1)
position_embedding = layers.Embedding(input_dim=seq_len, output_dim=d_model)(positions)
x = x + position_embedding

key_dim = max(1, d_model // n_heads)
for ffn_dim in n_hidden:
    attention_out = layers.MultiHeadAttention(num_heads=n_heads, key_dim=key_dim)(x, x, use_causal_mask=True)
    x = layers.LayerNormalization(epsilon=1e-6)(x + attention_out)
    ffn = layers.Dense(ffn_dim, activation='relu')(x)
    ffn = layers.Dense(d_model)(ffn)
    x = layers.LayerNormalization(epsilon=1e-6)(x + ffn)
outputs = layers.Dense(y_train_shape[2])(x)

attention_model = models.Model(inputs=inputs, outputs=outputs)
attention_model.compile(loss='mean_squared_error', optimizer='adam')
if verbose:
    attention_model.summary()

# Train.
from numpy import array, argmax
seq = array(X_train)
X = seq.reshape(X_train_shape[0], X_train_shape[1], X_train_shape[2])
seq = array(y_train)
y = seq.reshape(y_train_shape[0], y_train_shape[1], y_train_shape[2])
attention_model.fit(X, y, epochs=n_epochs, batch_size=X_train_shape[0], verbose=int(verbose))

# Validate.
predictions = attention_model.predict(X, batch_size=X_train_shape[0], verbose=0)
trainErrors = 0
trainTotal = 0
for path in range(X_train_shape[0]):
    for step in range(X_train_shape[1]):
        yvals = y[path][step]
        pvals = predictions[path][step]
        ymax = []
        pmax = []
        for j in range(n_features):
            yidx = argmax(yvals)
            if yvals[yidx] >= threshold:
                ymax.append(yidx)
            yvals[yidx] = 0.0
            pidx = argmax(pvals)
            if pvals[pidx] >= threshold:
                pmax.append(pidx)
            pvals[pidx] = 0.0
        ymax.sort()
        pmax.sort()
        trainTotal += 1
        if ymax != pmax:
            trainErrors += 1
trainErrorPct = 0
if trainTotal > 0:
    trainErrorPct = (float(trainErrors) / float(trainTotal)) * 100.0

# Predict.
seq = array(X_test)
X = seq.reshape(X_test_shape[0], X_test_shape[1], X_test_shape[2])
seq = array(y_test)
y = seq.reshape(y_test_shape[0], y_test_shape[1], y_test_shape[2])
predictions = attention_model.predict(X, batch_size=X_test_shape[0], verbose=0)
testErrors = 0
testTotal = 0
for path in range(X_test_shape[0]):
    for step in range(X_test_shape[1]):
        if step in y_test_predictable[path]:
            yvals = y[path][step]
            pvals = predictions[path][step]
            ymax = []
            pmax = []
            for j in range(n_features):
                yidx = argmax(yvals)
                if yvals[yidx] >= threshold:
                    ymax.append(yidx)
                yvals[yidx] = 0.0
                pidx = argmax(pvals)
                if pvals[pidx] >= threshold:
                    pmax.append(pidx)
                pvals[pidx] = 0.0
            ymax.sort()
            pmax.sort()
            testTotal += 1
            if ymax != pmax:
                testErrors += 1
testErrorPct = 0
if testTotal > 0:
    testErrorPct = (float(testErrors) / float(testTotal)) * 100.0

# Print results.
if verbose:
    print("Train prediction errors/total = ", trainErrors, "/", trainTotal, sep='', end='')
    print(" (", str(round(trainErrorPct, 2)), "%)", sep='', end='')
    print('')
    print("Test prediction errors/total = ", testErrors, "/", testTotal, sep='', end='')
    print(" (", str(round(testErrorPct, 2)), "%)", sep='', end='')
    print('')

# Write results to file.
with open(results_filename, 'w', newline='\n') as f:
    f.write('{')
    f.write('\"train_prediction_errors\":\"'+str(trainErrors)+'\",')
    f.write('\"train_total_predictions\":\"'+str(trainTotal)+'\",')
    f.write('\"train_error_pct\":\"'+str(round(trainErrorPct, 2))+'\",')
    f.write('\"test_prediction_errors\":\"'+str(testErrors)+'\",')
    f.write('\"test_total_predictions\":\"'+str(testTotal)+'\",')
    f.write('\"test_error_pct\":\"'+str(round(testErrorPct, 2))+'\"')
    f.write('}\n')

sys.exit(0)
