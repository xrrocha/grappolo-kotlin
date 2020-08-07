package grappolo

interface ClusterExtractor {
    fun extractCluster(elementIndex: Int, minSimilarity: Double, matrix: SimilarityMatrix, available: Set<Int>): Set<Int>
}

object ClosestSiblingClusterExtractor : ClusterExtractor, Named {

    override val name = "closestSiblings"

    override fun extractCluster(elementIndex: Int, minSimilarity: Double, matrix: SimilarityMatrix, available: Set<Int>): Set<Int> {

        val initialElementSet = matrix[elementIndex].elementsAbove(minSimilarity, available)

        return initialElementSet
                .flatMap { index ->
                    matrix[index]
                            .closestElements(minSimilarity, available)
                            .flatMap { siblingIndex ->
                                matrix[siblingIndex]
                                        .closestElements(minSimilarity, available).map { stepSiblingIndex ->
                                            listOf(index, siblingIndex, stepSiblingIndex)
                                        }
                            }
                }
                .filter { indices ->
                    indices.all(initialElementSet::contains)
                }
                .flatten()
                .toSet()
    }
}


object ExhaustiveTraversalClusterExtractor : ClusterExtractor, Named {

    override val name = "exhaustiveTraversal"

    override fun extractCluster(elementIndex: Int, minSimilarity: Double, matrix: SimilarityMatrix, available: Set<Int>): Set<Int> {

        data class ClusterStep(
                val collectedClusters: List<Set<Int>>,
                val clusteredElements: Set<Int>,
                val clusterPath: Set<Int>,
                val collectedPaths: Set<Set<Int>>
        )

        fun advance(index: Int, clusterStep: ClusterStep): ClusterStep {

            val neighborIndices = matrix[index].elementsAbove(minSimilarity, available)

            val nextStep =
                    neighborIndices.fold(clusterStep) { currentStep, neighborIndex ->

                        if (currentStep.clusteredElements.contains(neighborIndex) ||
                                currentStep.clusterPath.contains(neighborIndex) ||
                                !currentStep.clusterPath.all { matrix[it][neighborIndex] >= minSimilarity }
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

        return if (!available.contains(elementIndex)) {

            emptySet()

        } else {

            val initialStep = ClusterStep(
                    collectedClusters = listOf(),
                    clusteredElements = setOf(),
                    clusterPath = setOf(elementIndex),
                    collectedPaths = setOf(setOf(elementIndex))
            )

            advance(elementIndex, initialStep).collectedPaths.flatten().toSet()
        }
    }
}