package tel.schich.dockcross.execute

import org.gradle.process.ExecOperations
import tel.schich.dockcross.execute.ContainerRunner.Companion.JAVA_HOME_ENV
import tel.schich.dockcross.execute.ContainerRunner.Companion.MOUNT_SOURCE_ENV
import tel.schich.dockcross.execute.ContainerRunner.Companion.OUTPUT_DIR_ENV
import java.io.File
import java.net.URI
import java.nio.file.Path

class RemoteSshRunner(
    sshTarget: URI,
    private val sendEnv: Boolean = false,
    private val rsyncUploadCustomizer: CommandBuilder.() -> Unit = {},
) : ContainerRunner {
    private val sshUri = URI(sshTarget.scheme, sshTarget.userInfo, sshTarget.host, sshTarget.port, null, null, null)
    private val sshWorkDir = sshTarget.path
    private val sshPort = sshTarget.port
    private val rsyncSshPortOptions = if (sshPort != -1) {
        listOf("-e", "ssh -p $sshPort")
    } else {
        emptyList()
    }
    private val rsyncSshTarget = buildString {
        sshTarget.userInfo?.let {
            append(it)
            append("@")
        }
        append(sshTarget.host)
    }

    private fun simpleQuote(s: CharSequence): String =
        "'" + s.replace(singleQuoteRegex, "\\$1") + "'"

    private fun simpleQuote(s: CharSequence, input: SubstitutionInput): String {
        return if (sendEnv) {
            when (s) {
                is SubstitutingString -> "\"" + s.string.replace(doubleQuoteRegex, "\\$1") + "\""
                is VariableReference -> when (s.variable) {
                    Variable.MOUNT_SOURCE -> "\"\${MOUNT_SOURCE}\""
                    Variable.OUTPUT_DIR -> "\"\${OUTPUT_DIR}\""
                    Variable.JAVA_HOME -> "\"\${JAVA_HOME}\""
                }
                else -> simpleQuote(s)
            }
        } else {
            val substitutedString = if (s is Substituting) {
                substituteVariables(s, input)
            } else {
                s
            }
            simpleQuote(substitutedString)
        }
    }

    private fun ExecOperations.runRsyncCommand(context: ScriptContext, block: CommandBuilder.() -> Unit) {
        val rsyncCommand = buildList {
            add("rsync")
            add("-aP")
            addAll(rsyncSshPortOptions)
            CommandBuilder(this, context).block()
        }
        context.logger.lifecycle("Executing command: ${rsyncCommand.joinToString(" ") { "\'$it\'" }}")
        exec {
            commandLine(rsyncCommand)
        }
    }

    override fun preScript(cli: CliDispatcher, context: ScriptContext, execOps: ExecOperations) {
        execOps.runRsyncCommand(context) {
            rsyncUploadCustomizer()
            add("${context.mountSource}${File.separator}.")
            add("$rsyncSshTarget:$sshWorkDir")
        }
    }

    override fun run(
        cli: CliDispatcher,
        request: ExecutionRequest
    ) {
        val relativeOutputDir = request.mountSource.relativize(request.outputDir)
        val relativeWorkDir = request.mountSource.relativize(request.workDir)
        val outputDir = "$sshWorkDir/$relativeOutputDir"
        val sshEnv = buildMap {
            put(MOUNT_SOURCE_ENV, sshWorkDir)
            put(OUTPUT_DIR_ENV, outputDir)
            request.toolchainHome?.let {
                put(JAVA_HOME_ENV, it.toString())
            }
        }
        val completeEnv = buildMap {
            if (!sendEnv) {
                putAll(sshEnv)
            }
            putAll(request.extraEnv)
        }
        val substitutionInput = SubstitutionInput(
            mountSource = sshWorkDir,
            outputDir = outputDir,
            javaHome = request.toolchainHome?.toString(),
        )
        val encodedEnv = completeEnv.map { "${it.key}=${simpleQuote(it.value, substitutionInput)}" }.joinToString(separator = " ")
        val encodedCommand = request.command.joinToString(separator = " ") { simpleQuote(it, substitutionInput) }
        val encodedWorkDir = simpleQuote("$sshWorkDir/$relativeWorkDir")
        val sshCommand = "mkdir -p $encodedWorkDir; cd $encodedWorkDir; $encodedEnv $encodedCommand"
        val fullCommand = listOf("ssh", sshUri.toString(), sshCommand)
        request.logger.lifecycle("Executing command: ${fullCommand.joinToString(" ") { it }}")
        cli.execute(request.workDir, fullCommand, sshEnv)
    }

    override fun postScript(cli: CliDispatcher, context: ScriptContext, execOps: ExecOperations) {
        val relativeOutputDir = context.mountSource.relativize(context.outputDir)
        val outputDir = "$sshWorkDir/$relativeOutputDir"
        execOps.runRsyncCommand(context) {
            rsyncUploadCustomizer()
            add("$rsyncSshTarget:$outputDir/.")
            add("${context.outputDir}")
        }
    }

    inner class CommandBuilder(val list: MutableList<CharSequence>, val context: ScriptContext) : ScriptContext by context {
        fun add(vararg s: CharSequence) {
            list.addAll(s)
        }

        fun add(s: Iterable<CharSequence>) {
            list.addAll(s)
        }

        fun resolvePath(path: Path) = mountSource.relativize(path).toString()
    }

    private companion object {
        private val singleQuoteRegex = "(')".toRegex()
        private val doubleQuoteRegex = "(\")".toRegex()
    }
}