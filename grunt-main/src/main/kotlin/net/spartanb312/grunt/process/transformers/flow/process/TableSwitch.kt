package net.spartanb312.grunt.process.transformers.flow.process

import net.spartanb312.grunt.process.transformers.flow.ControlflowTransformer
import net.spartanb312.grunt.utils.builder.*
import org.objectweb.asm.Label
import org.objectweb.asm.Type
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import kotlin.random.Random

object TableSwitch {

    fun generate(
        targetLabel: LabelNode,
        methodNode: MethodNode,
        returnType: Type,
        conditions: Int,
        reverse: Boolean
    ): InsnList {
        return insnList {
            val endCase = Random.nextInt()
            val startCase = endCase - conditions + 1
            val defLabel = Label()
            val startLabel = Label()
            val labels = buildList { repeat(conditions) { add(Label()) } }
            val trueIndex = labels.indices.random()
            val trueCase = startCase + trueIndex
            val key = Random.nextInt()
            LABEL(startLabel)
            INT(trueCase xor key)
            INT(key)
            IXOR
            +TableSwitchInsnNode(
                startCase,
                endCase,
                getLabelNode(defLabel),
                *labels.map { getLabelNode(it) }.toTypedArray()
            )
            labels.forEachIndexed { index, label ->
                LABEL(label)
                if (index == trueIndex) +ReplaceGoto.generate(targetLabel, methodNode, returnType, reverse)
                else {
                    if (ControlflowTransformer.trappedCase && Random.nextInt(100) <= ControlflowTransformer.trapChance) +ReplaceGoto.generate(
                        getLabelNode(labels.toMutableList().apply { remove(label) }.random()),
                        methodNode,
                        returnType,
                        reverse
                    ) else if (ControlflowTransformer.fakeLoop && Random.nextInt(100) <= ControlflowTransformer.loopChance) +ReplaceGoto.generate(
                        getLabelNode(if (Random.nextBoolean()) defLabel else startLabel),
                        methodNode,
                        returnType,
                        reverse
                    ) else +JunkCode.generate(
                        methodNode,
                        returnType,
                        Random.nextInt(ControlflowTransformer.maxJunkCode)
                    )
                }
            }
            LABEL(defLabel)
            +JunkCode.generate(methodNode, returnType, Random.nextInt(ControlflowTransformer.maxJunkCode))
        }
    }

}