Consuming external geoprocesses from GeoTools. The ``process-external`` module
********************************************************************************

The ``process-external`` module wraps several external applications with geoprocessing capabilities, so they can be used to process GeoTools-based objects such as FeatureCollection or GridCoverage2D. External geoprocesses are wrapped as GeoTools processes and available through the corresponding factories, one for each application. Processes are called in much the same way as native ones. The factory is responsible of handling the necessary data conversions or other operations needed to ensure communication between the application and GeoTools.

Below is a description of the external applications currently supported, how to properly install them so GeoTools can make use of them, and how to call their processes.

GRASS
=======

Installation
-------------

GRASS modules are available as GeoScript processes unider both Windows and Linux. The way GeoTools calls GRASS is, however, different depending on the Operating System, and a different configuration is needed.

-If you are running Linux, just install GRASS the usual way, as explained at http://grass.osgeo.org/wiki/Installation_Guide

-Make sure that GRASS is installed by running ``grass`` in a console. You are ready to go, as no further configuration is needed. 


-If you are running Windows, install a native WinGRASS package from http://grass.osgeo.org/grass64/binary/mswindows/native/

-Add an environment variable named ``GRASS_BIN_PATH`` and set it to the folder where GRASS is installed

GRASS funcionality has been tested with GRASS 6.5.2, and that is the recommended version to use.

Calling GRASS modules
----------------------

GRASS-based processes are obtained from the ``GrassProcessFactory``. The name of the process is created with the ``grass`` namespace and the name of the module to execute, as it would be used from the GRASS command-line interface. For example, to get an instance of the ``r.fillgaps`` module ready to run, the following code can be used:

::

	GrassProcessFactory fact = new GrassProcessFactory();
	NameImpl name = new NameImpl("grass", "r.fillnulls");
	Process proc = fact.create(name);

The execute module, just like with any other GeoTools geoprocess, takes a ``Map<String, Object>``, having parameter names as keys and parameter values as map values. In the case of a process wrappping a GRASS command, the names of the parameter match their names when calling thar grass command from a command-line interface. In other words, it matches the parameter names that can be found in the corresponding GRASS help files or are produced by the ``interface-description`` modifier when describing the inputs needed by the module.

In the above case of the ``r.fillnulls`` module, the following is the parameter description from the GRASS documentation:

::

	Flags:

		--overwrite
			Allow output files to overwrite existing files
		--verbose
			Verbose module output
		--quiet
			Quiet module output
	
	Parameters:

		input=string
			Name of input raster map in which to fill nulls
		output=string
			Name for output raster map with nulls filled by interpolation
		tension=float
			Spline tension parameter
			Default: 40.
		smooth=float
			Spline smoothing parameter
			Default: 0.1
		method=string
			Interpolation method
			Options: bilinear,bicubic,rst
			Default: rst

Below you can find a valid code example to call that process from GeoTools, by running the ``execute`` method of the process instance that we obtained in the previous block of code.

::

	GridCoverage2D gc;
	//load coverage into gc variable here
	.
	.
	.
	HashMap<String, Object> map = new HashMap<String, Object>();
	map.put("input", gc);
	map.put("smooth", 0.1);
	map.put("tension", 40.0);
	Map<String, Object> result = proc.execute(map, null);



Raster layers are passed as ``GridCoverage2D`` objects, while vector layers are passed as ``FeatureCollection`` ones.

For the remaining types of input, the mapping from string literals used in the GRASS console to Java objects is rather straightforward. In the case of a parameter that accepts values from a list (like a method to use, to be selected from a set of available ones), check the GRASS module documentation and pass the selected option as a ``String`` object, with the same value that would be used for a command-line call.

Flags are also considered as parameters, and are set using a ``Boolean`` value. The double hyphen preceding the flag name should not be added to the name of the parameter.

Along with the parameters that correspond to the GRASS module itself, there are always three additional ones that are used to configure properties that in a normal GRASS session would the configured otherwise, but that in this case, and since we are executing GRASS from *outside* of it, are configured as just extra parameters. These additional parameters are the following ones:

- ``regionextent``: The extent of the GRASS region where the analysis will be performed. A ``ReferencedEnvelope`` should be used as value.
- ``regioncellsize``: The cellsize of the GRASS region where the analysis will be performed. A ``Double`` should be used as value.
- ``latlon``. A ``Boolean`` indicating whether the computation involves Lat/Lon layers or projected ones.

For users unfamiliar with the concept of *region* in GRASS, reading the following link is recommended: http://grass.fbk.eu/gdp/grass5tutor/HTML_en/c515.html.

As you can see from the example shown before, all these three extra parameters are optional. The region cellsize has a default value of 1 (care should be taking when accepting this default value, as it can be too small in many cases, resulting in huge raster layers), while the ``latlon`` parameter is false by default.

There is no default value for the region extent, but if the process takes some layer as input, it will be taken from the set of input layer in case is not explicitly set. PArticularly, the minimum extent needed to cover all input layers will be used. Only when there are no input layers and the region extent cannot be inferred, the ``regionextent`` parameter is mandatory. In that case, executing the process without explicitly setting its valus will result in an exception being thrown.

