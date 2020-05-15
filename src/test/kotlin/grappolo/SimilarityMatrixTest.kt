package grappolo

import org.junit.Test
import java.io.File

class SimilarityMatrixTest {

    /*
        "alejandro", "alejandor", "alexandro",
        "marlene", "marleny", "malrene",
        "marta", "martha", "mrata",
        "jorge", "jorje",
        "ricardo"
    */

    private val dataDirectory = File("data").also {
        require(it.isDirectory) { "Can't access data directory: ${it.absolutePath}" }
    }

    private val names = readLines(dataDirectory, "names/data.tsv")
    val expectedNameClusters = setOf(
        setOf("alejandro", "alejandor", "alexandro"),
        setOf("marlene", "marleny", "malrene"),
        setOf("marta", "martha", "mrata"),
        setOf("jorge", "jorje"),
        setOf("ricardo")
    )

    private val context = ClusteringContext(
        elements = names,
        minSimilarity = 0.0,
        similarityMetric = DamerauSimilarityMetric(names),
        pairGenerator = CartesianPairGenerator(names.size),
        clusterer = GrappoloClusterer,
        clusterEvaluator = SimpleClusterEvaluator
    )

    @Test
    fun `Builds similarity matrix`() {

        val nameDataDirectory = File(dataDirectory, "names")

        assert(context.similarityMatrix.size == context.size)

        val expectedScores =
            readLines(nameDataDirectory, "scores.tsv")
                .map { it.split("\\t".toRegex()) }
                .map { Triple(it[0].toInt(), it[1].toInt(), it[2].toDouble()) }
        assert(expectedScores.all { (i, j, similarity) ->
            context.similarityMatrix[i][j] == similarity
        })
        assert(expectedScores.size == context.similarityMatrix.rows.map { it.scores.size }.sum())

        val expectedSimilarities =
            readLines(nameDataDirectory, "similarities.tsv").map { it.toDouble() }
        assert(context.similarityMatrix.similarityValues == expectedSimilarities)
    }

    @Test
    fun `Builds clusters given minimum similarity`() {

        val minSimilarity = 0.6666666666666667
        val actualClusters = context.clusterer
            .cluster(context.similarityMatrix, minSimilarity)
            .map { cluster -> cluster.map { names[it] }.toSet() }
            .toSet()

        assert(actualClusters == expectedNameClusters)

        assert(names.toSet() == actualClusters.flatten().toSet())
    }

    @Test
    fun `Builds best clusters`() {

        val actualClusters =
            context.bestClustering.clusters
                .map { cluster -> cluster.map { names[it] }.toSet() }
                .toSet()

        assert(actualClusters == expectedNameClusters)

        assert(names.toSet() == actualClusters.flatten().toSet())
    }


    @Test
    fun `Builds surnames clusters correctly`() {

        val surnameDataDirectory = File(dataDirectory, "surnames")

        val dataFile = File(surnameDataDirectory, "data.tsv")
        require(dataFile.isFile) {
            "Can't read data file: ${dataFile.absolutePath}"
        }

        val surnameCount = Int.MAX_VALUE
        val lines = readLines(dataFile, linesToRead = surnameCount)
        logger.debug("Loaded ${lines.size} lines from ${dataFile}")

        val context = ClusteringContext(
            elements = lines,
            minSimilarity = 0.7,
            similarityMetric = DamerauSimilarityMetric(lines),
            pairGenerator = CartesianPairGenerator(lines.size),
            clusterer = GrappoloClusterer,
            clusterEvaluator = SimpleClusterEvaluator
        )

        val (evaluation, millis) = time { context.bestClustering }
        logger.debug(
            "${format(evaluation.clusters.size)} clusters found in ${format(lines.size)} elements. Elapsed time: ${format(millis)}"
        )

        context.dump(surnameDataDirectory)
    }

    companion object {
        private val logger = getLogger(this)
    }
}
