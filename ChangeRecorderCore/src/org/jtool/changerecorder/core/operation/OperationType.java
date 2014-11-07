/*
 *  Copyright 2014
 *  Software Science and Technology Lab.
 *  Department of Computer Science, Ritsumeikan University
 */

package org.jtool.changerecorder.core.operation;

/**
 * Defines the type of an operation.
 * @author Katsuhisa Maruyama
 */
public enum OperationType {
    
    /**
     * The string indicating if this is a normal operation
     */
    NORMAL_OPERATION,
    
    /**
     * The string indicating if this is an operation derived from <code>diff</code>
     */
    DIFF_OPERATION,
    
    /**
     * The string indicating if this is a compounded operation
     */
    COMPOUND_OPERATION,
    
    /**
     * The string indicating if this is a copy operation
     */
    COPY_OPERATION,
    
    /**
     * The string indicating if this is a file operation
     */
    FILE_OPERATION,
    
    /**
     * The string indicating if this is a menu operation
     */
    MENU_OPERATION,
    
    /**
     * The string indicating if this is a resource change operation
     */
    RESOURCE_OPERATION,
    
    /**
     * The string indicating if this is a null operation
     */
    NULL_OPERATION;
    
    /**
     * Tests if the specified operation is a text one.
     * @param op the operation to be checked
     * @return <code>true</code> if the operation is the text one, otherwise <code>false</code>
     */
    public static boolean isTextOperation(IOperation op) {
        OperationType type = op.getOperationType();
        return type == OperationType.NORMAL_OPERATION ||
               type == OperationType.DIFF_OPERATION ||
               type == OperationType.COMPOUND_OPERATION;
    }
}
