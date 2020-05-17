package grappolo

interface ClusterEvaluator {
    fun evaluate(clusters: List<Set<Index>>, similarityMatrix: SimilarityMatrix): Evaluation
}

object SimpleClusterEvaluator : ClusterEvaluator {

    private val logger = getLogger(this)

    override fun evaluate(clusters: List<Set<Index>>, similarityMatrix: SimilarityMatrix): Evaluation {

        require(clusters.isNotEmpty() && clusters.all { it.isNotEmpty() }) {
            "Invalid empty cluster(s)"
        }

        val elements = clusters.flatten()

        val minSeparation =
            clusters
                .filter { cluster -> cluster.size > 1 }
                .flatMap { cluster ->
                    elements
                        .filterNot(cluster::contains)
                        .flatMap { i -> cluster.map { j -> 1.0 - similarityMatrix[i][j] } }
                }
                .min()!!

        val maxDiameter =
            clusters
                .filter { cluster -> cluster.size > 1 }
                .flatMap { cluster ->
                    val clusterElements = cluster.toList()
                    cluster.indices
                        .flatMap { i ->
                            cluster.indices
                                .filter { j -> i != j }
                                .map { j -> similarityMatrix[clusterElements[i]][clusterElements[j]] }
                                .filter { similarity -> similarity != 0.0 }
                                .map { similarity -> 1.0 - similarity }
                        }
                }
                .max()!!

        val dunnIndex = minSeparation / maxDiameter
        logger.debug("Dunn index: $dunnIndex. Min separation $minSeparation. Max diameter: $maxDiameter")
        return dunnIndex
    }
}

