// For conditions of distribution and use, see copyright notice in LICENSE.txt.

// Mandala coder neural network.

package mandala;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Collections;
import java.util.List;
import org.deeplearning4j.datasets.iterator.impl.ListDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class MandalaCoderNN
{
   // Dimensions of cause (input), code (encoded cause-effect), and effect (output) vectors.
   public static int CAUSE_DIMENSION  = 16;
   public static int CODE_DIMENSION   = 8;
   public static int EFFECT_DIMENSION = 16;

   // Hidden layer dimension.
   public static int HIDDEN_DIMENSION = 128;

   // Network model.
   public MultiLayerNetwork causationModel;

   // Data.
   public INDArray trainCauseData;
   public INDArray trainEffectData;
   public DataSet  trainDataset;
   public INDArray testCauseData;
   public INDArray testEffectData;
   public DataSet  testDataset;

   // Predictions.
   INDArray trainPredictions;
   INDArray testPredictions;

   // Codes
   INDArray codes;

   // Train epochs.
   public int EPOCHS = 1000;

   // Usage.
   public static final String Usage =
      "Usage:\n" +
      "    java mandala.MandalaCoderNN\n" +
      "      -datasetFilename <file name>\n" +
      "      [-codeDimension <quantity> (default=" + CODE_DIMENSION + ")]\n" +
      "      [-hiddenDimension <quantity> (default=" + HIDDEN_DIMENSION + ")]\n" +
      "Exit codes:\n" +
      "  0=success\n" +
      "  1=error";

   // Constructors.
   public MandalaCoderNN(int codeDim, int hiddenDim)
   {
      CODE_DIMENSION   = codeDim;
      HIDDEN_DIMENSION = hiddenDim;
   }


   public MandalaCoderNN()
   {
   }


   /*
    * Import dataset file.
    * File format:
    * X_train_shape=[<number of vectors>,<cause vector dimension>]
    * X_train = [
    * <cause vectors>
    * ]
    * y_train_shape=[<number of vectors>,<effect vector dimension>]
    * y_train = [
    * <effect vectors>
    * ]
    * Optionally:
    * X_test_shape=[<number of vectors>,<cause vector dimension>]
    * X_test = [
    * <cause vectors>
    * ]
    * y_test_shape=[<number of vectors>,<effect vector dimension>]
    * y_test = [
    * <effect vectors>
    * ]
    */
   public void importDataset(String filename)
   {
      try (BufferedReader br = new BufferedReader(new FileReader(filename)))
         {
            String line = null;
            String[] parts = null;
            int numCause  = 0;
            int numEffect = 0;
            if (((line = br.readLine()) == null) || !line.startsWith("X_train_shape"))
            {
               System.err.println("Invalid X train shape in file " + filename);
               System.exit(1);
            }
            try
            {
               parts = line.split(",");
               if ((parts == null) || (parts.length != 2))
               {
                  System.err.println("Invalid X train shape in file " + filename);
                  System.exit(1);
               }
               numCause        = Integer.parseInt(parts[0].split("\\[")[1].trim());
               CAUSE_DIMENSION = Integer.parseInt(parts[1].split("\\]")[0].trim());
            }
            catch (Exception e)
            {
               System.err.println("Invalid X train shape in file " + filename + ", " + e.getMessage());
               System.exit(1);
            }
            if ((numCause < 0) || (CAUSE_DIMENSION < 0))
            {
               System.err.println("Invalid X train shape in file " + filename);
               System.exit(1);
            }
            trainCauseData = Nd4j.create(numCause, CAUSE_DIMENSION);
            float[] values = new float[CAUSE_DIMENSION];
            if (((line = br.readLine()) == null) || !line.equals("X_train = ["))
            {
               System.err.println("Invalid X train data in file " + filename);
               System.exit(1);
            }
            for (int i = 0; i < numCause; i++)
            {
               if ((line = br.readLine()) == null)
               {
                  System.err.println("Cannot read X train data from file " + filename);
                  System.exit(1);
               }
               parts = line.split(",");
               if (parts.length != CAUSE_DIMENSION)
               {
                  System.err.println("Invalid X train data in file " + filename + ", line = " + line);
                  System.exit(1);
               }
               for (int j = 0; j < CAUSE_DIMENSION; j++)
               {
                  try
                  {
                     values[j] = Float.parseFloat(parts[j].trim());
                  }
                  catch (NumberFormatException e)
                  {
                     System.err.println("Invalid X train data in file " + filename + ", value = " + parts[0].trim() + ", " + e.getMessage());
                     System.exit(1);
                  }
               }
               trainCauseData.putRow(i, Nd4j.createFromArray(values));
            }
            if (((line = br.readLine()) == null) || !line.equals("]"))
            {
               System.err.println("Cannot read X train data from file " + filename);
               System.exit(1);
            }
            if (((line = br.readLine()) == null) || !line.startsWith("y_train_shape"))
            {
               System.err.println("Invalid y train shape from file " + filename);
               System.exit(1);
            }
            try
            {
               parts = line.split(",");
               if ((parts == null) || (parts.length != 2))
               {
                  System.err.println("Invalid y train shape in file " + filename);
                  System.exit(1);
               }
               numEffect        = Integer.parseInt(parts[0].split("\\[")[1].trim());
               EFFECT_DIMENSION = Integer.parseInt(parts[1].split("\\]")[0].trim());
            }
            catch (Exception e)
            {
               System.err.println("Invalid y train shape in file " + filename + ", " + e.getMessage());
               System.exit(1);
            }
            if ((numEffect < 0) || (EFFECT_DIMENSION < 0))
            {
               System.err.println("Invalid y train shape in file " + filename);
               System.exit(1);
            }
            if (numCause != numEffect)
            {
               System.err.println("X and y train data must have equal number of vectors");
               System.exit(1);
            }
            if (CAUSE_DIMENSION != EFFECT_DIMENSION)
            {
               System.err.println("X train and y train vectors must have equal dimensions");
               System.exit(1);
            }
            trainEffectData = Nd4j.create(numEffect, EFFECT_DIMENSION);
            values          = new float[EFFECT_DIMENSION];
            if (((line = br.readLine()) == null) || !line.equals("y_train = ["))
            {
               System.err.println("Invalid y train data in file " + filename);
               System.exit(1);
            }
            for (int i = 0; i < numEffect; i++)
            {
               if ((line = br.readLine()) == null)
               {
                  System.err.println("Cannot read y train data from file " + filename);
                  System.exit(1);
               }
               parts = line.split(",");
               if (parts.length != EFFECT_DIMENSION)
               {
                  System.err.println("Invalid y train data in file " + filename + ", line = " + line);
                  System.exit(1);
               }
               for (int j = 0; j < EFFECT_DIMENSION; j++)
               {
                  try
                  {
                     values[j] = Float.parseFloat(parts[j].trim());
                  }
                  catch (NumberFormatException e)
                  {
                     System.err.println("Invalid y train data in file " + filename + ", value = " + parts[0].trim() + ", " + e.getMessage());
                     System.exit(1);
                  }
               }
               trainEffectData.putRow(i, Nd4j.createFromArray(values));
            }
            if (((line = br.readLine()) == null) || !line.equals("]"))
            {
               System.err.println("Cannot read y train data from file " + filename);
               System.exit(1);
            }

            // Create train dataset.
            trainDataset = new DataSet(trainCauseData, trainEffectData);

            // Testing data available?
            while (true)
            {
               line = br.readLine();
               if (line == null)
               {
                  return;
               }
               if (line.startsWith("X_test_shape"))
               {
                  break;
               }
            }
            try
            {
               parts = line.split(",");
               if ((parts == null) || (parts.length != 2))
               {
                  System.err.println("Invalid X test shape in file " + filename);
                  System.exit(1);
               }
               numCause = Integer.parseInt(parts[0].split("\\[")[1].trim());
               int n = Integer.parseInt(parts[1].split("\\]")[0].trim());
               if (n != CAUSE_DIMENSION)
               {
                  System.err.println("X train and X test vectors must have equal dimensions");
                  System.exit(1);
               }
            }
            catch (Exception e)
            {
               System.err.println("Invalid X test shape in file " + filename + ", " + e.getMessage());
               System.exit(1);
            }
            if (numCause < 0)
            {
               System.err.println("Invalid X test shape in file " + filename);
               System.exit(1);
            }
            testCauseData = Nd4j.create(numCause, CAUSE_DIMENSION);
            values        = new float[CAUSE_DIMENSION];
            if (((line = br.readLine()) == null) || !line.equals("X_test = ["))
            {
               System.err.println("Invalid X test data in file " + filename);
               System.exit(1);
            }
            for (int i = 0; i < numCause; i++)
            {
               if ((line = br.readLine()) == null)
               {
                  System.err.println("Cannot read X test data from file " + filename);
                  System.exit(1);
               }
               parts = line.split(",");
               if (parts.length != CAUSE_DIMENSION)
               {
                  System.err.println("Invalid X test data in file " + filename + ", line = " + line);
                  System.exit(1);
               }
               for (int j = 0; j < CAUSE_DIMENSION; j++)
               {
                  try
                  {
                     values[j] = Float.parseFloat(parts[j].trim());
                  }
                  catch (NumberFormatException e)
                  {
                     System.err.println("Invalid X test data in file " + filename + ", value = " + parts[0].trim() + ", " + e.getMessage());
                     System.exit(1);
                  }
               }
               testCauseData.putRow(i, Nd4j.createFromArray(values));
            }
            if (((line = br.readLine()) == null) || !line.equals("]"))
            {
               System.err.println("Cannot read X test data from file " + filename);
               System.exit(1);
            }
            if (((line = br.readLine()) == null) || !line.startsWith("y_test_shape"))
            {
               System.err.println("Invalid y test shape from file " + filename);
               System.exit(1);
            }
            try
            {
               parts = line.split(",");
               if ((parts == null) || (parts.length != 2))
               {
                  System.err.println("Invalid y test shape from file " + filename);
                  System.exit(1);
               }
               numEffect        = Integer.parseInt(parts[0].split("\\[")[1].trim());
               EFFECT_DIMENSION = Integer.parseInt(parts[1].split("\\]")[0].trim());
            }
            catch (Exception e)
            {
               System.err.println("Invalid y test shape in file " + filename + ", " + e.getMessage());
               System.exit(1);
            }
            if ((numEffect < 0) || (EFFECT_DIMENSION < 0))
            {
               System.err.println("Invalid y test shape in file " + filename);
               System.exit(1);
            }
            if (numCause != numEffect)
            {
               System.err.println("X and y test data must have equal number of vectors");
               System.exit(1);
            }
            if (CAUSE_DIMENSION != EFFECT_DIMENSION)
            {
               System.err.println("X test and y test vectors must have equal dimensions");
               System.exit(1);
            }
            testEffectData = Nd4j.create(numEffect, EFFECT_DIMENSION);
            values         = new float[EFFECT_DIMENSION];
            if (((line = br.readLine()) == null) || !line.equals("y_test = ["))
            {
               System.err.println("Invalid y test data in file " + filename);
               System.exit(1);
            }
            for (int i = 0; i < numEffect; i++)
            {
               if ((line = br.readLine()) == null)
               {
                  System.err.println("Cannot read y test data from file " + filename);
                  System.exit(1);
               }
               parts = line.split(",");
               if (parts.length != EFFECT_DIMENSION)
               {
                  System.err.println("Invalid y test data in file " + filename + ", line = " + line);
                  System.exit(1);
               }
               for (int j = 0; j < EFFECT_DIMENSION; j++)
               {
                  try
                  {
                     values[j] = Float.parseFloat(parts[j].trim());
                  }
                  catch (NumberFormatException e)
                  {
                     System.err.println("Invalid y test data in file " + filename + ", value = " + parts[0].trim() + ", " + e.getMessage());
                     System.exit(1);
                  }
               }
               testEffectData.putRow(i, Nd4j.createFromArray(values));
            }
            if (((line = br.readLine()) == null) || !line.equals("]"))
            {
               System.err.println("Cannot read y test data from file " + filename);
               System.exit(1);
            }

            // Create test dataset.
            testDataset = new DataSet(testCauseData, testEffectData);
         }
         catch (Exception e)
         {
            System.err.println("Cannot import dataset from file " + filename + ": " + e.getMessage());
            System.exit(1);
         }

   }


   // Build NN.
   public void build()
   {
      // Configure model.
      MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                                        .seed(12345)
                                        .weightInit(WeightInit.XAVIER)
                                        .updater(new Adam(1e-3))
                                        .list()
                                        .layer(0, new DenseLayer.Builder().nIn(CAUSE_DIMENSION).nOut(HIDDEN_DIMENSION)
                                                  .activation(Activation.RELU)
                                                  .build())
                                        .layer(1, new DenseLayer.Builder().nIn(HIDDEN_DIMENSION).nOut(CODE_DIMENSION)
                                                  .activation(Activation.RELU)
                                                  .l1(10e-5)
                                                  .build())
                                        .layer(2, new DenseLayer.Builder().nIn(CODE_DIMENSION).nOut(HIDDEN_DIMENSION)
                                                  .activation(Activation.RELU)
                                                  .build())
                                        .layer(3, new DenseLayer.Builder().nIn(HIDDEN_DIMENSION).nOut(HIDDEN_DIMENSION)
                                                  .activation(Activation.RELU)
                                                  .build())
                                        .layer(4, new OutputLayer.Builder().nIn(HIDDEN_DIMENSION).nOut(EFFECT_DIMENSION)
                                                  .activation(Activation.SIGMOID)
                                                  .lossFunction(LossFunctions.LossFunction.MSE)
                                                  .build())
                                        .setInputType(InputType.feedForward(CAUSE_DIMENSION))
                                        .build();

      // Build model.
      causationModel = new MultiLayerNetwork(conf);
      causationModel.init();
      causationModel.setListeners(Collections.singletonList(new ScoreIterationListener(10)));
   }


   // Train NN.
   public void train()
   {
      train(EPOCHS);
   }


   public void train(int epochs)
   {
      System.out.println("Train:");
      if (trainDataset == null)
      {
         System.err.println("No train dataset");
         System.exit(1);
      }
      for (int epoch = 0; epoch < epochs; epoch++)
      {
         causationModel.fit(trainDataset);
      }
      List<DataSet>                list     = trainDataset.asList();
      ListDataSetIterator<DataSet> iterator = new ListDataSetIterator<DataSet>(list);
      Evaluation eval = causationModel.evaluate(iterator);
      System.out.println(eval.stats());
   }


   // Validate training.
   public void validate()
   {
      System.out.println("Validate:");
      if (trainCauseData == null)
      {
         System.err.println("No train dataset");
         System.exit(1);
      }
      List<DataSet>                list     = trainDataset.asList();
      ListDataSetIterator<DataSet> iterator = new ListDataSetIterator<DataSet>(list);
      int errors = 0;
      int total  = 0;
      while (iterator.hasNext())
      {
         DataSet  batch       = iterator.next();
         INDArray features    = batch.getFeatures();
         INDArray labels      = batch.getLabels();
         INDArray predictions = causationModel.output(features);
         for (int i = 0; i < predictions.size(0); i++)
         {
            total++;
            INDArray singlePrediction = predictions.getRow(i);
            INDArray singleLabel      = labels.getRow(i);
            for (int j = 0, k = (int)singlePrediction.size(0); j < k; j++)
            {
               if ((singlePrediction.getFloat(j) >= 0.5f) && (singleLabel.getFloat(j) < 0.5f))
               {
                  errors++;
                  break;
               }
               else if ((singlePrediction.getFloat(j) < 0.5f) && (singleLabel.getFloat(j) >= 0.5f))
               {
                  errors++;
                  break;
               }
            }
         }
      }
      System.out.print("errors(" + errors + ") / total(" + total + ")");
      if (total > 0)
      {
         System.out.println(" = " + ((float)errors / (float)total));
      }
      codes = causationModel.activateSelectedLayers(0, 1, trainCauseData);
      System.out.println("codes: " + codes);
   }


   // Test NN.
   public void test()
   {
      System.out.println("Test:");
      if (testCauseData == null)
      {
         System.err.println("No test dataset");
         System.exit(1);
      }
      List<DataSet>                list     = testDataset.asList();
      ListDataSetIterator<DataSet> iterator = new ListDataSetIterator<DataSet>(list);
      int errors = 0;
      int total  = 0;
      while (iterator.hasNext())
      {
         DataSet  batch       = iterator.next();
         INDArray features    = batch.getFeatures();
         INDArray labels      = batch.getLabels();
         INDArray predictions = causationModel.output(features);
         for (int i = 0; i < predictions.size(0); i++)
         {
            total++;
            INDArray singlePrediction = predictions.getRow(i);
            INDArray singleLabel      = labels.getRow(i);
            for (int j = 0, k = (int)singlePrediction.size(0); j < k; j++)
            {
               if ((singlePrediction.getFloat(j) >= 0.5f) && (singleLabel.getFloat(j) < 0.5f))
               {
                  errors++;
                  break;
               }
               else if ((singlePrediction.getFloat(j) < 0.5f) && (singleLabel.getFloat(j) >= 0.5f))
               {
                  errors++;
                  break;
               }
            }
         }
      }
      System.out.print("errors(" + errors + ") / total(" + total + ")");
      if (total > 0)
      {
         System.out.println(" = " + ((float)errors / (float)total));
      }
      codes = causationModel.activateSelectedLayers(0, 1, testCauseData);
      System.out.println("codes: " + codes.toString());
   }


   // Predict effect.
   public INDArray predict(INDArray cause)
   {
      return(causationModel.output(cause));
   }


   // Encode cause-effect.
   public INDArray encode(INDArray cause)
   {
      return(causationModel.activateSelectedLayers(0, 1, cause));
   }


   // Main.
   public static void main(String[] args)
   {
      String datasetFilename = null;

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-datasetFilename"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid datasetFilename option");
               System.err.println(Usage);
               System.exit(1);
            }
            datasetFilename = args[i];
            continue;
         }
         if (args[i].equals("-codeDimension"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid codeDimension option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               CODE_DIMENSION = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid codeDimension option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (CODE_DIMENSION < 0)
            {
               System.err.println("Invalid codeDimension option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-hiddenDimension"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid hiddenDimension option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               HIDDEN_DIMENSION = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid hiddenDimension option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (HIDDEN_DIMENSION < 0)
            {
               System.err.println("Invalid hiddenDimension option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-help"))
         {
            System.out.println(Usage);
            System.exit(0);
         }
         System.err.println("Invalid option: " + args[i]);
         System.err.println(Usage);
         System.exit(1);
      }
      if (datasetFilename == null)
      {
         System.err.println(Usage);
         System.exit(1);
      }

      // Create NN.
      MandalaCoderNN mandalaCoderNN = new MandalaCoderNN();

      // Import dataset file.
      mandalaCoderNN.importDataset(datasetFilename);

      // Build NN.
      mandalaCoderNN.build();

      // Train.
      mandalaCoderNN.train(1000);

      // Validate.
      mandalaCoderNN.validate();

      // Test.
      mandalaCoderNN.test();

      System.exit(0);
   }
}
