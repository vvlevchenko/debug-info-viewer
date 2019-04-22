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
package org.jetbrains.commandline

class CommandlineOption(val longOptionName:String? = null, val shortOptionName:String? = null, val numberOfParameters:Int, val parse: ((CommandlineOption, Array<String>)->Unit)? = null)
open class BaseCommandlineParser(private val options: List<CommandlineOption>, private val freeArgument:((String)->Unit)? = null, private val noArguments:(()->Unit)? = null) {
    fun parse(args: Array<String>) {
        if (args.isEmpty())
            noArguments?.invoke() ?: error("no arguments passed")
        else
            parseHelper(args, 0)
    }

    private fun parseHelper(args: Array<String>, i: Int) {
        var offset = i + 1
        if (i == args.size)
            return
        if (args[i].startsWith('-')) {
            options.find { it.longOptionName == args[i] || it.shortOptionName == args[i]}?.run {
                this.parse?.invoke(this, args.copyOfRange(offset, offset + this.numberOfParameters))
                offset += this.numberOfParameters
            }
        } else
            freeArgument?.invoke(args[i]) ?: error("free arguments unsupported")
        parseHelper(args, offset)
    }
}