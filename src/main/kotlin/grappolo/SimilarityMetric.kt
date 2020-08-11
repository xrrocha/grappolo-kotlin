package grappolo

import info.debatty.java.stringsimilarity.Damerau
import info.debatty.java.stringsimilarity.interfaces.MetricStringDistance
import kotlin.math.max

interface SimilarityMetric {
    fun measureSimilarity(index1: Int, index2: Int): Double
}

open class DebattySimilarityMetric(private val stringDistance: MetricStringDistance,
                                   private val indexToString: (Int) -> String = Int::toString)
    : SimilarityMetric {
    override fun measureSimilarity(index1: Int, index2: Int): Double {
        val string1 = indexToString(index1)
        val string2 = indexToString(index2)
        return 1.0 - (stringDistance.distance(string1, string2) / max(string1.length, string2.length))
    }
}

class DamerauSimilarityMetric(indexToString: (Int) -> String = Int::toString):
        DebattySimilarityMetric(Damerau(), indexToString), Named {

    override val name = "damerau"
}

