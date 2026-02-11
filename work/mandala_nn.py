# For conditions of distribution and use, see copyright notice in LICENSE.txt

# Mandala NN.
# imports mandala_nn_dataset.py
# results written to mandala_nn_results.json

import os
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"
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
threshold = 0.5

# Allow interstitial contexts?
allow_interstitial_contexts = True

# Verbosity
verbose = True

# Get options
usage = 'mandala_nn.py [--dimensions <number of dimensions> (default=' + str(n_dimensions) + ')] [--features <number of features> (default=' + str(n_features) + ')] [--allow_interstitial_contexts <True or False> (default=' + str(allow_interstitial_contexts) + ')] [--neurons <number of neurons> (default=' + n_neurons + ', comma-separated list of neurons per layer)] [--epochs <epochs>] [--results_filename <filename> (default=' + results_filename + ')] [--quiet (quiet)]'
try:
  opts, args = getopt.getopt(sys.argv[1:],"hd:f:i:n:e:r:q",["help","dimensions=","features=","allow_interstitial_contexts=","neurons=","epochs=","results_filename=","quiet"])
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
  elif opt in ("-i", "--allow_interstitial_contexts"):
     if str(arg) == "True" or str(arg) == "true":
        allow_interstitial_contexts = True
     elif str(arg) == "False" or str(arg) == "false":
        allow_interstitial_contexts = False
     else:
        print(usage)
        sys.exit(1)
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
from mandala_nn_dataset import X_train_shape, y_train_shape, X_train, y_train, y_train_path_begin, X_test_shape, y_test_shape, X_test, y_test, y_test_path_begin, y_test_predictable, y_test_interstitial
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
predictions = model.predict(X, batch_size=X_train_shape[0], verbose=0)
trainErrors = 0
trainTotal = 0
pathnum = -1
stepnum = 0
for i in range(X_train_shape[0]):
    if i in y_train_path_begin:
        pathnum = pathnum + 1
        stepnum = 0
    if verbose:
        xvals = X[i].copy()
        xidxs = []
        for j in range(len(xvals)):
            if xvals[j] >= threshold:
                xidxs.append(j)
            if xvals[j] <= -threshold:
                xidxs.append(-j)
        yvals = y[i].copy()
        yidxs = []
        for j in range(len(yvals)):
            if yvals[j] >= threshold:
                yidxs.append(j)
            if yvals[j] <= -threshold:
                yidxs.append(-j)
        pvals = predictions[i].copy()
        pidxs = []
        for j in range(len(pvals)):
            if pvals[j] >= threshold:
                pidxs.append(j)
            if pvals[j] <= -threshold:
                pidxs.append(-j)
        xmax = max(xidxs)
        n = int(xmax / n_dimensions)
        if xmax % n_dimensions > 0:
            n = n + 1
        xlists = []
        xstr = 'X: ['
        for j in range(n):
            xmin = n_dimensions * j
            xmax = n_dimensions * (j + 1)
            xsublist = [x for x in xidxs if x >= xmin and x < xmax]
            xsublist2 = []
            for k in range(len(xsublist)):
                xsublist2.append(xsublist[k] - (n_dimensions * j))
            xsublist = xsublist2
            xlists.append(xsublist)
            if j == 0:
                xstr += 'sense='
            else:
                xstr += ', ctx' + str(j - 1) + '='
            xstr += str(xsublist)
        xstr += ']'
        xidxs = xlists
        ymax = max(yidxs, key=abs)
        if ymax < 0:
            ymax = -ymax
        n = int(ymax / n_dimensions)
        if ymax % n_dimensions > 0:
            n = n + 1
        ylists = []
        ystr = 'y: ['
        for j in range(n):
            ymin = n_dimensions * j
            ymax = n_dimensions * (j + 1)
            ysublist = [x for x in yidxs if abs(x) >= ymin and abs(x) < ymax]
            ysublist2 = []
            for k in range(len(ysublist)):
                v = ysublist[k]
                if v > 0:
                    ysublist2.append(v - (n_dimensions * j))
                elif v < 0:
                    ysublist2.append(v + (n_dimensions * j))
            ysublist = ysublist2
            ylists.append(ysublist)
            if j == 0:
                ystr += 'sense='
            else:
                ystr += ', ctx' + str(j - 1) + '='
            ystr += str(ysublist)
        ystr += ']'
        yidxs = ylists
        pmax = max(pidxs, key=abs)
        if pmax < 0:
            pmax = -pmax
        n = int(pmax / n_dimensions)
        if pmax % n_dimensions > 0:
            n = n + 1
        plists = []
        pstr = 'predictions: ['
        for j in range(n):
            pmin = n_dimensions * j
            pmax = n_dimensions * (j + 1)
            psublist = [x for x in pidxs if abs(x) >= pmin and abs(x) < pmax]
            psublist2 = []
            for k in range(len(psublist)):
                v = psublist[k]
                if v > 0:
                    psublist2.append(v - (n_dimensions * j))
                elif v < 0:
                    psublist2.append(v + (n_dimensions * j))
            psublist = psublist2
            plists.append(psublist)
            if j == 0:
                pstr += 'sense='
            else:
                pstr += ', ctx' + str(j - 1) + '='
            pstr += str(psublist)
        pstr += ']'
        pidxs = plists
        print('validate: path = ',pathnum,', step = ',stepnum,', ',xstr,', ',ystr,', ',pstr,sep='',end='')
    stepnum = stepnum + 1
    yvals = y[i].copy()
    yvals = yvals[0:n_dimensions]
    pvals = predictions[i].copy()
    pvals = pvals[0:n_dimensions]
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
        if verbose:
            print(', error')
    else:
        n_contexts = (int)(y_train_shape[1] / n_dimensions) - 1
        error = False
        for j in range(n_contexts):
            start = n_dimensions * (j + 1)
            end = start + n_dimensions
            yvals = y[i].copy()
            yvals = yvals[start:end]
            pvals = predictions[i].copy()
            pvals = pvals[start:end]
            ymax = []
            pmax = []
            for k in range(n_features):
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
                trainErrors += 1
                break
            yvals = y[i].copy()
            yvals = yvals[start:end]
            pvals = predictions[i].copy()
            pvals = pvals[start:end]
            ymin = []
            pmin = []
            for k in range(n_features):
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
seq = array(X_test)
X = seq.reshape(X_test_shape[0], X_test_shape[1])
seq = array(y_test)
y = seq.reshape(y_test_shape[0], y_test_shape[1])
testErrors = 0
testTotal = 0
pathnum = -1
stepnum = 0
prediction = None
for i in range(X_test_shape[0]):
    Xi = X[i].reshape(1, X_test_shape[1])
    if i not in y_test_path_begin:
        Xj = X[i - 1].reshape(1, X_test_shape[1])
        if allow_interstitial_contexts == True or i - 1 not in y_test_interstitial:
            for j in range(n_dimensions, X_test_shape[1]):
                Xi[0][j] = Xj[0][j]
                if prediction[0][j] >= threshold:
                    Xi[0][j] = 1.0
                elif prediction[0][j] <= -threshold:
                    Xi[0][j] = 0.0
        else:
            for j in range(n_dimensions, X_test_shape[1]):
                Xi[0][j] = Xj[0][j]
    else:
        pathnum = pathnum + 1
        stepnum = 0
        for j in range(n_dimensions, X_test_shape[1]):
            Xi[0][j] = 0.0
    yi = y[i].reshape(1, y_test_shape[1]).copy()
    prediction = model.predict(Xi, verbose=0)
    if verbose:
        xvals = Xi[0].copy()
        xidxs = []
        for j in range(len(xvals)):
            if xvals[j] >= threshold:
                xidxs.append(j)
            if xvals[j] <= -threshold:
                xidxs.append(-j)
        yvals = yi[0].copy()
        yidxs = []
        for j in range(len(yvals)):
            if yvals[j] >= threshold:
                yidxs.append(j)
            if yvals[j] <= -threshold:
                yidxs.append(-j)
        pvals = prediction[0].copy()
        pidxs = []
        for j in range(len(pvals)):
            if pvals[j] >= threshold:
                pidxs.append(j)
            if pvals[j] <= -threshold:
                pidxs.append(-j)
        if len(xidxs) == 0:
            xstr = 'X: []'
        else:
            xmax = max(xidxs, key=abs)
            if xmax < 0:
                xmax = -xmax
            n = int(xmax / n_dimensions)
            if xmax % n_dimensions > 0:
                n = n + 1
            xlists = []
            xstr = 'X: ['
            for j in range(n):
                xmin = n_dimensions * j
                xmax = n_dimensions * (j + 1)
                xsublist = [x for x in xidxs if x >= xmin and x < xmax]
                xsublist2 = []
                for k in range(len(xsublist)):
                    xsublist2.append(xsublist[k] - (n_dimensions * j))
                xsublist = xsublist2
                xlists.append(xsublist)
                if j == 0:
                    xstr += 'sense='
                else:
                    xstr += ', ctx' + str(j - 1) + '='
                xstr += str(xsublist)
            xstr += ']'
            xidxs = xlists
        if len(yidxs) == 0:
            ystr = 'y: []'
        else:
            ymax = max(yidxs, key=abs)
            if ymax < 0:
                ymax = -ymax
            n = int(ymax / n_dimensions)
            if ymax % n_dimensions > 0:
                n = n + 1
            ylists = []
            ystr = 'y: ['
            for j in range(n):
                ymin = n_dimensions * j
                ymax = n_dimensions * (j + 1)
                ysublist = [x for x in yidxs if abs(x) >= ymin and abs(x) < ymax]
                ysublist2 = []
                for k in range(len(ysublist)):
                    v = ysublist[k]
                    if v > 0:
                        ysublist2.append(v - (n_dimensions * j))
                    elif v < 0:
                        ysublist2.append(v + (n_dimensions * j))
                ysublist = ysublist2
                ylists.append(ysublist)
                if j == 0:
                    ystr += 'sense='
                else:
                    ystr += ', ctx' + str(j - 1) + '='
                ystr += str(ysublist)
            ystr += ']'
            yidxs = ylists
        if len(pidxs) == 0:
            pstr = 'predictions: []'
        else:
            pmax = max(pidxs, key=abs)
            if pmax < 0:
                pmax = -pmax
            n = int(pmax / n_dimensions)
            if pmax % n_dimensions > 0:
                n = n + 1
            plists = []
            pstr = 'predictions: ['
            for j in range(n):
                pmin = n_dimensions * j
                pmax = n_dimensions * (j + 1)
                psublist = [x for x in pidxs if abs(x) >= pmin and abs(x) < pmax]
                psublist2 = []
                for k in range(len(psublist)):
                    v = psublist[k]
                    if v > 0:
                        psublist2.append(v - (n_dimensions * j))
                    elif v < 0:
                        psublist2.append(v + (n_dimensions * j))
                psublist = psublist2
                plists.append(psublist)
                if j == 0:
                    pstr += 'sense='
                else:
                    pstr += ', ctx' + str(j - 1) + '='
                pstr += str(psublist)
            pstr += ']'
            pidxs = plists
        print('predict: path = ',pathnum,', step = ',stepnum,', ',xstr,', ',ystr,', ',pstr,sep='',end='')
    stepnum = stepnum + 1
    if i in y_test_predictable:
        yvals = yi[0].copy()
        yvals = yvals[0:n_dimensions]
        pvals = prediction[0].copy()
        pvals = pvals[0:n_dimensions]
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
            if verbose:
                print(', error')
        else:
            if verbose:
                print(', ok')
    else:
        if verbose:
            print()

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
