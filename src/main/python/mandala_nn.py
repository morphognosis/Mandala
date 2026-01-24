# For conditions of distribution and use, see copyright notice in LICENSE.txt

# Mandala NN.
# imports mandala_nn_dataset.py
# results written to mandala_nn_results.json

import os
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"
from numpy import array, argmax
from numpy import loadtxt
from keras.models import Sequential
from keras.layers import Input, Dense
import sys, getopt

# Parameters
n_dimensions = 64
n_features = 3
n_neurons = '128,128,128'
n_epochs = 500

# Results file name
results_filename = 'mandala_nn_results.json'

# Prediction significance threshold
threshold = 0.1

# Verbosity
verbose = True

# Get options
usage = 'mandala_nn.py [--dimensions <number of dimensions> (default=' + str(n_dimensions) + ')] [--features <number of features> (default=' + str(n_features) + ')] [--neurons <number of neurons> (default=' + n_neurons + ', comma-separated list of neurons per layer)] [--epochs <epochs>] [--results_filename <filename> (default=' + results_filename + ')] [--quiet (quiet)]'
try:
  opts, args = getopt.getopt(sys.argv[1:],"hd:f:n:e:r:q",["help","dimensions=","features=","neurons=","epochs=","results_filename=","quiet"])
except getopt.GetoptError:
  print(usage)
  sys.exit(1)
for opt, arg in opts:
  if opt in ("-h", "--help"):
     print(usage)
     sys.exit(0)
  if opt in ("-d", "--dimensions"):
     n_dimensions = int(arg)
  elif opt in ("-f", "--features"):
     n_features = int(arg)
  elif opt in ("-n", "--neurons"):
     n_neurons = arg
  elif opt in ("-e", "--epochs"):
     n_epochs = int(arg)
  elif opt in ("-r", "--results_filename"):
     results_filename = arg
  elif opt in ("-q", "--quiet"):
     verbose = False
  else:
     print(usage)
     sys.exit(1)
if n_dimensions < 1:
    print(usage, sep='')
    sys.exit(1)
if n_features < 1:
    print(usage, sep='')
    sys.exit(1)
if n_features > n_dimensions:
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

# Import dataset
from mandala_nn_dataset import X_train_shape, y_train_shape, X_train, y_train, X_test_shape, y_test_shape, X_test, y_test, y_test_predictable
if X_train_shape[0] == 0:
    print('Empty train dataset')
    sys.exit(1)
if X_test_shape[0] == 0:
    print('Empty test dataset')
    sys.exit(1)

# Create NN
model = Sequential()
model.add(Input((X_train_shape[1],)))
model.add(Dense(n_hidden[0], activation='relu'))
for i in range(1, len(n_hidden)):
    model.add(Dense(n_hidden[i], activation='relu'))
model.add(Dense(y_train_shape[1], activation='sigmoid'))
model.compile(loss='binary_crossentropy', optimizer='adam')
if verbose:
    model.summary()

# Train
seq = array(X_train)
X = seq.reshape(X_train_shape[0], X_train_shape[1])
seq = array(y_train)
y = seq.reshape(y_train_shape[0], y_train_shape[1])
model.fit(X, y, epochs=n_epochs, batch_size=X_train_shape[0], verbose=int(verbose))

# Validate
predictions = model.predict(X, batch_size=X_train_shape[0], verbose=int(verbose))
trainErrors = 0
trainTotal = 0
for i in range(y_train_shape[0]):
    yvals = y[i]
    pvals = predictions[i]
    tvals = []
    for j in range(len(pvals)):
        if j < n_dimensions:
            tvals.append(pvals[j])
        else:
            tvals.append(0.0)
    pvals = tvals
    ymax = []
    pmax = []
    for j in range(n_features):
        k = argmax(yvals)
        ymax.append(k)
        yvals[k] = 0.0
        k = argmax(pvals)
        pmax.append(k)
        pvals[k] = 0.0
    ymax.sort()
    pmax.sort()
    trainTotal += 1
    if ymax != pmax:
        trainErrors += 1
trainErrorPct = 0
if trainTotal > 0:
    trainErrorPct = (float(trainErrors) / float(trainTotal)) * 100.0

# Predict
seq = array(X_test)
X = seq.reshape(X_test_shape[0], X_test_shape[1])
seq = array(y_test)
y = seq.reshape(y_test_shape[0], y_test_shape[1])
testErrors = 0
testTotal = 0
p = None
for i in range(X_test_shape[0]):
    Xi = X[i].reshape(1, X_test_shape[1])
    if p is not None:
        for j in range(n_dimensions, X_test_shape[1]):
            Xi[0][j] = p[0][j]
    yi = y[i].reshape(1, y_test_shape[1])
    prediction = model.predict(Xi, verbose=int(verbose))
    p = None
    if i in y_test_predictable:
        p = prediction
        yvals = yi[0]
        pvals = prediction[0]
        for j in range(n_dimensions, len(pvals)):
            yvals[j] = 0.0
            pvals[j] = 0.0
        ymax = []
        pmax = []
        for j in range(n_features):
            k = argmax(yvals)
            ymax.append(k)
            yvals[k] = 0.0
            k = argmax(pvals)
            pmax.append(k)
            pvals[k] = 0.0
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
