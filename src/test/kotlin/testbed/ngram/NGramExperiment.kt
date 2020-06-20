package testbed.ngram

import java.io.File
import java.time.Instant

fun main() {

    val count = Int.MAX_VALUE // 10_000
    val ngramLength = 3
    val minPairCount = 2
    val separator = "\\s+".toRegex()
    val wordPattern = "[\\p{Alpha}][\\p{Alnum}]*".toRegex()

    println("Start: ${Instant.now()}")

    val words =
            File("data/school-names/school-name-words.tsv")
                    .readLines()
                    .asSequence()
                    .map { it.split(separator).first() }
                    .filter { word -> wordPattern.matches(word) }
                    .take(count)
                    .toList()

    val result =
            words.withIndex()
                    .flatMap { (index, word) ->
                        (0 until word.length - ngramLength + 1)
                                .map { pos ->
                                    val ngram = word.substring(pos, pos + ngramLength)
                                    ngram to index
                                }
                    }
                    .groupBy { (ngram, _) -> ngram }
                    .mapValues { entry ->
                        entry.value.map { it.second }.distinct().sorted()
                    }
                    .flatMap { entry ->
                        val elements = entry.value
                        elements.indices.flatMap { i ->
                            (i + 1 until elements.size).map { j ->
                                Pair(elements[i], elements[j])
                            }
                        }
                    }
                    .groupBy { it }
                    .mapValues { entry -> entry.value.size }
                    .filter { entry ->
                        entry.value >= minPairCount
                    }
                    .keys
                    .toList()

    File("data/school-names/word-pairs.tsv").printWriter().use { out ->
        result.forEach { (i, j) ->
            out.println("${words[i]}\t${words[j]}")
        }
    }

    println("End: ${Instant.now()}")

    println("result.size: ${result.size}")
}