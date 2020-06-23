package grappolo

import arrow.fx.asCoroutineContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

data class Row(val scores: Map<Index, Similarity>) {

    operator fun get(index: Index): Similarity = scores[index] ?: 0.0

    fun scoresAbove(minSimilarity: Similarity) = scores.filterValues { it >= minSimilarity }
}

class SimilarityMatrix(val rows: Array<Row>, val similarityMap: Map<Similarity, Similarity>) {

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
                        .flatMap { i ->
                            (i + 1 until elements.size)
                                    .map { j -> this[elements[i]][elements[j]] }
                        }
                        .average()
            }

    companion object {

        private val logger = getLogger(this)

        operator fun <T> invoke(
                elements: List<T>,
                minSimilarity: Similarity,
                pairGenerator: PairGenerator,
                similarityMetric: SimilarityMetric<T>
        ): SimilarityMatrix {

            require(elements.isNotEmpty()) { "Invalid similarity matrix size: ${elements.size}" }

            val rows = Array<MutableMap<Index, Similarity>>(elements.size) { ConcurrentHashMap() }

            logger.debug("Computing index pairs")
            val similarityValues: MutableSet<Similarity> = ConcurrentHashMap.newKeySet()
            val (pairs, pairGeneratorMillis) = time { pairGenerator.pairs() }
            logger.debug("Pair generation took ${format(pairGeneratorMillis)} milliseconds")

            logger.debug("Populating similarity matrix")
            fun consumePair(pair: Pair<Index, Index>) {

                val (i, j) = pair
                require(i in elements.indices) { "Invalid first index: $i (${elements.size})" }
                require(j in elements.indices) { "Invalid second index: $j (${elements.size})" }

                val similarity =
                        similarityMetric.computeSimilarity(elements[pair.first], elements[pair.second])
                require(similarity in 0.0..1.0) { "Invalid similarity for ($i, $j): $similarity" }

                if (similarity > 0.0 && similarity >= minSimilarity) {

                    similarityValues += similarity

                    rows[i][j] = similarity
                    rows[j][i] = similarity
                }
            }

            val (_, matrixBuildTime) = time {

                val coroutineContext = Executors.newFixedThreadPool(16).asCoroutineContext()

                runBlocking(coroutineContext) {

                    val channel = Channel<Pair<Index, Index>>()

                    repeat(128) {
                        launch {
                            for (pair in channel) {
                                consumePair(pair)
                            }
                        }
                    }

                    pairs.forEach { pair ->
                        channel.send(pair)
                    }

                    channel.close()
                }
            }
            logger.debug("Populated similarity matrix in $matrixBuildTime milliseconds")

            val (similarityMatrix, normalizationTime) = time {
                val minSimilarityValue = similarityValues.min() ?: 0.0
                val maxSimilarityValue = similarityValues.max() ?: 0.0
                val valueRatio = maxSimilarityValue - minSimilarityValue

                val normalizedSimilarityValues =
                        similarityValues
                                .map { similarityValue ->
                                    similarityValue to (similarityValue - minSimilarityValue) / valueRatio
                                }
                                .toMap()

                rows.forEach { row ->
                    row.keys.forEach { index: Index ->
                        row[index] = normalizedSimilarityValues[row[index]]!!
                    }
                }

                val similarityMap = normalizedSimilarityValues.map { entry -> entry.value to entry.key }.toMap()

                SimilarityMatrix(
                        rows = rows.map { row -> Row(row) }.toTypedArray(),
                        similarityMap = similarityMap)
            }
            logger.debug("Populated similarity values in $normalizationTime milliseconds")

            return similarityMatrix
        }
    }
}

