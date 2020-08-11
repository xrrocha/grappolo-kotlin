package grappolo

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentSet

// 4072-0.600-cartesian-damerau-exhaustive-greedy-intrasim
class ClusteringConfiguration(val elementCount: Int,
                              val similarityLowThreshold: Double,
                              val indexPairGenerator: IndexPairGenerator,
                              val similarityMetric: SimilarityMetric,
                              val clusterExtractor: ClusterExtractor,
                              val clusterComparator: ClusterComparator,
                              val clusteringEvaluator: ClusteringEvaluator) {

    fun signature(): String =
            listOf(
                    elementCount, similarityLowThreshold,
                    indexPairGenerator, similarityMetric,
                    clusterExtractor, clusterComparator, clusteringEvaluator
            )
                    .joinToString("-") {
                        when(it) {
                            is String -> it
                            is Named -> it.name
                            is Number -> it.toString()
                            else -> it::class.java.simpleName
                        }
                    }
}

data class ClusteringResult(val minSimilarity: Double, val evaluation: Double, val clusters: List<Cluster>)

interface ClusteringListener {
    fun beforeClustering(configuration: ClusteringConfiguration) {}
    fun onMatrixCreated(matrix: SimilarityMatrix, matrixCreationTime: Long) {}
    fun onEachSimilarity(minSimilarity: Double, index: Int) {}
    fun onEachClusterCreated(cluster: Cluster) {}
    fun onEachClusteringResult(result: ClusteringResult, evaluationTime: Long) {}
    fun afterClustering(result: ClusteringResult, clusteringTime: Long) {}
}

object Grappolo {

    fun cluster(configuration: ClusteringConfiguration, listener: ClusteringListener? = null): ClusteringResult {

        listener?.beforeClustering(configuration)

        val matrix = buildSimilarityMatrix(configuration, listener)

        if (matrix.size == 1) {
            val singletonResult = ClusteringResult(0.0, 0.0, listOf(Cluster(setOf(0), matrix)))
            listener?.afterClustering(singletonResult, 0L)
            return singletonResult
        }

        val (bestResult, clusteringTime) = time {
            matrix.distinctSimilarities().sorted().withIndex()
                    .fold(ClusteringResult(0.0, 0.0, emptyList())) { bestResultSoFar, (index, minSimilarity) ->

                        listener?.onEachSimilarity(minSimilarity, index)

                        val (clusters, _) =
                                generateSequence(Pair(persistentListOf<Cluster>(), (0 until matrix.size).toPersistentSet())) { accum ->

                                    val (_, remaining) = accum

                                    remaining
                                            .asSequence()
                                            .map { elementIndex ->
                                                configuration.clusterExtractor.extractCluster(elementIndex, minSimilarity, matrix, remaining)
                                            }
                                            .distinct()
                                            .map { Cluster(it, matrix) }
                                            .sortedWith(configuration.clusterComparator)
                                            .onEach { listener?.onEachClusterCreated(it) }
                                            .fold(accum) { accumSoFar, cluster ->
                                                val (clustersSoFar, unclusteredSoFar) = accumSoFar
                                                if (cluster.elements.map { it.index }.all(unclusteredSoFar::contains)) {
                                                    Pair(clustersSoFar.add(cluster), unclusteredSoFar.removeAll(cluster.elements.map { it.index }))
                                                } else {
                                                    accumSoFar
                                                }
                                            }
                                }
                                        .dropWhile { (_, unclustered) -> unclustered.isNotEmpty() }
                                        .first()

                        val clusteredCount = clusters.map { it.elements.size }.sum()
                        require(clusteredCount == matrix.size) {
                            "Cluster element count mismatch; expected ${matrix.size}, got $clusteredCount (from ${clusters.size})"
                        }

                        val (evaluation, evaluationTime) = time {
                            configuration.clusteringEvaluator.evaluateClustering(clusters, matrix, minSimilarity)
                        }
                        val currentResult = ClusteringResult(minSimilarity, evaluation, clusters)
                        listener?.onEachClusteringResult(currentResult, evaluationTime)

                        if (evaluation > bestResultSoFar.evaluation) {
                            currentResult
                        } else {
                            bestResultSoFar
                        }
                    }
        }

        listener?.afterClustering(bestResult, clusteringTime)
        return bestResult
    }

    private fun buildSimilarityMatrix(configuration: ClusteringConfiguration, listener: ClusteringListener? = null): SimilarityMatrix {

        require(configuration.elementCount > 0) {
            "Element count must be positive, not ${configuration.elementCount}"
        }

        require(configuration.similarityLowThreshold >= 0 && configuration.similarityLowThreshold < 1) {
            "Similarity low threshold must be between 0 (include) and 1 (exclusive; got ${configuration.similarityLowThreshold}"
        }

        val (matrix, matrixCreationTime) = time {
            SimilarityMatrix(configuration.elementCount).also { matrix ->
                for ((i, j) in configuration.indexPairGenerator.pairs()) {

                    require(i in 0 until configuration.elementCount) { "Invalid first index: $i" }
                    require(j in 0 until configuration.elementCount) { "Invalid second index: $j" }

                    val similarity = if (i == j) 1.0 else configuration.similarityMetric.measureSimilarity(i, j)
                    require(similarity >= 0 && similarity < 1.0) {
                        "Similarity must be between 0 (inclusive) and 1 (exclusive); got $similarity"
                    }

                    if (similarity >= configuration.similarityLowThreshold) {
                        matrix.addSimilarity(i, j, similarity)
                    }
                }
            }
        }

        listener?.onMatrixCreated(matrix, matrixCreationTime)
        return matrix
    }
}


