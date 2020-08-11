package grappolo

import org.slf4j.LoggerFactory
import java.io.File
import java.io.PrintWriter
import java.time.LocalDateTime

fun main() {

    val logger = LoggerFactory.getLogger("grappolo.MainKt")

    val datasetName = "surnames"
    val dataDirectory = File("data/$datasetName")
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
            similarityMetric = DamerauSimilarityMetric { values[it] },
            clusterExtractor = ExhaustiveTraversalClusterExtractor, // ClosestSiblingClusterExtractor,
            clusterComparator = GreedyClusterComparator,
            clusteringEvaluator = IntraSimilarityClusteringEvaluator)

    val result =
            Grappolo.cluster(configuration,
                    SimpleListener(datasetName, resultDirectory, values, logResults = true))
    logger.info("${result.clusters.size} clusters for similarity ${result.minSimilarity.fmt(4)} and evaluation ${result.evaluation.fmt(4)}")
}

class SimpleListener(private val datasetName: String,
                     private val resultDirectory: File,
                     private val values: List<String>,
                     private val logResults: Boolean = false) : ClusteringListener {

    private lateinit var outputDirectory: File
    private lateinit var summaryWriter: PrintWriter
    private lateinit var evaluationWriter: PrintWriter

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)!!
    }

    override fun beforeClustering(configuration: ClusteringConfiguration) {

        outputDirectory = File(resultDirectory, "$datasetName-${configuration.signature()}")
        logger.info("Cleaning up result directory: ${outputDirectory.absolutePath}")
        outputDirectory.deleteRecursively()
        outputDirectory.mkdirs()

        summaryWriter = File(outputDirectory, "results-summary.txt").printWriter()
        configuration.printSummary(summaryWriter)
        summaryWriter.println()
    }

    override fun onMatrixCreated(matrix: SimilarityMatrix, matrixCreationTime: Long) {
        evaluationWriter = File(outputDirectory, "evaluations.tsv").printWriter()
        evaluationWriter.println("similarity\tevaluation\tclusters")
        logger.info("Matrix created in $matrixCreationTime milliseconds. ${matrix.distinctSimilarities().size} similarities found")
        matrix.printToFile(File(outputDirectory, "matrix.tsv")) { values[it] }
    }

    override fun onEachSimilarity(minSimilarity: Double, index: Int) {
        logger.info("Processing similarity #${index + 1}: ${minSimilarity.fmt(4)}")
    }

    override fun onEachClusteringResult(result: ClusteringResult, evaluationTime: Long) {
        evaluationWriter.println("${result.minSimilarity.fmt(4)}\t${result.evaluation.fmt(4)}\t${result.clusters.size.fmt()}")
        logger.info("${result.clusters.size} clusters found with average intra-similarity ${result.clusters.map(Cluster::intraSimilarity).average()} for ${result.minSimilarity} in $evaluationTime milliseconds. Evaluation: ${result.evaluation}")
        if (logResults) {
            result.printToFile(File(outputDirectory, "clusters-${result.minSimilarity.fmt(4)}.tsv")) { values[it] }
        }
    }

    override fun afterClustering(result: ClusteringResult, clusteringTime: Long) {
        logger.info("Best result: ${result.clusters.size} clusters found in $clusteringTime milliseconds. Similarity: ${result.minSimilarity}. Evaluation: ${result.evaluation}")

        result.printSummary(summaryWriter)

        summaryWriter.close()
        evaluationWriter.close()
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
                        cluster.elements.map { it.index }.joinToString(",", transform = toString),
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
