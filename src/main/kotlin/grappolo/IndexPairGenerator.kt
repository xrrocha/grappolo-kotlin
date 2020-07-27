package grappolo

interface IndexPairGenerator {
    fun pairs(): Sequence<Pair<Int, Int>>
}

class CartesianPairGenerator(private val size: Int) : IndexPairGenerator {
    override fun pairs(): Sequence<Pair<Int, Int>> = sequence {
        for (i in (0 until size))
            for (j in (i + 1 until size))
                yield(Pair(i, j))
    }
}