In case there are input raster layers and a region cellsize is not provided, it will also be inferred from those layers. The minimum cellsize of all input raster layers will be used.

Most parameters except layers are optional, like string values or numerical ones, since there is a default value to use. In the case of a parameter to select from a list of possible ones, the first option is used in case a value for that parameter is not provided.

Parameters reprenting outputs do not have to be set. Outputs stored in temporary files, and the GeoTools-GRASS interface will take care of deleting them when necessary. As it is explained next, for a single output file, several intermediate files will be generated as well, but you do not have to worry about that.

Internal mechanism of the GeoTools-GRASS interface
---------------------------------------------------------

Here is some more technical and detailed information about how the GRASS interface works.

Executing a GRASS-based process in GeoTools involves the following steps.

- Writting the corresponding GeoTools object(s) to file(s), in a GDAL/OGR compatible format that can be read by GRASS.
- Creating a temporary GRASS mapset.
- Import the files representing the GeoTools data objects into the GRASS mapset.
- Perform the corresponding analysis.
- Export the results to a format readable by GeoTools
- Open the results and create the corresponding GeoTools objects.

Parts of this workflow can be skipped and optimized. Some of this optimization is done automatically by the processing factory, while some can be done manually. Particularly, if the GeoTools object data source is of a format that can be read by GRASS, the exporting part is ommitted and the source directly accessed.


SAGA
=====

Installation
-------------

SAGA algorithms are called by GeoTools using its command line version ``saga_cmd``. To install SAGA, follow the next steps.

- If you are running Windows, download SAGA from http://saga-gis.org
- Unzip the content of the downloaded file to a folder you select (let's say ``c:\saga``)
- Add that folder to the PATH environment variable 
- To check that everything is OK, open a console (Windows key + R, then type ``cmd`` and press Enter) and type ``saga_cmd``. You should see something like this.

:: 

	_____________________________________________
	  #####   ##   #####    ##
	 ###     ###  ##       ###
	  ###   # ## ##  #### # ##
	   ### ##### ##    # #####
	##### #   ##  ##### #   ##
	_____________________________________________


	error: module library

	available module libraries:
	- contrib_a_perego.dll
	- docs_html.dll
	- docs_pdf.dll
	- garden_3d_viewer.dll
	- garden_webservices.dll
	- geostatistics_grid.dll
	- geostatistics_kriging.dll
	- geostatistics_points.dll
	.
	.
	.


-If you are running Linux, packages are available from https://launchpad.net/~johanvdw/+archive/saga-gis
-After installing, just make sure that the command line version of SAGA is available, by running ``saga_cmd`` from a console.

In all cases, SAGA 2.0.8 is recommended, as it is the only version tested and supported for running from GeoTools.

Calling SAGA geoalgorithms
----------------------------

Like GRASS algorithms, SAGA algorithm are obtained from the corresponding factory (``SagaProcessFactory``), and executed using the ``execute`` method with a map of parameter names and values.

The process has ``saga`` as its namespace, and the name of the process is obtained by removing all character other than letters from the SAGA geoalgorithm name and putting it in lower case. 

Below you can see a listing of the 5 first algorithms in the ``ta_morphometry`` library.

::

	$saga_cmd ta_morphometry
	 0      - Slope, Aspect, Curvature
	 1      - Convergence Index
	 2      - Convergence Index (Search Radius)
	 3      - Surface Specific Points
	 4      - Curvature Classification
	 5      - Hypsometry

To get the corresponding processes from the SAGA factory class, you would use the following process names:

::

	slopeaspectcurvature
	convergenceindex
	convergenceindexsearchradius
	surfacespecificpoints
	curvatureclassification
	hypsometry

For instance, to get the process that computes the convergence index, the following code should be used:

::
	
	fac = new SagaFactory();
	proc = fact.create(new NameImpl("saga", "convergenceindex"));

Calling the process is also similar to the GRASS case in terms of parameters needed an their names. We will take the Convergence Index geoalgorithm, to see an example. Here is a valid call for that algorithm, using the command-line version of SAGA:

::

	$ saga_cmd ta_morphometry 1 -ELEVATION "dem.tif" -METHOD 0 -NEIGHBOURS 0 -RESULT "ci.tif"

And here is the corresponding GeoTools process call:

::

	SagaProcessFactory fact = new SagaProcessFactory();
	NameImpl name = new NameImpl("saga", "convergenceindex");
	Process proc = fact.create(name);	
	HashMap<String, Object> map = new HashMap<String, Object>();
	map.put("elevation", gc);
	map.put("method", new Integer(0));
	map.put("neighbours", new Integer(0));
	Map<String, Object> result = proc.execute(map, null);

``gc`` being the ``GridCoverage2D`` object containing the DEM to use as input.

Keys used for the parameter map match the names of the parameters, except for the case of boolean ones, which contain a hyphen that should be removed.

Another exception is found in processes requiring an extent (like, for instance, most interpolation ones). While SAGA solves this by asking the user 4 parameters (usually in the form of ``xmin, xmax, ymin`` and ``ymax`` parameters, though names vary across geoalgorithms), the corresponding GeoTools processes substitute the set of 4 parameters with a single parameters named ``extent``, which takes a  ``ReferencedEnvelope`` object. Here is an example to help understanding this mechanism. Below you can see the command line SAGA call for the Inverse Distance Weighting algorithm

::

	$ 

To execute the corresponding GeoTools process, the following block of code would be needed.


::


Notice that parameters that can take a value from a list of predefined ones are set using the zero-based index of the option to use, not its name or a text input, as it happened with GRASS.

As in the case of GRASS processes, most parameters can be ommited, as there are default values that can be used. The above code could be susbsituted by the following, more compact one:

::

	SagaProcessFactory fact = new SagaProcessFactory();
	NameImpl name = new NameImpl("saga", "convergenceindex");
	Process proc = fact.create(name);	
	HashMap<String, Object> map = new HashMap<String, Object>();
	map.put("elevation", gc);
	Map<String, Object> result = proc.execute(map, null);

Once again, as it happened with GRASS algorithms, outputs do not need to be defined.

Optimizing process workflows
-----------------------------

Calling external applications from GeoTools involves most of the times writing temporary intermediate files. If you are going to execute several processes together in a processing workflow, it is a good idea to try to minimize the number of intermediate files written to disk, as this is a time-consuming task. There are two ways of optimizing file-handling:

1) Reusing files written by GeoTools. If your data is not file-based, GeoTools will write it to a file so the external application can read it and process it. If several processes use the same GeoTools object as input, it should be written just once for the gloabl process instead of once for each process.

