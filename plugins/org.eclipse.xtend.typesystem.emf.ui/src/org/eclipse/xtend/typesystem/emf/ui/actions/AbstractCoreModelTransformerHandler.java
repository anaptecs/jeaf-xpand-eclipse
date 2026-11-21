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
package org.eclipse.xtend.typesystem.emf.ui.actions;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

/**
 * Loads the selected Ecore model, transforms it and saves it again.
 */
public abstract class AbstractCoreModelTransformerHandler extends AbstractHandler {

	protected static final String ANNO_SOURCE = "http://www.eclipse.org/emf/2002/GenModel";

	protected static final String ANNO_KEY = "body";

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException {
		final IFile file = selectedFile(event);
		if (file == null) {
			return null;
		}
		final URI fileURI = URI.createPlatformResourceURI(file.getFullPath().toPortableString(), true);
		final Resource r = new ResourceSetImpl().createResource(fileURI);
		try {
			r.load(null);
			transform(r);
			r.save(null);
		}
		catch (final IOException e) {
			MessageDialog.openError(HandlerUtil.getActiveShell(event), Messages.AbstractCoreModelTransformerAction_PluginName,
					e.getMessage());
			throw new ExecutionException(e.getMessage(), e);
		}
		return null;
	}

	public abstract void transform(Resource r);

	private static IFile selectedFile(final ExecutionEvent event) {
		ISelection selection = HandlerUtil.getActiveMenuSelection(event);
		if (selection == null) {
			selection = HandlerUtil.getCurrentSelection(event);
		}
		if (selection instanceof IStructuredSelection) {
			return Adapters.adapt(((IStructuredSelection) selection).getFirstElement(), IFile.class);
		}
		return null;
	}
}
