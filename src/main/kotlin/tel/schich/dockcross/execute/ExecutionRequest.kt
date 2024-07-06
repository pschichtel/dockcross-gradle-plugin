package tel.schich.dockcross.execute

import java.nio.file.Path

data class ExecutionRequest(
    val image: String,
    val command: List<CharSequence>,
    val runAs: Pair<Int, Int>?,
    val mountSource: Path,
    val outputDir: Path,
    val workdir: Path,
    val toolchainHome: Path?,
    val containerName: String?,
    val extraEnv: Map<String, CharSequence> = emptyMap(),
    val unsafeWritableMountSource: Boolean,
)
