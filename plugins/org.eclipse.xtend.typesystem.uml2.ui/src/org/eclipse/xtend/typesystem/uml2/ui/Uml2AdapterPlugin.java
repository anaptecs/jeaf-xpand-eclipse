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

package org.eclipse.xtend.typesystem.uml2.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.URIConverter;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.uml2.uml.Profile;
import org.eclipse.uml2.uml.resource.UML22UMLResource;
import org.osgi.framework.BundleContext;

/**
 * The main plug-in class to be used in the desktop.
 *
 * TODO plug-in and profiles initialization should be separated into different classes... but this would be a larger refactoring
 */
public class Uml2AdapterPlugin extends AbstractUIPlugin {
	// A resource set to store the loaded profiles in
	private static ResourceSet profilesResourceSet;

	// for guarding resource initialization
	private static final AtomicBoolean initializingResources = new AtomicBoolean(false);

	// these listener are notified whenever a new profile has been loaded by this plug-in
	private final List<UmlPluginListener> pluginListener = new ArrayList<UmlPluginListener>();

	/**
	 * This is a trick to ensure that there are no deadlocks and initialization issues
	 * when a) this plug-in is started and b) other plug-ins access the {@link #profilesResourceSet}
	 * and/or c) there are changes in the workspace.
	 */
	private static class FieldInitializer {
		static final ResourceSet profilesResourceSet;

		static final Map<IResource, Profile> fileModels;

		static {
			profilesResourceSet = new ResourceSetImpl();
			fileModels = new HashMap<IResource, Profile>();

			try {
				IWorkspace workspace = ResourcesPlugin.getWorkspace();
				workspace.getRoot().accept(
						new Uml2AdapterPlugin.ProfileResourceDeltaVisitor(profilesResourceSet, fileModels));
			}
			catch (CoreException e) {
				Uml2AdapterLog.logError(e);
			}
		}
	}

	/**
	 * Safely get the {@link ResourceSet} by guarding its initialization
	 * @since 2.0
	 */
	public static ResourceSet getProfilesResourceSet() {
		if (profilesResourceSet == null) {
			try {
				initializingResources.set(true);
				profilesResourceSet = FieldInitializer.profilesResourceSet;
			} finally {
				initializingResources.set(false);
			}
		}
		return profilesResourceSet;
	}

	static class ProfileResourceDeltaVisitor implements IResourceDeltaVisitor, IResourceVisitor {

		private final ResourceSet profilesResourceSet;

		private final Map<IResource, Profile> fileModels;

		public ProfileResourceDeltaVisitor(ResourceSet profilesResourceSet, Map<IResource, Profile> fileModels) {
			super();
			this.profilesResourceSet = profilesResourceSet;
			this.fileModels = fileModels;
		}

        public boolean visit(IResourceDelta delta) {

            if (delta.getKind() == IResourceDelta.REMOVED) {
                Uml2AdapterPlugin.removeFileModel(delta.getResource());
                return true;
            } else {
                IResource resource = delta.getResource();
                try {
                    return visit(resource);
                } catch (CoreException e) {
                    Uml2AdapterLog.logError(e);
                    return true;
                }
            }
        }

        public boolean visit(IResource resource) throws CoreException {
            if (resource instanceof IFile) {
                IFile f = (IFile) resource;
                if (isValidProfile(f)) {
                    if (JavaCore.create(f.getParent()) != null) {
                        try {
                            URI uri = URI.createURI(f.getFullPath().toString());
                            Profile p = loadProfile(uri);
                            if (p != null) {
                                fileModels.put(f, p);
                            }
                            // notify listeners
                            final List<UmlPluginListener> pluginListener = Uml2AdapterPlugin.getDefault().pluginListener;
                            for (UmlPluginListener listener : pluginListener)
                            {
                               listener.profileLoaded(f, p);
                            }
                        } catch (Exception e) {
                            Uml2AdapterLog.logError(e);
                        }
                    }
                }
            }
            return true;
        }

