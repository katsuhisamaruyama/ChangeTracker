/*
 *  Copyright 2014
 *  Software Science and Technology Lab.
 *  Department of Computer Science, Ritsumeikan University
 */

package org.jtool.changereplayer.ui;

import org.jtool.changereplayer.Activator;
import org.jtool.changereplayer.event.ViewChangedEvent;
import org.jtool.changereplayer.event.ViewChangedListener;
import org.jtool.changereplayer.event.ViewEventSource;
import org.jtool.changerepository.data.FileInfo;
import org.jtool.changerepository.data.ProjectInfo;
import org.jtool.changerepository.data.WorkspaceInfo;
import org.jtool.changerepository.operation.OperationManager;
import org.jtool.changerepository.operation.UnifiedOperation;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;

import java.util.List;

/**
 * Creates a source code viewer for replay.
 * @author Katsuhisa Maruyama
 */
public class SourceCodeView implements ViewChangedListener {
    
    /**
     * The composite that contains several controls.
     */
    protected Composite composite;
    
    /**
     * The button control contained in this source code view.
     */
    protected ButtonControl buttonControl;
    
    /**
     * The time-line control contained in this source code view.
     */
    protected TimelineControl timelineControl;
    
    /**
     * The source code control contained in this source code view.
     */
    protected SourceCodeControl sourcecodeControl;
    
    /**
     * The information on the file of interest.
     */
    protected FileInfo fileInfo;
    
    /**
     * The time when the operation of interest was performed.
     */
    protected long focalTime = -1;
    
    /**
     * The sequence number of the operation of interest.
     */
    protected int currentOperationIndex = 0;
    
    /**
     * Creates an instance of a source code view.
     */
    public SourceCodeView() {
        ViewEventSource.getInstance().addEventListener(this);
    }
    
    /**
     * Creates a button control that manages button actions.
     * @param site the primary interface between the editor and the workbench
     */
    public void createBottonActions(IEditorSite site) {
        buttonControl = new ButtonControl(this);
        buttonControl.makeToolBarActions(site);
    }
    
    /**
     * Show the source code of a file.
     * @param parent the parent widget of the source code view
     * @param finfo the file information on the file displayed on the source code view
     * @param time the focal time for the file
     */
    public void show(Composite parent, FileInfo finfo) {
        fileInfo = finfo;
        
        composite = createSourceCodeViewControl(parent);
    }
    
    /**
     * Creates a source code view control.
     * @param parent the parent widget of the source code view
     * @return the created instance
     */
    public Composite createSourceCodeViewControl(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        FormLayout layout = new FormLayout();
        composite.setLayout(layout);
        
        timelineControl = new TimelineControl(this);
        timelineControl.createPartControl(composite, 0);
        
        sourcecodeControl = new SourceCodeControl(this);
        sourcecodeControl.createPartControl(composite, timelineControl.getControl(), 0);
        
        return composite;
    }
    
    /**
     * Returns the control of this source code view.
     * @return the control of the source code view
     */
    public Control getControl() {
        return composite;
    }
    
    /**
     * Tests if this source code view has shown source code.
     * @return <code>true</code> if source code has been shown, or <code>false</code> 
     */
    public boolean hasShown() {
        return composite != null;
    }
    
    /**
     * Sets the focus to this source code view.
     */
    public void setFocus() {
        if (timelineControl != null) {
            timelineControl.setFocus();
        }
        if (sourcecodeControl != null) {
            sourcecodeControl.setFocus();
        }
        if (composite != null) {
            composite.setFocus();
        }
    }
    
    /**
     * Disposes of this this source code view.
     */
    public void dispose() {
        if (timelineControl != null) {
            timelineControl.dispose();
            timelineControl = null;
        }
        if (sourcecodeControl != null) {
            sourcecodeControl.dispose();
            sourcecodeControl = null;
        }
        if (composite != null) {
            composite.dispose();
            composite = null;
        }
    }
    
    /**
     * Returns the workspace information.
     * @return the workspace information
     */
    public WorkspaceInfo getWorkspaceInfo() {
        return fileInfo.getProjectInfo().getWorkspace();
    }
    
