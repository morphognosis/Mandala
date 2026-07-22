#!/usr/bin/env python3
"""
Mandala vs RNN Error Comparison Chart

Installation:
    pip install pandas matplotlib numpy

Usage:
    python chart_results.py [--results_csv <file name>] [--independent_column <column name:category_name_0,category_name_1,...>] [--x_axis_label <string> (default=no label)] [--graph_png <file name>]
"""

import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import sys,getopt

# Options
results_csv = 'mandala_test_results.csv'
independent_column = None
xlabel = None
graph_png = 'mandala_rnn_comparison.png'

# Get options
usage = 'chart_results.py [--results_csv <file name> (default=' + results_csv + ')] [--independent_column <column name with category names appended> (example=causation_hierarchies:1,2,3)] [--x_axis_label <string> (default=no label)] [--graph_png <file name> (default=' + graph_png + ')]'
try:
  opts, args = getopt.getopt(sys.argv[1:],"hf:c:x:g:",["help","results_csv=","independent_column=","x_axis_label=","graph_png="])
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
  elif opt in ("-x", "--x_axis_label"):
     xlabel = arg
  elif opt in ("-g", "--graph_png"):
     graph_png = arg
  else:
     print(usage)
     sys.exit(1)
if independent_column != None:
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

# Graph by column?
if independent_column != None:

    # Calculate averages
    df[independent_column] = df[independent_column].astype(str)
    data = df.groupby(independent_column).agg({
        'mandala_error_pct': 'mean',
        'rnn_error_pct': 'mean'
    }).reset_index()
    category_order = {name: i for i, name in enumerate(categories)}
    data = data.sort_values(independent_column, key=lambda col: col.map(category_order))

    # Create the bar chart
    fig, ax = plt.subplots(figsize=(10, 6))
    
    # Create bars
    x = np.arange(len(data))
    width = 0.35
    bar1 = ax.bar(x - width/2, data['mandala_error_pct'], width,
                   label='Mandala Error %', color='#2E86AB', alpha=0.8)
    bar2 = ax.bar(x + width/2, data['rnn_error_pct'], width,
                   label='RNN Error %', color='#A23B72', alpha=0.8)
    for bars in [ bar1, bar2 ]:
        for bar in bars:
            height = bar.get_height()
            ax.text(bar.get_x() + bar.get_width()/2., height,
                    f'{height:.2f}%',
                    ha='center', va='bottom', fontsize=9)
    
    # Customize the chart
    ax.set_ylabel('Average Error Percentage (%)', fontsize=12, fontweight='bold')
    #ax.set_title('Mandala vs RNN: Average Error', fontsize=14, fontweight='bold', pad=20)
    if xlabel != None:
        ax.set_xlabel(xlabel, fontsize=12, fontweight='bold')
    ax.set_xticks(x)
    ax.set_xticklabels(data[independent_column].str.capitalize())
    ax.legend(loc='upper right', fontsize=11)

    # Add grid
    ax.grid(axis='y', alpha=0.3, linestyle='--')
    ax.set_axisbelow(True)

    # Print summary
    print("\nSummary Statistics:")
    print(f"{independent_column:<15} {'Mandala Avg':<15} {'RNN Avg':<15} {'Difference':<15}")
    print("-" * 60)
    for _, row in data.iterrows():
        diff = row['rnn_error_pct'] - row['mandala_error_pct']
        print(f"{row[independent_column]:<15} "
              f"{row['mandala_error_pct']:>10.2f}%    "
              f"{row['rnn_error_pct']:>10.2f}%    "
              f"{diff:>10.2f}%")

else:

    # Calculate overall averages
    mandala_avg = df['mandala_error_pct'].mean()
    rnn_avg = df['rnn_error_pct'].mean()
    
    # Create simple data
    labels = ['Mandala', 'RNN']
    values = [mandala_avg, rnn_avg]
    
    # Create the bar chart
    fig, ax = plt.subplots(figsize=(8, 6))

    # Create bars
    bars = ax.bar(labels, values, color=['#2E86AB', '#A23B72'], alpha=0.8, width=0.5)
    for bar in bars:
        height = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2., height,
                f'{height:.2f}%',
                ha='center', va='bottom', fontsize=12, fontweight='bold')

    # Customize the chart
    ax.set_ylabel('Average Error Percentage (%)', fontsize=12, fontweight='bold')
    #ax.set_title('Mandala vs RNN: Overall Average Error', fontsize=14, fontweight='bold', pad=20)
    if xlabel != None:
        ax.set_xlabel(xlabel, fontsize=12, fontweight='bold')
    ax.grid(axis='y', alpha=0.3, linestyle='--')
    ax.set_axisbelow(True)

# Save the figure
plt.savefig(graph_png, dpi=300, bbox_inches='tight')
print("Chart saved to:", graph_png)

# Overall statistics
overall_mandala = df['mandala_error_pct'].mean()
overall_rnn = df['rnn_error_pct'].mean()
print("\n" + "=" * 60)
print(f"Overall Average:")
print(f"  Mandala: {overall_mandala:.2f}%")
print(f"  RNN:     {overall_rnn:.2f}%")
print(f"  RNN performs {overall_rnn - overall_mandala:.2f}% worse than Mandala")

# Show figure
plt.tight_layout()
plt.show()

sys.exit(0)
