package tel.schich.dockcross.tasks

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.mapProperty
import org.gradle.kotlin.dsl.property
import org.gradle.process.ExecOperations
import tel.schich.dockcross.execute.AutoDetectDockerLikeRunner
import tel.schich.dockcross.execute.ContainerRunner
import tel.schich.dockcross.execute.DefaultCliDispatcher
import tel.schich.dockcross.execute.ExecutionRequest
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import javax.inject.Inject

abstract class DockcrossRunTask @Inject constructor(private val execOps: ExecOperations) : DefaultTask() {
    @get:Input
    val mountSource: Property<File> = project.objects.property()

    @Optional
    @get:Input
    val containerName: Property<String> = project.objects.property()

    @get:Input
    val image: Property<String> = project.objects.property()

    @get:Input
    val dockcrossTag: Property<String> = project.objects.property()

    @get:Input
    val dockcrossRepository: Property<String> = project.objects.property()

    @get:Input
    val script: ListProperty<List<CharSequence>> = project.objects.listProperty()

    @get:Input
    val extraEnv: MapProperty<String, CharSequence> = project.objects.mapProperty()

    @get:Input
    val unsafeWritableMountSource: Property<Boolean> = project.objects.property()

    @Optional
    @get:InputDirectory
    val javaHome: DirectoryProperty = project.objects.directoryProperty()

    @get:OutputDirectory
    val output: DirectoryProperty = project.objects.directoryProperty()

    private var runner: ContainerRunner = AutoDetectDockerLikeRunner

    init {
        mountSource.convention(project.layout.projectDirectory.asFile)
        dockcrossTag.convention("latest")
        dockcrossRepository.convention("docker.io/dockcross/{image}")
        output.convention(project.layout.buildDirectory)
        extraEnv.convention(emptyMap())
        unsafeWritableMountSource.convention(false)
        group = "build"
    }

    /**
     * Configures a different ContainerRunner for this task.
     */
    fun runner(runner: ContainerRunner) {
        this.runner = runner
    }

    private fun run(cmd: List<String>): String? {
        val stdout = ByteArrayOutputStream()
        val result = execOps.exec {
            commandLine(cmd)
            standardOutput = stdout
        }

        if (result.exitValue != 0) {
            return null
        }

        return try {
            String(stdout.toByteArray(), Charsets.UTF_8)
        } catch (_: RuntimeException) {
            null
        }
    }

    private fun detectUser(): Pair<Int, Int>? {
        if (Os.isFamily(Os.FAMILY_WINDOWS)) {
            return null
        }
        val uid = run(listOf("id", "-u"))?.trim()?.toIntOrNull() ?: return null
        val gid = run(listOf("id", "-g"))?.trim()?.toIntOrNull() ?: return null

        return uid to gid
    }

    @TaskAction
    fun run() {
        val mountSource = mountSource.get().toPath().toRealPath()
        val outputPath = output.get().asFile.toPath().toRealPath()
        if (!outputPath.startsWith(mountSource)) {
            throw GradleException("The output path $outputPath is not inside the mount source $mountSource!")
        }
        Files.createDirectories(outputPath)
        val dispatcher = DefaultCliDispatcher(execOps)
        val toolchainHome = javaHome.orNull?.asFile?.toPath()

        val arch = image.get()
        val repo = dockcrossRepository.get().replace("{image}", arch)
        val image = "$repo:${dockcrossTag.get()}"
        for (command in script.get()) {
            val request = ExecutionRequest(
                image = image,
                containerName = containerName.orNull?.ifEmpty { null },
                command = command,
                runAs = detectUser(),
                mountSource = mountSource,
                outputDir = outputPath,
                workDir = outputPath,
                toolchainHome = toolchainHome,
                extraEnv = extraEnv.get(),
                unsafeWritableMountSource = unsafeWritableMountSource.get(),
            )
            runner.run(dispatcher, request)
        }
    }
}
