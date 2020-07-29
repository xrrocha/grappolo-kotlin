package grappolo

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentSet

class ClusteringConfiguration(val elementCount: Int,
                              val similarityLowThreshold: Double,
                              val indexPairGenerator: IndexPairGenerator,
                              val similarityMetric: SimilarityMetric,
                              val clusterExtractor: ClusterExtractor,
                              val clusterComparator: ClusterComparator,
                              val clusteringEvaluator: ClusteringEvaluator)

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

                                    val (_, unclustered) = accum

                                    unclustered
                                            .asSequence()
                                            .map { elementIndex ->
                                                configuration.clusterExtractor.extractCluster(elementIndex, minSimilarity, matrix, unclustered)
                                            }
                                            .distinct()
                                            .map { Cluster(it, matrix) }
                                            .sortedWith(configuration.clusterComparator)
                                            .onEach { listener?.onEachClusterCreated(it) }
                                            .fold(accum) { accumSoFar, cluster ->
                                                val (clustersSoFar, unclusteredSoFar) = accumSoFar
                                                if (cluster.elements.all(unclusteredSoFar::contains)) {
                                                    Pair(clustersSoFar.add(cluster), unclusteredSoFar.removeAll(cluster.elements))
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
                            configuration.clusteringEvaluator.evaluateClustering(clusters, matrix)
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


