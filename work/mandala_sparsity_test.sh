# Test Mandala sparsity for number of features 1, 3, and 5.

if [ $# -lt 2 ]
then
   echo "Usage: mandala_sparsity_test.sh <model> (mandala, RNN, or attention) <number of runs> [<causation hierarchies> or \"all\" [args]]"
   exit 1
fi
model=$1
if [ "$model" = "mandala" -o "$model" = "Mandala" ]
then
    model="mandala"
    args+=" -RNNepochs 0 -AttentionEpochs 0"
elif [ "$model" = "RNN" -o "$model" = "rnn" ]
then
    model="RNN"
    args+=" -NNepochs 0 -AttentionEpochs 0"
elif [ "$model" = "attention" -o "$model" = "Attention" ]
then
    model="attention"
    args+=" -NNepochs 0 -RNNepochs 0"
else
   echo "Usage: mandala_sparsity_test.sh <model> (mandala, RNN, or attention) <number of runs> [<causation hierarchies> or \"all\" [args]]"
   exit 1
fi
runs=$2
results_file_name="mandala_sparsity_test_results_"
results_file_name+=$(date +"%Y-%m-%d_%H-%M-%S")
hierarchies="1 2 3"
if [ "$3" != "" -a "$3" != "all" ]
then
   hierarchies=$3
   results_file_name+="_"
   results_file_name+=$hierarchies
fi
results_file_name+=".csv"
echo "Results written to" $results_file_name
args=""
if [ "$4" != "" ]
then
   args=$4
fi

# Parameters:
minNumNonterminals=10
incrNumNonterminals=5
maxNumNonterminals=20
minNumTerminals=10
incrNumTerminals=5
maxNumTerminals=20
minTerminalProductionProbability=.25
incrTerminalProductionProbability=.25
maxTerminalProductionProbability=.5
minMaxInterstitialTerminalSequence=0
incrMaxInterstitialTerminalSequence=5
maxMaxInterstitialTerminalSequence=10

echo causation_hierarchies,num_nonterminals,num_terminals,terminal_production_probability,max_interstitial_terminal_sequence,activation_threshold,one_features_train_error_pct,three_features_train_error_pct,five_features_train_error_pct,one_features_test_error_pct,one_features_test_stdev,three_features_test_error_pct,three_features_test_stdev,five_features_test_error_pct,five_features_test_stdev > $results_file_name

for causationHierarchies in $hierarchies
do
 for numNonterminals in $(seq $minNumNonterminals $incrNumNonterminals $maxNumNonterminals)
 do
  for numTerminals in $(seq $minNumTerminals $incrNumTerminals $maxNumTerminals)
  do
   for terminalProductionProbability in $(seq $minTerminalProductionProbability $incrTerminalProductionProbability $maxTerminalProductionProbability)
   do
    for maxInterstitialTerminalSequence in $(seq $minMaxInterstitialTerminalSequence $incrMaxInterstitialTerminalSequence $maxMaxInterstitialTerminalSequence)
    do
     > mandala_tmp_one_features_threshold.txt
     > mandala_tmp_one_features_train_errors.txt
     > mandala_tmp_one_features_test_errors.txt
     for i in $(seq $runs)
     do
      random=$RANDOM
      #echo ./mandala.sh -numFeatures 1 ${args} -numCausationHierarchies $causationHierarchies -numNonterminals $numNonterminals -numTerminals $numTerminals -terminalProductionProbability $terminalProductionProbability -maxInterstitialTerminalSequence $maxInterstitialTerminalSequence -randomSeed $random
      ./mandala.sh -numFeatures 1 ${args} -numCausationHierarchies $causationHierarchies -numNonterminals $numNonterminals -numTerminals $numTerminals -terminalProductionProbability $terminalProductionProbability -maxInterstitialTerminalSequence $maxInterstitialTerminalSequence -randomSeed $random > mandala_tmp.txt
      if [ "$model" = "mandala" ]
      then
        grep "Activation threshold" mandala_tmp.txt | cut -d" " -f4 | sed -n '1p' >> mandala_tmp_one_features_threshold.txt
        grep "Train prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '1p' >> mandala_tmp_one_features_train_errors.txt
        grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '1p' >> mandala_tmp_one_features_test_errors.txt
      elif [ "$model" = "RNN" ]
      then
        grep "Activation threshold" mandala_tmp.txt | cut -d" " -f4 | sed -n '2p' >> mandala_tmp_one_features_threshold.txt
        grep "Train prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '2p' >> mandala_tmp_one_features_train_errors.txt
        grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '2p' >> mandala_tmp_one_features_test_errors.txt
      elif [ "$model" = "attention" ]
      then
        grep "Activation threshold" mandala_tmp.txt | cut -d" " -f4 | sed -n '3p' >> mandala_tmp_one_features_threshold.txt
        grep "Train prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '3p' >> mandala_tmp_one_features_train_errors.txt
        grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '3p' >> mandala_tmp_one_features_test_errors.txt
      fi
      rm mandala_tmp.txt
     done
     > mandala_tmp_three_features_threshold.txt
     > mandala_tmp_three_features_train_errors.txt
     > mandala_tmp_three_features_test_errors.txt
     for i in $(seq $runs)
     do
      random=$RANDOM
      #echo ./mandala.sh -numFeatures 3 ${args} -numCausationHierarchies $causationHierarchies -numNonterminals $numNonterminals -numTerminals $numTerminals -terminalProductionProbability $terminalProductionProbability -maxInterstitialTerminalSequence $maxInterstitialTerminalSequence -randomSeed $random
      ./mandala.sh -numFeatures 3 ${args} -numCausationHierarchies $causationHierarchies -numNonterminals $numNonterminals -numTerminals $numTerminals -terminalProductionProbability $terminalProductionProbability -maxInterstitialTerminalSequence $maxInterstitialTerminalSequence -randomSeed $random > mandala_tmp.txt
      if [ "$model" = "mandala" ]
      then
        grep "Activation threshold" mandala_tmp.txt | cut -d" " -f4 | sed -n '1p' >> mandala_tmp_three_features_threshold.txt
        grep "Train prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '1p' >> mandala_tmp_three_features_train_errors.txt
        grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '1p' >> mandala_tmp_three_features_test_errors.txt
      elif [ "$model" = "RNN" ]
      then
        grep "Activation threshold" mandala_tmp.txt | cut -d" " -f4 | sed -n '2p' >> mandala_tmp_three_features_threshold.txt
        grep "Train prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '2p' >> mandala_tmp_three_features_train_errors.txt
        grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '2p' >> mandala_tmp_three_features_test_errors.txt
      elif [ "$model" = "attention" ]
      then
        grep "Activation threshold" mandala_tmp.txt | cut -d" " -f4 | sed -n '3p' >> mandala_tmp_three_features_threshold.txt
        grep "Train prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '3p' >> mandala_tmp_three_features_train_errors.txt
        grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '3p' >> mandala_tmp_three_features_test_errors.txt
      fi
      rm mandala_tmp.txt
     done
     > mandala_tmp_five_features_threshold.txt
     > mandala_tmp_five_features_train_errors.txt
     > mandala_tmp_five_features_test_errors.txt
     for i in $(seq $runs)
     do
      random=$RANDOM
      #echo ./mandala.sh -numFeatures 5 ${args} -numCausationHierarchies $causationHierarchies -numNonterminals $numNonterminals -numTerminals $numTerminals -terminalProductionProbability $terminalProductionProbability -maxInterstitialTerminalSequence $maxInterstitialTerminalSequence -randomSeed $random
      ./mandala.sh -numFeatures 5 ${args} -numCausationHierarchies $causationHierarchies -numNonterminals $numNonterminals -numTerminals $numTerminals -terminalProductionProbability $terminalProductionProbability -maxInterstitialTerminalSequence $maxInterstitialTerminalSequence -randomSeed $random > mandala_tmp.txt
      if [ "$model" = "mandala" ]
      then
        grep "Activation threshold" mandala_tmp.txt | cut -d" " -f4 | sed -n '1p' >> mandala_tmp_five_features_threshold.txt
        grep "Train prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '1p' >> mandala_tmp_five_features_train_errors.txt
        grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '1p' >> mandala_tmp_five_features_test_errors.txt
      elif [ "$model" = "RNN" ]
      then
        grep "Activation threshold" mandala_tmp.txt | cut -d" " -f4 | sed -n '2p' >> mandala_tmp_five_features_threshold.txt
        grep "Train prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '2p' >> mandala_tmp_five_features_train_errors.txt
        grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '2p' >> mandala_tmp_five_features_test_errors.txt
      elif [ "$model" = "attention" ]
      then
        grep "Activation threshold" mandala_tmp.txt | cut -d" " -f4 | sed -n '3p' >> mandala_tmp_five_features_threshold.txt
        grep "Train prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '3p' >> mandala_tmp_five_features_train_errors.txt
        grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '3p' >> mandala_tmp_five_features_test_errors.txt
      fi
      rm mandala_tmp.txt
     done
     echo -n ${causationHierarchies},${numNonterminals},${numTerminals},${terminalProductionProbability},${maxInterstitialTerminalSequence} >> $results_file_name
     activation_threshold=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_one_features_threshold.txt`
     one_features_train_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_one_features_train_errors.txt`
     three_features_train_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_three_features_train_errors.txt`
     five_features_train_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_five_features_train_errors.txt`
     one_features_test_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_one_features_test_errors.txt`
     three_features_test_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_three_features_test_errors.txt`
     five_features_test_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_five_features_test_errors.txt`
     if [ `cat mandala_tmp_one_features_test_errors.txt | wc -l` -gt 1 ]
     then
       one_features_test_stdev=`awk '{sum+=$0;a[NR]=$0}END{for(i in a)y+=(a[i]-(sum/NR))^2;print sqrt(y/(NR-1))}' mandala_tmp_one_features_test_errors.txt`
     else
       one_features_test_stdev=0
     fi
     if [ `cat mandala_tmp_three_features_test_errors.txt | wc -l` -gt 1 ]
     then
       three_features_test_stdev=`awk '{sum+=$0;a[NR]=$0}END{for(i in a)y+=(a[i]-(sum/NR))^2;print sqrt(y/(NR-1))}' mandala_tmp_three_features_test_errors.txt`
     else
       three_features_test_stdev=0
     fi
     if [ `cat mandala_tmp_five_features_test_errors.txt | wc -l` -gt 1 ]
     then
       five_features_test_stdev=`awk '{sum+=$0;a[NR]=$0}END{for(i in a)y+=(a[i]-(sum/NR))^2;print sqrt(y/(NR-1))}' mandala_tmp_five_features_test_errors.txt`
     else
       five_features_test_stdev=0
     fi
     rm mandala_tmp_one_features_threshold.txt
     rm mandala_tmp_three_features_threshold.txt
     rm mandala_tmp_five_features_threshold.txt
     rm mandala_tmp_one_features_train_errors.txt
     rm mandala_tmp_three_features_train_errors.txt
     rm mandala_tmp_five_features_train_errors.txt
     rm mandala_tmp_one_features_test_errors.txt
     rm mandala_tmp_three_features_test_errors.txt
     rm mandala_tmp_five_features_test_errors.txt
     echo ,${activation_threshold},${one_features_train_error},${three_features_train_error},${five_features_train_error},${one_features_test_error},${one_features_test_stdev},${three_features_test_error},${three_features_test_stdev},${five_features_test_error},${five_features_test_stdev} >> $results_file_name
     cat $results_file_name
    done
   done
  done
 done
done

exit 0

