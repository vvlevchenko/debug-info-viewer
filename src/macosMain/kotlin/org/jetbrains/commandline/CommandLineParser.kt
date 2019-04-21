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