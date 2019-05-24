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

fun digraph(name: String, body: Digraph.() -> Unit) = Digraph(name).also(body)

class Digraph(name :String):Graph(name)

open class Graph(name: String, val entry: String? = null):Node(name) {
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

fun Graph.graph(name: String, entry:String? = null, body: Graph.() -> Unit) = this.add(Graph(name, entry).also(body))
fun Graph.node(id : String, body: Node.() -> Unit) = this.add(Node(id).also(body))
fun Node.attribute(id: String, body:Attribute.() -> Unit) = this.add(Attribute(id).also(body))

fun String.html() = replace("<", "&lt;").replace(">", "&gt;")

