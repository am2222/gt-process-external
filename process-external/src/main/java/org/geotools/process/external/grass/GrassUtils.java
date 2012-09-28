package org.geotools.process.external.grass;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geotools.process.ProcessException;
import org.geotools.process.external.Utils;
import org.opengis.util.ProgressListener;

public class GrassUtils {

	private static String grassScriptFilename;
	private static String grassBatchJobFilename;
	private static String gisrcFilename;

	/**
	 * Creates a temporary location and mapset(s) for GRASS data processing. A
	 * minimal set of folders and files is created in the system's default
	 * temporary directory. The settings files are written with sane defaults,
	 * so GRASS can do its work. File structure and content will vary slightly
	 * depending on whether the user wants to process lat/lon or x/y data.
	 * 
	 * @throws IOException
	 * @returns the path to the created temporary location
	 * 
	 */
	public static String createMapset(boolean latlon) throws IOException {

		String parentTmpFolder = Utils.createTempFolder("gtgrassmapset");
		String tmpFolder = parentTmpFolder + File.separator + "temp_location";
		new File(tmpFolder).mkdir();
		new File(tmpFolder + File.separator + "PERMANENT").mkdir();
		new File(tmpFolder + File.separator + "user").mkdir();
		new File(tmpFolder + File.separator + "PERMANENT" + File.separator
				+ ".tmp").mkdir();
		writeGRASSWindow(tmpFolder + File.separator + "PERMANENT"
				+ File.separator + "DEFAULT_WIND", latlon);
		new File(tmpFolder + File.separator + "PERMANENT" + File.separator
				+ "MYNAME").createNewFile();
		try {
			final FileWriter fstream = new FileWriter(tmpFolder
					+ File.separator + "PERMANENT" + File.separator + "MYNAME");
			final BufferedWriter out = new BufferedWriter(fstream);
			if (latlon) {
				/* XY location */
				out.write("GeoTools GRASS interface: temporary x/y data processing location.\n");
			} else {
				/* lat/lon location */
				out.write("GeoTools GRASS interface: temporary lat/lon data processing location.\n");
			}
			out.close();
		} catch (final IOException e) {
			throw (e);
		}
		if (latlon) {
			new File(tmpFolder + File.separator + "PERMANENT" + File.separator
					+ "PROJ_INFO").createNewFile();
			try {
				final FileWriter fstream = new FileWriter(tmpFolder
						+ File.separator + "PERMANENT" + File.separator
						+ "PROJ_INFO");
				final BufferedWriter out = new BufferedWriter(fstream);
				out.write("name: Latitude-Longitude\n");
				out.write("proj: ll\n");
				out.write("ellps: wgs84\n");
				out.close();
			} catch (final IOException e) {
				throw e;
			}
			new File(tmpFolder + File.separator + "PERMANENT" + File.separator
					+ "PROJ_UNITS").createNewFile();
			try {
				final FileWriter fstream = new FileWriter(tmpFolder
						+ File.separator + "PERMANENT" + File.separator
						+ "PROJ_UNITS");
				final BufferedWriter out = new BufferedWriter(fstream);
				out.write("unit: degree\n");
				out.write("units: degrees\n");
				out.write("meters: 1.0\n");
				out.close();
			} catch (final IOException e) {
				throw e;
			}
		}
		writeGRASSWindow(tmpFolder + File.separator + "PERMANENT"
				+ File.separator + "WIND", latlon);
		new File(tmpFolder + File.separator + "user" + File.separator + "dbf")
				.mkdir();
		new File(tmpFolder + File.separator + "user" + File.separator + ".tmp")
				.mkdir();
		new File(tmpFolder + File.separator + "user" + File.separator + "VAR")
				.createNewFile();
		try {
			final FileWriter fstream = new FileWriter(tmpFolder
					+ File.separator + "user" + File.separator + "VAR");
			final BufferedWriter out = new BufferedWriter(fstream);
			out.write("DB_DRIVER: dbf\n");
			out.write("DB_DATABASE: $GISDBASE/$LOCATION_NAME/$MAPSET/dbf/\n");
			out.close();
		} catch (final IOException e) {
			throw (e);
		}
		writeGRASSWindow(tmpFolder + File.separator + "user" + File.separator
				+ "WIND", latlon);

		return parentTmpFolder;

	}

