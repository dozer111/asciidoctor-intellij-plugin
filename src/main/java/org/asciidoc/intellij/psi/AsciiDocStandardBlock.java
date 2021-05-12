package org.asciidoc.intellij.psi;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.tree.TokenSet;
import icons.AsciiDocIcons;
import org.asciidoc.intellij.inspections.AsciiDocVisitor;
import org.asciidoc.intellij.lexer.AsciiDocTokenTypes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author yole
 */
public class AsciiDocStandardBlock extends ASTWrapperPsiElement implements AsciiDocBlock {
  public AsciiDocStandardBlock(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof AsciiDocVisitor) {
      ((AsciiDocVisitor) visitor).visitBlocks(this);
      return;
    }

    try {
      super.accept(visitor);
    } catch (RuntimeException e) {
      if (e instanceof RuntimeExceptionWithAttachments || e instanceof ProcessCanceledException) {
        throw e;
      }
      String psiTree = DebugUtil.psiToString(this, false, true);
      // keep only structure in the attachment, clear out any text content to anonymize data
      psiTree = psiTree.replaceAll("\\('.*'\\)", "");
      throw new RuntimeExceptionWithAttachments("Problem occurred while running visitor " + visitor.getClass(), e, new Attachment("psi.txt", psiTree));
    }
  }

  @NotNull
  @Override
  public String getFoldedSummary() {
    PsiElement child = getFirstSignificantChildForFolding();
    if (child instanceof AsciiDocBlockAttributes) {
      return "[" + getStyle() + "]";
    }
    return child.getText();
  }

  @Override
  public String getDefaultTitle() {
    ASTNode delimiter = getNode().findChildByType(TokenSet.create(AsciiDocTokenTypes.BLOCK_DELIMITER, AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER));
    String title;
    if (delimiter != null) {
      String d = delimiter.getText();
      if (d.startsWith("|")) {
        title = "Table";
      } else if (d.startsWith("*")) {
        title = "Sidebar";
      } else if (d.startsWith("=")) {
        title = "Example";
      } else if (d.startsWith(".")) {
        title = "Literal";
      } else if (d.startsWith("_")) {
        title = "Quote";
      } else {
        title = AsciiDocBlock.super.getDefaultTitle();
      }
    } else {
      title = AsciiDocBlock.super.getDefaultTitle();
    }
    return title;
  }

  @Override
  public Type getType() {
    Type type = Type.UNKNOWN;
    ASTNode delimiter = getNode().findChildByType(TokenSet.create(AsciiDocTokenTypes.BLOCK_DELIMITER, AsciiDocTokenTypes.LITERAL_BLOCK_DELIMITER));
    if (delimiter != null) {
      String d = delimiter.getText();
      if (d.startsWith("|")) {
        type = Type.TABLE;
      } else if (d.startsWith("*")) {
        type = Type.SIDEBAR;
      } else if (d.startsWith("=")) {
        type = Type.EXAMPLE;
      } else if (d.startsWith(".")) {
        type = Type.LITERAL;
      } else if (d.startsWith("_")) {
        if ("verse".equals(getStyle())) {
          type = Type.VERSE;
        } else {
          type = Type.QUOTE;
        }
      } else if (d.startsWith("-")) {
        if ("verse".equals(getStyle())) {
          type = Type.VERSE;
        }
      }
    } else {
      if ("verse".equals(getStyle())) {
        type = Type.VERSE;
      }
    }
    return type;
  }

  @Override
  public Icon getIcon(int flags) {
    return AsciiDocIcons.Structure.BLOCK;
  }

}
