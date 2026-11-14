# Test Mandala.

if [ "$1" = "" ]
then
   echo "Usage: mandala_test.sh <number of runs> [<causation hierarchies> [args]]"
   exit 1
fi
runs=$1

results_file_name="mandala_test_results_"
results_file_name+=$(date +"%Y-%m-%d_%H-%M-%S")
hierarchies="1 2 3"
if [ "$2" != "" ]
then
   hierarchies=$2
   results_file_name+="_"
   results_file_name+=$hierarchies
fi
args=""
if [ "$3" != "" ]
then
   args=$3
fi
results_file_name+=".csv"
echo "Results written to" $results_file_name

# Parameters:
minNumNonterminals=20
incrNumNonterminals=5
maxNumNonterminals=20
minNumTerminals=15
incrNumTerminals=5
maxNumTerminals=20
minTerminalProductionProbability=.25
incrTerminalProductionProbability=.25
maxTerminalProductionProbability=.75
minMaxInterstitialTerminalSequence=0
incrMaxInterstitialTerminalSequence=5
maxMaxInterstitialTerminalSequence=10

echo causation_hierarchies,num_nonterminals,num_terminals,terminal_production_probability,max_interstitial_terminal_sequence,mandala_activation_threshold,rnn_activation_threshold,attention_activation_threshold,mandala_train_error_pct,rnn_train_error_pct,attention_train_error_pct,mandala_test_error_pct,mandala_test_stdev,rnn_test_error_pct,rnn_test_stdev,attention_test_error_pct,attention_test_stdev > $results_file_name

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
     > mandala_tmp_nn_threshold.txt
     > mandala_tmp_rnn_threshold.txt
     > mandala_tmp_attention_threshold.txt
     > mandala_tmp_nn_train_errors.txt
     > mandala_tmp_rnn_train_errors.txt
     > mandala_tmp_attention_train_errors.txt
     > mandala_tmp_nn_test_errors.txt
     > mandala_tmp_rnn_test_errors.txt
     > mandala_tmp_attention_test_errors.txt
     for i in $(seq $runs)
     do
      random=$RANDOM
      #echo ./mandala.sh $args -numCausationHierarchies $causationHierarchies -numNonterminals $numNonterminals -numTerminals $numTerminals -terminalProductionProbability $terminalProductionProbability -maxInterstitialTerminalSequence $maxInterstitialTerminalSequence -randomSeed $random
      ./mandala.sh $args -numCausationHierarchies $causationHierarchies -numNonterminals $numNonterminals -numTerminals $numTerminals -terminalProductionProbability $terminalProductionProbability -maxInterstitialTerminalSequence $maxInterstitialTerminalSequence -randomSeed $random > mandala_tmp.txt
      grep "Activation threshold" mandala_tmp.txt | cut -d" " -f4 | sed -n '1p' >> mandala_tmp_nn_threshold.txt
      grep "Activation threshold" mandala_tmp.txt | cut -d" " -f4 | sed -n '2p' >> mandala_tmp_rnn_threshold.txt
      grep "Activation threshold" mandala_tmp.txt | cut -d" " -f4 | sed -n '3p' >> mandala_tmp_attention_threshold.txt
      grep "Train prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '1p' >> mandala_tmp_nn_train_errors.txt
      grep "Train prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '2p' >> mandala_tmp_rnn_train_errors.txt
      grep "Train prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '3p' >> mandala_tmp_attention_train_errors.txt
      grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '1p' >> mandala_tmp_nn_test_errors.txt
      grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '2p' >> mandala_tmp_rnn_test_errors.txt
      grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '3p' >> mandala_tmp_attention_test_errors.txt
      rm mandala_tmp.txt
     done
     echo -n ${causationHierarchies},${numNonterminals},${numTerminals},${terminalProductionProbability},${maxInterstitialTerminalSequence} >> $results_file_name
     mandala_threshold=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_nn_threshold.txt`
     rnn_threshold=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_rnn_threshold.txt`
     attention_threshold=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_attention_threshold.txt`
     mandala_train_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_nn_train_errors.txt`
     rnn_train_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_rnn_train_errors.txt`
     attention_train_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_attention_train_errors.txt`
     mandala_test_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_nn_test_errors.txt`
     rnn_test_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_rnn_test_errors.txt`
     attention_test_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_attention_test_errors.txt`
     mandala_test_stdev=`awk '{sum+=$0;a[NR]=$0}END{for(i in a)y+=(a[i]-(sum/NR))^2;print sqrt(y/(NR-1))}' mandala_tmp_nn_test_errors.txt`
     rnn_test_stdev=`awk '{sum+=$0;a[NR]=$0}END{for(i in a)y+=(a[i]-(sum/NR))^2;print sqrt(y/(NR-1))}' mandala_tmp_rnn_test_errors.txt`
     attention_test_stdev=`awk '{sum+=$0;a[NR]=$0}END{for(i in a)y+=(a[i]-(sum/NR))^2;print sqrt(y/(NR-1))}' mandala_tmp_attention_test_errors.txt`
     rm mandala_tmp_nn_threshold.txt
     rm mandala_tmp_rnn_threshold.txt
     rm mandala_tmp_attention_threshold.txt
     rm mandala_tmp_nn_train_errors.txt
     rm mandala_tmp_rnn_train_errors.txt
     rm mandala_tmp_attention_train_errors.txt
     rm mandala_tmp_nn_test_errors.txt
     rm mandala_tmp_rnn_test_errors.txt
     rm mandala_tmp_attention_test_errors.txt
     echo ,${mandala_threshold},${rnn_threshold},${attention_threshold},${mandala_train_error},${rnn_train_error},${attention_train_error},${mandala_test_error},${mandala_test_stdev},${rnn_test_error},${rnn_test_stdev},${attention_test_error},${attention_test_stdev} >> $results_file_name
     cat $results_file_name
    done
   done
  done
 done
done

exit 0

