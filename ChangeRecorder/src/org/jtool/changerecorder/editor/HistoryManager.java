/*
 *  Copyright 2014
 *  Software Science and Technology Lab.
 *  Department of Computer Science, Ritsumeikan University
 */

package org.jtool.changerecorder.editor;

import org.jtool.changerecorder.Activator;
import org.jtool.changerecorder.core.event.OperationEventListener;
import org.jtool.changerecorder.core.event.OperationEventSource;
import org.jtool.changerecorder.core.history.OperationHistory;
import org.jtool.changerecorder.core.operation.CompoundOperation;
import org.jtool.changerecorder.core.operation.CopyOperation;
import org.jtool.changerecorder.core.operation.FileOperation;
import org.jtool.changerecorder.core.operation.IOperation;
import org.jtool.changerecorder.core.operation.TextOperation;
import org.jtool.changerecorder.core.operation.MenuOperation;
import org.jtool.changerecorder.core.operation.NormalOperation;
import org.jtool.changerecorder.core.util.Time;
import org.jtool.macrorecorder.recorder.Recorder;
import org.jtool.macrorecorder.recorder.MacroEvent;
import org.jtool.macrorecorder.recorder.MacroListener;
import org.jtool.macrorecorder.macro.Macro;
import org.jtool.macrorecorder.macro.ExecutionMacro;
import org.jtool.macrorecorder.macro.CopyMacro;
import org.jtool.macrorecorder.macro.DocumentMacro;
import org.jtool.macrorecorder.macro.CompoundMacro;
import org.jtool.macrorecorder.macro.ResourceMacro;
import org.jtool.macrorecorder.util.WorkspaceUtilities;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.ui.IEditorPart;
import java.util.ArrayList;
import java.util.List;

/**
 * A manager that manages the operation history.
 * @author Katsuhisa Maruyama
 */
public class HistoryManager extends OperationEventSource implements MacroListener {
    
    /**
     * The single instance of this history manager.
     */
    private static HistoryManager instance = new HistoryManager();
    
    /**
     * The operation history.
     */
    private OperationHistory history = new OperationHistory();
    
    /**
     * A recorder that records macros.
     */
    private Recorder recorder = null;
    
    /**
     * A listener that displays operation on the console.
     */
    private OperationEventListener consoleOperationListener = new ConsoleOperationListener();
    
    /**
     * Creates a manager that records operations performed on an editor.
     */
    public HistoryManager() {
        super();
        recorder = Recorder.getInstance();
    }
    
    /**
     * Returns the single instance that manages the operation history.
     * @return the history manager
     */
    public static HistoryManager getInstance() {
        return instance;
    }
    
    /**
     * Starts recording of operations.
     */
    public void start() {
        recorder.addMacroListener(this);
        addOperationEventListener(consoleOperationListener);
    }
    
    /**
     * Starts recording of operations on an editor.
     * @param editor the editor
     */
    public void start(IEditorPart editor) {
        if (recorder != null) {
            recorder.start(editor);
        }
    }
    
    /**
     * Stops recording of operations.
     */
    public void stop() {
        if (recorder != null) {
            recorder.removeMacroListener(this);
            removeOperationEventListener(consoleOperationListener);
        }
    }
    
    /**
     * Stops recording of operations.
     * @param editor the editor
     */
    public void stop(IEditorPart editor) {
        if (recorder != null) {
            recorder.stop(editor);
        }
    }
    
    /**
     * Receives a macro event when a new macro is added.
     * @param evt the macro event
     */
    @Override
    public void macroAdded(MacroEvent evt) {
        Macro macro = evt.getMacro();
        // System.out.println(macro.toString());
        
        if (macro instanceof DocumentMacro) {
            IOperation op = createOperation((DocumentMacro)macro);
            record(op);
            
        } else if (macro instanceof ExecutionMacro) {
            IOperation op = createOperation((ExecutionMacro)macro);
            record(op);
            
        } else if (macro instanceof CopyMacro) {
            IOperation op = createOperation((CopyMacro)macro);
            record(op);
            
        } else if (macro instanceof CompoundMacro) {
            IOperation op = createOperation((CompoundMacro)macro);
            record(op);
        }
    }
    
    /**
     * Receives a macro event when a document is changed.
     * @param evt the raw macro event
     */
    @Override
    public void documentChanged(MacroEvent evt) {
        // System.out.println("!MACRO: " + evt.getMacro());
    }
    
