package grappolo

interface ClusterComparator : Comparator<Cluster>

object GreedyClusterComparator : ClusterComparator {

    override fun compare(cluster1: Cluster, cluster2: Cluster): Int {

        if (cluster2.elements.size != cluster1.elements.size) {
            return cluster2.elements.size - cluster1.elements.size
        }

        if (cluster2.intraSimilarity != cluster1.intraSimilarity) {
            return (cluster2.intraSimilarity - cluster1.intraSimilarity).toInt()
        }

        if (cluster2.centroids.size != cluster1.centroids.size) {
            return cluster2.centroids.size - cluster1.centroids.size
        }

        if (cluster2.centroidWeight != cluster1.centroidWeight) {
            return (cluster2.centroidWeight - cluster1.centroidWeight).toInt()
        }

        val different =
                cluster2
                        .elements.zip(cluster1.elements)
                        .find { (e2, e1) -> e2 != e1 }
        if (different != null) {
            return different.second - different.first
        }

        return 0
    }
}
