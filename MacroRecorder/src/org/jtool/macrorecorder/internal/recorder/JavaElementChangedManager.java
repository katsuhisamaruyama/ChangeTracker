/*
 *  Copyright 2014
 *  Software Science and Technology Lab.
 *  Department of Computer Science, Ritsumeikan University
 */

package org.jtool.macrorecorder.internal.recorder;

import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.jtool.macrorecorder.macro.ResourceMacro;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages operations related to element changes.
 * @author Katsuhisa Maruyama
 */
public class JavaElementChangedManager implements IElementChangedListener {
    
    /**
     * A recorder that records menu actions.
     */
    private MenuMacroRecorder recorder;
    
    /**
     * Creates an object that records resource change events.
     * @param recorder a recorder that records menu actions
     */
    public JavaElementChangedManager(MenuMacroRecorder recorder) {
        this.recorder = recorder;
    }
    
    /**
     * Registers an element change manager with the Java model.
     * @param em the element change manager
     */
    public static void register(JavaElementChangedManager em) {
        JavaCore.addElementChangedListener(em);
    }
    
    /**
     * Unregisters an element change manager with the Java model.
     * @param em the element change manager
     */
    public static void unregister(JavaElementChangedManager em) {
        JavaCore.removeElementChangedListener(em);
    }
    
    /**
     * Receives an event when one or more Java elements have changed. 
     * @param event the change event
     */
    @Override
    public void elementChanged(ElementChangedEvent event) {
        long time = Time.getCurrentTime();
        ChangeCollector collector = new ChangeCollector(event.getDelta());
        
        for (IJavaElementDelta delta : collector.deltas) {
            if (delta.getKind() == IJavaElementDelta.REMOVED) {
                ResourceMacro macro = createResourceRemovedMacro(time, delta);
                if (macro != null) {
                    recorder.recordResourceMacro(macro);
                }
            }
        }
        
        for (IJavaElementDelta delta : collector.deltas) {
            if (delta.getKind() == IJavaElementDelta.ADDED) {
                ResourceMacro macro = createResourceAddedMacro(time, delta);
                if (macro != null) {
                    recorder.recordResourceMacro(macro);
                }
            }
        }
        
        for (IJavaElementDelta delta : collector.deltas) {
            if (delta.getKind() == IJavaElementDelta.CHANGED) {
                ResourceMacro macro = createResourceChangedMacro(time, delta);
                if (macro != null) {
                    recorder.recordResourceMacro(macro);
                }
            }
        }
    }
    
    /**
     * Creates a macro corresponding to the removed delta of the change.
     * @param time the time when the change occurred
     * @param delta the removed delta of the change
     * @return the created resource macro
     */
    private ResourceMacro createResourceRemovedMacro(long time, IJavaElementDelta delta) {
        IJavaElement elem = delta.getElement();
        String target = getTarget(elem);
        String path = elem.getPath().toString();
        String ipath = null;
        
        if (path == null) {
            return null;
        }
        
        System.out.println("REMOVED " + path);
        
        String type = "Removed";
        if ((delta.getFlags() & IJavaElementDelta.F_MOVED_TO) != 0) {
            ipath = delta.getMovedToElement().getPath().toString();
            
            if (isRenamed(delta.getElement(), delta.getMovedToElement(), elem.getElementType())) {
                type = "Renamed";
            } else {
                type = "Moved";
            }
        }
        
        return new ResourceMacro(time, type, path, target, ipath, "");
    }
    
    /**
     * Creates a macro corresponding to the added delta of the change.
     * @param time the time when the change occurred
     * @param delta the added delta of the change
     * @return the created resource macro
     */
    private ResourceMacro createResourceAddedMacro(long time, IJavaElementDelta delta) {
        IJavaElement elem = delta.getElement();
        String target = getTarget(elem);
        String path = elem.getPath().toString();
        String ipath = null;
        
        if (path == null) {
            return null;
        }
        
        System.out.println("ADDED " + path);
        
        String type = "Added";
        if ((delta.getFlags() & IJavaElementDelta.F_MOVED_FROM) != 0) {
            ipath = delta.getMovedFromElement().getPath().toString();
            
            if (isRenamed(delta.getElement(), delta.getMovedToElement(), elem.getElementType())) {
                type = "Renamed";
            } else {
                type = "Moved";
            }
        }
        
        String code = getCode(elem);
        return new ResourceMacro(time, type, path, target, ipath, code);
    }
    
