package tel.schich.dockcross.execute

import org.gradle.api.GradleException
import tel.schich.dockcross.execute.ContainerRunner.Companion.JAVA_HOME_ENV
import tel.schich.dockcross.execute.ContainerRunner.Companion.MOUNT_SOURCE_ENV
import tel.schich.dockcross.execute.ContainerRunner.Companion.OUTPUT_DIR_ENV

object NonContainerRunner : ContainerRunner {
    override fun run(cli: CliDispatcher, request: ExecutionRequest) {
        val substitutionInput = SubstitutionInput(
            mountSource = request.mountSource.toString(),
            outputDir = request.outputDir.toString(),
            javaHome = request.toolchainHome?.toString()
        )
        if (!request.unsafeWritableMountSource) {
            throw GradleException("The ${NonContainerRunner::class.simpleName} is always unsafe!")
        }
        val env = buildMap {
            putAll(request.extraEnv)
            put(MOUNT_SOURCE_ENV, request.mountSource.toString())
            put(OUTPUT_DIR_ENV, request.outputDir.toString())
            request.toolchainHome?.let {
                put(JAVA_HOME_ENV, it.toString())
            }
        }.mapValues { (_, value) -> substituteString(value, substitutionInput) }
        val command = request.command.map { substituteString(it, substitutionInput) }

        cli.execute(request.workdir, command, env)
    }
}
