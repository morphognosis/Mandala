# Sparse causation coder.
# Map input cause to output effect using a sparse encoding:
# 1. Choose sparse code.
# 2. Train cause to produce code.
# 3. Train code to produce effect.

import keras
from keras import layers

# Sizes of cause, code, and effect.
cause_dim = 16
code_dim = 32
effect_dim = 16

# Causations.
num_causations = 2
num_cause_features = 2
num_code_features = 3
num_effect_features = 2

# Hidden layer dimensions.
hidden_dim = 128

# Dataset size.
dataset_size = num_causations + 1

# Cause layer.
cause_layer = keras.Input(shape=(cause_dim,))

# Hidden encode layers.
encode_hidden = layers.Dense(hidden_dim, activation='relu')(cause_layer)
encode_hidden = layers.Dense(hidden_dim, activation='relu')(encode_hidden)

# Encode layer.
encode_layer = layers.Dense(code_dim, activation='sigmoid')(encode_hidden)

# Encode model maps cause to code.
encode_model = keras.Model(cause_layer, encode_layer)

# Code layer.
code_layer = keras.Input(shape=(code_dim,))

# Hidden decode layers.
decode_hidden = layers.Dense(hidden_dim, activation='relu')(code_layer)
decode_hidden = layers.Dense(hidden_dim, activation='relu')(decode_hidden)

# Effect layer.
effect_layer = layers.Dense(effect_dim, activation='sigmoid')(decode_hidden)

# Decode model maps code to effect.
decode_model = keras.Model(code_layer, effect_layer)

# Compile models with a mean squared error loss and Adam optimizer.
encode_model.compile(optimizer='adam', loss='mse')
decode_model.compile(optimizer='adam', loss='mse')

# Generate cause, code, and effect triplets.
# off=0.0, on=1.0
import numpy as np
cause_data = np.zeros((dataset_size, cause_dim))
code_data = np.zeros((dataset_size, code_dim))
effect_data = np.zeros((dataset_size, effect_dim))
import random
cause_feature_idxs = []
for _ in range(num_causations):
    while True:
        idxs = random.sample(range(0, cause_dim), num_cause_features)
        idxs.sort()
        found = False
        for i in cause_feature_idxs:
            if len(list(set(idxs) & set(i))) > 0:
                found = True
                break
        if found == False:
            cause_feature_idxs.append(idxs)
            break
code_feature_idxs = []
for _ in range(num_causations):
    while True:
        idxs = random.sample(range(0, code_dim), num_code_features)
        idxs.sort()
        found = False
        for i in code_feature_idxs:
            if len(list(set(idxs) & set(i))) > 0:
                found = True
                break
        if found == False:
            code_feature_idxs.append(idxs)
            break
effect_feature_idxs = []
for _ in range(num_causations):
    while True:
        idxs = random.sample(range(0, effect_dim), num_effect_features)
        idxs.sort()
        found = False
        for i in effect_feature_idxs:
            if len(list(set(idxs) & set(i))) > 0:
                found = True
                break
        if found == False:
            effect_feature_idxs.append(idxs)
            break
for row in range(dataset_size):
    idxs = [row]
    if row >= num_causations:
        idxs = []
        for i in range(num_causations):
            idxs.append(i)
    for i in idxs:
        for j in range(len(cause_feature_idxs[i])):
            idx = cause_feature_idxs[i][j]
            cause_data[row,idx] = 1.0
        for j in range(len(code_feature_idxs[i])):
            idx = code_feature_idxs[i][j]
            code_data[row,idx] = 1.0
        for j in range(len(effect_feature_idxs[i])):
            idx = effect_feature_idxs[i][j]
            effect_data[row,idx] = 1.0

# Train encode model.
encode_model.fit(cause_data, code_data,
                epochs=100,
                batch_size=256,
                shuffle=True)

# Predict codes from causes.
print('encode predictions:')
encode_data = np.zeros((dataset_size, code_dim))
for i in range(num_causations):
    print('causation=',i,':',sep='')
    print('cause feature indexes:', cause_feature_idxs[i])
    print('code feature indexes:', code_feature_idxs[i])
    cause_instance = np.array([cause_data[i]])
    encoded_instance = encode_model.predict(cause_instance)
    print('cause:', cause_instance)
    print('code:', np.array([code_data[i]]))
    print('prediction:', encoded_instance)
    encode_data[i] = encoded_instance

# Train decode model using encoded data.
decode_model.fit(encode_data, effect_data,
                epochs=100,
                batch_size=256,
                shuffle=True)

# Predict effects from codes.
print('decode predictions:')
for i in range(num_causations):
    print('causation=',i,':',sep='')
    print('code feature indexes:', code_feature_idxs[i])
    print('effect feature indexes:', effect_feature_idxs[i])
    code_instance = np.array([code_data[i]])
    decoded_instance = decode_model.predict(code_instance)
    print('code:', code_instance)
    print('effect:', np.array([effect_data[i]]))
    print('prediction:', decoded_instance)
    
# Predict effects from combined codes.
print('decode combined predictions:')
code_instance = np.array([code_data[0]])
for i in range(1,num_causations):
    code_temp = np.array([code_data[i]])
    for j in range(code_dim):
        if code_temp[0,j] > .9:
            code_instance[0,j] = 1
decoded_instance = decode_model.predict(code_instance)
print('code:', code_instance)
print('prediction:', decoded_instance)
