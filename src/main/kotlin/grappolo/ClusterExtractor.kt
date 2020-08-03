package grappolo

interface ClusterExtractor {
    fun extractCluster(elementIndex: Int, minSimilarity: Double, matrix: SimilarityMatrix, unclustered: Set<Int>): Set<Int>
}

object ClosestSiblingClusterExtractor : ClusterExtractor {

    override fun extractCluster(elementIndex: Int, minSimilarity: Double, matrix: SimilarityMatrix, unclustered: Set<Int>): Set<Int> {

        val initialElementSet = matrix[elementIndex].elementsAbove(minSimilarity, unclustered)

        return initialElementSet
                .flatMap { index ->
                    matrix[index]
                            .closestElements(minSimilarity, unclustered)
                            .flatMap { siblingIndex ->
                                matrix[siblingIndex]
                                        .closestElements(minSimilarity, unclustered).map { cousinIndex ->
                                            listOf(index, siblingIndex, cousinIndex)
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
