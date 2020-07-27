package grappolo

interface ClusterExtractor {
    fun extractCluster(elementIndex: Int, minSimilarity: Double, matrix: SimilarityMatrix): Set<Int>
}

object SiblingClusterExtractor : ClusterExtractor {

    override fun extractCluster(elementIndex: Int, minSimilarity: Double, matrix: SimilarityMatrix): Set<Int> {

        val initialElementSet = matrix[elementIndex].elementsAbove(minSimilarity)

        val candidateElementSet = initialElementSet
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

        require(candidateElementSet.contains(elementIndex)) {
            "Cluster does not contain element ($elementIndex): $candidateElementSet"
        }

        val elementCounts =
                candidateElementSet.map { index ->
                    index to candidateElementSet
                            .filter { siblingIndex -> matrix[index][siblingIndex] >= minSimilarity }
                            .size
                }

        val maxElementCount = elementCounts.map { it.second }.max()!!
        val clusterElements = elementCounts.filter { it.second == maxElementCount }.map { it.first }.toSet()
        assert(clusterElements.contains(elementIndex) && clusterElements.size == maxElementCount) {
            "Cluster count mismatch: expected ${maxElementCount}, got ${clusterElements.size}"
        }

        return clusterElements
    }
}
