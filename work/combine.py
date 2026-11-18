import pandas as pd
import argparse
import sys

parser = argparse.ArgumentParser(description="Combine mandala test results CSVs")
parser.add_argument("csv_in0", help="Input CSV 0")
parser.add_argument("csv_in1", help="Input CSV 1")
parser.add_argument("csv_out", help="Combined CSV")
args = parser.parse_args()
csv_in0 = args.csv_in0
csv_in1 = args.csv_in1
csv_out = args.csv_out

common_cols = ['causation_hierarchies','num_nonterminals','num_terminals','terminal_production_probability','max_interstitial_terminal_sequence']
mandala_act = ['mandala_activation_threshold']
other_act = ['rnn_activation_threshold','attention_activation_threshold']
mandala_train = ['mandala_train_error_pct']
other_train = ['rnn_train_error_pct','attention_train_error_pct']
mandala_test = ['mandala_test_error_pct','mandala_test_stdev']
other_test = ['rnn_test_error_pct','rnn_test_stdev','attention_test_error_pct','attention_test_stdev']

common_df = pd.read_csv(csv_in0, usecols=common_cols)
mandala_act_df = pd.read_csv(csv_in0, usecols=mandala_act)
other_act_df = pd.read_csv(csv_in1, usecols=other_act)
mandala_train_df = pd.read_csv(csv_in0, usecols=mandala_train)
other_train_df = pd.read_csv(csv_in1, usecols=other_train)
mandala_test_df = pd.read_csv(csv_in0, usecols=mandala_test)
other_test_df = pd.read_csv(csv_in1, usecols=other_test)

combined = pd.concat([common_df, mandala_act_df, other_act_df, mandala_train_df, other_train_df, mandala_test_df, other_test_df], axis=1, ignore_index=True)

combined.to_csv(csv_out, index=False)
