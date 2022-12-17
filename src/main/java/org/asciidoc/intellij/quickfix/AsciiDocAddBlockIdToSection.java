package org.asciidoc.intellij.quickfix;

import com.intellij.codeInsight.intention.FileModifier;
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.util.IntentionFamilyName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import org.asciidoc.intellij.AsciiDocBundle;
import org.asciidoc.intellij.file.AsciiDocFileType;
import org.asciidoc.intellij.psi.AsciiDocFileReference;
import org.asciidoc.intellij.psi.AsciiDocSection;
import org.asciidoc.intellij.psi.AsciiDocUtil;
import org.asciidoc.intellij.psi.HasAnchorReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Alexander Schwartz 2020
 */
public class AsciiDocAddBlockIdToSection implements LocalQuickFix {

  private final @NotNull SmartPsiElementPointer<PsiElement> element;

  public AsciiDocAddBlockIdToSection(PsiElement element) {
    this.element = SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);
  }

  @Override
  public @IntentionFamilyName @NotNull String getFamilyName() {
    return AsciiDocBundle.message("asciidoc.quickfix.addBlockIdToSection");
  }

  @Override
  public @Nullable PsiElement getElementToMakeWritable(@NotNull PsiFile currentFile) {
    if (element.getElement() instanceof HasAnchorReference) {
      AsciiDocSection section = ((HasAnchorReference) element.getElement()).resolveAnchorForSection();
      if (section != null) {
        if (currentFile.getOriginalFile().equals(section.getContainingFile())) {
          // this might be a copy of the original file to generate the preview
          return currentFile;
        } else {
          return section.getContainingFile();
        }
      }
    }
    return null;
  }

  @Override
  public @NotNull IntentionPreviewInfo generatePreview(@NotNull Project project, @NotNull ProblemDescriptor previewDescriptor) {
    IntentionPreviewInfo intentionPreviewInfo = LocalQuickFix.super.generatePreview(project, previewDescriptor);
    if (intentionPreviewInfo == IntentionPreviewInfo.EMPTY) {
      PsiElement element = previewDescriptor.getPsiElement();
      AsciiDocSection section = ((HasAnchorReference) previewDescriptor.getPsiElement()).resolveAnchorForSection();
      if (section != null) {
        if (section.getBlockId() == null) {
          PsiElement firstChild = section.getFirstChild();
          String id = section.getAutogeneratedId();
          AsciiDocFileReference reference = ((HasAnchorReference) element).getAnchorReference();
          if (reference != null && !reference.isPossibleRefText()) {
            id = reference.getRangeInElement().substring(element.getText());
          }
          String origText = section.getContainingFile().getText();
          StringBuilder newText = new StringBuilder(origText);
          newText.insert(firstChild.getTextOffset(), "[#" + id + "]\n");
          intentionPreviewInfo = new IntentionPreviewInfo.CustomDiff(AsciiDocFileType.INSTANCE, section.getContainingFile().getName(), origText, newText.toString());
        }
      }
    }
    return intentionPreviewInfo;
  }

  @Override
  public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
    PsiElement element = descriptor.getPsiElement();
    if (element instanceof HasAnchorReference) {
      AsciiDocSection section = ((HasAnchorReference) element).resolveAnchorForSection();
      if (section != null) {
        if (section.getBlockId() == null) {
          PsiElement firstChild = section.getFirstChild();
          String id = section.getAutogeneratedId();
          AsciiDocFileReference reference = ((HasAnchorReference) element).getAnchorReference();
          if (reference != null && !reference.isPossibleRefText()) {
            id = reference.getRangeInElement().substring(element.getText());
          }
          for (PsiElement child : createBlockId(project,
            "[#" + id + "]").getChildren()) {
            section.addBefore(child,
              firstChild);
          }
        }
      }
    }
  }

  @Override
  public @Nullable FileModifier getFileModifierForPreview(@NotNull PsiFile target) {
    return this;
  }

  @NotNull
  private static PsiElement createBlockId(@NotNull Project project, @NotNull String text) {
    return AsciiDocUtil.createFileFromText(project, text);
  }
}
