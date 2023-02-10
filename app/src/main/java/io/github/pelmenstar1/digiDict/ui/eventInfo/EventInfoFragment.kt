package io.github.pelmenstar1.digiDict.ui.eventInfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import io.github.pelmenstar1.digiDict.R
import io.github.pelmenstar1.digiDict.common.time.CompatDateTimeFormatter
import io.github.pelmenstar1.digiDict.common.time.TimeDifferenceFormatter
import io.github.pelmenstar1.digiDict.databinding.FragmentEventInfoBinding

@AndroidEntryPoint
class EventInfoFragment : Fragment() {
    private val viewModel by viewModels<EventInfoViewModel>()
    private val args by navArgs<EventInfoFragmentArgs>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val vm = viewModel
        val binding = FragmentEventInfoBinding.inflate(inflater, container, false)

        with(binding) {
            eventInfoContainer.setupLoadStateFlow(lifecycleScope, vm) { event ->
                val context = requireContext()
                val dateFormatter = CompatDateTimeFormatter(context, DATE_TIME_FORMAT)

                val startTime = event.startEpochSeconds
                val endTime = event.endEpochSeconds

                if (endTime >= 0) {
                    val timeDiffFormatter = TimeDifferenceFormatter(context)
                    val duration = endTime - startTime

                    eventInfoNameView.text = event.name
                    eventInfoEndTimeView.setValue(dateFormatter.format(endTime))
                    eventInfoDurationView.setValue(timeDiffFormatter.formatDifference(duration))
                } else {
                    // TODO: SHOW RECORDS IN EVENTS INFO FRAGMENT

                    // Explicitly show that the event is ongoing.
                    eventInfoNameView.text = getString(R.string.eventInfo_nameFormatOngoing, event.name)

                    // The event is ongoing, so currently there's no end time and duration, thus no sense in
                    // showing this TextViews.
                    eventInfoEndTimeView.visibility = View.GONE
                    eventInfoDurationView.visibility = View.GONE
                }

                eventInfoStartTimeView.setValue(dateFormatter.format(startTime))
                eventInfoTotalRecordsAddedView.setValue(event.totalRecordsAdded)
            }
        }

        vm.eventId = args.id

        return binding.root
    }

    companion object {
        private const val DATE_TIME_FORMAT = "dd MMMM yyyy HH:mm"
    }
}