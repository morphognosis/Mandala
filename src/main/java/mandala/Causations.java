// For conditions of distribution and use, see copyright notice in Mandala.java

// Generate causation hierarchies and learn paths through the hierarchies.

package mandala;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import org.json.JSONException;
import org.json.JSONObject;

public class Causations
{
   // Generation parameters.
   public static int   NUM_CAUSATION_HIERARCHIES       = 1;
   public static int   NUM_NONTERMINALS                = 5;
   public static int   NUM_TERMINALS                   = 10;
   public static int   MIN_PRODUCTION_RHS_LENGTH       = 2;
   public static int   MAX_PRODUCTION_RHS_LENGTH       = 5;
   public static float TERMINAL_PRODUCTION_PROBABILITY = 0.5f;

   // Dimensions.
   public static int NUM_DIMENSIONS        = 32;
   public static int NUM_TERMINAL_FEATURES = 3;

   // Causation.
   public static class Causation
   {
      public int hierarchy;
      public int id;
      public ArrayList<NonterminalCausation> parents;

      public Causation(int hierarchy, int id)
      {
         this.hierarchy = hierarchy;
         this.id        = id;
         parents        = new ArrayList<NonterminalCausation>();
      }
   };

   // Terminal causation.
   public static class TerminalCausation extends Causation
   {
      public ArrayList<Boolean> features;

      public TerminalCausation(int hierarchy, int id)
      {
         super(hierarchy, id);
         encodeFeatures();
      }


      // Encode features.
      public void encodeFeatures()
      {
         String seedString = hierarchy + "_" + id;
         long   seed       = seedString.hashCode();
         Random r          = new Random(seed);

         features = new ArrayList<Boolean>();
         for (int i = 0; i < NUM_DIMENSIONS; i++)
         {
            features.add(false);
         }
         ArrayList<Integer> idxs = new ArrayList<Integer>();
         for (int i = 0; i < NUM_TERMINAL_FEATURES; i++)
         {
            while (true)
            {
               int n = r.nextInt(NUM_DIMENSIONS);
               int j = 0;
               for ( ; j < idxs.size(); j++)
               {
                  if (n == idxs.get(j))
                  {
                     break;
                  }
               }
               if (j == idxs.size())
               {
                  idxs.add(n);
                  break;
               }
            }
         }
         for (int n : idxs)
         {
            features.set(n, true);
         }
      }


      public void print()
      {
         System.out.print("hierarchy=" + hierarchy);
         System.out.print(", id=" + id);
         System.out.print(", features:");
         for (int i = 0; i < features.size(); i++)
         {
            if (features.get(i))
            {
               System.out.print(" " + i);
            }
         }
         System.out.print(", parents:");
         for (Causation p : parents)
         {
            System.out.print(" " + p.id);
         }
         System.out.println();
      }


      public void printHierarchical(String indent, String childNum)
      {
         System.out.print(indent);
         System.out.print("terminal id=" + id);
         System.out.print(", features:");
         for (int i = 0; i < features.size(); i++)
         {
            if (features.get(i))
            {
               System.out.print(" " + i);
            }
         }
         if (childNum != null)
         {
            System.out.print(", child number=" + childNum);
         }
         System.out.println();
      }
   };

   // Nonterminal causation.
   public static class NonterminalCausation extends Causation
   {
      public ArrayList<Causation> children;

      public NonterminalCausation(int hierarchy, int id)
      {
         super(hierarchy, id);
         children = new ArrayList<Causation>();
      }


      public void print()
      {
         System.out.print("hierarchy=" + hierarchy);
         System.out.print(", id=" + id);
         System.out.print(", parents:");
         for (Causation p : parents)
         {
            System.out.print(" " + p.id);
         }
         System.out.print(", children:");
         for (Causation c : children)
         {
            System.out.print(" " + c.id);
         }
         System.out.println();
      }


      public void printHierarchical(String indent, String childNum, boolean recursive)
      {
         System.out.print(indent);
         System.out.print("nonterminal id=" + id);
         if (childNum != null)
         {
            System.out.print(", child number=" + childNum);
         }
         System.out.println();
         if (recursive)
         {
            for (int i = 0, j = children.size(); i < j; i++)
            {
               Causation child = children.get(i);
               if (child instanceof TerminalCausation)
               {
                  TerminalCausation terminal = (TerminalCausation)child;
                  terminal.printHierarchical(indent + "  ", i + "");
               }
               else
               {
                  NonterminalCausation nonterminal = (NonterminalCausation)child;
                  nonterminal.printHierarchical(indent + "  ", i + "", true);
               }
            }
         }
      }
   };
   public static ArrayList < ArrayList < Causation >> causationHierarchies;

   // Graph.
   public static String  CAUSATIONS_GRAPH_FILENAME = "causations.dot";
   public static boolean TREE_FORMAT = true;

   // Causation paths.
   public static class CausationTier
   {
      public Causation causation;
      public int       currentChild;

      public CausationTier(Causation causation, int currentChild)
      {
         this.causation    = causation;
         this.currentChild = currentChild;
      }


      public void print()
      {
         if (causation instanceof TerminalCausation)
         {
            ((TerminalCausation)causation).printHierarchical("", null);
         }
         else
         {
            NonterminalCausation nonterminalCausation = (NonterminalCausation)causation;
            String               childInfo            = currentChild + "/" + nonterminalCausation.children.size();
            nonterminalCausation.printHierarchical("", childInfo, false);
         }
      }
   };
   public static int   NUM_CAUSATION_PATHS = 5;
   public static class CausationPath
   {
      int hierarchy;
      int id;
      ArrayList < ArrayList < CausationTier >> steps;

      public CausationPath(int hierarchy, int id)
      {
         this.hierarchy = hierarchy;
         this.id        = id;
         steps          = new ArrayList < ArrayList < CausationTier >> ();
      }


      public void add(ArrayList<CausationTier> step)
      {
         steps.add(step);
      }