2) Reusing imported files. Some external applications need their files imported before processign them. For instance, GRASS needs data to be imported into a mapset, and SAGA can handle raster files only in its native ``sgrd`` format. They include processes to do that importing from other formats (the ones that GeoTools can write), but it involves an additional step in the process, so imported files should be reused when possible.

To optimize the two issues above, the ``process-external`` module has classes that should be used when writing a process workflow involving several processes. The fundamental idea behind them is to make processes aware of other similar processes that might need to use the same datafiles.

The main class is the ``ProcessGroup`` one, which deals with the first issue, that of reussing files written by GeoTools. This should be used independently of the external application being used, an even if the workflow involves calling processes based on several external applications.

Here is an example on how to use it to run two SAGA algorithms, namely Convergence Index and Terrain Rugedness Index. Both of them use the same DEM as input.

::


	ProcessGroup pg = new ProcessGroup();

	NameImpl name = new NameImpl("saga", "convergenceindex");
	ExternalProcess proc = fact.create(name);
	HashMap<String, Object> map = new HashMap<String, Object>();
	map.put("elevation", gc);
	map.put("method", new Integer(0));
	map.put("neighbours", new Integer(0));
	pg.addProcess(proc);

	NameImpl name2 = new NameImpl("saga", "terrainruggednessindextri");
	ExternalProcess proc2 = fact.create(name2);
	HashMap<String, Object> map2 = new HashMap<String, Object>();
	map2.put("dem", gc);
	pg.addProcess(proc2);

	Map<String, Object> result = proc.execute(map, null);
	Map<String, Object> result2 = proc2.execute(map2, null);

	pg.finish();


As you can see, the only thing to do it is to create a ``ProcessGroup`` object representing the set of related processes to run and then add those processes to it. When all processes are executed, call the ``finish()`` method to clean up. Intermediate layers are not cleaned up by each process in this case.

This code, however, does not optimize the usage of imported layers, and will convert layers to the native SAGA format more than what is strictly needed. To handle that, you have to use also a class that optimizes file-handling for a particular external application. In the case of SAGA, the ``SAGAGroupProcess`` is available for this tasks.

This second class is used in the same way. A single process can be added to several classes derived from the ``GroupProcess`` class. Each class will take care of optimizing a given aspect, as described above.

The above code can be improved, using a ``SAGAGroupProcess`` as shown next.

::

	ProcessGroup pg = new ProcessGroup();
	SAGAProcessGroup spg = new SAGAProcessGroup();

	NameImpl name = new NameImpl("saga", "convergenceindex");
	ExternalProcess proc = fact.create(name);
	HashMap<String, Object> map = new HashMap<String, Object>();
	map.put("elevation", gc);
	map.put("method", new Integer(0));
	map.put("neighbours", new Integer(0));
	pg.addProcess(proc);
	spg.addProcess(proc);

	NameImpl name2 = new NameImpl("saga", "terrainruggednessindextri");
	ExternalProcess proc2 = fact.create(name2);
	HashMap<String, Object> map2 = new HashMap<String, Object>();
	map2.put("dem", gc);
	pg.addProcess(proc2);
	spg.addProcess(proc2);

	Map<String, Object> result = proc.execute(map, null);
	Map<String, Object> result2 = proc2.execute(map2, null);

	pg.finish();
	spg.finish();

This will not only take care of not unnecesarilly repeating imports, but also will handle the case in which the output of a process is used as an input for a next one, minimizing as well the exporting/importing tasks involved in that case.

[Maybe I should add an example with several apps...mixing GRASS and SAGA]