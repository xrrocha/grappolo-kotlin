package grappolo

import kotlin.math.max
import kotlin.math.min

interface ClusterEvaluator {
    fun evaluate(clusters: List<Set<Index>>, similarityMatrix: SimilarityMatrix): Evaluation
}

object SimpleClusterEvaluator : ClusterEvaluator {

    private val logger = getLogger(this)

    // TODO Compute evaluation based on similarity, not distance
    override fun evaluate(clusters: List<Set<Index>>, similarityMatrix: SimilarityMatrix): Evaluation {

        val maxIntraClusterDistance =
            clusters
                .flatMap { clusterSet ->
                    val cluster = clusterSet.toList()
                    cluster.indices.flatMap { i ->
                        (i + 1 until cluster.size)
                            .map { j -> 1.0 - similarityMatrix[cluster[i]][cluster[j]] }
                            .filterNot { it == 1.0 }
                    }
                }
                .max()!!

        fun clusterDistance(cluster1: Set<Index>, cluster2: Set<Index>): Similarity =
            cluster1
                .flatMap { i ->
                    cluster2.map { j ->
                        1.0 - similarityMatrix[i][j]
                    }
                }
                .average()

        val minInterClusterDistance =
            clusters.indices.flatMap { i ->
                (i + 1 until clusters.size).map { j ->
                    clusterDistance(clusters[i], clusters[j])
                }
            }
                .min() ?: 0.0

        val minValue = min(minInterClusterDistance, maxIntraClusterDistance)
        val maxValue = max(minInterClusterDistance, maxIntraClusterDistance)
        val evaluation = 1.0 - minValue / maxValue
        // logger.debug("minInterClusterDistance: $minInterClusterDistance, maxIntraClusterDistance: $maxIntraClusterDistance, evaluation: $evaluation")

        return evaluation
    }
}
