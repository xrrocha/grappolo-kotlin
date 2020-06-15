package grappolo

import info.debatty.java.stringsimilarity.Cosine
import info.debatty.java.stringsimilarity.Damerau
import info.debatty.java.stringsimilarity.interfaces.StringDistance
import kotlin.math.max

interface SimilarityMetric {
    fun computeSimilarity(i: Index, j: Index): Similarity
}

// TODO Create separate module for string distance-based clustering
open class StringDistanceSimilarityMetric(
    private val elements: List<String>,
    private val stringDistance: StringDistance
) : SimilarityMetric {

    override fun computeSimilarity(i: Index, j: Index): Similarity {
        val s1 = elements[i]
        val s2 = elements[j]
        return 1.0 - (stringDistance.distance(s1, s2) / max(s1.length, s2.length))
    }
}

class DamerauSimilarityMetric(elements: List<String>) : StringDistanceSimilarityMetric(elements, Damerau())
class CosineSimilarityMetric(elements: List<String>) : StringDistanceSimilarityMetric(elements, Cosine())
