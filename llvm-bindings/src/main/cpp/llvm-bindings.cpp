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

#include <llvm/IR/DebugInfo.h>
#include <llvm/IR/Function.h>
#include <llvm/IR/IRBuilder.h>
#include <llvm/IR/DIBuilder.h>
#include <llvm/IR/DebugInfoMetadata.h>
#include <llvm/IR/Instruction.h>
#include <llvm/Support/Casting.h>
#include "llvm-bindings.h"

#include <stdio.h>
namespace llvm {
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DILocation, DILocationRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DILocalScope, DILocalScopeRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIFile, DIFileRef)
DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DISubprogram, DISubprogramRef)
//DEFINE_SIMPLE_CONVERSION_FUNCTIONS(DIScope, DIScopeRef)
}
//#define DI_DEBUG
#ifdef DI_DEBUG
namespace {
  inline const char * fileName(llvm::DIFile *file) {
    if (file == NULL) {
      return "no file";
    }
    auto fileName = file->getFilename();
    return fileName.str().c_str();
  }
}
#define return(x) do {\
  printf("(%s: %d) " #x " %s\n", __FILE__, __LINE__, (x));\
  return x;\
} while (0)
#else
#define return(x) return x
#endif

extern "C" {
  DILocationRef LLVMInstructionGetDiLocation(LLVMValueRef ref) {
    return llvm::wrap(llvm::unwrap<llvm::Instruction>(ref)->getDebugLoc().get());
  }

  unsigned LLVMInstructionBrGetNumSuccessors(LLVMValueRef ref) {
    auto instruction = llvm::unwrap<llvm::Instruction>(ref);
    return llvm::dyn_cast<llvm::BranchInst>(instruction)->getNumSuccessors();
  }

  LLVMBasicBlockRef LLVMInstructionBrGetSuccessor(LLVMValueRef ref, unsigned num) {
    auto instruction = llvm::unwrap<llvm::Instruction>(ref);
    return llvm::wrap(llvm::dyn_cast<llvm::BranchInst>(instruction)->getSuccessor(num));
  }

  LLVMBasicBlockRef LLVMInstructionInvokeGetNormalDest(LLVMValueRef ref) {
      auto instruction = llvm::unwrap<llvm::Instruction>(ref);
      return llvm::wrap(llvm::dyn_cast<llvm::InvokeInst>(instruction)->getNormalDest());
  }

  LLVMBasicBlockRef LLVMInstructionInvokeGetUnwindDest(LLVMValueRef ref) {
      auto instruction = llvm::unwrap<llvm::Instruction>(ref);
      return llvm::wrap(llvm::dyn_cast<llvm::InvokeInst>(instruction)->getUnwindDest());
  }

  unsigned LLVMLocationGetLine(DILocationRef ref) {
    return llvm::unwrap(ref)->getLine();
  }

  unsigned LLVMLocationGetColumn(DILocationRef ref) {
    return llvm::unwrap(ref)->getColumn();
  }

  DILocalScopeRef LLVMLocationGetScope(DILocationRef ref) {
    return llvm::wrap(llvm::unwrap(ref)->getScope());
  }

  DILocationRef LLVMLocationGetInlinedAt(DILocationRef ref) {
    return llvm::wrap(llvm::unwrap(ref)->getInlinedAt());
  }

  DIFileRef LLVMScopeGetFile(DILocalScopeRef ref) {
    auto scope = llvm::unwrap(ref);
    if (llvm::isa<llvm::DISubprogram>(scope)) {
      auto subprogram = llvm::dyn_cast<llvm::DISubprogram>(reinterpret_cast<llvm::DILocalScope *>(ref));
#ifdef DI_DEBUG
      printf("subprogram: %s(%s) %s(scopeFile:%s):%d\n",
      subprogram->getName().str().c_str(),
      subprogram->getLinkageName().str().c_str(),
      fileName(subprogram->getFile()),
      fileName(subprogram->getScope().resolve()->getFile()),
      subprogram->getLine());
#endif
      return llvm::wrap(subprogram->getFile());
    }
    return llvm::wrap(scope->getFile());
  }

  const char *LLVMFileGetFilename(DIFileRef ref) {
    auto mdstring = llvm::unwrap(ref)->getFilename();
    auto cstring = mdstring.str().c_str();
    return(cstring);
  }

  const char *LLVMFileGetDirectory(DIFileRef ref) {
      auto mdstring = llvm::unwrap(ref)->getDirectory();
      return mdstring.str().c_str();
  }
} /* extern "C" */

