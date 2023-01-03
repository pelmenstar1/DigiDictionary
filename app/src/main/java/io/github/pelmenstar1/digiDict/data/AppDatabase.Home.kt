package io.github.pelmenstar1.digiDict.data

private fun HomeSortType.getRawSortString(): String {
    val rowName = when (this) {
        HomeSortType.NEWEST, HomeSortType.OLDEST -> RecordTable.epochSeconds
        HomeSortType.ALPHABETIC_BY_EXPRESSION, HomeSortType.ALPHABETIC_BY_EXPRESSION_INVERSE -> RecordTable.expression
        HomeSortType.GREATEST_SCORE, HomeSortType.LEAST_SCORE -> RecordTable.score
    }

    val orderType = when (this) {
        HomeSortType.NEWEST, HomeSortType.GREATEST_SCORE, HomeSortType.ALPHABETIC_BY_EXPRESSION_INVERSE -> "DESC"
        else -> "ASC"
    }

    return "$rowName $orderType"
}


fun AppDatabase.getConciseRecordsWithBadgesForHome(
    limit: Int,
    offset: Int,
    sortType: HomeSortType
): Array<ConciseRecordWithBadges> {
    val rawSortStr = sortType.getRawSortString()
    return queryConciseRecordsWithBadges(
        sql = "SELECT id, expression, meaning, score, dateTime FROM records ORDER BY $rawSortStr LIMIT $limit OFFSET $offset",
        progressReporter = null
    )
}