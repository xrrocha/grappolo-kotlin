// DEPS info.debatty:java-string-similarity:2.0.0

import info.debatty.java.stringsimilarity.Damerau
import java.io.File
import kotlin.math.max

val separator = "\\s+".toRegex()
val names = """
    alejandro alejandor alexandro
    marlene marleny malrene
    marta martha mrata
    jorge jorje
    ricardo
"""
        .split(separator)
        .filterNot(String::isEmpty)
        .sorted()

val levenshteinDamerau = Damerau()
fun measureSimilarity(first: String, second: String): Double =
        1.0 - (levenshteinDamerau.distance(first, second) / max(first.length, second.length))

val similarities = mutableSetOf<Double>()
val matrix = Array(names.size) { Array(names.size) { 0.0 } }
for (i in matrix.indices) matrix[i][i] = 1.0
for (i in names.indices) {
    for (j in i + 1 until names.size) { // n*(n + 1) / 2
        val similarity = measureSimilarity(names[i], names[j])
        matrix[i][j] = similarity
        matrix[j][i] = similarity
        similarities += similarity
    }
}

File("matrix.tsv").printWriter(Charsets.UTF_8).use { out ->
    out.println("\t${names.joinToString("\t")}")
    matrix.indices.forEach { index ->
        out.println("${names[index]}\t${matrix[index].joinToString("\t")}")
    }
}

fun Double.fmt(digits: Int = 3) = "%.${digits}f".format(this)

fun findClustersFor(minSimilarity: Double): List<List<Int>> {

    val unclustered = matrix.indices.toMutableSet()
    fun traverse(i: Int, cluster: MutableList<Int>) {
        if (unclustered.contains(i) && cluster.all { j -> matrix[i][j] >= minSimilarity }) {
            cluster += i
            unclustered -= i
            matrix.indices
                    .filter { j -> matrix[i][j] >= minSimilarity }
                    .sortedByDescending { j -> matrix[i][j] }
                    .forEach { sibling -> traverse(sibling, cluster) }
        }
    }

    val clusters = mutableListOf<List<Int>>()
    generateSequence {
        val (index, _) =
                unclustered.fold(Pair(-1, 0.0)) { bestSoFar, i ->
                    val (_, bestWeight) = bestSoFar
                    val weight =
                            matrix[i].indices
                                    .filter { j ->
                                        matrix[i][j] >= minSimilarity && !unclustered.contains(j)
                                    }
                                    .map { j -> matrix[i][j] }
                                    .sum()
                    if (bestWeight > weight) {
                        bestSoFar
                    } else {
                        Pair(i, weight)
                    }
                }
        index
    }
            .takeWhile { index -> index >= 0 }
            .forEach { index ->
                val cluster = mutableListOf<Int>()
                traverse(index, cluster)
                if (cluster.size > 0) {
                    clusters += cluster
                }
            }

    return clusters
}

fun evaluateClustering(clusters: List<List<Int>>): Double =
        clusters
                .map { cluster ->
                    if (cluster.size == 1) {
                        0.0
                    } else {
                        cluster.indices
                                .flatMap { i ->
                                    cluster.indices
                                            .filter { j -> i != j }
                                            .map { j -> matrix[cluster[i]][cluster[j]] }
                                }
                                .average()
                    }
                }
                .average()

class Result(val minSimilarity: Double, val evaluation: Double, val clusters: List<List<Int>>) {
    fun toString(show: (Int) -> String) =
            "${minSimilarity.fmt()}\t${evaluation.fmt()}\t${clusters.map { cluster -> cluster.joinToString(",", "{", "}", transform = show) }}"
}

val results = similarities
        .map { minSimilarity ->
            val clusters = findClustersFor(minSimilarity)
            val evaluation = evaluateClustering(clusters)
            Result(minSimilarity, evaluation, clusters)
        }
        .sortedBy { it.minSimilarity }

val bestResult = results.fold(Result(-1.0, -1.0, emptyList())) { bestResultSoFar, currentResult ->
    if (currentResult.evaluation > bestResultSoFar.evaluation) {
        currentResult
    } else {
        bestResultSoFar
    }
}

File("clusters.tsv").printWriter().use { out ->
    bestResult.clusters.forEach { cluster ->
        out.println(cluster.joinToString(",") { names[it] })
    }
}

File("results.tsv").printWriter(Charsets.UTF_8).use { resultsOut ->

    for (result in results) {

        resultsOut.println(result.toString { names[it] })

        val clusterBasename = "clusters-${result.minSimilarity.fmt(2)}"
        File("$clusterBasename.dot").printWriter(Charsets.UTF_8).use { out ->
            out.println("graph {")

            for (i in names.indices) {
                out.println("  ${names[i]};")
            }
            result.clusters.forEach { cluster ->
                for (i in cluster.indices) {
                    for (j in i + 1 until cluster.size) {
                        val leftIndex = cluster[i]
                        val rightIndex = cluster[j]
                        val similarity = matrix[leftIndex][rightIndex].fmt(3)
                        out.println("  ${names[leftIndex]} -- ${names[rightIndex]} [label=\"$similarity\", weight = \"$similarity\"];")
                    }
                }
            }

            out.println("}")
        }

        val errorFile = File("$clusterBasename.err")
        ProcessBuilder()
                .command("dot", "-Tpng", "$clusterBasename.dot", "-o", "$clusterBasename.png")
                .redirectError(errorFile)
                .start()
                .waitFor()
        if (errorFile.length() == 0L) {
            errorFile.delete()
            File("$clusterBasename.dot").delete()
        }
    }
}
