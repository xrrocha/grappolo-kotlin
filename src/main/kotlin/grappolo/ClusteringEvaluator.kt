package grappolo

interface ClusteringEvaluator {
    fun evaluateClustering(clusters: Iterable<Cluster>, matrix: SimilarityMatrix, minSimilarity: Double): Double // Larger is better
}

object IntraSimilarityClusteringEvaluator : ClusteringEvaluator, Named {

    override val name = "intraSimilarity"

    override fun evaluateClustering(clusters: Iterable<Cluster>, matrix: SimilarityMatrix, minSimilarity: Double): Double =
            minSimilarity * clusters.map(Cluster::intraSimilarity).average()
}
