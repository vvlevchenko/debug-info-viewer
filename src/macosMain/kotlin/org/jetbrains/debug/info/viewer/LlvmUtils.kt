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


internal val LLVMBool.isOk: Boolean
    get() = (this == 0)

internal val LLVMValueRef?.name :String?
    get() = this?.run { LLVMGetValueName(this)?.toKString()}

internal val LLVMBasicBlockRef?.name :String?
    get() = this?.run{LLVMGetBasicBlockName(this)?.toKString()}


internal val UNDEFINED = 0u

internal val LLVMValueRef?.location: DILocationRef?
    get() = this?.run{ LLVMInstructionGetDiLocation(this) }

internal val DILocationRef?.line: UInt
    get() = this?.run { LLVMLocationGetLine(this) } ?: UNDEFINED

internal val DILocationRef?.column: UInt
    get() = this?.run { LLVMLocationGetColumn(this) } ?: UNDEFINED

internal val DILocationRef?.inlinedAt: DILocationRef?
    get() = this?.run { LLVMLocationGetInlinedAt(this)}

internal val DILocationRef?.scope: DILocalScopeRef?
    get() = this?.run { LLVMLocationGetScope(this) }

internal val DILocalScopeRef?.file: DIFileRef?
    get() = this?.run { LLVMScopeGetFile(this) }

internal val LLVMValueRef?.opcode : LLVMOpcode?
    get() = this?.run {LLVMGetInstructionOpcode(this)}

internal val DILocalScopeRef?.subprogram : DISubprogramRef?
    get() = this?.run{ LLVMLocalScopeGetSubprogram(this)}


internal val DISubprogramRef?.name : String?
    get() = this?.run{ LLVMSubprogramGetName(this)?.toKString()}

internal val DISubprogramRef?.linkageName : String?
    get() = this?.run{ LLVMSubprogramGetLinkageName(this)?.toKString() }

internal val CPointerVarOf<CPointer<ByteVar>>.str: String
    get() = value?.toKString() ?: "<null>"

internal val DIFileRef?.name: String?
    get() = this?.run { LLVMFileGetFilename(this)?.toKString()}

open class LLVMIterator<T>(private val first:T?, private val last: T?, private val next:(T?)->T?):Iterator<T?> {
    private var reachedEnd = first == null
    private var current:T? = null
    override fun hasNext(): Boolean = !reachedEnd

    override fun next(): T {
        current ?: return first!!.also {
            current = first
            reachedEnd = first == last
        }
        current = next(current)
        if (current == null) println("current null ${first} ${last}")
        return current!!.also {
            reachedEnd = it == last
        }
    }
}

class FunctionIterator(module: LLVMModuleRef): LLVMIterator<LLVMValueRef>(LLVMGetFirstFunction(module), LLVMGetLastFunction(module), ::LLVMGetNextFunction)
class BasicBlockIterator(function:LLVMValueRef?):LLVMIterator<LLVMBasicBlockRef>(LLVMGetFirstBasicBlock(function), LLVMGetLastBasicBlock(function), ::LLVMGetNextBasicBlock)
class InstructionIterator(basicBlock: LLVMBasicBlockRef?):LLVMIterator<LLVMValueRef>(LLVMGetFirstInstruction(basicBlock), LLVMGetLastInstruction(basicBlock), ::LLVMGetNextInstruction)