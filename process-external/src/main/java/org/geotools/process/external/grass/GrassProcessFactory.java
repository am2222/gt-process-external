package org.geotools.process.external.grass;

import java.awt.RenderingHints.Key;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.ProcessFactory;
import org.geotools.util.SimpleInternationalString;
import org.opengis.feature.type.Name;
import org.opengis.util.InternationalString;

public class GrassProcessFactory implements ProcessFactory {

	private static final String GRASS_VERSION = "6.4.2";
	HashMap<Name, GrassProcess> processes;

	public GrassProcessFactory() {
		URL url = this.getClass().getResource("/grassdesc.txt");
		File file = new File(url.getFile());
		loadProcesses(file);
	}

	private void loadProcesses(File descFile) {
		processes = new HashMap<Name, GrassProcess>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(descFile));
			String line;
			String s;
			s = "";
			while ((line = br.readLine()) != null) {
				if (!line.startsWith("----")) {
					s += line + "\n";
				} else {
					GrassProcess proc = new GrassProcess(s);
					processes.put(new NameImpl("grass", proc.getName()), proc);
					s = "";
				}
			}
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean isAvailable() {
		return true;
	}

	@Override
	public Map<Key, ?> getImplementationHints() {
		return null;
	}

	@Override
	public Process create(Name name) {
		GrassProcess process = getProcess(name);
		return process;
	}

	public GrassProcess getProcess(Name name) {
		if (!processes.containsKey(name)) {
			throw new IllegalArgumentException("Unknown process: '" + name
					+ "'");
		} else {
			return processes.get(name);
		}
	}

	@Override
	public InternationalString getDescription(Name name) {
		GrassProcess process = getProcess(name);
		return new SimpleInternationalString(process.getDescription());
	}

	@Override
	public Set<Name> getNames() {
		return processes.keySet();
	}

	@Override
	public Map<String, Parameter<?>> getParameterInfo(Name name) {
		GrassProcess process = getProcess(name);
		return process.getParameterInfo();
	}

	@Override
	public Map<String, Parameter<?>> getResultInfo(Name name,
			Map<String, Object> arg1) throws IllegalArgumentException {
		GrassProcess process = getProcess(name);
		return process.getResultInfo();
	}

	@Override
	public InternationalString getTitle() {
		return new SimpleInternationalString("GRASS factory");
	}

	@Override
	public InternationalString getTitle(Name name) {
		GrassProcess process = getProcess(name);
		return new SimpleInternationalString(process.getName());
	}

	@Override
	public String getVersion(Name name) {
		return GRASS_VERSION;
	}

	@Override
	public boolean supportsProgress(Name name) {
		return false;
	}

}
