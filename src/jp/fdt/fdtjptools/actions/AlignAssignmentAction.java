package jp.fdt.fdtjptools.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

import jp.fdt.fdtjptools.editor.AbstractEditor;

/**
 * Our sample action implements workbench action delegate. The action proxy will
 * be created by the workbench and shown in the UI. When the user tries to use
 * the action, this delegate will be created and execution will be delegated to
 * it.
 * 
 * @see IWorkbenchWindowActionDelegate
 */
public class AlignAssignmentAction implements IWorkbenchWindowActionDelegate {

	private IWorkbenchWindow window;
	private AbstractEditor editor;
	private Map<Integer, ColumnAndOffset> lineNumberToEqualsColumn;

	/**
	 * The constructor.
	 */
	public AlignAssignmentAction() {
	}

	/**
	 * The action has been activated. The argument of the method represents the
	 * 'real' action sitting in the workbench UI.
	 * 
	 * @see IWorkbenchWindowActionDelegate#run
	 */
	public void run(IAction action) {
		try {
			Execute();
		} catch (MalformedTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void Execute() throws MalformedTreeException, BadLocationException {
		
		//window.getActivePage();
		
		if (charsThatAssociateWithEquals == null) {
			initCharsThatAssociateWithEquals();
		}
		
		// Find all lines above and below with = signs
		editor = new AbstractEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor());

		IDocument doc = editor.getDocument();

		ITextSelection textSelection = editor.getTextSelection();
		int cursorPos = textSelection.getOffset();
		int currentLineNumber;

		currentLineNumber = doc.getLineOfOffset(cursorPos);

		lineNumberToEqualsColumn = new HashMap<Integer, ColumnAndOffset>();

		int line = currentLineNumber;
		int start = doc.getLineOffset(line);
		int end = start + doc.getLineLength(line);
		ColumnAndOffset columnAndOffset = getColumnNumberOfFirstEquals(
				new TextSnapshotLine(doc.get(doc.getLineOffset(line), doc
						.getLineLength(line)), end - start, start), line);

		if (columnAndOffset.Column == -1)
			return;

		lineNumberToEqualsColumn.put(currentLineNumber, columnAndOffset);

		int lineNumber = currentLineNumber;
		int minLineNumber = 0;
		int maxLineNumber = doc.getNumberOfLines();

		// If the selection spans multiple lines, only attempt to fix the
		// lines
		// in the selection
		if (textSelection.getStartLine() != textSelection.getEndLine()) {
			int selectionStartLine = doc.getLineOfOffset(textSelection
					.getOffset());
			if ((textSelection.getOffset() + textSelection.getLength()) > doc
					.getLineOffset(textSelection.getStartLine()))
				;
			{
				minLineNumber = selectionStartLine;
				maxLineNumber = textSelection.getEndLine();
			}
		}

		// Moving backwards
		for (lineNumber = currentLineNumber - 1; lineNumber >= minLineNumber; lineNumber--) {
			start = doc.getLineOffset(lineNumber);
			end = start + doc.getLineLength(lineNumber);
			columnAndOffset = getColumnNumberOfFirstEquals(
					new TextSnapshotLine(doc.get(doc.getLineOffset(lineNumber),
							doc.getLineLength(lineNumber)), end - start, start),
					lineNumber);

			if (columnAndOffset.Column == -1)
				break;

			lineNumberToEqualsColumn.put(lineNumber, columnAndOffset);
		}

		// Moving forwards
		for (lineNumber = currentLineNumber + 1; lineNumber <= maxLineNumber; lineNumber++) {
			start = doc.getLineOffset(lineNumber);
			end = start + doc.getLineLength(lineNumber);
			columnAndOffset = getColumnNumberOfFirstEquals(
					new TextSnapshotLine(doc.get(doc.getLineOffset(lineNumber),
							doc.getLineLength(lineNumber)), end - start, start),
					lineNumber);

			if (columnAndOffset.Column == -1)
				break;

			lineNumberToEqualsColumn.put(lineNumber, columnAndOffset);
		}

		// Perform the actual edit
		if (lineNumberToEqualsColumn.size() > 1) {
			performEdit();
		}
	}

	/**
	 * Selection in the workbench has been changed. We can change the state of
	 * the 'real' action here if we want, but this can only happen after the
	 * delegate has been created.
	 * 
	 * @see IWorkbenchWindowActionDelegate#selectionChanged
	 */
	public void selectionChanged(IAction action, ISelection selection) {
	}

	/**
	 * We can use this method to dispose of any system resources we previously
	 * allocated.
	 * 
	 * @see IWorkbenchWindowActionDelegate#dispose
	 */
	public void dispose() {
	}

	/**
	 * We will cache window object in order to be able to provide parent shell
	 * for the message dialog.
	 * 
	 * @see IWorkbenchWindowActionDelegate#init
	 */
	public void init(IWorkbenchWindow window) {
		this.window = window;
	}

	private void initCharsThatAssociateWithEquals() {
		charsThatAssociateWithEquals = new ArrayList<Character>();
		charsThatAssociateWithEquals.add('+');
		charsThatAssociateWithEquals.add('-');
		charsThatAssociateWithEquals.add('.');
		charsThatAssociateWithEquals.add('<');
		charsThatAssociateWithEquals.add('>');
		charsThatAssociateWithEquals.add('/');
		charsThatAssociateWithEquals.add(':');
		charsThatAssociateWithEquals.add('\\');
		charsThatAssociateWithEquals.add('*');
		charsThatAssociateWithEquals.add('&');
		charsThatAssociateWithEquals.add('^');
		charsThatAssociateWithEquals.add('%');
		charsThatAssociateWithEquals.add('$');
		charsThatAssociateWithEquals.add('#');
		charsThatAssociateWithEquals.add('@');
		charsThatAssociateWithEquals.add('!');
		charsThatAssociateWithEquals.add('~');
		charsThatAssociateWithEquals.add('|');
	}

	private void performEdit() {
		int columnToIndentTo = 0;

		for (ColumnAndOffset cof : lineNumberToEqualsColumn.values()) {
			columnToIndentTo = Math.max(columnToIndentTo, cof.Column);
		}

		DocumentRewriteSession session = editor.startUndoAction(true);// sci.BeginUndoAction();

		MultiTextEdit multiTextEdit = new MultiTextEdit();

		for (ColumnAndOffset cof : lineNumberToEqualsColumn.values()) {
			if (cof.Column >= columnToIndentTo)
				continue;

			int length = columnToIndentTo - cof.Column;

			char[] spaces = new char[length];
			for (int i = 0; i < length; i++) {
				spaces[i] = ' ';
			}
			int insertPosition;
			try {
				insertPosition = editor.getLineOffset(cof.Line) + cof.Offset;

				if (isComment(insertPosition))
					continue;

				multiTextEdit.addChild(new InsertEdit(insertPosition,
						new String(spaces)));
			} catch (BadLocationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		try {
			multiTextEdit.apply(editor.getDocument());
		} catch (MalformedTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		editor.endUndoAction(session);
	}

	private ColumnAndOffset getColumnNumberOfFirstEquals(TextSnapshotLine line,
			int linenum) {
		String snapshot = line.Text;

		// TODO: タブサイズの取得
		// int tabSize = Globals.MainForm.Settings.TabWidth;
		int tabSize = 4;
		int column = 0;
		int nonWhiteSpaceCount = 0;
		for (int i = 0; i < line.Length; i++) {
			char ch = snapshot.charAt(i);
			if (ch == '=') {
				if (isComment(line.Position + i)) {
					return new ColumnAndOffset(1, i - nonWhiteSpaceCount,
							linenum);
				} else {
					return new ColumnAndOffset(column, i - nonWhiteSpaceCount,
							linenum);
				}
			}

			// For the sake of associating characters with the '=', include only
			if (!CharAssociatesWithEquals(ch))
				nonWhiteSpaceCount = 0;
			else
				nonWhiteSpaceCount++;

			if (ch == '\t')
				column += tabSize - (column % tabSize);
			else
				column++;
		}

		return new ColumnAndOffset(-1, -1, linenum);
	}

	private List<Character> charsThatAssociateWithEquals;

	private boolean CharAssociatesWithEquals(char ch) {
		return charsThatAssociateWithEquals.contains(ch);
	}

	private boolean isComment(int pos) {
		// TODO: コメントの判別
		/*
		 * if (sci.StyleAt(pos) == 1 || sci.StyleAt(pos) == 2) { return true; }
		 */
		return false;
	}
}

class ColumnAndOffset {
	public int Column;
	public int Offset;
	public int Line;

	public ColumnAndOffset(int column, int offset, int line) {
		Column = column;
		Offset = offset;
		Line = line;
	}
}

class TextSnapshotLine {
	public String Text;
	public int Length;
	public int Position;

	public TextSnapshotLine(String text, int length, int pos) {
		Text = text;
		Length = length;
		Position = pos;
	}
}