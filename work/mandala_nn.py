# For conditions of distribution and use, see copyright notice in LICENSE.txt

# Mandala NN.
# imports mandala_nn_dataset.py
# results written to mandala_nn_results.json

import logging, os
logging.disable(logging.WARNING)
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
import numpy as np
from numpy import array
from numpy import loadtxt
from keras.models import Sequential
from keras.layers import Input, Dense
import sys, getopt

# Parameters
n_dimensions = 128
n_features = 1
n_neurons = '128,128,128'
n_epochs = 500
activation_threshold = '0.75'

# Results file name
results_filename = 'mandala_nn_results.json'

# Verbosity
verbose = True

# Get options
usage = 'mandala_nn.py [--dimensions <number of dimensions> (default=' + str(n_dimensions) + ')] [--features <number of features> (default=' + str(n_features) + ')] [--activation_threshold <threshold> (default=' + activation_threshold + ', comma-separated list)] [--neurons <number of neurons> (default=' + n_neurons + ', comma-separated list of neurons per layer)] [--epochs <epochs>] [--results_filename <filename> (default=' + results_filename + ')] [--quiet (quiet)]'
try:
  opts, args = getopt.getopt(sys.argv[1:],"hd:f:t:n:e:r:q",["help","dimensions=","features=","activation_threshold=","neurons=","epochs=","results_filename=","quiet"])
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
  elif opt in ("-t", "--activation_threshold"):
     activation_threshold = arg
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

# Import dataset
from mandala_nn_dataset import X_train_shape, y_train_shape, X_train, y_train, y_train_path_begin, X_test_shape, y_test_shape, X_test, y_test, y_test_path_begin, y_test_prediction, y_test_interstitial, context_tier_value_durations
if X_train_shape[0] == 0:
    print('Empty train dataset')
    sys.exit(1)
if X_test_shape[0] == 0:
    print('Empty test dataset')
    sys.exit(1)

# Create prediction NN
prediction_model = Sequential()
prediction_model.add(Input((X_train_shape[1],)))
prediction_model.add(Dense(n_hidden[0], activation='tanh'))
for i in range(1, len(n_hidden)):
    prediction_model.add(Dense(n_hidden[i], activation='tanh'))
prediction_model.add(Dense(y_train_shape[1], activation='tanh'))
prediction_model.compile(loss='mse', optimizer='adam')
if verbose:
    prediction_model.summary()

# Train prediction model
if verbose:
    print('train prediction model')
seq = array(X_train)
X_train_seq = seq.reshape(X_train_shape[0], X_train_shape[1])
seq = array(y_train)
y_train_seq = seq.reshape(y_train_shape[0], y_train_shape[1])
prediction_model.fit(X_train_seq, y_train_seq, epochs=n_epochs, batch_size=X_train_shape[0], verbose=int(verbose))

# Summarize features
def summarize_features(title, vals):
    idxs = []
    for j in range(len(vals)):
        if vals[j] >= threshold:
            idxs.append(j)
        if vals[j] <= -threshold:
            idxs.append(-j)
    if len(idxs) == 0:
        desc = title + ': []'
    else:
        maxval = max(idxs, key=abs)
        if maxval < 0:
            maxval = -maxval
        n = int(maxval / n_dimensions) + 1
        lists = []
        desc = title + ': ['
        for j in range(n):
            minval = n_dimensions * j
            maxval = n_dimensions * (j + 1)
            sublist = [val for val in idxs if (val >= minval and val < maxval) or (val <= -minval and val > -maxval)]
            sublisttmp = []
            for k in range(len(sublist)):
                val = sublist[k]
                if val > 0:
                    sublisttmp.append(val - (n_dimensions * j))
                else:
                    sublisttmp.append(val + (n_dimensions * j))
            sublist = sublisttmp
            lists.append(sublist)
            if j == 0:
                desc += 'sense='
            else:
                desc += ', tier' + str(j - 1) + '='
            desc += str(sublist)
        desc += ']'
        idxs = lists
    return desc, idxs

# Sensor features match?
def sensor_match(avals, bvals):
    amax = []
    for i in range(n_dimensions):
        if avals[i] >= threshold:
            amax.append(i)
    if len(amax) != n_features:
        return False
    amax.sort()
    bmax = []
    for i in range(n_dimensions):
        if bvals[i] >= threshold:
            bmax.append(i)
    if len(bmax) != n_features:
        return False
    bmax.sort()
    if amax != bmax:
        return False
    else:
        return True

# Tier features match?
def tier_match(avals, bvals):
    amax = []
    for i in range(n_dimensions):
        if avals[i] >= threshold:
            amax.append(i)
    amax.sort()
    bmax = []
    for i in range(n_dimensions):
        if bvals[i] >= threshold:
            bmax.append(i)
    bmax.sort()
    if amax != bmax:
        return False
    amin = []
    for i in range(n_dimensions):
        if avals[i] <= -threshold:
            amin.append(i)
    amin.sort()
    bmin = []
    for i in range(n_dimensions):
        if bvals[i] <= -threshold:
            bmin.append(i)
    bmin.sort()
    if amin != bmin:
        return False
    else:
        return True

