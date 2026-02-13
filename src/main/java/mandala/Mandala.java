// For conditions of distribution and use, see copyright notice in LICENSE.txt.

// Mandala is a hybrid of Morphognosis and Mona.
// Generate causation hierarchies and learn paths through the hierarchies.

package mandala;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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

public class Mandala
{
   // Generation parameters.
   public static int   NUM_CAUSATION_HIERARCHIES          = 1;
   public static int   NUM_NONTERMINALS                   = 10;
   public static int   NUM_TERMINALS                      = 20;
   public static int   NUM_INTERSTITIAL_TERMINALS         = 5;
   public static int   MAX_INTERSTITIAL_TERMINAL_SEQUENCE = 10;
   public static int   MIN_PRODUCTION_RHS_LENGTH          = 2;
   public static int   MAX_PRODUCTION_RHS_LENGTH          = 2;
   public static float TERMINAL_PRODUCTION_PROBABILITY    = 0.25f;

   // Sizes.
   public static int NUM_DIMENSIONS = 64;
   public static int NUM_FEATURES   = 3;

   // Save/load file name.
   public static String MANDALA_FILENAME = "mandala.dat";

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


      // Encode features.
      public static ArrayList<Integer> encodeFeatures(int hierarchy, int id)
      {
         String seedString = hierarchy + "_" + id + "_";
         Random r          = new Random(seedString.hashCode());

         ArrayList<Integer> features = new ArrayList<Integer>();
         for (int i = 0; i < NUM_FEATURES; i++)
         {
            int j = 0;
            int k = 100;
            for ( ; j < k; j++)
            {
               int n = r.nextInt(NUM_DIMENSIONS);
               int p = 0;
               int q = features.size();
               for ( ; p < q; p++)
               {
                  if (features.get(p) == n)
                  {
                     break;
                  }
               }
               if (p == q)
               {
                  features.add(n);
                  break;
               }
            }
            if (j == k)
            {
               System.err.println("Cannot encode features");
               System.exit(1);
            }
         }
         Collections.sort(features);
         return(features);
      }


