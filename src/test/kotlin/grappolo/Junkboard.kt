package grappolo

import info.debatty.java.stringsimilarity.Damerau
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import kotlin.math.max

//
//data class Size(val value: Int) {
//}
//data class Index(val value: Int)

class Matrix(private val size: Int) {

    private val similarities = mutableSetOf<Double>()

    inner class Vector(private val parentIndex: Int) {

        internal val elements = mutableMapOf(parentIndex to 1.0)

        operator fun get(index: Int): Double {
            validateIndex(index)
            return elements.getOrDefault(index, 0.0)
        }

        operator fun set(index: Int, similarity: Double): Unit {
            validateIndex(index)
            require(!elements.containsKey((index))) { "Reassignment to similarity value with index $index" }
            elements[index] = similarity
        }

        fun siblings(minSimilarity: Double): Set<Int> =
                pullSiblings().filterValues { it >= minSimilarity }.keys

        fun closestSiblings(minSimilarity: Double): Set<Int> {
            val siblings = siblings(minSimilarity)
            val maxSimilarity = siblings.map { this[it] }.max()
            return if (maxSimilarity == null) emptySet() else siblings.filter { this[it] == maxSimilarity }.toSet()
        }

        private fun pullSiblings() = elements.filterKeys { it != parentIndex }
    }

    private val vectors = Array(size) { Vector(it) }

    operator fun get(index: Int): Vector {
        validateIndex(index)
        return vectors[index]
    }

    fun addSimilarity(index1: Int, index2: Int, similarity: Double): Unit {
        require(index1 != index2 || similarity == 1.0) { "Identity similarity must be 1.0, not $similarity" }
        this[index1][index2] = similarity
        this[index2][index1] = similarity
        similarities += similarity
    }

    val allSimilarities: Set<Double> by lazy { similarities }
    val elementCount: Int by lazy { vectors.map { it.elements.size }.sum() }

    private fun validateIndex(index: Int) {
        require(index in 0 until size) { "Index out of bounds: $index; should be between 0 and $size" }
    }
}

fun Double.fmt(digits: Int = 2) = "%.${digits}f".format(this)

