package grappolo0

import java.io.File
import java.util.*

interface Source<T> {
    val elements: List<T>
}

data class ListSource<T>(override val elements: List<T>) : Source<T>

open class FileSource<T>(open val fileName: String,
                         open val linesToDrop: Int = 0,
                         open val elementsToLoad: Int = Int.MAX_VALUE,
                         open val transform: (String) -> T?) : Source<T> {
    override val elements: List<T> by lazy {

        val reader = File(fileName).bufferedReader()
        generateSequence {
            reader.readLine()
        }
                .drop(linesToDrop)
                .map(transform)
                .filter(Objects::nonNull)
                .take(elementsToLoad)
                .map { it!! }
                .toList()
    }
}

data class StringFileSource(override val fileName: String,
                            override val linesToDrop: Int = 0,
                            override val elementsToLoad: Int = Int.MAX_VALUE) :
        FileSource<String>(fileName, linesToDrop, elementsToLoad, { it })

