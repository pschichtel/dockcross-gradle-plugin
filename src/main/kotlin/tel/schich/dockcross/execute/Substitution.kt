package tel.schich.dockcross.execute

import java.io.Serializable

abstract class Substituting : CharSequence, Serializable {
    override val length: Int
        get() = TODO("Not yet implemented")

    override fun get(index: Int): Char {
        TODO("Not yet implemented")
    }

    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence {
        TODO("Not yet implemented")
    }
}

internal data class SubstitutionInput(val mountSource: String, val outputDir: String, val javaHome: String?)

/**
 * This class is a bit of a trick: It implements CharSequence, so it can be used together with normal Strings
 * without losing too much type safety
 */
class SubstitutingString(
    val string: String,
) : Substituting() {
    companion object {
        val Syntax = """\$\{([^}]+)}""".toRegex()
    }
}

enum class Variable {
    MOUNT_SOURCE,
    OUTPUT_DIR,
    JAVA_HOME,
}

class VariableReference(
    val variable: Variable,
) : Substituting()

internal fun substituteString(s: CharSequence, input: SubstitutionInput): String {
    fun value(ref: Variable) = when (ref) {
        Variable.MOUNT_SOURCE -> input.mountSource
        Variable.OUTPUT_DIR -> input.outputDir
        Variable.JAVA_HOME -> input.javaHome.orEmpty()
    }

    return when (s) {
        is SubstitutingString -> {
            s.string.replace(SubstitutingString.Syntax) { match ->
                val variable = match.groupValues.getOrNull(1)
                    ?.ifEmpty { null }
                    ?.let { name -> enumValues<Variable>().find { it.name == name } }
                if (variable != null) {
                    value(variable)
                } else {
                    match.value
                }
            }
        }

        is VariableReference -> value(s.variable)

        else -> s.toString()
    }
}