    /**
     * Returns the project information.
     * @return the project information
     */
    public ProjectInfo getProjectInfo() {
        return fileInfo.getProjectInfo();
    }
    
    /**
     * Sets the file information.
     * @param finfo the file information
     */
    public void setFileInfo(FileInfo finfo) {
        this.fileInfo = finfo;
    }
    
    /**
     * Returns the file information.
     * @return the file information
     */
    public FileInfo getFileInfo() {
        return fileInfo;
    }
    
    /**
     * Obtains the current selection on the source code displayed on this source code view.
     * @return the selection on the code
     */
    public TextSelection getSelection() {
        return sourcecodeControl.getSelection();
    }
    
    /**
     * Sets the time when the operation of interest was performed.
     * @param idx the sequence number of the operation
     * @param time the focal time
     */
    public void setFocalTime(int idx, long time) {
        currentOperationIndex = idx;
        focalTime = time;
        
        sourcecodeControl.update();
        timelineControl.update();
    }
    
    /**
     * Changes the sequence number of the the operation currently replayed.
     * @param time the time of interest
     */
    public void findFocalTime(long time) {
        if (fileInfo == null) {
            return;
        }
        
        List<UnifiedOperation> ops = fileInfo.getOperations();
        int prev = OperationManager.getLatestOperationBefore(ops, time);
        int next = OperationManager.getEarliestOperationAfter(ops, time);
        
        if (prev >= 0 && next >= 0) {
            long before = ops.get(prev).getTime();
            long after = ops.get(next).getTime();
            if (time - before < after - time) {
                setFocalTime(prev, ops.get(prev).getTime());
            } else {
                setFocalTime(prev, ops.get(next).getTime());
            }
            
        } else if (prev >= 0) {
            setFocalTime(prev, ops.get(prev).getTime());
            
        } else if (next >= 0) {
            setFocalTime(prev, ops.get(next).getTime());
        }
    }
    
    /**
     * Returns the time when the operation of interest was performed on a file.
     * @return the focal time for the file
     */
    public long getFocalTime() {
        return focalTime;
    }
    
    /**
     * Returns the sequence number of the operation of interest.
     * @return the sequence number of the operation
     */
    public int getCurrentOperationIndex() {
        return currentOperationIndex;
    }
    
    /**
     * Goes to a specified operation.
     * @param idx the sequence number of the operation
     */
    public void goTo(int idx) {
        if (getFileInfo() == null) {
            return;
        }
        
        List<UnifiedOperation> ops = getFileInfo().getOperations();
        if (idx < 0 || ops.size() <= idx) {
            return;
        }
        
        setFocalTime(idx, ops.get(idx).getTime());
        
        System.out.println("IDX = " + idx);
        
        buttonControl.updateButtonStates(idx, ops);
        
        setFocus();
        
        sendViewEvent();
    }
    
    /**
     * Sends the view changed event.
     */
    private void sendViewEvent() {
        ViewChangedEvent event = new ViewChangedEvent(this);
        ViewEventSource.getInstance().fire(event);
    }
    
    /**
     * Obtains the history view.
     * @return the history view instance, or <code>null</code> if such view was not found
     */
    protected HistoryView getHistoryView() {
        IWorkbenchPage workbenchPage = Activator.getWorkbenchPage();
        return (HistoryView)workbenchPage.findView(HistoryView.ID);
    }
    
    /**
     * Receives a view changed event.
     * @param evt the sent and received event
     */
    @Override
    public void notify(ViewChangedEvent evt) {
        Object source = evt.getSource();
        
        if (source instanceof HistoryView) {
            HistoryView hview = (HistoryView)source;
            
            goTo(hview.getCurrentOperationIndex());
        }
    }
    
    /**
     * Creates the current state of the source code view and returns it.
     * @return the state of the source code view
     */
    public SourceCodeViewState createSourceCodeViewState() {
        return new SourceCodeViewState(focalTime, currentOperationIndex, timelineControl.getScale());
    }
    
    /**
     * Creates the current state of the source code view and returns it.
     * @param state the state of the source code view
     */
    public void restoreSourceCodeViewState(SourceCodeViewState state) {
        timelineControl.setScale(state.getScale());
        setFocalTime(state.getCurrentOperationIndex(), state.getFocalTime());
    }
}
