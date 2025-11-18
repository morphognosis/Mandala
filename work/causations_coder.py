# Causations sparse hierarchical coder.

import tensorflow as tf
import keras
from keras import layers
import sys

# Size of encoding.
encoding_dim = 10

# Sizes of cause and effect.
causation_dim = 30

# Hidden layer dimensions.
hidden_dim = 128

# Sparse features.
num_features = 3

# Cause layer.
cause_layer = keras.Input(shape=(causation_dim,))

# Hidden layers.
hidden = layers.Dense(hidden_dim, activation='relu')(cause_layer)
hidden = layers.Dense(hidden_dim, activation='relu')(hidden)

# Effect layer.
effect_layer = layers.Dense(causation_dim, activation='sigmoid')(hidden)

# Causation model maps cause to effect.
causation_model = keras.Model(cause_layer, effect_layer)

# Loss.
def custom_loss(cause_data, effect_data):
    def loss(y_true, y_pred):
        return tf.reduce_mean(tf.square(y_pred - y_true), axis=-1)
    return loss

# Generate input sequence.
# off=0.0, on=1.0
import numpy as np
import random
feature_idxs = []
sequence_length = 4
for sequence in range(sequence_length):
    while True:
        idxs = random.sample(range(0, encoding_dim), num_features)
        idxs.sort()
        print(idxs)      # flibber
        sys.exit(0)      # flibber
        found = False
        for i in feature_idxs:
            if len(list(set(idxs) & set(i))) > 0:
                found = True
                break
        if found == False:
            feature_idxs.append(idxs)
            break
print(feature_idxs)  #flibber
sys.exit(0) # flibber
num_causations = 3
cause_data = np.zeros((num_causations, causation_dim))
effect_data = np.zeros((num_causations, causation_dim))
for row in range(num_causations):
    idxs = [row]
    for i in idxs:
        for j in range(len(feature_idxs[i])):
            idx = feature_idxs[i][j]
            cause_data[row,idx] = 1.0
    idxs = [row + 1]
    for i in idxs:
        for j in range(len(feature_idxs[i])):
            idx = effect_feature_idxs[i][j]
            effect_data[row,idx] = 1.0

# Compile model with a custom loss..
causation_model.compile(optimizer='adam', loss=[custom_loss(cause_data, effect_data), None])

# Train causation model.
causation_model.fit(cause_data, effect_data,
                epochs=100,
                batch_size=256,
                shuffle=True)

# Predict effects from causes.
print('predictions:')
for i in range(num_causations):
    print('causation=',i,':',sep='')
    print('cause feature indexes:', cause_feature_idxs[i])
    print('effect feature indexes:', effect_feature_idxs[i])
    cause_instance = np.array([cause_data[i]])
    coded_instance = coder_model.predict(cause_instance)
    predicted_instance = causation_model.predict(cause_instance)
    print('cause:', cause_instance)
    print('code:', coded_instance)
    print('effect:', np.array([effect_data[i]]))
    print('prediction:', predicted_instance)
i = num_causations
print('causation=all:')
cause_instance = np.array([cause_data[i]])
coded_instance = coder_model.predict(cause_instance)
predicted_instance = causation_model.predict(cause_instance)
print('cause:', cause_instance)
print('code:', coded_instance)
print('effect:', np.array([effect_data[i]]))
print('prediction:', predicted_instance)

sys.exit(0)


