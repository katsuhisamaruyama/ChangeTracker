/*
 *  Copyright 2014
 *  Software Science and Technology Lab.
 *  Department of Computer Science, Ritsumeikan University
 */

package org.jtool.changerepository.dependencygraph;

/**
 * Stores the information on the edge of the operation dependency graph.
 * @author Katsuhisa Maruyama
 */
public class OpDepGraphEdge {
    
    /**
     * The source node of this edge.
     */
    protected OpDepGraphNode src;
    
    /**
     * The destination node of this edge.
     */
    protected OpDepGraphNode dst;
    
    /**
     * Creates a new, empty edge.
     */
    protected OpDepGraphEdge() {
    }
    
    /**
     * Creates a new edge between the two nodes.
     * @param src the source node of this edge
     * @param dst the destination node of this edge
     */
    public OpDepGraphEdge(OpDepGraphNode src, OpDepGraphNode dst) {
        this.src = src;
        this.dst = dst;
    }
    
    /**
     * Returns the source node of this edge.
     * @return the source node
     */
    public OpDepGraphNode getSrcNode() {
        return src;
    }
    
    /**
     * Returns the destination node of this edge.
     * @return the destination node
     */
    public OpDepGraphNode getDstNode() {
        return dst;
    }
    
    /**
     * Tests if a given graph edge is the same as this.
     * @param member the instance of a graph edge
     * @return <code>true</code> if the two edges are the same source node and the same destination node, otherwise <code>false</code>
     */
    public boolean equals(OpDepGraphEdge edge) {
        return edge != null && edge.getSrcNode().equals(getSrcNode()) && edge.getDstNode().equals(getDstNode());
    }
    
    /**
     * Returns the string for printing, which does not contain a new line character at its end. 
     * @return the string for printing
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(getSrcNode().toSimpleString());
        buf.append(" -> ");
        buf.append(getDstNode().toSimpleString());
        
        return buf.toString();
    }
}
