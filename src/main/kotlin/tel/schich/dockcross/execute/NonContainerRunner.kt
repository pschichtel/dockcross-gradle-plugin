package tel.schich.dockcross.execute

import tel.schich.dockcross.execute.ContainerRunner.Companion.JAVA_HOME_ENV
import tel.schich.dockcross.execute.ContainerRunner.Companion.MOUNT_SOURCE_ENV
import tel.schich.dockcross.execute.ContainerRunner.Companion.OUTPUT_DIR_ENV

object NonContainerRunner : ContainerRunner {
    override fun run(cli: CliDispatcher, request: ExecutionRequest) {
        val env = buildMap {
            putAll(request.extraEnv)
            put(MOUNT_SOURCE_ENV, request.mountSource.toString())
            put(OUTPUT_DIR_ENV, request.outputDir.toString())
            request.toolchainHome?.let {
                put(JAVA_HOME_ENV, it.toString())
            }
        }
        cli.execute(request.workdir, request.command, env)
    }
}
