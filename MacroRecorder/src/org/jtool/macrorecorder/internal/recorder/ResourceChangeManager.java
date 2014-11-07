/*
 *  Copyright 2014
 *  Software Science and Technology Lab.
 *  Department of Computer Science, Ritsumeikan University
 */

package org.jtool.macrorecorder.internal.recorder;

import org.jtool.macrorecorder.macro.ResourceMacro;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages operations related to element changes.
 * @author Katsuhisa Maruyama
 */
public class ResourceChangeManager implements IResourceChangeListener {
    
    /**
     * A recorder that records menu actions.
     */
    private MenuMacroRecorder recorder;
    
    /**
     * Creates an object that records resource change events.
     * @param recorder a recorder that records menu actions
     */
    public ResourceChangeManager(MenuMacroRecorder recorder) {
        this.recorder = recorder;
    }
    
    /**
     * Registers an element change manager with the Java model.
     * @param em the element change manager
     */
    public static void register(ResourceChangeManager rm) {
        ResourcesPlugin.getWorkspace().addResourceChangeListener(rm, IResourceChangeEvent.POST_CHANGE);
    }
    
    /**
     * Unregisters an element change manager with the Java model.
     * @param em the element change manager
     */
    public static void unregister(ResourceChangeManager rm) {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener(rm);
    }
    
    /**
     * Receives an event when one or more Java elements have changed. 
     * @param event the change event
     */
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        long time = Time.getCurrentTime();
        
        try {
            ResourceChangeVisitor visitor = new ResourceChangeVisitor();
            event.getDelta().accept(visitor);
            
            for (IResourceDelta delta : visitor.deltas) {
                if (delta.getKind() == IResourceDelta.REMOVED) {
                    ResourceMacro macro = createResourceRemovedMacro(time, delta);
                    if (macro != null) {
                        recorder.recordResourceMacro(macro);
                    }
                }
            }
            
            for (IResourceDelta delta : visitor.deltas) {
                if (delta.getKind() == IResourceDelta.ADDED) {
                    ResourceMacro macro = createResourceAddedMacro(time, delta);
                    if (macro != null) {
                        recorder.recordResourceMacro(macro);
                    }
                }
            }
            
            for (IResourceDelta delta : visitor.deltas) {
                if (delta.getKind() == IResourceDelta.CHANGED) {
                    ResourceMacro macro = createResourceChangedMacro(time, delta);
                    if (macro != null) {
                        recorder.recordResourceMacro(macro);
                    }
                }
            }
            
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Creates a macro corresponding to the removed delta of the resource change.
     * @param time the time when the resource change occurred
     * @param delta the removed delta of the resource change
     * @return the created resource macro
     */
    private ResourceMacro createResourceRemovedMacro(long time, IResourceDelta delta) {
        IResource resource = delta.getResource();
        String target = getTarget(resource);
        String path = resource.getFullPath().toString();
        String ipath = null;
        
        if (path == null) {
            return null;
        }
        
        String type = "Removed";
        if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
            ipath = delta.getMovedToPath().toString();
            
            if (path.compareTo(ipath) == 0) {
                type = "Moved";
            } else {
                type = "Renamed";
            }
        }
        
        String code = getCode(resource);
        return new ResourceMacro(time, type, path, target, ipath, code);
    }
    
    /**
     * Creates a macro corresponding to the added delta of the resource change.
     * @param time the time when the resource change occurred
     * @param delta the added delta of the resource change
     * @return the created resource macro
     */
    private ResourceMacro createResourceAddedMacro(long time, IResourceDelta delta) {
        IResource resource = delta.getResource();
        String target = getTarget(resource);
        String path = resource.getFullPath().toString();
        String ipath = null;
        
        if (path == null) {
            return null;
        }
        
        String type = "Added";
        if ((delta.getFlags() & IResourceDelta.MOVED_FROM) != 0) {
            ipath = delta.getMovedFromPath().toString();
            
            if (path.compareTo(ipath) == 0) {
                type = "Moved";
            } else {
                type = "Renamed";
            }
        }
        
        String code = getCode(resource);
        return new ResourceMacro(time, type, path, target, ipath, code);
    }
    
    /**
     * Creates a macro corresponding to the changed delta of the resource change.
     * @param time the time when the resource change occurred
     * @param delta the changed delta of the resource change
     * @return the created resource macro
     */
    private ResourceMacro createResourceChangedMacro(long time, IResourceDelta delta) {
        IResource resource = delta.getResource();
        String target = getTarget(resource);
        String path = resource.getFullPath().toString();
        String ipath = null;
        
        if (path == null) {
            return null;
        }
        
        if ((delta.getFlags() & IResourceDelta.CONTENT) != 0) {
            System.out.println("CONTENT CHANGED " + resource.getFullPath());
        }
        if ((delta.getFlags() & IResourceDelta.REPLACED) != 0) {
            System.out.println("CONTENT REPLACED " + resource.getFullPath());
        }
        
        return new ResourceMacro(time, "Changed", path, target, ipath, "");
    }
    
    /**
     * Obtains source code of a resource
     * @param resource the changed resource 
     * @return the contents of the source code
     */
    private String getCode(IResource resource) {
        IJavaElement elem = JavaCore.create(resource);
        if (elem instanceof ICompilationUnit) {
            ICompilationUnit cu = (ICompilationUnit)elem;
            
            try {
                return cu.getSource();
            } catch (JavaModelException e) {
                e.printStackTrace();
            }
        }
        return "";
    }
    
    /**
     * Returns the target of the resource change.
     * @param resource the changed resource
     * @return the target of the resource change,
     *         or <code>null</code> if the target is not either a project, package, or file
     */
    private String getTarget(IResource resource) {
        IJavaElement elem = JavaCore.create(resource);
        if (elem == null) {
            return null;
        }
        
        int elemType = elem.getElementType();
        if (elemType == IJavaElement.JAVA_PROJECT) {
            return "Project";
            
        } else if (elemType == IJavaElement.PACKAGE_FRAGMENT) {
            return "Package";
            
        } else if (elemType == IJavaElement.COMPILATION_UNIT) {
            return "File";
        }
        return null;
    }
    
    /**
     * Visits resource deltas.
     */
    class ResourceChangeVisitor implements IResourceDeltaVisitor {
        
        /**
         * The collection of resource deltas
         */
        List<IResourceDelta> deltas = new ArrayList<IResourceDelta>();
        
        /** 
         * Visits the given resource delta.
         * @return <code>true</code> if the resource delta's children should be visited,
         *         or <code>false</code> if they should be skipped
         * @exception CoreException if the visit fails for some reason
         */
        public boolean visit(IResourceDelta delta) throws CoreException {
            IResource res = delta.getResource();
            String path =  res.getFullPath().toString();
            if (path.endsWith(".java")) {
                if (delta.getKind() == IResourceDelta.ADDED ||
                    delta.getKind() == IResourceDelta.REMOVED ||
                    delta.getKind() == IResourceDelta.CHANGED) {
                    if (!contain(delta)) {
                        deltas.add(delta);
                    }
                }
            }
            return true;
        }
        
        /**
         * Tests if a given resource delta was already contained in the change collection.
         * @param delta the resource delta
         * @return <code>true</code> if the resource delta was contained in the change collection,
         *         otherwise <code> false</code>
         */
        private boolean contain(IResourceDelta delta) {
            for (IResourceDelta d : deltas) {
                String p = d.getResource().getFullPath().toString();
                String path = delta.getResource().getFullPath().toString();
                if (p.compareTo(path) == 0 && d.getKind() == delta.getKind()) {
                    return true;
                }
            }
            return false;
        }
    }
}
