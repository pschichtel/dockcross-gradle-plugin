package tel.schich.dockcross.execute

interface ContainerRunner {
    fun run(cli: CliDispatcher, request: ExecutionRequest)

    companion object {
        const val JAVA_HOME_ENV = "JAVA_HOME"
        const val MOUNT_SOURCE_ENV = "MOUNT_SOURCE"
        const val OUTPUT_DIR_ENV = "OUTPUT_DIR"
    }
}
