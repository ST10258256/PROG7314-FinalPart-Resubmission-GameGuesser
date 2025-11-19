package com.example.gameguesser.ui.home

import android.content.Context
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.gameguesser.databinding.FragmentHomeBinding
import androidx.navigation.fragment.findNavController
import com.example.gameguesser.DAOs.UserDao
import com.example.gameguesser.Database.UserDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var userDb: UserDatabase
    private lateinit var userDao: UserDao

    private val compareGameStreakFlow = MutableStateFlow(0)
    private val keyWordStreakFlow = MutableStateFlow(0)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Navigation
        binding.playKeyWordsButton.setOnClickListener {
            findNavController().navigate(com.example.gameguesser.R.id.keyGameFragment)
        }

        binding.playBonusGameButton.setOnClickListener {
            findNavController().navigate(com.example.gameguesser.R.id.compareGameFragment)
        }

        userDb = UserDatabase.getDatabase(requireContext())
        userDao = userDb.userDao()

        checkAllStreaksOnAppLoad()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Collect flows once
        viewLifecycleOwner.lifecycleScope.launch {
            keyWordStreakFlow.collect { streakValue ->
                binding.keyWordsStreak.text = "🔥 $streakValue"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            compareGameStreakFlow.collect { streakValue ->
                binding.compareGame.text = "🔥 $streakValue"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAllStreaksOnAppLoad()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkAllStreaksOnAppLoad() {
        lifecycleScope.launch(Dispatchers.IO) {
            val userId = getLoggedInUserId() ?: return@launch
            val user = userDao.getUser(userId) ?: return@launch

            var wasReset = false

            if (user.lastPlayedCG > 0L && !wasYesterdayOrToday(user.lastPlayedCG)) {
                user.streakCG = 0
                wasReset = true
            }

            if (user.lastPlayedKW > 0L && !wasYesterdayOrToday(user.lastPlayedKW)) {
                user.streakKW = 0
                wasReset = true
            }

            if (wasReset) {
                userDao.upsert(user)
            }

            keyWordStreakFlow.value = user.streakKW
            compareGameStreakFlow.value = user.streakCG
        }
    }

    private fun wasYesterdayOrToday(timestamp: Long): Boolean {
        if (timestamp == 0L) return false

        val lastPlayedCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val todayCal = Calendar.getInstance()

        // Today
        if (lastPlayedCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            lastPlayedCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
            return true
        }

        // Yesterday
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return lastPlayedCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
                lastPlayedCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)
    }

    private fun getLoggedInUserId(): String? {
        val prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return prefs.getString("userId", null)
    }
}
