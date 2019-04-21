/*
 * Copyright 2010-2019 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.debug.info.viewer

import kotlinx.cinterop.*
import llvm.*


fun main(args: Array<String>) {
    val fileName = args[0]
    val functionName = args[1]
    memScoped {
        val messageBuffer = allocPointerTo<ByteVar>()
        val buffer = alloc<LLVMMemoryBufferRefVar>()

        if (!LLVMCreateMemoryBufferWithContentsOfFile(fileName, buffer.ptr, messageBuffer.ptr).isOk) {
            error(messageBuffer.str)
        }

        val module = alloc<LLVMModuleRefVar>()
        if (!LLVMParseBitcode(buffer.value, module.ptr, messageBuffer.ptr).isOk) {
            error(messageBuffer.str)
        }

        val filtered = FunctionIterator(module.value!!).asSequence().asIterable().filter { it.name?.startsWith(functionName) ?: false}

        val dia = digraph("zzz") {
            filtered.forEach {
                graph(it.name ?: "function-${nodeCounter++}") {
                    BasicBlockIterator(it).forEach { bb ->
                        node(bb.name ?: "bb-${nodeCounter++}") {
                            attribute("label") {
                                InstructionIterator(bb).forEach { i ->
                                    val location = i.location
                                    val inlinedAt = location.inlinedAt
                                    value.add("{${i.opcode?.beauty_name} | ${location.encoded} ${inlinedAt?.run{"|$encoded"}?:""}}")
                                    when(i.opcode) {
                                        LLVMOpcode.LLVMInvoke -> {
                                            edge(LLVMInstructionInvokeGetNormalDest(i).name!!)
                                            edge(LLVMInstructionInvokeGetUnwindDest(i).name!!)
                                        }
                                        LLVMOpcode.LLVMBr -> {
                                            val successors = LLVMInstructionBrGetNumSuccessors(i)
                                            for (s in 0u until successors) {
                                                edge(LLVMInstructionBrGetSuccessor(i, s).name!!)
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        LLVMDisposeModule(module.value)
        LLVMDisposeMemoryBuffer(buffer.value)
        buildString {
            appendln("digraph ${dia.name} {")
            appendln("node [labeljust=l, shape=record, style=filled, color=red, fillcolor=gray, fontcolor=black]")
            dia.nodes.forEach {
                appendln("subgraph {")
                appendln("\"${it.key}\" -> prologue") // specific
                (it.value as Graph).nodes.forEach {
                    appendln("${it.key} [ label = \"{${it.key}|${it.value.attributes["label"]?.value?.joinToString(separator = "|")}}\"]")
                    if (it.value.edges.isNotEmpty())
                        appendln("${it.key} -> ${it.value.edges.joinToString(separator = ",")}")
                }

                appendln("}")
            }
            appendln("}")
        }.also (::println)
    }
}

private val LLVMOpcode?.beauty_name: String?
    get() = this?.run { name.drop(4).decapitalize() }
val DILocationRef?.encoded: String?
    get() = this?.run{"${scope.file.name}|$line:$column"}

