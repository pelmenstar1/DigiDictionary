package io.github.pelmenstar1.digiDict.ui.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import io.github.pelmenstar1.digiDict.databinding.FragmentQuizNavBinding

class QuizNavFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentQuizNavBinding.inflate(inflater, container, false)
        val navController = findNavController()

        with(binding) {
            initNavButton(quizNavAll, navController, QuizMode.ALL)
            initNavButton(quizNavLast24Hours, navController, QuizMode.LAST_24_HOURS)
            initNavButton(quizNavLast48Hours, navController, QuizMode.LAST_48_HOURS)
        }

        return binding.root
    }

    private fun initNavButton(button: Button, navController: NavController, mode: QuizMode) {
        button.setOnClickListener {
            val directions = QuizNavFragmentDirections.actionQuizNavFragmentToQuizFragment(mode)

            navController.navigate(directions)
        }
    }
}