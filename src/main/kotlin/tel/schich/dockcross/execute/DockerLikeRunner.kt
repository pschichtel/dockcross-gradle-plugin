package tel.schich.dockcross.execute

import org.gradle.api.GradleException
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

fun runLikeDocker(executable: String, cli: CliDispatcher, request: ExecutionRequest) {
    val mountPoint = "/work"
    val workdir = request.mountSource.relativize(request.workdir)
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
    val command = buildList {
        add(executable)
        add("run")
        add("--rm")
        add("--tty")
        request.containerName?.let {
            add("--name")
            add(it)
        }
        request.runAs?.let { (uid, gid) ->
            add("-u")
            add("$uid:$gid")
        }
        bindMount(request.mountSource.toString(), mountPoint)
        env("MOUNT_SOURCE", mountPoint)
        tmpfs(mountPoint = "/tmp")
        request.toolchainHome?.let {
            val path = "/java-toolchain"
            bindMount(it.toString(), path, readOnly = true)
            env("JAVA_HOME", path)
        }
        add("--workdir")
        add("$mountPoint/$workdir")
        add(request.image)
        addAll(request.command)
    }
    println("Command: ${command.joinToString(" ")}")
    cli.execute(Paths.get("."), command, emptyMap())
}

class DockerRunner(private val binary: String = "docker") : ContainerRunner {
    override fun run(cli: CliDispatcher, request: ExecutionRequest) {
        runLikeDocker(binary, cli, request)
    }
}
class PodmanRunner(private val binary: String = "docker") : ContainerRunner {
    override fun run(cli: CliDispatcher, request: ExecutionRequest) {
        runLikeDocker(binary, cli, request)
    }
}

object AutoDetectDockerLikeRunner : ContainerRunner {
    private val executable: Path by lazy {
        val path = System.getenv("PATH")?.ifEmpty { null }
        if (path == null) {
            throw GradleException("No \$PATH defined, unable to auto-detect podman or docker!")
        }
        val pathElements = path.split(File.pathSeparator).filter { it.isNotBlank() }.map { Paths.get(it) }

        findExecutableInPath(pathElements, name = "podman")
            ?: findExecutableInPath(pathElements, name = "docker")
            ?: throw GradleException("Found neither podman nor docker on the path!")
    }

    private fun findExecutableInPath(path: List<Path>, name: String): Path? {
        return path.asSequence()
            .map { it.resolve(name) }
            .filter { Files.exists(it) && Files.isExecutable(it) }
            .firstOrNull()
    }

    override fun run(cli: CliDispatcher, request: ExecutionRequest) {
        runLikeDocker(executable.toString(), cli, request)
    }
}
