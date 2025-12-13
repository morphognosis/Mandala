# For conditions of distribution and use, see copyright notice in LICENSE.txt

# Causations NN.
# imports causations_nn_dataset.py
# results written to causations_nn_results.json

import os
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"
from numpy import array, argmax
from numpy import loadtxt
from keras.models import Sequential
from keras.layers import Input, Dense
import sys, getopt

# parameters
n_hidden = [128, 128, 128]
n_epochs = 500

# results file name
results_filename = 'causations_nn_results.json'

# prediction significance threshold
threshold = 0.5

# verbosity
verbose = True

# get options
first_hidden = True
usage = 'causations_nn.py [-h <hidden neurons> (repeat for additional layers)] [-e <epochs>] [-q (quiet)]'
try:
  opts, args = getopt.getopt(sys.argv[1:],"?qh:e:",["hidden=","epochs="])
except getopt.GetoptError:
  print(usage)
  sys.exit(1)
for opt, arg in opts:
  if opt in ("-?", "--help"):
     print(usage)
     sys.exit(0)
  if opt in ("-h", "--hidden"):
     if first_hidden:
         first_hidden = False
         n_hidden = []
     n_hidden.append(int(arg))
  elif opt in ("-e", "--epochs"):
     n_epochs = int(arg)
  elif opt in ("-q", "--quiet"):
     verbose = False
  else:
     print(usage)
     sys.exit(1)

# import dataset
from causations_nn_dataset import X_train_shape, y_train_shape, X_train, y_train, X_test_shape, y_test_shape, X_test, y_test, y_test_predictable
if X_train_shape[0] == 0:
    print('Empty train dataset')
    sys.exit(1)
if X_test_shape[0] == 0:
    print('Empty test dataset')
    sys.exit(1)

# create NN
model = Sequential()
model.add(Input((X_train_shape[1],)))
model.add(Dense(n_hidden[0], activation='relu'))
for i in range(1, len(n_hidden)):
    model.add(Dense(n_hidden[i], activation='relu'))
model.add(Dense(y_train_shape[1], activation='sigmoid'))
model.compile(loss='binary_crossentropy', optimizer='adam')
if verbose:
    model.summary()

# train
seq = array(X_train)
X = seq.reshape(X_train_shape[0], X_train_shape[1])
seq = array(y_train)
y = seq.reshape(y_train_shape[0], y_train_shape[1])
model.fit(X, y, epochs=n_epochs, batch_size=X_train_shape[0], verbose=int(verbose))

# validate
seq = array(X_train)
X = seq.reshape(X_train_shape[0], X_train_shape[1])
seq = array(y_train)
y = seq.reshape(y_train_shape[0], y_train_shape[1])
predictions = model.predict(X, batch_size=X_train_shape[0], verbose=int(verbose))
trainErrors = 0
trainTotal = 0
for i in range(y_train_shape[0]):
    yvals = y[i]
    pvals = predictions[i]
    trainTotal += 1
    for j in range(len(yvals)):
        if yvals[j] >= threshold and pvals[j] < threshold:
            trainErrors += 1
            break
        if yvals[j] < threshold and pvals[j] >= threshold:
            trainErrors += 1
            break
trainErrorPct = 0
if trainTotal > 0:
    trainErrorPct = (float(trainErrors) / float(trainTotal)) * 100.0

# predict
seq = array(X_test)
X = seq.reshape(X_test_shape[0], X_test_shape[1])
seq = array(y_test)
y = seq.reshape(y_test_shape[0], y_test_shape[1])
predictions = model.predict(X, batch_size=X_test_shape[0], verbose=int(verbose))
testErrors = 0
testTotal = 0
for i in range(y_test_shape[0]):
    if i in y_test_predictable:
        yvals = y[i]
        pvals = predictions[i]
        testTotal += 1
        for j in range(len(yvals)):
            if yvals[j] >= threshold and pvals[j] < threshold:
                testErrors += 1
                break
            if yvals[j] < threshold and pvals[j] >= threshold:
                testErrors += 1
                break
testErrorPct = 0
if testTotal > 0:
    testErrorPct = (float(testErrors) / float(testTotal)) * 100.0

# print results.
if verbose:
    print("Train prediction errors/total = ", trainErrors, "/", trainTotal, sep='', end='')
    print(" (", str(round(trainErrorPct, 2)), "%)", sep='', end='')
    print('')
    print("Test prediction errors/total = ", testErrors, "/", testTotal, sep='', end='')
    print(" (", str(round(testErrorPct, 2)), "%)", sep='', end='')
    print('')

# write results to file.
with open(results_filename, 'w') as f:
    f.write('{')
    f.write('\"train_prediction_errors\":\"'+str(trainErrors)+'\",')
    f.write('\"train_total_predictions\":\"'+str(trainTotal)+'\",')
    f.write('\"train_error_pct\":\"'+str(round(trainErrorPct, 2))+'\",')
    f.write('\"test_prediction_errors\":\"'+str(testErrors)+'\",')
    f.write('\"test_total_predictions\":\"'+str(testTotal)+'\",')
    f.write('\"test_error_pct\":\"'+str(round(testErrorPct, 2))+'\"')
    f.write('}\n')

sys.exit(0)
