/*******************************************************************************
 * Copyright (c) 2005, 2009 committers of openArchitectureWare and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     committers of openArchitectureWare - initial API and implementation
 *******************************************************************************/
package org.eclipse.xtend.typesystem.emf.ui;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.converter.util.ConverterUtil;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.impl.EPackageRegistryImpl;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.xtend.shared.ui.Activator;
import org.eclipse.xtend.shared.ui.MetamodelContributor;
import org.eclipse.xtend.shared.ui.core.IModelManager;
import org.eclipse.xtend.shared.ui.core.internal.JDTUtil;
import org.eclipse.xtend.shared.ui.core.internal.ResourceID;
import org.eclipse.xtend.shared.ui.core.metamodel.MetamodelContributorRegistry;
import org.eclipse.xtend.typesystem.emf.ui.internal.EmfToolsLog;

/**
 * Analyzes a project's classpath for ecore metamodels. We need to take care of Ecore files
 * <ul>
 * <li>directly contained in the classpath</li>
 * <li>in JARs within the classpath</li>
 * <li>in the classpath of projects referenced by this project (recursively)</li>
 * <li>in plugins referenced by the project, either within the target platform or as referenced plugin projects
 * (recursively)</li>
 * </ul>
 * <p>
 * Reading Ecore files occurs in background Jobs. Each Job uses its own ResourceSet. This avoids concurrency issues.
 * </p>
 */
final class ProjectAnalyzer extends Job {

  private final IJavaProject project;
  private ResourceSet rs;
  private Map<IStorage, Resource> mapping;
  private Map<String, EPackage> packages;

  public ProjectAnalyzer(final IJavaProject project) {
    super(Messages.ProjectAnalyzer_AnalysingPrompt + project.getProject().getProject().getName());
    // see bug https://bugs.eclipse.org/bugs/show_bug.cgi?id=268516
    setRule(project.getProject().getWorkspace().getRoot());
    this.project = project;
  }

  @Override
  protected IStatus run(final IProgressMonitor monitor) {
    if (EmfToolsPlugin.trace) {
      System.out.println(Messages.ProjectAnalyzer_1 + this.project.getProject().getProject().getName());
    }

    // load models
    this.rs = ConverterUtil.createResourceSet();
    this.mapping = new HashMap<IStorage, Resource>();
    // TODO: ensure that the packages map contains the ePackages from
    // reexported projects as well
    this.packages = new HashMap<String, EPackage>();
    loadMetamodelsForProject(this.project, this.rs, monitor);
    // always add ecore
    this.packages.put(EcorePackage.eNS_URI, EcorePackage.eINSTANCE);

    EPackage.Registry registry = new EPackageRegistryImpl(this.rs.getPackageRegistry());
    registry.putAll(this.packages);
    this.rs.setPackageRegistry(registry);

    // done. now trigger build for the project.
    // only do this if it is an Xpand project
    // do not build referencing projects. the EmfToolsPlugin will take care
    // of this
    IModelManager extXptModelManager = Activator.getExtXptModelManager();
    if ((extXptModelManager != null) && (extXptModelManager.findProject(this.project.getProject()) != null)) {
      new BuildJob(this.project.getProject(), null).schedule();
    }

    return Status.OK_STATUS;
  }

