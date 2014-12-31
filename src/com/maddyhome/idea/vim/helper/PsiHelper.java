/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2014 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.helper;

import com.intellij.codeInsight.navigation.MethodUpDownUtil;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import gnu.trove.TIntArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PsiHelper {
  public static int findMethodStart(@NotNull Editor editor, int offset, int count) {
    return findMethodOrClass(editor, offset, count, true);
  }

  public static int findMethodEnd(@NotNull Editor editor, int offset, int count) {
    return findMethodOrClass(editor, offset, count, false);
  }

  private static int findMethodOrClass(@NotNull Editor editor, int offset, int count, boolean isStart) {
    PsiFile file = getFile(editor);

    if (file == null) {
      return -1;
    }

    //Lilx
    //IDEA14不再支持 builder.createStructureViewModel() 了，如果调用 builder.createStructureViewModel() 将直接抛出异常
    //StructureViewBuilder structureViewBuilder = LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(file);
    //if (!(structureViewBuilder instanceof TreeBasedStructureViewBuilder)) return -1;
    //TreeBasedStructureViewBuilder builder = (TreeBasedStructureViewBuilder)structureViewBuilder;
    //StructureViewModel model = builder.createStructureViewModel();

    // Lilx
    // 暂时不支持 goto method end
    int[] offsets = MethodUpDownUtil.getNavigationOffsets(file, offset);
    for(int i = offsets.length - 1; i >= 0; i--){
      int ioffset = offsets[i];
      if (ioffset < offset){
          //Lilx
          // count = -1 表示向前跳，1 表示向后跳
          int resultIndex = i + ((count == -1) ? 0 : 1);
          if (resultIndex < 0) resultIndex = 0;
          if (resultIndex >= offsets.length) resultIndex = offsets.length - 1;
          return offsets[resultIndex];
      }
      else if (ioffset == offset && count == 1) {
        if (i + 1 < offsets.length)
          return offsets[i + 1];
        else
          return offsets[i];
      }
    }
    return -1;
  }

  private static void addNavigationElements(@NotNull TreeElement root, @NotNull TIntArrayList navigationOffsets, boolean start) {
    if (root instanceof PsiTreeElementBase) {
      PsiElement element = ((PsiTreeElementBase)root).getValue();
      int offset;
      if (start) {
        offset = element.getTextRange().getStartOffset();
        if (element.getLanguage().getID().equals("JAVA")) {
          // HACK: for Java classes and methods, we want to jump to the opening brace
          int textOffset = element.getTextOffset();
          int braceIndex = element.getText().indexOf('{', textOffset - offset);
          if (braceIndex >= 0) {
            offset += braceIndex;
          }
        }
      }
      else {
        offset = element.getTextRange().getEndOffset() - 1;
      }
      if (!navigationOffsets.contains(offset)) {
        navigationOffsets.add(offset);
      }
    }
    for (TreeElement child : root.getChildren()) {
      addNavigationElements(child, navigationOffsets, start);
    }
  }

  @Nullable
  private static PsiFile getFile(@NotNull Editor editor) {
    VirtualFile vf = EditorData.getVirtualFile(editor);
    if (vf != null) {
      Project proj = editor.getProject();
      if (proj != null) {
        PsiManager mgr = PsiManager.getInstance(proj);
        return mgr.findFile(vf);
      }
    }
    return null;
  }
}
