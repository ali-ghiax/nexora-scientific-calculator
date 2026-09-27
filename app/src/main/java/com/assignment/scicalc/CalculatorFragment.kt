package com.assignment.scicalc

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.assignment.scicalc.databinding.FragmentCalculatorBinding

class CalculatorFragment : Fragment() {

    private var _binding: FragmentCalculatorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AppViewModel by activityViewModels()

    private var expression
        get() = viewModel.calcExpression
        set(value) { viewModel.calcExpression = value }
    private var lastResult
        get() = viewModel.calcResult
        set(value) { viewModel.calcResult = value }
    private var justEvaluated
        get() = viewModel.calcEvaluated
        set(value) { viewModel.calcEvaluated = value }
    private var secondMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bindButtons()
        binding.tvResult.setOnLongClickListener {
            copyResult()
            true
        }
        refreshUi()
        render()
    }

    override fun onResume() {
        super.onResume()
        viewModel.engine.degreeMode = viewModel.degreeMode
        refreshUi()
        if (expression.isNotEmpty()) renderLive()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun bindButtons() = with(binding) {
        mapOf(
            btn0 to "0", btn1 to "1", btn2 to "2", btn3 to "3", btn4 to "4",
            btn5 to "5", btn6 to "6", btn7 to "7", btn8 to "8", btn9 to "9", btnDot to "."
        ).forEach { (button, value) -> button.setOnClickListener { onNumber(value) } }

        btnAdd.setOnClickListener { onOperator("+") }
        btnSub.setOnClickListener { onOperator("−") }
        btnMul.setOnClickListener { onOperator("×") }
        btnDiv.setOnClickListener { onOperator("÷") }
        btnPow.setOnClickListener { if (secondMode) appendFunction("cbrt") else onOperator("^") }
        btnPercent.setOnClickListener { appendSuffix("%") }
        btnLParen.setOnClickListener { onOpenParen() }
        btnRParen.setOnClickListener { appendToken(")") }
        btnPi.setOnClickListener { appendConstant("π") }
        btnE.setOnClickListener { appendConstant("e") }
        btnSin.setOnClickListener { appendFunction(if (secondMode) "asin" else "sin") }
        btnCos.setOnClickListener { appendFunction(if (secondMode) "acos" else "cos") }
        btnTan.setOnClickListener { appendFunction(if (secondMode) "atan" else "tan") }
        btnLog.setOnClickListener { if (secondMode) appendFunction("exp10") else appendFunction("log") }
        btnLn.setOnClickListener { if (secondMode) appendFunction("exp") else appendFunction("ln") }
        btnSqrt.setOnClickListener { if (secondMode) appendSuffix("^2") else appendFunction("√") }
        btnFact.setOnClickListener { appendSuffix("!") }
        btnInv.setOnClickListener { onReciprocal() }
        btnPlusMinus.setOnClickListener { toggleSign() }
        btnEquals.setOnClickListener { evaluate() }
        btnAc.setOnClickListener { clearAll() }
        btnBack.setOnClickListener { backspace() }
        btnSecond.setOnClickListener { toggleSecond() }
        btnDeg.setOnClickListener { toggleAngle() }
        tvMode.setOnClickListener { toggleAngle() }
    }

    private fun onNumber(value: String) {
        buzz()
        if (justEvaluated) {
            expression = ""
            justEvaluated = false
        }
        if (value == ".") {
            val lastNumber = currentNumber()
            if (lastNumber.contains('.')) return
            if (lastNumber.isEmpty()) {
                expression += "0."
                render()
                return
            }
        }
        expression += if (endsWithConstantOrClose()) "×$value" else value
        renderLive()
    }

    private fun onOperator(symbol: String) {
        buzz()
        if (justEvaluated) {
            expression = lastResult
            justEvaluated = false
        }
        if (expression.isEmpty()) {
            if (symbol == "−") expression = "−"
            render()
            return
        }
        expression = if (expression.last() in OPERATORS) expression.dropLast(1) + symbol else expression + symbol
        render()
    }

    private fun onOpenParen() {
        buzz()
        prepareContinueOrReplace()
        expression += if (expression.isNotEmpty() && (expression.last().isDigit() || expression.last() in "πe)!%")) "×(" else "("
        render()
    }

    private fun appendToken(token: String) {
        buzz()
        if (justEvaluated) {
            expression = lastResult
            justEvaluated = false
        }
        expression += token
        render()
    }

    private fun appendConstant(symbol: String) {
        buzz()
        prepareContinueOrReplace()
        expression += if (expression.isNotEmpty() && (expression.last().isDigit() || expression.last() in "πe)!%")) "×$symbol" else symbol
        renderLive()
    }

    private fun appendFunction(name: String) {
        buzz()
        prepareContinueOrReplace()
        if (expression.isNotEmpty() && (expression.last().isDigit() || expression.last() in "πe)!%")) expression += "×"
        expression += if (name == "exp10") "10^(" else "$name("
        if (secondMode) toggleSecond()
        render()
    }

    private fun appendSuffix(suffix: String) {
        buzz()
        if (justEvaluated) {
            expression = lastResult
            justEvaluated = false
        }
        if (expression.isEmpty()) return
        val last = expression.last()
        if (last.isDigit() || last in "πe)") {
            expression += suffix
            renderLive()
        }
    }

    private fun onReciprocal() {
        buzz()
        if (justEvaluated) {
            expression = lastResult
            justEvaluated = false
        }
        if (expression.isEmpty()) return
        expression = "1/($expression)"
        renderLive()
    }

    private fun toggleSign() {
        buzz()
        if (justEvaluated) {
            lastResult = if (lastResult.startsWith("−") || lastResult.startsWith("-")) lastResult.drop(1) else if (lastResult != "0") "−$lastResult" else lastResult
            expression = lastResult
            render()
            return
        }
        if (expression.isEmpty()) {
            expression = "−"
            render()
            return
        }
        expression = if (expression.startsWith("−(") && expression.endsWith(")")) {
            expression.removePrefix("−(").removeSuffix(")")
        } else {
            "−($expression)"
        }
        renderLive()
    }

    private fun evaluate() {
        buzz()
        val source = if (expression.isBlank()) lastResult else expression
        val result = viewModel.engine.evaluate(source)
        lastResult = result
        justEvaluated = true
        binding.tvExpression.text = source
        binding.tvResult.text = result
        viewModel.addHistory(source, result)
    }

    private fun clearAll() {
        buzz()
        expression = ""
        lastResult = "0"
        justEvaluated = false
        render()
    }

    private fun backspace() {
        buzz()
        justEvaluated = false
        if (expression.isEmpty()) {
            render()
            return
        }
        val functions = listOf("asin(", "acos(", "atan(", "sqrt(", "cbrt(", "sin(", "cos(", "tan(", "log(", "ln(", "exp(", "√(", "10^(")
        val match = functions.firstOrNull { expression.endsWith(it) }
        expression = if (match != null) expression.removeSuffix(match) else expression.dropLast(1)
        renderLive()
    }

    private fun toggleSecond() {
        buzz()
        secondMode = !secondMode
        refreshSecondLabels()
    }

    private fun toggleAngle() {
        buzz()
        viewModel.degreeMode = !viewModel.degreeMode
        refreshAngle()
        if (expression.isNotEmpty()) renderLive()
    }

    private fun prepareContinueOrReplace() {
        if (justEvaluated) {
            expression = ""
            justEvaluated = false
        }
    }

    private fun currentNumber(): String {
        val builder = StringBuilder()
        for (c in expression.reversed()) {
            if (c.isDigit() || c == '.') builder.append(c) else break
        }
        return builder.reverse().toString()
    }

    private fun endsWithConstantOrClose(): Boolean = expression.isNotEmpty() && expression.last() in "πe)!"

    private fun render() {
        binding.tvExpression.text = expression
        binding.tvResult.text = if (justEvaluated) lastResult else expression.ifBlank { "0" }
    }

    private fun renderLive() {
        binding.tvExpression.text = expression
        if (expression.isBlank()) {
            binding.tvResult.text = "0"
            return
        }
        val preview = viewModel.engine.evaluate(expression)
        binding.tvResult.text = if (viewModel.engine.isError(preview)) expression else preview
    }

    private fun refreshUi() {
        val showSci = viewModel.scientificEnabled
        binding.sciPad.visibility = if (showSci) View.VISIBLE else View.GONE
        binding.extraRow.visibility = if (showSci) View.VISIBLE else View.GONE
        refreshSecondLabels()
        refreshAngle()
    }

    private fun refreshSecondLabels() = with(binding) {
        btnSecond.isSelected = secondMode
        btnSin.text = if (secondMode) "sin⁻¹" else "sin"
        btnCos.text = if (secondMode) "cos⁻¹" else "cos"
        btnTan.text = if (secondMode) "tan⁻¹" else "tan"
        btnLog.text = if (secondMode) "10ˣ" else "log"
        btnLn.text = if (secondMode) "eˣ" else "ln"
        btnSqrt.text = if (secondMode) "x²" else "√"
        btnPow.text = if (secondMode) "³√" else "xʸ"
    }

    private fun refreshAngle() {
        val label = if (viewModel.degreeMode) "DEG" else "RAD"
        binding.btnDeg.text = label
        binding.tvMode.text = label
    }

    private fun copyResult() {
        val text = binding.tvResult.text.toString()
        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Nexora", text))
        Toast.makeText(requireContext(), R.string.copied, Toast.LENGTH_SHORT).show()
    }

    private fun buzz() {
        if (viewModel.hapticEnabled) {
            binding.root.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    companion object {
        private val OPERATORS = setOf('+', '−', '×', '÷', '^')
    }
}