    /**
     * Records an operation.
     * @param op the operation to be recorded
     */
    private void record(IOperation op) {
        storeOperation(op);
    }
    
    /**
     * Stores an operation into the operation history.
     * @param op the operation to be stored
     */
    private void storeOperation(IOperation op) {
        history.add(op);
        notify(op);
    }
    
    /**
     * Obtains the last operation from this operation history.
     * @return the last operation, or <code>null</code> if none
     */
    IOperation getLastOperation() {
        return history.getLastOperation();
    }
    
    /**
     * Records a file operation.
     * @param file the file
     * @param code the contents of the source code when the operation was performed
     * @param type the type of the operation
     * @param codeWrite <code>true</code> if source code will be written, otherwise <code>false</code>
     */
    void recordFileOperation(IFile file, String code, FileOperation.Type type, boolean codeWrite) {
        FileOperation op;
        if (codeWrite) {
            op = new FileOperation(Time.getCurrentTime(), file.getFullPath().toString(), type, code);
        } else {
            op = new FileOperation(Time.getCurrentTime(), file.getFullPath().toString(), type, null);
        }
        storeOperation(op);
    }
    
    /**
     * Creates a text operation from a macro.
     * @param macro the recorded macro
     * @return the created operation
     */
    private TextOperation createOperation(DocumentMacro macro) {
        NormalOperation.Type type = NormalOperation.Type.EDIT;
        if (macro.getType().compareTo("Cut") == 0) {
            type = NormalOperation.Type.CUT;
        } else if (macro.getType().compareTo("Paste") == 0) {
            type = NormalOperation.Type.PASTE;
        } else if (macro.getType().compareTo("Undo") == 0) {
            type = NormalOperation.Type.UNDO;
        } else if (macro.getType().compareTo("Redo") == 0) {
            type = NormalOperation.Type.REDO;
        }
        
        return new NormalOperation(Time.getCurrentTime(), macro.getPath(), 
                                   macro.getStart(), macro.getInsertedText(), macro.getDeletedText(), type);
    }
    
    /**
     * Creates a menu operation from a macro.
     * @param macro the recorded macro
     * @return the created operation
     */
    private IOperation createOperation(ExecutionMacro macro) {
        return new MenuOperation(macro.getStartTime(), macro.getPath(), macro.getCommandId());
    }
    
    /**
     * Creates a copy operation from a macro.
     * @param macro the recorded macro
     * @return the created operation
     */
    private IOperation createOperation(CopyMacro macro) {
        return new CopyOperation(Time.getCurrentTime(), macro.getPath(), macro.getStart(), macro.getCopiedText());
    }
    
    /**
     * Creates a compound operation from a macro.
     * @param macro the recorded macro
     * @return the created operation
     */
    private IOperation createOperation(CompoundMacro macro) {
        List<IOperation> ops = new ArrayList<IOperation>();
        for (Macro m : macro.getMacros()) {
            if (m instanceof DocumentMacro) {
                ops.add(createOperation((DocumentMacro)m));
            }
        }
        
        return new CompoundOperation(macro.getStartTime(), ops, macro.getType());
    }
    
    /**
     * Writes the operation history related to a file.
     * @param file the file
     */
    void writeHistory(IFile file) {
        history.sort();
        
        String dpath = getOperationHistoryDirPath();
        String wpath = dpath + '/' + String.valueOf(Time.getCurrentTime()) + ".xml";
        String encoding = WorkspaceUtilities.getEncoding();
        try {
            if (file != null) {
                encoding = file.getCharset();
            }
        } catch (CoreException e) {
            encoding = WorkspaceUtilities.getEncoding();
        }
        
        history.write(wpath, encoding);
        // System.out.println(history.toString());
        
        history.clear();
    }
    
    /**
     * Returns the directory path of the plug-in's workspace, which contains operation history. 
     * @return the the directory into which the operation history is stored
     */
    public static String getOperationHistoryDirPath() {
        IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        IPath workspaceDir = workspaceRoot.getLocation();
        return workspaceDir.append(Activator.DEFAULT_HISTORY_TOPDIR).toString();
    }
    
    /**
     * Records a resource change operation.
     * @param path the path name of the file on which the operation was performed
     * @param code the contents of the source code when the operation was performed
     * @param type the type of the operation
     * @param codeWrite <code>true</code> if source code will be written, otherwise <code>false</code>
     */
    void recordResourceOperation(ResourceMacro macro) {
        // System.out.println(op.toString());
    }
}
