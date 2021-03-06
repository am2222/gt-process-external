package org.geotools.process.external.saga;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.geotools.process.ProcessException;
import org.geotools.process.external.Utils;
import org.opengis.util.ProgressListener;

public class SagaUtils {

	public static String getBatchJobFilename() {		
		String ext = "sh";
		if (Utils.isWindows()) {
			ext = "bat";
		}
		String sagaBatchJobFilename = Utils.getTempFilename("saga_batchjob", ext);
		return sagaBatchJobFilename;
	}

	public static void createSagaBatchJobFileFromSagaCommands(
			final String[] commands, String batchJobFilename) {

		try {
			final BufferedWriter output = new BufferedWriter(new FileWriter(
					batchJobFilename));
			for (int i = 0; i < commands.length; i++) {
				output.write("saga_cmd " + commands[i] + "\n");
			}
			output.write("exit");
			output.close();
		} catch (final IOException e) {
			throw new ProcessException("Error creating SAGA batch file:\n"
					+ e.getMessage());
		}

	}

	public static int executeSaga(String[] commands, ProgressListener progress) {

		String batchJobFilename = getBatchJobFilename();
		createSagaBatchJobFileFromSagaCommands(commands, batchJobFilename);
		final List<String> list = new ArrayList<String>();
		ProcessBuilder pb;
		pb = new ProcessBuilder(list);
		if (!Utils.isWindows()) {
			new File(batchJobFilename).setExecutable(true);
			list.add(batchJobFilename);
		} else {
			list.add("cmd.exe");
			list.add("/C");
			list.add(batchJobFilename);
		}

		Process process;
		try {
			process = pb.start();
			final StreamGobbler errorGobbler = new StreamGobbler(
					process.getErrorStream(), progress);
			final StreamGobbler outputGobbler = new StreamGobbler(
					process.getInputStream(), progress);
			errorGobbler.start();
			outputGobbler.start();
			final int iReturn = process.waitFor();
			new File(batchJobFilename).delete();
			return iReturn;
		} catch (final Exception e) {
			throw new ProcessException("Error executing SAGA:\n"
					+ e.getMessage());
		}

	}


}

class StreamGobbler extends Thread {

	InputStream is;
	String type;
	ProgressListener progress;

	StreamGobbler(final InputStream is, ProgressListener progress) {

		this.is = is;
		this.progress = progress;

	}

	@Override
	public void run() {
		try {
			final InputStreamReader isr = new InputStreamReader(is);
			final BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null) {
				if (line.contains("%")) {
					try {
						int percentage = Integer
								.parseInt(line.replace("%", ""));
						if (progress != null) {
							progress.progress(percentage / 100f);
						}
					} catch (NumberFormatException e) {
						// we ignore this
					}
				}
				if (line.length() > 5){
					//System.out.println(line);
				}
			}
		} catch (final IOException ioe) {

		}
	}
}
