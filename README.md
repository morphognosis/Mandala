# Mandala: Contextual learning for interruption tolerance and multitasking.

Natural environments abound in event streams that require interruption tolerance and multitasking. 
This project introduces Mandala, a neural network that improves multitasking in the form of dealing 
with intervening events from overlaid causation chains, a capability that a conventional recurrent 
artificial neural network (RNN) struggles with, as evidenced by the results. This capability is also 
tolerant of interrupting events. Mandala achieves this by accumulating contextual tiers of temporal 
states that are fed into a multilayer perceptron (MLP) at each time step. Mandala is also an effort 
to combine the Morphognosis and Mona neural network models into a comprehensive model for learning 
and behavior. Mona features a contextual causation learning with goal-directed motivation. 
Morphognosis features contextual MLP learning. In addition, accumulating temporal information 
discretely labels hierarchical cause-and-effect relationships that can be used for augmented 
processing. In the case of Mona, channeling motivation through the network for the purpose of 
goal-seeking requires this feature.

Mona:
A pair of cooperating nest-building and foraging birds. 
See: http://tom.portegys.com/research.html#nestingbirds

Morphognosis:
Honey bees forage for flower nectar cooperatively.
See: http://tom.portegys.com/research.html#honey_bees

Build with Eclipse project or build.sh/build.bat and export to bin/mandala.jar to run with scripts in the work directory.
Test with mandala_test.sh which creates mandala_test_results.csv

Requires java, python, and the keras machine learning package.

```
Usage:
  New run:
    java mandala.Mandala
      [-numCausationHierarchies <quantity> (default=1)]
      [-numNonterminals <quantity> (default=10)]
      [-numTerminals <quantity> (default=20)]
      [-numInterstitialTerminals <quantity> (default=5, if 0 resort to non-interstitial terminals)]
      [-terminalProductionProbability <probability> (default=0.25)]
      [-numDimensions <quantity> (default=64)]
      [-numFeatures <quantity> (default=3)]
      [-maxInterstitialTerminalSequence <length> (default=10)]
      [-exportCausationsGraph [<file name> (Graphviz dot format, default=mandala_causations.dot)]
          [-treeFormat "true" | "false" (default=true)]]
      [-numCausationPaths <quantity per hierarchy> (default=2)]
      [-maxContextTier <value> (default=5)]
      [-contextTierValueDurationType "minimum" | "expected" | "maximum" (default=maximum)]
      [-NNdatasetTrainFraction <fraction> (default=0.5)]
      [-NNneurons<number of neurons> (comma-separated for additional layers) (default=128,128,128)]
      [-NNepochs <number of epochs> (default=500)]
      [-RNNdatasetTrainFraction <fraction> (default=0.5)]
      [-RNNneurons <number of neurons> (comma-separated for additional layers) (default=128)]
      [-RNNepochs <number of epochs> (default=500)]
      [-randomSeed <seed> (default=45)]
      [-quiet]
      [-save [<file name> (default=mandala.dat)]
  Load:
    java mandala.Mandala
      -load [<file name> (default=mandala.dat)]
      [-maxInterstitialTerminalSequence <length> (default=10)]
      [-exportCausationsGraph [<file name> (Graphviz dot format, default=mandala_causations.dot)]
          [-treeFormat "true" | "false" (default=true)]]
      [-numCausationPaths <quantity per hierarchy> (default=2)]
      [-maxContextTier <value> (default=5)]
      [-contextTierValueDurationType "minimum" | "expected" | "maximum" (default=maximum)]
      [-NNdatasetTrainFraction <fraction> (default=0.5)]
      [-NNneurons<number of neurons> (comma-separated for additional layers) (default=128,128,128)]
      [-NNepochs <number of epochs> (default=500)]
      [-RNNdatasetTrainFraction <fraction> (default=0.5)]
      [-RNNneurons <number of neurons> (comma-separated for additional layers) (default=128)]
      [-RNNepochs <number of epochs> (default=500)]
      [-randomSeed <seed> (default=45)]
      [-quiet]
  Help:
    java mandala.Mandala -help
Exit codes:
  0=success
  1=error
```
