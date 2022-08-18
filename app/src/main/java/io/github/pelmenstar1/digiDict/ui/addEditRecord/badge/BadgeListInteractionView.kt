package io.github.pelmenstar1.digiDict.ui.addEditRecord.badge

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.AbsSavedState
import android.view.Gravity
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.core.view.setPadding
import androidx.fragment.app.FragmentManager
import com.google.android.material.button.MaterialButton
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.EmptyArray
import io.github.pelmenstar1.digiDict.common.ui.adjustViewCount

class BadgeListInteractionView : HorizontalScrollView {
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
            val c = container
            val badgeCount = c.childCount - 1

            return if (badgeCount == 0) {
                EmptyArray.STRING
            } else {
                Array(badgeCount) { i -> c.getBadgeViewAt(i).text }
            }
        }
        set(value) {
            adjustInputCount(value.size)

            val c = container
            for (i in 0 until (c.childCount - 1)) {
                c.getBadgeViewAt(i).text = value[i]
            }
        }

    var onGetFragmentManager: (() -> FragmentManager)? = null

    private lateinit var container: LinearLayout
    private lateinit var addButton: Button

    private val badgeRemoveListener = OnClickListener {
        val name = (it.parent as BadgeWithRemoveButtonView).text

        removeBadgeByName(name)
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        @AttrRes defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    init {
        init()
    }

    private fun init() {
        addView(createContainer().also { container = it })
    }

    private fun adjustInputCount(newCount: Int) {
        container.adjustViewCount(newCount, lastViewsCount = 1) {
            addView(createBadgeView(), 0)
        }
    }

    private fun createContainer(): LinearLayout {
        return LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )

            orientation = LinearLayout.HORIZONTAL

            addView(createAddButton().also { addButton = it })
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

                marginStart = res.getDimensionPixelOffset(R.dimen.badgeInteraction_addButton_startMargin)
            }

            setPadding(0)

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
                marginStart = res.getDimensionPixelOffset(R.dimen.badge_startMargin)
            }

            setOnRemoveListener(badgeRemoveListener)
        }
    }

    private fun addBadge(name: String) {
        val c = container
        val index = c.childCount - 1

        c.addView(createBadgeView().apply {
            text = name
        }, index)
    }

    private fun removeBadgeByName(name: String) {
        val c = container

        for (i in 0 until (c.childCount - 1)) {
            val view = c.getBadgeViewAt(i)

            if (view.text == name) {
                c.removeViewAt(i)
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

    @Suppress("NOTHING_TO_INLINE")
    private inline fun LinearLayout.getBadgeViewAt(index: Int) = getChildAt(index) as BadgeWithRemoveButtonView

    private fun getFragmentManager() =
        onGetFragmentManager?.invoke() ?: throw IllegalStateException("onGetFragmentManager == null")

    companion object {
        private const val SELECTOR_DIALOG_TAG = "BadgeSelectorDialog"
    }
}