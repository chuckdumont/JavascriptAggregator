/*
 * (C) Copyright 2012, IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ibm.jaggr.core.impl.resource;

import com.ibm.jaggr.core.IAggregator;
import com.ibm.jaggr.core.IAggregatorExtension;
import com.ibm.jaggr.core.IExtensionInitializer;
import com.ibm.jaggr.core.resource.IResource;
import com.ibm.jaggr.core.resource.IResourceConverter;

import java.io.File;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converter for jsx files.  If the requested JavaScript file doesn't exist, then
 * check to see if there's a .jsx file in the same directory with the same base name
 * and if there is, then return the compiled output as the converted resource.
 */
public class JsxResourceConverter implements IResourceConverter, IExtensionInitializer {
	private static final String sourceClass = JsxResourceConverter.class.getName();
	private static final Logger log = Logger.getLogger(sourceClass);

	private static final String JSX_CACHE_DIRNAME = "jsx"; //$NON-NLS-1$
	private IAggregator aggregator;
	private File cacheDirectory;

	/* (non-Javadoc)
	 * @see com.ibm.jaggr.core.IExtensionInitializer#initialize(com.ibm.jaggr.core.IAggregator, com.ibm.jaggr.core.IAggregatorExtension, com.ibm.jaggr.core.IExtensionInitializer.IExtensionRegistrar)
	 */
	@Override
	public void initialize(IAggregator aggregator, IAggregatorExtension extension,
			IExtensionRegistrar registrar) {
		final String sourceMethod = "initialize"; //$NON-NLS-1$
		final boolean isTraceLogging = log.isLoggable(Level.FINER);
		if (isTraceLogging) {
			log.entering(sourceMethod, sourceMethod, new Object[]{aggregator, extension});
		}
		this.aggregator = aggregator;
		if (isTraceLogging) {
			log.exiting(sourceClass, sourceMethod);
		}
	}

	/* (non-Javadoc)
	 * @see com.ibm.jaggr.core.resource.IResourceConverter#convert(java.net.URI, com.ibm.jaggr.core.resource.IResource)
	 */
	@Override
	public IResource convert(final URI uri, final IResource resource) {
		final String sourceMethod = "convert"; //$NON-NLS-1$
		final boolean isTraceLogging = log.isLoggable(Level.FINER);
		if (isTraceLogging) {
			log.entering(sourceMethod, sourceMethod, new Object[]{uri, resource});
		}
		IResource result = resource;
		// is the request for a JavaScript file
		if (resource.getPath().endsWith(".js")) { //$NON-NLS-1$
			if (!resource.exists()){
				// Construct cache file name from the resource path, replacing characters that are
				// invalid in a path component.
				String cacheName = resource.getPath().replace(":", "$").replace("/", "|"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
				File cacheDir = getCacheDirectory();
				File cacheFile = new File(cacheDir, cacheName);
				// Look for the file in the cache directory.
				if (cacheFile.exists() && !aggregator.getOptions().isDevelopmentMode()) {
					// don't bother checking last modified if not in development mode
					result = aggregator.newResource(cacheFile.toURI());
				} else {
					// build uri to the jsx resource
					String path = resource.getPath();
					int idx = path.lastIndexOf("/"); //$NON-NLS-1$
					String name = path.substring(idx == -1 ? 0 : idx+1, path.length()-3) + ".jsx"; //$NON-NLS-1$
					URI jsxResUri = uri.resolve(name);
					IResource jsxRes = aggregator.newResource(jsxResUri);
					// see if the jsx resource exists.
					if (jsxRes.exists()) {
						// There's a jsx resource that can satisfy the request
						if (cacheFile.exists() && cacheFile.lastModified() == jsxRes.lastModified()) {
							// source jsx is not changed, so return the cache entry
							result = aggregator.newResource(cacheFile.toURI());
						} else {
							// Compile the jsx resource to the cache file and return the cache file resource
							// Need some synchronization here so that we don't compile
							// the same module more than once on different threads, but still
							// allow compiles for different modules to proceed concurrently
							if (compileJsxResource(jsxRes, cacheFile)) {
								result = aggregator.newResource(cacheFile.toURI());
							}
						}
					}
				}
			}
		}
		if (isTraceLogging) {
			log.exiting(sourceClass, sourceMethod, result);
		}
		return result;
	}

	/**
	 * Compile the specified .jsx resource to the specified output file.
	 *
	 * @param jsxRes
	 *            the input resource to compile
	 * @param target
	 *            the target file to compile to
	 * @return true if the file was successfully compiled
	 */
	boolean compileJsxResource(final IResource jsxRes, final File target) {
		final String sourceMethod = "compileJsxResource"; //$NON-NLS-1$
		final boolean isTraceLogging = log.isLoggable(Level.FINER);
		if (isTraceLogging) {
			log.entering(sourceMethod, sourceMethod, new Object[]{jsxRes, target});
		}
		boolean result = false;	// not yet implemented
		if (isTraceLogging) {
			log.exiting(sourceClass, sourceMethod, result);
		}
		return result;
	}

	/**
	 * Returns the cache directory for compiled jsx files.  Note that we can't initialize the
	 * cache directory in our extension initializer because the cache manager hasn't been
	 * initialized yet.
	 *
	 * @return the cache directory
	 */
	protected File getCacheDirectory() {
		if (cacheDirectory == null) {
			synchronized(this) {
				if (cacheDirectory == null) {
					cacheDirectory = new File(aggregator.getCacheManager().getCacheDir(), JSX_CACHE_DIRNAME);
					cacheDirectory.mkdir();
				}
			}
		}
		return cacheDirectory;
	}

}
