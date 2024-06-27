package net.spartanb312.grunt.process.transformers.rename

import net.spartanb312.grunt.config.value
import net.spartanb312.grunt.process.Transformer
import net.spartanb312.grunt.process.hierarchy.FastHierarchy
import net.spartanb312.grunt.process.resource.NameGenerator
import net.spartanb312.grunt.process.resource.ResourceCache
import net.spartanb312.grunt.process.transformers.rename.MethodRenameTransformer.transform
import net.spartanb312.grunt.utils.count
import net.spartanb312.grunt.utils.extensions.isAnnotation
import net.spartanb312.grunt.utils.isExcludedIn
import net.spartanb312.grunt.utils.isNotExcludedIn
import net.spartanb312.grunt.utils.logging.Logger
import net.spartanb312.grunt.utils.nextBadKeyword
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap
import kotlin.system.measureTimeMillis

/**
 * Renaming fields
 * Last update on 2024/06/27
 */
object FieldRenameTransformer : Transformer("FiledRename", Category.Renaming) {

    private val dictionary by value("Dictionary", "Alphabet")
    private val randomKeywordPrefix by value("RandomKeywordPrefix", false)
    private val prefix by value("Prefix", "")
    private val exclusion by value("Exclusion", listOf())
    private val excludedName by value("ExcludedName", listOf("INSTANCE", "Companion"))

    private val malPrefix = (if (randomKeywordPrefix) "$nextBadKeyword " else "") + prefix

    override fun ResourceCache.transform() {
        Logger.info(" - Renaming fields...")

        val mappings = HashMap<String, String>()
        val fields: MutableList<FieldNode> = ArrayList()
        nonExcluded.forEach { fields.addAll(it.fields) }
        fields.shuffle()

        val dictionaries = ConcurrentHashMap<ClassNode?, NameGenerator>()
        val count = count {
            for (fieldNode in fields) {
                if (fieldNode.name.isExcludedIn(excludedName)) continue
                val c = getOwner(fieldNode, classes)
                val dic = dictionaries.getOrPut(c) { NameGenerator.getByName(dictionary) }
                val name = malPrefix + dic.nextName()
                val stack: Stack<ClassNode> = Stack()
                stack.add(c)
                while (stack.size > 0) {
                    val classNode = stack.pop()
                    val key = classNode.name + "." + fieldNode.name
                    if (key.isNotExcludedIn(exclusion)) {
                        mappings[key] = name
                    }
                    classes.values.forEach {
                        if (it.superName == classNode.name || it.interfaces.contains(classNode.name))
                            stack.add(it)
                    }
                }
                add()
            }
        }.get()

        Logger.info("    Applying remapping for fields...")
        applyRemap("fields", mappings)

        Logger.info("    Renamed $count fields")
    }

    private fun getOwner(f: FieldNode, classNodes: MutableMap<String, ClassNode>): ClassNode? {
        for (c in classNodes.values) if (c.fields.contains(f)) return c
        return null
    }

}