package tel.schich.dockcross.execute

import org.gradle.api.GradleException
import tel.schich.dockcross.execute.ContainerRunner.Companion.JAVA_HOME_ENV
import tel.schich.dockcross.execute.ContainerRunner.Companion.MOUNT_SOURCE_ENV
import tel.schich.dockcross.execute.ContainerRunner.Companion.OUTPUT_DIR_ENV
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

enum class DockerMode {
    DOCKER,
    PODMAN,
}

fun runLikeDocker(executable: String, mode: DockerMode, cli: CliDispatcher, request: ExecutionRequest) {
    val javaMountPoint = Paths.get("/java-toolchain")
    val mountPoint = Paths.get("/work")
    val outputDir = mountPoint.resolve(request.mountSource.relativize(request.outputDir))
    val workdir = mountPoint.resolve(request.mountSource.relativize(request.workdir))
    fun MutableList<String>.bindMount(from: String, to: String = from, readOnly: Boolean = false) {
        val roFlag = if (readOnly) ":ro" else ""
        add("-v")
        add("$from:$to$roFlag")
    }
    fun MutableList<String>.tmpfs(mountPoint: String) {
        add("--mount")
        add("type=tmpfs,destination=$mountPoint")
    }
    fun MutableList<String>.env(name: String, value: String) {
        add("-e")
        add("$name=$value")
    }

    val substitutionInput = SubstitutionInput(
        mountSource = mountPoint.toString(),
        outputDir = outputDir.toString(),
        javaHome = if (request.toolchainHome != null) javaMountPoint.toString() else "",
    )

    val command = buildList {
        add(executable)
        add("run")
        add("--rm")
        add("--tty")
        request.containerName?.let {
            add("--name")
            add(it)
        }
        if (mode == DockerMode.PODMAN) {
            add("--userns=keep-id")
        }
        if (request.runAs != null) {
            add("-u")
            add("${request.runAs.first}:${request.runAs.second}")
        }
        for ((name, value) in request.extraEnv) {
            env(name, substituteString(value, substitutionInput))
        }
        bindMount(request.mountSource.toString(), mountPoint.toString(), readOnly = !request.unsafeWritableMountSource)
        env(MOUNT_SOURCE_ENV, mountPoint.toString())
        bindMount(request.outputDir.toString(), outputDir.toString())
        env(OUTPUT_DIR_ENV, outputDir.toString())
        tmpfs(mountPoint = "/tmp")
        request.toolchainHome?.let {
            bindMount(it.toString(), javaMountPoint.toString(), readOnly = true)
            env(JAVA_HOME_ENV, javaMountPoint.toString())
        }
        add("--workdir")
        add(workdir.toString())
        add(request.image)
        addAll(request.command.map { substituteString(it, substitutionInput) })
    }
    println("Command: ${command.joinToString(" ")}")
    cli.execute(Paths.get("."), command, emptyMap())
}

class DockerRunner(private val binary: String = "docker") : ContainerRunner {
    override fun run(cli: CliDispatcher, request: ExecutionRequest) {
        runLikeDocker(binary, DockerMode.DOCKER, cli, request)
    }
}
class PodmanRunner(private val binary: String = "docker") : ContainerRunner {
    override fun run(cli: CliDispatcher, request: ExecutionRequest) {
        runLikeDocker(binary, DockerMode.PODMAN, cli, request)
    }
}

object AutoDetectDockerLikeRunner : ContainerRunner {
    private val executable: Pair<Path, DockerMode> by lazy {
        val path = System.getenv("PATH")?.ifEmpty { null }
        if (path == null) {
            throw GradleException("No \$PATH defined, unable to auto-detect podman or docker!")
        }
        val pathElements = path.split(File.pathSeparator).filter { it.isNotBlank() }.map { Paths.get(it) }

        findExecutableInPath(pathElements, name = "podman")?.let { it to DockerMode.PODMAN }
            ?: findExecutableInPath(pathElements, name = "docker")?.let { it to DockerMode.DOCKER }
            ?: throw GradleException("Found neither podman nor docker on the path!")
    }

    private fun findExecutableInPath(path: List<Path>, name: String): Path? {
        return path.asSequence()
            .map { it.resolve(name) }
            .filter { Files.exists(it) && Files.isExecutable(it) }
            .firstOrNull()
    }

    override fun run(cli: CliDispatcher, request: ExecutionRequest) {
        runLikeDocker(executable.first.toString(), executable.second, cli, request)
    }
}
