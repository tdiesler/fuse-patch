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
package org.wildfly.extras.patch.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wildfly.extras.patch.ManagedPath;
import org.wildfly.extras.patch.PatchException;
import org.wildfly.extras.patch.PatchId;
import org.wildfly.extras.patch.PatchTool;
import org.wildfly.extras.patch.PatchToolBuilder;

public class Main {

	private static final Logger LOG = LoggerFactory.getLogger(Main.class);
	
    public static void main(String[] args) {
        try {
            mainInternal(args);
        } catch (Throwable th) {
            Runtime.getRuntime().exit(1);
        }
    }

    // Entry point with no system exit
    public static void mainInternal(String[] args) throws Exception {
        
        Options options = new Options();
        CmdLineParser parser = new CmdLineParser(options);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            helpScreen(parser);
            return;
        }

        try {
        	run(parser, options);
        } catch (PatchException ex) {
            LOG.error("ERROR {}", ex.getMessage());
            LOG.debug("Patch Exception", ex);
            throw ex;
        } catch (Throwable th) {
            LOG.error("Error executing command", th);
            throw th;
        }
    }

	private static void run(CmdLineParser cmdParser, Options options) throws IOException {
		
	    boolean opfound = false;
	    
        // Query the repository
        if (options.queryRepository) {
            PatchTool patchTool = new PatchToolBuilder().repositoryUrl(options.repositoryUrl).build();
            printPatches(patchTool.getRepository().queryAvailable(null));
            opfound = true;
        } 
        
        // Query the server
        if (options.queryServer) {
            PatchTool patchTool = new PatchToolBuilder().serverPath(options.serverHome).build();
            printPatches(patchTool.getServer().queryAppliedPackages());
            opfound = true;
        } 
        
        // Query the server paths
        if (options.queryServerPaths != null) {
            PatchTool patchTool = new PatchToolBuilder().serverPath(options.serverHome).build();
            List<String> managedPaths = new ArrayList<>();
            for (ManagedPath managedPath : patchTool.getServer().queryManagedPaths(options.queryServerPaths)) {
                managedPaths.add(managedPath.toString());
            }
            printLines(managedPaths);
            opfound = true;
        } 
        
        // Add to repository
        if (options.addUrl != null) {
            PatchTool patchTool = new PatchToolBuilder().repositoryUrl(options.repositoryUrl).build();
            PatchId oneoffId = null;
            Set<PatchId> dependencies = new LinkedHashSet<>();
            if (options.oneoffId != null) {
                oneoffId = PatchId.fromString(options.oneoffId);
                dependencies.add(oneoffId);
            }
            if (options.dependencies != null) {
                for (String depid : options.dependencies) {
                    dependencies.add(PatchId.fromString(depid));
                }
            }
            patchTool.getRepository().addArchive(options.addUrl, oneoffId, dependencies, options.force);
            opfound = true;
        }
        
        // Remove from repository
        if (options.removeId != null) {
            PatchTool patchTool = new PatchToolBuilder().repositoryUrl(options.repositoryUrl).build();
            patchTool.getRepository().removeArchive(PatchId.fromString(options.removeId));
            opfound = true;
        }
        
        // Add post install command
        if (options.addCmd != null) {
            PatchTool patchTool = new PatchToolBuilder().repositoryUrl(options.repositoryUrl).build();
            PatchId patchId;
            String[] cmdarr;
            if (options.addUrl != null) {
                patchId = PatchId.fromURL(options.addUrl);
                cmdarr = options.addCmd;
            } else {
                patchId = PatchId.fromString(options.addCmd[0]);
                cmdarr = Arrays.copyOfRange(options.addCmd, 1, options.addCmd.length);
            }
            patchTool.getRepository().addPostCommand(patchId, cmdarr);
            opfound = true;
        }
        
        // Install to server
        if (options.installId != null) {
            PatchTool patchTool = new PatchToolBuilder().serverPath(options.serverHome).repositoryUrl(options.repositoryUrl).build();
            patchTool.install(PatchId.fromString(options.installId), options.force);
            opfound = true;
        }
        
        // Update the server
        if (options.updateName != null) {
            PatchTool patchTool = new PatchToolBuilder().serverPath(options.serverHome).repositoryUrl(options.repositoryUrl).build();
            patchTool.update(options.updateName, options.force);
            opfound = true;
        } 
        
        // Install to server
        if (options.uninstallId != null) {
            PatchTool patchTool = new PatchToolBuilder().serverPath(options.serverHome).build();
            patchTool.uninstall(PatchId.fromString(options.uninstallId), options.force);
            opfound = true;
        }
        
        // Print the audit log
        if (options.auditLog) {
            PatchTool patchTool = new PatchToolBuilder().serverPath(options.serverHome).build();
            printLines(patchTool.getServer().getAuditLog());
            opfound = true;
        } 
        
		// Show help screen
		if (!opfound) {
            helpScreen(cmdParser);
		}
	}

    private static void helpScreen(CmdLineParser cmdParser) {
        System.err.println("fusepatch [options...]");
        cmdParser.printUsage(System.err);
    }

	private static void printLines(List<String> lines) {
	    for (String line : lines) {
	        System.out.println(line);
	    }
    }

    private static void printPatches(List<PatchId> patches) {
        for (PatchId patchId : patches) {
            System.out.println(patchId.toString());
        }
    }
}
