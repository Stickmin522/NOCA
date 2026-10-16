package app.noca

data class ObservedTransaction(val record: Transaction, val readAt: Long, val position: Int)

fun restoreHistoryAmounts(history: List<ObservedTransaction>): List<Transaction> {
    val observations = history.associateBy { it.readAt to it.position }
    return history.map { observation ->
        val record = observation.record
        val older = observations[observation.readAt to observation.position + 1]?.record
        val adjacent =
            older != null &&
                ((record.position == observation.position &&
                    older.position == observation.position + 1) ||
                    ((record.sequence - older.sequence) and 0xffff) == 1)
        if (record.delta == null && adjacent) record.copy(delta = record.balance - older!!.balance)
        else record
    }
}
