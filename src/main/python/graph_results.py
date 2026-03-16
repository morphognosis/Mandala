#!/usr/bin/env python3
"""
Mandala vs RNN Error Comparison Chart

Installation:
    pip install pandas matplotlib numpy

Usage:
    python graph_results.py [--results_csv <file name>] [--independent_column <column name:category_name_0,category_name_1,...>] [--graph_png <file name>]
"""

import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import sys,getopt

# Options
results_csv = 'mandala_test_results.csv'
independent_column = 'causation_hierarchies:1,2,3'
graph_png = 'mandala_rnn_comparison.png'

# Get options
usage = 'graph_results.py [--results_csv <file name> (default=' + results_csv + ')] [--independent_column <column name with category names appended> (default=' + independent_column + ')] [--graph_png <file name> (default=' + graph_png + ')]'
try:
  opts, args = getopt.getopt(sys.argv[1:],"hf:c:g:",["help","results_csv=","independent_column=","graph_png="])
except getopt.GetoptError:
  print(usage)
  sys.exit(1)
for opt, arg in opts:
  if opt in ("-h", "--help"):
     print(usage)
     sys.exit(0)
  if opt in ("-f", "--results_csv"):
     results_csv = arg
  elif opt in ("-c", "--independent_column"):
     independent_column = arg
  elif opt in ("-g", "--graph_png"):
     graph_png = arg
  else:
     print(usage)
     sys.exit(1)
clist = independent_column.split(":")
if len(clist) != 2:
    print(usage, sep='')
    sys.exit(1)
independent_column = clist[0]
clist = clist[1].split(",")
if len(clist) == 0:
    print(usage, sep='')
    sys.exit(1)
categories = []
for c in clist:
    categories.append(str(c))

# Read the CSV file
df = pd.read_csv(results_csv)
df[independent_column] = df[independent_column].astype(str)

# Group by independent column and calculate averages
grouped = df.groupby(independent_column).agg({
    'mandala_error_pct': 'mean',
    'rnn_error_pct': 'mean'
}).reset_index()
grouped = grouped.sort_values(independent_column)

# Create the bar chart
fig, ax = plt.subplots(figsize=(10, 6))

x = np.arange(len(grouped))
width = 0.35

bars1 = ax.bar(x - width/2, grouped['mandala_error_pct'], width,
               label='Mandala Error %', color='#2E86AB', alpha=0.8)
bars2 = ax.bar(x + width/2, grouped['rnn_error_pct'], width,
               label='RNN Error %', color='#A23B72', alpha=0.8)

# Customize the chart
ax.set_xlabel(independent_column, fontsize=12, fontweight='bold')
ax.set_ylabel('Average Error Percentage (%)', fontsize=12, fontweight='bold')
ax.set_title('Mandala vs RNN: Average Error', fontsize=14, fontweight='bold', pad=20)
ax.set_xticks(x)
ax.set_xticklabels(grouped[independent_column].str.capitalize())
ax.legend(loc='upper right', fontsize=11)

# Add grid
ax.grid(axis='y', alpha=0.3, linestyle='--')
ax.set_axisbelow(True)

# Add value labels on bars
for bars in [bars1, bars2]:
    for bar in bars:
        height = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2., height,
                f'{height:.2f}%',
                ha='center', va='bottom', fontsize=9)

plt.tight_layout()

# Save the figure
plt.savefig(graph_png, dpi=300, bbox_inches='tight')
print("Chart saved to:", graph_png)

# Print summary
print("\nSummary Statistics:")
print(f"{independent_column:<15} {'Mandala Avg':<15} {'RNN Avg':<15} {'Difference':<15}")
print("-" * 60)
for _, row in grouped.iterrows():
    diff = row['rnn_error_pct'] - row['mandala_error_pct']
    print(f"{row[independent_column]:<15} "
          f"{row['mandala_error_pct']:>10.2f}%    "
          f"{row['rnn_error_pct']:>10.2f}%    "
          f"{diff:>10.2f}%")

# Overall statistics
overall_mandala = df['mandala_error_pct'].mean()
overall_rnn = df['rnn_error_pct'].mean()
print("\n" + "=" * 60)
print(f"Overall Average:")
print(f"  Mandala: {overall_mandala:.2f}%")
print(f"  RNN:     {overall_rnn:.2f}%")
print(f"  RNN performs {overall_rnn - overall_mandala:.2f}% worse than Mandala")

sys.exit(0)
