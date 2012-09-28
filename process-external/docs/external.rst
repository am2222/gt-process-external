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

XXXXXX

Below you can find a valid code example to call that process from GeoTools, by running the ``execute`` method of the process instance that we obtained in the previous block of code.

::

	XXXXX


Raster layers are passed as ``GridCoverage2D`` objects, while vector ones are passed as ``FeatureCollection`` ones.

For the remaining types of input, the mapping from Java objects to string literals used in the GRASS cosole is rather straightforward. In the case of a parameter that accepts values from a list (like a method to use, to select from a set of available ones), check the GRASS module documentation and pass the selected option as a ``String`` object, with the same value that would be used for a command-line call.

Along with the parameters that correspond to the GRASS module itself, there are always three additional ones that are used to configure properties that in a normal GRASS session would the configured otherwise, but that in this case, and since we are executing GRASS from *outside* of it, are configured as just extra parameters. These additional parameters are the following ones:

- ``regionextent``: The extent of the GRASS region where the analysis will be performed. A ``ReferencedEnvelope`` should be used as value.
- ``regioncellsize``: The cellsize of the GRASS region where the analysis will be performed. A ``Double`` should be used as value.
- ``latlon``. A ``Boolean`` indicating whether the computation involves Lat/Lon layers or projected ones.

For users unfamiliar with the concept of *region* in GRASS, reading the following link is recommended: http://grass.fbk.eu/gdp/grass5tutor/HTML_en/c515.html.

As you can see from the example shown before, all these three extra parameters are optional. The region cellsize has a default value of 1 (care should be taking when accepting this default value, as it can be too small in many cases, resulting in huge raster layers), while the ``latlon`` parameter is false by default.

There is no default value for the region extent, but if the process takes some layer as input, it will be taken from the set of input layer in case is not explicitly set. PArticularly, the minimum extent needed to cover all input layers will be used. Only when there are no input layers and the region extent cannot be inferred, the ``regionextent`` parameter is mandatory. In that case, executing the process without explicitly setting its valus will result in an exception being thrown.

In case there are input raster layers and a region cellsize is not provided, it will also be inferred from those layers. The minimum cellsize of all input raster layers will be used.


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

[Mapset mapping]

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

The name of the parameter has ``saga`` as its namespace, and the name of the process is obtained by removing all character other than letters from the SAGA geoalgorithm name. 

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

*****


Calling the process is also similar to the GRASS case in terms of parameters needed an their names. We will take the Convergence Index geoalgorithm, to see an example. Here is a valid call for that algorithm, from the command-line:

XXXXXXXXXXXx

And here is the corresponding GeoTools process call:

XXXXXXXXXXX

Keys using for the parameter maps match the names of the parameter, except for the case of boolean ones, which contain a hyphen that should be removed.