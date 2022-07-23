package io.github.pelmenstar1.digiDict.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.github.pelmenstar1.digiDict.databinding.FragmentQuizNavBinding

class QuizNavFragment : Fragment() {
    private val onButtonClickListener = View.OnClickListener {
        val mode = it.tag as QuizMode
        val directions = QuizNavFragmentDirections.actionQuizNavFragmentToQuizFragment(mode)

        findNavController().navigate(directions)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentQuizNavBinding.inflate(inflater, container, false)

        with(binding) {
            initNavButton(quizNavAll, QuizMode.ALL)
            initNavButton(quizNavLast24Hours, QuizMode.LAST_24_HOURS)
            initNavButton(quizNavLast48Hours, QuizMode.LAST_48_HOURS)
        }

        return binding.root
    }

    private fun initNavButton(button: Button, mode: QuizMode) {
        button.tag = mode
        button.setOnClickListener(onButtonClickListener)
    }
}