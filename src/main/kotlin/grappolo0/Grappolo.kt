package grappolo0

import java.io.File

fun main(args: Array<String>) {

    val logger = getLogger("grappolo.Main")

    val configFileName = if (args.isEmpty()) "grappolo.kts" else args[0]
    val configFile = File(configFileName)
    require(configFile.isFile && configFile.canRead()) {
        "Can't access configuration script: ${configFile.absolutePath}"
    }

    logger.debug("Running script: ${configFile.absolutePath}")
    val context = ClusteringConfiguration.load<String>(configFile)
    logger.debug("ClusterContext loaded from script")

    val (evaluation, millis) = time { context.bestClustering }
    logger.debug("${format(evaluation.clusters.size)} clusters found in ${format(context.elements.size)} elements.}")
    logger.debug("Elapsed time: ${format(millis)}")

    val workDirectory = configFile.parentFile
    context.dump(workDirectory)

    val expectedLines = context.elements.sorted()
    val actualLines = evaluation.clusters.map { it.toList() }.flatten().map { context.elements[it] }.sorted()
    if (actualLines != expectedLines) {

        logger.error("Mismatch between expected (${expectedLines.size}) and cluster (${actualLines.size}) counts")

        File(workDirectory, "actualElements.tsv").printWriter().use { out ->
            expectedLines.forEach(out::println)
        }

        File(workDirectory, "expectedElements.tsv").printWriter().use { out ->
            actualLines.forEach(out::println)
        }
    }
}
