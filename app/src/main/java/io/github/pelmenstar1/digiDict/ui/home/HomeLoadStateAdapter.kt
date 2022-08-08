package io.github.pelmenstar1.digiDict.ui.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.progressindicator.CircularProgressIndicator
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.getLazyValue
import io.github.pelmenstar1.digiDict.common.ui.ErrorContainer

class HomeLoadStateAdapter(
    private val retry: () -> Unit
) : LoadStateAdapter<HomeLoadStateAdapter.ViewHolder>() {
    inner class ViewHolder(container: ViewGroup) : RecyclerView.ViewHolder(container) {
        private val progressIndicator = container.findViewById<CircularProgressIndicator>(R.id.home_loadingIndicator)
        private val errorContainer = container.findViewById<ErrorContainer>(R.id.home_errorContainer)

        init {
            errorContainer.setOnRetryListener(retryOnClickListener)
        }

        fun bind(state: LoadState) {
            when (state) {
                is LoadState.Loading -> {
                    progressIndicator.visibility = View.VISIBLE
                    errorContainer.visibility = View.GONE
                }
                is LoadState.Error -> {
                    progressIndicator.visibility = View.GONE
                    errorContainer.visibility = View.VISIBLE
                }
                else -> {}
            }
        }
    }

    private var itemLayoutParams: LinearLayout.LayoutParams? = null
    private val retryOnClickListener = View.OnClickListener { retry() }

    private var layoutInflater: LayoutInflater? = null

    override fun onCreateViewHolder(parent: ViewGroup, loadState: LoadState): ViewHolder {
        val context = parent.context

        val inflater = getLazyValue(
            layoutInflater,
            { LayoutInflater.from(context) },
            { layoutInflater = it }
        )

        val layoutParams = getLazyValue(
            itemLayoutParams,
            { createLayoutParams(context) },
            { itemLayoutParams = it }
        )

        val container = inflater.inflate(R.layout.home_loading_error_and_progress_frame, parent, false).also {
            it.layoutParams = layoutParams
        }

        return ViewHolder(container as ViewGroup)
    }

    override fun onBindViewHolder(holder: ViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    companion object {
        internal fun createLayoutParams(context: Context): LinearLayout.LayoutParams {
            val res = context.resources

            return LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = res.getDimensionPixelOffset(R.dimen.home_stateContainerMarginTop)
                bottomMargin = res.getDimensionPixelOffset(R.dimen.home_stateContainerMarginBottom)
            }
        }
    }
}