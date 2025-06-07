package tel.schich.dockcross.execute

data class ExecutionRequest(
    val command: List<CharSequence>,
    val scriptContext: ScriptContext,
) : ScriptContext by scriptContext
