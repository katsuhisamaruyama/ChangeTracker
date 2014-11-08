/*
 *  Copyright 2014
 *  Software Science and Technology Lab.
 *  Department of Computer Science, Ritsumeikan University
 */

package org.jtool.changerecorder.core.operation;

import org.jtool.changerecorder.core.util.StringComparator;
import org.jtool.changerecorder.core.util.Whoami;

/**
 * Defines the abstract class used for accessing information on the all kinds of operations.
 * @author Katsuhisa Maruyama
 */
public abstract class AbstractOperation implements IOperation {
    
    /**
     * The time when this operation was performed.
     */
    protected long time;
    
    /**
     * The sequence number that indicates the order of change operations in the same time.
     */
    private int sequenceNumber = 0;
    
    /**
     * The path name of the file on which this operation was performed.
     */
    protected String path;
    
    /**
     * The name of a developer who performed this operation.
     */
    protected String author;
    
    /**
     * Creates an instance storing information on this operation.
     * @param time the time when this operation was performed
     * @param seq the sequence number of this operation
     * @param path the name of the file on which this operation was performed
     * @param author the author's name
     */
    protected AbstractOperation(long time, int seq, String path, String author) {
        this.time = time;
        this.sequenceNumber = seq;
        this.path = path;
        this.author = author;
    }
    
    /**
     * Creates an instance storing information on this operation.
     * @param time the time when this operation was performed
     * @param seq the sequence number of this operation
     * @param path the name of the file on which this operation was performed
     */
    protected AbstractOperation(long time, int seq, String path) {
        this(time, seq, path, getUserName());
    }
    
    /**
     * Creates an instance storing information on this operation.
     * @param time the time when this operation was performed
     * @param path the name of the file on which this operation was performed
     * @param author the author's name
     */
    protected AbstractOperation(long time, String path, String author) {
        this(time, 0, path, author);
    }
    
    /**
     * Creates an instance storing information on this operation.
     * @param time the time when this operation was performed
     * @param path the name of the file on which this operation was performed
     */
    protected AbstractOperation(long time, String path) {
        this(time, 0, path);
    }
    
    /**
     * Obtains the user name by executing <code>whoami</code command.
     * @return the user name
     */
    protected static String getUserName() {
        return Whoami.getUserName();
    }
    
    /**
     * Returns the time when this operation was performed.
     * @return the time of the operation
     */
    @Override
    public long getTime() {
        return time;
    }
    
    /**
     * Sets the sequence number that indicates the order of operations in the same time.
     * @param number the sequence number of this operation
     */
    public void setSequenceNumber(int number) {
        this.sequenceNumber = number;
    }
    
    /**
     * Returns the sequence number that indicates the order of operations in the same time.
     * @return the sequence number of this operation
     */
    public int getSequenceNumber() {
        return sequenceNumber;
    }
    
    /**
     * Returns the path name of the file on which this operation was performed.
     * @return the path name containing the operation
     */
    @Override
    public String getFilePath() {
        return path;
    }
    
    /**
     * Returns the author's name.
     * @return the author's name
     */
    @Override
    public String getAuthor() {
        return author;
    }
    
    /**
     * Tests if this operation is the same as a given one.
     * @param op the given operation
     * @return <code>true</code> if the two operations are the same, otherwise <code>false</code>
     */
    public boolean equals(IOperation op) {
        if (!(op instanceof AbstractOperation)) {
            return false;
        }
        
        return time == op.getTime() && sequenceNumber == getSequenceNumber() &&
               StringComparator.isSame(path, op.getFilePath()) &&
               StringComparator.isSame(author, op.getAuthor());
    }
}
