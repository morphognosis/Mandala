# Mandala: Contextual learning for multitasking and causation labeling.

This project is an effort to combine the Morphognosis and Mona neural network models into a 
comprehensive model for learning and behavior called Mandala. Mona features a contextual 
causation learning with goal-directed motivation. Morphognosis features contextual multilayer 
perceptron (MLP) learning. Mandala achieves this by externally accumulating tiers of temporal 
information that are fed into an MLP at each time step. Natural environments abound in event 
streams that require multitasking. Mandala affords multitasking as it is robust in the presence 
of intervening events representing overlaid causation streams, a capability that conventional 
recurrent artificial neural networks (RNNs) struggle with. In addition, externally accumulating 
temporal information discretely labels hierarchical cause-and-effect relationships that can 
be used for augmented processing. In the case of Mona, channeling motivation through the network 
for the purpose of goal-seeking requires this feature.

Mona:
A pair of cooperating nest-building and foraging birds. 
See: http://tom.portegys.com/research.html#nestingbirds

Morphognosis:
Honey bees forage for flower nectar cooperatively.
See: http://tom.portegys.com/research.html#honey_bees

Build with Eclipse project or build.sh/build.bat and export to bin/mandala.jar to run with scripts in the work directory.
Requires java, python, and the keras machine learning package.

```
Usage:
  New run:
    java mandala.Mandala
      [-numCausationHierarchies <quantity> (default=1)]
      [-numNonterminals <quantity> (default=10)]
      [-numTerminals <quantity> (default=20)]
      [-numInterstitialTerminals <quantity> (default=5, if 0 resort to non-interstitial terminals)]
      [-minProductionRightHandSideLength <quantity> (default=2)]
      [-maxProductionRightHandSideLength <quantity> (default=2)]
      [-terminalProductionProbability <probability> (default=0.25)]
      [-numDimensions <quantity> (default=64)]
      [-numFeatures <quantity> (default=3)]
      [-maxInterstitialTerminalSequence <length> (default=10)]
      [-exportCausationsGraph [<file name> (Graphviz dot format, default=mandala_causations.dot)]
          [-treeFormat "true" | "false" (default=true)]]
      [-numCausationPaths <quantity per hierarchy> (default=20)]
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
      [-numCausationPaths <quantity per hierarchy> (default=20)]
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