	private static void writeGRASSWindow(final String filename, boolean latlon)
			throws IOException {

		new File(filename).createNewFile();
		try {
			final FileWriter fstream = new FileWriter(filename);
			final BufferedWriter out = new BufferedWriter(fstream);
			if (!latlon) {
				/* XY location */
				out.write("proj:       0\n");
				out.write("zone:       0\n");
				out.write("north:      1\n");
				out.write("south:      0\n");
				out.write("east:       1\n");
				out.write("west:       0\n");
				out.write("cols:       1\n");
				out.write("rows:       1\n");
				out.write("e-w resol:  1\n");
				out.write("n-s resol:  1\n");
				out.write("top:        1\n");
				out.write("bottom:     0\n");
				out.write("cols3:      1\n");
				out.write("rows3:      1\n");
				out.write("depths:     1\n");
				out.write("e-w resol3: 1\n");
				out.write("n-s resol3: 1\n");
				out.write("t-b resol:  1\n");
			} else {
				/* lat/lon location */
				out.write("proj:       3\n");
				out.write("zone:       0\n");
				out.write("north:      1N\n");
				out.write("south:      0\n");
				out.write("east:       1E\n");
				out.write("west:       0\n");
				out.write("cols:       1\n");
				out.write("rows:       1\n");
				out.write("e-w resol:  1\n");
				out.write("n-s resol:  1\n");
				out.write("top:        1\n");
				out.write("bottom:     0\n");
				out.write("cols3:      1\n");
				out.write("rows3:      1\n");
				out.write("depths:     1\n");
				out.write("e-w resol3: 1\n");
				out.write("n-s resol3: 1\n");
				out.write("t-b resol:  1\n");
			}
			out.close();
		} catch (final IOException e) {
			throw e;
		}
	}

	public static String grassPath() {
		return System.getenv("GRASS_BIN_PATH");
	}

	/**
	 * Runs a set of GRASS commands
	 * 
	 * @param commands
	 *            a list with GRASS commands
	 * @param gisdbase
	 *            the GRASS gisdbase folder to base this execution on. It should
	 *            already exist
	 * @param progress
	 *            a ProgressListener to track progress (currently not used)
	 */
	public static void executeGrass(ArrayList<String> commands,
			String gisdbase, ProgressListener progress) {
		String command = null;
		final List<String> list = new ArrayList<String>();
		ProcessBuilder pb = new ProcessBuilder(list);
		final Map env = pb.environment();
		if (Utils.isWindows()) {
			createGrassScript(commands, gisdbase);
			list.add("cmd.exe");
			list.add("/C");
			list.add(grassScriptFilename());
		} else {
			String gisrc = Utils.getTempFilename("geotools", "gisrc");
			env.put("GISRC", gisrc);
			env.put("GRASS_MESSAGE_FORMAT", "gui");
			env.put("GRASS_BATCH_JOB", grassBatchJobFilename());
			createGrassBatchJobFileFromGrassCommands(commands);
			list.add("grass64");
			list.add(gisdbase);
			list.add("/user");
		}

		try {
			new File(grassBatchJobFilename()).setExecutable(true);
			final Process process = pb.start();
			final StreamGobbler errorGobbler = new StreamGobbler(
					process.getErrorStream());
			final StreamGobbler outputGobbler = new StreamGobbler(
					process.getInputStream());
			errorGobbler.start();
			outputGobbler.start();
			process.waitFor();
		} catch (IOException e) {
			throw new ProcessException("Error Executing GRASS:\n"
					+ e.getMessage());
		} catch (InterruptedException e) {
			throw new ProcessException("Error Executing GRASS:\n"
					+ e.getMessage());
		}
	}

	private static String grassBatchJobFilename() {
		if (grassBatchJobFilename == null) {
			grassBatchJobFilename = Utils.getTempFilename("grass_batchjob",
					"bat");
		}
		return grassBatchJobFilename;
	}

	private static String grassScriptFilename() {
		if (grassScriptFilename == null) {
			grassScriptFilename = Utils.getTempFilename("grass_script", "bat");
		}
		return grassScriptFilename;
	}

	private static String getGisrcFilename() {
		if (gisrcFilename == null) {
			gisrcFilename = Utils.getTempFilename("geotools", "gisrc");
		}
		return gisrcFilename;
	}

