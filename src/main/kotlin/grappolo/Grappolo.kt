package grappolo

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

data class ClusteringResult(val minSimilarity: Double, val evaluation: Double, val clusters: List<Cluster>)

interface ClusteringListener {
    fun beforeMatrixCreation(elementCount: Int, similarityLowThreshold: Double) {}
    fun onMatrixCreated(matrix: SimilarityMatrix, matrixCreationTime: Long) {}
    fun onSimilarity(minSimilarity: Double, index: Int) {}
    fun onClusterCreated(cluster: Cluster) {}
    fun onEachClusteringResult(result: ClusteringResult, evaluationTime: Long) {}
    fun onBestClusteringResult(bestResult: ClusteringResult, clusteringTime: Long) {}
}

object Grappolo {

    fun cluster(elementCount: Int,
                similarityLowThreshold: Double,
                indexPairGenerator: IndexPairGenerator,
                similarityMetric: SimilarityMetric,
                clusterExtractor: ClusterExtractor,
                clusterComparator: ClusterComparator,
                clusteringEvaluator: ClusteringEvaluator,
                listener: ClusteringListener? = null)
            : ClusteringResult {

        val matrix = buildSimilarityMatrix(
                elementCount, similarityLowThreshold,
                indexPairGenerator, similarityMetric,
                listener
        )

        return cluster(matrix, clusterExtractor, clusterComparator, clusteringEvaluator, listener)
    }

    fun cluster(matrix: SimilarityMatrix,
                clusterExtractor: ClusterExtractor,
                clusterComparator: ClusterComparator,
                clusteringEvaluator: ClusteringEvaluator,
                listener: ClusteringListener? = null)
            : ClusteringResult {

        if (matrix.size == 1) {
            val singletonResult = ClusteringResult(0.0, 0.0, listOf(Cluster(setOf(0), matrix)))
            listener?.onBestClusteringResult(singletonResult, 0L)
            return singletonResult
        }

        val (bestResult, clusteringTime) = time {
            matrix.distinctSimilarities().sorted().withIndex()
                    .fold(ClusteringResult(0.0, 0.0, emptyList())) { bestResultSoFar, (index, minSimilarity) ->

                        listener?.onSimilarity(minSimilarity, index)

                        val (partialClusters, clusteredElements) =
                                (0 until matrix.size)
                                        .asSequence()
                                        .map { elementIndex ->
                                            clusterExtractor.extractCluster(elementIndex, minSimilarity, matrix)
                                        }
                                        .distinct()
                                        .map { Cluster(it, matrix) }
                                        .onEach { listener?.onClusterCreated(it) }
                                        .sortedWith(clusterComparator)
                                        .fold(Pair(persistentListOf<Cluster>(), persistentSetOf<Int>())) { accumSoFar, cluster ->
                                            val (clustersSoFar, clusteredSoFar) = accumSoFar
                                            if (cluster.elements.none(clusteredSoFar::contains)) {
                                                Pair(clustersSoFar.add(cluster), clusteredSoFar.addAll(cluster.elements))
                                            } else {
                                                accumSoFar
                                            }
                                        }

                        val clusters =
                                partialClusters +
                                        (0 until matrix.size)
                                                .asSequence()
                                                .filterNot(clusteredElements::contains)
                                                .map { Cluster(setOf(it), matrix) }

                        val clusteredCount = clusters.map { it.elements.size }.sum()
                        require(clusteredCount == matrix.size) {
                            "Cluster element count mismatch; expected ${matrix.size}, got $clusteredCount (from ${clusters.size})"
                        }

                        val (evaluation, evaluationTime) = time {
                            clusteringEvaluator.evaluateClustering(clusters, matrix)
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

        listener?.onBestClusteringResult(bestResult, clusteringTime)
        return bestResult
    }

    fun buildSimilarityMatrix(elementCount: Int,
                              similarityLowThreshold: Double,
                              indexPairGenerator: IndexPairGenerator,
                              similarityMetric: SimilarityMetric,
                              listener: ClusteringListener? = null)
            : SimilarityMatrix {

        require(elementCount > 0) {
            "Element count must be positive, not $elementCount"
        }

        require(similarityLowThreshold >= 0 && similarityLowThreshold < 1)
        {
            "Similarity low threshold must be between 0 (include) and 1 (exclusive; got $similarityLowThreshold"
        }

        listener?.beforeMatrixCreation(elementCount, similarityLowThreshold)

        val (matrix, matrixCreationTime) = time {
            SimilarityMatrix(elementCount).also { matrix ->
                for ((i, j) in indexPairGenerator.pairs()) {

                    require(i in 0 until elementCount) { "Invalid first index: $i" }
                    require(j in 0 until elementCount) { "Invalid second index: $j" }

                    val similarity = if (i == j) 1.0 else similarityMetric.measureSimilarity(i, j)
                    require(similarity >= 0 && similarity < 1.0) {
                        "Similarity must be between 0 (inclusive) and 1 (exclusive); got $similarity"
                    }

                    if (similarity >= similarityLowThreshold) {
                        matrix.addSimilarity(i, j, similarity)
                    }
                }
            }
        }

        listener?.onMatrixCreated(matrix, matrixCreationTime)
        return matrix
    }
}


