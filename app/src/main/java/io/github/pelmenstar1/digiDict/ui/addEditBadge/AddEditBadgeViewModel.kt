package io.github.pelmenstar1.digiDict.ui.addEditBadge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.pelmenstar1.digiDict.common.DataLoadStateManager
import io.github.pelmenstar1.digiDict.common.ValidityFlow
import io.github.pelmenstar1.digiDict.common.android.viewModelAction
import io.github.pelmenstar1.digiDict.common.firstSuccess
import io.github.pelmenstar1.digiDict.data.RecordBadgeDao
import io.github.pelmenstar1.digiDict.data.RecordBadgeInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddEditBadgeViewModel @Inject constructor(
    private val badgeDao: RecordBadgeDao
) : ViewModel() {
    private val currentBadgeIdFlow = MutableStateFlow(-1)
    private val checkNameChannel = Channel<String>(Channel.CONFLATED)

    /**
     * Gets or sets id of current badge.
     * If there's no current badge, the id is -1.
     * The setter should be called exactly one time.
     */
    var currentBadgeId: Int = -1
        set(value) {
            field = value

            currentBadgeIdFlow.value = value

            if (value < 0) {
                // If we're in addition mode, the name is actually empty, so
                // the appropriate error should be set
                _nameErrorFlow.value = AddEditBadgeFragmentMessage.EMPTY_TEXT
            }
        }

    // RecordBadgeInfo is purposely non-null because it has no sense to edit nonexistent badge
    val currentBadgeStateManager = DataLoadStateManager<RecordBadgeInfo>(TAG)
    val currentBadgeStateFlow = currentBadgeStateManager.buildFlow(viewModelScope) {
        fromFlow {
            currentBadgeIdFlow.filter { it >= 0 }.map {
                val badge = badgeDao.getById(it)!!

                _name = badge.name
                outlineColor = badge.outlineColor

                validity.mutate {
                    set(nameValidityField, value = true)
                }

                badge
            }
        }
    }

    private val _nameErrorFlow = MutableStateFlow<AddEditBadgeFragmentMessage?>(null)
    val nameErrorFlow = _nameErrorFlow.asStateFlow()

    val validity = ValidityFlow(validityScheme)

    private var _name = ""

    /**
     * Name of the badge. The string is expected to be without trailing and leading whitespaces.
     */
    var name: String
        get() = _name
        set(value) {
            _name = value

            scheduleCheckName(value)
        }

    var outlineColor = 0

    val addOrEditAction = viewModelAction(TAG) {
        val currentId = currentBadgeId

        if (currentId >= 0) {
            badgeDao.update(RecordBadgeInfo(currentId, name, outlineColor))
        } else {
            badgeDao.insert(RecordBadgeInfo(id = 0, name, outlineColor))
        }
    }

    init {
        startCheckNameJob()
    }

    fun scheduleCheckName(name: String) {
        validity.mutate {
            set(nameValidityField, value = false, isComputed = false)
        }

        // It can't fail as checkNameChannel is conflated
        checkNameChannel.trySend(name)
    }

    fun retryLoadCurrentBadge() {
        currentBadgeStateManager.retry()
    }

    fun addOrEditBadge() {
        addOrEditAction.runWhenValid(validity)
    }

    private fun startCheckNameJob() {
        viewModelScope.launch(Dispatchers.Default) {
            var allBadgeNames: Array<String>? = null
            var currentBadge: RecordBadgeInfo? = null

            while (true) {
                val name = checkNameChannel.receive()
                var error: AddEditBadgeFragmentMessage? = null

                if (name.isEmpty()) {
                    error = AddEditBadgeFragmentMessage.EMPTY_TEXT
                } else {
                    var checkAllBadges = true

                    if (currentBadgeId >= 0) {
                        if (currentBadge == null) {
                            currentBadge = currentBadgeStateFlow.firstSuccess()
                        }

                        // It's totally valid that current name is equal to name of badge we're editing
                        // because it doesn't violate the rule that all badge names should be unique.
                        // So, it's illegal to use the path for other names as allBadgeNames will contain
                        // current badge name and name will become invalid which isn't right.
                        checkAllBadges = name != currentBadge.name
                    }

                    if (checkAllBadges) {
                        if (allBadgeNames == null) {
                            allBadgeNames = badgeDao.getAllNames()
                        }

                        // All badge names should be unique.
                        if (allBadgeNames.contains(name)) {
                            error = AddEditBadgeFragmentMessage.NAME_EXISTS
                        }
                    }
                }

                _nameErrorFlow.value = error
                validity.mutate {
                    set(nameValidityField, value = error == null)
                }
            }
        }
    }

    companion object {
        private const val TAG = "AddEditBadgeFragmentVM"

        private val nameValidityField = ValidityFlow.Field(ordinal = 0)
        private val validityScheme = ValidityFlow.Scheme(nameValidityField)
    }
}