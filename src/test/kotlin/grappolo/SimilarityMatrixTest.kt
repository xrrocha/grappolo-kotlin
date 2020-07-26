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

    private val source = ListSource(NameFixture.elements)
    private val context = ClusteringConfiguration(
            source = source,
            minSimilarity = 0.0,
            similarityMetric = DamerauSimilarityMetric,
            pairGenerator = CartesianPairGenerator(NameFixture.elements.size),
            clusterer = GrappoloClusterer,
            clusterEvaluator = SimpleClusterEvaluator
//            mapOf(
//                    "source" to source,
//                    "minSimilarity" to 0.0,
//                    "similarityMetric" to StringDistanceSimilarityMetric(source, Damerau()),
//                    "pairGenerator" to CartesianPairGenerator(NameFixture.elements.size),
//                    "clusterer" to GrappoloClusterer,
//                    "clusterEvaluator" to SimpleClusterEvaluator
//            )
    )

    @Test
    fun `Builds similarity matrix`() {

        assert(context.similarityMatrix.size == context.elements.size)

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
