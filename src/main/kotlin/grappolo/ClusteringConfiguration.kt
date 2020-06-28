package grappolo

import java.io.File
import java.io.PrintWriter
import java.nio.charset.Charset
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
                // TODO Determine abnormally high first evaluation for cosine, jaccard
                similarityMatrix.similarityMap.keys.sorted().drop(1)
                        //.filter { it in 0.5..1.0 }
                        .fold(Triple(0.0, listOf<Set<Index>>(), 0.0)) { accumSoFar, similarity ->

                            val (bestEvaluation, _, _) = accumSoFar

                            val clusters = cluster(similarity)
                            val clusterElements = clusters.flatten().map { index -> elements[index] }.toSet()
                            require(clusterElements == elementSet) {
                                "Input element count (${clusterElements.size}) differs from clustering (${elementSet.size})"
                            }

                            val evaluation = clusterEvaluator.evaluate(clusters, similarityMatrix)
                            logger.debug("Evaluation for $similarity: $evaluation. Clusters: ${clusters.size}")

                            if (bestEvaluation > evaluation) {
                                accumSoFar
                            } else {
                                Triple(evaluation, clusters, similarity)
                            }
                        }
        logger.debug("Best evaluation: $evaluation. Similarity: $similarity, Cluster count: ${clusters.size}")

        ClusterEvaluation(clusters.sortedBy { -it.size }, similarity)
    }

    // TODO Dump similarities (w/normalized)
    fun dump(directory: File) {

        val utf8 = Charset.forName("UTF-8")

        fun withFile(baseName: String, action: (PrintWriter) -> Unit) {
            val fileName = "$minSimilarity-$baseName.tsv"
            File(directory, fileName).printWriter(utf8).use { out ->
                action(out)
                out.flush()
            }
        }

        fun show(index: Index): String = "$index/${elements[index].toString()}"

        directory.mkdirs()

        withFile("similarities") { out ->
            for ((key, value) in similarityMatrix.similarityMap.toList().sortedBy { it.first }) {
                out.println("$key\t$value")
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

        withFile("vectors") { out ->
            for (i in similarityMatrix.rows.indices) {
                val vector =
                        (0 until similarityMatrix.size)
                                .map { j -> similarityMatrix.rows[i].scores.getOrDefault(j, 0.0) }
                                .joinToString(",")
                out.println(vector)
            }
        }
    }
}


