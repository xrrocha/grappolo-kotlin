package grappolo

import java.io.File

interface PairGenerator {
    fun pairs(): Sequence<Pair<Index, Index>>
}

class CartesianPairGenerator(private val size: Index) : PairGenerator {

    override fun pairs(): Sequence<Pair<Index, Index>> = sequence {
        for (i in (0 until size))
            for (j in (i + 1 until size))
                yield(Pair(i, j))
    }
}

class FilePairGenerator(filename: String, separator: String = "\\s+") : PairGenerator {

    private val separatorRegex = separator.toRegex()

    private val reader = File(filename).bufferedReader()

    companion object {
        val IntRegex = "[\\p{Digit}]+".toRegex()
    }

    override fun pairs(): Sequence<Pair<Index, Index>> =
            generateSequence { reader.readLine() }
                    .map { line ->
                        val (firstIndex, secondIndex) =
                                line.split(separatorRegex)
                                        .map(String::trim)
                                        .filter(IntRegex::matches)
                                        .take(2)
                                        .map(String::toInt)
                        Pair(firstIndex, secondIndex)
                    }
}

//class NGramPairGenerator(private val strings: List<String>, private val ngramLength: Int = 2) : PairGenerator {
//
//    companion object {
//        private val logger = getLogger(this)
//    }
//
//    override fun pairs(): Iterator<Pair<Index, Index>> {
//
//        val ngramMap = mutableMapOf<String, MutableSet<Int>>()
//        strings.forEachIndexed { stringIndex, string ->
//            val lastPosition = string.length - ngramLength + 1
//            (0 until lastPosition).forEach { startIndex ->
//                val endIndex = startIndex + ngramLength
//                val ngram = string.take(endIndex).drop(startIndex)
//                val indexSet = ngramMap.getOrPut(ngram) { mutableSetOf<Int>() }
//                indexSet += stringIndex
//            }
//        }
//        logger.debug("${ngramMap.size} n-grams found with length $ngramLength")
//
//        // TODO Make index pair generation lazy!
//        val indexPairs: MutableSet<Pair<Int, Int>> = mutableSetOf()
//        ngramMap.forEach { entry ->
//            val stringIndices = entry.value.toList().sorted()
//            for (i in stringIndices.indices) {
//                for (j in (i + 1) until stringIndices.size) {
//                    indexPairs += Pair(stringIndices[i], stringIndices[j])
//                }
//            }
//        }
//        logger.debug("${indexPairs.size} pairs found")
//
//        return indexPairs.iterator()
//    }
//}

