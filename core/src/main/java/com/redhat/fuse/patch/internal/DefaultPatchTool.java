/*
 * #%L
 * Fuse Patch :: Parser
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
package com.redhat.fuse.patch.internal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

import com.redhat.fuse.patch.PatchId;
import com.redhat.fuse.patch.PatchRepository;
import com.redhat.fuse.patch.PatchSet;
import com.redhat.fuse.patch.PatchTool;
import com.redhat.fuse.patch.ServerInstance;
import com.redhat.fuse.patch.SmartPatch;


public final class DefaultPatchTool implements PatchTool {

    private ServerInstance server;
    private PatchRepository repository;
    private Path serverPath;
    private URL repoUrl;
    
	public DefaultPatchTool(Path serverPath, URL repoUrl) {
	    this.serverPath = serverPath;
	    this.repoUrl = repoUrl;
    }

    @Override
    public List<PatchId> queryServer() {
		return getServerInstance().queryAppliedPatches();
	}

    @Override
    public List<PatchId> queryRepository() {
		return getPatchRepository().queryAvailable(null);
	}

    @Override
    public PatchId add(Path filePath) throws IOException {
        return getPatchRepository().addArchive(filePath);
    }

    @Override
    public void install(PatchId patchId) throws IOException {
        PatchId serverId = null;
        String symbolicName = patchId.getSymbolicName();
        for (PatchId pid : getServerInstance().queryAppliedPatches()) {
            if (pid.getSymbolicName().equals(symbolicName)) {
                serverId = pid;
            }
        }
        
        PatchSet latest = serverId != null ? getServerInstance().getAppliedPatchSet(serverId) : null;
        SmartPatch smartPatch = getPatchRepository().getSmartPatch(latest, patchId);
        getServerInstance().applySmartPatch(smartPatch);
    }
    
    @Override
    public void update(String symbolicName) throws IOException {
        PatchId serverId = null;
        for (PatchId pid : getServerInstance().queryAppliedPatches()) {
            if (pid.getSymbolicName().equals(symbolicName)) {
                serverId = pid;
            }
        }
        
        PatchId repoId = null;
        for (PatchId pid : getPatchRepository().queryAvailable(symbolicName)) {
            repoId = pid;
        }
        
        PatchSet latest = serverId != null ? getServerInstance().getAppliedPatchSet(serverId) : null;
        SmartPatch smartPatch = getPatchRepository().getSmartPatch(latest, repoId);
        getServerInstance().applySmartPatch(smartPatch);
    }

    private ServerInstance getServerInstance() {
        if (server == null) {
            server = new WildFlyServerInstance(serverPath);
        }
        return server;
    }

    private PatchRepository getPatchRepository() {
        if (repository == null) {
            if (repoUrl == null) {
                try {
                    repoUrl = getServerInstance().getDefaultRepositoryPath().toUri().toURL();
                } catch (MalformedURLException ex) {
                    throw new IllegalStateException(ex);
                }
            }
            repository = new DefaultPatchRepository(repoUrl);
        }
        return repository;
    }
}