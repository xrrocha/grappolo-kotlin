package grappolo

import org.junit.Test

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

    private val context = ClusteringContext(
        elements = NameFixture.elements,
        minSimilarity = 0.0,
        similarityMetric = DamerauSimilarityMetric(NameFixture.elements),
        pairGenerator = CartesianPairGenerator(NameFixture.elements.size),
        clusterer = GrappoloClusterer,
        clusterEvaluator = SimpleClusterEvaluator
    )

    @Test
    fun `Builds similarity matrix`() {

        assert(context.similarityMatrix.size == context.size)

        assert(NameFixture.expectedScores.all { (i, j, similarity) ->
            context.similarityMatrix.similarityMap[context.similarityMatrix[i][j]] == similarity
        })
        assert(NameFixture.expectedScores.size == context.similarityMatrix.rows.map { it.scores.size }.sum())

        assert(context.similarityMatrix.similarityMap.values.sorted() == NameFixture.expectedSimilarities)
    }

    @Test
    fun `Builds clusters given minimum similarity`() {

        val minSimilarity = 0.6666666666666667
        val actualClusters = context.clusterer
            .cluster(context.similarityMatrix, minSimilarity)
            .map { cluster -> cluster.map { NameFixture.elements[it] }.toSet() }
            .toSet()

        assert(actualClusters == NameFixture.expectedClusters)

        assert(NameFixture.elements.toSet() == actualClusters.flatten().toSet())
    }

    @Test
    fun `Builds best clusters`() {

        val actualClusters =
            context.bestClustering.clusters
                .map { cluster -> cluster.map { NameFixture.elements[it] }.toSet() }
                .toSet()

        println(actualClusters)
        assert(actualClusters == NameFixture.expectedClusters)

        assert(NameFixture.elements.toSet() == actualClusters.flatten().toSet())
    }
}