      public void print()
      {
         System.out.println("path: hierarchy=" + hierarchy + "/root id=" + id + "\nsteps: ");
         for (int i = 0, j = steps.size(); i < j; i++)
         {
            System.out.println("step " + i + ": ");
            ArrayList<CausationTier> tiers = steps.get(i);
            for (CausationTier tier : tiers)
            {
               tier.print();
            }
         }
      }
   };
   public static ArrayList<CausationPath> causationPaths;

   // Causation attenuation.
   public static float CAUSATION_FEATURE_ATTENUATION = 0.1f;
   public static float CAUSATION_TIER_ATTENUATION    = 0.0f;

   // Non-terminal causation features.
   public static class CausationFeature
   {
      public int   feature;
      public int   tier;
      public float value;
      public int   begin;
      public int   end;

      // Set feature from component features.
      public void setFeature(ArrayList<Integer> component1, ArrayList<Integer> component2)
      {
         String s = "";

         for (Integer i : component1)
         {
            s += i + "_";
         }
         for (Integer i : component2)
         {
            s += i + "_";
         }
         Random r = new Random(s.hashCode());
         feature = r.nextInt(NUM_DIMENSIONS);
      }


      // Attenuate value.
      public boolean attentuate()
      {
         value -= CAUSATION_FEATURE_ATTENUATION / Math.pow(tier, CAUSATION_TIER_ATTENUATION);
         if (value > 0.0f)
         {
            return(true);
         }
         else
         {
            value = 0.0f;
            return(false);
         }
      }
   };
   public static ArrayList < ArrayList < CausationFeature >> causationFeatures;

   // Datasets.
   public static String NN_DATASET_FILENAME        = "causations_nn_dataset.py";
   public static float  NN_DATASET_TRAIN_FRACTION  = 0.75f;
   public static String RNN_DATASET_FILENAME       = "causations_rnn_dataset.py";
   public static float  RNN_DATASET_TRAIN_FRACTION = 0.75f;
   public static String TCN_DATASET_FILENAME       = "causations_tcn_dataset.py";
   public static float  TCN_DATASET_TRAIN_FRACTION = 0.75f;

   // Learners.
   public static String NN_FILENAME         = "causations_nn.py";
   public static String NN_RESULTS_FILENAME = "causations_nn_results.json";
   public static String NN_HIDDEN           = "128,128,128";
   public static int    NN_EPOCHS           = 500;
   public static String RNN_FILENAME        = "causations_rnn.py";
   public static String TCN_FILENAME        = "causations_tcn.py";

   // Learning results.
   public static class LearningResults
   {
      public int   train_prediction_errors;
      public int   train_total_predictions;
      public float train_error_pct;
      public int   test_prediction_errors;
      public int   test_total_predictions;
      public float test_error_pct;

      public void print()
      {
         System.out.println("Train prediction errors/total = " + train_prediction_errors + "/" + train_total_predictions);
         System.out.println(" (" + train_error_pct + "%)");
         System.out.print("Test prediction errors/total = " + test_prediction_errors + "/" + test_total_predictions);
         System.out.println(" (" + test_error_pct + "%)");
      }
   };

   // Random numbers.
   public static int    RANDOM_SEED = 4517;
   public static Random randomizer  = null;

   // Verbosity.
   public static boolean VERBOSE = true;

   // Usage.
   public static final String Usage =
      "Usage:\n" +
      "    java mandala.Causations\n" +
      "      [-numCausationHierarchies <quantity> (default=" + NUM_CAUSATION_HIERARCHIES + ")]\n" +
      "      [-numNonterminals <quantity> (default=" + NUM_NONTERMINALS + ")]\n" +
      "      [-numTerminals <quantity> (default=" + NUM_TERMINALS + ")]\n" +
      "      [-minProductionRightHandSideLength <quantity> (default=" + MIN_PRODUCTION_RHS_LENGTH + ")]\n" +
      "      [-maxProductionRightHandSideLength <quantity> (default=" + MAX_PRODUCTION_RHS_LENGTH + ")]\n" +
      "      [-terminalProductionProbability <probability> (default=" + TERMINAL_PRODUCTION_PROBABILITY + ")]\n" +
      "      [-numDimensions <quantity> (default=" + NUM_DIMENSIONS + ")]\n" +
      "      [-numTerminalFeatures <quantity> (default=" + NUM_TERMINAL_FEATURES + ")]\n" +
      "      [-exportCausationsGraph [<file name> (Graphviz dot format, default=" + CAUSATIONS_GRAPH_FILENAME + ")]\n" +
      "          [-treeFormat \"true\" | \"false\" (default=" + TREE_FORMAT + ")]]\n" +
      "      [-numCausationPaths <quantity> (default=" + NUM_CAUSATION_PATHS + ")]\n" +
      "      [-causationFeatureAttenuation <multiplier> (default=" + CAUSATION_FEATURE_ATTENUATION + ")]\n" +
      "      [-causationTierAttenuation <divisor> (default=" + CAUSATION_TIER_ATTENUATION + ")]\n" +
      "      [-NNdatasetTrainFraction <fraction> (default=" + NN_DATASET_TRAIN_FRACTION + ")]\n" +
      "      [-NNnumHidden <number of hidden neurons> (comma-separated for additional layers) (default=" + NN_HIDDEN + ")]\n" +
      "      [-NNnumEpochs <number of epochs> (default=" + NN_EPOCHS + ")]\n" +
      "      [-RNNdatasetTrainFraction <fraction> (default=" + RNN_DATASET_TRAIN_FRACTION + ")]\n" +
      "      [-TCNdatasetTrainFraction <fraction> (default=" + TCN_DATASET_TRAIN_FRACTION + ")]\n" +
      "      [-randomSeed <seed> (default=" + RANDOM_SEED + ")]\n" +
      "      [-quiet]\n" +
      "  Help:\n" +
      "    java mandala.Causations -help\n" +
      "Exit codes:\n" +
      "  0=success\n" +
      "  1=error";

