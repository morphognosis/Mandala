# Test Mandala.

if [ "$1" = "" ]
then
   echo "Usage: mandala_test.sh <number of runs>"
   exit 1
fi
runs=$1

echo "Results written to mandala_test_results.csv"

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
minContextTierValueDurationType=0
incrContextTierValueDurationType=1
maxContextTierValueDurationType=2

echo causation_hierarchies,num_nonterminals,num_terminals,terminal_production_probability,context_tier_value_duration_type,nn_error_pct,rnn_error_pct > mandala_test_results.csv

for causationHierarchies in 2 1 3
do
 for numNonterminals in $(seq $minNumNonterminals $incrNumNonterminals $maxNumNonterminals)
 do
  for numTerminals in $(seq $minNumTerminals $incrNumTerminals $maxNumTerminals)
  do
   for terminalProductionProbability in $(seq $minTerminalProductionProbability $incrTerminalProductionProbability $maxTerminalProductionProbability)
   do
    for contextTierValueDurationType in $(seq $minContextTierValueDurationType $incrContextTierValueDurationType $maxContextTierValueDurationType)
    do
     > mandala_tmp_nn.txt
     > mandala_tmp_rnn.txt
     for i in $(seq $runs)
     do
      if [ $contextTierValueDurationType -eq 0 ]
      then
       type=minimum
      elif [ $contextTierValueDurationType -eq 1 ]
      then
       type=expected
      else
       type=maximum
      fi
      random=$RANDOM
      echo ./mandala.sh -numCausationHierarchies $causationHierarchies -numNonterminals $numNonterminals -numTerminals $numTerminals -terminalProductionProbability $terminalProductionProbability -contextTierValueDurationType $type -randomSeed $random
      ./mandala.sh -numCausationHierarchies $causationHierarchies -numNonterminals $numNonterminals -numTerminals $numTerminals -terminalProductionProbability $terminalProductionProbability -contextTierValueDurationType $type -randomSeed $random > mandala_tmp.txt
      grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | head -1 >> mandala_tmp_nn.txt
      grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | tail -1 >> mandala_tmp_rnn.txt
      rm mandala_tmp.txt
     done
     echo -n ${causationHierarchies},${numNonterminals},${numTerminals},${terminalProductionProbability},${type} >> mandala_test_results.csv
     nn_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_nn.txt`
     rnn_error=`awk '{ total += $1; count++ } END { print total/count }' mandala_tmp_rnn.txt`
     rm mandala_tmp_nn.txt
     rm mandala_tmp_rnn.txt
     echo ,${nn_error},${rnn_error} >> mandala_test_results.csv
    done
   done
  done
 done
done

exit 0