  private void loadMetamodelsForProject(final IJavaProject javaProject, final ResourceSet rs,
      final IProgressMonitor monitor) {
    try {
      final String ext = "ecore"; //$NON-NLS-1$
      if (!javaProject.exists()) {
        return;
      }
      for (final IPackageFragmentRoot root : javaProject.getPackageFragmentRoots()) {
        if (!root.isArchive()) {
          IResource rootResource = null;
          if (root.isExternal()) {
            rootResource = JDTUtil.getExternalFolder(root);
          }
          else {
            rootResource = root.getUnderlyingResource();
          }
          if (rootResource != null) {
            try {
              if (!rootResource.exists() || !rootResource.isAccessible()) {
                rootResource.refreshLocal(IResource.DEPTH_INFINITE, monitor);
              }
              rootResource.accept(new IResourceVisitor() {

                public boolean visit(final IResource resource) throws CoreException {
                  if ((resource.getType() == IResource.FILE) && ext.equals(((IFile) resource).getFileExtension())) {
                    loadModelFromStorage(rs, (IFile) resource);
                  }
                  return true;
                }
              });
            }
            catch (final CoreException e) {
              EmfToolsLog.logError(e);
            }
          }
        }
        else {
          // skip JRE jars
          if (root.getPath().toString().contains("jre/lib")) { //$NON-NLS-1$
            if (EmfToolsPlugin.trace) {
              System.out.println(Messages.ProjectAnalyzer_4 + root.getPath().toString());
            }
            continue;
          }

          root.open(monitor);
          try (final ZipFile zip = JDTUtil.openArchive(root)) {
            final Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
              final ZipEntry entry = entries.nextElement();
              final String name = entry.getName();
              if (name.endsWith(ext)) {
                final String fqn = name.substring(0, name.length() - ext.length() - 1).replace("/", "::"); //$NON-NLS-1$
                final ResourceID resourceID = new ResourceID(fqn, ext);
                final IStorage findStorage = JDTUtil.loadFromJar(resourceID, root);
                if (findStorage != null) {
                  loadModelFromStorage(rs, findStorage);
                }
              }
            }
          }
          catch (final CoreException | IOException e) {
            EmfToolsLog.logError(e);
          }
          finally {
            root.close();
          }
        }
      }
    }
    catch (final JavaModelException e) {
      EmfToolsLog.logError(e);
    }
  }

  private void loadModelFromStorage(final ResourceSet rs, final IStorage storage) {
    final URI uri = URI.createPlatformResourceURI(storage.getFullPath().toString(), true);
    if (EmfToolsPlugin.trace) {
      System.out.println(Messages.ProjectAnalyzer_7 + storage.getFullPath().toString());
    }

    final Resource r = rs.createResource(uri);
    if (r.isLoaded() && !r.isModified()) {
      return;
    }
    try {
      r.load(storage.getContents(), Collections.EMPTY_MAP);
      this.mapping.put(storage, r);
      registerEPackages(rs, r, false);
    }
    catch (final IOException e) {
      EmfToolsLog.logError(Messages.ProjectAnalyzer_8 + uri, e);
    }
    catch (final CoreException e) {
      EmfToolsLog.logError(Messages.ProjectAnalyzer_9 + uri, e);
    }
    catch (final RuntimeException e) {
      EmfToolsLog.logError(Messages.ProjectAnalyzer_10 + uri, e);
    }
  }

  private void registerEPackages(final ResourceSet rs, final Resource r, final boolean overwrite) {
    final Collection<EPackage> packages = EcoreUtil.getObjectsByType(r.getContents(), EcorePackage.Literals.EPACKAGE);
    for (final EPackage pack : packages) {
      registerPackage(pack, rs, overwrite);
    }
  }

  private void registerPackage(final EPackage pack, final ResourceSet rs, final boolean overwrite) {
    // finding duplicates by nsURI is better than by name since package
    // names may be used across MMs
    if (!overwrite && this.packages.containsKey(pack.getNsURI())) {
      if (EmfToolsPlugin.trace) {
        System.out.println(Messages.ProjectAnalyzer_11 + pack.getName() + Messages.ProjectAnalyzer_12);
      }
    }
    else {
      this.packages.put(pack.getNsURI(), pack);
    }
    // recurse into subpackages
    for (final EPackage p : pack.getESubpackages()) {
      registerPackage(p, rs, overwrite);
    }
  }

  public Map<String, EPackage> getNamedEPackageMap() {
    if (this.packages == null) {
      run(new NullProgressMonitor());
    }
    return Collections.unmodifiableMap(this.packages);
  }

   /**
   * Checks if {@link EmfMetamodelContributor} is activated either in project property or in preference
   * 
   * @return true if {@link EmfMetamodelContributor} is available and active, else false
   */
  public boolean isEnabled() {
    List<? extends MetamodelContributor> activeMetamodelContributors =
        MetamodelContributorRegistry.getActiveMetamodelContributors(this.project);
    for (MetamodelContributor metamodelContributor : activeMetamodelContributors) {
      if (metamodelContributor instanceof EmfMetamodelContributor) {
        return true;
      }
    }
    return false;
  }  
  
}