package com.assignment.scicalc

import kotlin.math.PI
import kotlin.math.E
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Expression evaluator for the scientific calculator.
 * Supports arithmetic, powers, factorials, roots, logs, and trig
 * in either degrees or radians.
 */
class CalculatorEngine {

    var degreeMode: Boolean = true

    fun evaluate(raw: String, variables: Map<String, Double> = emptyMap()): String {
        val expression = raw.trim()
        if (expression.isEmpty()) return "0"
        return try {
            val tokens = tokenize(normalize(expression))
            val withImplicit = insertImplicitMultiplication(tokens)
            val rpn = toRpn(withImplicit)
            val value = evalRpn(rpn, variables)
            formatNumber(value)
        } catch (error: CalcException) {
            error.message ?: "Error"
        } catch (_: Exception) {
            "Error"
        }
    }

    fun isError(value: String): Boolean {
        return value in ERRORS
    }

    private fun normalize(input: String): String {
        return input
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace(" ", "")
            .replace("π", "pi")
            .replace("√", "sqrt")
            .replace("³√", "cbrt")
    }

    private sealed class Token {
        data class Number(val value: Double) : Token()
        data class Operator(val symbol: Char) : Token()
        data class Function(val name: String) : Token()
        data object LParen : Token()
        data object RParen : Token()
        data object Factorial : Token()
        data object Percent : Token()
        data object UnaryMinus : Token()
        data class Variable(val name: String) : Token()
    }

