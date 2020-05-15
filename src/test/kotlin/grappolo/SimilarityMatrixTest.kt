package grappolo

import org.junit.Test
import java.io.File

class SimilarityMatrixTest {

    companion object {
        private val logger = getLogger(this)
    }

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
}
