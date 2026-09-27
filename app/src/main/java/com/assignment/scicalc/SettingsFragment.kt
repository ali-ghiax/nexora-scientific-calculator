package com.assignment.scicalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.assignment.scicalc.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.switchAngle.isChecked = viewModel.degreeMode
        binding.switchHaptic.isChecked = viewModel.hapticEnabled
        binding.switchScientific.isChecked = viewModel.scientificEnabled
        binding.switchDark.isChecked = viewModel.darkTheme
        refreshAngleLabel()

        binding.switchAngle.setOnCheckedChangeListener { _, checked ->
            viewModel.degreeMode = checked
            refreshAngleLabel()
        }
        binding.switchHaptic.setOnCheckedChangeListener { _, checked ->
            viewModel.hapticEnabled = checked
        }
        binding.switchScientific.setOnCheckedChangeListener { _, checked ->
            viewModel.scientificEnabled = checked
        }
        binding.switchDark.setOnCheckedChangeListener { _, checked ->
            viewModel.darkTheme = checked
            AppCompatDelegate.setDefaultNightMode(
                if (checked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }

    private fun refreshAngleLabel() {
        binding.tvAngleSub.setText(if (viewModel.degreeMode) R.string.degrees else R.string.radians)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
