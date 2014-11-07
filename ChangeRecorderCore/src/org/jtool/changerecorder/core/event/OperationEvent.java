/*
 *  Copyright 2014
 *  Software Science and Technology Lab.
 *  Department of Computer Science, Ritsumeikan University
 */

package org.jtool.changerecorder.core.event;

import org.jtool.changerecorder.core.operation.IOperation;

/**
 * Manages an event indicating that the operation has performed.
 * @author Katsuhisa Maruyama
 */
public class OperationEvent {
    
    public static final int OPERATION_ADDED = 1;
    
    /**
     * The type of this event
     */
    private int type;
    
    /**
     * The operation sent by this operation event
     */
    private IOperation operation;
    
    /**
     * Creates an instance containing information on this operation event.
     * @param type the type of this operation event
     * @param operation the operation sent by this operation event
     */
    public OperationEvent(int type, IOperation operation) {
        this.type = type;
        this.operation = operation;
    }
    
    /**
     * Creates an instance containing information on this operation event.
     * @param operation the operation sent by this operation event
     */
    public OperationEvent(IOperation operation) {
        this.operation = operation;
    }
    
    /**
     * Returns the type of this operation event.
     * @return the type of this operation event
     */
    public int getType() {
        return type;
    }
    
    /**
     * Returns the operation sent by this operation event.
     * @return the operation related to this operation event
     */
    public IOperation getOperation() {
        return operation;
    }
}
