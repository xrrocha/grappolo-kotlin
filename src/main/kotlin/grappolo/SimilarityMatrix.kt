package grappolo

data class Row(val scores: Map<Index, Similarity>) {

    operator fun get(index: Index): Similarity = scores[index] ?: 0.0

    fun scoresAbove(minSimilarity: Similarity) = scores.filterValues { it >= minSimilarity }
}

class SimilarityMatrix(val rows: Array<Row>, val similarityValues: List<Similarity>) {

    val size: Index = rows.size

    operator fun get(index: Index): Row {
        require(index >= 0 && index < rows.size) { "Invalid index: $index" }
        return rows[index]
    }

    fun intraSimilarity(cluster: Set<Index>): Similarity =
            if (cluster.size < 2) {
                0.0
            } else {

                val elements = cluster.toList()
                elements.indices
                        .flatMap { i -> (i + 1 until elements.size).map { j -> this[elements[i]][elements[j]] } }
                        .average()
            }

    companion object {

        private val logger = getLogger(this)

        operator fun invoke(
                size: Index,
                minSimilarity: Similarity,
                pairGenerator: PairGenerator,
                similarityMetric: SimilarityMetric
        ): SimilarityMatrix {

            require(size > 0) { "Invalid similarity matrix size: $size" }

            val rows = Array<MutableMap<Index, Similarity>>(size) { mutableMapOf() }

            logger.debug("Computing index pairs")
            val similarityValues = mutableSetOf<Similarity>()
            val (pairs, millis) = time { pairGenerator.pairs() }
            logger.debug("Pair generation took ${format(millis)} milliseconds")

            // TODO Parallelize index pair consumption
            logger.debug("Populating similarity matrix")
            for ((i, j) in pairs) {

                require(i in 0 until size) { "Invalid first index: $i ($size)" }
                require(j in 0 until size) { "Invalid second index: $j ($size)" }

                val similarity = similarityMetric.computeSimilarity(i, j)
                require(similarity in 0.0..1.0) { "Invalid similarity for ($i, $j): $similarity" }

                if (similarity > 0.0 && similarity >= minSimilarity) {

                    rows[i][j] = similarity
                    rows[j][i] = similarity

                    similarityValues += similarity
                }
            }
            logger.debug("Done populating similarity matrix")

            val minSimilarityValue = similarityValues.min() ?: 0.0
            val maxSimilarityValue = similarityValues.max() ?: 0.0
            val valueRatio = maxSimilarityValue - minSimilarityValue

            val normalizedSimilarityValues =
                    similarityValues
                            .map { similarityValue -> (similarityValue - minSimilarityValue) / valueRatio }
                            .toList()
                            .sorted()

            val normalizedRows = rows.map { row ->
                row
                        .toList()
                        .map { (index, similarity) -> index to (similarity - minSimilarityValue) / valueRatio }
                        .toMap()
            }

            val rowArray = normalizedRows.map { row -> Row(row) }.toTypedArray()

            return SimilarityMatrix(rowArray, normalizedSimilarityValues)
        }
    }
}

