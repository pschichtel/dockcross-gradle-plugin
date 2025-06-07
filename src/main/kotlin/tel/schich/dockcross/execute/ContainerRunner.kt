package tel.schich.dockcross.execute

import org.gradle.process.ExecOperations

interface ContainerRunner {
    fun run(cli: CliDispatcher, request: ExecutionRequest)

    fun preScript(cli: CliDispatcher, context: ScriptContext, execOps: ExecOperations) {}
    fun postScript(cli: CliDispatcher, context: ScriptContext, execOps: ExecOperations) {}

    companion object {
        const val JAVA_HOME_ENV = "JAVA_HOME"
        const val MOUNT_SOURCE_ENV = "MOUNT_SOURCE"
        const val OUTPUT_DIR_ENV = "OUTPUT_DIR"
    }
}
