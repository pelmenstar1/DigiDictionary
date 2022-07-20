package io.github.pelmenstar1.digiDict.ui.resolveImportConflicts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.RecordDateTimeFormatter
import io.github.pelmenstar1.digiDict.data.ConflictEntry
import io.github.pelmenstar1.digiDict.ui.MeaningTextHelper
import io.github.pelmenstar1.digiDict.utils.getLazyValue
import io.github.pelmenstar1.digiDict.utils.getLocaleCompat
import java.util.*

class ResolveImportConflictsAdapter(
    private val onItemStateChanged: (index: Int, state: Int) -> Unit
) : RecyclerView.Adapter<ResolveImportConflictsAdapter.ViewHolder>() {
    private class ConflictEntryAndIndex(val stableIndex: Int, val value: ConflictEntry)

    private class CompareBlockInfo(
        container: ViewGroup,
        @IdRes meaningId: Int,
        @IdRes notesId: Int,
        @IdRes scoreId: Int,
        @IdRes dateTimeId: Int
    ) {
        private val meaningView = container.findViewById<TextView>(meaningId)
        private val additionalNotesView = container.findViewById<TextView>(notesId)
        private val scoreView = container.findViewById<TextView>(scoreId)
        private val dateTimeView = container.findViewById<TextView>(dateTimeId)

        private val additionalNotesFormat: String
        private val scoreFormat: String

        private val locale: Locale
        private val dateTimeFormatter: RecordDateTimeFormatter

        init {
            val context = container.context
            locale = context.getLocaleCompat()

            dateTimeFormatter = RecordDateTimeFormatter(context)

            with(context.resources) {
                additionalNotesFormat = getString(R.string.additionalNotesAndValueFormat)
                scoreFormat = getString(R.string.scoreAndValueFormat)
            }
        }

        fun setContent(
            rawMeaning: String,
            additionalNotes: String,
            score: Int,
            epochSeconds: Long
        ) {
            meaningView.text = MeaningTextHelper.parseRawMeaningToFormatted(rawMeaning)
            additionalNotesView.text = String.format(locale, additionalNotesFormat, additionalNotes)
            scoreView.text = String.format(locale, scoreFormat, score)
            dateTimeView.text = dateTimeFormatter.format(epochSeconds)
        }
    }

    inner class ViewHolder(container: ViewGroup) : RecyclerView.ViewHolder(container) {
        private val expressionView = container.findViewById<TextView>(R.id.itemResolveImportConflict_expression)

        private val oldBlock = CompareBlockInfo(
            container,
            meaningId = R.id.itemResolveImportConflict_oldMeaning,
            notesId = R.id.itemResolveImportConflict_oldAdditionalNotes,
            scoreId = R.id.itemResolveImportConflict_oldScore,
            dateTimeId = R.id.itemResolveImportConflict_oldDateTime
        )

        private val newBlock = CompareBlockInfo(
            container,
            meaningId = R.id.itemResolveImportConflict_newMeaning,
            notesId = R.id.itemResolveImportConflict_newAdditionalNotes,
            scoreId = R.id.itemResolveImportConflict_newScore,
            dateTimeId = R.id.itemResolveImportConflict_newDateTime
        )

        private val acceptOldButton = container.findViewById<Button>(R.id.itemResolveImportConflict_acceptOld)
        private val acceptNewButton = container.findViewById<Button>(R.id.itemResolveImportConflict_acceptNew)
        private val mergeButton = container.findViewById<Button>(R.id.itemResolveImportConflict_merge)

        init {
            acceptOldButton.initButton { ACCEPT_OLD }
            acceptNewButton.initButton { ACCEPT_NEW }
            mergeButton.initButton { MERGE }
        }

        fun bind(entry: ConflictEntry) {
            expressionView.text = entry.expression

            oldBlock.setContent(entry.oldRawMeaning, entry.oldAdditionalNotes, entry.oldScore, entry.oldEpochSeconds)
            newBlock.setContent(entry.newRawMeaning, entry.newAdditionalNotes, entry.newScore, entry.newEpochSeconds)

            // Can't do "merge" when both old and new records have non-empty additional notes.
            val showMergeButton = entry.oldAdditionalNotes == entry.newAdditionalNotes ||
                    entry.oldAdditionalNotes.isEmpty() ||
                    entry.newAdditionalNotes.isEmpty()

            mergeButton.visibility = if(showMergeButton) View.VISIBLE else View.GONE
        }

        private inline fun Button.initButton(state: ResolveImportConflictItemState.() -> Int) {
            initButton(ResolveImportConflictItemState.state())
        }

        private fun Button.initButton(state: Int) {
            setOnClickListener {
                setItemState(bindingAdapterPosition, state)
            }
        }
    }

    private val viewItems = ArrayList<ConflictEntryAndIndex>()
    private var layoutInflaterHolder: LayoutInflater? = null

    // Should be called only once AFTER submitItems.
    fun setItemStates(states: IntArray) {
        for ((i, state) in states.withIndex()) {
            if (state != ResolveImportConflictItemState.INITIAL) {
                viewItems.removeAt(i)

                notifyItemRemoved(i)
            }
        }
    }

    fun setItemState(adapterIndex: Int, state: Int) {
        val stateIndex = viewItems[adapterIndex].stableIndex

        if (state != ResolveImportConflictItemState.INITIAL) {
            viewItems.removeAt(adapterIndex)

            notifyItemRemoved(adapterIndex)
        }

        onItemStateChanged(stateIndex, state)
    }

    fun submitItems(items: Collection<ConflictEntry>) {
        viewItems.clear()
        viewItems.ensureCapacity(items.size)

        items.forEachIndexed { index, item ->
            viewItems.add(ConflictEntryAndIndex(index, item))
        }

        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = viewItems.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = getLazyValue(
            layoutInflaterHolder,
            { LayoutInflater.from(parent.context) }
        ) { layoutInflaterHolder = it }

        val container = layoutInflater.inflate(R.layout.item_resolve_import_conflict, parent, false)
        container.layoutParams = ITEM_LAYOUT_PARAMS

        return ViewHolder(container as ViewGroup)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = viewItems[position]

        holder.bind(entry.value)
    }

    companion object {

        private val ITEM_LAYOUT_PARAMS = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}