/*******************************************************************************
 * Copyright (c) 2026 anaptecs GmbH and others. All rights reserved. This program
 * and the accompanying materials are made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.xtend.shared.ui.core.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.xtend.shared.ui.Activator;

/**
 * Entry of an archive (e.g. Xpand template in a JAR file). In contrast to <code>ZipEntryStorage</code> the archive is
 * only opened while the content is read. Storages are kept by cached resources, so an archive would stay open (and
 * locked on Windows) as long as Eclipse runs otherwise.
 */
public class ArchiveEntryStorage extends PlatformObject implements IStorage {
	private final File archive;

	private final String entryName;

	public ArchiveEntryStorage(final File archive, final String entryName) {
		this.archive = archive;
		this.entryName = entryName;
	}

	@Override
	public InputStream getContents() throws CoreException {
		try (ZipFile zip = new ZipFile(archive)) {
			ZipEntry entry = zip.getEntry(entryName);
			if (entry == null) {
				throw new CoreException(
						new Status(IStatus.ERROR, Activator.getId(), "Entry " + entryName + " not found in " + archive));
			}
			try (InputStream in = zip.getInputStream(entry)) {
				return new ByteArrayInputStream(in.readAllBytes());
			}
		}
		catch (IOException e) {
			throw new CoreException(
					new Status(IStatus.ERROR, Activator.getId(), "Entry " + entryName + " can't be read from " + archive, e));
		}
	}

	@Override
	public IPath getFullPath() {
		return IPath.fromOSString(archive.getPath()).append(entryName);
	}

	@Override
	public String getName() {
		int index = entryName.lastIndexOf('/');
		return index == -1 ? entryName : entryName.substring(index + 1);
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	public File getArchive() {
		return archive;
	}

	public String getEntryName() {
		return entryName;
	}

	@Override
	public boolean equals(final Object object) {
		if (object instanceof ArchiveEntryStorage) {
			ArchiveEntryStorage other = (ArchiveEntryStorage) object;
			return archive.equals(other.archive) && entryName.equals(other.entryName);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return entryName.hashCode();
	}

	@Override
	public String toString() {
		return getFullPath().toString();
	}
}
