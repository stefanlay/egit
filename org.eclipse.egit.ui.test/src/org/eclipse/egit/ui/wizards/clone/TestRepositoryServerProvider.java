package org.eclipse.egit.ui.wizards.clone;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.egit.ui.IRepositoryServerProvider;

public class TestRepositoryServerProvider implements IRepositoryServerProvider {

	public Map<String, URI> getRepositoryServerURIs() {
		Map<String, URI> servers = new HashMap<String, URI>();
		servers.put("Local Gerrit", null);
		servers.put("EGit Gerrit", null);
		
		return servers;
	}

}
