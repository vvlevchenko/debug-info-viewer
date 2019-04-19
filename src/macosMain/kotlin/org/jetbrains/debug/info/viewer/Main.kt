package org.jetbrains.debug.info.viewer

import kotlinx.cinterop.*
import llvm.*


fun main(args: Array<String>) {
    val fileName = args[0]
    val functionName = "kfun:foo" // configurable parameter
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

fun digraph(name: String, body: Digraph.() -> Unit) = Digraph(name).also(body)

class Digraph(name :String):Graph(name)

open class Graph(name: String):Node(name) {
    var nodeCounter = 0
    val nodes = mutableMapOf<String, Node>()
    fun add(node:Node) = nodes.put(node.name, node)
}

open class Node(val name: String) {
    val attributes = mutableMapOf<String, Attribute>()
    val edges = mutableListOf<String>()
    fun add(a:Attribute) = attributes.put(a.id, a)
    fun edge(name: String) = edges.add(name)
}

class Attribute(val id: String) {
    var value = mutableListOf<String>()
}

fun Graph.graph(name: String, body: Graph.() -> Unit) = this.add(Graph(name).also(body))
fun Graph.node(id : String, body: Node.() -> Unit) = this.add(Node(id).also(body))
fun Node.attribute(id: String, body:Attribute.() -> Unit) = this.add(Attribute(id).also(body))

private val LLVMOpcode?.beauty_name: String?
    get() = this?.run { name.drop(4).decapitalize() }
val DILocationRef?.encoded: String?
    get() = this?.run{"${scope.file.name}|$line:$column"}