# Evaluate each activation threshold
best_threshold = -1.0
best_trainErrors = -1
best_trainTotal = -1
best_trainErrorPct = -1.0
best_testErrors = -1
best_testTotal = -1
best_testErrorPct = -1.0
for threshold in thresholds:
    print('threshold =', threshold)

    # Validate
    if verbose:
        print('validate training')
    predictions = prediction_model.predict(X_train_seq, batch_size=X_train_shape[0], verbose=0)
    trainErrors = 0
    trainTotal = 0
    pathnum = -1
    stepnum = 0
    for i in range(X_train_shape[0]):
        if i in y_train_path_begin:
            pathnum += 1
            stepnum = 0
            if verbose:
                print('validate: begin path', pathnum)
        if verbose:
            xstr,xidxs = summarize_features('X', X_train_seq[i])
            ystr,yidxs = summarize_features('y', y_train_seq[i])
            pstr,pidxs = summarize_features('prediction', predictions[i])
            print('validate: path = ',pathnum,', step = ',stepnum,', ',xstr,', ',ystr,', ',pstr, sep='',end='')
        stepnum += 1
        trainTotal += 1
        start = 0
        end = n_dimensions
        if sensor_match(y_train_seq[i][start:end], predictions[i][start:end]) == False:
            trainErrors += 1
            if verbose:
                print(', error')
        else:
            n_contexts = (int)(y_train_shape[1] / n_dimensions) - 1
            error = False
            for j in range(n_contexts):
                start = (n_dimensions * (j + 1))
                end = start + n_dimensions
                if tier_match(y_train_seq[i][start:end], predictions[i][start:end]) == False:
                    trainErrors += 1
                    error = True
                    break
            if verbose:
                if error:
                    print(', error')
                else:
                    print(', ok')
    
    trainErrorPct = 0
    if trainTotal > 0:
        trainErrorPct = (float(trainErrors) / float(trainTotal)) * 100.0
    
    # Predict
    if verbose:
        print('predict')
    seq = array(X_test)
    X = seq.reshape(X_test_shape[0], X_test_shape[1])
    seq = array(y_test)
    y = seq.reshape(y_test_shape[0], y_test_shape[1])
    expiration_counters = None
    if context_tier_value_durations != None:
        expiration_counters = [0] * n_dimensions * len(context_tier_value_durations)
    testErrors = 0
    testTotal = 0
    pathnum = -1
    stepnum = 0
    prediction_valid_count = 0
    for i in range(X_test_shape[0]):
        Xi = X[i].reshape(1, X_test_shape[1])
        if i not in y_test_path_begin:
            if expiration_counters != None:
                for j in range(len(expiration_counters)):
                    if expiration_counters[j] > 0:
                        expiration_counters[j] -= 1
            Xj = X[i - 1].reshape(1, X_test_shape[1])
            for j in range(n_dimensions, X_test_shape[1]):
                if expiration_counters != None:
                    if expiration_counters[j - n_dimensions] > 0:
                        Xi[0][j] = Xj[0][j]
                    else:
                        Xi[0][j] = 0.0
                else:
                    Xi[0][j] = Xj[0][j]
            if prediction_valid_count > 0:
                prediction_valid_count -= 1
                for j in range(n_dimensions, X_test_shape[1]):
                    if prediction[0][j] >= threshold:
                        Xi[0][j] = 1.0
                        if expiration_counters != None:
                            k = int((j - n_dimensions) / n_dimensions)
                            expiration_counters[j - n_dimensions] = context_tier_value_durations[k]
                    elif prediction[0][j] <= -threshold:
                        Xi[0][j] = 0.0
                        if expiration_counters != None:
                            expiration_counters[j - n_dimensions] = 0
        else:
            prediction_valid_count = 0
            pathnum += 1
            stepnum = 0
            for j in range(n_dimensions, X_test_shape[1]):
                Xi[0][j] = 0.0
            if expiration_counters != None:
                for j in range(len(expiration_counters)):
                    expiration_counters[j] = 0
        yi = y[i].reshape(1, y_test_shape[1]).copy()
        prediction = prediction_model.predict(Xi, verbose=0)
        if verbose:
            xstr,xidxs = summarize_features('X', Xi[0])
            ystr,yidxs = summarize_features('y', yi[0])
            pstr,pidxs = summarize_features('prediction', prediction[0])
            print('predict: path = ',pathnum,', step = ',stepnum,', ',xstr,', ',ystr,', ',pstr, sep='', end='')
        stepnum += 1
        if i in y_test_prediction:
            testTotal += 1
        if prediction_valid_count == 0:
            if sensor_match(yi[0][0:n_dimensions], prediction[0][0:n_dimensions]) == True:
                prediction_valid_count = 2
                if i in y_test_prediction:
                    if verbose:
                        print(', ok')
                else:
                    if verbose:
                        print(', unexpected')
            else:
                if i in y_test_prediction:
                    testErrors += 1
                    if verbose:
                        print(', error')
                elif i in y_test_interstitial:
                    if verbose:
                        print(', interstitial')
                else:
                    if verbose:
                        print()
        else:
            if verbose:
                print(', effect')
    
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
