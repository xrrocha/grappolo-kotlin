package grappolo0

import org.slf4j.Logger
import org.slf4j.LoggerFactory
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