	private static void createGrassScript(ArrayList<String> commands,
			String gisdbase) {
		String folder = grassPath();

		String script = grassScriptFilename();
		String gisrc = getGisrcFilename();

		try {
			new File(script).createNewFile();
			FileWriter fstream = new FileWriter(gisrc);
			BufferedWriter output = new BufferedWriter(fstream);
			String location = "temp_location";
			String mapset = "user";
			output.write("GISDBASE: " + gisdbase + "\n");
			output.write("LOCATION_NAME: " + location + "\n");
			output.write("MAPSET: " + mapset + "\n");
			output.write("GRASS_GUI: text\n");
			output.close();

			fstream = new FileWriter(script);
			output = new BufferedWriter(fstream);

			output.write("set HOME=" + System.getProperty("user.home") + "\n");
			output.write("set GISRC=" + gisrc + "\n");
			output.write("set WINGISRC=" + gisrc + "\n");
			output.write("set WINGISBASE=" + folder + "\n");
			output.write("set GISBASE=" + folder + "\n");
			output.write("set GRASS_SH=%GISBASE%\\msys\\bin\\sh.exe\n");
			output.write("set PATH=%GISBASE%\\msys\\bin;%PATH%\n");
			output.write("set PATH=%GISBASE%\\extrabin;%GISBASE%\\extralib;%PATH%\n");
			output.write("set PATH=%GISBASE%\\tcl-tk\\bin;%GISBASE%\\sqlite\\bin;%GISBASE%\\gpsbabel;%PATH%\n");
			output.write("set PATH=%GISBASE%\\bin;%GISBASE%\\scripts;%PATH%\n");
			output.write("set GRASS_PROJSHARE=%GISBASE%\\proj\n");
			output.write("set GRASS_MESSAGE_FORMAT=gui\n");
			output.write("if \"%GRASS_ADDON_PATH%\"==\"\" set PATH=%WINGISBASE%\\bin;%WINGISBASE%\\lib;%PATH%\n");
			output.write("if not \"%GRASS_ADDON_PATH%\"==\"\" set PATH=%WINGISBASE%\\bin;%WINGISBASE%\\lib;%GRASS_ADDON_PATH%;%PATH%\n");
			output.write("\n");
			output.write("set GRASS_VERSION=" + getGrassVersion() + "\n");
			output.write("if not \"%LANG%\"==\"\" goto langset\n");
			output.write("FOR /F \"usebackq delims==\" %%i IN (`\"%WINGISBASE%\\etc\\winlocale\"`) DO @set LANG=%%i\n");
			output.write(":langset\n");
			output.write("\n");
			output.write("set PATHEXT=%PATHEXT%;.PY\n");
			output.write("set GRASS_HTML_BROWSER=explorer\n");
			output.write("set GDAL_DATA=%GISBASE%\\share\\gdal\n");
			output.write("set PROJ_LIB=%GISBASE%\\proj\n");
			output.write("set GEOTIFF_CSV=%GISBASE%\\share\\epsg_csv\n");
			output.write("set PYTHONHOME=%GISBASE%\\Python27\n");
			output.write("if \"x%GRASS_PYTHON%\" == \"x\" set GRASS_PYTHON=python\n");
			output.write("set PYTHONPATH=%PYTHONPATH%;%WINGISBASE%\\etc\\python;%WINGISBASE%\\etc\\wxpython\\n");
			output.write("\n");
			output.write("g.gisenv.exe set=\"MAPSET=" + mapset + "\"\n");
			output.write("g.gisenv.exe set=\"LOCATION=" + location + "\"\n");
			output.write("g.gisenv.exe set=\"LOCATION_NAME=" + location
					+ "\"\n");
			output.write("g.gisenv.exe set=\"GISDBASE=" + gisdbase + "\"\n");
			output.write("g.gisenv.exe set=\"GRASS_GUI=text\"\n");

			for (String command : commands) {
				output.write(command + "\n");
			}
			output.write("\n");
			output.write("exit\n");
			output.close();
		} catch (Exception e) {

		}
	}

	private static String getGrassVersion() {
		return "6.4.2";
	}

	private static void createGrassBatchJobFileFromGrassCommands(
			List<String> commands) {
		String filename = grassBatchJobFilename();
		try {
			new File(filename).createNewFile();
			final FileWriter fstream = new FileWriter(filename);
			final BufferedWriter fout = new BufferedWriter(fstream);
			for (String command : commands) {
				fout.write(command + "\n");
				fout.write("exit");
				fout.close();
			}
		} catch (Exception e) {
			throw new ProcessException(e.getMessage());
			// TODO****
		}
	}

	public static void deleteMapset(String mapset) {
		// TODO Auto-generated method stub

	}

}

class StreamGobbler extends Thread {

	InputStream is;
	String type;

	StreamGobbler(final InputStream is) {

		this.is = is;

	}

	@Override
	public void run() {
		try {
			final InputStreamReader isr = new InputStreamReader(is);
			final BufferedReader br = new BufferedReader(isr);
			String line = null;
			while ((line = br.readLine()) != null) {
				// try {
				// GrassUtils.filterGRASSOutput(String.copyValueOf(line.toCharArray()).trim());
				System.out.println(line);
				// }
				// catch (final GrassExecutionException e) {}
			}
		} catch (final IOException ioe) {
		}
	}
}
