package grappolo

class Cluster(set: Set<Int>, matrix: SimilarityMatrix) {

    val elements: List<Int>
    val centroids: List<Int>
    val centroidWeight: Double
    val intraSimilarity: Double

    init {

        require(set.isNotEmpty()) { "Cluster cannot be empty" }

        require(set.all { it >= 0 && it < matrix.size }) {
            "Cluster set contains indices inconsistent with similarity matrix"
        }

        elements = set.sorted()

        intraSimilarity =
                if (elements.size == 1) {
                    0.0
                } else {
                    val list = elements.toList()
                    elements.indices.flatMap { i -> elements.indices.map { j -> matrix[list[i]][list[j]] } }.average()
                }

        val weightMap =
                elements
                        .flatMap { i -> elements.map { j -> i to matrix[i][j] } }
                        .groupBy { it.first }
                        .mapValues { entry -> entry.value.map { it.second }.sum() }
        centroidWeight = weightMap.values.max()!!
        centroids = weightMap.filterValues { it == centroidWeight }.keys.sorted()
    }

    override fun equals(other: Any?): Boolean =
            other != null && other is Cluster && this.elements.size == other.elements.size && this.elements == other

    override fun hashCode(): Int = elements.hashCode()
}