fun main() {

    fun log(message: String) {
        println("${ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())}: $message")
    }

    val baseDirectory = File("./build/results").also { it.mkdirs() }

    val values = File("./data/surnames/data.tsv").readLines()
//    val separator = "\\s+".toRegex()
//    val values = """
//        alejandro alejandor alexandro
//        marlene marleny malrene
//        marta martha mrata
//        jorge jorje
//        ricardo
//    """
//            .split(separator)
//            .filterNot(String::isEmpty)
//            .sorted()

    val similarityThreshold = 0.63

    val levenshteinDamerau = Damerau()
    fun measureSimilarity(first: String, second: String): Double =
            1.0 - (levenshteinDamerau.distance(first, second) / max(first.length, second.length))

    log("Starting clustering. Values: ${values.size}. Similarity threshold: $similarityThreshold")

    val matrix = Matrix(values.size)
    log("Creating similarity matrix")
    for (i in values.indices) {
        for (j in i + 1 until values.size) { // n*(n + 1) / 2
            val similarity = measureSimilarity(values[i], values[j])
            if (similarity >= similarityThreshold) {
                matrix.addSimilarity(i, j, similarity)
            }
        }
    }
    log("Created similarity matrix with ${matrix.elementCount} elements")

    fun intraSimilarity(cluster: List<Int>): Double =
            if (cluster.size == 1) {
                0.0
            } else {
                cluster.indices
                        .flatMap { i ->
                            cluster.indices.map { j -> matrix[cluster[i]][cluster[j]] }
                        }
                        .average()
            }

    fun Matrix.extractCluster(elementIndex: Int, minSimilarity: Double): Set<Int> {

        val initialCluster = this[elementIndex].siblings(minSimilarity)

        val candidateCluster = initialCluster
                .flatMap { index ->
                    this[index].closestSiblings(minSimilarity).flatMap { siblingIndex ->
                        this[siblingIndex].closestSiblings(minSimilarity).map { cousinIndex ->
                            listOf(index, siblingIndex, cousinIndex)
                        }
                    }
                }
                .filter { indices ->
                    indices.all(initialCluster::contains)
                }
                .flatten()
                .toSet()

        if (candidateCluster.isEmpty()) {
            return setOf(elementIndex)
        }

        val pairs =
                candidateCluster.map { index ->
                    index to candidateCluster
                            .filter { siblingIndex -> this[index][siblingIndex] >= minSimilarity }
                            .size
                }

        val maxSiblingCount = pairs.map { it.second }.max()!!
        val cluster = pairs.filter { it.second == maxSiblingCount }.map { it.first }.toSet()
        assert(cluster.size == maxSiblingCount)
        { "Cluster count mismatch for ${values[elementIndex]}. Expected ${maxSiblingCount}, got ${cluster.size}" }

        return if (cluster.isEmpty()) setOf(elementIndex) else cluster
    }

    fun evaluateClustering(clusters: List<List<Int>>): Double = clusters.map(::intraSimilarity).average()

    data class Result(val minSimilarity: Double, val evaluation: Double, val clusters: List<List<Int>>)

    val clusterComparator = Comparator<List<Int>> { cluster1, cluster2 ->
        if (cluster2.size != cluster1.size) {
            cluster2.size - cluster1.size
        } else {
            fun weight(cluster: List<Int>): Double =
                    cluster.indices.flatMap { i ->
                        (i + 1 until cluster.size)
                                .map { j ->
                                    matrix[cluster[i]]!![cluster[j]] ?: 0.0
                                }
                    }
                            .sum()

            (weight(cluster2) - weight(cluster1)).toInt()
        }
    }

    log("Building clusters for ${matrix.allSimilarities.size} similarities")
    val bestResult =
            File(baseDirectory, "results.tsv").printWriter(Charsets.UTF_8).use { out ->

                matrix.allSimilarities
                        .sorted()
                        .fold(Result(0.0, 0.0, emptyList())) { bestResultSoFar, minSimilarity ->

                            log("Processing minimum similarity: $minSimilarity")

                            val (clusters, _) =
                                    values.indices
                                            .map { elementIndex ->
                                                matrix.extractCluster(elementIndex, minSimilarity).sorted()
                                            }
                                            .distinct()
                                            .sortedWith(clusterComparator)
                                            .fold(Pair(persistentListOf<List<Int>>(), persistentSetOf<Int>())) { accumSoFar, cluster ->
                                                val (clustersSoFar, clusteredSoFar) = accumSoFar
                                                if (cluster.none(clusteredSoFar::contains)) {
                                                    Pair(clustersSoFar.add(cluster), clusteredSoFar.addAll(cluster))
                                                } else {
                                                    accumSoFar
                                                }
                                            }

                            val evaluation = evaluateClustering(clusters)

                            out.println("${minSimilarity.fmt(4)}\t${evaluation.fmt(4)}\t${clusters.size}")

                            val currentResult = Result(minSimilarity, evaluation, clusters)
                            log("Results for similarity ${minSimilarity.fmt(2)}. Evaluation = ${evaluation.fmt(2)}. Clusters: ${clusters.size}")

                            if (evaluation > bestResultSoFar.evaluation) {
                                currentResult
                            } else {
                                bestResultSoFar
                            }

                        }
            }

    log("${bestResult.clusters.size} clusters created")
    log("Result. minSimilarity: ${bestResult.minSimilarity}, evaluation: ${bestResult.evaluation}, clusters: ${bestResult.clusters.size}")

    File(baseDirectory, "clusters.tsv").printWriter().use { out ->
        bestResult.clusters.forEach { cluster ->
            out.println("${cluster.size}\t${cluster.sortedBy { values[it] }.joinToString(",") { values[it] }}")
        }
    }

    log("End of source deck")
}


