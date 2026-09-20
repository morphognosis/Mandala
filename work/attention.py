import tensorflow as tf
from tensorflow.keras import layers, models
import numpy as np

# 1. Create dummy sequence data: y = sin(x)
def create_dataset(n_samples=1000, n_steps=10):
    X, y = [], []
    for i in range(n_samples):
        start = i * 0.1
        seq = np.sin(start + np.arange(n_steps + 1) * 0.1)
        X.append(seq[:-1])  # Input sequence
        y.append(seq[-1])   # Next step target
    return np.array(X)[..., np.newaxis], np.array(y)

X_train, y_train = create_dataset()

# 2. Build the Model with Keras Attention
n_steps = 10
n_features = 1

# Encoder LSTM (must return sequences to apply attention over time steps)
encoder_inputs = layers.Input(shape=(n_steps, n_features))
encoder_lstm = layers.LSTM(64, return_sequences=True, return_state=True)(encoder_inputs)
encoder_outputs, state_h, state_c = encoder_lstm

# Attention layer query and context setup
# Using the last hidden state as query, and all encoder outputs as keys/values
query = layers.Lambda(lambda x: tf.expand_dims(x[:, -1, :], 1))(encoder_outputs)
attention_out = layers.Attention()([query, encoder_outputs])
attention_context = layers.Lambda(lambda x: tf.squeeze(x, 1))(attention_out)

# Final prediction layers
decoder_combined = layers.Concatenate()([attention_context, state_h])
outputs = layers.Dense(1)(decoder_combined)

model = models.Model(inputs=encoder_inputs, outputs=outputs)
model.compile(optimizer='adam', loss='mse')

# 3. Train the model
model.fit(X_train, y_train, epochs=10, batch_size=32)

# 4. Predict the next step for a new sequence
x_new = np.sin(np.arange(10) * 0.1)[np.newaxis, :, np.newaxis]
next_step_pred = model.predict(x_new)
print("Predicted next value:", next_step_pred)
