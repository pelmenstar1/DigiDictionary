package io.github.pelmenstar1.digiDict.ui.addEditRecord.badge

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.AbsSavedState
import android.view.Gravity
import android.view.View.OnClickListener
import android.widget.Button
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import androidx.fragment.app.FragmentManager
import com.google.android.material.button.MaterialButton
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.EmptyArray
import io.github.pelmenstar1.digiDict.common.ui.MultilineHorizontalLinearLayout
import io.github.pelmenstar1.digiDict.common.ui.adjustViewCount
import io.github.pelmenstar1.digiDict.common.ui.getTypedViewAt

class BadgeListInteractionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : MultilineHorizontalLinearLayout(context, attrs, defStyleAttr, defStyleRes) {
    private class SavedState : AbsSavedState {
        var badges: Array<String> = EmptyArray.STRING

        constructor(superState: Parcelable?) : super(superState)

        constructor(parcel: Parcel) : super(parcel) {
            badges = parcel.createStringArray() ?: EmptyArray.STRING
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)

            dest.writeStringArray(badges)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(source: Parcel) = SavedState(source)
            override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
        }
    }

    var badges: Array<String>
        get() {
            val badgeCount = childCount - 1

            return if (badgeCount == 0) {
                EmptyArray.STRING
            } else {
                Array(badgeCount) { i -> getBadgeViewAt(i).text }
            }
        }
        set(value) {
            adjustInputCount(value.size)

            for (i in 0 until (childCount - 1)) {
                getBadgeViewAt(i).text = value[i]
            }
        }

    var onGetFragmentManager: (() -> FragmentManager)? = null

    private val addButton: Button

    private val badgeRemoveListener = OnClickListener {
        val name = (it.parent as BadgeWithRemoveButtonView).text

        removeBadgeByName(name)
    }

    init {
        addView(createAddButton().also { addButton = it })
    }

    private fun adjustInputCount(newCount: Int) {
        adjustViewCount(newCount, lastViewsCount = 1) {
            addView(createBadgeView(), 0)
        }
    }

    private fun createAddButton(): Button {
        val res = resources

        return MaterialButton(context, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL

                // Add button should be in one line with badges.
                topMargin = res.getDimensionPixelOffset(R.dimen.badge_topMargin)
                //marginStart = res.getDimensionPixelOffset(R.dimen.badgeInteraction_addButton_startMargin)
            }

            res.getDimensionPixelOffset(R.dimen.badgeInteraction_addButton_rightPadding).also {
                setPadding(0, 0, it, 0)
            }

            iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
            setIconResource(R.drawable.ic_add)
            setText(R.string.badgeInteraction_badge)

            setOnClickListener { showBadgeSelectorDialog() }
        }
    }

    private fun createBadgeView(): BadgeWithRemoveButtonView {
        val res = resources

        return BadgeWithRemoveButtonView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_VERTICAL

                topMargin = res.getDimensionPixelOffset(R.dimen.badge_topMargin)
                marginEnd = res.getDimensionPixelOffset(R.dimen.badge_endMargin)
            }

            setOnRemoveListener(badgeRemoveListener)
        }
    }

    private fun addBadge(name: String) {
        val index = childCount - 1

        addView(createBadgeView().apply { text = name }, index)
    }

    private fun removeBadgeByName(name: String) {
        for (i in 0 until (childCount - 1)) {
            val view = getBadgeViewAt(i)

            if (view.text == name) {
                removeViewAt(i)
                break
            }
        }
    }

    private fun showBadgeSelectorDialog() {
        val fm = getFragmentManager()
        val dialog = BadgeSelectorDialog().also {
            it.arguments = BadgeSelectorDialog.args(badges)
        }

        initBadgeSelectorDialog(dialog)

        dialog.show(fm, SELECTOR_DIALOG_TAG)
    }

    private fun initBadgeSelectorDialog(dialog: BadgeSelectorDialog) {
        dialog.onSelected = { name ->
            addBadge(name)
        }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)

        addButton.isEnabled = enabled
    }

    override fun onSaveInstanceState(): Parcelable {
        return SavedState(super.onSaveInstanceState()).also {
            it.badges = badges
        }
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state is SavedState) {
            badges = state.badges

            super.onRestoreInstanceState(state.superState)
        }

        getFragmentManager().also { fm ->
            fm.findFragmentByTag(SELECTOR_DIALOG_TAG)?.also { dialog ->
                initBadgeSelectorDialog(dialog as BadgeSelectorDialog)
            }
        }
    }

    private fun getBadgeViewAt(index: Int) = getTypedViewAt<BadgeWithRemoveButtonView>(index)

    private fun getFragmentManager() = requireNotNull(onGetFragmentManager?.invoke()) {
        "Fragment manager getter shouldn't bet null"
    }

    companion object {
        private const val SELECTOR_DIALOG_TAG = "BadgeSelectorDialog"
    }
}