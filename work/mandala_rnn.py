# For conditions of distribution and use, see copyright notice in LICENSE.txt

# Mandala RNN.
# imports mandala_rnn_dataset.py
# results written to mandala_rnn_results.json

# Default parameters.
n_features = 3
n_neurons = '128'
n_epochs = 500
results_filename = 'mandala_rnn_results.json'
verbose = True

# Prediction significance threshold
threshold = 0.5

# Get options.
import os
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"
import getopt
import sys
usage = 'usage: python mandala_rnn.py [--features <number of features> (default=' + str(n_features) + ')] [--neurons <number of neurons> (default=' + n_neurons + ', comma-separated list of neurons per layer)] [--epochs <number of epochs> (default=' + str(n_epochs) + ')] [--results_filename <filename> (default=' + results_filename + ')] [--quiet (quiet)]'
try:
  opts, args = getopt.getopt(sys.argv[1:],"hf:n:e:r:q",["help","features=","neurons=","epochs=","results_filename=","quiet"])
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
from numpy import array, argmax
seq = array(X_train)
X = seq.reshape(X_train_shape[0], X_train_shape[1], X_train_shape[2])
seq = array(y_train)
y = seq.reshape(y_train_shape[0], y_train_shape[1], y_train_shape[2])
rnn_model.fit(X, y, epochs=n_epochs, batch_size=X_train_shape[0], verbose=int(verbose))

# Validate.
predictions = rnn_model.predict(X, batch_size=X_train_shape[0], verbose=int(verbose))
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
            if yvals[yidx] >= 0.5:
                ymax.append(yidx)
            yvals[yidx] = 0.0
            pidx = argmax(pvals)
            if pvals[pidx] >= 0.5:
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
predictions = rnn_model.predict(X, batch_size=X_test_shape[0], verbose=int(verbose))
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
                if yvals[yidx] >= 0.5:
                    ymax.append(yidx)
                yvals[yidx] = 0.0
                pidx = argmax(pvals)
                if pvals[pidx] >= 0.5:
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
