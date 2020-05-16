package grappolo

import kotlin.math.max
import kotlin.math.min

interface ClusterEvaluator {
    fun evaluate(clusters: List<Set<Index>>, similarityMatrix: SimilarityMatrix): Evaluation
}

object SimpleClusterEvaluator : ClusterEvaluator {

    private val logger = getLogger(this)

    override fun evaluate(clusters: List<Set<Index>>, similarityMatrix: SimilarityMatrix): Evaluation {

        val minIntraClusterSimilarity =
            clusters
                .flatMap { clusterSet ->
                    val cluster = clusterSet.toList()
                    cluster.indices.flatMap { i ->
                        (i + 1 until cluster.size)
                            .map { j -> similarityMatrix[cluster[i]][cluster[j]] }
                            .filterNot { it == 0.0 } // TODO Address lossy minSimilarity :-(
                    }
                }
                .min() ?: 0.0

        fun clusterSimilarity(cluster1: Set<Index>, cluster2: Set<Index>): Similarity =
            cluster1
                .flatMap { i ->
                    cluster2.map { j ->
                        similarityMatrix[i][j]
                    }
                }
                .average()

        val maxInterClusterSimilarity =
            clusters.indices.flatMap { i ->
                (i + 1 until clusters.size).map { j ->
                    clusterSimilarity(clusters[i], clusters[j])
                }
            }
                .max() ?: 1.0

        val minValue = min(maxInterClusterSimilarity, minIntraClusterSimilarity)
        val maxValue = max(maxInterClusterSimilarity, minIntraClusterSimilarity)
        val evaluation = 1.0 - minValue / maxValue
        // logger.debug("minInterClusterDistance: $minInterClusterDistance, maxIntraClusterDistance: $maxIntraClusterDistance, evaluation: $evaluation")

        return evaluation
    }
}
