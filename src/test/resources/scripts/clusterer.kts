// DEPS info.debatty:java-string-similarity:2.0.0

import info.debatty.java.stringsimilarity.Damerau
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.BASIC_ISO_DATE
import kotlin.math.max

fun Double.fmt(digits: Int = 3) = "%.${digits}f".format(this)

fun log(message: String) {
    println("${DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now())}: $message")
}

val baseDirectory = File("./build/results").also { it.mkdirs() }

    val separator = "\\s+".toRegex()
    val values = """
    alejandro alejandor alexandro
    marlene marleny malrene
    marta martha mrata
    jorge jorje
    ricardo
"""
            .split(separator)
            .filterNot(String::isEmpty)
            .sorted()

val similarityThreshold = 0.5

val levenshteinDamerau = Damerau()
fun measureSimilarity(first: String, second: String): Double =
        1.0 - (levenshteinDamerau.distance(first, second) / max(first.length, second.length))

log("Starting clustering. Values: ${values.size}. Similarity threshold: $similarityThreshold")

val similarities = mutableSetOf<Double>()
val emptyMap = mutableMapOf<Int, Double>().withDefault { 0.0 }
val matrix = mutableMapOf<Int, MutableMap<Int, Double>>().withDefault { emptyMap }
values.indices.forEach { i -> matrix[i] = mutableMapOf(i to 1.0).withDefault { 0.0 } }
log("Creating similarity matrix")
for (i in values.indices) {
    for (j in i + 1 until values.size) { // n*(n + 1) / 2
        val similarity = measureSimilarity(values[i], values[j])
        if (similarity >= similarityThreshold) {
            matrix[i]!![j] = similarity
            matrix[j]!![i] = similarity
            similarities += similarity
        }
    }
}
log("Created similarity matrix with ${matrix.values.map { it.size }.sum()} elements")

File(baseDirectory, "matrix.tsv").printWriter().use { out ->
    out.println("\t${values.joinToString("\t") { it }}")
    values.indices.forEach { i ->
        out.print(values[i])
        values.indices.forEach { j ->
            out.print("\t${matrix[i]!![j]?:0.0.fmt(4)}")
        }
        out.println()
    }
}

fun intraSimilarity(cluster: List<Int>): Double =
        if (cluster.size == 1) {
            0.0
        } else {
            cluster.indices
                    .flatMap { i ->
                        cluster.indices
                                .filter { j -> i != j }
                                .map { j -> matrix[cluster[i]]!![cluster[j]] ?: 0.0 }
                    }
                    .average()
        }

fun Map<Int, Map<Int, Double>>.extractCluster(elementIndex: Int, minSimilarity: Double): Set<Int> {

    fun Map<Int, Double>.siblingsAbove(minSimilarity: Double): Set<Int> =
            this.filterValues { it >= minSimilarity }.keys

    val siblingsByElementIndex: Map<Int, Set<Int>> =
            this[elementIndex]!!
                    .siblingsAbove(minSimilarity)
                    .flatMap { siblingIndex ->
                        this[siblingIndex]!!
                                .siblingsAbove(minSimilarity)
                                .map { cousinIndex -> siblingIndex to cousinIndex }
                    }
                    .groupBy { it.first }
                    .mapValues { entry -> entry.value.map { it.second }.toSet() }
                    .filter { entry -> entry.value.contains(elementIndex) }

    if (siblingsByElementIndex.isEmpty()) {
        return setOf(elementIndex)
    }

    val maxSiblingCount: Int = siblingsByElementIndex.values.map { it.size }.max()!!

    val maxIntraSimilarity =
            siblingsByElementIndex
                    .filter { entry -> entry.value.size == maxSiblingCount }
                    .map { entry -> intraSimilarity(entry.value.toList()) }
                    .max()!!

    val bestIndex =
            siblingsByElementIndex
                    .toList()
                    .find { (_, cluster) ->
                        cluster.size == maxSiblingCount && intraSimilarity(cluster.toList()) == maxIntraSimilarity
                    }
                    ?.first
                    ?: return setOf(elementIndex)

    return siblingsByElementIndex[bestIndex]!!
}

fun evaluateClustering(clusters: List<List<Int>>): Double = clusters.map(::intraSimilarity).average()

data class Result(val minSimilarity: Double, val evaluation: Double, val clusters: List<List<Int>>)

log("Building clusters for ${similarities.size} similarities")
val bestResult =
        File(baseDirectory, "results.tsv").printWriter(Charsets.UTF_8).use { out ->

            similarities
                    .sorted()
                    .fold(Result(0.0, 0.0, emptyList())) { bestResultSoFar, minSimilarity ->

                        log("Processing minimum similarity: $minSimilarity")

                        val (clusters, _) =
                                matrix.keys
                                        .map { elementIndex ->
                                            matrix.extractCluster(elementIndex, minSimilarity).sorted()
                                        }
                                        .distinct()
                                        .sortedWith(Comparator { cluster1, cluster2 ->
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
                                        })
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

                        val clusterBasename = "clusters-${minSimilarity.fmt(2)}"
                        File(baseDirectory, "$clusterBasename.dot").printWriter(Charsets.UTF_8).use { dotOut ->
                            dotOut.println("graph {")
                            for (i in values.indices) {
                                dotOut.println("  \"${values[i]}\";")
                            }
                            clusters.forEach { cluster ->
                                for (i in cluster.indices) {
                                    for (j in i + 1 until cluster.size) {
                                        val leftIndex = cluster[i]
                                        val rightIndex = cluster[j]
                                        val similarity = (matrix[leftIndex]!![rightIndex] ?: 0.0).fmt(3)
                                        dotOut.println("  \"${values[leftIndex]}\" -- \"${values[rightIndex]}\" [label=\"$similarity\", weight = \"$similarity\"];")
                                    }
                                }
                            }
                            dotOut.println("}")
                            val errorFile = File(baseDirectory, "$clusterBasename.err")
                            val exitCode =
                                    ProcessBuilder()
                                            .directory(baseDirectory)
                                            .command("dot", "-Tpng", "$clusterBasename.dot", "-o", "$clusterBasename.png")
                                            .redirectError(errorFile)
                                            .start()
                                            .waitFor()
                            if (exitCode != 0) {
                                log("Exit code: $exitCode")
                            }
                            if (exitCode != 0) {
                                log("Exit code: $exitCode")
                            }
                            if (errorFile.length() > 0L) {
                                log("Error generating cluster graph: ${errorFile.readText()}")
                            } else {
                                errorFile.delete()
                                File(baseDirectory, "$clusterBasename.dot").delete()
                            }
                        }

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
        out.println("${cluster.size}\t${cluster.joinToString(",") { values[it] }}")
    }
}

log("End of source deck")
