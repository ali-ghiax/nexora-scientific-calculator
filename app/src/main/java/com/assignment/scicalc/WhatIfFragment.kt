package com.assignment.scicalc

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.assignment.scicalc.databinding.FragmentWhatifBinding

class WhatIfFragment : Fragment() {

    private var _binding: FragmentWhatifBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by activityViewModels()
    private var xValue = 1.0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentWhatifBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.etExpr.setText("2x + 5")
        binding.seekX.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                xValue = (progress - 100) / 10.0
                refresh()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        binding.etExpr.doAfterTextChanged { refresh() }
        listOf(
            binding.preset0 to 0.0,
            binding.preset1 to 1.0,
            binding.preset2 to 2.0,
            binding.preset5 to 5.0,
            binding.preset10 to 10.0
        ).forEach { (chip, value) ->
            chip.setOnClickListener { setX(value) }
        }
        binding.btnReset.setOnClickListener {
            binding.etExpr.setText("2x + 5")
            setX(1.0)
        }
        binding.btnSave.setOnClickListener {
            val expr = displayExpression()
            val result = binding.tvWhatResult.text.toString()
            viewModel.addHistory(expr, result)
            Toast.makeText(requireContext(), R.string.save_to_history, Toast.LENGTH_SHORT).show()
        }
        refresh()
    }

    private fun setX(value: Double) {
        xValue = value
        binding.seekX.progress = ((value * 10) + 100).toInt().coerceIn(0, 200)
        refresh()
    }

    private fun refresh() {
        viewModel.engine.degreeMode = viewModel.degreeMode
        binding.tvXValue.text = viewModel.engine.formatNumber(xValue)
        val raw = binding.etExpr.text?.toString().orEmpty()
        val result = viewModel.engine.evaluate(raw, mapOf("x" to xValue))
        binding.tvWhatResult.text = result
    }

    private fun displayExpression(): String {
        val raw = binding.etExpr.text?.toString().orEmpty().ifBlank { "2x + 5" }
        return raw.replace("x", "(${viewModel.engine.formatNumber(xValue)})")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