        private boolean isValidProfile(IFile f) {
            return !f.isDerived() && f.isAccessible() && !f.isLinked() && (f.getName().endsWith("profile.uml2") || f.getName().endsWith("profile.uml"))&& f.exists();
        }

        public synchronized final Profile loadProfile(URI uri) {
             Resource r = profilesResourceSet.getResource(uri, false);
             if (r == null) {
                 // this resource has not been loaded before...
                 // hence, demandLoad it
                 r = profilesResourceSet.getResource(uri, true);
             } else {
                 if (r.isLoaded()) { // Is this a request to reload the resource?
                     r.unload();
                 }
                 try {
                    r.load(new HashMap<Object,Object>());
                 } catch (IOException e) {
                    throw new RuntimeException(e);
                 }                         
             }
            
            List<EObject> c = r.getContents();
            if (c.isEmpty()) {
               return null;
            }
            if (c.get(0) instanceof Profile) {
               Profile p = (Profile) c.get(0);
               URIConverter.URI_MAP.put(URI.createURI(uri.lastSegment()), uri);
               return p;
            }
            return null;
         }
    }

	// TODO should be private
    static Map<IResource, Profile> fileModels;

    // guard resource initialization
	private static Map<IResource, Profile> getFileModelsInternal() {
		if (fileModels == null) {
			try {
				initializingResources.set(true);
				fileModels = FieldInitializer.fileModels;
			}
			finally {
				initializingResources.set(false);
			}
		}
		return fileModels;
	}

    public final static Map<IResource, Profile> getFileModels() {
        return Collections.unmodifiableMap(getFileModelsInternal());
    }

    public static void removeFileModel(IResource resource) {
        URI uri = URI.createURI(resource.getFullPath().toString());
             Resource r = profilesResourceSet.getResource(uri, false);
             if (r != null) {
                             if (r.isLoaded()) // Is this a request to reload the resource?
                                             r.unload();
             }
        getFileModelsInternal().remove(resource);
    }

    // The shared instance.
    private static Uml2AdapterPlugin plugin;

    private static IResourceChangeListener listener = new IResourceChangeListener() {
        public void resourceChanged(IResourceChangeEvent event) {
            // we are only interested in POST_CHANGE events
            if (event.getType() != IResourceChangeEvent.POST_CHANGE)
                return;
            IResourceDelta rootDelta = event.getDelta();
            try {
            	if (!initializingResources.get()) {
            		rootDelta.accept(new Uml2AdapterPlugin.ProfileResourceDeltaVisitor(getProfilesResourceSet(), getFileModelsInternal()));
            	}
            } catch (CoreException e) {
                Uml2AdapterLog.logError(e);
            }

        }
    };

    protected static List<EObject> loadContents(IFile file) {
        try {
            URI uri = URI.createFileURI(file.getFullPath().toString());
            Resource r = new ResourceSetImpl().createResource(uri);
            r.load(file.getContents(), Collections.EMPTY_MAP);
            return r.getContents();
        } catch (Exception e) {
            Uml2AdapterLog.logError(e);
        }
        return null;
    }

    /**
     * The constructor.
     */
    public Uml2AdapterPlugin() {
        plugin = this;
    }

	/**
	 * @since 2.0
	 */
    public void registerPluginListener(final UmlPluginListener listener)
    {
       this.pluginListener.add(listener);
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put("uml2", UML22UMLResource.Factory.INSTANCE);
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        workspace.addResourceChangeListener(listener);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        super.stop(context);
        plugin = null;
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        workspace.removeResourceChangeListener(listener);
    }

    /**
     * Returns the shared instance.
     */
    public static Uml2AdapterPlugin getDefault() {
        return plugin;
    }

    /**
     * Returns an image descriptor for the image file at the given plug-in
     * relative path.
     * 
     * @param path
     *            the path
     * @return the image descriptor
     */
    public static ImageDescriptor getImageDescriptor(String path) {
        return AbstractUIPlugin.imageDescriptorFromPlugin(getId(), path);
    }

    public static String getId() {
        return getDefault().getBundle().getSymbolicName();
    }

}
