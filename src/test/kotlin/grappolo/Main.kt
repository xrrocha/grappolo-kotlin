package grappolo

import java.io.File

fun main() {

    val logger = getLogger("grappolo.Main")

    val minSimilarity = 0.7

    val linesToSkip = 0
    val linesToRead = Int.MAX_VALUE

    val ngramSize = 2

    val similarityMetric: (List<String>) -> SimilarityMetric = { lines ->
        DamerauSimilarityMetric(lines)
    }
    val pairGenerator: (List<String>, Int) -> PairGenerator = { lines, nGramLength ->
        NGramPairGenerator(lines, nGramLength)
    }
    val clusterer = GrappoloClusterer
    val clusterEvaluator = SimpleClusterEvaluator

    val dataFileName = "data.tsv"
    val dataDirectoryName = "surnames"
    val baseDirectoryName = "data"

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

    val lines = readLines(dataFile, linesToRead, linesToSkip)
    logger.debug("Loaded ${lines.size} lines from $dataFile")

    val context = ClusteringContext(
        lines, minSimilarity,
        similarityMetric(lines), pairGenerator(lines, ngramSize),
        clusterer, clusterEvaluator
    )

    val (evaluation, millis) = time { context.bestClustering }
    logger.debug("${format(evaluation.clusters.size)} clusters found in ${format(lines.size)} elements.}")
    logger.debug("Elapsed time: ${format(millis)}")

    context.dump(workDirectory)
}