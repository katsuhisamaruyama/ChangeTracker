/*
 *  Copyright 2014
 *  Software Science and Technology Lab.
 *  Department of Computer Science, Ritsumeikan University
 */

package org.jtool.changerepository.data;

import org.jtool.changerepository.Activator;
import org.jtool.changerepository.event.RepositoryEventSource;
import org.jtool.changerepository.event.RepositoryChangedEvent;
import org.jtool.changerepository.operation.UnifiedOperation;
import org.jtool.changerecorder.operation.IOperation;
import org.jtool.changerecorder.history.OperationHistory;
import org.jtool.changerecorder.history.Xml2Operation;
import org.jtool.changerecorder.util.XmlFileStream;
import org.jtool.changerecorder.util.Time;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.IWorkbenchWindow;
import org.w3c.dom.Document;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.InvocationTargetException;

/**
 * Collects information on a workspace and elements (projects, packages, and files) under it.
 * @author Katsuhisa Maruyama
 */
public class RepositoryManager {
    
    /**
     * The collection of all operations in this history repository.
     */
    private static List<UnifiedOperation> allOperations = new ArrayList<UnifiedOperation>(65536);
    
    /**
     * The map stores projects currently existing in this repository.
     */
    private static Map<String, ProjectInfo> projectInfoMap = new HashMap<String, ProjectInfo>();
    
    /**
     * The map stores packages currently existing in this repository.
     */
    private static Map<String, PackageInfo> packageInfoMap = new HashMap<String, PackageInfo>();
    
    /**
     * The map stores files currently existing in this repository.
     */
    private static Map<String, FileInfo> fileInfoMap = new HashMap<String, FileInfo>();
    
    /**
     * The information on the whole repository.
     */
    private static WorkspaceInfo workspaceInfo;
    
    /**
     * The top path for the directory containing the history files.
     */
    private static String dirpath;
    
    /**
     * The time when the repository information was last updated.
     */
    private static long lastUpdateTime = 0;
    
    /**
     * Sets the path of the directory containing the history files.
     * @param path the top path for the directory
     */
    public static void setDirectoryPath(String path) {
        dirpath = path;
        workspaceInfo = new WorkspaceInfo(dirpath, new ArrayList<UnifiedOperation>());
    }
    
    /**
     * Collects newly all operations stored in the history files existing in a specified directory.
     */
    public static void collectOperations() {
        if (dirpath == null) {
            return;
        }
        
        List<File> files = getAllHistoryFiles(dirpath);
        if (files.size() == 0) {
            return;
        }
        
        long updateTime = Time.getCurrentTime();
        
        if (allOperations.size() == 0) {
            collectOperations(files);
            
        } else {
            for (File file : files) {
                if (file.lastModified() > lastUpdateTime) {
                    clearAllInfo();
                    collectOperations(files);
                    break;
                }
            }
        }
        
        lastUpdateTime = updateTime;
    }
    
    /**
     * Clears all information related to the whole repository.
     */
    public static void clearAllInfo() {
        projectInfoMap.clear();
        packageInfoMap.clear();
        fileInfoMap.clear();
        
        allOperations.clear();
        workspaceInfo.clear();
        
        RepositoryChangedEvent evt =
          new RepositoryChangedEvent(new RepositoryManager(), RepositoryChangedEvent.Type.CLEAR);
        RepositoryEventSource.getInstance().fire(evt);
    }
    
    /**
     * Collects all operations stored in the history files existing in a specified directory.
     * @param targetFiles the collection of history files storing operations
     */
    private static void collectOperations(final List<File> files) {
        try {
            IWorkbenchWindow window = Activator.getWorkbenchWindow();
            window.run(false, true, new IRunnableWithProgress() {
                
                /**
                 * Reads history files existing in the specified directory.
                 * @param monitor the progress monitor to use to display progress and receive requests for cancellation
                 * @exception InterruptedException if the operation detects a request to cancel
                 */
                @Override
                public void run(IProgressMonitor monitor) throws InterruptedException {
                    monitor.beginTask("Extracting operations", files.size() * 2);
                    
                    readHistoryFiles(files, monitor);
                    workspaceInfo = new WorkspaceInfo(dirpath, allOperations);
                    
                    registOperations(monitor);
                    
                    workspaceInfo.setTimeRange();
                    workspaceInfo.fixMismatches();
                    
                    monitor.done();
                }
            });
            
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            System.err.println("Fails to read the history files " + e.getCause());
            clearAllInfo();
            return;
            
        } catch (InterruptedException e) {
            System.err.println("Fails to read the history files " + e.getCause());
            clearAllInfo();
            return;
        }
        
        RepositoryChangedEvent evt =
          new RepositoryChangedEvent(new RepositoryManager(), RepositoryChangedEvent.Type.UPDATE);
        RepositoryEventSource.getInstance().fire(evt);
    }
    
