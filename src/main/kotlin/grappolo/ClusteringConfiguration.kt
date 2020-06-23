package grappolo

import java.io.File
import java.io.PrintWriter
import javax.script.ScriptEngineManager

data class ClusterEvaluation(val clusters: List<Set<Int>>, val similarity: Similarity)

data class ClusteringConfiguration<T>(
        val source: Source<T>,
        val pairGenerator: PairGenerator,
        val similarityMetric: SimilarityMetric<T>,
        val minSimilarity: Similarity = 0.0,
        val clusterer: Clusterer = GrappoloClusterer,
        val clusterEvaluator: ClusterEvaluator = SimpleClusterEvaluator
) {

    companion object {
        private val logger = getLogger(this)

        @Suppress("UNCHECKED_CAST")
        fun <T> load(file: File): ClusteringConfiguration<T> {
            val factory = ScriptEngineManager().getEngineByExtension("kts").factory
            val engine = factory!!.scriptEngine
            return engine.eval(file.readText()) as ClusteringConfiguration<T>
        }
    }

    val elements = source.elements

    private val elementSet = elements.toSet()

    val similarityMatrix: SimilarityMatrix by lazy {
        val (matrix, millis) = time { SimilarityMatrix(elements, minSimilarity, pairGenerator, similarityMetric) }
        logger.debug("Similarity matrix built in $millis milliseconds")
        matrix
    }

    init {
        require(elements.size == elementSet.size) {
            "${elements.size - elementSet.size} duplicates in input set"
        }
    }

    private fun cluster(minSimilarity: Similarity): List<Set<Index>> {
        logger.debug("Clustering ${elements.size} elements at $minSimilarity")
        return clusterer.cluster(similarityMatrix, minSimilarity)
    }

    // TODO Consider agglomeration?
    val bestClustering: ClusterEvaluation by lazy {
        logger.debug("Finding best clustering")

        // TODO Parallelize cluster evaluation
        val (evaluation, clusters, similarity) =
                similarityMatrix.similarityMap.keys
                        .fold(Triple(0.0, listOf<Set<Index>>(), 0.0)) { accumSoFar, minSimilarity ->

                            val (bestEvaluation, _, _) = accumSoFar

                            val clusters = cluster(minSimilarity)
                            val clusterElements = clusters.flatten().map { index -> elements[index] }.toSet()
                            require(clusterElements == elementSet) {
                                "Input element count (${clusterElements.size}) differs from clustering (${elementSet.size})"
                            }

                            val evaluation = clusterEvaluator.evaluate(clusters, similarityMatrix)
                            logger.debug("Evaluation for $minSimilarity: $evaluation")

                            if (bestEvaluation > evaluation) {
                                accumSoFar
                            } else {
                                Triple(evaluation, clusters, minSimilarity)
                            }
                        }
        logger.debug("Best evaluation: $evaluation. Similarity: $similarity, Cluster count: ${clusters.size}")

        ClusterEvaluation(clusters.sortedBy { -it.size }, similarity)
    }

    // TODO Dump similarities (w/normalized)
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
            for (similarityValue in similarityMatrix.similarityMap.keys) {
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


