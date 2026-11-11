# Test Mandala copy memory task.

if [ "$#" == "0" ]
then
   echo "Usage: mandala_copy_test.sh <number of runs> [args]"
   exit 1
fi
runs=$1
args=""
if [ "$2" != "" ]
then
   args=$2
fi

results_file_name="mandala_copy_test_results_"
results_file_name+=$(date +"%Y-%m-%d_%H-%M-%S")
results_file_name+=".csv"
echo "Results written to" $results_file_name

# Parameters:
minLength=50
incrLength=50
maxLength=200
minCharacters=20
incrCharacters=1
maxCharacters=20
minStrings=1
incrStrings=1
maxStrings=1

echo length,characters,strings,mandala_activation_threshold,rnn_activation_threshold,attention_activation_threshold,mandala_train_error_pct,rnn_train_error_pct,attention_train_error_pct,mandala_test_error_pct,mandala_test_stdev,rnn_test_error_pct,rnn_test_stdev,attention_test_error_pct,attention_test_stdev > $results_file_name

for length in $(seq $minLength $incrLength $maxLength)
do
 for characters in $(seq $minCharacters $incrCharacters $maxCharacters)
 do
  for strings in $(seq $minStrings $incrStrings $maxStrings)
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
      echo ./mandala.sh $args -copyTask $length $characters $strings -randomSeed $random
      ./mandala.sh $args -copyTask $length $characters $strings -randomSeed $random > mandala_tmp.txt
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
     echo ${length},${characters},${strings},${mandala_threshold},${rnn_threshold},${attention_threshold},${mandala_train_error},${rnn_train_error},${attention_train_error},${mandala_test_error},${mandala_test_stdev},${rnn_test_error},${rnn_test_stdev},${attention_test_error},${attention_test_stdev} >> $results_file_name
  done
 done
done

exit 0

