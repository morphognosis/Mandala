# For conditions of distribution and use, see copyright notice in LICENSE.txt

# Mandala NN.
# imports mandala_nn_dataset.py
# results written to mandala_nn_results.json

import logging, os
logging.disable(logging.WARNING)
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "3"
os.environ["TF_ENABLE_ONEDNN_OPTS"] = "0"
import numpy as np
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
threshold = 0.85

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
from mandala_nn_dataset import X_train_shape, y_train_shape, X_train, y_train, X_signature_train_shape, y_signature_train_shape, X_signature_train, y_signature_train, y_train_path_begin, X_test_shape, y_test_shape, X_test, y_test, y_test_path_begin, y_test_predictable, y_test_interstitial, context_tier_value_durations
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

# Create signature NN
signature_model = Sequential()
signature_model.add(Input((X_signature_train_shape[1],)))
signature_model.add(Dense(n_hidden[0], activation='tanh'))
for i in range(1, len(n_hidden)):
    signature_model.add(Dense(n_hidden[i], activation='tanh'))
signature_model.add(Dense(y_signature_train_shape[1], activation='tanh'))
signature_model.compile(loss='mse', optimizer='adam')
if verbose:
    signature_model.summary()

# Train prediction model
if verbose:
    print('train prediction model')
seq = array(X_train)
X_train_seq = seq.reshape(X_train_shape[0], X_train_shape[1])
seq = array(y_train)
y_train_seq = seq.reshape(y_train_shape[0], y_train_shape[1])
prediction_model.fit(X_train_seq, y_train_seq, epochs=n_epochs, batch_size=X_train_shape[0], verbose=int(verbose))

# Train signature model
if verbose:
    print('train signature model')
seq = array(X_signature_train)
X_signature_seq = seq.reshape(X_signature_train_shape[0], X_signature_train_shape[1])
seq = array(y_signature_train)
y_signature_seq = seq.reshape(y_signature_train_shape[0], y_signature_train_shape[1])
signature_model.fit(X_signature_seq, y_signature_seq, epochs=n_epochs, batch_size=X_signature_train_shape[0], verbose=int(verbose))

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
        n = int(maxval / n_dimensions)
        if maxval % n_dimensions > 0:
            n = n + 1
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

# Summarize signature
def summarize_signature(vals):
    idxs = []
    for j in range(len(vals)):
        if vals[j] >= threshold:
            idxs.append(j)
    return str(idxs)

# Check signature validity
def signature_valid(xvals, pvals):
    xmax = []
    for i in range(n_dimensions):
        xidx = argmax(xvals)
        if xvals[xidx] >= threshold:
            xmax.append(xidx)
            xvals[xidx] = 0.0
        else:
            break
    xmax.sort()
    pidxs = []
    n = len(pvals)
    for i in range(len(xmax)):
        pidxs.append(xmax[i] % n)
    pidxs = list(set(pidxs))
    for i in range(len(pidxs)):
        pidx = pidxs[i]
        if pvals[pidx] < threshold:
            return False
        else:
            pvals[pidx] = -1.0
    for i in range(n):
        if pvals[i] >= threshold:
            return False
    return True

# Check tier max values prediction
def tier_max_prediction(yvals, pvals):
    ymax = []
    pmax = []
    for i in range(n_features):
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
    if ymax != pmax:
        return False
    else:
        return True

# Check tier min values prediction
def tier_min_prediction(yvals, pvals):
    ymin = []
    pmin = []
    for i in range(n_features):
        yidx = argmin(yvals)
        if yvals[yidx] <= -threshold:
            ymin.append(yidx)
        yvals[yidx] = 0.0
        pidx = argmin(pvals)
        if pvals[pidx] <= -threshold:
            pmin.append(pidx)
        pvals[pidx] = 0.0
    ymin.sort()
    pmin.sort()
    if ymin != pmin:
        return False
    else:
        return True

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
    sxvals = X_train_seq[i].copy()
    sxvals = sxvals[0:n_dimensions]
    signature_prediction = signature_model.predict(array([sxvals]), verbose=0)
    if verbose:
        sstr = summarize_signature(signature_prediction[0])
        xstr,xidxs = summarize_features('X', X_train_seq[i].copy())
        ystr,yidxs = summarize_features('y', y_train_seq[i].copy())
        pstr,pidxs = summarize_features('prediction', predictions[i].copy())
        print('validate: path = ',pathnum,', step = ',stepnum,', ',xstr,', signature: ',sstr,', ',ystr,', ',pstr,sep='',end='')
    stepnum += 1
    trainTotal += 1
    if signature_valid(sxvals, signature_prediction.copy()[0]) == False:
        trainErrors += 1
        if verbose:
            print(', invalid')
    else:
        start = 0
        end = n_dimensions
        if tier_max_prediction(y_train_seq[i].copy()[start:end], predictions[i].copy()[start:end]) == False:
            trainErrors += 1
            if verbose:
                print(', error')
        else:
            n_contexts = (int)(y_train_shape[1] / n_dimensions) - 1
            error = False
            for j in range(n_contexts):
                start = (n_dimensions * (j + 1))
                end = start + n_dimensions
                if tier_max_prediction(y_train_seq[i].copy()[start:end], predictions[i].copy()[start:end]) == False:
                    trainErrors += 1
                    error = True
                    break
                if tier_min_prediction(y_train_seq[i].copy()[start:end], predictions[i].copy()[start:end]) == False:
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
    sxvals = Xi[0].copy()
    sxvals = sxvals[0:n_dimensions]
    signature_prediction = signature_model.predict(array([sxvals]), verbose=0)
    if verbose:
        sstr = summarize_signature(signature_prediction[0])
        xstr,xidxs = summarize_features('X', Xi[0].copy())
        ystr,yidxs = summarize_features('y', yi[0].copy())
        pstr,pidxs = summarize_features('prediction', prediction[0].copy())
        print('predict: path = ',pathnum,', step = ',stepnum,', ',xstr,', signature: ',sstr,', ',ystr,', ',pstr,sep='',end='')
    stepnum += 1
    if prediction_valid_count == 0:
        if signature_valid(sxvals, signature_prediction.copy()[0]) == True:
            prediction_valid_count = 2
            testTotal += 1
            start = 0
            end = n_dimensions
            if tier_max_prediction(yi[0].copy()[start:end], prediction[0].copy()[start:end]) == False:
                testErrors += 1
                if verbose:
                    print(', error')
            else:
                if verbose:
                    print(', ok')
        else:
            if i in y_test_predictable:
                testErrors += 1
                if verbose:
                    print(', invalid')
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
