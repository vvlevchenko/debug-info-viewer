/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

#ifndef __DEBUG_INFO_C_H__
# define __DEBUG_INFO_C_H__
#include <llvm-c/Core.h>
# ifdef __cplusplus
extern "C" {
# endif

typedef struct DILocation *DILocationRef;
typedef struct DILocalScope *DILocalScopeRef;
typedef struct DIFile *DIFileRef;
//typedef struct DIScope *DIScopeRef;
typedef struct DISubprogram *DISubprogramRef;

unsigned LLVMInstructionBrGetNumSuccessors(LLVMValueRef);
LLVMBasicBlockRef LLVMInstructionBrGetSuccessor(LLVMValueRef, unsigned);
LLVMBasicBlockRef LLVMInstructionInvokeGetNormalDest(LLVMValueRef);
LLVMBasicBlockRef LLVMInstructionInvokeGetUnwindDest(LLVMValueRef);
DILocationRef LLVMInstructionGetDiLocation(LLVMValueRef);
unsigned LLVMLocationGetLine(DILocationRef);
unsigned LLVMLocationGetColumn(DILocationRef);
DILocalScopeRef LLVMLocationGetScope(DILocationRef);
DILocationRef LLVMLocationGetInlinedAt(DILocationRef);
DIFileRef LLVMScopeGetFile(DILocalScopeRef);
const char *LLVMFileGetFilename(DIFileRef);
const char *LLVMFileGetDirectory(DIFileRef);
# ifdef __cplusplus
}
# endif
#endif
