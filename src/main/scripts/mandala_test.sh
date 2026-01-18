# Test Mandala.

if [ "$1" = "" ]
then
   echo "Usage: mandala_test.sh <number of runs>"
   exit 1
fi
runs=$1

# Parameters:
minCausationHierarchies=1
incrCausationHierarchies=1
maxCausationHierarchies=3
minNumNonterminals=10
incrNumNonterminals=5
maxNumNonterminals=20
minNumTerminals=20
incrNumTerminals=5
maxNumTerminals=30
minTerminalProductionProbability=.25
incrTerminalProductionProbability=.25
maxTerminalProductionProbability=.75
minFeatureValueDurationType=0
incrFeatureValueDurationType=1
maxFeatureValueDurationType=2

echo causation_hierarchies,num_nonterminals,num_terminals,terminal_production_probability,feature_value_duration_type,nn_error_pct,rnn_error_pct > mandala_test_results.csv

for causationHierarchies in $(seq $minCausationHierarchies $incrCausationHierarchies $maxCausationHierarchies)
do
 for numNonterminals in $(seq $minNumNonterminals $incrNumNumNonterminals $maxNumNonterminals)
 do
  for numTerminals in $(seq $minNumTerminals $incrNumTerminals $maxNumTerminals)
  do
   for terminalProductionProbability in $(seq $minTerminalProductionProbability $incrTerminalProductionProbability $maxTerminalProductionProbability)
   do
    for featureValueDurationType in $(seq $minFeatureValueDurationType $incrFeatureValueDurationType $maxFeatureValueDurationType)
    do
     > mandala_tmp_nn.txt
     > mandala_tmp_rnn.txt
     for i in $(seq $runs)
     do
      if [ $featureValueDurationType -eq 0 ]
      then
       type=minimum
      elif [ $featureValueDurationType -eq 1 ]
      then
       type=expected
      else
       type=maximum
      fi
      ./mandala.sh -numCausationHierarchies $causationHierarchies -numNonterminals $numNonterminals -terminalProductionProbability $terminalProductionProbability -featureValueDurationType $type > mandala_tmp.txt
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

