package grappolo

class SimilarityMatrix(val size: Int) {

    private val similarities = mutableSetOf<Double>()

    inner class Vector(private val parentIndex: Int) {

        private val elements = mutableMapOf(parentIndex to 1.0)

        operator fun get(index: Int): Double {
            validateIndex(index)
            return elements.getOrDefault(index, 0.0)
        }

        operator fun set(index: Int, similarity: Double) {
            validateIndex(index)
            require(!elements.containsKey((index))) { "Reassignment to similarity value with index $index" }
            elements[index] = similarity
        }

        fun elementsAbove(minSimilarity: Double): Set<Int> =
                elements.filterValues { it >= minSimilarity }.keys

        fun closestElements(minSimilarity: Double): Set<Int> {
            val siblings = elementsAbove(minSimilarity) - parentIndex
            val maxSimilarity = siblings.map { this[it] }.max()
            val set =
                    if (maxSimilarity == null) {
                        emptySet()
                    } else {
                        siblings.filter { this[it] == maxSimilarity }.toSet()
                    }
            return set + parentIndex
        }
    }

    private val vectors = Array(size) { Vector(it) }

    operator fun get(index: Int): Vector {
        validateIndex(index)
        return vectors[index]
    }

    fun addSimilarity(index1: Int, index2: Int, similarity: Double) {
        require(index1 != index2 || similarity == 1.0) {
            "Identity similarity must be 1.0, not $similarity"
        }
        this[index1][index2] = similarity
        this[index2][index1] = similarity
        similarities += similarity
    }

    fun distinctSimilarities(): Set<Double> = similarities.toSet()

    private fun validateIndex(index: Int) {
        require(index in 0 until size) {
            "Index out of bounds: $index; should be between 0 and $size"
        }
    }
}
