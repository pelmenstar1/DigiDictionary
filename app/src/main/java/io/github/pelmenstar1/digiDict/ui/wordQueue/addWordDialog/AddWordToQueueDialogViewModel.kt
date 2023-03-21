package io.github.pelmenstar1.digiDict.ui.wordQueue.addWordDialog

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.ValidityFlow
import io.github.pelmenstar1.digiDict.common.android.viewModelAction
import io.github.pelmenstar1.digiDict.data.WordQueueDao
import io.github.pelmenstar1.digiDict.data.WordQueueEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddWordToQueueDialogViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val queueDao: WordQueueDao
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

            viewModelScope.launch {
                // Either allWords or allWordEntries should be non-null
                var allWords: Array<String>? = null
                val allWordEntries = cachedWordEntries

                if (allWordEntries == null) {
                    allWords = queueDao.getAllWords()
                }

                wordFlow.collect { word ->
                    val error = if (word.isBlank()) {
                        AddWordToQueueDialogError.EMPTY_TEXT
                    } else {
                        val trimmedWord = word.trim()

                        val containsWord =
                            allWords?.contains(trimmedWord) ?: allWordEntries!!.any { it.word == trimmedWord }

                        if (containsWord) AddWordToQueueDialogError.WORD_EXISTS else null
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