   // Main.
   public static void main(String[] args)
   {
      boolean gotExportCausationsGraph = false;
      boolean gotTreeFormat            = false;

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-numCausationHierarchies"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid numCausationHierarchies option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NUM_CAUSATION_HIERARCHIES = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid numCausationHierarchies option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NUM_CAUSATION_HIERARCHIES < 0)
            {
               System.err.println("Invalid numCausationHierarchies option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-numNonterminals"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid numNonterminals option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NUM_NONTERMINALS = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid numNonterminals option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NUM_NONTERMINALS < 0)
            {
               System.err.println("Invalid numNonterminals option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-numTerminals"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid numTerminals option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NUM_TERMINALS = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid numTerminals option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NUM_TERMINALS < 1)
            {
               System.err.println("Invalid numTerminals option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-minProductionRightHandSideLength"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid minProductionRightHandSideLength option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               MIN_PRODUCTION_RHS_LENGTH = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid minProductionRightHandSideLength option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (MIN_PRODUCTION_RHS_LENGTH < 1)
            {
               System.err.println("Invalid minProductionRightHandSideLength option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-maxProductionRightHandSideLength"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid maxProductionRightHandSideLength option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               MAX_PRODUCTION_RHS_LENGTH = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid maxProductionRightHandSideLength option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (MAX_PRODUCTION_RHS_LENGTH < 1)
            {
               System.err.println("Invalid maxProductionRightHandSideLength option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-terminalProductionProbability"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid terminalProductionProbability option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               TERMINAL_PRODUCTION_PROBABILITY = Float.parseFloat(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid terminalProductionProbability option");
               System.err.println(Usage);
               System.exit(1);
            }
            if ((TERMINAL_PRODUCTION_PROBABILITY < 0.0f) || (TERMINAL_PRODUCTION_PROBABILITY > 1.0f))
            {
               System.err.println("Invalid terminalProductionProbability option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-numDimensions"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid numDimensions option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NUM_DIMENSIONS = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid numDimensions option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NUM_DIMENSIONS < 1)
            {
               System.err.println("Invalid numDimensions option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-numTerminalFeatures"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid numTerminalFeatures option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NUM_TERMINAL_FEATURES = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid numTerminalFeatures option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NUM_TERMINAL_FEATURES < 1)
            {
               System.err.println("Invalid numTerminalFeatures option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-exportCausationsGraph"))
         {
            if (((i + 1) < args.length) && !args[(i + 1)].startsWith("-"))
            {
               i++;
               CAUSATIONS_GRAPH_FILENAME = args[i];
            }
            gotExportCausationsGraph = true;
            continue;
         }
         if (args[i].equals("-treeFormat"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid treeFormat option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (args[i].equals("true"))
            {
               TREE_FORMAT = true;
            }
            else if (args[i].equals("false"))
            {
               TREE_FORMAT = false;
            }
            else
            {
               System.err.println("Invalid treeFormat option");
               System.err.println(Usage);
               System.exit(1);
            }
            gotTreeFormat = true;
            continue;
         }
         if (args[i].equals("-numCausationPaths"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid numCausationPaths option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NUM_CAUSATION_PATHS = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid numCausationPaths option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NUM_CAUSATION_PATHS < 0)
            {
               System.err.println("Invalid numCausationPaths option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-causationFeatureAttenuation"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid causationFeatureAttenuation option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               CAUSATION_FEATURE_ATTENUATION = Float.parseFloat(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid causationFeatureAttenuation option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (CAUSATION_FEATURE_ATTENUATION < 0.0f)
            {
               System.err.println("Invalid causationFeatureAttenuation option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-causationTierAttenuation"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid causationTierAttenuation option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               CAUSATION_TIER_ATTENUATION = Float.parseFloat(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid causationTierAttenuation option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (CAUSATION_TIER_ATTENUATION < 0.0f)
            {
               System.err.println("Invalid causationTierAttenuation option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-NNdatasetTrainFraction"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid NNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NN_DATASET_TRAIN_FRACTION = Float.parseFloat(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid NNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            if ((NN_DATASET_TRAIN_FRACTION < 0.0f) || (NN_DATASET_TRAIN_FRACTION > 1.0f))
            {
               System.err.println("Invalid NNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-NNnumHidden"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid NNnumHidden option");
               System.err.println(Usage);
               System.exit(1);
            }
            NN_HIDDEN = args[i].replaceAll("\\s", "");
            if (NN_HIDDEN.isEmpty())
            {
               System.err.println("Invalid NNnumHidden option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-NNnumEpochs"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid NNnumEpochs option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NN_EPOCHS = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid NNnumEpochs option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NN_EPOCHS < 0)
            {
               System.err.println("Invalid NNnumEpochs option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-RNNdatasetTrainFraction"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid RNNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               RNN_DATASET_TRAIN_FRACTION = Float.parseFloat(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid RNNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            if ((RNN_DATASET_TRAIN_FRACTION < 0.0f) || (RNN_DATASET_TRAIN_FRACTION > 1.0f))
            {
               System.err.println("Invalid RNNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-TCNdatasetTrainFraction"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid TCNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               TCN_DATASET_TRAIN_FRACTION = Float.parseFloat(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid TCNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            if ((TCN_DATASET_TRAIN_FRACTION < 0.0f) || (TCN_DATASET_TRAIN_FRACTION > 1.0f))
            {
               System.err.println("Invalid TCNdatasetTrainFraction option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-randomSeed"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid randomSeed option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               RANDOM_SEED = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid randomSeed option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-quiet"))
         {
            VERBOSE = false;
            continue;
         }
         if (args[i].equals("-help") || args[i].equals("-h") || args[i].equals("-?"))
         {
            System.out.println(Usage);
            System.exit(0);
         }
         System.err.println("Invalid option: " + args[i]);
         System.err.println(Usage);
         System.exit(1);
      }

      // Validate options.
      if (MIN_PRODUCTION_RHS_LENGTH > MAX_PRODUCTION_RHS_LENGTH)
      {
         System.err.println(Usage);
         System.exit(1);
      }
      if (NUM_TERMINAL_FEATURES > NUM_DIMENSIONS)
      {
         System.err.println(Usage);
         System.exit(1);
      }
      if (!gotExportCausationsGraph && gotTreeFormat)
      {
         System.err.println(Usage);
         System.exit(1);
      }


      // Initialize random numbers.
      randomizer = new Random(RANDOM_SEED);

      // Generate causations.
      causationHierarchies = new ArrayList < ArrayList < Causation >> ();
      for (int i = 0; i < NUM_CAUSATION_HIERARCHIES; i++)
      {
         ArrayList<Causation> causations = new ArrayList<Causation>();
         if (NUM_NONTERMINALS > 0)
         {
            ArrayList<NonterminalCausation> nonterminalInstances = generateCausationHierarchy(i);
            for (NonterminalCausation causation : nonterminalInstances)
            {
               if (causation != null)
               {
                  causations.add((Causation)causation);
               }
            }
         }
         else if (NUM_TERMINALS > 0)
         {
            causations.add(new TerminalCausation(i, 0));
         }
         causationHierarchies.add(causations);
      }

      // Print causations?
      if (VERBOSE)
      {
         printCausations();
      }

      // Export causations graph.
      if (gotExportCausationsGraph)
      {
         exportCausationsGraph(CAUSATIONS_GRAPH_FILENAME, TREE_FORMAT);
      }

      // Generate causation paths.
      generateCausationPaths(NUM_CAUSATION_PATHS);

      // Export causation datasets.
      exportNNdataset(NN_DATASET_FILENAME, NN_DATASET_TRAIN_FRACTION);
      exportDataset(RNN_DATASET_FILENAME, RNN_DATASET_TRAIN_FRACTION);
      exportDataset(TCN_DATASET_FILENAME, TCN_DATASET_TRAIN_FRACTION);

      // Learn causations.
      learnCausationsNN(NN_DATASET_FILENAME);
      learnCausationsRNN(RNN_DATASET_FILENAME);
      learnCausationsTCN(TCN_DATASET_FILENAME);

      System.exit(0);
   }


   // Generate causation hierarchy.
   public static ArrayList<NonterminalCausation> generateCausationHierarchy(int hierarchy)
   {
      NonterminalCausation root = new NonterminalCausation(hierarchy, 0);

      ArrayList<NonterminalCausation> open = new ArrayList<NonterminalCausation>();
      open.add(root);
      ArrayList<NonterminalCausation> nonterminalInstances = new ArrayList<NonterminalCausation>();
      nonterminalInstances.add(root);
      for (int i = 1; i < NUM_NONTERMINALS; i++)
      {
         nonterminalInstances.add(null);
      }
      ArrayList<TerminalCausation> terminalInstances = new ArrayList<TerminalCausation>();
      for (int i = 0; i < NUM_TERMINALS; i++)
      {
         terminalInstances.add(null);
      }
      Graph graph = new Graph(NUM_NONTERMINALS);
      expandNonterminal(open, graph, nonterminalInstances, terminalInstances);
      return(nonterminalInstances);
   }


   // Expand nonterminal causation.
   public static void expandNonterminal(ArrayList<NonterminalCausation> open, Graph graph,
                                        ArrayList<NonterminalCausation> nonterminalInstances, ArrayList<TerminalCausation> terminalInstances)
   {
      int n = randomizer.nextInt(open.size());
      NonterminalCausation parent = open.get(n);

      open.remove(n);
      if (randomizer.nextFloat() < TERMINAL_PRODUCTION_PROBABILITY)
      {
         expandTerminal(parent, terminalInstances);
      }
      else
      {
         n = randomizer.nextInt(MAX_PRODUCTION_RHS_LENGTH - MIN_PRODUCTION_RHS_LENGTH + 1) + MIN_PRODUCTION_RHS_LENGTH;
         for (int i = 0; i < n; i++)
         {
            ArrayList<Causation> newChildren = new ArrayList<Causation>();
            ArrayList<Integer>   available   = new ArrayList<Integer>();
            for (int j = 0; j < NUM_NONTERMINALS; j++)
            {
               available.add(j);
            }
            NonterminalCausation child = null;
            for (int j = 0; j < NUM_NONTERMINALS && child == null; j++)
            {
               int a = randomizer.nextInt(available.size());
               int k = available.get(a);
               graph.addEdge(parent.id, k);
               if (graph.hasCycle())
               {
                  graph.removeEdge(parent.id, k);
                  available.remove(a);
               }
               else
               {
                  child = nonterminalInstances.get(k);
                  if (child == null)
                  {
                     child = new NonterminalCausation(parent.hierarchy, k);
                     nonterminalInstances.set(k, child);
                     newChildren.add(child);
                     open.add(child);
                  }
                  child.parents.add(parent);
                  parent.children.add(child);
               }
            }
            if (child == null)
            {
               for (int j = 0, k = newChildren.size(); j < k; j++)
               {
                  Causation newChild = newChildren.get(j);
                  graph.removeEdge(parent.id, newChild.id);
                  nonterminalInstances.set(newChild.id, null);
                  open.remove(newChild);
               }
               parent.children = new ArrayList<Causation>();
               expandTerminal(parent, terminalInstances);
               return;
            }
         }
      }
      if (open.size() > 0)
      {
         expandNonterminal(open, graph, nonterminalInstances, terminalInstances);
      }
   }


   // Expand causation to terminals.
   public static void expandTerminal(NonterminalCausation parent, ArrayList<TerminalCausation> terminalInstances)
   {
      int n = randomizer.nextInt(MAX_PRODUCTION_RHS_LENGTH - MIN_PRODUCTION_RHS_LENGTH + 1) + MIN_PRODUCTION_RHS_LENGTH;

      for (int i = 0; i < n; i++)
      {
         int               j     = randomizer.nextInt(NUM_TERMINALS);
         TerminalCausation child = terminalInstances.get(j);
         if (child == null)
         {
            child = new TerminalCausation(parent.hierarchy, j);
            terminalInstances.set(j, child);
         }
         child.parents.add(parent);
         parent.children.add(child);
      }
   }


   // Print causations.
   public static void printCausations()
   {
      System.out.println("hierarchies");
      for (int i = 0; i < causationHierarchies.size(); i++)
      {
         System.out.println("  hierarchy_" + i);
         Causation root = causationHierarchies.get(i).get(0);
         if (root != null)
         {
            if (root instanceof TerminalCausation)
            {
               TerminalCausation terminal = (TerminalCausation)root;
               terminal.printHierarchical("    ", null);
            }
            else
            {
               NonterminalCausation nonterminal = (NonterminalCausation)root;
               nonterminal.printHierarchical("    ", null, true);
            }
         }
      }
   }


   // Export causations graph (Graphviz dot format).
   public static void exportCausationsGraph(String filename, boolean treeFormat)
   {
      try
      {
         FileWriter  fileWriter  = new FileWriter(filename);
         PrintWriter printWriter = new PrintWriter(fileWriter);
         printWriter.println("digraph causations {");
         printWriter.println("hierarchies [label=\"hierarchies\", shape=triangle];");
         for (int i = 0; i < causationHierarchies.size(); i++)
         {
            Causation       root     = causationHierarchies.get(i).get(0);
            HashSet<String> vertices = new HashSet<String>();
            vertices.add("h" + i + " [label=\"hierarchy_" + i + "\", shape=triangle];");
            HashSet<String> edges = new HashSet<String>();
            edges.add("hierarchies -> h" + i);
            if (root instanceof TerminalCausation)
            {
               edges.add("h" + i + " -> h" + i + "_t" + root.id);
            }
            else
            {
               edges.add("h" + i + " -> h" + i + "_nt" + root.id);
            }
            listGraph(i, root, "", vertices, edges);
            for (String vertex : vertices)
            {
               printWriter.println(vertex);
            }
            for (String edge : edges)
            {
               printWriter.println(edge);
            }
         }
         printWriter.println("}");
         printWriter.close();
      }
      catch (IOException e)
      {
         System.err.println("Cannot save causations graph to file " + filename);
         System.exit(1);
      }
   }


   // List graph elements.
   private static void listGraph(int hierarchy, Causation vertex, String pathPrefix, HashSet<String> vertices, HashSet<String> edges)
   {
      if (vertex instanceof TerminalCausation)
      {
         TerminalCausation terminal = (TerminalCausation)vertex;
         String            s        = "(";
         for (int i = 0, j = terminal.features.size(); i < j; i++)
         {
            if (terminal.features.get(i))
            {
               s += i + ",";
            }
         }
         if (s.length() > 0)
         {
            s = s.substring(0, s.length() - 1);
         }
         s += ")";
         vertices.add("h" + hierarchy + "_t" + pathPrefix + vertex.id + " [label=\"" + vertex.id + s + "\", shape=square];");
      }
      else
      {
         vertices.add("h" + hierarchy + "_nt" + pathPrefix + vertex.id + " [label=\"" + vertex.id + "\", shape=circle];");
         NonterminalCausation nonTerminal = (NonterminalCausation)vertex;
         for (int i = 0; i < nonTerminal.children.size(); i++)
         {
            Causation child           = nonTerminal.children.get(i);
            String    childPathPrefix = "";
            if (TREE_FORMAT)
            {
               childPathPrefix = new String(pathPrefix) + vertex.id + "_" + i + "_";
            }
            if (child instanceof TerminalCausation)
            {
               edges.add("h" + hierarchy + "_nt" + pathPrefix + vertex.id + " -> h" + hierarchy + "_t" + childPathPrefix + child.id + " [label=\"" + i + "\"];");
            }
            else
            {
               edges.add("h" + hierarchy + "_nt" + pathPrefix + vertex.id + " -> h" + hierarchy + "_nt" + childPathPrefix + child.id + " [label=\"" + i + "\"];");
            }
            listGraph(hierarchy, child, childPathPrefix, vertices, edges);
         }
      }
   }


   // Generate causation paths.
   public static void generateCausationPaths(int numPaths)
   {
      // Generate causation hierarchy paths.
      causationPaths = new ArrayList<CausationPath> ();
      if (causationHierarchies.size() == 0)
      {
         return;
      }
      for (int i = 0; i < numPaths; i++)
      {
         int hierarchy = randomizer.nextInt(causationHierarchies.size());
         ArrayList<Causation> causationHierarchy = causationHierarchies.get(hierarchy);
         Causation            root = causationHierarchy.get(randomizer.nextInt(causationHierarchy.size()));
         CausationPath        path = new CausationPath(hierarchy, root.id);
         causationPaths.add(path);
         ArrayList<CausationTier> step = new ArrayList<CausationTier>();
         step.add(new CausationTier(root, 0));
         while (root instanceof NonterminalCausation)
         {
            NonterminalCausation nonterminal = (NonterminalCausation)root;
            root = nonterminal.children.get(0);
            step.add(new CausationTier(root, 0));
         }
         path.add(step);
         while (stepCausationPath(path, 0)) {}
         for (ArrayList<CausationTier> s : path.steps)
         {
            Collections.reverse(s);
         }
      }

      if (VERBOSE)
      {
         System.out.println("causation paths:");
         for (int i = 0; i < numPaths; i++)
         {
            CausationPath path = causationPaths.get(i);
            System.out.println("path=" + i + ", hierarchy=" + path.hierarchy);
            for (int j = 0; j < path.steps.size(); j++)
            {
               System.out.println("step=" + j);
               ArrayList<CausationTier> step = path.steps.get(j);
               for (int k = 0; k < step.size(); k++)
               {
                  CausationTier tier = step.get(k);
                  tier.print();
               }
            }
         }
      }
   }


   // Step along causation path.
   public static boolean stepCausationPath(CausationPath path, int context)
   {
      ArrayList<CausationTier> currentStep   = path.steps.get(path.steps.size() - 1);
      CausationTier            CausationTier = currentStep.get(context);
      if (CausationTier.causation instanceof TerminalCausation)
      {
         return(false);
      }
      if (stepCausationPath(path, context + 1))
      {
         return(true);
      }
      NonterminalCausation nonterminalCausation = (NonterminalCausation)CausationTier.causation;
      if (CausationTier.currentChild == nonterminalCausation.children.size() - 1)
      {
         return(false);
      }
      ArrayList<CausationTier> nextStep = new ArrayList<CausationTier>();
      for (int i = 0; i < context; i++)
      {
         CausationTier tier = currentStep.get(i);
         tier = new CausationTier(tier.causation, tier.currentChild);
         nextStep.add(tier);
      }
      CausationTier tier = currentStep.get(context);
      tier = new CausationTier(tier.causation, tier.currentChild);
      tier.currentChild++;
      nextStep.add(tier);
      do
      {
         NonterminalCausation parent = (NonterminalCausation)tier.causation;
         Causation            child  = parent.children.get(tier.currentChild);
         tier = new CausationTier(child, 0);
         nextStep.add(tier);
      } while (tier.causation instanceof NonterminalCausation);
      path.add(nextStep);
      return(true);
   }


   // Export NN dataset.
   public static void exportNNdataset()
   {
      exportNNdataset(NN_DATASET_FILENAME, NN_DATASET_TRAIN_FRACTION);
   }


   public static void exportNNdataset(String filename, float trainFraction)
   {
      int numPaths = causationPaths.size();
      int maxTiers = 0;

      if (VERBOSE)
      {
         System.out.println("export NN dataset");
      }
      for (int i = 0; i < numPaths; i++)
      {
         CausationPath path = causationPaths.get(i);
         for (int j = 0; j < path.steps.size(); j++)
         {
            ArrayList<CausationTier> step = path.steps.get(j);
            if (step.size() > maxTiers)
            {
               maxTiers = step.size();
            }
         }
      }

      causationFeatures = new ArrayList < ArrayList < CausationFeature >> ();
      for (int i = 0; i < maxTiers; i++)
      {
         causationFeatures.add(new ArrayList<CausationFeature>());
      }

      if (VERBOSE)
      {
         System.out.println("training dataset:");
      }
      int numTrain = (int)((float)numPaths * NN_DATASET_TRAIN_FRACTION);
      ArrayList < ArrayList < Float >> X_train = new ArrayList < ArrayList < Float >> ();
      ArrayList < ArrayList < Float >> y_train = new ArrayList < ArrayList < Float >> ();
      for (int i = 0; i < numTrain; i++)
      {
         CausationPath path = causationPaths.get(i);
         if (VERBOSE)
         {
            path.print();
            System.out.println("data:");
         }
         int pathLength = 0;
         int p          = path.steps.size() - 1;
         for (int j = 0; j < p; j++)
         {
            ArrayList<CausationTier> xstep              = path.steps.get(j);
            ArrayList<CausationTier> ystep              = path.steps.get(j + 1);
            Causation                xcausation         = xstep.get(0).causation;
            TerminalCausation        xterminalCausation = (TerminalCausation)xcausation;
            Causation                ycausation         = ystep.get(0).causation;
            TerminalCausation        yterminalCausation = (TerminalCausation)ycausation;
            if ((xstep.size() > 1) && (xstep.get(1).currentChild == 0))
            {
               int id = randomizer.nextInt(NUM_TERMINALS);
               while (id != xcausation.id)
               {
                  ArrayList<Float>  X_train_step    = new ArrayList<Float>();
                  ArrayList<Float>  y_train_step    = new ArrayList<Float>();
                  TerminalCausation randomCausation = new TerminalCausation(xcausation.hierarchy, id);
                  if (VERBOSE)
                  {
                     System.out.print("X: *");
                     randomCausation.print();
                     System.out.print("y: ");
                     yterminalCausation.print();
                  }
                  for (int k = 0; k < maxTiers; k++)
                  {
                     if (k == 0)
                     {
                        for (int q = 0; q < NUM_DIMENSIONS; q++)
                        {
                           if (randomCausation.features.get(q))
                           {
                              X_train_step.add(1.0f);
                           }
                           else
                           {
                              X_train_step.add(0.0f);
                           }
                        }
                        for (int q = 0; q < NUM_DIMENSIONS; q++)
                        {
                           if (xterminalCausation.features.get(q))
                           {
                              y_train_step.add(1.0f);
                           }
                           else
                           {
                              y_train_step.add(0.0f);
                           }
                        }
                     }
                     else
                     {
                        for (int q = 0; q < NUM_DIMENSIONS; q++)
                        {
                           X_train_step.add(0.0f);
                           y_train_step.add(0.0f);
                        }
                     }
                  }
                  X_train.add(X_train_step);
                  y_train.add(y_train_step);
                  pathLength++;
                  id = randomizer.nextInt(NUM_TERMINALS);
               }
            }
            if (VERBOSE)
            {
               System.out.print("X: ");
               xterminalCausation.print();
               System.out.print("y: ");
               yterminalCausation.print();
            }
            ArrayList<Float> X_train_step = new ArrayList<Float>();
            ArrayList<Float> y_train_step = new ArrayList<Float>();
            for (int k = 0; k < maxTiers; k++)
            {
               if (k == 0)
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     if (xterminalCausation.features.get(q))
                     {
                        X_train_step.add(1.0f);
                     }
                     else
                     {
                        X_train_step.add(0.0f);
                     }
                  }
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     if (yterminalCausation.features.get(q))
                     {
                        y_train_step.add(1.0f);
                     }
                     else
                     {
                        y_train_step.add(0.0f);
                     }
                  }
               }
               else
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     X_train_step.add(0.0f);
                     y_train_step.add(0.0f);
                  }
               }
            }
            X_train.add(X_train_step);
            y_train.add(y_train_step);
            pathLength++;
         }
         if (VERBOSE)
         {
            System.out.println("training path length=" + pathLength);
         }
      }
      if (VERBOSE)
      {
         System.out.println("testing dataset:");
      }
      int numTest = numPaths - numTrain;
      ArrayList < ArrayList < Float >> X_test = new ArrayList < ArrayList < Float >> ();
      ArrayList < ArrayList < Float >> y_test = new ArrayList < ArrayList < Float >> ();
      for (int i = numTrain; i < numPaths; i++)
      {
         CausationPath path = causationPaths.get(i);
         if (VERBOSE)
         {
            path.print();
            System.out.println("data:");
         }
         int p          = path.steps.size() - 1;
         int pathLength = 0;
         for (int j = 0; j < p; j++)
         {
            ArrayList<CausationTier> xstep              = path.steps.get(j);
            ArrayList<CausationTier> ystep              = path.steps.get(j + 1);
            Causation                xcausation         = xstep.get(0).causation;
            TerminalCausation        xterminalCausation = (TerminalCausation)xcausation;
            Causation                ycausation         = ystep.get(0).causation;
            TerminalCausation        yterminalCausation = (TerminalCausation)ycausation;
            if ((xstep.size() > 1) && (xstep.get(1).currentChild == 0))
            {
               int id = randomizer.nextInt(NUM_TERMINALS);
               while (id != xcausation.id)
               {
                  ArrayList<Float>  X_test_step     = new ArrayList<Float>();
                  ArrayList<Float>  y_test_step     = new ArrayList<Float>();
                  TerminalCausation randomCausation = new TerminalCausation(xcausation.hierarchy, id);
                  if (VERBOSE)
                  {
                     System.out.print("X: *");
                     randomCausation.print();
                     System.out.print("y: ");
                     yterminalCausation.print();
                  }
                  for (int k = 0; k < maxTiers; k++)
                  {
                     if (k == 0)
                     {
                        for (int q = 0; q < NUM_DIMENSIONS; q++)
                        {
                           if (randomCausation.features.get(q))
                           {
                              X_test_step.add(1.0f);
                           }
                           else
                           {
                              X_test_step.add(0.0f);
                           }
                        }
                        for (int q = 0; q < NUM_DIMENSIONS; q++)
                        {
                           if (xterminalCausation.features.get(q))
                           {
                              y_test_step.add(1.0f);
                           }
                           else
                           {
                              y_test_step.add(0.0f);
                           }
                        }
                     }
                     else
                     {
                        for (int q = 0; q < NUM_DIMENSIONS; q++)
                        {
                           X_test_step.add(0.0f);
                           y_test_step.add(0.0f);
                        }
                     }
                  }
                  X_test.add(X_test_step);
                  y_test.add(y_test_step);
                  pathLength++;
                  id = randomizer.nextInt(NUM_TERMINALS);
               }
            }
            if (VERBOSE)
            {
               System.out.print("X: ");
               xterminalCausation.print();
               System.out.print("y: ");
               yterminalCausation.print();
            }
            ArrayList<Float> X_test_step = new ArrayList<Float>();
            ArrayList<Float> y_test_step = new ArrayList<Float>();
            for (int k = 0; k < maxTiers; k++)
            {
               if (k == 0)
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     if (xterminalCausation.features.get(q))
                     {
                        X_test_step.add(1.0f);
                     }
                     else
                     {
                        X_test_step.add(0.0f);
                     }
                  }
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     if (yterminalCausation.features.get(q))
                     {
                        y_test_step.add(1.0f);
                     }
                     else
                     {
                        y_test_step.add(0.0f);
                     }
                  }
               }
               else
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     X_test_step.add(0.0f);
                     y_test_step.add(0.0f);
                  }
               }
            }
            X_test.add(X_test_step);
            y_test.add(y_test_step);
            pathLength++;
         }
         if (VERBOSE)
         {
            System.out.println("testing path length=" + pathLength);
         }
      }

      try
      {
         FileWriter  fileWriter  = new FileWriter(filename);
         PrintWriter printWriter = new PrintWriter(fileWriter);

         printWriter.println("X_train_shape = [ " + X_train.size() + ", " + (maxTiers * NUM_DIMENSIONS) + " ]");
         printWriter.println("X_train = [");
         for (int i = 0, j = X_train.size(); i < j; i++)
         {
            ArrayList<Float> X_train_step = X_train.get(i);
            for (int k = 0, q = X_train_step.size(); k < q; k++)
            {
               printWriter.print(X_train_step.get(k) + "");
               if ((i != j - 1) || (k != q - 1))
               {
                  printWriter.print(",");
               }
            }
            printWriter.println();
         }
         printWriter.println("]");
         printWriter.println("y_train_shape = [ " + y_train.size() + ", " + (maxTiers * NUM_DIMENSIONS) + " ]");
         printWriter.println("y_train = [");
         for (int i = 0, j = y_train.size(); i < j; i++)
         {
            ArrayList<Float> y_train_step = y_train.get(i);
            for (int k = 0, q = y_train_step.size(); k < q; k++)
            {
               printWriter.print(y_train_step.get(k) + "");
               if ((i != j - 1) || (k != q - 1))
               {
                  printWriter.print(",");
               }
            }
            printWriter.println();
         }
         printWriter.println("]");
         printWriter.println("X_test_shape = [ " + X_test.size() + ", " + (maxTiers * NUM_DIMENSIONS) + " ]");
         printWriter.println("X_test = [");
         for (int i = 0, j = X_test.size(); i < j; i++)
         {
            ArrayList<Float> X_test_step = X_test.get(i);
            for (int k = 0, q = X_test_step.size(); k < q; k++)
            {
               printWriter.print(X_test_step.get(k) + "");
               if ((i != j - 1) || (k != q - 1))
               {
                  printWriter.print(",");
               }
            }
            printWriter.println();
         }
         printWriter.println("]");
         printWriter.println("y_test_shape = [ " + y_test.size() + ", " + (maxTiers * NUM_DIMENSIONS) + " ]");
         printWriter.println("y_test = [");
         for (int i = 0, j = y_test.size(); i < j; i++)
         {
            ArrayList<Float> y_test_step = y_test.get(i);
            for (int k = 0, q = y_test_step.size(); k < q; k++)
            {
               printWriter.print(y_test_step.get(k) + "");
               if ((i != j - 1) || (k != q - 1))
               {
                  printWriter.print(",");
               }
            }
            printWriter.println();
         }
         printWriter.println("]");
         printWriter.close();
      }
      catch (IOException e)
      {
         System.err.println("Cannot write NN dataset to file " + filename);
         System.exit(1);
      }
   }


   // Export RNN dataset.
   public static void exportRNNdataset()
   {
      exportDataset(RNN_DATASET_FILENAME, RNN_DATASET_TRAIN_FRACTION);
   }


   // Export TCN dataset.
   public static void exportTCNdataset()
   {
      exportDataset(TCN_DATASET_FILENAME, TCN_DATASET_TRAIN_FRACTION);
   }


   public static void exportDataset(String filename, float trainFraction)
   {
   }


   // Learn causations with NN.
   public static LearningResults learnCausationsNN(String filename)
   {
      if (VERBOSE)
      {
         System.out.println("Learn NN");
      }
      try
      {
         InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(NN_FILENAME);
         if (in == null)
         {
            System.err.println("Cannot access " + NN_FILENAME);
            System.exit(1);
         }
         File             pythonScript = new File(NN_FILENAME);
         FileOutputStream out          = new FileOutputStream(pythonScript);
         byte[] buffer = new byte[1024];
         int bytesRead;
         while ((bytesRead = in.read(buffer)) != -1)
         {
            out.write(buffer, 0, bytesRead);
         }
         out.close();
      }
      catch (Exception e)
      {
         System.err.println("Cannot create " + NN_FILENAME);
         System.exit(1);
      }
      new File(NN_RESULTS_FILENAME).delete();
      ArrayList<String> commandList = new ArrayList<>();
      commandList.add("python");
      commandList.add(NN_FILENAME);
      String[] hidden = NN_HIDDEN.split(",");
      for (String neurons : hidden)
      {
         commandList.add("-h");
         commandList.add(neurons);
      }
      commandList.add("-e");
      commandList.add(NN_EPOCHS + "");
      if (!VERBOSE)
      {
         commandList.add("-q");
      }
      ProcessBuilder processBuilder = new ProcessBuilder(commandList);
      processBuilder.inheritIO();
      Process process;
      try
      {
         process = processBuilder.start();
         process.waitFor();
      }
      catch (InterruptedException e) {}
      catch (IOException e)
      {
         System.err.println("Cannot run " + NN_FILENAME + ":" + e.getMessage());
         System.exit(1);
      }
      if (VERBOSE)
      {
         System.out.println("Results written to " + NN_RESULTS_FILENAME);
      }

      // Fetch the results.
      LearningResults results = new LearningResults();
      try
      {
         BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(NN_RESULTS_FILENAME)));
         String         json;
         if ((json = br.readLine()) != null)
         {
            JSONObject jObj = null;
            try
            {
               jObj = new JSONObject(json);
            }
            catch (JSONException e)
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            String train_prediction_errors = jObj.getString("train_prediction_errors");
            if ((train_prediction_errors == null) || train_prediction_errors.isEmpty())
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            try
            {
               results.train_prediction_errors = Integer.parseInt(train_prediction_errors);
            }
            catch (NumberFormatException e)
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            String train_total_predictions = jObj.getString("train_total_predictions");
            if ((train_total_predictions == null) || train_total_predictions.isEmpty())
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            try
            {
               results.train_total_predictions = Integer.parseInt(train_total_predictions);
            }
            catch (NumberFormatException e)
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            String train_error_pct = jObj.getString("train_error_pct");
            if ((train_error_pct == null) || train_error_pct.isEmpty())
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            try
            {
               results.train_error_pct = Float.parseFloat(train_error_pct);
            }
            catch (NumberFormatException e)
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            String test_prediction_errors = jObj.getString("test_prediction_errors");
            if ((test_prediction_errors == null) || test_prediction_errors.isEmpty())
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            try
            {
               results.test_prediction_errors = Integer.parseInt(test_prediction_errors);
            }
            catch (NumberFormatException e)
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            String test_total_predictions = jObj.getString("test_total_predictions");
            if ((test_total_predictions == null) || test_total_predictions.isEmpty())
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            try
            {
               results.test_total_predictions = Integer.parseInt(test_total_predictions);
            }
            catch (NumberFormatException e)
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            String test_error_pct = jObj.getString("test_error_pct");
            if ((test_error_pct == null) || test_error_pct.isEmpty())
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
            try
            {
               results.test_error_pct = Float.parseFloat(test_error_pct);
            }
            catch (NumberFormatException e)
            {
               System.err.println("Error parsing results file " + NN_RESULTS_FILENAME);
               System.exit(1);
            }
         }
         else
         {
            System.err.println("Cannot read results file " + NN_RESULTS_FILENAME);
            System.exit(1);
         }
         br.close();
      }
      catch (Exception e)
      {
         System.err.println("Cannot read results file " + NN_RESULTS_FILENAME + ":" + e.getMessage());
         System.exit(1);
      }
      return(results);
   }


   // Learn causations with RNN.
   public static void learnCausationsRNN(String filename)
   {
   }


   // Learn causations with TCN.
   public static void learnCausationsTCN(String filename)
   {
   }
}
