# Causation discriminator.
# Discriminate relevant features.

import tensorflow as tf
import keras
from keras import layers

# Sizes of cause and effect.
cause_dim = 16
effect_dim = 16

# Hidden layer dimensions.
hidden_dim = 128

# Causations.
num_causations = 2
num_cause_features = 8
num_effect_features = 2

# Dataset size.
dataset_size = 4 # num_causations * 2

# Cause layer.
cause_layer = keras.Input(shape=(cause_dim,))

# Hidden layers.
hidden = layers.Dense(hidden_dim, activation='relu')(cause_layer)
hidden = layers.Dense(hidden_dim, activation='relu')(hidden)

# Effect layer.
effect_layer = layers.Dense(effect_dim, activation='sigmoid')(hidden)

# Causation model maps cause to effect.
causation_model = keras.Model(cause_layer, effect_layer)

# Generate cause and effect training pairs.
# off=0.0, on=1.0
import numpy as np
cause_train = np.zeros((dataset_size, cause_dim))
effect_train = np.zeros((dataset_size, effect_dim))
cause_train[0,0] = 1.0
cause_train[0,2] = 1.0
effect_train[0,0] = 1.0
cause_train[1,0] = 1.0
effect_train[1,0] = 1.0
cause_train[2,1] = 1.0
cause_train[2,3] = 1.0
effect_train[2,1] = 1.0
cause_train[3,1] = 1.0
effect_train[3,1] = 1.0

# Compile model.
causation_model.compile(optimizer='adam', loss='mse')

# Train causation model.
causation_model.fit(cause_train, effect_train,
                epochs=100,
                batch_size=4,
                shuffle=True)

# Generate cause and effect testing pairs.
# off=0.0, on=1.0
cause_test= np.zeros((dataset_size, cause_dim))
effect_test = np.zeros((dataset_size, effect_dim))
cause_test[0,0] = 1.0
cause_test[0,3] = 1.0
effect_test[0,0] = 1.0
cause_test[1,0] = 1.0
effect_test[1,0] = 1.0
cause_test[2,1] = 1.0
cause_test[2,2] = 1.0
effect_test[2,1] = 1.0
cause_test[3,1] = 1.0
effect_test[3,1] = 1.0

# Predict effects from causes.
print('predictions:')
for i in range(dataset_size):
    print('causation=',i,':',sep='')
    cause_instance = np.array([cause_test[i]])
    predicted_instance = causation_model.predict(cause_instance)
    print('cause:', cause_instance)
    print('effect:', np.array([effect_test[i]]))
    print('prediction:', predicted_instance)



