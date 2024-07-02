package tel.schich.dockcross.execute

object NonContainerRunner : ContainerRunner {
    override fun run(cli: CliDispatcher, request: ExecutionRequest) {
        val env = buildMap {
            put("MOUNT_SOURCE", request.mountSource.toString())
            request.toolchainHome?.let {
                put("JAVA_HOME", it.toString())
            }
        }
        cli.execute(request.workdir, request.command, env)
    }
}
