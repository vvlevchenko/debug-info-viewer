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
import org.jetbrains.commandline.CommandlineOption
import llvm.*
import org.jetbrains.commandline.BaseCommandlineParser

class ViewerCommandlineParser {
    val bitcode:String
        get() = options[bitcodeFile]!!
    val prefix:String
        get() = options[functionPrefix]!!

    private val options = mutableMapOf<CommandlineOption, String> ()
    private val oneValue = fun (op:CommandlineOption, args:Array<String>){
        options[op] = args[0]
    }
    private val bitcodeFile = CommandlineOption(longOptionName = "--bitcode-file", shortOptionName = "-b", numberOfParameters = 1, parse = oneValue)
    private val functionPrefix = CommandlineOption(longOptionName = "--prefix", shortOptionName = "-p", numberOfParameters = 1, parse = oneValue)

    fun parse(args:Array<String>) {
        BaseCommandlineParser(options = listOf(bitcodeFile, functionPrefix), freeArgument = ::unsupported, noArguments = ::help).parse(args)
    }

    private fun help() {

    }

    private fun unsupported(argument: String) {

    }
}

fun main(args: Array<String>) {
    val parser = ViewerCommandlineParser().also { it.parse(args) }
    memScoped {
        val messageBuffer = allocPointerTo<ByteVar>()
        val buffer = alloc<LLVMMemoryBufferRefVar>()

        if (!LLVMCreateMemoryBufferWithContentsOfFile(parser.bitcode, buffer.ptr, messageBuffer.ptr).isOk) {
            error(messageBuffer.str)
        }

        val module = alloc<LLVMModuleRefVar>()
        if (!LLVMParseBitcode(buffer.value, module.ptr, messageBuffer.ptr).isOk) {
            error(messageBuffer.str)
        }

        val filtered = FunctionIterator(module.value!!).asSequence().asIterable().filter { it.name?.startsWith(parser.prefix) ?: false}

        val dia = digraph("zzz") {
            filtered.forEach {

                graph(it.name ?: "function-${nodeCounter++}", LLVMGetEntryBasicBlock(it)?.name ?: "prologue") {
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
        buildString {
            appendln("digraph ${dia.name} {")
            appendln("node [labeljust=l, shape=record, style=filled, color=red, fillcolor=gray, fontcolor=black]")
            dia.nodes.forEach { subgraph ->
                appendln("subgraph {")
                appendln("\"${subgraph.key}\" -> ${(subgraph.value as Graph).entry}")
                (subgraph.value as Graph).nodes.forEach { node ->
                    appendln("${node.key} [ label = \"{${node.key}|${node.value.attributes["label"]?.value?.joinToString(separator = "|")}}\"]")
                    if (node.value.edges.isNotEmpty())
                        appendln("${node.key} -> ${node.value.edges.joinToString(separator = ",")}")
                }

                appendln("}")
            }
            appendln("}")
        }.also (::println)
        LLVMDisposeModule(module.value)
        LLVMDisposeMemoryBuffer(buffer.value)
    }
}

private val LLVMOpcode?.beauty_name: String?
    get() = this?.run { name.drop(4).decapitalize() }
val DILocationRef?.encoded: String?
    get() = this?.run{"${scope.subprogram.linkageName}|${scope.file.name}|$line:$column"}

