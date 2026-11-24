#!/usr/bin/env python3
"""
Sparsity error chart

Installation:
    pip install pandas matplotlib numpy

Usage:
    python chart_sparsity_results.py [--results_csv <file name>] [--independent_column <column name:category_name_0,category_name_1,...>] [--x_axis_label <string> (default=no label)] [--graph_png <file name>]
"""

import pandas as pd
import matplotlib.pyplot as plt
import numpy as np
import sys,getopt

# Options
results_csv = 'mandala_sparsity_test_results.csv'
no_title = False
independent_column = None
xlabel = None
graph_png = None

# Get options
usage = 'chart_sparsity_results.py [--results_csv <file name> (default=' + results_csv + ')] [--no-title] [--independent_column <column name with category names appended> (example=causation_hierarchies:1,2,3)] [--x_axis_label <string> (default=no label)] [--graph_png <file name>]'
try:
  opts, args = getopt.getopt(sys.argv[1:],"hnf:c:x:g:",["help","no_title","results_csv=","independent_column=","x_axis_label=","graph_png="])
except getopt.GetoptError:
  print(usage)
  sys.exit(1)
for opt, arg in opts:
  if opt in ("-h", "--help"):
     print(usage)
     sys.exit(0)
  if opt in ("-f", "--results_csv"):
     results_csv = arg
  elif opt in ("-n", "--no_title"):
     no_title = True
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
        'one_features_test_error_pct': 'mean',
        'three_features_test_error_pct': 'mean',
        'five_features_test_error_pct': 'mean'
    }).reset_index()
    category_order = {name: i for i, name in enumerate(categories)}
    data = data.sort_values(independent_column, key=lambda col: col.map(category_order))

    # Create the bar chart
    fig, ax = plt.subplots(figsize=(10, 6))

    # Create bars
    x = np.arange(len(data))
    width = 0.25
    bar1 = ax.bar(x - width, data['one_features_test_error_pct'], width,
                   label='One feature error %', color='#2E86AB', alpha=0.8)
    bar2 = ax.bar(x, data['three_features_test_error_pct'], width,
                   label='Three features error %', color='#A23B72', alpha=0.8)
    bar3 = ax.bar(x + width, data['five_features_test_error_pct'], width,
                   label='Five features error %', color='#F18F01', alpha=0.8)
    for bars in [ bar1, bar2, bar3 ]:
        for bar in bars:
            height = bar.get_height()
            ax.text(bar.get_x() + bar.get_width()/2., height,
                    f'{height:.2f}%',
                    ha='center', va='bottom', fontsize=9)
    
    # Customize the chart
    if no_title == False:
        ax.set_title(results_csv, fontsize=14, fontweight='bold', pad=20)
    ax.set_ylabel('Average error percentage (%)', fontsize=12, fontweight='bold')
    if xlabel != None:
        xlabel = independent_column
    ax.set_xlabel(xlabel, fontsize=12, fontweight='bold')
    ax.set_xticks(x)
    ax.set_xticklabels(data[independent_column].str.capitalize())
    ax.legend(loc='upper right', fontsize=11)

    # Add grid
    ax.grid(axis='y', alpha=0.3, linestyle='--')
    ax.set_axisbelow(True)

    # Print summary
    print("\nSummary Statistics:")
    print(f"{independent_column:<15} {'One feature avg':<15} {'Three features avg':<15} {'Five features avg':<15} {'Three features diff':<15} {'Five features diff':<15}")
    print("-" * 90)
    for _, row in data.iterrows():
        three_features_diff = row['three_features_test_error_pct'] - row['one_features_test_error_pct']
        five_features_diff = row['five_features_test_error_pct'] - row['one_features_test_error_pct']
        print(f"{row[independent_column]:<15} "
              f"{row['one_features_test_error_pct']:>10.2f}%    "
              f"{row['three_features_test_error_pct']:>10.2f}%    "
              f"{row['five_features_test_error_pct']:>10.2f}%    "
              f"{three_features_diff:>10.2f}%    "
              f"{five_features_diff:>10.2f}%")

else:

    # Calculate overall averages
    one_features_avg = df['one_features_test_error_pct'].mean()
    three_features_avg = df['three_features_test_error_pct'].mean()
    five_features_avg = df['five_features_test_error_pct'].mean()

    # Create simple data
    labels = ['One feature', 'Three features', 'Five features']
    values = [one_features_avg, three_features_avg, five_features_avg]

    # Create the bar chart
    fig, ax = plt.subplots(figsize=(8, 6))

    # Create bars
    bars = ax.bar(labels, values, color=['#2E86AB', '#A23B72', '#F18F01'], alpha=0.8, width=0.5)
    for bar in bars:
        height = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2., height,
                f'{height:.2f}%',
                ha='center', va='bottom', fontsize=12, fontweight='bold')

    # Customize the chart
    if no_title == False:
        ax.set_title(results_csv, fontsize=14, fontweight='bold', pad=20)
    ax.set_ylabel('Average error percentage (%)', fontsize=12, fontweight='bold')
    if xlabel != None:
        ax.set_xlabel(xlabel, fontsize=12, fontweight='bold')
    ax.grid(axis='y', alpha=0.3, linestyle='--')
    ax.set_axisbelow(True)

# Save the figure?
if graph_png != None:
    plt.savefig(graph_png, dpi=300, bbox_inches='tight')
    print("Chart saved to:", graph_png)

# Overall statistics
overall_one_features = df['one_features_test_error_pct'].mean()
overall_three_features = df['three_features_test_error_pct'].mean()
overall_five_features = df['five_features_test_error_pct'].mean()
print("\n" + "=" * 60)
print(f"Overall Average:")
print(f"  One feature:   {overall_one_features:.2f}%")
print(f"  Three features:       {overall_three_features:.2f}%")
print(f"  Five features: {overall_five_features:.2f}%")
print(f"  Three features performs {overall_three_features - overall_one_features:.2f}% worse than one feature")
print(f"  Five features performs {overall_five_features - overall_one_features:.2f}% worse than one feature")

# Show figure
plt.tight_layout()
plt.show()

sys.exit(0)
