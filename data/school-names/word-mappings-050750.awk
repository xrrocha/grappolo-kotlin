BEGIN {
    FS = OFS = "\t"

    while ((getline < "word-mappings-050750.tsv") > 0) {
        if (NF == 2) {
            mappings[$1] = $2
        }
    }

    FS = OFS = " "
}

{
    for (i = 1; i <= NF; i++) {
        if ($i in mappings) printf("%s ", mappings[$i])
        else printf("%s ", $i)
    }
    printf("\n")
}

