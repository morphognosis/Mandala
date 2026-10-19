# Test Mandala.

if [ "$1" = "" ]
then
   echo "Usage: mandala_test.sh <number of runs> [<causation hierarchies>] [args]"
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
minNumNonterminals=10
incrNumNonterminals=5
maxNumNonterminals=20
minNumTerminals=10
incrNumTerminals=5
maxNumTerminals=20
minTerminalProductionProbability=.25
incrTerminalProductionProbability=.25
maxTerminalProductionProbability=.75
minMaxInterstitialTerminalSequence=0
incrMaxInterstitialTerminalSequence=5
maxMaxInterstitialTerminalSequence=10

echo causation_hierarchies,num_nonterminals,num_terminals,terminal_production_probability,max_interstitial_terminal_sequence,mandala_error_pct,rnn_error_pct,attention_error_pct,mandala_activation_threshold,rnn_activation_threshold,attention_activation_threshold > $results_file_name

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
     > mandala_tmp_nn_errors.txt
     > mandala_tmp_rnn_errors.txt
     > mandala_tmp_attention_errors.txt
     > mandala_tmp_nn_threshold.txt
     > mandala_tmp_rnn_threshold.txt
     > mandala_tmp_attention_threshold.txt
     for i in $(seq $runs)
     do
      random=$RANDOM
      echo ./mandala.sh $args -numCausationHierarchies $causationHierarchies -numNonterminals $numNonterminals -numTerminals $numTerminals -terminalProductionProbability $terminalProductionProbability -maxInterstitialTerminalSequence $maxInterstitialTerminalSequence -randomSeed $random
      ./mandala.sh $args -numCausationHierarchies $causationHierarchies -numNonterminals $numNonterminals -numTerminals $numTerminals -terminalProductionProbability $terminalProductionProbability -maxInterstitialTerminalSequence $maxInterstitialTerminalSequence -randomSeed $random > mandala_tmp.txt
      grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '1p' >> mandala_tmp_nn_errors.txt
      grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '2p' >> mandala_tmp_rnn_errors.txt
      grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '3p' >> mandala_tmp_attention_errors.txt
      grep "Activation threshold" mandala_tmp.txt | cut -d" " -f4 | sed -n '1p' >> mandala_tmp_nn_threshold.txt
      grep "Activation threshold" mandala_tmp.txt | cut -d" " -f4 | sed -n '2p' >> mandala_tmp_rnn_threshold.txt
      grep "Activation threshold" mandala_tmp.txt | cut -d" " -f4 | sed -n '3p' >> mandala_tmp_attention_threshold.txt
      rm mandala_tmp.txt
     done
     echo -n ${causationHierarchies},${numNonterminals},${numTerminals},${terminalProductionProbability},${maxInterstitialTerminalSequence} >> $results_file_name
     mandala_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_nn_errors.txt`
     rnn_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_rnn_errors.txt`
     attention_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_attention_errors.txt`
     mandala_threshold=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_nn_threshold.txt`
     rnn_threshold=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_rnn_threshold.txt`
     attention_threshold=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_attention_threshold.txt`
     rm mandala_tmp_nn_errors.txt
     rm mandala_tmp_rnn_errors.txt
     rm mandala_tmp_attention_errors.txt
     rm mandala_tmp_nn_threshold.txt
     rm mandala_tmp_rnn_threshold.txt
     rm mandala_tmp_attention_threshold.txt
     echo ,${mandala_error},${rnn_error},${attention_error},${mandala_threshold},${rnn_threshold},${attention_threshold} >> $results_file_name
    done
   done
  done
 done
done

exit 0