    /**
     * Obtains the information on the whole repository.
     * @return the information on the whole repository
     */
    public static WorkspaceInfo getWorkspaceInfo() {
        return workspaceInfo;
    }
    
    /**
     * Returns all the descendant history files of the specified directory.
     * @param path the path of the specified directory
     * @return the descendant files
     */
    private static List<File> getAllHistoryFiles(String path) {
        List<File> files = new ArrayList<File>();
        
        File dir = new File(path);
        if (dir.isFile()) {
            if (path.endsWith(".xml")) {
                files.add(dir);
            }
        } else if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            for (File f : children) {
                files.addAll(getAllHistoryFiles(f.getPath()));
            }
        }
        
        return files;
    }
    
    /**
     * Reads history files existing in the specified directory.
     * @param files the collection of the history files 
     * @param monitor the progress monitor to use to display progress and receive requests for cancellation
     * @throws InterruptedException if the operation detects a request to cancel
     */
    private static void readHistoryFiles(List<File> files, IProgressMonitor monitor) throws InterruptedException {
        for (File file : files) {
            String fpath = file.getAbsolutePath();
            Document doc = null;
            try {
                doc = XmlFileStream.read(fpath);
            } catch (Exception e) {
                System.err.println("Fails to read the history file: " + fpath);
            }
            
            OperationHistory history = Xml2Operation.convert(doc);
            
            if (history != null) {
                for (int i = 0; i < history.size(); i++) {
                    IOperation op = history.getOperation(i);
                    List<UnifiedOperation> ops = UnifiedOperation.create(op);
                    allOperations.addAll(ops);
                }
            }
            
            if (monitor.isCanceled()) {
                monitor.done();
                throw new InterruptedException();
            }
            
            monitor.worked(1);
        }
    }
    
    /**
     * Registers operations on information of their respective files, packages, and projects. 
     * @param monitor the progress monitor to use to display progress and receive requests for cancellation
     * @throws InterruptedException if the operation detects a request to cancel
     */
    private static void registOperations(IProgressMonitor monitor) throws InterruptedException {
        for (int idx = 0; idx < allOperations.size(); idx++) {
            UnifiedOperation op = allOperations.get(idx);
            
            String path = op.getFile();
            String projectName = getProjectName(path);
            String packageName = getPackageName(path);
            String fileName = getFileName(path);
            // System.out.println("OP = " + idx + " " + projectName + " " + packageName + " " + fileName);
            
            if (op.isResourceOperation()) {
                resourecChange(op, projectName, packageName, fileName);
                
            } else {
                registOperation(op, projectName, packageName, fileName);
            }
            
            if (monitor.isCanceled()) {
                monitor.done();
                throw new InterruptedException();
            }
            
            monitor.worked(1);
        }
    }
    
    /**
     * Registers resource change operations on information of their respective files.
     * @param op the operation to be stored
     * @param projectName the name of the project related to the stored operation
     * @param packageName the name of the package related to the stored operation
     * @param fileName the name of the file related to the stored operation
     */
    private static void resourecChange(UnifiedOperation op, String projectName, String packageName, String fileName) {
        if (op.isFileResourceOperation()) {
            if (op.isResourceAddedOperation()) {
                registOperation(op, projectName, packageName, fileName);
                
            } else if (op.isResourceRemovedOperation()) {
                registOperation(op, projectName, packageName, fileName);
                fileInfoMap.remove(fileName);
                
            } else if (op.isResourceMovedToOperation() || op.isResourceRenamedToOperation()) {
                registOperation(op, projectName, packageName, fileName);
                
            } else if (op.isResourceMovedFromOperation() || op.isResourceRenamedFromOperation()) {
                String oldPath = op.getIdenticalPath();
                String oldFileName = getFileName(oldPath);
                FileInfo oldFileInfo = fileInfoMap.get(oldFileName);
                fileInfoMap.remove(oldFileName);
                
                registOperation(op, projectName, packageName, fileName);
                FileInfo newFileInfo = fileInfoMap.get(fileName);
                
                oldFileInfo.setFileInfoTo(newFileInfo);
                newFileInfo.setFileInfoFrom(oldFileInfo);
            }
        }
    }
    
    /**
     * Registers operations on information of their respective files, packages, and projects.
     * @param op the operation to be stored
     * @param projectName the name of the project related to the stored operation
     * @param packageName the name of the package related to the stored operation
     * @param fileName the name of the file related to the stored operation
     */
    private static void registOperation(UnifiedOperation op, String projectName, String packageName, String fileName) {
        ProjectInfo projectInfo = projectInfoMap.get(getProjectKey(projectName));
        if (projectInfo == null) {
            projectInfo = new ProjectInfo(projectName, workspaceInfo);
            projectInfoMap.put(getProjectKey(projectName), projectInfo);
            
            workspaceInfo.addProjectInfo(projectInfo);
        }
        
        PackageInfo packageInfo = packageInfoMap.get(getPackageKey(projectName, packageName));
        if (packageInfo == null) {
            packageInfo = new PackageInfo(packageName, projectInfo);
            packageInfoMap.put(getPackageKey(projectName, packageName), packageInfo);
            
            projectInfo.addPackageInfo(packageInfo);
        }
        
        FileInfo fileInfo = fileInfoMap.get(getFileKey(projectName, packageName, fileName));
        if (fileInfo == null) {
            fileInfo = new FileInfo(fileName, op.getFile(), projectInfo, packageInfo);
            fileInfoMap.put(getFileKey(projectName, packageName, fileName), fileInfo);
            
            workspaceInfo.addFileInfo(fileInfo);
            projectInfo.addFileInfo(fileInfo);
            packageInfo.addFileInfo(fileInfo);
        }
        op.setFileInfo(fileInfo);
        fileInfo.addOperation(op);
    }
    
    /**
     * Returns the key for retrieval of project information.
     * @param projectName the project name
     * @return the key string for the project
     */
    private static String getProjectKey(String projectName) {
        return projectName;
    }
    
    /**
     * Returns the key for retrieval of package information.
     * @param projectName the project name
     * @param packageName the package name
     * @return the key string for the package
     */
    private static String getPackageKey(String projectName, String packageName) {
        return projectName + "%" + packageName;
    }
    
    /**
     * Returns the key for retrieval of file information.
     * @param projectName the project name
     * @param packageName the package name
     * @param fileName the file name
     * @return the key string for the file
     */
    private static String getFileKey(String projectName, String packageName, String fileName) {
        return projectName + "%" + packageName + "%" + fileName;
    }
    
    /**
     * Returns the name of the project under a given path.
     * @param the path for the file
     * @return the project name
     */
    private static String getProjectName(String path) {
        int firstIndex = path.indexOf('/', 1);
        if (firstIndex == -1) {
            return "Unknown";
        }
        
        return path.substring(1, firstIndex);
    }
    
    /**
     * Return the name of the package under a given path.
     * @param path the path for the file
     * @return the package name
     */
    private static String getPackageName(String path) {
        final String SRCDIR = "/src/";
        int firstIndex = path.indexOf(SRCDIR);
        int lastIndex = path.lastIndexOf('/') + 1;
        if (firstIndex == -1 || lastIndex == -1) {
            return "Unknown";
        }
        
        if (firstIndex + SRCDIR.length() == lastIndex) {
            return "(default package)";
        }
        
        String name = path.substring(firstIndex + SRCDIR.length(), lastIndex - 1);
        return name.replace('/', '.');
    }
    /**
     * Return the name of the file under a given path.
     * @param path the path for the file
     * @return the file name without its path information
     */
    private static String getFileName(String path) {
        if (path == null) {
            return "Unknown";
        }
        
        int lastIndex = path.lastIndexOf('/') + 1;
        if (lastIndex == -1) {
            return path;
        }
        return path.substring(lastIndex);
    }
}
