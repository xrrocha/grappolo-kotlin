package grappolo

import java.io.File

fun main() {

    val logger = getLogger("grappolo.Main")

    val minSimilarity = 0.0

    val linesToSkip =  0
    val linesToRead = Int.MAX_VALUE

    val baseDirectoryName = "data"
    val dataFileName = "words.tsv"
    val dataDirectoryName = "school-names"

    val baseDirectory = File(baseDirectoryName).also {
        require(it.isDirectory && it.canRead()) {
            "Can't access data directory: ${it.absolutePath}"
        }
    }

    val workDirectory = File(baseDirectory, dataDirectoryName).also {
        require(it.isDirectory && it.canRead() && it.canWrite()) {
            "Can't access data directory: ${it.absolutePath}"
        }
    }

    val dataFile = File(workDirectory, dataFileName).also {
        require(it.isFile && it.canRead()) {
            "Can't read data file: ${it.absolutePath}"
        }
    }

    val similarityMetric: (List<String>) -> SimilarityMetric = { lines ->
        DamerauSimilarityMetric(lines)
    }

    val pairGenerator: (Int) -> PairGenerator = {
        FilePairGenerator("${workDirectory.absolutePath}/word-pairs.tsv")
    }

    val clusterer = GrappoloClusterer
    val clusterEvaluator = SimpleClusterEvaluator

    val lines = readLines(dataFile, linesToRead, linesToSkip)
    logger.debug("Loaded ${lines.size} lines from $dataFile")

    val context = ClusteringContext(
            lines, minSimilarity,
            similarityMetric(lines), pairGenerator(lines.size),
            clusterer, clusterEvaluator
    )

    val (evaluation, millis) = time { context.bestClustering }
    logger.debug("${format(evaluation.clusters.size)} clusters found in ${format(lines.size)} elements.}")
    logger.debug("Elapsed time: ${format(millis)}")

    context.dump(workDirectory)

    val expectedLines = lines.sorted()
    val actualLines = evaluation.clusters.map { it.toList() }.flatten().map { lines[it] }.sorted()
    if (actualLines != expectedLines) {

        logger.error("Mismatch expected elements (${expectedLines.size}) and cluster elements (${actualLines.size})")

        File(workDirectory, "actualElements.tsv").printWriter().use { out ->
            expectedLines.forEach(out::println)
        }

        File(workDirectory, "expectedElements.tsv").printWriter().use { out ->
            actualLines.forEach(out::println)
        }
    }
}