    private fun tokenize(input: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val functions = listOf(
            "asin", "acos", "atan", "sinh", "cosh", "tanh",
            "sqrt", "cbrt", "sin", "cos", "tan", "log", "ln", "abs", "exp"
        )

        while (i < input.length) {
            val c = input[i]
            when {
                c.isDigit() || c == '.' -> {
                    val start = i
                    i++
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) i++
                    if (i < input.length && (input[i] == 'e' || input[i] == 'E')) {
                        val next = i + 1
                        if (next < input.length && (input[next].isDigit() || input[next] == '+' || input[next] == '-')) {
                            i++
                            if (input[i] == '+' || input[i] == '-') i++
                            while (i < input.length && input[i].isDigit()) i++
                        }
                    }
                    val text = input.substring(start, i)
                    tokens += Token.Number(text.toDoubleOrNull() ?: throw CalcException("Invalid number"))
                }

                input.startsWith("pi", i) -> {
                    tokens += Token.Number(PI)
                    i += 2
                }

                c == 'e' && (i + 1 >= input.length || !input[i + 1].isLetter()) -> {
                    tokens += Token.Number(E)
                    i++
                }

                c == 'x' && (i + 1 >= input.length || !input[i + 1].isLetter()) -> {
                    tokens += Token.Variable("x")
                    i++
                }

                c.isLetter() -> {
                    val match = functions.firstOrNull { input.startsWith(it, i) }
                        ?: throw CalcException("Unknown function")
                    tokens += Token.Function(match)
                    i += match.length
                }

                c == '(' -> {
                    tokens += Token.LParen
                    i++
                }

                c == ')' -> {
                    tokens += Token.RParen
                    i++
                }

                c == '!' -> {
                    tokens += Token.Factorial
                    i++
                }

                c == '%' -> {
                    tokens += Token.Percent
                    i++
                }

                c == '+' || c == '*' || c == '/' || c == '^' -> {
                    tokens += Token.Operator(c)
                    i++
                }

                c == '-' -> {
                    val prev = tokens.lastOrNull()
                    val unary = prev == null ||
                        prev is Token.Operator ||
                        prev is Token.LParen ||
                        prev is Token.Function ||
                        prev is Token.UnaryMinus
                    tokens += if (unary) Token.UnaryMinus else Token.Operator('-')
                    i++
                }

                else -> throw CalcException("Invalid character")
            }
        }
        return tokens
    }

    private fun insertImplicitMultiplication(tokens: List<Token>): List<Token> {
        if (tokens.isEmpty()) return tokens
        val result = mutableListOf<Token>()
        for (token in tokens) {
            val prev = result.lastOrNull()
            if (prev != null && needsImplicitMultiply(prev, token)) {
                result += Token.Operator('*')
            }
            result += token
        }
        return result
    }

    private fun needsImplicitMultiply(left: Token, right: Token): Boolean {
        val leftValue = left is Token.Number ||
            left is Token.Variable ||
            left is Token.RParen ||
            left is Token.Factorial ||
            left is Token.Percent
        val rightValue = right is Token.Number ||
            right is Token.Variable ||
            right is Token.LParen ||
            right is Token.Function ||
            right is Token.UnaryMinus
        return leftValue && rightValue
    }

    private fun precedence(token: Token): Int = when (token) {
        is Token.Operator -> when (token.symbol) {
            '+', '-' -> 1
            '*', '/' -> 2
            '^' -> 4
            else -> 0
        }
        is Token.UnaryMinus -> 3
        is Token.Function -> 5
        else -> 0
    }

    private fun isRightAssociative(token: Token): Boolean {
        return token is Token.Operator && token.symbol == '^' || token is Token.UnaryMinus
    }

    private fun toRpn(tokens: List<Token>): List<Token> {
        val output = mutableListOf<Token>()
        val stack = ArrayDeque<Token>()

        for (token in tokens) {
            when (token) {
                is Token.Number, is Token.Variable -> output += token
                is Token.Function, is Token.UnaryMinus -> stack.addLast(token)
                is Token.Operator -> {
                    while (stack.isNotEmpty()) {
                        val top = stack.last()
                        if (top is Token.LParen) break
                        val shouldPop = precedence(top) > precedence(token) ||
                            (precedence(top) == precedence(token) && !isRightAssociative(token))
                        if (!shouldPop) break
                        output += stack.removeLast()
                    }
                    stack.addLast(token)
                }
                is Token.LParen -> stack.addLast(token)
                is Token.RParen -> {
                    while (stack.isNotEmpty() && stack.last() !is Token.LParen) {
                        output += stack.removeLast()
                    }
                    if (stack.isEmpty()) throw CalcException("Mismatched parentheses")
                    stack.removeLast()
                    if (stack.isNotEmpty() && (stack.last() is Token.Function || stack.last() is Token.UnaryMinus)) {
                        output += stack.removeLast()
                    }
                }
                is Token.Factorial, is Token.Percent -> output += token
            }
        }

        while (stack.isNotEmpty()) {
            val top = stack.removeLast()
            if (top is Token.LParen || top is Token.RParen) {
                throw CalcException("Mismatched parentheses")
            }
            output += top
        }
        return output
    }

    private fun evalRpn(tokens: List<Token>, variables: Map<String, Double>): Double {
        val stack = ArrayDeque<Double>()
        for (token in tokens) {
            when (token) {
                is Token.Number -> stack.addLast(token.value)
                is Token.Variable -> {
                    val value = variables[token.name]
                        ?: throw CalcException("Add x to your expression.")
                    stack.addLast(value)
                }
                is Token.Operator -> {
                    if (stack.size < 2) throw CalcException("Incomplete expression")
                    val b = stack.removeLast()
                    val a = stack.removeLast()
                    stack.addLast(applyOperator(a, b, token.symbol))
                }
                is Token.UnaryMinus -> {
                    if (stack.isEmpty()) throw CalcException("Incomplete expression")
                    stack.addLast(-stack.removeLast())
                }
                is Token.Function -> {
                    if (stack.isEmpty()) throw CalcException("Incomplete expression")
                    stack.addLast(applyFunction(token.name, stack.removeLast()))
                }
                is Token.Factorial -> {
                    if (stack.isEmpty()) throw CalcException("Incomplete expression")
                    stack.addLast(factorial(stack.removeLast()))
                }
                is Token.Percent -> {
                    if (stack.isEmpty()) throw CalcException("Incomplete expression")
                    stack.addLast(stack.removeLast() / 100.0)
                }
                else -> throw CalcException("Error")
            }
        }
        if (stack.size != 1) throw CalcException("Incomplete expression")
        val result = stack.last()
        if (result.isNaN() || result.isInfinite()) throw CalcException("Math error")
        return result
    }

    private fun applyOperator(a: Double, b: Double, op: Char): Double {
        return when (op) {
            '+' -> a + b
            '-' -> a - b
            '*' -> a * b
            '/' -> {
                if (b == 0.0) throw CalcException("Cannot divide by zero")
                a / b
            }
            '^' -> {
                if (a == 0.0 && b < 0.0) throw CalcException("Math error")
                a.pow(b)
            }
            else -> throw CalcException("Unknown operator")
        }
    }

    private fun applyFunction(name: String, x: Double): Double {
        val angle = if (degreeMode) Math.toRadians(x) else x
        return when (name) {
            "sin" -> sin(angle)
            "cos" -> cos(angle)
            "tan" -> {
                val deg = if (degreeMode) ((x % 180) + 180) % 180 else null
                if (deg != null && abs(deg - 90.0) < 1e-10) throw CalcException("Undefined")
                tan(angle)
            }
            "asin" -> fromAngle(asin(requireDomain(x, -1.0, 1.0)))
            "acos" -> fromAngle(acos(requireDomain(x, -1.0, 1.0)))
            "atan" -> fromAngle(atan(x))
            "sinh" -> kotlin.math.sinh(x)
            "cosh" -> kotlin.math.cosh(x)
            "tanh" -> kotlin.math.tanh(x)
            "log" -> {
                if (x <= 0.0) throw CalcException("Invalid log")
                log10(x)
            }
            "ln" -> {
                if (x <= 0.0) throw CalcException("Invalid ln")
                ln(x)
            }
            "sqrt" -> {
                if (x < 0.0) throw CalcException("Invalid root")
                sqrt(x)
            }
            "cbrt" -> cbrt(x)
            "abs" -> abs(x)
            "exp" -> exp(x)
            else -> throw CalcException("Unknown function")
        }
    }

    private fun fromAngle(radians: Double): Double {
        return if (degreeMode) Math.toDegrees(radians) else radians
    }

    private fun requireDomain(value: Double, min: Double, max: Double): Double {
        if (value < min || value > max) throw CalcException("Out of domain")
        return value
    }

    private fun factorial(value: Double): Double {
        if (value < 0.0 || value != kotlin.math.floor(value)) {
            throw CalcException("Invalid factorial")
        }
        if (value > 170.0) throw CalcException("Overflow")
        var result = 1.0
        var n = value.toInt()
        while (n > 1) {
            result *= n
            n--
        }
        return result
    }

    fun formatNumber(value: Double): String {
        if (value == 0.0) return "0"
        val absValue = abs(value)
        if (absValue >= 1e12 || (absValue < 1e-9 && absValue > 0.0)) {
            return String.format("%.8e", value)
                .replace("e+", "e")
                .replace(Regex("(\\.\\d*?)0+(e)"), "$1$2")
                .replace("e", "e")
        }
        val text = String.format("%.10f", value)
        return text.trimEnd('0').trimEnd('.')
    }

    private class CalcException(message: String) : RuntimeException(message)

    companion object {
        private val ERRORS = setOf(
            "Error", "Math error", "Undefined", "Cannot divide by zero",
            "Invalid log", "Invalid ln", "Invalid root", "Invalid factorial",
            "Overflow", "Out of domain", "Incomplete expression",
            "Mismatched parentheses", "Invalid number", "Invalid character",
            "Unknown function", "Unknown operator", "Add x to your expression."
        )
    }
}
