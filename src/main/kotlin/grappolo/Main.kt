package grappolo

import info.debatty.java.stringsimilarity.Damerau
import org.slf4j.LoggerFactory
import java.io.File

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

    val values = inputFile.readLines().map(String::trim).distinct().sorted()

    val listener = object : ClusteringListener {

        val logger = LoggerFactory.getLogger(this::class.java)!!

        init {
            logger.info("Clustering lines in file ${inputFile.absolutePath}")
        }

        override fun beforeMatrixCreation(elementCount: Int, similarityLowThreshold: Double) {
            logger.info("About to cluster $elementCount elements with low similarity threshold of $similarityLowThreshold")
        }

        override fun onMatrixCreated(matrix: SimilarityMatrix, matrixCreationTime: Long) {
            logger.info("Matrix created in $matrixCreationTime milliseconds. ${matrix.distinctSimilarities().size} similarities found")
        }

        override fun onSimilarity(minSimilarity: Double, index: Int) {
            logger.info("Processing similarity #${index + 1}: ${minSimilarity.fmt(4)}")
        }

        override fun onClusterCreated(cluster: Cluster) {}

        override fun onEachClusteringResult(result: ClusteringResult, evaluationTime: Long) {
            logger.info("${result.clusters.size} clusters found in $evaluationTime milliseconds")
        }

        override fun onBestClusteringResult(bestResult: ClusteringResult, clusteringTime: Long) {
            logger.info("${bestResult.clusters.size} clusters found in $clusteringTime milliseconds")
        }
    }

    val clusteringResult = Grappolo.cluster(
            elementCount = values.size,
            similarityLowThreshold = 0.5,
            indexPairGenerator = CartesianPairGenerator(values.size),
            similarityMetric = DebattySimilarityMetric(Damerau()) { values[it] },
            clusterExtractor = SiblingClusterExtractor,
            clusterComparator = GreedyClusterComparator,
            clusteringEvaluator = IntraSimilarityClusteringEvaluator,
            listener = listener)

    val resultDirectoryName = "./build/results"
    val resultDirectory = File(resultDirectoryName).apply { mkdirs() }
    File(resultDirectory, "clusters.tsv").printDelimited(
            separator = "\t",
            titles = listOf("size", "elements", "centroids", "centroidWeight", "intraSimilarity"),
            data = clusteringResult.clusters.asSequence().map { cluster ->
                listOf(
                        cluster.elements.size,
                        cluster.elements.joinToString(",") { values[it] },
                        cluster.centroids.joinToString(",") { values[it] },
                        cluster.centroidWeight.fmt(4),
                        cluster.intraSimilarity.fmt(4)
                )
            }
    )
}
