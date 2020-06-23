import grappolo.*

ClusteringConfiguration(
    minSimilarity = 0.0,
    source = StringFileSource("data/surnames/data.tsv"),
    similarityMetric = DamerauSimilarityMetric(),
    pairGenerator = CartesianPairGenerator(4072),
    clusterer = GrappoloClusterer,
    clusterEvaluator = SimpleClusterEvaluator
)

