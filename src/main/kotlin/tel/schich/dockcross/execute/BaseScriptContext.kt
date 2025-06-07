package tel.schich.dockcross.execute

import org.gradle.api.logging.Logger
import java.nio.file.Path

interface ScriptContext {
    val image: String
    val runAs: Pair<Int, Int>?
    val mountSource: Path
    val outputDir: Path
    val workDir: Path
    val toolchainHome: Path?
    val containerName: String?
    val extraEnv: Map<String, CharSequence>
    val unsafeWritableMountSource: Boolean
    val logger: Logger
}

data class BaseScriptContext(
    override val image: String,
    override val runAs: Pair<Int, Int>?,
    override val mountSource: Path,
    override val outputDir: Path,
    override val workDir: Path,
    override val toolchainHome: Path?,
    override val containerName: String?,
    override val extraEnv: Map<String, CharSequence> = emptyMap(),
    override val unsafeWritableMountSource: Boolean,
    override val logger: Logger,
) : ScriptContext
