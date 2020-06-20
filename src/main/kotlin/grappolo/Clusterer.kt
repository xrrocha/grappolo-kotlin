package grappolo

interface Clusterer {
    fun cluster(similarityMatrix: SimilarityMatrix, minSimilarity: Similarity): List<Set<Index>>
}

object GrappoloClusterer : Clusterer {

    override fun cluster(similarityMatrix: SimilarityMatrix, minSimilarity: Similarity): List<Set<Index>> {

        data class ClusterStep(
                val collectedClusters: List<Set<Index>>,
                val clusteredElements: Set<Index>,
                val clusterPath: Set<Index>,
                val collectedPaths: Set<Set<Index>>
        )

        fun advance(index: Index, clusterStep: ClusterStep): ClusterStep {

            val neighborIndices = similarityMatrix[index].scoresAbove(minSimilarity).keys

            val nextStep = neighborIndices.fold(clusterStep) { currentStep, neighborIndex ->

                if (currentStep.clusteredElements.contains(neighborIndex) ||
                        currentStep.clusterPath.contains(neighborIndex) ||
                        !currentStep.clusterPath.all { similarityMatrix[it][neighborIndex] >= minSimilarity }
                ) {
                    currentStep
                } else {
                    advance(
                            neighborIndex,
                            currentStep.copy(
                                    clusterPath = currentStep.clusterPath + neighborIndex
                            )
                    )
                }
            }

            return when {
                nextStep.clusterPath.size < nextStep.collectedPaths.first().size -> {
                    nextStep
                }
                nextStep.clusterPath.size == nextStep.collectedPaths.first().size -> {
                    nextStep.copy(
                            collectedPaths = nextStep.collectedPaths.plusElement(nextStep.clusterPath)
                    )
                }
                else -> {
                    nextStep.copy(
                            collectedPaths = setOf(nextStep.clusterPath)
                    )
                }
            }
        }

        val initialStep = ClusterStep(
                collectedClusters = listOf(),
                clusteredElements = setOf(),
                clusterPath = setOf(),
                collectedPaths = setOf()
        )

        val indices = similarityMatrix.rows.indices

        val lastClusterStep = indices.fold(initialStep) { clusterStep, index ->

            if (clusterStep.clusteredElements.contains(index)) {

                clusterStep
            } else {

                val resultClusterStep =
                        advance(
                                index,
                                clusterStep.copy(
                                        clusterPath = setOf(index),
                                        collectedPaths = setOf(setOf(index))
                                )
                        )

//                val (_, clusters) =
//                        resultClusterStep.collectedPaths
//                                .map { cluster -> Pair(similarityMatrix.intraSimilarity(cluster), cluster) }
//                                .fold(Pair(0.0, emptySet<Set<Index>>())) { accumPair, clusterPair ->
//                                    val (bestIntraSimilarity, bestClusters) = accumPair
//                                    val (clusterIntraSimilarity, cluster) = clusterPair
//                                    when {
//                                        clusterIntraSimilarity > bestIntraSimilarity -> Pair(clusterIntraSimilarity, setOf(cluster))
//                                        clusterIntraSimilarity == bestIntraSimilarity -> Pair(bestIntraSimilarity, bestClusters.plusElement(cluster))
//                                        else -> accumPair
//                                    }
//                                }
//
//                val baseCluster = clusters.flatten().toSet()

                val baseCluster = resultClusterStep.collectedPaths.flatten().toSet()

                // Add centroid siblings
                val intraSimilarities = baseCluster.map { i ->
                    val intraSimilarity =
                            baseCluster
                                    .filter { j -> i != j }
                                    .map { j -> similarityMatrix[i][j] }
                                    .average()
                    Pair(i, intraSimilarity)
                }

                val maxIntraSimilarity =
                        intraSimilarities
                                .map { it.second }
                                .max() ?: 0.0
                val centroids =
                        intraSimilarities
                                .filter { it.second == maxIntraSimilarity }
                                .map { it.first }

                val cluster = baseCluster + centroids.flatMap { i ->
                    similarityMatrix[i]
                            .scoresAbove(minSimilarity)
                            .keys
                            .filterNot(clusterStep.clusteredElements::contains)
                }

                resultClusterStep.copy(
                        collectedClusters = clusterStep.collectedClusters.plusElement(cluster),
                        clusteredElements = clusterStep.clusteredElements + cluster
                )
            }
        }

        return lastClusterStep.collectedClusters
    }
}

