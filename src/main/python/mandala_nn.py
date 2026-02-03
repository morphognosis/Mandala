# For conditions of distribution and use, see copyright notice in LICENSE.txt

# Mandala NN.
# imports mandala_nn_dataset.py
# results written to mandala_nn_results.json

import os
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"
from numpy import array, argmax, argmin
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
from mandala_nn_dataset import X_train_shape, y_train_shape, X_train, y_train, X_test_shape, y_test_shape, X_test, y_test, y_test_path_begin, y_test_predictable
if X_train_shape[0] == 0:
    print('Empty train dataset')
    sys.exit(1)
if X_test_shape[0] == 0:
    print('Empty test dataset')
    sys.exit(1)

# Create NN
model = Sequential()
model.add(Input((X_train_shape[1],)))
model.add(Dense(n_hidden[0], activation='tanh'))
for i in range(1, len(n_hidden)):
    model.add(Dense(n_hidden[i], activation='tanh'))
model.add(Dense(y_train_shape[1], activation='tanh'))
model.compile(loss='mse', optimizer='adam')
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
    yvals = yvals[0:n_dimensions]
    pvals = predictions[i]
    pvals = pvals[0:n_dimensions]
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
    else:
        n_contexts = (int)(y_train_shape[1] / n_dimensions) - 1
        for j in range(n_contexts):
            start = n_dimensions * (j + 1)
            end = start + n_dimensions
            yvals = y[i]
            yvals = yvals[start:end]
            pvals = predictions[i]
            pvals = pvals[start:end]
            ymax = []
            pmax = []
            for k in range(n_features):
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
            if ymax != pmax:
                trainErrors += 1
                break
            yvals = y[i]
            yvals = yvals[start:end]
            pvals = predictions[i]
            pvals = pvals[start:end]
            ymin = []
            pmin = []
            for k in range(n_features):
                yidx = argmin(yvals)
                if yvals[yidx] <= -0.5:
                    ymin.append(yidx)
                yvals[yidx] = 0.0
                pidx = argmin(pvals)
                if pvals[pidx] <= -0.5:
                    pmin.append(pidx)
                pvals[pidx] = 0.0
            ymin.sort()
            pmin.sort()
            if ymin != pmin:
                trainErrors += 1
                break

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
prediction = None
for i in range(X_test_shape[0]):
    Xi = X[i].reshape(1, X_test_shape[1])
    if i > 0:
        Xj = X[i - 1].reshape(1, X_test_shape[1])
        yj = y[i - 1].reshape(1, y_test_shape[1])
        for j in range(n_dimensions, X_test_shape[1]):
            Xi[0][j] = Xj[0][j] + yj[0][j]
    yi = y[i].reshape(1, y_test_shape[1])
    prediction = model.predict(Xi, verbose=int(verbose))
    if i in y_test_predictable:
        yvals = yi[0]
        pvals = prediction[0]
        for j in range(n_dimensions, X_test_shape[1]):
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
