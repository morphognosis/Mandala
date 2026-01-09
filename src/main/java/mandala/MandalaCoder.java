// For conditions of distribution and use, see copyright notice in LICENSE.txt.

package mandala;

import java.util.ArrayList;
import java.util.Collections;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

public class MandalaCoder
{
   // Version.
   public static final String VERSION = "1.0";

   // Causations.
   public static int num_causations      = 2;
   public static int num_cause_features  = 2;
   public static int num_effect_features = 2;

   // Dataset size.
   public static int dataset_size = num_causations + 1;

   // Neural network.
   public static MandalaCoderNN mandalaCoderNN;

   public static void main(String[] args)
   {
      // Create network.
      mandalaCoderNN = new MandalaCoderNN();

      // Generate cause and effect pairs.
      // off=0.0, on=1.0
      ArrayList < ArrayList < Integer >> cause_feature_idxs = new ArrayList < ArrayList < Integer >> ();
      ArrayList<Integer> shuffle_idxs = new ArrayList<Integer>();
      for (int i = 0; i < MandalaCoderNN.CAUSE_DIMENSION; i++)
      {
         shuffle_idxs.add(i);
      }
      for (int i = 0; i < num_causations; i++)
      {
         while (true)
         {
            Collections.shuffle(shuffle_idxs);
            ArrayList<Integer> test_idxs = new ArrayList<Integer>();
            for (int j = 0; j < num_cause_features; j++)
            {
               test_idxs.add(shuffle_idxs.get(j));
            }
            Collections.sort(test_idxs);
            boolean found = false;
            for (int j = 0; j < cause_feature_idxs.size() && !found; j++)
            {
               ArrayList<Integer> idxs = cause_feature_idxs.get(j);
               for (int k = 0; k < test_idxs.size(); k++)
               {
                  if (idxs.contains(test_idxs.get(k)))
                  {
                     found = true;
                     break;
                  }
               }
            }
            if (!found)
            {
               cause_feature_idxs.add(test_idxs);
               break;
            }
         }
      }
      ArrayList < ArrayList < Integer >> effect_feature_idxs = new ArrayList < ArrayList < Integer >> ();
      shuffle_idxs = new ArrayList<Integer>();
      for (int i = 0; i < MandalaCoderNN.EFFECT_DIMENSION; i++)
      {
         shuffle_idxs.add(i);
      }
      for (int i = 0; i < num_causations; i++)
      {
         while (true)
         {
            Collections.shuffle(shuffle_idxs);
            ArrayList<Integer> test_idxs = new ArrayList<Integer>();
            for (int j = 0; j < num_effect_features; j++)
            {
               test_idxs.add(shuffle_idxs.get(j));
            }
            Collections.sort(test_idxs);
            boolean found = false;
            for (int j = 0; j < effect_feature_idxs.size() && !found; j++)
            {
               ArrayList<Integer> idxs = effect_feature_idxs.get(j);
               for (int k = 0; k < idxs.size(); k++)
               {
                  if (idxs.contains(test_idxs.get(k)))
                  {
                     found = true;
                     break;
                  }
               }
            }
            if (!found)
            {
               effect_feature_idxs.add(test_idxs);
               break;
            }
         }
      }
      INDArray cause_data = Nd4j.create(dataset_size, MandalaCoderNN.CAUSE_DIMENSION);
      float[] vals       = new float[MandalaCoderNN.CAUSE_DIMENSION];
      float[] accum_vals = new float[MandalaCoderNN.CAUSE_DIMENSION];
      for (int j = 0; j < MandalaCoderNN.CAUSE_DIMENSION; j++)
      {
         accum_vals[j] = 0.0f;
      }
      for (int i = 0; i < cause_feature_idxs.size(); i++)
      {
         for (int j = 0; j < MandalaCoderNN.CAUSE_DIMENSION; j++)
         {
            vals[j] = 0.0f;
         }
         ArrayList<Integer> idxs = cause_feature_idxs.get(i);
         for (int j = 0; j < num_cause_features; j++)
         {
            int k = idxs.get(j);
            vals[k]       = 1.0f;
            accum_vals[k] = 1.0f;
         }
         cause_data.putRow(i, Nd4j.createFromArray(vals));
      }
      cause_data.putRow(dataset_size - 1, Nd4j.createFromArray(accum_vals));
      INDArray effect_data = Nd4j.create(dataset_size, MandalaCoderNN.EFFECT_DIMENSION);
      vals       = new float[MandalaCoderNN.EFFECT_DIMENSION];
      accum_vals = new float[MandalaCoderNN.EFFECT_DIMENSION];
      for (int j = 0; j < MandalaCoderNN.EFFECT_DIMENSION; j++)
      {
         accum_vals[j] = 0.0f;
      }
      for (int i = 0; i < effect_feature_idxs.size(); i++)
      {
         for (int j = 0; j < MandalaCoderNN.EFFECT_DIMENSION; j++)
         {
            vals[j] = 0.0f;
         }
         ArrayList<Integer> idxs = effect_feature_idxs.get(i);
         for (int j = 0; j < num_effect_features; j++)
         {
            int k = idxs.get(j);
            vals[k]       = 1.0f;
            accum_vals[k] = 1.0f;
         }
         effect_data.putRow(i, Nd4j.createFromArray(vals));
      }
      effect_data.putRow(dataset_size - 1, Nd4j.createFromArray(accum_vals));
      mandalaCoderNN.trainDataset    = new DataSet(cause_data, effect_data);
      mandalaCoderNN.trainCauseData  = cause_data;
      mandalaCoderNN.trainEffectData = effect_data;
      mandalaCoderNN.testCauseData   = cause_data;
      mandalaCoderNN.testEffectData  = effect_data;

      // Build network.
      mandalaCoderNN.build();

      // Train network.
      mandalaCoderNN.train(100);

      // Test.
      mandalaCoderNN.test();
   }
}
