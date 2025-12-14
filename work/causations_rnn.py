# For conditions of distribution and use, see copyright notice in LICENSE.txt

# Causations RNN.
# imports causations_nn_dataset.py
# results written to causations_rnn_results.json

# Default parameters.
rnn_type = 'lstm'
n_neurons = '128'
n_epochs = 500
results_filename = 'causations_rnn_results.json'
verbose = True

# Prediction significance threshold
threshold = 0.5

# Get options.
import os
os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"
import getopt
import sys
usage = 'usage: python causations_rnn.py [--rnn_type <"lstm" | "attention"> (default=' + rnn_type + ')] [--neurons <number of neurons> (default=' + n_neurons + ', comma-separated list of neurons per layer)] [--epochs <number of epochs> (default=' + str(n_epochs) + ')] [--results_filename <filename> (default=' + results_filename + ')] [--quiet (quiet)]'
try:
  opts, args = getopt.getopt(sys.argv[1:],"ht:n:e:f:q",["help","rnn_type=","neurons=","epochs=","results_filename=","quiet"])
except getopt.GetoptError:
  print(usage, sep='')
  sys.exit(1)
for opt, arg in opts:
  if opt in ("-h", "--help"):
     print(usage, sep='')
     sys.exit(0)
  if opt in ("-t", "--rnn_type"):
     rnn_type= arg
  elif opt in ["-n", "--neurons"]:
     n_neurons = arg
  elif opt in ["-e", "--epochs"]:
     n_epochs = int(arg)
  elif opt in ["-f", "--results_filename"]:
     results_filename = arg
  elif opt in ["-q", "--quiet"]:
     verbose = False
  else:
     print(usage, sep='')
     sys.exit(1)
if rnn_type == 'lstm' or rnn_type == 'attention':
    pass
else:
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
from causations_rnn_dataset import X_train_shape, y_train_shape, X_train, y_train, X_test_shape, y_test_shape, X_test, y_test, y_test_predictable
if X_train_shape[0] == 0:
    print('Empty train dataset')
    sys.exit(1)
if X_test_shape[0] == 0:
    print('Empty test dataset')
    sys.exit(1)

# Create model.
if rnn_type == 'lstm':
    # LSTM.
    from tensorflow.keras.models import Sequential
    from tensorflow.keras.layers import Input, LSTM, Dense, TimeDistributed
    rnn_model=Sequential()
    rnn_model.add(Input(shape=(X_train_shape[1],1)))
    n=len(n_hidden)
    for i in range(0, n):
        if i < (n - 1):
            rnn_model.add(LSTM(units=n_hidden[i], return_sequences=True))
        else:
            rnn_model.add(LSTM(units=n_hidden[i]))
    rnn_model.add(TimeDistributed(Dense(y_train_shape[2])))
    rnn_model.compile(loss='mean_squared_error',optimizer='adam')
else:
    # Attention.
    from tensorflow.keras import Model
    from attention import Attention
    from tensorflow.keras.layers import Input, LSTM, Dense, TimeDistributed
    model_input = Input(shape=(X_train_shape[1],1))
    x = LSTM(n_hidden[0], return_sequences=True)(model_input)
    n=len(n_hidden)
    for i in range(1, n):
        x = LSTM(units=n_hidden[i], return_sequences=True)(x)
    x = Attention(units=n_hidden[n-1])(x)
    model_output = TimeDistributed(Dense(y_train_shape[2]))(x)
    rnn_model = Model(model_input, model_output)
    rnn_model.compile(loss='mean_squared_error', optimizer='adam')

if verbose:
    rnn_model.summary()

# Train.
seq = array(X_train)
X = seq.reshape(X_train_shape[0], X_train_shape[1], X_train_shape[2])
seq = array(y_train)
y = seq.reshape(y_train_shape[0], y_train_shape[1], y_train_shape[2])
rnn_model.fit(X, y, epochs=n_epochs, batch_size=X_train_shape[0], verbose=int(verbose))

# Validate.
seq = array(X_train)
X = seq.reshape(X_train_shape[0], X_train_shape[1])
seq = array(y_train)
y = seq.reshape(y_train_shape[0], y_train_shape[1])
predictions = rnn.model.predict(X, batch_size=X_train_shape[0], verbose=int(verbose))
trainErrors = 0
trainTotal = 0
for path in range(X_train_shape[0]):
    for step in range(X_train_shape[1]):
        for i in range(len(step)):
            yvals = y[path][step]
            pvals = predictions[path][step]
            if yvals[i] >= threshold and pvals[i] < threshold:
                trainErrors += 1
                break
            if yvals[i] < threshold and pvals[i] >= threshold:
                trainErrors += 1
                break
        trainTotal += 1
trainErrorPct = 0
if trainTotal > 0:
    trainErrorPct = (float(trainErrors) / float(trainTotal)) * 100.0

# Predict.
seq = array(X_test)
X = seq.reshape(X_test_shape[0], X_test_shape[1])
seq = array(y_test)
y = seq.reshape(y_test_shape[0], y_test_shape[1])
predictions = rnn.model.predict(X, batch_size=X_test_shape[0], verbose=int(verbose))
testErrors = 0
testTotal = 0
for path in range(X_test_shape[0]):
    for step in range(X_test_shape[1]):
        for i in range(len(step)):
            yvals = y[path][step]
            pvals = predictions[path][step]
            if yvals[i] >= threshold and pvals[i] < threshold:
                testErrors += 1
                break
            if yvals[i] < threshold and pvals[i] >= threshold:
                testErrors += 1
                break
        testTotal += 1
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
