package grappolo

import java.io.File

fun Double.fmt(digits: Int = 2) = "%.${digits}f".format(this)

fun <T> time(action: () -> T): Pair<T, Long> {
    val start = System.currentTimeMillis()
    val result = action()
    return result to (System.currentTimeMillis() - start)
}

fun File.printDelimited(separator: String, titles: List<String>, data: Sequence<List<Any?>>) {

    this.printWriter().use { out ->

        out.println(titles.joinToString(separator))

        data.withIndex().forEach { (index, fields) ->

            require(fields.size == titles.size) {
                "Error in line ${index + 1}: expected ${titles.size} fields, got ${fields.size}"
            }

            val fieldValues = fields.map { it?.toString() ?: "" }
            out.println(fieldValues.joinToString(separator))
        }
    }
}


