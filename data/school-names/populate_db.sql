SET SCHEMA 'school_names';

DROP TABLE IF EXISTS word_pairs;
DROP TABLE IF EXISTS ngram_words;
DROP TABLE IF EXISTS words;

CREATE TABLE words
(
    word     TEXT   NOT NULL,
    position SERIAL NOT NULL
);

\COPY words(word) FROM 'c:/local/ricardo/workspace/grappolo/grappolo-kotlin/data/school-names/words.tsv';

CREATE UNIQUE INDEX wr_word ON words (word);

CREATE OR REPLACE FUNCTION ngrams(varchar, integer) RETURNS SETOF TEXT AS
$$
SELECT SUBSTRING($1 FROM n FOR $2)::TEXT
FROM GENERATE_SERIES(1, LENGTH($1) - ($2 - 1), 1) n;
$$ LANGUAGE SQL;

CREATE TABLE ngram_words AS
SELECT DISTINCT ngrams(word, 3) AS ngram,
                word            AS word,
                position        AS position
FROM words;

CREATE UNIQUE INDEX nw_ngram_word ON ngram_words (ngram, word);

CREATE EXTENSION fuzzystrmatch;

CREATE OR REPLACE FUNCTION normalized_levenshtein(first varchar, second varchar) RETURNS NUMERIC AS
$$
DECLARE
    levenshtein_value NUMERIC := levenshtein(first, second);
    max_len INTEGER := GREATEST(LENGTH(first), LENGTH(second));
BEGIN
    RETURN 1.0 - levenshtein_value / max_len;
END;
$$
LANGUAGE 'plpgsql' IMMUTABLE;

SET temp_file_limit TO -1;

CREATE TABLE word_pairs AS
SELECT  *,
        normalized_levenshtein(t.first_word, t.second_word) AS similarity
FROM    (
            SELECT first.position  - 1 AS first_position,
                   first.word          AS first_word,
                   second.position - 1 AS second_position,
                   second.word         AS second_word
            FROM ngram_words AS first
                     JOIN ngram_words AS second
                          ON second.ngram = first.ngram AND second.word > first.word
            GROUP BY first.position, first_word, second.position, second_word
            HAVING COUNT(*) >= 2
            ORDER BY first_word, second_word
        )   AS t;