    /**
     * Creates a macro corresponding to the changed delta of the change.
     * @param time the time when the change occurred
     * @param delta the changed delta of the change
     * @return the created resource macro
     */
    private ResourceMacro createResourceChangedMacro(long time, IJavaElementDelta delta) {
        IJavaElement elem = delta.getElement();
        String target = getTarget(elem);
        String path = elem.getPath().toString();
        String ipath = null;
        
        if (path == null) {
            return null;
        }
        
        if ((delta.getFlags() & IJavaElementDelta.F_CONTENT) != 0) {
            System.out.println("CONTENT CHANGED " + path);
        }
        
        String code = getCode(elem);
        return new ResourceMacro(time, "Changed", path, target, ipath, code);
    }
    
    /**
     * Obtains source code of a resource
     * @param resource the changed resource 
     * @return the contents of the source code
     */
    private String getCode(IJavaElement elem) {
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
     * Tests if the element was renamed by the change.
     * @param before the element before the change
     * @param after the element after the change
     * @param type the type of the element
     * @return <code>true</code> if renaming was applied, otherwise <code>false</code>
     */
    private boolean isRenamed(IJavaElement before, IJavaElement after, int type) {
        if (before == null || after == null) {
            return true;
        }
        
        String beforen = getElementName(before, type);
        String aftern = getElementName(after, type);
        if (beforen.compareTo(aftern) != 0) {
            return true;
        }
        return false;
    }
    
    /**
     * Obtains the name of the specified element.
     * @param elem the element
     * @param type the type of the element
     * @return the name string
     */
    private String getElementName(IJavaElement elem, int type) {
        if (type == IJavaElement.JAVA_PROJECT) {
            return elem.getResource().getName();
            
        } else if (type == IJavaElement.PACKAGE_FRAGMENT) {
            IPackageFragment jpackage = (IPackageFragment)elem;
            return jpackage.getElementName();
            
        } else if (type == IJavaElement.COMPILATION_UNIT) {
            return elem.getResource().getName();
        }
        
        return "";
    }
    
    /**
     * Returns the target of the change.
     * @param elem the changed element
     * @return the target of the change,
     *         or <code>null</code> if the target is not either a project, package, or file
     */
    private String getTarget(IJavaElement elem) {
        if (elem == null) {
            return null;
        }
        
        int type = elem.getElementType();
        if (type == IJavaElement.JAVA_PROJECT) {
            return "Project";
            
        } else if (type == IJavaElement.PACKAGE_FRAGMENT) {
            return "Package";
            
        } else if (type == IJavaElement.COMPILATION_UNIT) {
            return "File";
        }
        return null;
    }
    
    /**
     * Collects change deltas.
     */
    class ChangeCollector {
        
        /**
         * The collection of change deltas
         */
        List<IJavaElementDelta> deltas = new ArrayList<IJavaElementDelta>();
        
        /**
         * Creates an object that collects the deltas of element changes.
         * @param delta the root delta of the change
         */
        ChangeCollector(IJavaElementDelta delta) {
            collectDeltas(delta);
        }
        
        /**
         * Collects all the deltas of the changes.
         * @param delta the root delta of the change
         * @param deltas the collection of the deltas to be collected
         */
        private void collectDeltas(IJavaElementDelta delta) {
            if (delta.getKind() == IJavaElementDelta.ADDED ||
                delta.getKind() == IJavaElementDelta.REMOVED) {
                if (!contain(delta)) {
                    deltas.add(delta);
                }
            } else if (delta.getKind() == IJavaElementDelta.CHANGED &&
                    ((delta.getFlags() & IJavaElementDelta.F_CONTENT) != 0)) {
                if (!contain(delta)) {
                    deltas.add(delta);
                }
            }
            
            for (IJavaElementDelta d : delta.getAffectedChildren()) {
                collectDeltas(d);
            }
        }
        
        /**
         * Tests if a given change delta was already contained in the change collection.
         * @param delta the change delta
         * @return <code>true</code> if the change delta was contained in the change collection, otherwise <code> false</code>
         */
        private boolean contain(IJavaElementDelta delta) {
            String path = delta.getElement().getPath().toString();
            for (IJavaElementDelta d : deltas) {
                String p = d.getElement().getPath().toString();
                if (p.compareTo(path) == 0 && d.getKind() == delta.getKind()) {
                    return true;
                }
            }
            return false;
        }
    }
}
