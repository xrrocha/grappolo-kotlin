package grappolo

import info.debatty.java.stringsimilarity.Damerau
import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter
import java.time.LocalDateTime

fun main() {

    val inputDirectoryName = "surnames"
    val dataDirectory = File("data/$inputDirectoryName")
    require(dataDirectory.exists() && dataDirectory.canRead()) {
        "Inaccessible data directory ${dataDirectory.absolutePath}"
    }
    val inputFile = File(dataDirectory, "data.tsv")
    require(inputFile.isFile && inputFile.canRead()) {
        "Inaccessible file ${inputFile.absolutePath}"
    }

    val resultDirectoryName = "./build/results"
    val resultDirectory = File(resultDirectoryName).apply { mkdirs() }

    // Each line assumed to be *distinct*
    val values = inputFile.readLines().map(String::trim)

    val configuration = ClusteringConfiguration(
            elementCount = values.size,
            similarityLowThreshold = 0.5,
            indexPairGenerator = CartesianPairGenerator(values.size),
            similarityMetric = DebattySimilarityMetric(Damerau()) { values[it] },
            clusterExtractor = ClosestSiblingClusterExtractor,
            clusterComparator = GreedyClusterComparator,
            clusteringEvaluator = IntraSimilarityClusteringEvaluator)

    val clusteringResult =
            Grappolo.cluster(configuration, SimpleListener(resultDirectory, values, logResults = true))

    File(resultDirectory, "results-summary.txt").printWriter().use { out ->
        clusteringResult.printSummary(out)
        out.println()
        configuration.printSummary(out)
    }
}

class SimpleListener(private val resultDirectory: File,
                     private val values: List<String>,
                     private val logResults: Boolean = false) : ClusteringListener {

    private val logger = LoggerFactory.getLogger(this::class.java)!!

    override fun onMatrixCreated(matrix: SimilarityMatrix, matrixCreationTime: Long) {
        logger.info("Matrix created in $matrixCreationTime milliseconds. ${matrix.distinctSimilarities().size} similarities found")
        matrix.printToFile(File(resultDirectory, "matrix.tsv")) { values[it] }
    }

    override fun onEachSimilarity(minSimilarity: Double, index: Int) {
        logger.info("Processing similarity #${index + 1}: ${minSimilarity.fmt(4)}")
    }

    override fun onEachClusteringResult(result: ClusteringResult, evaluationTime: Long) {
        logger.info("${result.clusters.size} clusters found with average intra-similarity ${result.clusters.map(Cluster::intraSimilarity).average()} for ${result.minSimilarity} in $evaluationTime milliseconds. Evaluation: ${result.evaluation}")
        if (logResults) {
            result.printToFile(File(resultDirectory, "clusters-${result.minSimilarity.fmt(4)}.tsv")) { values[it] }
        }
    }

    override fun afterClustering(result: ClusteringResult, clusteringTime: Long) {
        logger.info("Best result: ${result.clusters.size} clusters found in $clusteringTime milliseconds. Similarity: ${result.minSimilarity}. Evaluation: ${result.evaluation}")
    }
}

fun SimilarityMatrix.printToFile(file: File, toString: (Int) -> String = Int::toString) {
    file.printDelimited(
            separator = "\t",
            titles = listOf("index", "elements"),
            data = (0 until this.size).asSequence().map { i ->
                val elements =
                        (0 until this.size)
                                .filter { j -> this[i][j] > 0.0 }
                                .joinToString(",") { j -> "$j/${toString(j)}/${this[i][j]}" }
                listOf(i, elements)
            }
    )
}

fun ClusteringResult.printToFile(file: File, toString: (Int) -> String = Int::toString) {
    file.printDelimited(
            separator = "\t",
            titles = listOf("size", "elements", "centroids", "centroidWeight", "intraSimilarity"),
            data = this.clusters.asSequence().map { cluster ->
                listOf(
                        cluster.elements.size,
                        cluster.elements.joinToString(",", transform = toString),
                        cluster.centroids.joinToString(",", transform = toString),
                        cluster.centroidWeight.fmt(4),
                        cluster.intraSimilarity.fmt(4)
                )
            })
}

fun ClusteringConfiguration.printSummary(out: PrintWriter) {

    fun Any.className() = this::class.java.simpleName

    out.println("*** Configuration")
    out.println("Element count: $elementCount")
    out.println("Similarity low threshold: $similarityLowThreshold")
    out.println("Index pair generator: ${indexPairGenerator.className()}")
    out.println("Similarity metric: ${similarityMetric.className()}")
    out.println("Cluster extractor: ${clusterExtractor.className()}")
    out.println("Cluster comparator: ${clusterComparator.className()}")
    out.println("Clustering evaluator: ${clusteringEvaluator.className()}")
}

fun ClusteringResult.printSummary(out: PrintWriter) {
    out.println("*** Clustering result")
    out.println("Date/Time: ${LocalDateTime.now()}")
    out.println("Similarity: $minSimilarity")
    out.println("Evaluation: $evaluation")
    out.println("Element Count: ${clusters.map { it.elements.size }.sum()}")
    out.println("Cluster Count: ${clusters.size}")
}
