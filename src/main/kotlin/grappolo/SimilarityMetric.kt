package grappolo

import info.debatty.java.stringsimilarity.Cosine
import info.debatty.java.stringsimilarity.Damerau
import info.debatty.java.stringsimilarity.Jaccard
import info.debatty.java.stringsimilarity.interfaces.StringDistance
import kotlin.math.max

interface SimilarityMetric<T> {
    fun computeSimilarity(first: T, second: T): Similarity
}

// TODO Create separate module for string distance-based clustering
open class StringDistanceSimilarityMetric(private val stringDistance: StringDistance) : SimilarityMetric<String> {

    override fun computeSimilarity(first: String, second: String): Similarity =
            1.0 - (stringDistance.distance(first, second) / max(first.length, second.length))
}

class DamerauSimilarityMetric() : StringDistanceSimilarityMetric(Damerau())
class CosineSimilarityMetric() : StringDistanceSimilarityMetric(Cosine())
class JaccardSimilarityMetric() : StringDistanceSimilarityMetric(Jaccard())
