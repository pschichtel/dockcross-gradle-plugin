package tel.schich.dockcross.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.listProperty
import org.gradle.kotlin.dsl.property
import org.gradle.process.ExecOperations
import tel.schich.dockcross.execute.AutoDetectDockerLikeRunner
import tel.schich.dockcross.execute.ContainerRunner
import tel.schich.dockcross.execute.DefaultCliDispatcher
import tel.schich.dockcross.execute.ExecutionRequest
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject

abstract class DockcrossRunTask @Inject constructor(private val execOps: ExecOperations) : DefaultTask() {
    @get:InputDirectory
    val mountSource: DirectoryProperty = project.objects.directoryProperty()

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
    val script: ListProperty<List<String>> = project.objects.listProperty()

    @Optional
    @get:InputDirectory
    val javaHome: DirectoryProperty = project.objects.directoryProperty()

    @get:OutputDirectory
    val output: DirectoryProperty = project.objects.directoryProperty()

    private var runner: ContainerRunner = AutoDetectDockerLikeRunner

    init {
        mountSource.convention(project.layout.projectDirectory)
        dockcrossTag.convention("latest")
        dockcrossRepository.convention("docker.io/dockcross/{image}")
        output.convention(project.layout.buildDirectory)
        group = "build"
    }

    /**
     * Configures a different ContainerRunner for this task.
     */
    fun runner(runner: ContainerRunner) {
        this.runner = runner
    }

    @TaskAction
    fun run() {
        val mountSource = mountSource.get().asFile.toPath().toRealPath()
        val outputPath = output.get().asFile.toPath().toRealPath()
        if (!outputPath.startsWith(mountSource)) {
            throw GradleException("The output path $outputPath is not inside the mount source $mountSource!")
        }
        Files.createDirectories(outputPath)
        val dispatcher = DefaultCliDispatcher(execOps)
        val toolchainHome = javaHome.orNull?.asFile?.toPath()
            ?: System.getenv("JAVA_HOME")?.ifEmpty { null }?.let { Paths.get(it) }

        val arch = image.get()
        val repo = dockcrossRepository.get().replace("{image}", arch)
        val image = "$repo:${dockcrossTag.get()}"
        for (command in script.get()) {
            val request = ExecutionRequest(
                image = image,
                containerName = containerName.orNull?.ifEmpty { null },
                command = command,
                runAs = null,
                mountSource = mountSource,
                outputDir = outputPath,
                workdir = outputPath,
                toolchainHome = toolchainHome
            )
            runner.run(dispatcher, request)
        }
    }
}
