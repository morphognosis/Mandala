# Test Mandala.

if [ "$1" = "" ]
then
   echo "Usage: mandala_test.sh <number of runs> [<causation hierarchies>]"
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

echo causation_hierarchies,num_nonterminals,num_terminals,terminal_production_probability,max_interstitial_terminal_sequence,mandala_error_pct,rnn_error_pct,attention_error_pct > $results_file_name

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
     > mandala_tmp_nn.txt
     > mandala_tmp_rnn.txt
     > mandala_tmp_attention.txt
     for i in $(seq $runs)
     do
      random=$RANDOM
      echo ./mandala.sh -numCausationHierarchies $causationHierarchies -numNonterminals $numNonterminals -numTerminals $numTerminals -terminalProductionProbability $terminalProductionProbability -maxInterstitialTerminalSequence $maxInterstitialTerminalSequence -randomSeed $random
      ./mandala.sh -numCausationHierarchies $causationHierarchies -numNonterminals $numNonterminals -numTerminals $numTerminals -terminalProductionProbability $terminalProductionProbability -maxInterstitialTerminalSequence $maxInterstitialTerminalSequence -randomSeed $random > mandala_tmp.txt
      grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '1p' >> mandala_tmp_nn.txt
      grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '2p' >> mandala_tmp_rnn.txt
      grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '3p' >> mandala_tmp_attention.txt
      rm mandala_tmp.txt
     done
     echo -n ${causationHierarchies},${numNonterminals},${numTerminals},${terminalProductionProbability},${maxInterstitialTerminalSequence} >> $results_file_name
     mandala_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_nn.txt`
     rnn_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_rnn.txt`
     attention_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_attention.txt`
     rm mandala_tmp_nn.txt
     rm mandala_tmp_rnn.txt
     rm mandala_tmp_attention.txt
     echo ,${mandala_error},${rnn_error},${attention_error} >> $results_file_name
    done
   done
  done
 done
done

exit 0

