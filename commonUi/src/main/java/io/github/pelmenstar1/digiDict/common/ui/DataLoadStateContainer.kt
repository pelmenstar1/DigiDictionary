package io.github.pelmenstar1.digiDict.common.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.AttrRes
import androidx.annotation.StyleRes
import com.google.android.material.progressindicator.CircularProgressIndicator
import io.github.pelmenstar1.digiDict.common.DataLoadState
import io.github.pelmenstar1.digiDict.common.launchFlowCollector
import kotlinx.coroutines.CoroutineScope

class DataLoadStateContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    @AttrRes defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {
    private val loadingIndicator: CircularProgressIndicator
    private val errorContainer: ErrorContainer

    private var _content: View? = null
    val content: View
        get() = _content ?: throw IllegalStateException("No content was previously added")

    init {
        super.addView(CircularProgressIndicator(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }

            isIndeterminate = true
            loadingIndicator = this
        })

        super.addView(ErrorContainer(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }

            errorContainer = this
        })

        if (attrs != null) {
            val theme = context.theme

            val a = theme.obtainStyledAttributes(attrs, R.styleable.DataLoadStateContainer, defStyleAttr, defStyleRes)

            try {
                a.getText(R.styleable.DataLoadStateContainer_errorText)?.also {
                    errorContainer.setErrorText(it)
                }
            } finally {
                a.recycle()
            }
        }
    }

    fun <T> setupLoadStateFlow(
        scope: CoroutineScope,
        stateHolder: SingleDataLoadStateHolder<T>,
        onSuccess: suspend (T) -> Unit
    ) {
        errorContainer.setOnRetryListener {
            stateHolder.retryLoadData()
        }

        scope.launchFlowCollector(stateHolder.dataStateFlow) {
            when (it) {
                is DataLoadState.Loading -> {
                    loadingIndicator.visibility = View.VISIBLE
                    errorContainer.visibility = View.GONE
                    content.visibility = View.GONE
                }
                is DataLoadState.Error -> {
                    errorContainer.visibility = View.VISIBLE
                    loadingIndicator.visibility = View.GONE
                    content.visibility = View.GONE
                }
                is DataLoadState.Success -> {
                    content.visibility = View.VISIBLE
                    loadingIndicator.visibility = View.GONE
                    errorContainer.visibility = View.GONE

                    onSuccess(it.value)
                }
            }
        }
    }

    override fun addView(child: View?, index: Int, params: ViewGroup.LayoutParams?) {
        _content = child

        super.addView(child, index, params)
    }
}