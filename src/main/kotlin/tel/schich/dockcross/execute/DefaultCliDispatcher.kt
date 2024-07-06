package tel.schich.dockcross.execute

import org.gradle.api.GradleException
import org.gradle.process.ExecOperations
import java.nio.file.Path

class DefaultCliDispatcher(private val execOps: ExecOperations) : CliDispatcher {
    override fun execute(workdir: Path, command: List<String>, extraEnv: Map<String, String>) {
        val result = execOps.exec {
            commandLine(command)
            workingDir(workdir)
            environment(extraEnv)
        }
        if (result.exitValue != 0) {
            throw GradleException("Command failed: $result")
        }
    }
}
