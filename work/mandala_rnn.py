# For conditions of distribution and use, see copyright notice in LICENSE.txt

# Mandala RNN.
# imports mandala_rnn_dataset.py
# results written to mandala_rnn_results.json

import logging, os
logging.disable(logging.WARNING)
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"

# Default parameters.
n_features = 1
n_neurons = '128'
n_epochs = 500
results_filename = 'mandala_rnn_results.json'
verbose = True
activation_threshold = '0.5'

# Get options.
import getopt
import sys
usage = 'usage: python mandala_rnn.py [--features <number of features> (default=' + str(n_features) + ')] [--activation_threshold <threshold> (default=' + activation_threshold + ', comma-separated list)] [--neurons <number of neurons> (default=' + n_neurons + ', comma-separated list of neurons per layer)] [--epochs <number of epochs> (default=' + str(n_epochs) + ')] [--results_filename <filename> (default=' + results_filename + ')] [--quiet (quiet)]'
try:
  opts, args = getopt.getopt(sys.argv[1:],"hf:t:n:e:r:q",["help","features=","activation_threshold=","neurons=","epochs=","results_filename=","quiet"])
except getopt.GetoptError:
  print(usage, sep='')
  sys.exit(1)
for opt, arg in opts:
  if opt in ("-h", "--help"):
     print(usage, sep='')
     sys.exit(0)
  if opt in ("-f", "--features"):
     n_features = int(arg)
  elif opt in ("-t", "--activation_threshold"):
     activation_threshold = arg
  elif opt in ["-n", "--neurons"]:
     n_neurons = arg
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
thresholds = []
threshold_list = activation_threshold.split(",")
if len(threshold_list) == 0:
    print(usage, sep='')
    sys.exit(1)
for i in threshold_list:
    try:
        float(i)
    except ValueError:
        print(usage, sep='')
        sys.exit(1)
    if float(i) < 0.0:
        print(usage, sep='')
        sys.exit(1)
    thresholds.append(float(i))
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
if n_epochs < 0:
    print(usage, sep='')
    sys.exit(1)

# Load dataset.
from mandala_rnn_dataset import X_train_shape, y_train_shape, X_train, y_train, X_test_shape, y_test_shape, X_test, y_test, y_test_predictable
if X_train_shape[0] == 0:
    print('Empty train dataset')
    sys.exit(1)
if X_test_shape[0] == 0:
    print('Empty test dataset')
    sys.exit(1)

# Create model.
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense
import warnings
warnings.filterwarnings("ignore", category=UserWarning, message="Do not pass an `input_shape`/`input_dim` argument to a layer.*")
rnn_model=Sequential()
rnn_model.add(LSTM(input_shape=(X_train_shape[1], X_train_shape[2]), units=n_hidden[0], return_sequences=True))
for i in range(1, len(n_hidden)):
    rnn_model.add(LSTM(units=n_hidden[i], return_sequences=True))
rnn_model.add(Dense(y_train_shape[2]))
rnn_model.compile(loss='mean_squared_error',optimizer='adam')
if verbose:
    rnn_model.summary()

# Train.
from numpy import array
seq = array(X_train)
X_train_seq = seq.reshape(X_train_shape[0], X_train_shape[1], X_train_shape[2])
seq = array(y_train)
y_train_seq = seq.reshape(y_train_shape[0], y_train_shape[1], y_train_shape[2])
rnn_model.fit(X_train_seq, y_train_seq, epochs=n_epochs, batch_size=X_train_shape[0], verbose=int(verbose))

# Sensor features match?
def sensor_match(yvals, pvals, dimensions):
    ymax = []
    for i in range(dimensions):
        if yvals[i] >= threshold:
            ymax.append(i)
    if len(ymax) != n_features:
        return False
    ymax.sort()
    pmax = []
    for i in range(dimensions):
        if pvals[i] >= threshold:
            pmax.append(i)
    if len(pmax) != n_features:
        return False
    pmax.sort()
    if ymax != pmax:
        return False
    else:
        return True

# Evaluate each activation threshold.
best_threshold = -1.0
best_trainErrors = -1
best_trainTotal = -1
best_trainErrorPct = -1.0
best_testErrors = -1
best_testTotal = -1
best_testErrorPct = -1.0
for threshold in thresholds:
    print('threshold =', threshold)

    # Validate.
    predictions = rnn_model.predict(X_train_seq, batch_size=X_train_shape[0], verbose=0)
    trainErrors = 0
    trainTotal = 0
    for path in range(X_train_shape[0]):
        for step in range(X_train_shape[1]):
            if y_train_seq[path][step].max() >= threshold:
                trainTotal += 1
                if sensor_match(y_train_seq[path][step], predictions[path][step], X_train_shape[2]) == False:
                    trainErrors += 1
    trainErrorPct = 0
    if trainTotal > 0:
        trainErrorPct = (float(trainErrors) / float(trainTotal)) * 100.0

    # Predict.
    seq = array(X_test)
    X = seq.reshape(X_test_shape[0], X_test_shape[1], X_test_shape[2])
    seq = array(y_test)
    y = seq.reshape(y_test_shape[0], y_test_shape[1], y_test_shape[2])
    predictions = rnn_model.predict(X, batch_size=X_test_shape[0], verbose=0)
    testErrors = 0
    testTotal = 0
    for path in range(X_test_shape[0]):
        for step in range(X_test_shape[1]):
            if step in y_test_predictable[path]:
                testTotal += 1
                if sensor_match(y[path][step], predictions[path][step], X_test_shape[2]) == False:
                    testErrors += 1
    testErrorPct = 0
    if testTotal > 0:
        testErrorPct = (float(testErrors) / float(testTotal)) * 100.0

    if best_threshold < 0.0 or testErrorPct < best_testErrorPct:
        best_threshold = threshold
        best_trainErrors = trainErrors
        best_trainTotal = trainTotal
        best_trainErrorPct = trainErrorPct
        best_testErrors = testErrors
        best_testTotal = testTotal
        best_testErrorPct = testErrorPct

# Print results.
if verbose:
    print("Train prediction errors/total = ", best_trainErrors, "/", best_trainTotal, sep='', end='')
    print(" (", str(round(best_trainErrorPct, 2)), "%)", sep='')
    print("Test prediction errors/total = ", best_testErrors, "/", best_testTotal, sep='', end='')
    print(" (", str(round(best_testErrorPct, 2)), "%)", sep='')
    print("Activation threshold = ", best_threshold, sep='')

# Write results to file.
with open(results_filename, 'w', newline='\n') as f:
    f.write('{')
    f.write('\"train_prediction_errors\":\"'+str(best_trainErrors)+'\",')
    f.write('\"train_total_predictions\":\"'+str(best_trainTotal)+'\",')
    f.write('\"train_error_pct\":\"'+str(round(best_trainErrorPct, 2))+'\",')
    f.write('\"test_prediction_errors\":\"'+str(best_testErrors)+'\",')
    f.write('\"test_total_predictions\":\"'+str(best_testTotal)+'\",')
    f.write('\"test_error_pct\":\"'+str(round(best_testErrorPct, 2))+'\",')
    f.write('\"activation_threshold\":\"'+str(round(best_threshold, 2))+'\"')
    f.write('}\n')

sys.exit(0)
