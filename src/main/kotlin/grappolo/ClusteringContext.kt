package grappolo

import java.io.File
import java.io.PrintWriter

data class ClusterEvaluation(val clusters: List<Set<Int>>, val similarity: Similarity)

class ClusteringContext<T>(
    val elements: List<T>,
    val minSimilarity: Similarity,
    val similarityMetric: SimilarityMetric,
    val pairGenerator: PairGenerator,
    val clusterer: Clusterer,
    val clusterEvaluator: ClusterEvaluator
) {

    companion object {
        private val logger = getLogger(this)
    }

    val size: Index = elements.size

    val similarityMatrix: SimilarityMatrix by lazy {
        val (matrix, millis) = time { SimilarityMatrix(size, minSimilarity, pairGenerator, similarityMetric) }
        logger.debug("Similarity matrix built in $millis milliseconds")
        matrix
    }

    private val elementSet = elements.toSet()

    init {
        require(elements.size == elementSet.size) {
            "${elements.size - elementSet.size} duplicates in input set"
        }
    }

    private fun cluster(minSimilarity: Similarity): List<Set<Index>> {
        logger.debug("Clustering $size elements at $minSimilarity")
        return clusterer.cluster(similarityMatrix, minSimilarity)
    }

    // TODO Should add cluster agglomeration?
    val bestClustering: ClusterEvaluation by lazy {
        logger.debug("Finding best clustering")

        val (evaluation, clusters, similarity) =
            similarityMatrix.similarityValues
                .fold(Triple(-1.0, listOf<Set<Index>>(), -1.0)) { accumSoFar, minSimilarity ->

                    val (bestEvaluation, _, _) = accumSoFar

                    val clusters = cluster(minSimilarity)
                    val clusterElements = clusters.flatten().map { index -> elements[index] }.toSet()
                    require(clusterElements == elementSet) {
                        "Input element count (${clusterElements.size}) differs from clustering (${clusterElements.size})"
                    }

                    val evaluation = clusterEvaluator.evaluate(clusters, similarityMatrix)
                    logger.debug("Evaluation for $minSimilarity: $evaluation")

                    if (bestEvaluation > evaluation) {
                        accumSoFar
                    } else {
                        Triple(evaluation, clusters, minSimilarity)
                    }
                }
        logger.debug("Best clustering evaluation: $evaluation. Cluster count: ${clusters.size}")

        ClusterEvaluation(clusters.sortedBy { -it.size }, similarity)
    }

    fun dump(directory: File) {

        fun withFile(baseName: String, action: (PrintWriter) -> Unit) {
            val fileName = "$minSimilarity-$baseName.tsv"
            File(directory, fileName).printWriter().use { out ->
                action(out)
                out.flush()
            }
        }

        fun show(index: Index): String = "$index/${elements[index].toString()}"

        directory.mkdirs()

        withFile("similarities") { out ->
            for (similarityValue in similarityMatrix.similarityValues) {
                out.println(similarityValue)
            }
        }

        withFile("matrix") { out ->
            for (index in similarityMatrix.rows.indices) {
                val rowString =
                    similarityMatrix[index]
                        .scores
                        .toList()
                        .sortedBy { (neighborIndex, _) -> neighborIndex }
                        .joinToString(",") { (neighborIndex, similarity) ->
                            "${show(neighborIndex)}/$similarity"
                        }
                out.println("${show(index)};$rowString")
            }
        }

        withFile("clusters") { out ->
            for (index in bestClustering.clusters.indices) {
                val cluster = bestClustering.clusters[index]
                val clusterElements = cluster.joinToString(",") { neighborIndex -> show(neighborIndex) }
                out.println("$index|${cluster.size};$clusterElements")
            }
        }
    }
}


