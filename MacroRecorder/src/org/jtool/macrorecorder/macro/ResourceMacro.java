/*
 *  Copyright 2014
 *  Software Science and Technology Lab.
 *  Department of Computer Science, Ritsumeikan University
 */

package org.jtool.macrorecorder.macro;

/**
 * Stores a resource change macro.
 * @author Katsuhisa Maruyama
 */
public class ResourceMacro extends Macro {
    
    /**
     * The type of the resource changed by this macro.
     */
    private String target;
    
    /**
     * The path of a resource identical to the resource changed by this macro.
     */
    private String ipath;
    
    /**
     * The contents of source code after the resource change.
     */
    private String code;
    
    /**
     * A flag that indicates if the changed resource is currently edited.
     */
    private boolean onEdit = false;
    
    /**
     * Creates an object storing information on an resource change macro.
     * @param time the time when the macro started
     * @param type the type of the change
     * @param path the path of the changed resource
     * @param target type of the changed resource
     * @param ipath the path of a resource identical to the changed resource
     * @param code the contents of source code after the resource change
     */
    public ResourceMacro(long time, String type, String path, String target, String ipath, String code) {
        super(time, time, type, path);
        this.target = target;
        this.ipath = ipath;
        this.code = code;
    }
    
    /**
     * Returns the target of this macro.
     * @return the target of this macro
     */
    public String getTarget() {
        return target;
    }
    
    /**
     * Returns the path of a resource moved/renamed from or to.
     * @return the path of the identical resource
     */
    public String getIdenticalPath() {
        return ipath;
    }
    
    /**
     * Returns the source code after the resource change.
     * @return the source code
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Sets the flag that indicates if the changed resource is currently edited.
     * @param bool <code>true</code> if the changed resource is currently edited, otherwise <code>false</code>
     */
    public void setOnEdit(boolean bool) {
        onEdit = bool;
    }
    
    /**
     * Tests if the changed resource is currently edited.
     * @return <code>true</code> if the changed resource is currently edited, otherwise <code>false</code>
     */
    public boolean getOnEdit() {
        return onEdit;
    }
    
    /**
     * Returns the string for printing, which does not contain a new line character at its end.
     * @return the string for printing
     */
    public String toString() {
        return "RESO(" + getType() + ")= " + getStartTime() + "-" + getEndTime() + ": " +
               getPath() + " " + getTarget() + " FROM/TO " + getIdenticalPath();
    }
}
