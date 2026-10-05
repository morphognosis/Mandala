# Test Mandala copy memory task.

if [ "$#" != "1" ]
then
   echo "Usage: mandala_copy_test.sh <number of runs>"
   exit 1
fi
runs=$1

results_file_name="mandala_copy_test_results_"
results_file_name+=$(date +"%Y-%m-%d_%H-%M-%S")
results_file_name+=".csv"
echo "Results written to" $results_file_name

# Parameters:
minLength=10
incrLength=5
maxLength=50
minCharacters=5
incrCharacters=5
maxCharacters=20
minStrings=1
incrStrings=1
maxStrings=3

echo length,characters,strings,mandala_error_pct,rnn_error_pct,attention_error_pct > $results_file_name

for length in $(seq $minLength $incrLength $maxLength)
do
 for characters in $(seq $minCharacters $incrCharacters $maxCharacters)
 do
  for strings in $(seq $minStrings $incrStrings $maxStrings)
  do
     > mandala_tmp_nn.txt
     > mandala_tmp_rnn.txt
     > mandala_tmp_attention.txt
     for i in $(seq $runs)
     do
      random=$RANDOM
      echo ./mandala.sh -copyTask $length $characters $strings -randomSeed $random
      ./mandala.sh -copyTask $length $characters $strings -randomSeed $random > mandala_tmp.txt
      grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '1p' >> mandala_tmp_nn.txt
      grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '2p' >> mandala_tmp_rnn.txt
      grep "Test prediction errors" mandala_tmp.txt | cut -d"(" -f2 | cut -d"%" -f1 | sed -n '3p' >> mandala_tmp_attention.txt
      rm mandala_tmp.txt
     done
     echo -n ${length},${characters},${strings} >> $results_file_name
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

exit 0

