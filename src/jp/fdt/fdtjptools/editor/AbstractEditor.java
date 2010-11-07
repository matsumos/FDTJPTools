package jp.fdt.fdtjptools.editor;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

public class AbstractEditor{

	private IEditorPart editorPart;

	public AbstractEditor(IEditorPart editorPart) {
		this.editorPart = editorPart;
	}

	public IDocument getDocument() {
		IDocumentProvider provider = getTextEditor().getDocumentProvider();
		IDocument document = provider.getDocument(editorPart.getEditorInput());
		return document;
	}

	public ITextSelection getTextSelection() {
		ISelectionProvider selectionProvider = getTextEditor()
				.getSelectionProvider();
		ISelection selection = selectionProvider.getSelection();
		return (ITextSelection) selection;
	}

	public ITextEditor getTextEditor() {
		return (ITextEditor) editorPart;
	}

	public IEditorInput getInput() {
		if (editorPart == null) {
			return null;
		}
		return editorPart.getEditorInput();
	}

	public String getText() {
		IDocument document = getDocument();
		return document != null ? document.get() : null;
	}

	public void setText(String text) {
		IDocument document = getDocument();
		if (document != null)
			document.set(text);
	}

	public String getText(int offset, int length) throws BadLocationException {
		return getDocument().get(offset, length);
	}

	public char getChar(int offset) throws BadLocationException {
		return getDocument().getChar(offset);
	}

	public int getLineLength(int line) throws BadLocationException {
		return getDocument().getLineLength(line);
	}

	public int getLineOffset(int line) throws BadLocationException {
		return getDocument().getLineOffset(line);
	}

	public int getLineOfOffset(int offset) throws BadLocationException {
		return getDocument().getLineOfOffset(offset);
	}

	public int getNumberOfLines() {
		return getDocument().getNumberOfLines();
	}
	
	public String getTitle() {
		return ((AbstractTextEditor) editorPart).getTitle();
	}

	public int getNumberOfLines(int offset, int length)
			throws BadLocationException {
		return getDocument().getNumberOfLines(offset, length);
	}

	public IRegion find(int startOffset, String findString,
			boolean forwardSearch, boolean caseSensitive, boolean wholeWord,
			boolean regExSearch) throws BadLocationException {
		FindReplaceDocumentAdapter finder = new FindReplaceDocumentAdapter(
				getDocument());
		return finder.find(startOffset, findString, forwardSearch,
				caseSensitive, wholeWord, regExSearch);
	}

	public void insert(int offset, String text) throws MalformedTreeException,
			BadLocationException {
		InsertEdit edit = new InsertEdit(offset, text);
		edit.apply(getDocument());
	}

	public void replace(int offset, int length, String text)
			throws BadLocationException {
		getDocument().replace(offset, length, text);
	}

	public boolean equals(Object obj) {
		if (obj == this) {
			return true;
		}
		if (!(obj instanceof AbstractEditor)) {
			return false;
		}
		AbstractEditor other = (AbstractEditor) obj;
		if (this.editorPart != other.editorPart) {
			return false;
		}
		return this.hashCode() == other.hashCode();
	}

	public void dispose() {
		editorPart = null;
	}

	public void endUndoAction(DocumentRewriteSession session) {
		IDocument document = getDocument();
		IDocumentExtension4 extension = (IDocumentExtension4) document;
		extension.stopRewriteSession(session);

		Object adapter = editorPart.getAdapter(IRewriteTarget.class);
		if (adapter instanceof IRewriteTarget) {
			IRewriteTarget target = (IRewriteTarget) adapter;
			target.endCompoundChange();
			target.setRedraw(true);
		}
	}

	public DocumentRewriteSession startUndoAction(boolean normalized) {
		DocumentRewriteSession rewriteSession = null;
		// de/activate listeners etc, prepare multiple replace
		Object adapter = editorPart.getAdapter(IRewriteTarget.class);
		if (adapter instanceof IRewriteTarget) {
			IRewriteTarget target = (IRewriteTarget) adapter;
			target.setRedraw(false);
			target.beginCompoundChange();
		}

		IDocument document = getDocument();
		IDocumentExtension4 extension = (IDocumentExtension4) document;
		if (normalized) {
			rewriteSession = extension
					.startRewriteSession(DocumentRewriteSessionType.STRICTLY_SEQUENTIAL);
		} else {
			rewriteSession = extension
					.startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
		}

		return rewriteSession;
	}
}
