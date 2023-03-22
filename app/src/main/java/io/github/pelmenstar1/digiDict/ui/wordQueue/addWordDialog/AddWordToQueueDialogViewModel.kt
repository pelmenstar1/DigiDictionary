package io.github.pelmenstar1.digiDict.ui.wordQueue.addWordDialog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.ValidityFlow
import io.github.pelmenstar1.digiDict.common.android.viewModelAction
import io.github.pelmenstar1.digiDict.common.containsLetterOrDigit
import io.github.pelmenstar1.digiDict.data.RecordDao
import io.github.pelmenstar1.digiDict.data.WordQueueDao
import io.github.pelmenstar1.digiDict.data.WordQueueEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddWordToQueueDialogViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val queueDao: WordQueueDao,
    private val recordDao: RecordDao
) : ViewModel() {
    private var isCheckWordJobStarted = false

    private val _wordErrorFlow = MutableStateFlow<AddWordToQueueDialogError?>(null)
    val wordErrorFlow: StateFlow<AddWordToQueueDialogError?>
        get() = _wordErrorFlow

    val wordFlow: StateFlow<String> = savedStateHandle.getStateFlow(KEY_WORD, initialValue = "")

    val validity = ValidityFlow(validityScheme)

    var word: String
        get() = wordFlow.value
        set(value) {
            validity.mutate {
                set(wordValidityField, false, isComputed = false)
            }

            savedStateHandle[KEY_WORD] = value
        }

    var cachedWordEntries: Array<WordQueueEntry>? = null
        set(value) {
            field = value

            startCheckWordJobIfNecessary()
        }

    val addAction = viewModelAction(TAG) {
        queueDao.insert(WordQueueEntry(id = 0, word = word.trim()))
    }

    private fun startCheckWordJobIfNecessary() {
        if (!isCheckWordJobStarted) {
            isCheckWordJobStarted = true

            viewModelScope.launch(Dispatchers.Default) {
                // Either allWords or allWordEntries should be non-null
                var allWords: Array<String>? = null
                val allWordEntries = cachedWordEntries

                if (allWordEntries == null) {
                    allWords = queueDao.getAllWords()
                }

                val recordsExpressions = recordDao.getAllExpressions().also {
                    // Sort to make binary search work.
                    it.sort()
                }

                wordFlow.collect { word ->
                    val trimmedWord = word.trim()

                    val error = when {
                        word.isEmpty() -> AddWordToQueueDialogError.EMPTY_TEXT
                        !word.containsLetterOrDigit() -> AddWordToQueueDialogError.WORD_NO_LETTER_OR_DIGIT

                        // If allWords is non-null, checks whether the array contains trimmedWord
                        // Otherwise, checks whether allWordEntries contains trimmedWord.
                        allWords?.contains(trimmedWord) ?: allWordEntries!!.any { it.word == trimmedWord } ->
                            AddWordToQueueDialogError.WORD_EXISTS
                        recordsExpressions.binarySearch(trimmedWord) >= 0 -> AddWordToQueueDialogError.RECORD_EXPRESSION_EXISTS
                        else -> null
                    }

                    validity.mutate {
                        set(wordValidityField, error == null)
                    }

                    _wordErrorFlow.value = error
                }
            }
        }
    }

    fun addEntry() {
        addAction.runWhenValid(validity)
    }

    companion object {
        private const val TAG = "AddWordToQueueDialogViewModel"

        private const val KEY_WORD = "io.github.pelmenstar1.digiDict.AddWordToQueueDialogViewModel.word"

        val wordValidityField = ValidityFlow.Field(ordinal = 0)
        private val validityScheme = ValidityFlow.Scheme(wordValidityField)
    }
}