      public static ArrayList<Integer> encodeFeatures(ArrayList<Integer> features1, ArrayList<Integer> features2)
      {
         String seedString = "";

         for (int i : features1)
         {
            seedString += i + "_";
         }
         Random             r        = new Random(seedString.hashCode());
         ArrayList<Integer> features = new ArrayList<Integer>();
         for (int i = 0; i < NUM_FEATURES; i++)
         {
            int j = 0;
            int k = 100;
            for ( ; j < k; j++)
            {
               int n = r.nextInt(NUM_DIMENSIONS);
               int p = 0;
               int q = features.size();
               for ( ; p < q; p++)
               {
                  if (features.get(p) == n)
                  {
                     break;
                  }
               }
               if (p == q)
               {
                  features.add(n);
                  break;
               }
            }
            if (j == k)
            {
               System.err.println("Cannot encode features");
               System.exit(1);
            }
         }
         Collections.sort(features);
         return(features);
      }
   };

   // Terminal causation.
   public static class TerminalCausation extends Causation
   {
      public ArrayList<Integer> features;

      public TerminalCausation(int hierarchy, int id)
      {
         super(hierarchy, id);
         features = encodeFeatures(hierarchy, id);
      }


      // Save.
      public void save(DataOutputStream writer) throws IOException
      {
         Utility.saveInt(writer, hierarchy);
         Utility.saveInt(writer, id);
      }


      // Load.
      public static TerminalCausation load(DataInputStream reader) throws IOException
      {
         TerminalCausation terminalCausation =
            new TerminalCausation(Utility.loadInt(reader), Utility.loadInt(reader));

         return(terminalCausation);
      }


      // Print.
      public void print()
      {
         System.out.print("hierarchy=" + hierarchy);
         System.out.print(", id=" + id);
         System.out.print(", features:");
         for (int i = 0, j = features.size(); i < j; i++)
         {
            System.out.print(features.get(i));
            if (i < j - 1)
            {
               System.out.print(",");
            }
         }
         System.out.print(", parents:");
         for (Causation p : parents)
         {
            System.out.print(" " + p.id);
         }
         System.out.println();
      }


      // Hierarchical print.
      public void printHierarchical(String indent, String childNum)
      {
         System.out.print(indent);
         System.out.print("terminal id=" + id);
         System.out.print(", features:");
         for (int i = 0, j = features.size(); i < j; i++)
         {
            System.out.print(features.get(i));
            if (i < j - 1)
            {
               System.out.print(",");
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


      // Save.
      public void save(DataOutputStream writer) throws IOException
      {
         Utility.saveInt(writer, hierarchy);
         Utility.saveInt(writer, id);
      }


      // Load.
      public static NonterminalCausation load(DataInputStream reader) throws IOException
      {
         NonterminalCausation nonterminalCausation =
            new NonterminalCausation(Utility.loadInt(reader), Utility.loadInt(reader));

         return(nonterminalCausation);
      }


      // Print.
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


      // Hierarchical print.
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

   // Causations graph.
   public static String  CAUSATIONS_GRAPH_FILENAME = "mandala_causations.dot";
   public static boolean TREE_FORMAT = true;

   // Causation paths.
   public static class CausationTier
   {
      public Causation causation;
      public int       currentChild;

      // COnstructor.
      public CausationTier(Causation causation, int currentChild)
      {
         this.causation    = causation;
         this.currentChild = currentChild;
      }


      // Print..
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

   // Causation paths.
   public static int NUM_CAUSATION_PATHS = 20;

   public static class CausationPath
   {
      int hierarchy;
      int id;
      ArrayList < ArrayList < CausationTier >> steps;

      // Constructor.
      public CausationPath(int hierarchy, int id)
      {
         this.hierarchy = hierarchy;
         this.id        = id;
         steps          = new ArrayList < ArrayList < CausationTier >> ();
      }


      // Add step.
      public void add(ArrayList<CausationTier> step)
      {
         steps.add(step);
      }


      // Print.
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

   // Maximum context feature tier.
   public static int MAX_CONTEXT_FEATURE_TIER = 5;

   // Feature value durations.
   public static String             FEATURE_VALUE_DURATION_TYPE = "maximum";
   public static ArrayList<Integer> featureValueDurations;

   // Context features.
   public static class ContextFeatures
   {
      public ArrayList<Integer> features;
      public float              value;
      public int                age;
      public int                tier;

      // Constructors.
      public ContextFeatures(Causation causation, int tier)
      {
         features  = Causation.encodeFeatures(causation.hierarchy, causation.id);
         value     = 1.0f;
         age       = 0;
         this.tier = tier;
      }


      public ContextFeatures(ContextFeatures contextFeatures1, ContextFeatures contextFeatures2, int tier)
      {
         features  = Causation.encodeFeatures(contextFeatures1.features, contextFeatures2.features);
         value     = 1.0f;
         age       = 0;
         this.tier = tier;
      }


      // Duplicate?
      public boolean duplicate(ContextFeatures contextFeatures)
      {
         if (!featuresEqual(contextFeatures))
         {
            return(false);
         }
         return(true);
      }


      // Features are equal?
      public boolean featuresEqual(ContextFeatures contextFeatures)
      {
         return(featuresEqual(contextFeatures.features));
      }


      public boolean featuresEqual(ArrayList<Integer> otherFeatures)
      {
         if (features.size() != otherFeatures.size())
         {
            return(false);
         }
         for (int i : features)
         {
            if (!otherFeatures.contains(i))
            {
               return(false);
            }
         }
         return(true);
      }


      // Expire value.
      // Return true if not expired, else false.
      public boolean expire()
      {
         if (featureValueDurations != null)
         {
            int duration = featureValueDurations.get(tier);
            if (age >= duration)
            {
               value = 0.0f;
               return(false);
            }
            else
            {
               age++;
               return(true);
            }
         }
         else
         {
            value = 0.0f;
            return(false);
         }
      }


      // Print.
      public void print()
      {
         System.out.print("features:");
         for (int i = 0, j = features.size(); i < j; i++)
         {
            System.out.print(features.get(i));
            if (i < j - 1)
            {
               System.out.print(",");
            }
         }
         System.out.println(", tier=" + tier + ", value=" + value + ", age=" + age);
      }
   };
   public static ArrayList<ContextFeatures> contextTiers;

   // Datasets.
   public static String NN_DATASET_FILENAME        = "mandala_nn_dataset.py";
   public static float  NN_DATASET_TRAIN_FRACTION  = 0.5f;
   public static String RNN_DATASET_FILENAME       = "mandala_rnn_dataset.py";
   public static float  RNN_DATASET_TRAIN_FRACTION = 0.5f;

   // Learners.
   public static String NN_FILENAME          = "mandala_nn.py";
   public static String NN_NEURONS           = "128,128,128";
   public static int    NN_EPOCHS            = 500;
   public static String NN_RESULTS_FILENAME  = "mandala_nn_results.json";
   public static String RNN_FILENAME         = "mandala_rnn.py";
   public static String RNN_NEURONS          = "128";
   public static int    RNN_EPOCHS           = 500;
   public static String RNN_RESULTS_FILENAME = "mandala_rnn_results.json";

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
   public static int    RANDOM_SEED = 45;
   public static Random randomizer  = null;

   // Verbosity.
   public static boolean VERBOSE = true;

   // Usage.
   public static final String Usage =
      "Usage:\n" +
      "  New run:\n" +
      "    java mandala.Mandala\n" +
      "      [-numCausationHierarchies <quantity> (default=" + NUM_CAUSATION_HIERARCHIES + ")]\n" +
      "      [-numNonterminals <quantity> (default=" + NUM_NONTERMINALS + ")]\n" +
      "      [-numTerminals <quantity> (default=" + NUM_TERMINALS + ")]\n" +
      "      [-numInterstitialTerminals <quantity> (default=" + NUM_INTERSTITIAL_TERMINALS + ", if 0 resort to non-interstitial terminals)]\n" +
      "      [-minProductionRightHandSideLength <quantity> (default=" + MIN_PRODUCTION_RHS_LENGTH + ")]\n" +
      "      [-maxProductionRightHandSideLength <quantity> (default=" + MAX_PRODUCTION_RHS_LENGTH + ")]\n" +
      "      [-terminalProductionProbability <probability> (default=" + TERMINAL_PRODUCTION_PROBABILITY + ")]\n" +
      "      [-numDimensions <quantity> (default=" + NUM_DIMENSIONS + ")]\n" +
      "      [-numFeatures <quantity> (default=" + NUM_FEATURES + ")]\n" +
      "      [-maxInterstitialTerminalSequence <length> (default=" + MAX_INTERSTITIAL_TERMINAL_SEQUENCE + ")]\n" +
      "      [-exportCausationsGraph [<file name> (Graphviz dot format, default=" + CAUSATIONS_GRAPH_FILENAME + ")]\n" +
      "          [-treeFormat \"true\" | \"false\" (default=" + TREE_FORMAT + ")]]\n" +
      "      [-numCausationPaths <quantity> (default=" + NUM_CAUSATION_PATHS + ")]\n" +
      "      [-maxContextFeatureTier <value> (default=" + MAX_CONTEXT_FEATURE_TIER + ")]\n" +
      "      [-featureValueDurationType \"minimum\" | \"expected\" | \"maximum\" (default=" + FEATURE_VALUE_DURATION_TYPE + ")]\n" +
      "      [-NNdatasetTrainFraction <fraction> (default=" + NN_DATASET_TRAIN_FRACTION + ")]\n" +
      "      [-NNneurons<number of neurons> (comma-separated for additional layers) (default=" + NN_NEURONS + ")]\n" +
      "      [-NNepochs <number of epochs> (default=" + NN_EPOCHS + ")]\n" +
      "      [-RNNdatasetTrainFraction <fraction> (default=" + RNN_DATASET_TRAIN_FRACTION + ")]\n" +
      "      [-RNNneurons <number of neurons> (comma-separated for additional layers) (default=" + RNN_NEURONS + ")]\n" +
      "      [-RNNepochs <number of epochs> (default=" + RNN_EPOCHS + ")]\n" +
      "      [-randomSeed <seed> (default=" + RANDOM_SEED + ")]\n" +
      "      [-quiet]\n" +
      "      [-save [<file name> (default=" + MANDALA_FILENAME + ")]\n" +
      "  Load:\n" +
      "    java mandala.Mandala\n" +
      "      -load [<file name> (default=" + MANDALA_FILENAME + ")]\n" +
      "      [-maxInterstitialTerminalSequence <length> (default=" + MAX_INTERSTITIAL_TERMINAL_SEQUENCE + ")]\n" +
      "      [-exportCausationsGraph [<file name> (Graphviz dot format, default=" + CAUSATIONS_GRAPH_FILENAME + ")]\n" +
      "          [-treeFormat \"true\" | \"false\" (default=" + TREE_FORMAT + ")]]\n" +
      "      [-numCausationPaths <quantity> (default=" + NUM_CAUSATION_PATHS + ")]\n" +
      "      [-maxContextFeatureTier <value> (default=" + MAX_CONTEXT_FEATURE_TIER + ")]\n" +
      "      [-featureValueDurationType \"minimum\" | \"expected\" | \"maximum\" (default=" + FEATURE_VALUE_DURATION_TYPE + ")]\n" +
      "      [-NNdatasetTrainFraction <fraction> (default=" + NN_DATASET_TRAIN_FRACTION + ")]\n" +
      "      [-NNneurons<number of neurons> (comma-separated for additional layers) (default=" + NN_NEURONS + ")]\n" +
      "      [-NNepochs <number of epochs> (default=" + NN_EPOCHS + ")]\n" +
      "      [-RNNdatasetTrainFraction <fraction> (default=" + RNN_DATASET_TRAIN_FRACTION + ")]\n" +
      "      [-RNNneurons <number of neurons> (comma-separated for additional layers) (default=" + RNN_NEURONS + ")]\n" +
      "      [-RNNepochs <number of epochs> (default=" + RNN_EPOCHS + ")]\n" +
      "      [-randomSeed <seed> (default=" + RANDOM_SEED + ")]\n" +
      "      [-quiet]\n" +
      "  Help:\n" +
      "    java mandala.Mandala -help\n" +
      "Exit codes:\n" +
      "  0=success\n" +
      "  1=error";

   // Main.
   public static void main(String[] args)
   {
      boolean gotNew  = false;
      boolean gotLoad = false;
      boolean gotSave = false;
      boolean gotExportCausationsGraph = false;
      boolean gotTreeFormat            = false;

      for (int i = 0; i < args.length; i++)
      {
         if (args[i].equals("-load"))
         {
            if (((i + 1) < args.length) && !args[(i + 1)].startsWith("-"))
            {
               i++;
               MANDALA_FILENAME = args[i];
            }
            gotLoad = true;
            continue;
         }
         if (args[i].equals("-save"))
         {
            if (((i + 1) < args.length) && !args[(i + 1)].startsWith("-"))
            {
               i++;
               MANDALA_FILENAME = args[i];
            }
            gotSave = true;
            gotNew  = true;
            continue;
         }
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
            gotNew = true;
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
            gotNew = true;
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
            gotNew = true;
            continue;
         }
         if (args[i].equals("-numInterstitialTerminals"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid numInterstitialTerminals option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NUM_INTERSTITIAL_TERMINALS = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid numInterstitialTerminals option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NUM_INTERSTITIAL_TERMINALS < 0)
            {
               System.err.println("Invalid numInterstitialTerminals option");
               System.err.println(Usage);
               System.exit(1);
            }
            gotNew = true;
            continue;
         }
         if (args[i].equals("-maxInterstitialTerminalSequence"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid maxInterstitialTerminals option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               MAX_INTERSTITIAL_TERMINAL_SEQUENCE = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid maxInterstitialTerminalSequence option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (MAX_INTERSTITIAL_TERMINAL_SEQUENCE < 0)
            {
               System.err.println("Invalid maxInterstitialTerminalSequence option");
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
            gotNew = true;
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
            gotNew = true;
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
            gotNew = true;
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
            gotNew = true;
            continue;
         }
         if (args[i].equals("-numFeatures"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid numFeatures option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NUM_FEATURES = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid numFeatures option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NUM_FEATURES < 1)
            {
               System.err.println("Invalid numFeatures option");
               System.err.println(Usage);
               System.exit(1);
            }
            gotNew = true;
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
            if (NUM_CAUSATION_PATHS < 2)
            {
               System.err.println("Invalid numCausationPaths option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-maxContextFeatureTier"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid maxContextFeatureTier option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               MAX_CONTEXT_FEATURE_TIER = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid maxContextFeatureTier option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (MAX_CONTEXT_FEATURE_TIER < -1)
            {
               System.err.println("Invalid maxContextFeatureTier option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-featureValueDurationType"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid featureValueDurationType option");
               System.err.println(Usage);
               System.exit(1);
            }
            FEATURE_VALUE_DURATION_TYPE = new String(args[i]);
            if (!FEATURE_VALUE_DURATION_TYPE.equals("minimum") && !FEATURE_VALUE_DURATION_TYPE.equals("expected") &&
                !FEATURE_VALUE_DURATION_TYPE.equals("maximum"))
            {
               System.err.println("Invalid featureValueDurationType option");
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
         if (args[i].equals("-NNneurons"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid NNneurons option");
               System.err.println(Usage);
               System.exit(1);
            }
            NN_NEURONS = args[i].replaceAll("\\s", "");
            if (NN_NEURONS.isEmpty())
            {
               System.err.println("Invalid NNneurons option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-NNepochs"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid NNepochs option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               NN_EPOCHS = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid NNepochs option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (NN_EPOCHS < 0)
            {
               System.err.println("Invalid NNepochs option");
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
         if (args[i].equals("-RNNneurons"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid RNNneurons option");
               System.err.println(Usage);
               System.exit(1);
            }
            RNN_NEURONS = args[i].replaceAll("\\s", "");
            if (RNN_NEURONS.isEmpty())
            {
               System.err.println("Invalid RNNneurons option");
               System.err.println(Usage);
               System.exit(1);
            }
            continue;
         }
         if (args[i].equals("-RNNepochs"))
         {
            i++;
            if (i >= args.length)
            {
               System.err.println("Invalid RNNepochs option");
               System.err.println(Usage);
               System.exit(1);
            }
            try
            {
               RNN_EPOCHS = Integer.parseInt(args[i]);
            }
            catch (NumberFormatException e) {
               System.err.println("Invalid RNNepochs option");
               System.err.println(Usage);
               System.exit(1);
            }
            if (RNN_EPOCHS < 0)
            {
               System.err.println("Invalid RNNepochs option");
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
      if (gotLoad && gotNew)
      {
         System.err.println(Usage);
         System.exit(1);
      }
      if (MIN_PRODUCTION_RHS_LENGTH > MAX_PRODUCTION_RHS_LENGTH)
      {
         System.err.println(Usage);
         System.exit(1);
      }
      if (NUM_DIMENSIONS < NUM_FEATURES)
      {
         System.err.println(Usage);
         System.exit(1);
      }
      if (!gotExportCausationsGraph && gotTreeFormat)
      {
         System.err.println(Usage);
         System.exit(1);
      }

      if (VERBOSE)
      {
         System.out.print("options: ");
         for (String arg : args)
         {
            System.out.print(arg + " ");
         }
         System.out.println();
      }

      // Load?
      if (gotLoad)
      {
         try
         {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(MANDALA_FILENAME)));

            // Load parameters.
            NUM_CAUSATION_HIERARCHIES       = Utility.loadInt(reader);
            NUM_NONTERMINALS                = Utility.loadInt(reader);
            NUM_TERMINALS                   = Utility.loadInt(reader);
            NUM_INTERSTITIAL_TERMINALS      = Utility.loadInt(reader);
            MIN_PRODUCTION_RHS_LENGTH       = Utility.loadInt(reader);
            MAX_PRODUCTION_RHS_LENGTH       = Utility.loadInt(reader);
            TERMINAL_PRODUCTION_PROBABILITY = Utility.loadFloat(reader);
            NUM_DIMENSIONS                  = Utility.loadInt(reader);
            NUM_FEATURES = Utility.loadInt(reader);

            // Load causations.
            causationHierarchies = new ArrayList < ArrayList < Causation >> ();
            for (int i = 0; i < NUM_CAUSATION_HIERARCHIES; i++)
            {
               ArrayList<Causation> causations = new ArrayList<Causation>();
               if (NUM_NONTERMINALS > 0)
               {
                  ArrayList<NonterminalCausation> nonterminalInstances = new ArrayList<NonterminalCausation>();
                  for (int j = 0; j < NUM_NONTERMINALS; j++)
                  {
                     nonterminalInstances.add(new NonterminalCausation(i, j));
                  }
                  ArrayList<TerminalCausation> terminalInstances = new ArrayList<TerminalCausation>();
                  for (int j = 0; j < NUM_TERMINALS; j++)
                  {
                     terminalInstances.add(new TerminalCausation(i, j));
                  }
                  int k = Utility.loadInt(reader);
                  for (int j = 0; j < k; j++)
                  {
                     int id = Utility.loadInt(reader);
                     NonterminalCausation nonterminalCausation = nonterminalInstances.get(id);
                     causations.add(nonterminalCausation);
                     if (Utility.loadInt(reader) == 0)
                     {
                        for (int p = 0, q = Utility.loadInt(reader); p < q; p++)
                        {
                           NonterminalCausation child = nonterminalInstances.get(Utility.loadInt(reader));
                           nonterminalCausation.children.add(child);
                           child.parents.add(nonterminalCausation);
                        }
                     }
                     else
                     {
                        for (int p = 0, q = Utility.loadInt(reader); p < q; p++)
                        {
                           TerminalCausation child = terminalInstances.get(Utility.loadInt(reader));
                           nonterminalCausation.children.add(child);
                           child.parents.add(nonterminalCausation);
                        }
                     }
                  }
               }
               else if (NUM_TERMINALS > 0)
               {
                  causations.add(new TerminalCausation(i, 0));
               }
               causationHierarchies.add(causations);
            }

            // Load causation paths.
            NUM_CAUSATION_PATHS = Utility.loadInt(reader);
            causationPaths      = new ArrayList<CausationPath>();
            for (int i = 0; i < NUM_CAUSATION_PATHS; i++)
            {
               causationPaths.add(new CausationPath(Utility.loadInt(reader), Utility.loadInt(reader)));
            }

            reader.close();
         }
         catch (Exception e)
         {
            System.err.println("Error loading file " + MANDALA_FILENAME + ":" + e.getMessage());
            System.exit(1);
         }
      }
      else
      {
         // Generate causations.
         randomizer           = new Random(RANDOM_SEED);
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
         causationPaths = null;
      }

      // Print parameters and causations.
      if (VERBOSE)
      {
         System.out.println("parameters:");
         System.out.println("NUM_CAUSATION_HIERARCHIES=" + NUM_CAUSATION_HIERARCHIES);
         System.out.println("NUM_NONTERMINALS=" + NUM_NONTERMINALS);
         System.out.println("NUM_TERMINALS=" + NUM_TERMINALS);
         System.out.println("NUM_INTERSTITIAL_TERMINALS=" + NUM_INTERSTITIAL_TERMINALS);
         System.out.println("MIN_PRODUCTION_RHS_LENGTH=" + MIN_PRODUCTION_RHS_LENGTH);
         System.out.println("MAX_PRODUCTION_RHS_LENGTH=" + MAX_PRODUCTION_RHS_LENGTH);
         System.out.println("TERMINAL_PRODUCTION_PROBABILITY=" + TERMINAL_PRODUCTION_PROBABILITY);
         System.out.println("NUM_DIMENSIONS=" + NUM_DIMENSIONS);
         System.out.println("NUM_FEATURES=" + NUM_FEATURES);
         System.out.println("MAX_INTERSTITIAL_TERMINAL_SEQUENCE=" + MAX_INTERSTITIAL_TERMINAL_SEQUENCE);
         System.out.println("CAUSATIONS_GRAPH_FILENAME=" + CAUSATIONS_GRAPH_FILENAME + ", TREE_FORMAT=" + TREE_FORMAT);
         System.out.println("NUM_CAUSATION_PATHS=" + NUM_CAUSATION_PATHS);
         System.out.println("MAX_CONTEXT_FEATURE_TIER=" + MAX_CONTEXT_FEATURE_TIER);
         System.out.println("FEATURE_VALUE_DURATION_TYPE=" + FEATURE_VALUE_DURATION_TYPE);
         System.out.println("NN_DATASET_TRAIN_FRACTION=" + NN_DATASET_TRAIN_FRACTION);
         System.out.println("NN_NEURONS=" + NN_NEURONS);
         System.out.println("NN_EPOCHS=" + NN_EPOCHS);
         System.out.println("RNN_DATASET_TRAIN_FRACTION=" + RNN_DATASET_TRAIN_FRACTION);
         System.out.println("RNN_NEURONS=" + RNN_NEURONS);
         System.out.println("RNN_EPOCHS=" + RNN_EPOCHS);
         System.out.println("RANDOM_SEED=" + RANDOM_SEED);
         System.out.println("MANDALA_FILENAME=" + MANDALA_FILENAME);
         printCausations();
      }

      // Export causations graph.
      if (gotExportCausationsGraph)
      {
         exportCausationsGraph(CAUSATIONS_GRAPH_FILENAME, TREE_FORMAT);
      }

      // Generate causation paths.
      randomizer = new Random(RANDOM_SEED);
      generateCausationPaths();

      // Analyze causations.
      analyzeCausations();

      // Export causation datasets.
      exportNNdataset(NN_DATASET_FILENAME, NN_DATASET_TRAIN_FRACTION, RANDOM_SEED);
      exportRNNdataset(RNN_DATASET_FILENAME, RNN_DATASET_TRAIN_FRACTION, RANDOM_SEED);

      // Learn causations.
      learnCausationsNN(NN_DATASET_FILENAME);
      learnCausationsRNN(RNN_DATASET_FILENAME);

      // Save?
      if (gotSave)
      {
         try
         {
            System.setProperty("line.separator", "\n");
            FileWriter  fileWriter = new FileWriter(MANDALA_FILENAME);
            PrintWriter writer     = new PrintWriter(fileWriter);

            // Save parameters.
            Utility.saveInt(writer, NUM_CAUSATION_HIERARCHIES, "NUM_CAUSATION_HIERARCHIES");
            Utility.saveInt(writer, NUM_NONTERMINALS, "NUM_NONTERMINALS");
            Utility.saveInt(writer, NUM_TERMINALS, "NUM_TERMINALS");
            Utility.saveInt(writer, NUM_INTERSTITIAL_TERMINALS, "NUM_INTERSTITIAL_TERMINALS");
            Utility.saveInt(writer, MIN_PRODUCTION_RHS_LENGTH, "MIN_PRODUCTION_RHS_LENGTH");
            Utility.saveInt(writer, MAX_PRODUCTION_RHS_LENGTH, "MAX_PRODUCTION_RHS_LENGTH");
            Utility.saveFloat(writer, TERMINAL_PRODUCTION_PROBABILITY, "TERMINAL_PRODUCTION_PROBABILITY");
            Utility.saveInt(writer, NUM_DIMENSIONS, "NUM_DIMENSIONS");
            Utility.saveInt(writer, NUM_FEATURES, "NUM_FEATURES");

            // Save causations.
            for (int i = 0; i < NUM_CAUSATION_HIERARCHIES; i++)
            {
               ArrayList<Causation> causations = causationHierarchies.get(i);
               if (NUM_NONTERMINALS > 0)
               {
                  Utility.saveInt(writer, causations.size(), "number of nonterminals, hierarchy=" + i);
                  for (int j = 0, k = causations.size(); j < k; j++)
                  {
                     NonterminalCausation nonterminalCausation = (NonterminalCausation)causations.get(j);
                     Utility.saveInt(writer, nonterminalCausation.id, "nonterminalCausation id");
                     for (int p = 0, q = nonterminalCausation.children.size(); p < q; p++)
                     {
                        Causation child = nonterminalCausation.children.get(p);
                        if (p == 0)
                        {
                           if (child instanceof NonterminalCausation)
                           {
                              Utility.saveInt(writer, 0, "nonterminal children type");
                           }
                           else
                           {
                              Utility.saveInt(writer, 1, "terminal children type");
                           }
                           Utility.saveInt(writer, nonterminalCausation.children.size(), "children size");
                        }
                        Utility.saveInt(writer, child.id, "child id");
                     }
                  }
               }
            }

            // Save causation paths.
            Utility.saveInt(writer, NUM_CAUSATION_PATHS, "NUM_CAUSATION_PATHS");
            for (CausationPath path : causationPaths)
            {
               Utility.saveInt(writer, path.hierarchy, "hierarchy");
               Utility.saveInt(writer, path.id, "id");
            }

            writer.close();
         }
         catch (Exception e)
         {
            System.err.println("Error saving to file " + MANDALA_FILENAME + ":" + e.getMessage());
            System.exit(1);
         }
      }

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
         System.setProperty("line.separator", "\n");
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
         String            features = "(";
         for (int i = 0, j = terminal.features.size(); i < j; i++)
         {
            features += terminal.features.get(i);
            if (i < j - 1)
            {
               features += ",";
            }
         }
         features += ")";
         vertices.add("h" + hierarchy + "_t" + pathPrefix + vertex.id + " [label=\"" + vertex.id + features + "\", shape=square];");
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
   public static void generateCausationPaths()
   {
      if (causationHierarchies.size() == 0)
      {
         causationPaths = new ArrayList<CausationPath> ();
         return;
      }
      if (causationPaths == null)
      {
         causationPaths = new ArrayList<CausationPath> ();
         for (int i = 0; i < NUM_CAUSATION_PATHS; i++)
         {
            int hierarchy = randomizer.nextInt(causationHierarchies.size());
            ArrayList<Causation> causationHierarchy = causationHierarchies.get(hierarchy);
            Causation            root = causationHierarchy.get(randomizer.nextInt(causationHierarchy.size()));
            causationPaths.add(new CausationPath(hierarchy, root.id));
         }
      }
      for (int i = 0; i < NUM_CAUSATION_PATHS; i++)
      {
         CausationPath        path               = causationPaths.get(i);
         int                  hierarchy          = path.hierarchy;
         ArrayList<Causation> causationHierarchy = causationHierarchies.get(hierarchy);
         Causation            root               = causationHierarchy.get(0);
         for (int j = 1, k = causationHierarchy.size(); j < k && root.id != path.id; j++)
         {
            root = causationHierarchy.get(j);
         }
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
         for (int i = 0; i < NUM_CAUSATION_PATHS; i++)
         {
            CausationPath path = causationPaths.get(i);
            System.out.println("path=" + i + ", hierarchy=" + path.hierarchy + ", id=" + path.id);
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


   // Analyze causations.
   public static void analyzeCausations()
   {
      if (VERBOSE)
      {
         System.out.println("analyze causations");
      }
      if ((MIN_PRODUCTION_RHS_LENGTH != 2) || (MAX_PRODUCTION_RHS_LENGTH != 2))
      {
         System.err.println("analysis requires cause/effect structure");
         return;
      }
      featureValueDurations = new ArrayList<Integer>();
      ArrayList < ArrayList < Integer >> hierarchyTiers = new ArrayList < ArrayList < Integer >> ();
      int maxTier = 0;
      for (int i = 0, j = causationHierarchies.size(); i < j; i++)
      {
         ArrayList<Integer> tiers = new ArrayList<Integer>();
         hierarchyTiers.add(tiers);
         ArrayList<Causation> causations = causationHierarchies.get(i);
         for (Causation causation : causations)
         {
            int tier = getTier(causation);
            tiers.add(tier);
            if (tier > maxTier)
            {
               maxTier = tier;
            }
         }
      }
      if (VERBOSE)
      {
         System.out.println("maximum tier=" + maxTier);
      }
      int[] durationAccums = new int[maxTier + 1];
      int[] durationCounts = new int[maxTier + 1];
      durationAccums[0]    = 1;
      durationCounts[0]    = 1;
      for (int i = 1; i <= maxTier; i++)
      {
         durationAccums[i] = 0;
         durationCounts[i] = 0;
      }
      for (ArrayList<Causation> causations : causationHierarchies)
      {
         for (Causation causation : causations)
         {
            if (causation instanceof TerminalCausation)
            {
               if (VERBOSE)
               {
                  ((TerminalCausation)causation).print();
                  System.out.println("terminal span=1");
               }
            }
            else
            {
               if (VERBOSE)
               {
                  ((NonterminalCausation)causation).print();
               }
               int root_min      = spanCausation(causation, 0);
               int root_expected = spanCausation(causation, 1);
               int root_max      = spanCausation(causation, 2);
               int tier          = getTier(causation);
               if (FEATURE_VALUE_DURATION_TYPE.equals("minimum"))
               {
                  durationAccums[tier] += root_min;
               }
               else if (FEATURE_VALUE_DURATION_TYPE.equals("expected"))
               {
                  durationAccums[tier] += root_expected;
               }
               else
               {
                  durationAccums[tier] += root_max;
               }
               durationCounts[tier]++;
               NonterminalCausation root  = (NonterminalCausation)causation;
               Causation            cause = root.children.get(0);
               int       cause_min        = spanCausation(cause, 0);
               int       cause_expected   = spanCausation(cause, 1);
               int       cause_max        = spanCausation(cause, 2);
               Causation effect           = root.children.get(1);
               int       effect_min       = spanCausation(effect, 0);
               int       effect_expected  = spanCausation(effect, 1);
               int       effect_max       = spanCausation(effect, 2);
               if (VERBOSE)
               {
                  System.out.println("min span=" + root_min + ", expected span=" + root_expected + ", max span=" + root_max);
                  System.out.println("cause min span=" + cause_min + ", expected span=" + cause_expected + ", max span=" + cause_max);
                  System.out.println("effect min span=" + effect_min + ", expected span=" + effect_expected + ", max span=" + effect_max);
               }
            }
         }
      }
      if (VERBOSE)
      {
         System.out.println("feature value durations:");
      }
      for (int i = 0; i <= maxTier; i++)
      {
         int duration = (int)((float)durationAccums[i] / (float)durationCounts[i]);
         featureValueDurations.add(duration);
         if (VERBOSE)
         {
            System.out.println("tier=" + i + ", duration=" + duration);
         }
      }
   }


   // Get causation tier.
   public static int getTier(Causation causation)
   {
      if (causation instanceof TerminalCausation)
      {
         return(0);
      }
      else
      {
         NonterminalCausation nonterminalCausation = (NonterminalCausation)causation;
         int cause_tier  = getTier(nonterminalCausation.children.get(0)) + 1;
         int effect_tier = getTier(nonterminalCausation.children.get(1)) + 1;
         if (cause_tier > effect_tier)
         {
            return(cause_tier);
         }
         else
         {
            return(effect_tier);
         }
      }
   }


   // Measure traversal span.
   public static int spanCausation(Causation causation, int type)
   {
      int numTerminals = countTerminals(causation);

      switch (type)
      {
      case 0:      // minimum.
         return(numTerminals);

      case 1:      // expected.
         int count = 0;
         for (int i = 0; i < 100; i++)
         {
            for (int j = 0, k = numTerminals / 2; j < k; j++)
            {
               for (int n = 0; n < MAX_INTERSTITIAL_TERMINAL_SEQUENCE; n++)
               {
                  if (NUM_INTERSTITIAL_TERMINALS == 0)
                  {
                     if (randomizer.nextInt(NUM_TERMINALS) == 0)
                     {
                        break;
                     }
                  }
                  else
                  {
                     if (randomizer.nextInt(NUM_INTERSTITIAL_TERMINALS + 1) == 0)
                     {
                        break;
                     }
                  }
                  count++;
               }
            }
         }
         count /= 100;
         return(numTerminals + count);

      case 2:       // maximum.
         return(numTerminals + ((numTerminals / 2) * MAX_INTERSTITIAL_TERMINAL_SEQUENCE));
      }
      return(0);
   }


   // Count terminals.
   public static int countTerminals(Causation causation)
   {
      int count = 0;

      if (causation instanceof TerminalCausation)
      {
         count = 1;
      }
      else
      {
         NonterminalCausation nonterminalCausation = (NonterminalCausation)causation;
         count  = countTerminals(nonterminalCausation.children.get(0));
         count += countTerminals(nonterminalCausation.children.get(1));
      }
      return(count);
   }


   // Export NN dataset.
   public static void exportNNdataset(String filename, float trainFraction, int randomSeed)
   {
      if (VERBOSE)
      {
         System.out.println("export NN dataset");
      }
      Random randomID = new Random(randomSeed);
      int    numPaths = causationPaths.size();
      int    maxTiers = 0;
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

      if (VERBOSE)
      {
         System.out.println("training dataset:");
      }
      ArrayList < ArrayList < Float >> X_train = new ArrayList < ArrayList < Float >> ();
      ArrayList < ArrayList < Float >> y_train = new ArrayList < ArrayList < Float >> ();
      ArrayList<Integer> y_train_path_begin = new ArrayList<Integer>();     
      int                trainCount           = 0;
      int                numTrain           = (int)((float)numPaths * NN_DATASET_TRAIN_FRACTION);
      for (int i = 0; i < numTrain; i++)
      {
         CausationPath path = causationPaths.get(i);
         if (VERBOSE)
         {
            path.print();
            System.out.println("data:");
         }
         contextTiers = new ArrayList<ContextFeatures> ();
         for (int j = 0, k = maxTiers - 1; j < k; j++)
         {
            contextTiers.add(null);
         }
         int step = 0;
         for (int j = 0, p = path.steps.size() - 1; j < p; j++)
         {
            ArrayList<CausationTier> xstep              = path.steps.get(j);
            ArrayList<CausationTier> ystep              = path.steps.get(j + 1);
            Causation                xcausation         = xstep.get(0).causation;
            TerminalCausation        xterminalCausation = (TerminalCausation)xcausation;
            Causation                ycausation         = ystep.get(0).causation;
            TerminalCausation        yterminalCausation = (TerminalCausation)ycausation;
            if (j == 0)
            {
               y_train_path_begin.add(trainCount);
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
            ArrayList < ArrayList < Float >> X_contexts = new ArrayList < ArrayList < Float >> ();
            for (int k = 0; k < maxTiers; k++)
            {
               if (k == 0)
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     if (xterminalCausation.features.contains(q))
                     {
                        X_train_step.add(1.0f);
                     }
                     else
                     {
                        X_train_step.add(0.0f);
                     }
                  }
               }
               else
               {
                  if (VERBOSE)
                  {
                     System.out.println("get tier context for X, terminal id=" + xterminalCausation.id + ", tier=" + (k - 1) + ", step=" + step);
                  }
                  ArrayList<Float> X_context = getTierContext(k - 1);
                  X_contexts.add(X_context);
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     X_train_step.add(X_context.get(q));
                  }
               }
            }
            updateContexts(xterminalCausation);
            for (int k = 0; k < maxTiers; k++)
            {
               if (k == 0)
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     if (yterminalCausation.features.contains(q))
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
                  if (VERBOSE)
                  {
                     System.out.println("get tier context for y, terminal id=" + xterminalCausation.id + ", tier=" + (k - 1) + ", step=" + step);
                  }
                  ArrayList<Float> y_context = getTierContext(k - 1);
                  ArrayList<Float> X_context = X_contexts.get(k - 1);
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     y_train_step.add(y_context.get(q) - X_context.get(q));
                  }
               }
            }
            X_train.add(X_train_step);
            y_train.add(y_train_step);
            step++;
            trainCount++;
         }
         if (VERBOSE)
         {
            System.out.println("training path length=" + step);
         }         
      }
      ArrayList<Integer> y_train_interstitial = new ArrayList<Integer>();      
      for (int i = 0; i < NUM_INTERSTITIAL_TERMINALS; i++)
      {
          ArrayList<Float> X_train_step = new ArrayList<Float>();
          ArrayList<Float> y_train_step = new ArrayList<Float>();
          TerminalCausation xterminalCausation = new TerminalCausation(NUM_CAUSATION_HIERARCHIES, i);          
          for (int k = 0; k < maxTiers; k++)
          {
             if (k == 0)
             {
                for (int q = 0; q < NUM_DIMENSIONS; q++)
                {
                   if (xterminalCausation.features.contains(q))
                   {
                      X_train_step.add(1.0f);
                   }
                   else
                   {
                      X_train_step.add(0.0f);
                   }
                }
             }
             else
             {
                for (int q = 0; q < NUM_DIMENSIONS; q++)
                {
                   X_train_step.add(0.0f);
                }
             }
          }
          for (int k = 0; k < maxTiers; k++)
          {
                for (int q = 0; q < NUM_DIMENSIONS; q++)
                {
                   y_train_step.add(0.0f);
                }
          }
          X_train.add(X_train_step);
          y_train.add(y_train_step);
          y_train_interstitial.add(trainCount);          
    	  trainCount++;
      }
      if (VERBOSE)
      {
         System.out.println("testing dataset:");
      }
      ArrayList < ArrayList < Float >> X_test = new ArrayList < ArrayList < Float >> ();
      ArrayList < ArrayList < Float >> y_test = new ArrayList < ArrayList < Float >> ();
      ArrayList<Integer> y_test_path_begin   = new ArrayList<Integer>();
      ArrayList<Integer> y_test_predictable  = new ArrayList<Integer>();
      ArrayList<Integer> y_test_interstitial = new ArrayList<Integer>();
      int testCount = 0;
      for (int i = numTrain; i < numPaths; i++)
      {
         CausationPath path = causationPaths.get(i);
         if (VERBOSE)
         {
            path.print();
            System.out.println("data:");
         }
         int     step      = 0;
         boolean pathBegin = true;
         for (int j = 0, p = path.steps.size() - 1; j < p; j++)
         {
            ArrayList<CausationTier> xstep              = path.steps.get(j);
            ArrayList<CausationTier> ystep              = path.steps.get(j + 1);
            Causation                xcausation         = xstep.get(0).causation;
            TerminalCausation        xterminalCausation = (TerminalCausation)xcausation;
            Causation                ycausation         = ystep.get(0).causation;
            TerminalCausation        yterminalCausation = (TerminalCausation)ycausation;
            if ((xstep.size() > 1) && (xstep.get(1).currentChild == 0))
            {
               if (pathBegin)
               {
                  y_test_path_begin.add(testCount);
                  pathBegin = false;
               }
               int xid, yid;
               if (NUM_INTERSTITIAL_TERMINALS == 0)
               {
                  xid = randomID.nextInt(NUM_TERMINALS);
               }
               else
               {
                  xid = randomID.nextInt(NUM_INTERSTITIAL_TERMINALS + 1);
                  if (xid == 0)
                  {
                     xid = xcausation.id;
                  }
                  else
                  {
                     xid += (NUM_TERMINALS - 1);
                  }
               }
               for (int n = 0; xid != xcausation.id && n < MAX_INTERSTITIAL_TERMINAL_SEQUENCE; n++)
               {
                  ArrayList<Float>  X_test_step      = new ArrayList<Float>();
                  ArrayList<Float>  y_test_step      = new ArrayList<Float>();
                  TerminalCausation xrandomCausation = new TerminalCausation(NUM_CAUSATION_HIERARCHIES, xid);
                  if (NUM_INTERSTITIAL_TERMINALS == 0)
                  {
                     yid = randomID.nextInt(NUM_TERMINALS);
                  }
                  else
                  {
                     yid = randomID.nextInt(NUM_INTERSTITIAL_TERMINALS + 1);
                     if (yid == 0)
                     {
                        yid = xcausation.id;
                     }
                     else
                     {
                        yid += (NUM_TERMINALS - 1);
                     }
                  }
                  TerminalCausation yrandomCausation = new TerminalCausation(NUM_CAUSATION_HIERARCHIES, yid);
                  if (VERBOSE)
                  {
                     System.out.print("X: *");
                     xrandomCausation.print();
                     System.out.print("y: ");
                     yrandomCausation.print();
                  }
                  for (int k = 0; k < maxTiers; k++)
                  {
                     if (k == 0)
                     {
                        for (int q = 0; q < NUM_DIMENSIONS; q++)
                        {
                           if (xrandomCausation.features.contains(q))
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
                           y_test_step.add(0.0f);
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
                  y_test_interstitial.add(testCount);
                  step++;
                  testCount++;
                  xid = yid;
               }
            }
            if (pathBegin)
            {
               y_test_path_begin.add(testCount);
               pathBegin = false;
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
                     if (xterminalCausation.features.contains(q))
                     {
                        X_test_step.add(1.0f);
                     }
                     else
                     {
                        X_test_step.add(0.0f);
                     }
                  }
               }
               else
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     X_test_step.add(0.0f);
                  }
               }
            }
            for (int k = 0; k < maxTiers; k++)
            {
               if (k == 0)
               {
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     if (yterminalCausation.features.contains(q))
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
                     y_test_step.add(0.0f);
                  }
               }
            }
            X_test.add(X_test_step);
            y_test.add(y_test_step);
            if (xstep.size() > 1)
            {
               CausationTier        causationTier        = xstep.get(1);
               NonterminalCausation nonterminalCausation = (NonterminalCausation)causationTier.causation;
               if (nonterminalCausation.children.size() > causationTier.currentChild + 1)
               {
                  y_test_predictable.add(testCount);
               }
            }
            step++;
            testCount++;
         }
         if (VERBOSE)
         {
            System.out.println("testing path length=" + step);
         }
      }

      try
      {
         System.setProperty("line.separator", "\n");
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
         printWriter.print("y_train_path_begin = [");
         for (int i = 0, j = y_train_path_begin.size(); i < j; i++)
         {
            printWriter.print(y_train_path_begin.get(i) + "");
            if (i < j - 1)
            {
               printWriter.print(",");
            }
         }
         printWriter.println("]");
         printWriter.print("y_train_interstitial = [");
         for (int i = 0, j = y_train_interstitial.size(); i < j; i++)
         {
            printWriter.print(y_train_interstitial.get(i) + "");
            if (i < j - 1)
            {
               printWriter.print(",");
            }
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
         printWriter.print("y_test_path_begin = [");
         for (int i = 0, j = y_test_path_begin.size(); i < j; i++)
         {
            printWriter.print(y_test_path_begin.get(i) + "");
            if (i < j - 1)
            {
               printWriter.print(",");
            }
         }
         printWriter.println("]");
         printWriter.print("y_test_predictable = [");
         for (int i = 0, j = y_test_predictable.size(); i < j; i++)
         {
            printWriter.print(y_test_predictable.get(i) + "");
            if (i < j - 1)
            {
               printWriter.print(",");
            }
         }
         printWriter.println("]");
         printWriter.print("y_test_interstitial = [");
         for (int i = 0, j = y_test_interstitial.size(); i < j; i++)
         {
            printWriter.print(y_test_interstitial.get(i) + "");
            if (i < j - 1)
            {
               printWriter.print(",");
            }
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


   // Get tier feature context.
   static ArrayList<Float> getTierContext(int tier)
   {
      ArrayList<Float> features = new ArrayList<Float>();
      for (int i = 0; i < NUM_DIMENSIONS; i++)
      {
         features.add(0.0f);
      }
      ContextFeatures contextFeatures = contextTiers.get(tier);
      if (contextFeatures != null)
      {
         if (VERBOSE)
         {
            contextFeatures.print();
         }
         for (int i : contextFeatures.features)
         {
            features.set(i, contextFeatures.value);
         }
      }
      return(features);
   }


   // Update feature contexts.
   static void updateContexts(TerminalCausation causation)
   {
      // Expire contexts.
      for (int i = 0, j = contextTiers.size(); i < j; i++)
      {
         ContextFeatures contextFeatures = contextTiers.get(i);
         if (contextFeatures != null)
         {
            if (!contextFeatures.expire())
            {
               contextTiers.set(i, null);
            }
         }
      }

      // Add contexts.
      ContextFeatures contextFeatures1 = new ContextFeatures(causation, 0);
      for (int i = 0, j = contextTiers.size(); i < j && i <= MAX_CONTEXT_FEATURE_TIER; i++)
      {
         ContextFeatures contextFeatures2 = contextTiers.get(i);
         if (contextFeatures2 != null)
         {
            contextTiers.set(i, null);
            contextFeatures1 = new ContextFeatures(contextFeatures1, contextFeatures2, i + 1);
         }
         else
         {
            contextTiers.set(i, contextFeatures1);
            break;
         }
      }
   }


   // Export RNN dataset.
   public static void exportRNNdataset(String filename, float trainFraction, int randomSeed)
   {
      if (VERBOSE)
      {
         System.out.println("export RNN dataset");
         System.out.println("training dataset:");
      }
      Random randomID = new Random(randomSeed);
      int    numPaths = causationPaths.size();
      ArrayList < ArrayList < Float >> X_train = new ArrayList < ArrayList < Float >> ();
      ArrayList < ArrayList < Float >> y_train = new ArrayList < ArrayList < Float >> ();
      int numTrain      = (int)((float)numPaths * NN_DATASET_TRAIN_FRACTION);
      int numTest       = numPaths - numTrain;
      int maxPathLength = 0;
      for (int i = 0; i < numTrain; i++)
      {
         CausationPath path = causationPaths.get(i);
         if (VERBOSE)
         {
            path.print();
            System.out.println("data:");
         }
         ArrayList<Float> X_train_path = new ArrayList<Float> ();
         ArrayList<Float> y_train_path = new ArrayList<Float> ();
         for (int j = 0, p = path.steps.size() - 1; j < p; j++)
         {
            ArrayList<CausationTier> xstep              = path.steps.get(j);
            ArrayList<CausationTier> ystep              = path.steps.get(j + 1);
            Causation                xcausation         = xstep.get(0).causation;
            TerminalCausation        xterminalCausation = (TerminalCausation)xcausation;
            Causation                ycausation         = ystep.get(0).causation;
            TerminalCausation        yterminalCausation = (TerminalCausation)ycausation;
            if (VERBOSE)
            {
               System.out.print("X: ");
               xterminalCausation.print();
               System.out.print("y: ");
               yterminalCausation.print();
            }
            for (int q = 0; q < NUM_DIMENSIONS; q++)
            {
               if (xterminalCausation.features.contains(q))
               {
                  X_train_path.add(1.0f);
               }
               else
               {
                  X_train_path.add(0.0f);
               }
            }
            for (int q = 0; q < NUM_DIMENSIONS; q++)
            {
               if (yterminalCausation.features.contains(q))
               {
                  y_train_path.add(1.0f);
               }
               else
               {
                  y_train_path.add(0.0f);
               }
            }
         }
         X_train.add(X_train_path);
         y_train.add(y_train_path);
         int pathLength = X_train_path.size() / NUM_DIMENSIONS;
         if (pathLength > maxPathLength)
         {
            maxPathLength = pathLength;
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
      ArrayList < ArrayList < Float >> X_test          = new ArrayList < ArrayList < Float >> ();
      ArrayList < ArrayList < Float >> y_test          = new ArrayList < ArrayList < Float >> ();
      ArrayList < ArrayList < Integer >> y_predictable = new ArrayList < ArrayList < Integer >> ();
      for (int i = numTrain; i < numPaths; i++)
      {
         int           sequence = 0;
         CausationPath path     = causationPaths.get(i);
         if (VERBOSE)
         {
            path.print();
            System.out.println("data:");
         }
         ArrayList<Float>   X_test_path        = new ArrayList<Float> ();
         ArrayList<Float>   y_test_path        = new ArrayList<Float> ();
         ArrayList<Integer> y_predictable_path = new ArrayList<Integer>();
         int                p = path.steps.size() - 1;
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
               int xid, yid;
               if (NUM_INTERSTITIAL_TERMINALS == 0)
               {
                  xid = randomID.nextInt(NUM_TERMINALS);
               }
               else
               {
                  xid = randomID.nextInt(NUM_INTERSTITIAL_TERMINALS + 1);
                  if (xid == 0)
                  {
                     xid = xcausation.id;
                  }
                  else
                  {
                     xid += (NUM_TERMINALS - 1);
                  }
               }
               for (int n = 0; xid != xcausation.id && n < MAX_INTERSTITIAL_TERMINAL_SEQUENCE; n++)
               {
                  TerminalCausation xrandomCausation = new TerminalCausation(NUM_CAUSATION_HIERARCHIES, xid);
                  if (NUM_INTERSTITIAL_TERMINALS == 0)
                  {
                     yid = randomID.nextInt(NUM_TERMINALS);
                  }
                  else
                  {
                     yid = randomID.nextInt(NUM_INTERSTITIAL_TERMINALS + 1);
                     if (yid == 0)
                     {
                        yid = xcausation.id;
                     }
                     else
                     {
                        yid += (NUM_TERMINALS - 1);
                     }
                  }
                  TerminalCausation yrandomCausation = new TerminalCausation(NUM_CAUSATION_HIERARCHIES, yid);
                  if (VERBOSE)
                  {
                     System.out.print("X: *");
                     xrandomCausation.print();
                     System.out.print("y: ");
                     yrandomCausation.print();
                  }
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     if (xrandomCausation.features.contains(q))
                     {
                        X_test_path.add(1.0f);
                     }
                     else
                     {
                        X_test_path.add(0.0f);
                     }
                  }
                  for (int q = 0; q < NUM_DIMENSIONS; q++)
                  {
                     y_test_path.add(0.0f);
                  }
                  sequence++;
                  xid = yid;
               }
            }
            if (VERBOSE)
            {
               System.out.print("X: ");
               xterminalCausation.print();
               System.out.print("y: ");
               yterminalCausation.print();
            }
            for (int q = 0; q < NUM_DIMENSIONS; q++)
            {
               if (xterminalCausation.features.contains(q))
               {
                  X_test_path.add(1.0f);
               }
               else
               {
                  X_test_path.add(0.0f);
               }
            }
            for (int q = 0; q < NUM_DIMENSIONS; q++)
            {
               if (yterminalCausation.features.contains(q))
               {
                  y_test_path.add(1.0f);
               }
               else
               {
                  y_test_path.add(0.0f);
               }
            }
            if (xstep.size() > 1)
            {
               CausationTier        causationTier        = xstep.get(1);
               NonterminalCausation nonterminalCausation = (NonterminalCausation)causationTier.causation;
               if (nonterminalCausation.children.size() > causationTier.currentChild + 1)
               {
                  y_predictable_path.add(sequence);
               }
            }
            sequence++;
         }
         X_test.add(X_test_path);
         y_test.add(y_test_path);
         y_predictable.add(y_predictable_path);
         int pathLength = X_test_path.size() / NUM_DIMENSIONS;
         if (pathLength > maxPathLength)
         {
            maxPathLength = pathLength;
         }
         if (VERBOSE)
         {
            System.out.println("testing path length=" + pathLength);
         }
      }

      try
      {
         System.setProperty("line.separator", "\n");
         FileWriter  fileWriter  = new FileWriter(filename);
         PrintWriter printWriter = new PrintWriter(fileWriter);
         printWriter.println("X_train_shape = [ " + numTrain + ", " + maxPathLength + ", " + NUM_DIMENSIONS + " ]");
         printWriter.println("X_train = [");
         for (int i = 0, j = X_train.size(); i < j; i++)
         {
            ArrayList<Float> X_train_path = X_train.get(i);
            for (int k = 0, p = X_train_path.size(), q = (maxPathLength * NUM_DIMENSIONS); k < q; k++)
            {
               if (k < p)
               {
                  printWriter.print(X_train_path.get(k) + "");
               }
               else
               {
                  printWriter.print("0.0");
               }
               if ((i != j - 1) || (k != q - 1))
               {
                  printWriter.print(",");
               }
            }
            printWriter.println();
         }
         printWriter.println("]");
         printWriter.println("y_train_shape = [ " + numTrain + ", " + maxPathLength + ", " + NUM_DIMENSIONS + " ]");
         printWriter.println("y_train = [");
         for (int i = 0, j = y_train.size(); i < j; i++)
         {
            ArrayList<Float> y_train_path = y_train.get(i);
            for (int k = 0, p = y_train_path.size(), q = (maxPathLength * NUM_DIMENSIONS); k < q; k++)
            {
               if (k < p)
               {
                  printWriter.print(y_train_path.get(k) + "");
               }
               else
               {
                  printWriter.print("0.0");
               }
               if ((i != j - 1) || (k != q - 1))
               {
                  printWriter.print(",");
               }
            }
            printWriter.println();
         }
         printWriter.println("]");
         printWriter.println("X_test_shape = [ " + numTest + ", " + maxPathLength + ", " + NUM_DIMENSIONS + " ]");
         printWriter.println("X_test = [");
         for (int i = 0, j = X_test.size(); i < j; i++)
         {
            ArrayList<Float> X_test_path = X_test.get(i);
            for (int k = 0, p = X_test_path.size(), q = (maxPathLength * NUM_DIMENSIONS); k < q; k++)
            {
               if (k < p)
               {
                  printWriter.print(X_test_path.get(k) + "");
               }
               else
               {
                  printWriter.print("0.0");
               }
               if ((i != j - 1) || (k != q - 1))
               {
                  printWriter.print(",");
               }
            }
            printWriter.println();
         }
         printWriter.println("]");
         printWriter.println("y_test_shape = [ " + numTest + ", " + maxPathLength + ", " + NUM_DIMENSIONS + " ]");
         printWriter.println("y_test = [");
         for (int i = 0, j = y_test.size(); i < j; i++)
         {
            ArrayList<Float> y_test_path = y_test.get(i);
            for (int k = 0, p = y_test_path.size(), q = (maxPathLength * NUM_DIMENSIONS); k < q; k++)
            {
               if (k < p)
               {
                  printWriter.print(y_test_path.get(k) + "");
               }
               else
               {
                  printWriter.print("0.0");
               }
               if ((i != j - 1) || (k != q - 1))
               {
                  printWriter.print(",");
               }
            }
            printWriter.println();
         }
         printWriter.println("]");
         printWriter.println("y_test_predictable = [");
         for (int i = 0, j = y_predictable.size(); i < j; i++)
         {
            ArrayList<Integer> y_test_predictable_path = y_predictable.get(i);
            printWriter.print("[");
            for (int k = 0, q = y_test_predictable_path.size(); k < q; k++)
            {
               printWriter.print(y_test_predictable_path.get(k) + "");
               if (k != q - 1)
               {
                  printWriter.print(",");
               }
            }
            printWriter.print("]");
            if (i < j - 1)
            {
               printWriter.print(",");
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
      commandList.add("--dimensions");
      commandList.add(NUM_DIMENSIONS + "");
      commandList.add("--features");
      commandList.add(NUM_FEATURES + "");
      commandList.add("--neurons");
      commandList.add(NN_NEURONS);
      commandList.add("--epochs");
      commandList.add(NN_EPOCHS + "");
      if (!VERBOSE)
      {
         commandList.add("--quiet");
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
   public static LearningResults learnCausationsRNN(String filename)
   {
      if (VERBOSE)
      {
         System.out.println("Learn RNN");
      }
      try
      {
         InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(RNN_FILENAME);
         if (in == null)
         {
            System.err.println("Cannot access " + RNN_FILENAME);
            System.exit(1);
         }
         File             pythonScript = new File(RNN_FILENAME);
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
         System.err.println("Cannot create " + RNN_FILENAME);
         System.exit(1);
      }
      new File(RNN_RESULTS_FILENAME).delete();
      ArrayList<String> commandList = new ArrayList<>();
      commandList.add("python");
      commandList.add(RNN_FILENAME);
      commandList.add("--features");
      commandList.add(NUM_FEATURES + "");
      commandList.add("--neurons");
      commandList.add(RNN_NEURONS);
      commandList.add("--epochs");
      commandList.add(RNN_EPOCHS + "");
      if (!VERBOSE)
      {
         commandList.add("--quiet");
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
         System.err.println("Cannot run " + RNN_FILENAME + ":" + e.getMessage());
         System.exit(1);
      }
      if (VERBOSE)
      {
         System.out.println("Results written to " + RNN_RESULTS_FILENAME);
      }

      // Fetch the results.
      LearningResults results = new LearningResults();
      try
      {
         BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(RNN_RESULTS_FILENAME)));
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
               System.err.println("Error parsing results file " + RNN_RESULTS_FILENAME);
               System.exit(1);
            }
            String train_prediction_errors = jObj.getString("train_prediction_errors");
            if ((train_prediction_errors == null) || train_prediction_errors.isEmpty())
            {
               System.err.println("Error parsing results file " + RNN_RESULTS_FILENAME);
               System.exit(1);
            }
            try
            {
               results.train_prediction_errors = Integer.parseInt(train_prediction_errors);
            }
            catch (NumberFormatException e)
            {
               System.err.println("Error parsing results file " + RNN_RESULTS_FILENAME);
               System.exit(1);
            }
            String train_total_predictions = jObj.getString("train_total_predictions");
            if ((train_total_predictions == null) || train_total_predictions.isEmpty())
            {
               System.err.println("Error parsing results file " + RNN_RESULTS_FILENAME);
               System.exit(1);
            }
            try
            {
               results.train_total_predictions = Integer.parseInt(train_total_predictions);
            }
            catch (NumberFormatException e)
            {
               System.err.println("Error parsing results file " + RNN_RESULTS_FILENAME);
               System.exit(1);
            }
            String train_error_pct = jObj.getString("train_error_pct");
            if ((train_error_pct == null) || train_error_pct.isEmpty())
            {
               System.err.println("Error parsing results file " + RNN_RESULTS_FILENAME);
               System.exit(1);
            }
            try
            {
               results.train_error_pct = Float.parseFloat(train_error_pct);
            }
            catch (NumberFormatException e)
            {
               System.err.println("Error parsing results file " + RNN_RESULTS_FILENAME);
               System.exit(1);
            }
            String test_prediction_errors = jObj.getString("test_prediction_errors");
            if ((test_prediction_errors == null) || test_prediction_errors.isEmpty())
            {
               System.err.println("Error parsing results file " + RNN_RESULTS_FILENAME);
               System.exit(1);
            }
            try
            {
               results.test_prediction_errors = Integer.parseInt(test_prediction_errors);
            }
            catch (NumberFormatException e)
            {
               System.err.println("Error parsing results file " + RNN_RESULTS_FILENAME);
               System.exit(1);
            }
            String test_total_predictions = jObj.getString("test_total_predictions");
            if ((test_total_predictions == null) || test_total_predictions.isEmpty())
            {
               System.err.println("Error parsing results file " + RNN_RESULTS_FILENAME);
               System.exit(1);
            }
            try
            {
               results.test_total_predictions = Integer.parseInt(test_total_predictions);
            }
            catch (NumberFormatException e)
            {
               System.err.println("Error parsing results file " + RNN_RESULTS_FILENAME);
               System.exit(1);
            }
            String test_error_pct = jObj.getString("test_error_pct");
            if ((test_error_pct == null) || test_error_pct.isEmpty())
            {
               System.err.println("Error parsing results file " + RNN_RESULTS_FILENAME);
               System.exit(1);
            }
            try
            {
               results.test_error_pct = Float.parseFloat(test_error_pct);
            }
            catch (NumberFormatException e)
            {
               System.err.println("Error parsing results file " + RNN_RESULTS_FILENAME);
               System.exit(1);
            }
         }
         else
         {
            System.err.println("Cannot read results file " + RNN_RESULTS_FILENAME);
            System.exit(1);
         }
         br.close();
      }
      catch (Exception e)
      {
         System.err.println("Cannot read results file " + RNN_RESULTS_FILENAME + ":" + e.getMessage());
         System.exit(1);
      }
      return(results);
   }
}
