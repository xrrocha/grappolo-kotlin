package grappolo

class Cluster(set: Set<Int>, matrix: SimilarityMatrix) {

    class ClusterElement(val index: Int, val intraSimilarity: Double) {
        override fun toString(): String = "ClusterElement(index: $index, intraSimilarity: ${intraSimilarity.fmt(4)})"
    }

    val elements: List<ClusterElement>
    val centroids: List<Int>
    val centroidWeight: Double
    val intraSimilarity: Double

    init {

        require(set.isNotEmpty()) { "Cluster cannot be empty" }

        require(set.all { it >= 0 && it < matrix.size }) {
            "Cluster set contains indices inconsistent with similarity matrix"
        }

        fun intraSimilarity(elementIndex: Int) =
                set
                        .filter { index -> index != elementIndex }
                        .map { index -> matrix[elementIndex][index] }
                        .filter { it > 0.0 } // TODO Reinstate score for cluster members
                        .average()

        elements =
                set.sorted().map { elementIndex ->
                    ClusterElement(elementIndex, intraSimilarity(elementIndex))
                }

        if (elements.size == 1) {
            intraSimilarity = 0.0
            centroidWeight = 0.0
            centroids = elements.map { it.index }
        } else {
            intraSimilarity = elements.map { it.intraSimilarity }.average()
            val weightMap =
                    elements
                            .map { it.index }
                            .flatMap { i ->
                                elements
                                        .map { it.index }
                                        .filter { j -> i != j }
                                        .map { j -> i to matrix[i][j] }
                            }
                            .groupBy { it.first }
                            .mapValues { entry -> entry.value.map { it.second }.sum() }
            centroidWeight = weightMap.values.max()!!
            centroids = weightMap.filterValues { it == centroidWeight }.keys.sorted()
        }
    }

    override fun equals(other: Any?): Boolean =
            other != null && other is Cluster && this.elements.size == other.elements.size && this.elements == other

    override fun hashCode(): Int = elements.hashCode()

    override fun toString(): String {
        return "Cluster(size=${elements.size},elements=$elements, centroidWeight: ${centroidWeight.fmt(4)}, centroids=$centroids, intraSimilarity=$intraSimilarity)"
    }
}
