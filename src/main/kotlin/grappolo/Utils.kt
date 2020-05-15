package grappolo

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.File
import java.text.DecimalFormat

fun <T> time(action: () -> T): Pair<T, Long> {
    val start = System.currentTimeMillis()
    val result = action()
    return Pair(result, System.currentTimeMillis() - start)
}

private val NUMBER_FORMAT = DecimalFormat("###,###,###")
fun format(value: Number) = NUMBER_FORMAT.format(value)!!

fun getLogger(target: Any): Logger {
    val className = target::class.java.name
    val loggerName =
        if (className.endsWith("\$Companion")) className.substring(0, className.length - 10)
        else className
    return LoggerFactory.getLogger(loggerName)!!
}

fun readLines(directory: File, fileName: String, linesToRead: Int = Int.MAX_VALUE, linesToSkip: Int = 0): List<String> =
    readLines(File(directory, fileName), linesToRead, linesToSkip)

fun readLines(file: File, linesToRead: Int = Int.MAX_VALUE, linesToSkip: Int = 0): List<String> =
    readLines(file.bufferedReader(), linesToRead, linesToSkip)

fun readLines(reader: BufferedReader, linesToRead: Int = Int.MAX_VALUE, linesToSkip: Int = 0): List<String> =
    reader
        .readLines()
        .drop(linesToSkip)
        .take(linesToRead)
