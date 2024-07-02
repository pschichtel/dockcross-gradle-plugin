Dockcross Gradle Plugin
=======================

This is a very simple gradle plugin that runs arbitrary commands inside [dockcross](https://github.com/dockcross/dockcross) images using, by default, other podman or docker.

The primary purpose of this plugin is cross-compilation of native components for JNI. An example of the plugin in action is [JavaCAN](https://github.com/pschichtel/JavaCAN).

Usage
-----

Apply the plugin in any build that defines run tasks, e.g. like this:

```kotlin
plugins {
    id("tel.schich.dockcross") version "<latest version>"
}
```

The plugin does _not_ register tasks by default as it does not assume anything about your native compilation setup.

Register your own task of type `DockcrossRunTask` like this:

```kotlin
val build_x86_64 by tasks.registering(DockcrossRunTask::class) {
    // declare your inputs
    inputs.dir(project.rootProject.layout.projectDirectory.dir("src/main/c"))
    
    // the dockcross/... image from dockerhub to be used
    image = "linux-x64"

    // optional but recommended: Set the javaHome to your toolchain's installationPath
    javaHome = javaToolchains.launcherFor(java.toolchain).map { it.metadata.installationPath }

    // the sequence of commands to be executed in the container
    // checkout the script execution section below for additional details
    script = listOf(
        listOf("cmake", "build"),
        listOf("make", "-j${project.gradle.startParameter.maxWorkerCount}"),
    )
}
```

### Script Execution

Each command listed in the script is executed in a dedicated container.
This enforces that state is only retained between commands, if it is placed in the output directory.
If a java home can be determined either through explicit configuration our by reading the `JAVA_HOME` env var, then
within the container `JAVA_HOME` will point to a path (not the same path) with that JDK mounted to it as read-only.
The env var `MOUNT_SOURCE` will always point to the path where the mountSource is mounted to.

When using `runner(NonContainerRunner)`, no dockcross container is used to run the command, instead it is executed as is
on the machine gradle is executed on. Environment variables are configured in the same way so commands should run
similarly. This can be useful e.g. as a "host architecture" compilation target used for running unit tests.