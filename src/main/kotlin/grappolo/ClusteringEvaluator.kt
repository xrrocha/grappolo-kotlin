package grappolo

interface ClusteringEvaluator {
    fun evaluateClustering(clusters: Iterable<Cluster>, matrix: SimilarityMatrix): Double // Larger is better
}

object IntraSimilarityClusteringEvaluator : ClusteringEvaluator {

    override fun evaluateClustering(clusters: Iterable<Cluster>, matrix: SimilarityMatrix): Double =
            clusters.map(Cluster::intraSimilarity).average()
}
