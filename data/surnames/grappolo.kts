import grappolo.*

val fileName = "data/surnames/data.tsv"
val lines = java.io.File(fileName).readLines()

ClusteringConfiguration(
        minSimilarity = 0.0,
        source = ListSource(lines),
        similarityMetric = DamerauSimilarityMetric,
        pairGenerator = CartesianPairGenerator(lines.size),
        clusterer = GrappoloClusterer,
        clusterEvaluator = SimpleClusterEvaluator
)

