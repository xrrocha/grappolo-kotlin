import grappolo.*

val separator = """\s+""".toRegex()
val fileName = "data/school-names/school-names-050750.tsv"
val stopWords =  emptySet<String>() // setOf("ESCUELA", "COLEGIO", "JARDIN", "TECNICO", "INSTITUTO", "LICEO", "ACADEMIA")
val lines = java.io.File(fileName)
        .readLines()
        .map { line ->
            line
                    .trim()
                    .split(separator)
                    .map { it.trim() }
                    .filterNot { it.isEmpty() || stopWords.contains(it) }
                    .joinToString(" ")
        }
        .distinct()

ClusteringConfiguration(
        minSimilarity = 0.0,
        source = ListSource(lines),
        similarityMetric = SoftSSTFIDF(com.wcohen.ss.Jaro(), 0.9, lines), // SSSimilarityMetric(com.wcohen.ss.TFIDF(), lines),
        pairGenerator = CartesianPairGenerator(lines.size),
        clusterer = GrappoloClusterer,
        clusterEvaluator = SimpleClusterEvaluator
)

