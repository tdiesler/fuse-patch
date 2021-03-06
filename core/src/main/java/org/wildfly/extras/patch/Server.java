/*
 * #%L
 * Fuse Patch :: Core
 * %%
 * Copyright (C) 2015 Private
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.wildfly.extras.patch;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * A server instance.
 *
 * @author thomas.diesler@jboss.com
 * @since 10-Jun-2015
 */
public interface Server {

	/**
	 * Get the server home path
	 */
	Path getServerHome();
	
	/**
	 * Get the default repository path
	 */
    Path getDefaultRepositoryPath();
    
    /**
     * Get the audit log
     */
    List<String> getAuditLog();
    
	/**
	 * Query the list of applied packages
	 */
	List<PatchId> queryAppliedPackages();

	/**
	 * Query managed server paths
	 */
    List<ManagedPath> queryManagedPaths(String pathsPattern);
    
    /**
     * Get the applied package for a given prefix
     * @return package or null
     */
	Package getPackage(String prefix);

    /**
     * Get the applied package for the given id
     */
    Package getPackage(PatchId patchId);
    
	/**
	 * Apply a smart patch and return the result
	 */
	Package applySmartPatch(SmartPatch smartPatch, boolean force) throws IOException;
}
