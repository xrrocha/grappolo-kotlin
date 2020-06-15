package grappolo

interface ClusterEvaluator {

    fun evaluate(clusters: List<Set<Index>>, similarityMatrix: SimilarityMatrix): Evaluation
}

object SimpleClusterEvaluator : ClusterEvaluator {

    override fun evaluate(clusters: List<Set<Index>>, similarityMatrix: SimilarityMatrix): Evaluation {

        val intraSimilarities = clusters.map { cluster ->

            if (cluster.size == 1) {
                0.0
            } else {

                val elements = cluster.toList()
                elements.indices.flatMap { i ->
                    (i + 1 until elements.size).map { j -> similarityMatrix[elements[i]][elements[j]] }
                }
                        .average()
            }
        }

        return intraSimilarities.average()
    }
}

object DunnIndexClusterEvaluator : ClusterEvaluator {

    private val logger = getLogger(this)

    override fun evaluate(clusters: List<Set<Index>>, similarityMatrix: SimilarityMatrix): Evaluation {

        require(clusters.isNotEmpty() && clusters.all { it.isNotEmpty() }) {
            "Invalid empty cluster(s)"
        }

        val (maxInterClusterSimilarity, minIntraClusterSimilarity) =
                clusters.fold(Pair(-Double.MAX_VALUE, Double.MAX_VALUE)) { accum, cluster ->

                    val (maxInterClusterSimilaritySoFar, minIntraClusterSimilaritySoFar) = accum

                    val maxInterClusterSimilarity =
                            cluster.map { i ->
                                similarityMatrix[i].scores
                                        .filterKeys { j -> !cluster.contains(j) }
                                        .map { entry -> entry.value }
                                        .filter { similarity -> similarity != 0.0 } // Shouldn't happen
                                        .average()
                            }
                                    .max()!!

                    val similarities =
                            cluster.flatMap { i ->
                                cluster
                                        .filter { j -> i != j }
                                        .map { j -> similarityMatrix[i][j] }
                                        .filter { similarity -> similarity != 0.0 }
                            }
                    val minIntraClusterSimilarity = if (similarities.isEmpty()) {
                        minIntraClusterSimilaritySoFar
                    } else {
                        1.0 - (similarities.max()!! - similarities.min()!!)
                    }

                    val nextMaxInterClusterSimilarity =
                            if (maxInterClusterSimilarity > maxInterClusterSimilaritySoFar) {
                                maxInterClusterSimilarity
                            } else {
                                maxInterClusterSimilaritySoFar
                            }

                    val nextMinIntraClusterSimilarity =
                            if (minIntraClusterSimilarity < minIntraClusterSimilaritySoFar) {
                                minIntraClusterSimilarity
                            } else {
                                minIntraClusterSimilaritySoFar
                            }

                    Pair(nextMaxInterClusterSimilarity, nextMinIntraClusterSimilarity)
                }

        val dunnIndex = minIntraClusterSimilarity / maxInterClusterSimilarity
        logger.debug("Dunn index: $dunnIndex, maxInterClusterSimilarity: $maxInterClusterSimilarity, minIntraClusterSimilarity: $minIntraClusterSimilarity")
        return dunnIndex
    }
}
