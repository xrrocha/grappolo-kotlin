package grappolo

interface ClusterExtractor {
    fun extractCluster(elementIndex: Int, minSimilarity: Double, matrix: SimilarityMatrix): Set<Int>
}

object SiblingClusterExtractor : ClusterExtractor {

    override fun extractCluster(elementIndex: Int, minSimilarity: Double, matrix: SimilarityMatrix): Set<Int> {

        val initialElementSet = matrix[elementIndex].elementsAbove(minSimilarity)

        return initialElementSet
                .flatMap { index ->
                    matrix[index].closestElements(minSimilarity).flatMap { siblingIndex ->
                        matrix[siblingIndex].closestElements(minSimilarity).map { cousinIndex ->
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
