package grappolo0

import com.wcohen.ss.*
import com.wcohen.ss.api.StringWrapper
import info.debatty.java.stringsimilarity.Cosine
import info.debatty.java.stringsimilarity.Damerau
import info.debatty.java.stringsimilarity.Jaccard
import info.debatty.java.stringsimilarity.interfaces.StringDistance
import kotlin.math.max
import kotlin.math.min
import com.wcohen.ss.api.StringDistance as SSStringDistance

interface SimilarityMetric<T> {
    fun computeSimilarity(first: T, second: T): Similarity
}

// TODO Create separate module for string distance-based clustering
open class StringDistanceSimilarityMetric(private val stringDistance: StringDistance) : SimilarityMetric<String> {

    override fun computeSimilarity(first: String, second: String): Similarity =
            1.0 - (stringDistance.distance(first, second) / max(first.length, second.length))
}

object DamerauSimilarityMetric : StringDistanceSimilarityMetric(Damerau())
object CosineSimilarityMetric : StringDistanceSimilarityMetric(Cosine())
object JaccardSimilarityMetric : StringDistanceSimilarityMetric(Jaccard())

open class SSSimilarityMetric(private val stringDistance: SSStringDistance, documents: Iterable<String>) : SimilarityMetric<String> {
    override fun computeSimilarity(first: String, second: String): Similarity =
            min(stringDistance.score(first, second), 1.0)

    init {
        if (stringDistance is AbstractStatisticalTokenDistance) {
            for (document in documents) {
                stringDistance.train(BasicStringWrapperIterator(documents.map(::BasicStringWrapper).iterator()))
            }
        }
    }
}

class SoftSSTFIDF(tokenDistance:SSStringDistance,
                  tokenMatchThreshold: Double,
                  documents: Iterable<String>) : SoftTFIDF(tokenDistance, tokenMatchThreshold), SimilarityMetric<String> {

    init {
        for (document in documents) {
            train(BasicStringWrapperIterator(documents.map(::BasicStringWrapper).iterator()))
        }
    }

    override fun computeSimilarity(first: String, second: String): grappolo0.Similarity =
            min(score(first, second), 1.0)
}

object DamerauSSStringDistance : AbstractStringDistance() {

    override fun score(s: StringWrapper?, t: StringWrapper?): Double =
            DamerauSimilarityMetric.computeSimilarity(s!!.unwrap()!!, t!!.unwrap()!!)

    override fun explainScore(s: StringWrapper?, t: StringWrapper?): String? = null
}
