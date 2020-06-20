package grappolo

import java.io.File

interface PairGenerator {
    fun pairs(): Iterator<Pair<Index, Index>>
}

class CartesianPairGenerator(private val size: Index) : PairGenerator {
    override fun pairs(): Iterator<Pair<Index, Index>> =
            (0 until size).flatMap { i ->
                (i + 1 until size).map { j ->
                    Pair(i, j)
                }
            }
                    .iterator()
}

class FilePairGenerator(filename: String, separator: String = "\\s+") : PairGenerator {

    val separatorRegex = separator.toRegex()

    val reader = File(filename).bufferedReader()

    companion object {
        val IntRegex = "[\\p{Digit}]+".toRegex()
    }

    override fun pairs(): Iterator<Pair<Index, Index>> {

        return object : Iterator<Pair<Int, Int>> {

            private var currentLine = reader.readLine()

            override fun hasNext(): Boolean = currentLine != null

            override fun next(): Pair<Index, Index> {

                val (firstIndex, secondIndex) =
                        currentLine
                                .split(separatorRegex)
                                .map(String::trim)
                                .filter(IntRegex::matches)
                                .take(2)
                                .map(String::toInt)
                val nextPair = Pair(firstIndex, secondIndex)

                currentLine = reader.readLine()

                return nextPair
            }
        }
    }
}

class NGramPairGenerator(private val strings: List<String>, private val ngramLength: Int = 2) : PairGenerator {

    companion object {
        private val logger = getLogger(this)
    }

    override fun pairs(): Iterator<Pair<Index, Index>> {

        val ngramMap = mutableMapOf<String, MutableSet<Int>>()
        strings.forEachIndexed { stringIndex, string ->
            val lastPosition = string.length - ngramLength + 1
            (0 until lastPosition).forEach { startIndex ->
                val endIndex = startIndex + ngramLength
                val ngram = string.take(endIndex).drop(startIndex)
                val indexSet = ngramMap.getOrPut(ngram) { mutableSetOf<Int>() }
                indexSet += stringIndex
            }
        }
        logger.debug("${ngramMap.size} n-grams found with length $ngramLength")

        // TODO Make index pair generation lazy!
        val indexPairs: MutableSet<Pair<Int, Int>> = mutableSetOf()
        ngramMap.forEach { entry ->
            val stringIndices = entry.value.toList().sorted()
            for (i in stringIndices.indices) {
                for (j in (i + 1) until stringIndices.size) {
                    indexPairs += Pair(stringIndices[i], stringIndices[j])
                }
            }
        }
        logger.debug("${indexPairs.size} pairs found")

        return indexPairs.iterator()
    }
}

