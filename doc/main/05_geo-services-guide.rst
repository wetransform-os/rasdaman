.. highlight:: text

.. _sec_geo-services-guide:

##################
Geo Services Guide
##################

This guide covers the details of geo data management and retrieval in rasdaman.
The rasdaman Array DBMS is domain agnostic; the specific semantics of space and time
is provided through a layer on top of rasdaman, historically known as *petascope*.
It offers spatio-temporal access and analytics through APIs based on the OGC data standard
*Coverage Implementation Schema* (CIS) and the OGC service standards
*Web Map Service* (WMS), *Web Coverage Service* (WCS), and *Web Coverage Processing Service* (WCPS).

.. NOTE::
   While the name petascope addresses a specific component we frequently use the name *rasdaman*
   to refer to the complete system, including petascope.

OGC Coverage Standards Overview
===============================

For operating rasdaman geo services as well as for accessing such geo services
through these APIs it is important to understand the mechanics of the relevant
standards. In particular, the concept of OGC / ISO *coverages* is important.

In standardization, coverages are used to represent space/time varying
phenomena, concretely: regular and irregular grids, point clouds, and general
meshes. The coverage standards offer data and service models for dealing with
those. In rasdaman, focus is on multi-dimensional gridded ("raster") coverages.

In rasdaman, the OGC standards WMS, WCS, and WCPS are supported, being reference
implementation for WCS. These APIs serve different purposes:

- WMS delivers a 2D map as a visual image, suitable for consunmption by humans
- WCS delivers n-D data, suitable for further processing and analysis
- WCPS performs flexible server-side processing, filtering, analytics, and fusion 
  on coverages.

These coverage data and service concepts are summarized briefly below; for
specific details on coordinate reference system handling see also :ref:`CRS
definition management <crs-def-management>`. Ample material is also available on
the Web for familiarization with coverages (best consult in this sequence):

- `hands-on demos <https://standards.rasdaman.org>`__ for multi-dimensional 
  coverage services provided by
  `Jacobs University <https://www.jacobs-university.de/lsis>`__;
- a series of 
  `webinars and tutorial slides <https://www.earthserver.xyz/webinars>`__ 
  provided by `EarthServer <https://www.earthserver.xyz>`__;
- a `range of background information <http://myogc.org/go/coveragesDWG>`__ on 
  these standards provided by `OGC <http://www.opengeospatial.org>`__;
- the official standards documents maintained by 
  `OGC <http://www.opengeospatial.org>`__:

 * `WCS 2.0.1 <https://portal.opengeospatial.org/files/09-110r4>`__
 * `WCS-T 2.0 <http://docs.opengeospatial.org/is/13-057r1/13-057r1.html>`__
 * `WCPS 1.0 <https://portal.opengeospatial.org/files/08-059r4>`__
 * `WMS 1.3.0 <http://portal.opengeospatial.org/files/?artifact_id=4756&passcode=4hy072w9zerhjyfbqfhq>`__


Coverage Data
-------------

OGC CIS specifies an interoperable, conformance-testable coverage structure
independent from any particular format encoding.
Encodings are defined in OGC in GML, JSON, RDF,
as well as a series of binary formats including GeoTIFF, netCDF, JPEG2000,
and GRIB2).

By separating the data definition (CIS) from the service definition (WCS)
it is possible for coverages to be served throuigh a variety of APIs, such
as WMS, WPS, and SOS.
However, WCS and WCPS have coverage-specific functionality
making them particularly suitable for flexible coverage acess, analytics,
and fusion.

Coverage Services
-----------------

OGC WMS delivers 2D maps generated from styled layers stacked up.
As such, WMS is a visualization service sitting at the end of processing
pipelines, geared towards human consumption.

OGC WCS, on the other hand, rpovides data suitable for further processing
(including visualization); as such, it is suitable also for machine-to-machine
communication as it appears in the middle of longer processing pipelines.
WCS is a modular suite of service functionality on coverages.
WCS Core defines download of coverages and parts thereof, through *subsetting*
directives, as well as delivery in some output format requested by the client.
A set of WCS Extensions adds further functionality facets.

One of those is WCS Processing; it defines the ``ProcessCoverages`` request
which allows sending a coverage analytics request through the WCPS
spatio-temporal analytics language. WCPS supports extraction, analytics, and
fusion of multi-dimensional coverage expressed in a high-level, declarative, and
safe language.


OGC Web Services Endpoint
=========================

Once the petascope servlet is deployed (see :ref:`rasdaman installation guide
<sec-system-install-packages>`) coverages can be accessed through service
endpoint ``/rasdaman/ows``.

.. NOTE::

   Endpoint ``/rasdaman/rasql``, which by default is also available after
   deploying rasdaman, does not know about coverages and their services, but
   only knows domain-agnostic rasql.

For example, assuming that the service's IP address is ``123.456.789.1`` and the
service port is ``8080``, the following request URLs would deliver the
Capabilities documents for OGC WMS and WCS, respectively:

.. code-block:: text

    http://123.456.789.1:8080/rasdaman/ows?SERVICE=WMS&REQUEST=GetCapabilities&VERSION=1.3.0
    http://123.456.789.1:8080/rasdaman/ows?SERVICE=WCS&REQUEST=GetCapabilities&VERSION=2.0.1


OGC Coverage Implementation Schema (CIS)
========================================

A coverage consists mainly of:

    - *domain set*: provides information about where data sit in space and time.
      All coordinates expressed there are relative to the coverage's
      Coordinate Reference System or *Native CRS*. Both CRS and its axes, units
      of measure, etc. are indiciated in the domain set.
      Petascope currently supports grid topologies whose axes are aligned with the
      axes of the CRS. Along these axes, grid lines can be spaced regularly
      or irregularly.
    - *range set*: the "pixel payload", ie: the values (which can be atomic,
      like in a DEM, or records of values, like in hyperspectral imagery).
    - *range type*: the semantics of the range values, given by type information,
      null values, accuracy, etc.
    - *metadata*: a black bag which can contain any data: the coverage will not
      understand these, but duly transport them along so that the connection
      between data and metadata is not lost.

Further components include ``Envelope`` which gives a rough, simplified overview
on the coverage's location in space and time and ``CoverageFunction`` which is
unused by any implementation known to us.


Coordinate Reference Systems in Coverages
-----------------------------------------

Every coverage, as per OGC CIS, must have exactly one Native CRS. Sometimes
definitions for such CRSs are readily available, such as with the EPSG registry
where 2-D WGS84 is readily available under its code EPSG:4326. In particular
spatio-temporal CRSs, however, are not always readily available, at least not
in all combinations of spatial and temporal axes. To this end, composition
of CRS is supported so that the single Native CRS can be built from
"ingredient" CRSs by concatenating these CRSs into a composite one.

For instance, a time-series of WGS84 images would have the following Native CRS:

.. code-block:: text

   http://localhost:8080/def/crs-compound?
         1=http://localhost:8080/def/crs/EPSG/0/4326
        &2=http://localhost:8080/def/crs/OGC/0/AnsiDate

Coordinate tuples in this CRS represent an ordered composition of a geospatial
CRS with Latitude followed by Longitude, as per EPSG:4326, followed by
a temporal coordinate expressed in ISO 8601 syntax,
such as: ``2012-01-01T00:01:20Z``.

Several ways exist for determining the Native CRS of coverage domain set:

    * in a WCS ``GetCapabilities`` response, check the ``wcs:CoverageSummary/ows:BoundingBox@crs`` attribute;
    * in a WCS ``DescribeCoverage`` response, check the ``@srsName`` attribute in the ``gml:domainSet``;
    * in WCPS, use function ``crsSet(e)`` to determine the CRS of a coverage expression *e*;

.. NOTE::

   In a coverage also consider the ``axisLabels`` attributes giving the axis
   names as used in the coverage, in proper sequence as per CRS; 
   the ``uomLabels`` attribute contains the units of measure for each axis.

The following graphics illustrates, on the example of an image timeseries,
how dimension, CRS, and axis labels affect the domain set in
a CIS 1.0 ``RectifiedGridCoverage``.

.. IMAGE:: media/geo-services-guide/GridDomainSetAxes.png
    :align: center
    :scale: 50%

.. NOTE::

   This handling of coordinates in CIS 1.0 bears some legacy burden from GML;
   in the ``GeneralGridCoverage`` introduced with CIS 1.1 coordinate handling is much simplified.


.. _crs-def-management:

CRS Management
--------------

the Native CRS of a coverage is given by a URL, as per OGC convention. Resolving
this URL should deliver the CRS definition. The `OGC CRS resolver
<http://external.opengeospatial.org/twiki_public/CRSdefinitionResolver>`__ is
one such service. Its implementation is running SECORE which is part of rasdaman
community.

By providing the source code of the OGC resolver it is possible to deploy
one's own resolver under an own URL, such as http://rasdaman.org:8080/def/crs/EPSG/0/27700.


Range Type
----------

Range values can be atomic or (possibly nested) records over atomic values,
described by the range type. In rasdaman the following atomic data types
are supported; all of these can be combined freely in records of values,
such as in hyperspectral images or climate variables.

.. TABLE:: Mapping of rasdaman base types to SWE Quantity types

    +--------------------+------------+------------------------------------------+
    | **rasdaman type**  | **size**   | **Quantity types**                       |
    +====================+============+==========================================+
    | ``boolean``        | 8 bit      | unsignedByte                             |
    +--------------------+------------+------------------------------------------+
    | ``octet``          | 8 bit      | signedByte                               |
    +--------------------+------------+------------------------------------------+
    | ``char``           | 8 bit      | unsignedByte                             |
    +--------------------+------------+------------------------------------------+
    | ``short``          | 16 bit     | signedShort                              |
    +--------------------+------------+------------------------------------------+
    | ``unsigned short`` | 16 bit     | unsignedShort                            |
    | = ``ushort``       |            |                                          |
    +--------------------+------------+------------------------------------------+
    | ``long``           | 32 bit     | signedInt                                |
    +--------------------+------------+------------------------------------------+
    | ``unsigned long``  | 32 bit     | unsignedInt                              |
    | = ``ulong``        |            |                                          |
    +--------------------+------------+------------------------------------------+
    | ``float``          | 32 bit     | float32                                  |
    +--------------------+------------+------------------------------------------+
    | ``double``         | 64 bit     | float64                                  |
    +--------------------+------------+------------------------------------------+
    | ``complex``        | 64 bit     | cfloat32                                 |
    +--------------------+------------+------------------------------------------+
    | ``complexd``       | 128 bit    | cfloat64                                 |
    +--------------------+------------+------------------------------------------+


Nil Values
----------
Nil (**"null"**) values, as per SWE, are supported by rasdaman in an extended way:

    - null values can be defined over any data type
    - nulls can be single values
    - nulls can be intervals
    - a null definnition in a coverage can be a list of all of the above alternatives.

.. NOTE::

   It is highly recommended to NOT define null values **over floating-point numbers**
   as this causes numerical problems well known in mathematics.
   This is not related to rasdaman, but intrinsic to the nature and handling of
   floating-point numbers in computers. If really desired, **a floating-point
   interval should be defined** around the desired float null value
   (this corresponds to interval arithmetics in numerical mathematics).


OGC Web Coverage Service
========================

WCS Core offers request types:

    - ``GetCapabilities`` for obtaining a list of coverages offered together
      with an overall service description;
    - ``DescribeCoverage`` for obtaining information about a coverage without 
      downloading it;
    - ``GetCoverage`` for downloading, extracting, and reformatting of coverages;
      this is the central workhorse of WCS.

WCS Extensions in part enhance ``GetCoverage`` with additional functionality
controlled by further parameters, and in part establish new request types,
such as:

    - WCS-T defining ``InsertCoverage``, ``DeleteCoverage``, and
      ``UpdateCoverage`` requests;
    - WCS Processing defining ``ProcessCoverages`` for submitting WCPS
      analytics code.

You can use ``http://localhost:8080/rasdaman/ows`` as service endpoints to which to
send WCS requests, for example:

.. code-block:: text

    http://localhost:8080/rasdaman/ows?service=WCS&version=2.0.1&request=GetCapabilities

See `example queries
<http://rasdaman.org/browser/systemtest/testcases_services/test_wcs/queries>`__
in the WCS systemtest which send KVP (key value pairs) GET request and XML POST
request to Petascope.

Subsetting behavior
-------------------

In general, subsetting in petascope behaves similarly to subsetting in gdal,
with a couple of deviations necessary for n-D. Specifically, subsetting
follows the next rules:

- Slicing (``geoPoint``): the grid slice with index corresponding to the 
  requested slicing geo point is returned. This is computed as follows:

  .. code-block:: text

     gridIndex = floor((geoPoint - minGeoLowerBound) / axisResolution)

- Trimming (``geoLowerBound``:``geoUpperBound``): the lower bound of the grid 
  interval is determined as in the case of slicing. The number of returned grid 
  points follows gdal:

  - If axis resolution is positive (e.g. ``Long`` axis):

    .. code-block::text

      gridLowerBound = floor((geoLowerBound - minGeoLowerBound) / axisResolution)
      numberOfGridPixels = floor(((geoUpperBound - geoLowerBound) / axisResolution) + 0.5)
      gridUpperBound = gridLowerBound + numberOfGridPixels - 1

  - If axis resolution is negative (e.g. ``Lat`` axis):

    .. code-block::text

      gridLowerBound = floor((geoUpperBound - maxGeoLowerBound) / axisResolution)
      numberOfGridPixels = floor((geoLowerBound - geoUpperBound) / axisResolution) + 0.5)
      gridUpperBound = gridLowerBound + numberOfGridPixels - 1

  .. NOTE::

     If a trimming subset is applied on an axis with 
     ``(geoUpperBound - geoLowerBound) / axisResolution < 0.5``, then lower grid 
     bound is translated by the slicing formula and upper grid bound is set to 
     lower grid bound.

For example, a 2D coverage has ``Long`` (X) and ``Lat`` (Y) axes with CRS
``EPSG:4326``. The resolution for axis ``Long`` is ``10`` and the resolution
for axis ``Lat`` is ``-10``. The geo bounds of axis ``Long`` are ``[0:180]``
and the geo bounds of axis ``Lat`` are ``[0:90]``.

- Calculate *slicing* on ``Long`` axis by geo coordinates to grid coordinates:

  .. code-block:: text

      - Long(0):          returns [0]
      - Long(9):          returns [0]
      - Long(10):         returns [1]
      - Long(15):         returns [1]
      - Long(20):         returns [2]
      - Long(40):         returns [4]
      - Long(49.99999):   returns [4]
      - Long(50.0):       returns [5]

- Calculate *trimming* on ``Long`` axis by geo coordinates to grid coordinates:

  .. code-block:: text
  
      - Long(0:5):         returns [0:0]
      - Long(0:10):        returns [0:0]
      - Long(0:14.999):    returns [0:0]
      - Long(0:15):        returns [0:1]
      - Long(0:24.999):    returns [0:1]
      - Long(0:25.0):      returns [0:2]
      - Long(9,11): returns [0:0]

CIS 1.0 to 1.1 Transformation
-----------------------------

Under WCS 2.1 - ie: with ``SERVICE=2.1.0`` - both ``DescribeCoverage``and
``GetCoverage`` requests understand the proprietary parameter
``OUTPUTTYPE=GeneralGridCoverage`` which formats the result as CIS 1.1
``GeneralGridCoverage`` even if it has been imported into the server as a CIS
1.0 coverage, for example:

.. code-block:: text

   http://localhost:8080/rasdaman/ows?SERVICE=WCS&VERSION=2.1.0
       &REQUEST=DescribeCoverage
       &COVERAGEID=test_mean_summer_airtemp
       &OUTPUTTYPE=GeneralGridCoverage

   http://localhost:8080/rasdaman/ows?SERVICE=WCS&VERSION=2.1.0
       &REQUEST=GetCoverage
       &COVERAGEID=test_mean_summer_airtemp
       &FORMAT=application/gml+xml
       &OUTPUTTYPE=GeneralGridCoverage

Polygon/Raster Clipping
-----------------------

WCS and WCPS support clipping of polygons expressed in the
`WKT format <https://en.wikipedia.org/wiki/Well-known_text>`__ format.
Polygons can be ``MultiPolygon (2D)``, ``Polygon (2D)`` and ``LineString (1D+)``.
The result is always a 2D coverage in case of MultiPolygon and Polygon, and
is a 1D coverage in case of ``LineString``.

Further clipping patterns include ``curtain`` and ``corridor`` on 3D+ coverages
from ``Polygon (2D)`` and ``Linestring (1D)``. The result of ``curtain``
clipping has the same dimensionality as the input coverage whereas the result of
``corridor`` clipping is always a 3D coverage, with the first axis being the
*trackline* of the corridor by convention.

Below some examples are presented expaining the mimics for WCS.

Syntactically, clipping is expressed by adding a ``&CLIP=`` parameter to the
request. If the ``SUBSETTINGCRS`` parameter is specified then this CRS also
applies to the clipping WKT, otherwise it is assumed that the WKT is in the
Native coverage CRS.

Clipping Examples
^^^^^^^^^^^^^^^^^

-  Polygon clipping on coverage with Native CRS ``EPSG:4326``, for example:

   .. hidden-code-block:: text

        http://localhost:8080/rasdaman/ows?SERVICE=WCS&VERSION=2.0.1&
          &REQUEST=GetCoverage
          &COVERAGEID=test_wms_4326
          &CLIP=POLYGON((55.8 -96.6, 15.0 -17.3))
          &FORMAT=image/png

-  Polygon clipping with coordinates in ``EPSG:3857`` (from ``subsettingCRS`` parameter)
   on coverage with Native CRS ``EPSG:4326``, for example:

   .. hidden-code-block:: text

        http://localhost:8080/rasdaman/ows?SERVICE=WCS&VERSION=2.0.1
          &REQUEST=GetCoverage
          &COVERAGEID=test_wms_4326
          &CLIP=POLYGON((13589894.568 -2015496.69612, 15086830.0246 -1780682.3822))
          &SUBSETTINGCRS=http://opengis.net/def/crs/EPSG/0/3857
          &FORMAT=image/png

-  Linestring clipping on a 3D coverage with axes ``X``, ``Y``, ``ansidate``,
   for example:

   .. hidden-code-block:: text

        http://localhost:8080/rasdaman/ows?SERVICE=WCS&VERSION=2.0.1
          &REQUEST=GetCoverage
          &COVERAGEID=test_irr_cube_2
          &CLIP=LineStringZ(75042.7273594 5094865.55794 "2008-01-01T02:01:20.000Z", 705042.727359 5454865.55794 "2008-01-08T00:02:58.000Z")
          &FORMAT=text/csv

-  Multipolygon clipping on 2D coverage, for example:

   .. hidden-code-block:: text

        http://localhost:8080/rasdaman/ows?SERVICE=WCS&VERSION=2.0.1
          &REQUEST=GetCoverage
          &COVERAGEID=test_mean_summer_airtemp
          &CLIP=Multipolygon( ((-23.189600 118.432617, -27.458321 117.421875,
                                -30.020354 126.562500, -24.295789 125.244141)),
                              ((-27.380304 137.768555, -30.967012 147.700195,
                                -25.491629 151.259766, -18.050561 142.075195)) )
          &FORMAT=image/png

-  Curtain clipping by a Linestring on 3D coverage, for example:

   .. hidden-code-block:: text

        http://localhost:8080/rasdaman/ows?SERVICE=WCSVERSION=2.0.1
          &REQUEST=GetCoverage
          &COVERAGEID=test_eobstest
          &CLIP=CURTAIN( projection(Lat, Long), linestring(25 41, 30 41, 30 45, 30 42) )
          &FORMAT=text/csv

-  Curtain clipping by a Polygon on 3D coverage, for example:

   .. hidden-code-block:: text

        http://localhost:8080/rasdaman/ows?SERVICE=WCS&VERSION=2.0.1
          &REQUEST=GetCoverage
          &COVERAGEID=test_eobstest
          &CLIP=CURTAIN(projection(Lat, Long), Polygon((25 40, 30 40, 30 45, 30 42)))
          &FORMAT=text/csv

-  Corridor clipping by a Linestring on 3D coverage, for example:

   .. hidden-code-block:: text

        http://localhost:8080/rasdaman/ows?SERVICE=WCS&VERSION=2.0.1
          &REQUEST=GetCoverage
          &COVERAGEID=test_irr_cube_2
          &CLIP=CORRIDOR( projection(E, N),
               &LineString(75042.7273594  5094865.55794 "2008-01-01T02:01:20.000Z",
                          &75042.7273594 5194865.55794 "2008-01-01T02:01:20.000Z"),
               &LineString(75042.7273594 5094865.55794, 75042.7273594 5094865.55794,
                          &85042.7273594 5194865.55794, 95042.7273594 5194865.55794)
              &)
          &FORMAT=application/gml+xml

-  Corridor clipping by a Polygon on 3D coverage, for example:

   .. hidden-code-block:: text

        http://localhost:8080/rasdaman/ows?SERVICE=WCS&VERSION=2.0.1
          &REQUEST=GetCoverage
          &COVERAGEID=test_eobstest
          &CLIP=corridor( projection(Lat, Long),
                          LineString(26 41 "1950-01-01", 28 41 "1950-01-02"),
                          Polygon((25 40, 30 40, 30 45, 25 45)), discrete )
          &FORMAT=application/gml+xml

WCS-T
-----

Currently, WCS-T supports importing coverages in GML format. The metadata of
the coverage is thus explicitly specified, while the raw cell values can be
stored either explicitly in the GML body, or in an external file linked in the
GML body, as shown in the examples below. The format of the file storing the
cell values must be 

- 2-D data `supported by the GDAL library <http://www.gdal.org/formats_list.html>`__,
  such as TIFF / GeoTIFF, JPEG / JPEG2000, PNG, etc;
- n-D data in NetCDF or GRIB format

The coverage metadata must be in XML or JSON format; any other format will lead
to an error.

In addition to the WCS-T standard parameters petascope supports additional
proprietary parameters.

.. NOTE::

   For coverage management normally WCS-T is not used directly. Rather, the more
   convenient ``wcst_import`` Python importing tool is recommended for
   :ref:`data-import`.

Inserting coverages
^^^^^^^^^^^^^^^^^^^

Inserting a new coverage into the server's WCS offerings is done using the
``InsertCoverage`` request.

.. table:: WCS-T Standard Parameters

    +------------------+------------------------+----------------------------------------------------------+-----------------------------+
    |Request           |Value                   |Description                                               |Required                     |
    |Parameter         |                        |                                                          |                             |
    +==================+========================+==========================================================+=============================+
    |SERVICE           |WCS                     |service standard                                          |Yes                          |
    +------------------+------------------------+----------------------------------------------------------+-----------------------------+
    |VERSION           |2.0.1 or later          |WCS version used                                          |Yes                          |
    +------------------+------------------------+----------------------------------------------------------+-----------------------------+
    |REQUEST           |InsertCoverage          |Request type to be performed                              |Yes                          |
    +------------------+------------------------+----------------------------------------------------------+-----------------------------+
    |INPUTCOVERAGEREF  |{url}                   |URl pointing to the coverage to be inserted               |One of inputCoverageRef or   |
    |                  |                        |                                                          |inputCoverage is required    |
    +------------------+------------------------+----------------------------------------------------------+-----------------------------+
    |INPUTCOVERAGE     |{coverage}              |A coverage to be inserted                                 |One of inputCoverageRef or   |
    |                  |                        |                                                          |inputCoverage is required    |
    +------------------+------------------------+----------------------------------------------------------+-----------------------------+
    |USEID             |new | existing          |Indicates wheter to use the coverage's id ("existing")    |No (default: existing)       |
    |                  |                        |or to generate a new unique one ("new")                   |                             |
    +------------------+------------------------+----------------------------------------------------------+-----------------------------+

.. table:: WCS-T Proprietary Enhancements

    +-------------+-------------------------------------------------+----------------------------------------------------------+--------+
    |Request      |Value                                            |Description                                               |Required|
    |Parameter    |                                                 |                                                          |        |
    +=============+=================================================+==========================================================+========+
    |PIXELDATATYPE|GDAL supported base data type (eg: "Float32") or |In cases where range values are given in the GML body     |No      |
    |             |comma-separated concatenated data types, (eg:    |the datatype can be indicated through this parameter.     |        |
    |             |"Float32,Int32,Float32")                         |Default: Byte.                                            |        |
    +-------------+-------------------------------------------------+----------------------------------------------------------+--------+
    |TILING       |rasdaman tiling clause, see                      |Indicates the array tiling to be applied during insertion |No      |
    |             |`wiki:Tiling <http://rasdaman.org/wiki/Tiling>`__|                                                          |        |
    +-------------+-------------------------------------------------+----------------------------------------------------------+--------+

The response of a successful coverage request is the coverage id of the newly
inserted coverage. For example: The coverage available at
http://schemas.opengis.net/gmlcov/1.0/examples/exampleRectifiedGridCoverage-1.xml
can be imported with the following request:

.. code-block:: text

    http://localhost:8080/rasdaman/ows?SERVICE=WCS&VERSION=2.0.1
        &REQUEST=InsertCoverage
        &COVERAGEREF=http://schemas.opengis.net/gmlcov/1.0/examples/exampleRectifiedGridCoverage-1.xml

The following example shows how to insert a coverage stored on the
server on which rasdaman runs. The cell values are stored in a TIFF file
(attachment:myCov.gml), the coverage id is generated by the server and
aligned tiling is used for the array storing the cell values:

.. code-block:: text

    http://localhost:8080/rasdaman/ows?SERVICE=WCS&VERSION=2.0.1
        &REQUEST=InsertCoverage
        &COVERAGEREF=file:///etc/data/myCov.gml
        &USEID=new
        &TILING=aligned[0:500,0:500]


Updating Coverages
^^^^^^^^^^^^^^^^^^

.. _update-coverage:

Updating an existing coverage into the server's WCS offerings is done using the ``UpdateCoverage`` request.

.. table:: WCS-T Standard Parameters

    +------------------+----------------------------------------------+----------------------------------------------------------+-----------------------------+
    |Request           |Value                                         |Description                                               |Required                     |
    |Parameter         |                                              |                                                          |                             |
    +==================+==============================================+==========================================================+=============================+
    |SERVICE           |WCS                                           |service standard                                          |Yes                          |
    +------------------+----------------------------------------------+----------------------------------------------------------+-----------------------------+
    |VERSION           |2.0.1 or later                                |WCS version used                                          |Yes                          |
    +------------------+----------------------------------------------+----------------------------------------------------------+-----------------------------+
    |REQUEST           |UpdateCoverage                                |Request type to be performed                              |Yes                          |
    +------------------+----------------------------------------------+----------------------------------------------------------+-----------------------------+
    |COVERAGEID        |{string}                                      |Identifier of the coverage to be updated                  |Yes                          |
    +------------------+----------------------------------------------+----------------------------------------------------------+-----------------------------+
    |INPUTCOVERAGEREF  |{url}                                         |URl pointing to the coverage to be inserted               |One of inputCoverageRef or   |
    |                  |                                              |                                                          |inputCoverage is required    |
    +------------------+----------------------------------------------+----------------------------------------------------------+-----------------------------+
    |INPUTCOVERAGE     |{coverage}                                    |A coverage to be updated                                  |One of inputCoverageRef or   |
    |                  |                                              |                                                          |inputCoverage is required    |
    +------------------+----------------------------------------------+----------------------------------------------------------+-----------------------------+
    |SUBSET            |AxisLabel(geoLowerBound, geoUpperBound)       |Trim or slice expression, one per updated                 |No                           |
    |                  |                                              |coverage dimension                                        |                             |
    +------------------+----------------------------------------------+----------------------------------------------------------+-----------------------------+

The following example shows how to update an existing coverage ``test_mr_metadata``
from a generated GML file by ``wcst_import`` tool:

.. code-block:: text

    http://localhost:8080/rasdaman/ows?SERVICE=WCS&version=2.0.1
        &REQUEST=UpdateCoverage
        &COVRAGEID=test_mr_metadata
        &SUBSET=i(0,60)
        &subset=j(0,40)
        &INPUTCOVERAGEREF=file:///tmp/4514863c_55bb_462f_a4d9_5a3143c0e467.gml


Deleting Coverages
^^^^^^^^^^^^^^^^^^

.. _delete-coverage:

The ``DeleteCoverage`` request type serves to delete a coverage (consisting of
the underlying rasdaman collection, the associated WMS layer (if exists)
and the petascope metadata).
For example: The coverage ``test_mr`` can be deleted as follows:

.. code-block:: text

    http://localhost:8080/rasdaman/ows?SERVICE=WCS&VERSION=2.0.1
      &REQUEST=DeleteCoverage
      &COVERAGEID=test_mr

Renaming coverage
^^^^^^^^^^^^^^^^^

.. _rename-coverage:

The ``UpdateCoverageId`` request allows to update 
a coverage's id and the associated WMS layer if one exists (v10.0+).

For example, the coverage ``test_mr`` can be renamed to ``test_mr_new`` as follows:

.. code-block:: text

    http://localhost:8080/rasdaman/admin/UpdateCoverageId?
        COVERAGEID=test_mr
       &NEWID=test_mr_new

.. _petascope-update-coverage-metadata:

Coverage Metadata Update
------------------------

Coverage metadata can be updated through the interactive rasdaman WSClient
by selecting a text file (MIME type one of: ``text/xml``, ``application/json``,
``text/plain``) containing new metadata and upload it to petascope.
Then, petascope will read the content of the text file and update corresponding
coverage's metadata.

.. NOTE::

   This WSClient feature is login protected: **OGC WCS > Describe Coverage tab**
   when one is already **logged in** with petascope admin user in **Admin tab**.

The service URL for this feature is ``http://localhost:8080/rasdaman/admin/UpdateCoverageMetadata``
which operates through multipart/form-data POST requests. The request should
contain 2 parts: the first part is coverageId to update, the second part is a
path to a local text file to be uploaded to server.

Alternatively, one can use REST API to update a coverage metadata with
petascope admin user's credentials *via basic authentication headers method*
by *curl* tool. For example: Metadata of coverage ``test_mr_metadata``
will be updated from the local XML file at ``/home/rasdaman/Downloads/test_metadata.xml``:

.. code-block:: text

   curl --user petauser:PETASCOPE_ADMIN_PASSWORD 
               -F "coverageId=test_mr_metadata" 
               -F "file=@/home/rasdaman/Downloads/test_metadata.xml" 
               "http://localhost:8080/rasdaman/admin/UpdateCoverageMetadata"

Check if a coverage / layer exists
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
In v10+, rasdaman supports nonstandard REST request to check if 
a coverage / layer exists. The result is ``true/false`` string literal.

.. code-block:: text

    http://localhost:8080/rasdaman/ows/objectExists?coverageId=${coverageId}
    http://localhost:8080/rasdaman/ows/objectExists?layer=${layer}


OGC Web Coverage Processing Service (WCPS)
==========================================

The OGC Web Coverage Processing Service (WCPS) standard defines a
protocol-independent language for the extraction, processing, analysis,
and fusion of multi-dimensional gridded coverages, often called
`datacubes <https://en.wikipedia.org/wiki/Data_cube>`__.

General
-------

WCPS requests can be submitted in both
abstract syntax (`example <http://rasdaman.org/browser/systemtest/testcases_services/test_wcps/queries/233-extra_params_merge_new_metadata.test>`__)
and in XML (`example <http://rasdaman.org/browser/systemtest/testcases_services/test_wcps/queries/245-test_enqoute_cdata_greate_less_character.xml>`__).

For example, using the WCS GET/KVP protocol binding a WCPS request can be sent
through the following ``ProcessCoverages`` request:

.. code-block:: text

    http://localhost:8080/rasdaman/ows?service=WCS&version=2.0.1
      &REQUEST=ProcessCoverage&QUERY=<wcps-query>

The following subsections list enhancements rasdaman offers over the
OGC WCPS standard. A brief introduction to the WCPS language is given
in the `WCPS cheatsheet <https://doc.rasdaman.org/11_cheatsheets.html#wcps>`__; 
more educational material is available on `EarthServer <https://earthserver.xyz/wcs>`__.  


Polygon/Raster Clipping
-----------------------

The proprietary ``clip()`` function with the same effect as clipping is available with WCPS.
The signature is as follows: 

.. code-block:: text

    clip( coverageExpression, wkt [, subsettingCrs ] )

where
-  ``coverageExpression`` is an expression of result type coverage, eg: ``dem + 10``;
-  ``wkt`` is a valid WKT (Well-Known Text) expression, e.g. ``POLYGON((...))``, ``LineString(...)``;
-  ``subsettingCrs`` is an optional CRS in which the ``wkt``coordinates are expressed, eg: ``http://opengis.net/def/crs/EPSG/0/4326``.

Clipping Examples
^^^^^^^^^^^^^^^^^

- Polygon clipping with coordinates in ``EPSG:4326`` on coverage with Native CRS ``EPSG:3857``:

  .. hidden-code-block:: text

    for c in (test_wms_3857)
    return
      encode(
        clip( c,
              POLYGON((
                -17.8115 122.0801, -15.7923 135.5273,
                -24.8466 151.5234, -19.9733 137.4609,
                -33.1376 151.8750, -22.0245 135.6152,
                -37.5097 145.3711, -24.4471 133.0664,
                -34.7416 135.8789, -25.7207 130.6934,
                -31.8029 130.6934, -26.5855 128.7598,
                -32.6949 125.5078, -26.3525 126.5625,
                -35.0300 118.2129, -25.8790 124.2773,
                -30.6757 115.4004, -24.2870 122.3438,
                -27.1374 114.0820, -23.2413 120.5859,
                -22.3501 114.7852, -21.4531 118.5645
              )),
              "http://opengis.net/def/crs/EPSG/0/4326"
        ),
        "image/png"
      )

- Linestring clipping on 3D coverage with axes ``X``, ``Y``, ``datetime``.

  .. hidden-code-block:: text

    for c in (test_irr_cube_2)
    return
      encode(
        clip( c,
              LineStringZ(75042.7273594 5094865.55794 "2008-01-01T02:01:20.000Z", 705042.727359 5454865.55794 "2008-01-08T00:02:58.000Z")
        ),
        "text/csv"
      )

- Linestring clipping on 2D coverage with axes ``X``, ``Y``.

  .. hidden-code-block:: text

    for c in (test_mean_summer_airtemp)
    return
      encode(
        clip( c, LineString(-29.3822 120.2783, -19.5184 144.4043) ),
        "text/csv"
      )

   In this case the geo coordinates of the values on the linestring will be
   included as well in the result. The first band of the result will hold the
   X coordinate, the second band the Y coordinate, and the remaining bands the
   original cell values. Example output for the above query: ::

   .. code-block:: text

    "-28.975 119.975 90","-28.975 120.475 84","-28.475 120.975 80", ...

-  Multipolygon clipping on 2D coverage.

   .. hidden-code-block:: text

    for c in (test_mean_summer_airtemp)
    return
      encode(
        clip( c, Multipolygon(
                   (( -20.4270 131.6931, -28.4204 124.1895,
                      -27.9944 139.4604, -26.3919 129.0015 )),
                   (( -20.4270 131.6931, -19.9527 142.4268,
                      -27.9944 139.4604, -21.8819 140.5151 ))
                 )
        ),
        "image/png"
      )

- Curtain clipping by a Linestring on 3D coverage

  .. hidden-code-block:: text

    for c in (test_eobstest)
    return
      encode(
        clip( c, CURTAIN(projection(Lat, Long), linestring(25 40, 30 40, 30 45, 30 42) ) ),
        "text/csv"
      )

- Curtain clipping by a Polygon on 3D coverage

  .. hidden-code-block:: text

    for c in (test_eobstest)
    return
      encode(
        clip( c, CURTAIN(projection(Lat, Long), Polygon((25 40, 30 40, 30 45, 30 42)) ) ),
        "text/csv"
      )


- Corridor clipping by a Linestring on 3D coverage

  .. hidden-code-block:: text

    for c in (test_irr_cube_2)
    return
      encode(
        clip( c,
              corridor(
                projection(E, N),
                LineString( 75042.7273594  5094865.55794 "2008-01-01T02:01:20.000Z",
                            75042.7273594 5194865.55794 "2008-01-01T02:01:20.000Z" ),
                Linestring( 75042.7273594 5094865.55794, 75042.7273594 5094865.55794,
                            85042.7273594 5194865.55794, 95042.7273594 5194865.55794 )
              )
        ),
        "application/gml+xml"
      )

- Corridor clipping by a Polygon on 3D coverage (geo CRS: ``EPSG:4326``)
  with input geo coordinates in ``EPSG:3857``. 

  .. hidden-code-block:: text

    for c in (test_eobstest)
    return
      encode(
        clip( c,
              corridor(
                projection(Lat, Long),
                LineString(4566099.12252 2999080.94347 "1950-01-01",
                           4566099.12252 3248973.78965 "1950-01-02"),
                Polygon((4452779.63173 2875744.62435, 4452779.63173 3503549.8435,
                         5009377.0857 3503549.8435, 5009377.0857 2875744.62435) )
              ),
              "http://localhost:8080/def/crs/EPSG/0/3857"
        ),
        "application/gml+xml"
      )

Auto-ratio for scaling X or Y axis in WCPS
------------------------------------------

As aproprietary extension, the ``scale()`` function in WCPS allows to specify
the target extent of only one of the spatial horizontal axes, instead of both.
In this case, the extent of the other axis will be determined automatically
while preserving the original ratio between the two spatial axes.

For example in the request below, petascope will automatically set
the extent  of ``Lat`` to a value that preserves the ratio in the output result: 

.. hidden-code-block:: text

   for $c in (test_mean_summer_airtemp)
   return
     encode( scale( $c, { Long:"CRS:1"(0:160) } ), "image/png" )

Implicitly add full domains in scale() for unspecified non X/Y axes in WCPS
---------------------------------------------------------------------------

The ``scale()`` function in WCPS will implicitly add the full domains
of unspecified non X/Y axes of a given coverage in the scales' domain intervals.
In other words, unspecified non X/Y axes will not be scaled.

For example in this query below, if a coverage is 3D and one only
specified ``X (E)`` and ``Y (N)`` axes as scale's domain intervals,
then the ``time`` axis's domains will be added implicitly in petascope.

.. hidden-code-block:: text

   for $c in (test_irr_cube_2)
   return encode(
                 scale(
                       $c[ansi("2008-01-01T02:01:20":"2008-01-08T00:02:58")] ,  
                       { E:"CRS:1"(0:20), N:"CRS:1"(0:10) }
                      )
                , "json" )


Automatic domain extraction
---------------------------

The domain interval can be extracted from a domain, including an 
``imageCrsDomain`` (in modern nomenclature: index domain).
Both the interval - ie: ``[lowerBound:upperBound]`` - and lower as well 
as upper bound can be retrieved for each axis.

Syntax: ::

   operator(.lo|.hi)?

with ``.lo`` or ``.hi`` returning the lower bound or upper bound of this interval.

.. code-block:: text

   Coverage test_eobstest has 3 dimensions with extent (0:5,0:29,0:39).
   Expression imageCrsdomain(c,Long) in this case returns 0:39
   whereas imageCrsdomain(c,Long).hi returns 39.

Further, the third argument of the ``domain()`` operator, the CRS URL,
is now optional. If not specified, ``domain()`` will use the CRS of the
selected axis (ie, the second argument) instead.


LET clause in WCPS
------------------

An optional ``LET`` clause is supported in WCPS queries.
It allows binding alias variables to valid WCPS query sub-expressions,
and subsequently make use of the variables in the ``return`` clause
instead of repeating the aliased sub-expressions.

The syntax is ::

   FOR-CLAUSE
   LET $variable := assignment [ , $variable := assignment ]
       ...
   [ WHERE-CLAUSE ]
   RETURN-CLAUSE

where ::

   assignment ::= coverageExpression | domainExpression


.. code-block:: text

  for $c in (test_mr) 
  let $a := $c[i(0:50), j(0:40)],  
      $b := avg($c) * 2 
  return
    encode( scale( $c, { imageCrsDomain( $c ) } ) + $b, "image/png" )

A special shorthand subset expression allows to conveniently specify domains.
The variable in the LET clause follows this syntax: ::
  
  LET $variable := [ dimensionalIntervalList ]

This can readily be used in a subset expression: ::

  coverageVariable[$variable1]

.. code-block:: text

  for $c in (test_mr) 
  let $a := [i(20), j(40)], 
      $b := 10 
  return
    encode( $c[ $a ] + $b, "itext/json" )

Binary min, max operations in WCPS
----------------------------------

Since v10+, rasdaman supports binary ``min()`` and ``max()`` operations.
For two compatible coverages A and B they calculate a result coverage 
with the minimum / maximum for each pair of corresponding cell values of A and B.
Example:

.. code-block:: text

  min(A, B)

where A and B are coverage expressions.

.. _positional_parameters_in_wcps:

Positional parameter in WCPS
----------------------------

Since v10+, rasdaman supports positional parameters in WCPS (non-standard).
Positional parameters allow to reference binary or string values in a WCPS query.
The binary/string values are specified in a POST request, in addition to the
WCPS query.

Syntax
^^^^^^

The syntax for the positional parameter is:

.. code-block:: text

   $POSITIVE_INTEGER e.g: $1, $2,..

The endpoint to send WCPS query by POST with extra values is:

.. code-block:: text

    localhost:8080/rasdaman/ows?SERVICE=WCS&VERSION=2.0.1&REQUEST=ProcessCoverages

with the mandatory parameter ``query`` and optional positional parameters: ``1``, ``2``,...
The value of a positional parameter can be either **binary file content** 
or **string value**.


.. _positional_parameters_in_wcps_example:

Example
^^^^^^^

One can use ``curl`` tool as a client to send a WCPS request with 
**positional parameters** to rasdaman via POST. ``curl`` reads the contents
of the selected files automatically via file paths with ``@`` parameter.

For example, one wants to combine an existing coverage (``$c`` variable)
with two temporary covarages (``$d`` and ``$e`` variables) in ``png`` format 
from positional parameters ``$1``, ``$2`` and ``$3`` respectively :

.. code-block:: text

   curl -s "http://localhost:8080/rasdaman/ows?SERVICE=WCS&VERSION=2.0.1&REQUEST=ProcessCoverages" \
   -F 'query=for $c in (existing_coverage), $d in (decode($1)), $e in (decode($2)) return encode(($c + $d + $e)[Lat(0:90), Long(-180:180)], "$3"))' \
   -F "1=@/home/rasdaman/file1.tiff" \
   -F "2=@/home/rasdaman/file2.tiff" \
   -F "3=png" \
   > ~/Downloads/test.png


Decode Operator in WCPS
-----------------------

Since v10+, rasdaman supports the non-standard ``decode()`` operator
in WCPS. This feature allows one to combine existing coverages with
temporary coverages created in memory from input files attached
in POST request body.

Syntax
^^^^^^

The syntax for the ``decode()`` operator is:

.. code-block:: text

   decode(${positional_parameter}) 

with ``${positional_parameter)`` refers to the indicated files in the POST request.
See :ref:`positional parameters in WCPS <positional_parameters_in_wcps>`.

.. NOTE::

    Currently, WCPS ``decode()`` operator supports only 2D geo-referenced
    files readable by GDAL. One way to check if a file is readable by GDAL is
    with ``gdalinfo ${file}``. ``netCDF/GRIB`` format is not supported.

Example
^^^^^^^

See :ref:`example about positional parameters in WCPS <positional_parameters_in_wcps_example>`.


Case Distinction
----------------

As another proprietary extension, conditional evaluation is added to WCPS
following the overall XQuery-oriented syntax.

Syntax
^^^^^^

.. code-block:: text

  SWITCH
    CASE condExp RETURN resultExp
    [ CASE condExp RETURN resultExp ]*
    DEFAULT RETURN resultExpDefault

where ``condExp`` and ``resultExp`` are either scalar-valued or
coverage-valued expressions.

Constraints
^^^^^^^^^^^

- All condition expressions must return either boolean values or boolean coverages
- All result expressions must return either scalar values, or coverages
- The domain of all condition expressions must be the same
- The domain of all result expressions must be the same (that means same extent, 
  resolution/direct positions, crs)

Evaluation Rules
^^^^^^^^^^^^^^^^

If the result expressions return scalar values, the returned scalar value on a
branch is used in places where the condition expression on that branch evaluates
to ``True``. If the result expressions return coverages, the values of the returned
coverage on a branch are copied in the result coverage in all places where the
condition coverage on that branch contains pixels with value ``True``.   

The conditions of the statement are evaluated in a manner similar to the
IF-THEN-ELSE statement in programming languages such as Java or C++. This
implies that the conditions must be specified by order of generality, starting
with the least general and ending with the default result, which is the most
general one. A less general condition specified after a more general condition
will be ignored, as the expression meeting the less general expression will have
had already met the more general condition.

Furthermore, the following hold:

- ``domainSet(result)`` = ``domainSet(condExp1)``
- ``metadata(result)`` = ``metadata(condExp1)``
- ``rangeType(result)`` = ``rangeType(resultExp1)``. In case resultExp1
  is a scalar, the result range type is the range type describing the
  coverage containing the single pixel resultExp1. 

Examples
^^^^^^^^

.. code-block:: text

  switch
    case $c < 10 return {red: 0;   green: 0;   blue: 255}
    case $c < 20 return {red: 0;   green: 255; blue:   0}
    case $c < 30 return {red: 255; green: 0;   blue:   0}
    default      return {red: 0;   green: 0;   blue:   0}

The above example assigns blue to all pixels in the $c coverage having a value
less than 10, green to the ones having values at least equal to 10, but less
than 20, red to the ones having values at least equal to 20 but less than 30 and
black to all other pixels.

.. code-block:: text

  switch
    case $c > 0 return log($c)
    default     return 0

The above example computes log of all positive values in $c, and assigns 0 to
the remaining ones.

.. code-block:: text

  switch
    case $c < 10 return $c * {red: 0;   green: 0;   blue: 255}
    case $c < 20 return $c * {red: 0;   green: 255; blue: 0}
    case $c < 30 return $c * {red: 255; green: 0;   blue: 0}
    default      return      {red: 0;   green: 0;   blue: 0}

The above example assigns *blue: 255* multiplied by the original pixel value to all
pixels in the $c coverage having a value less than 10, *green: 255* multiplied by
the original pixel value to the ones having values at least equal to 10, but
less than 20, *red: 255* multiplied by the original pixel value to the ones having
values at least equal to 20 but less than 30 and black to all other pixels.


CIS 1.0 to CIS 1.1 Transformation
---------------------------------

For output format ``application/gml+xml`` WCPS supports delivery
as CIS 1.1 ``outputType=GeneralGridCoverage`` by specifying an additional
proprietary parameter ``outputType`` in the ``encode()`` function.

.. code-block:: text

    for c in (test_irr_cube_2)
    return encode( c, "application/gml+xml", 
                      "{\"outputType\":\"GeneralGridCoverage\"}" ) 

Query Parameter
---------------

In v10+, rasdaman supports the ``q`` parameter besides the ``query`` parameter
to store a WCPS query. A request must contain only one ``q`` or ``query`` parameter.

.. code-block:: text

    http://localhost:8080/rasdaman/ows?service=WCS&version=2.0.1
      &REQUEST=ProcessCoverage&q=<wcps-query>

Describe Operator in WCPS
-------------------------

For some ``coverageExpression`` the ``describe()`` function delivers a "coverage description" 
consisting of the result coverage, except the range set, in either GML or JSON.
This function is not part of the WCPS standard.

Syntax
^^^^^^

.. code-block:: text

   describe( coverageExpression, outputFormat [ , extraParameter ] )

where

- ``outputFormat`` is a string specifying the format encoding
  in which the result will be formatted. Formats are indicated through
  their MIME type identifier, just as in ``encode()``. Formats supported:
   
   - ``application/gml+xml`` (or short: ``gml``) for GML
   - ``application/json`` (or short: ``json``) for JSON

- ``extraParameters`` is an optional string containing parameters
  for fine-tuning the output, just as in ``encode()``. Options supported:

   - ``"outputType=GeneralGridCoverage"`` to return a CIS 1.1
     General Grid Coverage structure

Semantics
^^^^^^^^^

A ``describe()`` operation returns a description of the coverage resulting
from the coverage expression passed, consisting of domain set, range type, and metadata, 
but not the range set. As such, this operator is the WCPS equivalent 
to a WCS ``DescribeCoverage`` request, and the output adheres to the same WCS schema.

The coverage description generated will follow the coverage's type, 
so one of Rectified Grid Coverage (CIS 1.0), ReferenceableGridCoverage (CIS 1.0),
General Grid Coverage (CIS 1.0). 

By default, the coverage will be provided
as Rectified or Referenceable Grid Coverage (in accordance with its type); 
optionally, a General Grid Coverage can be generated instead. 
However, generation of a General Grid Coverage structure can be enforced through
``"outputType=GeneralGridCoverage"``. 
As JSON is supported only from OGC CIS 1.1 onwards this format is only available 
(i) if the coverage is stored as a CIS 1.1 General Grid Coverage 
(currently not supported) or (ii) this output type is selected
explicitly through ``extraParameter``.

**Efficiency**: The ``describe()`` operator normally does not materialize
the complete coverage, but determines only the coverage description making
this function very efficient. A full evaluation is only required
if ``coverageExpression`` contains a ``clip()`` performing a curtain, corridor,
or linestring operation.

Examples
^^^^^^^^

- Determine coverage description as a CIS 1.0 Rectified Grid Coverage in GML, 
  without evaluating the range set:

  .. code-block:: text

     for $c in (Cov)
     return describe( $c.red[Lat(10:20), Long(30:40), "application/gml+xml" )

- Deliver coverage description as a CIS 1.1 General Grid Coverage in GML,
  where range type changes in the query:

  .. code-block:: text

     for $c in (Cov)
     return describe( { $c.red; $c.green; $c.blue }, "application/gml+xml", 
                                         "outputType=GeneralGridCoverage" )

- Deliver coverage description as a CIS 1.1 General Grid Coverage, in JSON:

  .. code-block:: text

     for $c in (Cov)
     return describe( $c, "application/json", "outputType=GeneralGridCoverage" )


Specific Exceptions
^^^^^^^^^^^^^^^^^^^

- Unsupported output format
- This format only supported for General Grid Coverage
- Illegal extra parameter


OGC Web Map Service (WMS)
=========================

The OGC Web Map Service (WMS) standard provides a simple HTTP interface
for requesting overlays of geo-registered map images, ready for display.

With petascope, geo data can be served simultaneously via WMS, WCS,
and WCPS. Further information:

- :ref:`How to publish a WMS layer via WCST\_Import <wms-import>`.
- :ref:`Add WMS style queries to existing layers <style-creation>`.

WMS GetMap: Special Functionality
---------------------------------

Transparency
^^^^^^^^^^^^

By adding a parameter ``transparent=true`` to WMS requests the returned image
will have ``NoData Value=0`` in the metadata indicating to the client 
that all pixels with value *0* value should be considered transparent for PNG
encoding format.

.. hidden-code-block:: text

    http://localhost:8080/rasdaman/ows?SERVICE=WMS&VERSION=1.3.0
        &REQUEST=GetMap&LAYERS=waxlake1
        &BBOX=618887,3228196,690885,3300195.0
        &CRS=EPSG:32615&WIDTH=600&HEIGHT=600&FORMAT=image/png
        &TRANSPARENT=true

.. _wms-interpolation:

Interpolation
^^^^^^^^^^^^^

If in a ``GetMap`` request the output CRS requested is different from
the coverage's Native CRS petascope will duly reproject the map applying
resampling and interpolation. The algorithm used can be controlled with
the proprietary ``GetMap`` parameter ``interpolation={method}``;
default is nearest-neighbour interpolation.
See :ref:`sec-geo-projection` for the methods available and their meaning.

.. hidden-code-block:: text

    http://localhost:8080/rasdaman/ows?SERVICE=WMS
        &VERSION=1.3.0
        &REQUEST=GetMap
        &LAYERS=test_wms_3857
        &BBOX=-44.525,111.976,-8.978,156.274
        &CRS=EPSG:4326
        &WIDTH=60&HEIGHT=60
        &FORMAT=image/png
        &INTERPOLATION=bilinear

3D+ Coverages as WMS Layers
---------------------------

Petascope allows to import a 3D+ coverage as a WMS layer.
To this end, the ingrdients file used for ``wcst_import``
must contain ``wms_import": true``. This works for 3D+ coverages
with recipes *regular_time_series*, *irregular_time_series*, and *general_coverage* recipes.
`This example <http://rasdaman.org/browser/systemtest/testcases_services/test_all_wcst_import/testdata/wms_3d_time_series_irregular/ingest.template.json>`__
demonstrates how to define an *irregular_time_series* 3D coverage from 2D GeoTIFF files.

Once the coverage is created, a ``GetMap`` request can use the additional
(non-horizontal) axes for subsetting according to the OGC WMS 1.3.0 standard.

.. TABLE:: WMS Subset Parameters for Different Axis Types

    +------------------++-------------------------------------------------+
    |Axis Type         |Subset parameter                                  |
    +==================+==================================================+
    |Time              |time=...                                          |
    +------------------+--------------------------------------------------+
    |Elevation         |elevation=...                                     |
    +------------------+--------------------------------------------------+
    |Other             |dim_AxisName=... (e.g dim_pressure=...)           |
    +------------------+--------------------------------------------------+


According to the WMS 1.3.0 specification, the subset
for non-geo-referenced axes can have these formats:

- Specific value (*value1*): time='2012-01-01T00:01:20Z, dim_pressure=20,...

- Range values (*min/max*): time='2012-01-01T00:01:20Z'/'2013-01-01T00:01:20Z,
  dim_pressure=20/30,...

- Multiple values (*value1,value2,value3,...*): time='2012-01-01T00:01:20Z,
  '2013-01-01T00:01:20Z, dim_pressure=20,30,60,100,...

- Multiple range values (*min1/max1,min2/max2,...*):
  dim_pressure=20/30,40/60,...

.. NOTE::

   A ``GetMap`` request always returns a 2D result. If a non-geo-referenced axis
   is omitted from the request it will be considered as a slice
   on the upper bound along this axis. For example, in a time-series 
   the youngest timeslice will be delivered).

  Examples:

  - Multiple values on `time axis of 3D coverage <http://rasdaman.org/browser/systemtest/testcases_services/test_wms/queries/29-get_map_on_3d_time_series_irregular_time_specified.test>`__.
  - Multiple values on `time, dim_pressure axes of 4d coverage <http://rasdaman.org/browser/systemtest/testcases_services/test_wms/queries/31-get_map_on_4d_coverage_dim_pressure_and_time_irregular_specified.test>`__.

WMS Layer Management
--------------------

Additional proprietary requests, beyond the WMS standard, allow for service maintenance.

Layers can be easily created from existing coverages in WCS in two ways:

- By specifying WMS setup during import coverage in the respective
  ingredients file; see :ref:`wms_import <wms-import>`;
- By sending an :ref:`InsertWCSLayer <insert-wcs-layer>` HTTP request
  to petascope.

The following proprietary WMS request types serve to manage the WMS offering
of rasdaman:

.. _insert-wcs-layer:

- ``InsertWCSLayer``: create a new WMS layer from an existing coverage.

.. hidden-code-block:: text

    http://localhost:8080/rasdaman/ows?SERVICE=WMS&VERSION=1.3.0
           &REQUEST=InsertWCSLayer
           &WCSCOVERAGEID=MyCoverage

- ``UpdateWCSLayer``: update an existing WMS layer from an existing coverage
  which associates with this WMS layer.

.. hidden-code-block:: text

    http://localhost:8080/rasdaman/ows?SERVICE=WMS&VERSION=1.3.0
            &REQUEST=UpdateWCSLayer
            &WCSCOVERAGEID=MyCoverage

- A layer can be removed either directly with a ``DeleteLayer`` 
  request (since rasdaman v10.0), or indirectly when deleting a coverage
  (:ref:`removing the associated WCS coverage <delete-coverage>`). 
  The ``DeleteLayer`` request is of the form:

.. hidden-code-block:: text

    http://localhost:8080/rasdaman/ows?SERVICE=WMS&VERSION=1.3.0
            &REQUEST=DeleteLayer
            &LAYER=MyLayer

.. _style-creation:

WMS Style Management
--------------------

Styles can be created for layers using rasql and WCPS query fragments. This
allows users to define several visualization options for the same dataset in a
flexible way. Examples of such options would be color classification, NDVI
detection etc. The following HTTP request will create a style with the name,
abstract and layer provided in the KVP parameters below

.. NOTE::

    For Tomcat version 7+ it requires the query (WCPS/rasql fragment)
    to be URL-encoded correctly. `This site <http://meyerweb.com/eric/tools/dencoder/>`__ 
    offers such an encoding service.


Style Definition Variants
^^^^^^^^^^^^^^^^^^^^^^^^^

-  WCPS query fragment example (since rasdaman 9.5):

   .. hidden-code-block:: text

    http://localhost:8080/rasdaman/ows?SERVICE=WMS&VERSION=1.3.0
        &REQUEST=InsertStyle
        &NAME=wcpsQueryFragment
        &LAYER=test_wms_4326
        &ABSTRACT=This style marks the areas where fires are in progress with the color red
        &WCPSQUERYFRAGMENT=switch case $c > 1000 return {red: 107; green:17; blue:68} default return {red: 150; green:103; blue:14})

   Variable ``$c`` will be replaced by a layer name when sending a ``GetMap``
   request containing this layer's style.

-  Rasql query fragment examples:

   .. hidden-code-block:: text

    http://localhost:8080/rasdaman/ows?SERVICE=WMS&version=1.3.0&REQUEST=InsertStyle
        &NAME=FireMarkup
        &LAYER=dessert_area
        &ABSTRACT=This style marks the areas where fires are in progress with the color red
        &RASQLTRANSFORMFRAGMENT=case $Iterator when ($Iterator + 2) > 200 then {255, 0, 0} else {0, 255, 0} end

   Variable ``$Iterator`` will be replaced with the actual name of the rasdaman
   collection and the whole fragment will be integrated inside the regular
   ``GetMap`` request.

-  Multiple layers can be used in a style definition. 
   Besides the iterators ``$c`` in WCPS query fragments and ``$Iterator`` in rasql
   query fragments, which always refer to the current layer, other layers
   can be referenced by name using an iterator of the form ``$LAYER_NAME`` in the style expression. 
  
   Example: create a WCPS query fragment style referencing 2 layers
   (``$c`` refers to layer *sentinel2_B4* which defines the style):

   .. hidden-code-block:: text

    http://localhost:8080/rasdaman/ows?SERVICE=WMS&VERSION=1.3.0
        &REQUEST=InsertStyle
        &NAME=BandsCombined
        &LAYER=sentinel2_B4
        &ABSTRACT=This style needs 2 layers
        &WCPSQUERYFRAGMENT=$c + $sentinel2_B8

   Then, in any ``GetMap`` request using this style
   the result will be obtained from the combination of the 2 layers
   *sentinel2_B4* and *sentinel2_B8*:

   .. hidden-code-block:: text

    http://localhost:8080/rasdaman/ows?SERViCE=WMS&VERSION=1.3.0
        &REQUEST=GetMap
        &LAYERS=sentinel2_B4
        &BBOX=-44.975,111.975,-8.975,155.975&CRS=EPSG:4326
        &WIDTH=800&HEIGHT=600
        &FORMAT=image/png&transparent=true
        &STYLES=BandsCombined

-  WMS styling supports a ``ColorTable`` definition which
   allows to colorize the result of WMS GetMap request when the style is requested.
   A style can contain either one or both **query fragment** and **Color Table** definitions.
   The ``InsertStyle`` request supports two new **non-standard** 
   extra parameters ``colorTableType`` (valid values: ``ColorMap``, ``GDAL`` and ``SLD``)
   and ``colorTableDefintion`` containing corresponding definition, example:

   .. hidden-code-block:: text

    http://localhost:8080/rasdaman/ows?SERVICE=WMS&VERSION=1.3.0
        &REQUEST=InsertStyle
        &NAME=test
        &LAYER=test_wms_4326
        &ABSTRACT=This style marks the areas where fires are in progress with the color red
        &WCPSQUERYFRAGMENT=switch case $c > 1000 return {red: 107; green:17; blue:68} default return {red: 150; green:103; blue:14})
        &COLORTABLETYPE=ColorMap
        &COLORTABLEDEFINITION={"type": "intervals", "colorTable": {  "0": [0, 0, 255, 0], "100": [125, 125, 125, 255], "255": [255, 0, 0, 255] } }

   Below the supported color table definitions for each color table type are explained:

    * Rasdaman ``ColorMap``: check :ref:`coloring-arrays` for more details.
      The color table definition must be a JSON object, for example:

      .. hidden-code-block:: json

        { 
          "type": "intervals",  
          "colorTable": {  "0":   [0,     0, 255,   0],  
                           "100": [125, 125, 125, 255],  
                           "255": [255,   0,   0, 255]  
                        } 
        }

    * GDAL ``ColorPalette``: check :ref:`encode` for more details.
      The color table definition must be a JSON object and contains **256 color arrays**
      in ``colorTable`` array, example:

      .. hidden-code-block:: json

        {
           "colorTable": [
                          [255,  0,  0,255],
                          [216, 31, 30,255],
                          [216, 31, 30,255],
                          ...,
                          [ 43,131,186,255]
                         ]
        }

    * WMS ``Styled Layer Descriptor (SLD)``: The color table definition must be valid XML
      and contains ``ColorMap`` element. Check :ref:`coloring-arrays` for details about the supported types
      (``ramp`` (default), ``values``, ``intervals``), example ``ColorMap`` with ``type="values"``: 

      .. hidden-code-block:: xml

        <?xml version="1.0" encoding="UTF-8"?>
        <StyledLayerDescriptor xmlns="http://www.opengis.net/sld"
                               xmlns:gml="http://www.opengis.net/gml" 
                               xmlns:sld="http://www.opengis.net/sld"
                               xmlns:ogc="http://www.opengis.net/ogc"
                               version="1.0.0">
          <UserLayer>
            <sld:LayerFeatureConstraints>
              <sld:FeatureTypeConstraint/>
            </sld:LayerFeatureConstraints>
            <sld:UserStyle>
              <sld:Name>sqi_fig5_crop1</sld:Name>
              <sld:FeatureTypeStyle>
                <sld:Rule>
                  <sld:RasterSymbolizer>
                    <sld:ColorMap type="values">
                       <ColorMapEntry color="#0000FF" quantity="150" />
                       <ColorMapEntry color="#FFFF00" quantity="200" />
                       <ColorMapEntry color="#FF0000" quantity="250" />
                    </sld:ColorMap>
                  </sld:RasterSymbolizer>
                </sld:Rule>
              </sld:FeatureTypeStyle>
            </sld:UserStyle>
          </UserLayer>
        </StyledLayerDescriptor>

WMS Style Removal
^^^^^^^^^^^^^^^^^

The proprietary ``DeleteStyle`` WMS request type allows to remove
a particular style of an existing WMS layer. ::

    http://localhost:8080/rasdaman/ows?SERVICE=WMS&VERSION=1.3.0
        &REQUEST=DeleteStyle
        &LAYER=dessert_area
        &NAME=FireMarkup


Testing a WMS Setup
-------------------

A rasdaman WMS service can be tested with any conformant client through
a ``GetMap`` request like the following:

.. hidden-code-block:: text

    http://example.org/rasdaman/ows?service=WMS&version=1.3.0&request=GetMap
        &layers=MyLayer
        &bbox=618885.0,3228195.0,690885.0,3300195.0
        &crs=EPSG:32615
        &width=600
        &height=600
        &format=image/png


Errors and Workarounds
----------------------

**Cannot load new WMS layer in QGIS**
    In this case, the problem is due to QGIS caching the WMS GetCapabilities from the last
    request so the new layer does not exist (see
    `clear caching solution <http://osgeo-org.1560.x6.nabble.com/WMS-provider-Cannot-calculate-extent-td5250516.html>`__).

.. _wcs-t-non-standard-requests-wms:

WMS Pyramid Management
----------------------

The following proprietary WMS requests are used to manage downscaled coverages. 
Internally they are used for efficient zooming in/out
in WMS, and downscaling when using the ``scale()`` function in WCPS
or scaling extension in WCS.

.. _create_pyramid_member:

* ``CreatePyramidMember``: create a pyramid member coverage *c* 
  for a base coverage *b* with given scale factors for each axis 
  (note: only regular axis can have *scale factor > 1*);  
  e.g. to create a downscaled coverage *cov_3D_4* of a 3D coverage *cov_3D*
  that is *4x smaller* for Lat and Long regular axes 
  (Time is irregular axis, hence, scale factor must be 1):

  .. hidden-code-block:: text

    http://localhost:8080/rasdaman/admin?
        &REQUEST=CreatePyramidMember
        &BASE=cov_3D
        &MEMBER=cov_3D_4
        &SCALEFACTOR=1,4,4

.. _add_pyramid_member:

* ``AddPyramidMember``: add an existing coverage *c* as a pyramid member coverage
  for a base coverage *b*. The scale factors for each axis of the pyramid member coverage will
  be calculated implicitly based on axis resolutions. 
  If *harvesting=true* (default is false), recursively collect pyramid members
  of coverage *c*  and add them as pyramid member of coverage *b*; 
  e.g. to add a downscaled coverage *cov_3D_4* (4x smaller) and its pyramid members
  recursively as pyramid member coverages of base coverage *cov_3D*:

  .. hidden-code-block:: text

    http://localhost:8080/rasdaman/admin?
        &REQUEST=AddPyramidMember
        &BASE=cov_3D
        &MEMBER=cov_3D_4
        &HARVESTING=true    
  

* ``RemovePyramidMember``: remove an existing pyramid member coverage *c*
  from a base coverage *b* (coverage *c* still exists, until admin
  deletes with WCS-T :ref:`DeleteCoverage request <delete-coverage>`); 
  e.g. to remove downscaled coverage *cov_3D_4* from base coverage *cov_3D*:

  .. hidden-code-block:: text

    http://localhost:8080/rasdaman/admin
        &REQUEST=RemovePyramidMember
        &BASE=cov_3D
        &MEMBER=cov_3D_4

* ``ListPyramidMembers``: get a JSON list with objects of all pyramid member coverages
  associated with a base coverage:

  .. hidden-code-block:: text

    http://localhost:8080/rasdaman/admin
        &REQUEST=ListPyramidMembers
        &BASE=Sentinel2_10m

  Output example:
    
  .. hidden-code-block:: json  

    {
      "coverage": "Sentinel2_10m",
      "members": [
          {
            "coverage": "Sentinel2_20m",
            "scale_factors": [ 1, 2, 2 ]
          }, 
          {
            "coverage": "Sentinel2_60m",
            "scale_factors": [ 1, 6, 6 ]
          }
        ]
    }


``wcst_import`` can send ``CreatePyramidMember`` requests automatically 
when importing data with it with ``scale_levels`` or ``scale_factors`` option
in the ingredients file, more details :ref:`here <data-import-intro>`.


.. _data-import:

Data Import
===========

Raster data in a variety of formats, such as TIFF, netCDF, GRIB, etc.
can be imported in rasdaman through the ``wcst_import.sh`` utility.
Internally it is based on ``WCS-T`` requests, but hides the complexity and
maintains the geo-related metadata in its so-called ``petascopedb``
while the raster data get imported into the rasdaman array store.

Building large *time-series / datacubes*, *mosaics*, etc. and keeping them
up-to-date as new data become available is supported for a large variety of data
formats and file/directory organizations.

The systemtest contains `many examples <http://rasdaman.org/browser/systemtest/testcases_services/test_all_wcst_import/testdata>`__
for importing different types of data.

.. _data-import-intro:

Introduction
------------

The ``wcst_import.sh`` tool is based on two concepts:

- **Recipe** - A recipe defines how a set of data files can be combined into a
  well-defined coverage (e.g. a 2-D mosaic, regular or irregular 3-D timeseries, 
  etc.);

- **Ingredients** - A JSON file that configures how the recipe should build the 
  coverage (e.g. the server endpoint, the coverage name, which files to consider,
  etc.).

To execute an ingredients file in order to import some data: ::

    $ wcst_import.sh path/to/my_ingredients.json

Alternatively, ``wcst_import.sh`` can be started in the background as a daemon: ::

    $ wcst_import.sh path/to/my_ingredients.json --daemon start

or as a daemon that is "watching" for new data at some interval (in seconds): ::

    $ wcst_import.sh path/to/my_ingredients.json --watch <interval>

For further informations regarding the usage of ``wcst_import.sh``: ::

    $ wcst_import.sh --help

The workflow behind is depicted approximately on :numref:`wcst_import_workflow`.

.. _wcst_import_workflow:

.. figure:: media/geo-services-guide/wcst_import.png
   :align: center
   :scale: 40%

   Data importing process with ``wcst_import.sh``

An ingredients file showing all possible options (across all recipes) can be found `here
<http://rasdaman.org/browser/applications/wcst_import/ingredients/possible_ingredients.json>`__ 
in the `same directory <http://rasdaman.org/browser/applications/wcst_import/ingredients>`__
there are several examples of different recipes.

.. _data-import-recipes:

The following recipes are provided in the rasdaman repository:

* :ref:`Mosaic map <data-import-recipe-mosaic-map>`
* :ref:`Regular timeseries <data-import-recipe-regular-timeseries>`
* :ref:`Irregular timeseries <data-import-recipe-irregular-timeseries>`
* :ref:`General coverage <data-import-recipe-general>`
* :ref:`Import from external WCS <data-import-recipe-wcs_extract>`
* Specialized recipes

  * :ref:`Sentinel 1 <data-import-recipe-sentinel1>`
  * :ref:`Sentinel 2 <data-import-recipe-sentinel2>`

For each one of these there is an ingredients example under the
`ingredients/ <http://rasdaman.org/browser/applications/wcst_import/ingredients>`__
directory, together with an example for the available parameters
Further on each recipe type is described in turn, starting with the common
options shared by all recipes.

.. _data-import-common-options:

Common Options
--------------

Some options are commonly applicable to all recipes. We describe these options
for each top-level section of an ingredient file: config, input, recipe, and
hooks.

config section
^^^^^^^^^^^^^^

* ``service_url`` - The endpoint of the WCS service with the WCS-T extension enabled

  .. code-block::json

      "service_url": "http://localhost:8080/rasdaman/ows"

* ``mock`` - Print WCS-T requests but do not execute anything if set to ``true``.
  Set to ``false`` by default.

* ``automated`` - Set to ``true`` to avoid any interaction during the data import
  process. Useful in production environments for automated deployment for example.
  By default it is ``false``, i.e. user confirmation is needed to execute the
  actual import.

* ``blocking`` (since v9.8) - Set to ``false`` to analyze and import each file
  separately (*non-blocking mode*). By default blocking is set to ``true``,
  i.e. wcst_import will analyze all input files first to create corresponding
  coverage descriptions, and only then import them. The advantage of non-blocking
  mode is that the analyzing and importing happens incrementally
  (in blocking mode the analyzing step can take a long time, e.g. days,
  before the import can even begin).

  .. note::

      When importing in *non-blocking* import mode for coverages with
      irregular axes, it will *only rely on sorted files by filenames* and it
      can fail if these axes' coefficients are collected from input files'
      metadata (e.g: DateTime value in TIFF's tag or GRIB metadata) as they
      might not be consecutive. wcst_import will not analyze all files to
      collect metadata to be sorted by DateTime as in default *blocking*
      import mode.


* ``default_null_values`` - This parameter adds default null values for bands that
  do *not* have a null value provided by the file itself. The value for this
  parameter should be an array containing the desired null value either as a
  closed interval ``low:high`` or single values. Example:

  .. hidden-code-block:: json

      "default_null_values": [ 9.96921e+36, "9.96921e+35:*" ],

  .. NOTE::

     If set this parameter will override the null/nodata values present in
     the input files.

  .. NOTE::

     If a null value interval is specified, e.g. e.g ``"9.96921e+35:*"``,
     during encode it will not be preserved as-is because null value intervals 
     are not supported by most formats. In this case it is recommended to 
     first specify a non-interval null value, followed by the interval, e.g.
     ``[9.96921e+35, "9.96921e+35:*"]``.

* ``tmp_directory`` - Temporary directory in which gml and data files are created;
  should be readable and writable by rasdaman, petascope and current user. By
  default this is ``/tmp``.

* ``crs_resolver`` - The crs resolver to use for generating WCS-T request. By
  default it is determined from the ``petascope.properties`` setting.

* ``url_root`` - In case the files are exposed via a web-server and not locally,
  you can specify the root file url here; the default value is ``"file://"``.

* ``skip`` - Set to ``true`` to ignore files that failed to import; by default it
  is ``false``, i.e. the import process is terminated when a file fails to import.

* ``retry`` - Set to ``true`` to retry a failed request. The number of retries is
  either 5, or the value of setting ``retries`` if specified. This is set to
  ``false`` by default.

* ``retries`` - Control how many times to retry a failed WCS-T request; set to 5
  by default.

* ``retry_sleep`` - Set number of seconds to wait before retrying after an error;
  a floating-point number can also be specified for sub-second precision.
  Default values is 1.

* ``track_files`` - Set to ``true`` to allow input files to be tracked in a JSON file
  ``<coverage_id>.resume.json`` containing a list of imported file paths, 
  in order to avoid reimporting them when wcst_import.sh is subsequently executed again.
  The JSON file is generated in the directory set
  by the ``resumer_dir_path`` setting. This setting is enabled by default.
  Example content of a resume file ``S2_L2A_32633_B01.resume.json`` of a
  coverage ``S2_L2A_32633_B01``:

  .. hidden-code-block:: json

      ["/tmp/s2_l2A_32633_B01_1.tiff",
       "/tmp/s2_l2A_32633_B01_2.tiff",
       ...
       "/tmp/s2_l2A_32633_B01_10.tiff"]


* ``resumer_dir_path`` - The directory in which to store the resume file generated
  when ``track_files`` is set to ``true``. The user invoking 
  wcst_import.sh must have permissions to write in this  directory.
  By default the resume file will be stored in the same directory as the ingredients file.

* ``slice_restriction`` - Limit the slices that are imported to the ones that fit
  in a specified bounding box. Each subset in the bounding box should be of form
  ``{ "low": 0, "high": <max> }``, where low/high are given in the axis format.
  Example:

  .. hidden-code-block:: json

      "slice_restriction": [
        { "low": 0, "high": 36000 },
        { "low": 0, "high": 18000 },
        { "low": "2012-02-09", "high": "2012-12-09", "type": "date" }
      ]

* ``description_max_no_slices`` - Maximum number of slices (files) to show for
  preview before starting the actual data import.

* ``subset_correction`` (*deprecated* since v9.6) - In some cases the
  resolution is small enough to affect the precision of the transformation from
  domain coordinates to grid coordinates. To allow for corrections that will
  make the import possible, set this parameter to ``true``.

input section
^^^^^^^^^^^^^

* ``coverage_id`` - The name of the coverage to be created; if the coverage 
  already exists, it will be updated with the new files collected by ``paths``.

* ``paths`` - List of absolute or relative (to the ingredients file) paths or regex patterns that
  would work with the ls command. Multiple paths separated by commas
  can be specified. The collected paths are sorted by file name by default,
  unless specified otherwise in the recipe section (e.g. by date/time for 
  time-series recipes).


recipe section
^^^^^^^^^^^^^^

* ``import_order`` - Allow to sort the input files (``ascending`` (default)
  or ``descending``).Currently, it sorts by *datetime* which allows
  to import coverage from the first date or the recent date. Example:

  .. hidden-code-block:: json

      "import_order": "descending"

* ``tiling`` - Specifies the tile structure to be created for the coverage
  in rasdaman. You can set arbitrary tile sizes for the tiling option only
  if the tile name is ``ALIGNED``. Example:

  .. hidden-code-block:: json

      "tiling": "ALIGNED [0:0, 0:1023, 0:1023] TILE SIZE 5000000"

  For more information on tiling please check the :ref:`storage-layout`

.. _wms-import:

* ``wms_import`` - If set to ``true``, after importing data to coverage,
  it will also create a WMS layer from the imported coverage and populate
  metadata for this layer. After that, this layer will be available from
  *WMS GetCapabilties request*. Example:

  .. hidden-code-block:: json

      "wms_import": true

.. _scale-levels:

* ``scale_levels`` - Enable the :ref:`WMS pyramids <wms-image-pyramids>` feature.
  Level must be positive number and greater than 1 (note: only spatial geo axes,
  e.g. Lat and Long are scaled down in the pyramid member coverage).
  A new coverage as pyramid member of the importing coverage will be created with
  :ref:`this pattern <wms_scale_level_coverage_id_pattern>`.
  Syntax:

  .. hidden-code-block:: json

      "scale_levels": [ 1.5, 2, 4, ... ]

* ``scale_factors`` - Enable the :ref:`WMS pyramids <wms-image-pyramids>` feature.
  It is a more flexible variant of the ``scale_levels`` setting. The two settings are exclusive, 
  either ``scale_levels`` or ``scale_factors`` can exist in the ingredient file.
  The *coverage_id* of each factor must be unique in rasdaman and manually set by the user.
  The *factors* is a list of decimal values corresponding to the coverage axes according to
  its CRS order; a scale value for an irregular axis must be 1, while for a regular axis it 
  should be greater than 1; see more details :ref:`here <create_pyramid_member>`.
  For example, you can create two pyramid member 2D coverages which are 2x smaller
  (*cov_level_2*) and 4x smaller (*cov_level_4*) on the regular *Lat* and *Long* axes:
  

  .. hidden-code-block:: json

      "scale_factors": [
        {
          "coverage_id": "cov_level_2",
          "factors": [2, 2]
        },
        {
          "coverage_id": "cov_level_4",
          "factors": [4, 4]
        }
       ]


* ``import_overviews`` -  If specified with indices (0-based), wcst_import will import
  the corresponding overview levels defined in the input files
  as separated coverages with :ref:`this naming pattern <wms_scale_level_coverage_id_pattern>`.
  The selected overview coverages are then added as pyramid memberds to the base
  importing coverage. For example, to import overview levels 0 and 3 from a tiff
  file which has 4 overview levels in total

  .. hidden-code-block:: text

      gdalinfo 20100101.tif
      ...
      Band 1 Block=89x71 Type=Byte, ColorInterp=Gray
      Overviews: 45x36, 23x18, 12x9, 6x5

  you can specify ``"import_overviews": [0, 3]`` in the ingredients.
  
  By default this setting is set to an empty array, i.e. no overview levels will
  be imported. Only GDAL recipes and gdal version 2+ are supported.

* ``import_all_overviews`` - If specified with ``true``, all overview levels 
  which exist in the input files will be imported.
  For example, to import all 4 overview levels from a tiff file you can
  specify ``"import_all_overviews": true`` in the ingredient file.

  This setting and ``import_overviews`` are exclusive, only one can be specified.
  By default it is set to `false`. Only GDAL recipes and gdal version 2+ are supported.

* ``import_overviews_only`` - If specified with ``true``, input files are not imported
  to the *base* coverage specified with ``coverage_id``, but only to the *overview*
  coverages as specified in the ingredients file by either ``import_all_overviews`` or 
  ``import_overviews``. This setting is set to ``false`` by default if not specified explicitly.

  .. NOTE::

     If the input files were already imported to the *base* coverage and they were tracked
     in ``<base_coverage_id>.resume.json``, it is necessary to remove this resume file in 
     order to import only the overview coverages. Alternatively the ingredients file can
     be copied to another directory and adapted to set ``import_overviews_only`` to ``true``.

* ``pyramid_members`` - List of existing coverages which can be added
  as pyramid members of the importing coverage, see :ref:`request <add_pyramid_member>`.
  Syntax:

  .. hidden-code-block:: json

      "pyramid_members": [ "cov_level_2",  "cov_level_4"]

* ``pyramid_bases`` - List of existing coverages to which the importing coverage
  will be added as a pyramid member. This parameter has the opposite effect of `pyramid_members`,
  see :ref:`request <add_pyramid_member>`.
  Syntax:

  .. hidden-code-block:: json

      "pyramid_bases": [ "cov_A",  "cov_B"]

* ``pyramid_harvesting`` - If set to ``true``, recursively add all nested pyramid 
  members of the pyramid *member* coverage to the target *base* coverage. The
  pyramid member coverage depends on which of these two settings is used:

  - If ``pyramid_bases`` is specified, then the currently importing coverage is 
    the pyramid member of the the base coverages listed in ``pyramid_bases``;

  - Otherwise, if ``pyramid_members`` is specified, then the currently importing
    coverage is the base coverage of the pyramid member coverages listed in 
    ``pyramid_members``;

  - Otherwise, if neither of the above options is specified, an error is throws.

  See :ref:`request <add_pyramid_member>`for more details on the underlying
  request sent to petascope when this option is set to ``true``.
  By default this option is set to ``false``.


.. _wms-image-pyramids:

Image pyramids
~~~~~~~~~~~~~~

Since v9.7 it is possible to create downscaled versions of a given coverage,
eventually achieving something like an image pyramid, in order to enable
faster WMS requests when zooming in/out.

.. _wms_scale_level_coverage_id_pattern:

By using the :ref:`scale_levels <scale-levels>` option of wcst_import 
when importing a coverage with WMS enabled, petascope will create downscaled
collections in rasdaman following this pattern: ``coverageId_<level>``.
If level is a float, then *the dot* is replaced with an *underscore*,
as dots are not permitted in a collection name. Some examples:

- MyCoverage, level 2 -> MyCoverage_2
- MyCoverage, level 2.45 -> MyCoverage_2_45

Example ingredients specification to create two downscaled levels which are
*8x* and *32x* smaller than the original coverage:

.. hidden-code-block:: json

    "options": {
      "scale_levels": [8, 32],
      ...
    }

Two new WCS-T non-standard requests are utilized by wcst_import for this feature,
see :ref:`here for more information <wcs-t-non-standard-requests-wms>`.


hooks section
^^^^^^^^^^^^^

Since v9.8, it is possible to run shell commands *before/after data import*
by adding optional ``hooks`` configuration in an ingredient file.
There are 2 types of hooks:

* ``before_ingestion`` - Run shell commands before analyzing the input files,
  e.g. reproject input files from EPSG:3857 to EPSG:4326 with gdalwarp and 
  import the *reprojected* files only.

* ``after_ingestion`` - Run shell commands after importing the input files,
  e.g. clean all projected files from running gdalwarp above.

When import mode is set to non-blocking (``"blocking": false``), wcst_import
will run before/after hook(s) for the file which is being used to update
coverage, while the default blocking importing mode will run before/after
hooks for *all input files* before/after they are updated to a coverage.
Parameters are explained below.

.. hidden-code-block:: json

  "hooks": [
      {
        // Describe what this hook does
        "description": "reproject input files.",

        // Run bash command before importing file(s) to coverage
        "when": "before_ingestion",

        // Bash command which should be run for each input file
        "cmd": "gdalwarp -t_srs EPSG:4326 -tr 0.02 0.02 -overwrite \"${file:path}\" \"${file:path}.projected\"",

        // If set to true, when a bash command line returns any error, wcst_import
        // terminates immediately. Only valid for before_ingestion hooks.
        "abort_on_error": true,

        // wcst_import will consider the specified path(s) as the actual file(s)
        // to be imported after running the hook, rather than the original file.
        // This is an array of paths where globbing is allowed (same as the
        // "input":"paths" option. Only valid for before_ingestion hooks.
        "replace_path": ["${file:path}.projected"]
      },

      {
        // Describe what this hook does
        "description": "Remove projected files.",

        // Run bash command after importing file(s)
        "when": "after_ingestion",

        // Bash command which should be run after each imported file(s)
        "cmd": "rm -rf \"${file:path}.projected\""
      },

      // more hooks if needed
      ...
  ]


*Example: Import GDAL subdatasets*

The example ingredients below contains a pre-hook which replaces the collected
file path into a GDAL subdataset form; in this particular case, with the GDAL
driver for NetCDF a single variable from the collected NetCDF files is imported.

.. hidden-code-block:: json

  "hooks": [
      {
        "description": "Import one variable for netCDF with subdataset",
        "when": "before_ingestion",
        "cmd": "",
        "abort_on_error": true,
        // GDAL netCDF subdataset variable file path
        "replace_path": ["NETCDF:${file:path}:area"]
      }
   ]


.. _data-import-recipe-mosaic-map:

Recipe map_mosaic
-----------------

Well suited for importing a tiled map, not necessarily continuous; it
will place all input files given under a single coverage and deal with
their position in space. Parameters are explained below.

.. hidden-code-block:: json

    {
      "config": {
        // The endpoint of the WCS service with the WCS-T extension enabled
        "service_url": "http://localhost:8080/rasdaman/ows",
        // If set to true, it will print the WCS-T requests and will not
        // execute them. To actually execute them set it to false.
        "mock": true,
        // If set to true, the process will not require any user confirmation.
        // This is useful for production environments when deployment is automated.
        "automated": false
      },
      "input": {
        // The name of the coverage; if the coverage already exists,
        // it will be updated with the new files
        "coverage_id": "MyCoverage",
        // Absolute or relative (to the ingredients file) path or regex that
        // would work with the ls command. Multiple paths separated by commas
        // can be specified.
        "paths": [ "/var/data/*" ]
      },
      "recipe": {
        // The name of the recipe
        "name": "map_mosaic",
        "options": {
          // The tiling to be applied in rasdaman
          "tiling": "ALIGNED [0:511, 0:511]"
        }
      }
    }


.. _data-import-recipe-regular-timeseries:

Recipe time_series_regular
--------------------------

Well suited for importing multiple 2-D slices created at regular
intervals of time (e.g sensor data, satelite imagery etc) as 3-D cube
with the third axis being a temporal one. Parameters are explained below

.. hidden-code-block:: json

    {
      "config": {
        // The endpoint of the WCS service with the WCS-T extension enabled
        "service_url": "http://localhost:8080/rasdaman/ows",
        // If set to true, it will print the WCS-T requests and will not
        // execute them. To actually execute them set it to false.
        "mock": true,
        // If set to true, the process will not require any user confirmation.
        // This is useful for production environments when deployment is automated.
        "automated": false
      },
      "input": {
        // The name of the coverage; if the coverage already exists,
        // it will be updated with the new files
        "coverage_id": "MyCoverage",
        // Absolute or relative (to the ingredients file) path or regex that
        // would work with the ls command. Multiple paths separated by commas
        // can be specified.
        "paths": [ "/var/data/*" ]
      },
      "recipe": {
        // The name of the recipe
        "name": "time_series_regular",
        "options": {
          // Starting date for the first spatial slice
          "time_start": "2012-12-02T20:12:02",
          // Format of the time provided above: `auto` to try to guess it,
          // otherwise use any combination of YYYY:MM:DD HH:mm:ss
          "time_format": "auto",
          // Distance between each slice in time, granularity seconds to days
          "time_step": "2 days 10 minutes 3 seconds",

          // CRS to be used for the time axis
          "time_crs": "http://localhost:8080/def/crs/OGC/0/AnsiDate",
          // The tiling to be applied in rasdaman
          "tiling": "ALIGNED [0:1000, 0:1000, 0:2]"
        }
      }
    }


.. _data-import-recipe-irregular-timeseries:

Recipe time_series_irregular
----------------------------

Well suited for importing multiple 2-D slices created at irregular intervals of
time into a 3-D cube with the third axis being a temporal one. There are two
types of time parameters in "options", one needs to be choosed according to the
particular use case:

- ``tag_name`` - e.g. ``TIFFTAG_DATETIME`` in the image's metadata; the 
  metadata should be checked with ``gdalinfo <file>``, as not every image may
  have the tag. Below is an example:

  .. hidden-code-block:: json

    {
      "config": {
        // The endpoint of the WCS service with the WCS-T extension enabled
        "service_url": "http://localhost:8080/rasdaman/ows"
      },
      "input": {
        // The name of the coverage; if the coverage already exists,
        // it will be updated with the new files
        "coverage_id": "CoverageExampleTagName",
        // Absolute or relative (to the ingredients file) path or regex that
        // would work with the ls command. Multiple paths separated by commas
        // can be specified.
        "paths": [ "/home/rasdaman/images/tag_name/*.tif" ]
      },
      "recipe": {
        // The name of the recipe
        "name": "time_series_irregular",
        "options": {
          // Get the date for the slice from a tag that can be read by GDAL
          "time_parameter": {
            // The name of such a tag
            "metadata_tag": { "tag_name": "TIFFTAG_DATETIME" },
            // The format of the datetime value in the tag
            // Y = Year, e.g. to match TIFFTAG_DATETIME=2005
            "datetime_format": "YYYY"
          },
          // CRS to be used for the time axis
          "time_crs": "http://opengis.net/def/crs/OGC/0/AnsiDate",
          // The tiling to be applied in rasdaman
          "tiling": "ALIGNED [0:10, 0:1000, 0:500]"
        }
      }
    }

- ``filename`` allows an arbitrary pattern to extract the time information
  from the data file paths. Below is an example:

  .. hidden-code-block:: json

    {
      "config": {
        // The endpoint of the WCS service with the WCS-T extension enabled
        "service_url": "http://localhost:8080/rasdaman/ows"
      },
      "input": {
        // The name of the coverage; if the coverage already exists,
        // it will be updated with the new files
        "coverage_id": "CoverageExampleFilename",
        // Absolute or relative (to the ingredients file) path or regex that
        // would work with the ls command. Multiple paths separated by commas
        // can be specified.
        "paths": [ "/home/rasdaman/images/filename/*" ]
      },
      "recipe": {
        // The name of the recipe
        "name": "time_series_irregular",
        "options": {
          // Extract the date/time from the file name
          "time_parameter" :{
            "filename": {
              // The regex has to contain groups of tokens in parentheses
              "regex": "(.*)_(.*)_(.+?)_(.*)",
              // Which regex group to use for retrieving the time value
              "group": "2"
            },
          }
          // CRS to be used for the time axis
          "time_crs": "http://localhost:8080/def/crs/OGC/0/AnsiDate",
          // The tiling to be applied in rasdaman
          "tiling": "ALIGNED [0:2, 0:1000, 0:1000]"
        }
      }
    }


.. _data-import-recipe-general:

Recipe general_coverage
-----------------------

This is a highly flexible recipe that can handle any kind of data files (be it
2D, 3D or n-D) and model them in coverages of any dimensionality. It does that
by allowing users to define their own coverage models with any number of bands
and axes and fill the necesary coverage information through the so called
ingredient sentences inside the ingredients.


Coverage parameters
^^^^^^^^^^^^^^^^^^^

Using `ingredient sentences <data-import-ingredient-sentences>`__ we can define
any coverage model directly in the options of the ingredients file. Each
coverage model contains the following parts:

* ``crs`` - Indicates the crs of the coverage to be constructed. Either a CRS 
  url can be used e.g. http://opengis.net/def/crs/EPSG/0/4326 or the shorthand 
  notation ``CRS1@CRS2@CRS3``, e.g. ``OGC/0/AnsiDate@EPSG/0/4326`` for 
  indicating a time/date + spatial CRS.

* ``metadata`` - A group of options controlling metadata extraction and 
  consolidation; more detailed information follows :ref:`below 
  <data-import-recipe-general-coverage-metadata>`

* ``slicer`` - A group of options controlling the data decoding and placement
  into the overall datacube; more detailed information follows :ref:`below
  <data-import-recipe-general-coverage-slicer>`.

.. _data-import-recipe-general-coverage-metadata:

metadata section
~~~~~~~~~~~~~~~~

The ``metadata`` section specifies in which format you want the metadata (json
or xml). It can only contain characters and is limited in size by the backend
database limit for CLOB columns; for postgresql (the default backend for
petascope) the maximum size is 2GB (`source
<https://giswiki.hsr.ch/PostgreSQL_-_Binary_Large_Objects>`__).

* ``type`` - Specifies the format for storing the coverage metadata; ``xml`` and
  ``json`` are supported, and it is set to ``xml`` by default.

* ``global`` - Specifies fields which should be saved once for the whole
  coverage (e.g. the data licence, the creator etc). For example a "Title" 
  metadata value can be set with ``"global": { "Title": "'Drought code'", ... }``.

* ``local`` - Specifies fields which are fetched from each input file to be
  stored in coverage's metadata. When subsetting in the output coverage only
  *local* metadata associated to the subsetted areas will be added to the result.
  E.g., ``"local": { "LocalMetadataKey": "${netcdf:metadata:LOCAL_METADATA}" }``
  sets LocalMetadataKey to a metadata value extracted from the input data;
  the ``${..}`` is explained in :ref:`data-import-possible-expressions`. For a
  more detailed explanation of local metadata see the dedicated
  :ref:`local-metadata` section.

* ``colorPaletteTable`` - Controls collection of color palette table for the
  created coverage, which can then be used internally when encoding coverage to,
  e.g. PNG, to colorize the result. Currently only GDAL-style ``colorTable`` 
  with 256 color entries is supported. 

  A path to an explicit Color Palette Table file can be specified, see `example file
  <http://rasdaman.org/browser/systemtest/testcases_services/test_all_wcst_import/testdata/055-wcps_color_palette_rasql_ready_encoded_png/color_palette_table_rasql_READY.cpt>`__;
  such a file can be referenced in the ingredients file with, e.g.,
  ``"colorPaletteTable": "PATH/TO/table.cpt"``.

  If ``colorPaletteTable`` is set to ``"auto"`` or not specified at all, and
  the slicer is set to ``gdal`` (see next section for info on slicers), then
  the color table will be read automatically from the first input file if its
  metadata contains one.

  If ``colorPaletteTable`` is set to an empty string ``""``, any color table
  metadata will be ignored when creating coverage's global metadata.

* ``bands`` and ``axes`` - Allow specifying metadata for the coverage bands 
  and/or axes; more details can be found in :ref:`band-and-dim-metadata`.


.. _data-import-recipe-general-coverage-slicer:

slicer section
~~~~~~~~~~~~~~

The ``slicer`` subsection specifies the driver to use to read from the data files,
the required bands from data files and for each axis from the CRS how to obtain the 
bounds and resolution corresponding to each file.

* ``type`` - Specifies the decoding driver to be used; currently the following
  are supported:

  * ``gdal`` - for TIFF, PNG, and other encoding format that can be read with 
    GDAL (check with ``gdalinfo <file>``);

  * ``netcdf`` - for importing NetCDF data. If a netCDF file is flipped on Lat
    axis (South -> North coordinates  increase in the output of ``ncdump -c``)
    instead of GDAL style (North -> South coordinates decrease), then it is
    necessary to flip it before importing as rasdaman, e.g. with 
    ``cdo invertlat input.nc output.nc``.

  * ``grib`` - for GRIB data. Currently, rasdaman only supports GRIB files with
    ``gridType`` format of regular lat long ``regular_ll``. If the format is
    different, it is necessary to preprocess the input files into regular grid 
    type. The grid type can be retreived with 
    ``grib_dump file.grib | grep 'gridType'``.

    If a GRIB file is flipped on Lat axis (South -> North with 
    ``jScansPositively = 0`` in the output of ``grib_dump``) instead of GDAL 
    style (North -> South with ``jScansPositively = 1``), then it is necessary 
    to flip it before importing to rasdaman, e.g. with
    ``cdo invertlat input.grib output.grib``.

* ``pixelIsPoint`` - Only valid if ``type`` is ``netcdf`` or ``grib``.
  In some cases, by convention in the input files, the coordinates 
  are set in the middle of grid pixels, hence, set to ``true`` to extend the 
  lower and upper bounds of each regular axis by half grid pixel to be able to import.
  By default it is set to ``false``. 

* ``bands`` - A list of bands/chanels/variables from the input files which
  should be imported to the importing coverage. Each entry is a JSON object with
  the following options, of which ``identifier`` and ``name`` are mandatory to 
  specify while the rest are optional:

  * ``identifier`` - The name of the band in the input file;
  * ``name`` - The name of the band which will be used in the created coverage;
    this can be set to different from the ``indentifier``;
  * ``description`` - Metadata description of the band;
  * ``nilValue``` - Metadata null value of the band;
  * ``nilReason`` - Metadata reason for the null value of the band;
  * ``uomCode`` - Set the Unit of measurement (uom) code of the band. Besides 
    setting it directly, it can also be derived from the input file metadata,
    with e.g. ``${netcdf:variable:NAME:units}`` for NetCDF or
    ``${grib:unitsOfFirstFixedSurface}`` for GRIB.
  * Further ``"key": "value"`` entries can be specified to add customized band
    metadata to the global coverage metadata.

* ``axes`` - A JSON object which configures the properties of each axis of the
  created coverage with ``"axisLabel": { properties... }``. The possible 
  properties are listed below; generally, ``gridOrder``, ``min``, ``max``,
  and ``resolution`` have to be specified, except for irregular axes where
  ``resolution`` is not applicable.

  * ``gridOrder`` - The index of the axis in the input file (0-based);
  * ``crsOrder`` - Specifies a different name for this axis than the one 
    configured in the CRS's definition; more details can be found :ref:`here 
    <customized-axis-labels>`;
  * ``min`` - The lower bound of the axis (coordinates in the axis CRS);
  * ``max``- The upper bound of the axis (coordinates in the axis CRS);
  * ``resolution`` - The resolution of the axis from the input file;
    if this axis is irregular, the resolution is set to ``1``;
  * ``statements`` - Import python utility libraries (e.g. ``datetime`` / 
    ``timedelta``) to support calculating ``min``, ``max``, ``resolution``, etc;
    covered in more detail in a subsequent :ref:`section 
    <data-import-using-python-libraries>`;

  A few additional options are specific to *irregular axes*:

  * ``irregular`` - Set to ``true`` to specify that this axis is irregular, e.g.
    a time axis with irregular datetime indexes; if not specified, it is set to 
    ``false`` by default;
  * ``directPositions`` - A list of coefficients which are extracted or 
    calculated from the axis values specified in the input file; for an example 
    see this `ingredients file
    <http://rasdaman.org/browser/systemtest/testcases_services/test_all_wcst_import/testdata/089-wcs_aggregated_ansidate_netcdf/ingest.template.json#L46>`__;
  * ``dataBound`` - Set to ``false`` to specify that this axis should be 
    imported as a slicing point instead of a subset with lower and upper bounds;
    typical use case for this is when extracting irregular datetime values from 
    the input file names. When not specified it is set to ``true`` by default.
  * ``sliceGroupSize`` - Group multiple input slices into a single slice in the
    created coverage, e.g., multiple daily data files onto a single week index
    on the coverage time axis; explained in more detail :ref:`here <slice-group-size>`;

.. _data-import-recipe-general-coverage-examples:

Examples
~~~~~~~~

The examples below illustrate importing data in different formats with the
``general_coverage`` recipe; many more can be found in the rasdaman
`test suite <http://rasdaman.org/browser/systemtest/testcases_services/test_all_wcst_import/testdata/>`__.

* Commented example for importing GRIB data (only the ``recipe`` section is 
  shown for brevity):

  .. hidden-code-block:: json

    "recipe": {
      "name": "general_coverage",
      "options": {
        // Provide the coverage description and the method of building it
        "coverage": {
          // The coverage has 4 axes by combining 3 CRSes (Lat, Long, ansi, ensemble)
          "crs": "EPSG/0/4326@OGC/0/AnsiDate@OGC/0/Index1D?axis-label=\"ensemble\"",

          // specify metadata in json format
          "metadata": {
            "type": "json",

            "global": {
              // We will save the following fields from the input file
              // for the whole coverage
              "MarsType": "'${grib:marsType}'",
              "Experiment": "'${grib:experimentVersionNumber}'"
            },

            // or automatically import metadata, netcdf/gdal only (!)
            "global": "auto"

            "local": {
              // and the following field for each file that will compose the final coverage
              "level": "${grib:level}"
            }
          },

          // specify the "driver" for reading each file
          "slicer": {
            // Use the grib driver, which gives access to grib and file expressions.
            "type": "grib",
            // The pixels in grib are considered to be 0D in the middle of the cell,
            // as opposed to e.g. GeoTiff, which considers pixels to be intervals
            "pixelIsPoint": true,
            // Define the bands to create from the files (1 band in this case)
            "bands": [
              {
                "name": "temp2m",
                "definition": "The temperature at 2 meters.",
                "description": "We measure temperature at 2 meters using sensors and
                                then we process the values using a sophisticated algorithm.",
                "nilReason": "The nil value represents an error in the sensor."
                "uomCode": "${grib:unitsOfFirstFixedSurface}",
                "nilValue": "-99999"
              }
            ],
            "axes": {
              // For each axis specify how to extract the spatio-temporal position
              // of each file that is imported
              "Latitude": {
                // E.g. to determine at which Latitude the nth file will be positioned,
                // we will evaluate the given expression on the file
                "min": "${grib:latitudeOfLastGridPointInDegrees} +
                        (${grib:jDirectionIncrementInDegrees}
                         if bool(${grib:jScansPositively})
                         else -${grib:jDirectionIncrementInDegrees})",
                "max": "${grib:latitudeOfFirstGridPointInDegrees}",
                "resolution": "${grib:jDirectionIncrementInDegrees}
                               if bool(${grib:jScansPositively})
                               else -${grib:jDirectionIncrementInDegrees}",

                // This optional configuration is added since version 9.8.
                // The crs order specifies the order of the CRS axis in coverage
                // that will be created and allows to change standard abbreviation for axis label
                // from EPSG database to a different name (e.g: "Lat" -> "Latitude").
                "crsOrder": 0
                // The grid order specifies the order of the axis in the raster
                // that will be created
                "gridOrder": 3
              },
              "Long": {
                "min": "${grib:longitudeOfFirstGridPointInDegrees}",
                "max": "${grib:longitudeOfLastGridPointInDegrees} +
                         (-${grib:iDirectionIncrementInDegrees}
                          if bool(${grib:iScansNegatively})
                          else ${grib:iDirectionIncrementInDegrees})",
                "resolution": "-${grib:iDirectionIncrementInDegrees}
                               if bool(${grib:iScansNegatively})
                               else ${grib:iDirectionIncrementInDegrees}",
                "crsOrder": 1
                "gridOrder": 2
              },
              "ansi": {
                "min": "grib_datetime(${grib:dataDate}, ${grib:dataTime})",
                "resolution": "1.0 / 4.0",
                "type": "ansidate",
                "crsOrder": 2,
                "gridOrder": 1,
                // In case and axis does not natively belong to a file (e.g. as time),
                // then this property must set to false; by default it is true otherwise.
                "dataBound": false
              },
              "ensemble": {
                "min": "${grib:localDefinitionNumber}",
                "resolution": 1,
                "crsOrder": 3,
                "gridOrder": 0
              }
            }
          },

          "tiling": "REGULAR [0:0, 0:20, 0:1023, 0:1023]"
      }
    }

- Example for importing NetCDF data (full ingredients file `here 
  <http://rasdaman.org/browser/systemtest/testcases_services/test_all_wcst_import/testdata/072-wcps_irregular_time_nc/ingest.template.json>`__):

  .. hidden-code-block:: json

    "recipe": {
      "name": "general_coverage",
      "options": {
        "coverage": {
          "crs": "OGC/0/UnixTime@EPSG/0/3577",
          "metadata": {
            "type": "xml",
            "global": {
              "date_created": "'${netcdf:metadata:date_created}'",
              "Conventions": "'${netcdf:metadata:Conventions}'",
              "history": "\"${netcdf:metadata:history}\"",
              "title": "'${netcdf:metadata:title}'",
              "summary": "'${netcdf:metadata:summary}'",
              "product_version": "'${netcdf:metadata:product_version}'",
              "test_empty_attribute": "",
              "source": "'${netcdf:metadata:source}'"
            },
            "bands": {
              "band_1": {
                "product_version": "'${netcdf:metadata:product_version}'",
                "test_empty_attribute": ""
              },
              "band_7": {
                "date_created": "'${netcdf:metadata:date_created}'",
                "Conventions": "'${netcdf:metadata:Conventions}'"
              }
            },
            "axes": {
              "unix": {
                "min": "${netcdf:variable:unix:min}",
                "max": "${netcdf:variable:unix:max}",
                "directPositions": "${netcdf:variable:E:min}"
              }
            }
          },
          "slicer": {
            "type": "netcdf",
            "pixelIsPoint": true,
            "bands": [
              {
                "name": "band_1",
                "description": "Nadir BRDF Adjusted Reflectance 0.43-0.45 microns (Coastal Aerosol)",
                "identifier": "band_1",
                "nilValue": "-999"
              },
              {
                "name": "band_2",
                "identifier": "band_2",
                "nilValue": "-999"
              },
              {
                "name": "band_3",
                "identifier": "band_3",
                "nilValue": "-999"
              },
              {
                "name": "band_4",
                "identifier": "band_4",
                "nilValue": "-999"
              },
              {
                "name": "band_5",
                "identifier": "band_5",
                "nilValue": "-999"
              },
              {
                "name": "band_6",
                "identifier": "band_6",
                "nilValue": "-999"
              },
              {
                "name": "band_7",
                "identifier": "band_7",
                "nilValue": "-999"
              }
            ],
            "axes": {
              "unix": {
                "min": "${netcdf:variable:unix:min}",
                "max": "${netcdf:variable:unix:max}",
                "directPositions": "${netcdf:variable:unix}",
                "gridOrder": 0,
                "irregular": true
              },
              "E": {
                "min": "${netcdf:variable:E:min}",
                "max": "${netcdf:variable:E:max}",
                "gridOrder": 2,
                "resolution": 25
              },
              "N": {
                "min": "${netcdf:variable:N:min}",
                "max": "${netcdf:variable:N:max}",
                "gridOrder": 1,
                "resolution": -25
              }
            }
          }
        },
        "tiling": "ALIGNED [0:13, 0:999, 0:999] TILE SIZE 4000000"
      }
    }

* Example for importing TIFF data with the ``gdal`` driver 
  (full ingredients file `here 
  <http://rasdaman.org/browser/systemtest/testcases_services/test_all_wcst_import/testdata/wcs_slice_group_size_7days/ingest.template.json>`__):

  .. hidden-code-block:: json

    "recipe": {
      "name": "general_coverage",
      "options": {
        "import_order": "ascending",
        "coverage": {
          "crs": "OGC/0/AnsiDate@EPSG/0/4326",
          "metadata": {
            "type": "xml",
            "global": {
              "Title": "'This is a test coverage'"
            }
          },
          "slicer": {
            "type": "gdal",
            "bands": [
              {
                "name": "Gray",
                "identifier": "0"
              }
            ],
            "axes": {
              "MyTimeAxis": {
                "min": "datetime(regex_extract('${file:name}', '(.*)\\.(.*)',1), 'YYYYMMDD')",
                "crsOrder": 0,
                "gridOrder": 0,
                "type": "ansidate",
                "irregular": true,
                "sliceGroupSize": 7,
                "dataBound": false
              },
              "long": {
                "min": "${gdal:minX}",
                "max": "${gdal:maxX}",
                "crsOrder": 2,
                "gridOrder": 1,
                "resolution": "${gdal:resolutionX}"
              },
              "lat": {
                "min": "${gdal:minY}",
                "max": "${gdal:maxY}",
                "crsOrder": 1,
                "gridOrder": 2,
                "resolution": "${gdal:resolutionY}"
              }
            }
          }
        },
        "tiling": "ALIGNED [0:0, 0:1023, 0:1023]"
      }
    }


.. _data-import-ingredient-sentences:

Ingredient sentences
^^^^^^^^^^^^^^^^^^^^

An *ingredient sentence* can be of multiple types:

- *Numeric* - e.g. ``2``, ``4.5``

- *Strings* - e.g. ``'Some information'``

- *Functions* - e.g. ``datetime('2012-01-01', 'YYYY-mm-dd')``

- *Data expressions* - Allow to collect information from the data file being
  imported with a specific format driver. An expression is of form
  ``${driverName:driverOperation}`` - e.g. ``${gdal:minX}`` or
  ``${netcdf:variable:time:min``. All possible expressions are documented in
  :ref:`data-import-possible-expressions`.

- *Python expressions* - The types above can be combined into any valid Python
  expression; this allows to do mathematical operations, string parsing, 
  date/time manipulation, etc. E.g. ``${gdal:minX} + 1/2 * ${gdal:resolutionX}``
  or ``datetime(${netcdf:variable:time:min} * 24 * 3600)``. Expressions can
  use functions from any Python library which just needs to be explicitly
  imported as explained in :ref:`data-import-using-python-libraries`.


.. _data-import-possible-expressions:

Data expressions
^^^^^^^^^^^^^^^^

Each driver allows expressions to extract information from input files.
We will mark with capital letters things that vary in the expression.
E.g. ``${gdal:metadata:FIELD}`` means that you can replace
``FIELD`` with any valid gdal metadata tag such as ``TIFFTAG_DATETIME``.
Example ingredients where data expressions are used can be found in
:ref:`data-import-recipe-general-coverage-examples`.

NetCDF
~~~~~~

+-----------+-----------------------------------------------------+-------------------------------+
|  **Type** |                **Description**                      |        **Examples**           |
+===========+=====================================================+===============================+
|Metadata   |                                                     |                               |
|information|``${netcdf:metadata:YOUR_METADATA_FIELD}``           |``${netcdf:metadata:title}``   |
+-----------+-----------------------------------------------------+-------------------------------+
|Variable   |``${netcdf:variable:VAR_NAME:MODIFIER}``             |``${netcdf:variable:t:min}``   |
|information|where ``VAR_NAME`` can be any variable in the        |``${netcdf:variable:t:units}`` |
|           |file and ``MODIFIER`` can be one of:                 |                               |
|           |first|last|max|min; Any extra modifiers will return  |                               |
|           |the corresponding metadata field on the given        |                               |
|           |variable                                             |                               |
+-----------+-----------------------------------------------------+-------------------------------+
|Dimension  |``${netcdf:dimension:DIM_NAME}``                     |``${netcdf:dimension:time}``   |
|information|where ``DIM_NAME`` can be any dimension in the       |                               |
|           |file. This will return the value on the selected     |                               |
|           |dimension.                                           |                               |
+-----------+-----------------------------------------------------+-------------------------------+

GDAL
~~~~

Relevant for TIFF, PNG, JPEG, and other 2D data formats.

+-----------+-----------------------------------------------------+-----------------------------+
|  **Type** |                **Description**                      |        **Examples**         |
+===========+=====================================================+=============================+
|Metadata   |                                                     |                             |
|information|``${gdal:metadata:METADATA_FIELD}``                  |``${gdal:metadata:TIFFTAG}`` |
+-----------+-----------------------------------------------------+-----------------------------+
|Geo Bounds |``${gdal:BOUND_NAME}`` where ``BOUND_NAME`` can be   |``${gdal:minX}``             |
|           |one of the minX|maxX|minY|maxY                       |                             |
+-----------+-----------------------------------------------------+-----------------------------+
|Geo        |``${gdal:RESOLUTION_NAME}`` where ``RESOLUTION_NAME``|``${gdal:resolutionX}``      |
|Resolution |can be one of the resolutionX|resolutionY            |                             |
+-----------+-----------------------------------------------------+-----------------------------+
|Origin     |``${gdal:ORIGIN_NAME}`` where ``ORIGIN_NAME`` can be |``${gdal:originY}``          |
|           |one of the originX|originY                           |                             |
+-----------+-----------------------------------------------------+-----------------------------+

GRIB
~~~~

+-----------+------------------------------------------------+------------------------------------------+
|  **Type** |                **Description**                 |               **Examples**               |
+===========+================================================+==========================================+
|GRIB Key   |``${grib:KEY}`` where ``KEY`` can be any of the |``${grib:experimentVersionNumber}``       |
|           |keys contained in the GRIB file                 |                                          |
|           |``${grib:messagenumber}`` is the special value  |                                          |
|           |to get the current processed GRIB message index |                                          |
|           |(starting from 1)                               |                                          |
+-----------+------------------------------------------------+------------------------------------------+

File
~~~~

+-----------------+--------------------------------------------------------------------+-----------------------------+
|  **Type**       |                **Description**                                     |  **Examples**               |
+=================+====================================================================+=============================+
|File Information |``${file:PROPERTY}`` where property can be one of                   |                             |
|                 |path|name|dir_path|original_path|original_dir_path                  |``${file:path}``             |
|                 |original_* allows to get the original input file's path/directory.  |                             |
|                 |Used only in ``before_ingestion`` hooks with ``replace_path``       |                             |
|                 |to replace original input file paths with customized file paths.    |                             |
+-----------------+--------------------------------------------------------------------+-----------------------------+
|Imported File    |``${imported_file:PROPERTY}`` where property can be one of          |                             |
|Information      |path|name|dir_path|original_path|original_dir_path                  |                             |
|                 |Files which were imported to rasdaman (excluding *skipped files*).  |``${imported_file:path}``    |
|                 |This variable is used only in ``after_ingestion`` hooks.            |                             |
+-----------------+--------------------------------------------------------------------+-----------------------------+

.. _data-import-expressions-special-functions:

Special functions
~~~~~~~~~~~~~~~~~

A couple of special functions are available to help with more complicated
expressions:

+----------------------------------+-------------------------------------------------+--------------------------------------------+
| **Function and Arguments**       |             **Description**                     |             **Examples**                   |
+==================================+=================================================+============================================+
|                                  |                                                 |::                                          |
|``grib_datetime``                 |                                                 |                                            |
|                                  |This function helps to deal with the usual grib  |  grib_datetime(${grib:dataDate},           |
|- date                            |date and time format. It returns back a datetime |                ${grib:dataTime})           |
|- time                            |string in ISO format.                            |                                            |
+----------------------------------+-------------------------------------------------+--------------------------------------------+
|                                  |                                                 |::                                          |
|``datetime``                      |                                                 |                                            |
|                                  |This function helps to deal with strange date    |  datetime("20120101:1200",                 |
|- date                            |time formats. It returns back a datetime string  |           "YYYYMMDD:HHmm")                 |
|- format                          |in ISO format.                                   |                                            |
+----------------------------------+-------------------------------------------------+--------------------------------------------+
|                                  |                                                 |::                                          |
|``regex_extract``                 |                                                 |                                            |
|                                  |This function extracts information from a string | datetime(                                  |
|- string                          |using regex; input is the string you parse, regex|   regex_extract('${file:name}',            |
|- regex                           |is the regular expression, group is the regex    |     '(.*)_(\\d*-\\d\\d)(.*)', 2),          |
|- group                           |group you want to select                         |   'YYYY-MM')                               |
+----------------------------------+-------------------------------------------------+--------------------------------------------+
|``replace``                       |                                                 |::                                          |
|                                  |                                                 |                                            |
|- str                             |Replaces all occurrences of a substring with     | replace('${file:path}',                    |
|- old                             |another substring in the input string            |         '.tiff', '.xml')                   |
|- new                             |                                                 |                                            |
+----------------------------------+-------------------------------------------------+--------------------------------------------+

.. _data-import-using-python-libraries:

Using libraries in sentences
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

In case the ingredient sentences require functionality from extra Python
libraries, they can be imported with a ``statements`` option.  For example, to
calculate the lower bound and upper bound for the time axis ``ansi`` (starting
days from ``1978-12-31T12:00:00``) one could use ``datetime`` and ``timedelta``
from the ``datatime`` library.

.. hidden-code-block:: json

              "ansi": {
                "statements": "from datetime import datetime, timedelta",

                "min": "(datetime(1978,12,31,12,0,0) + timedelta(days=${netcdf:variable:time:min})).strftime(\"%Y-%m-%dT%H:%M\")",
                "max": "(datetime(1978,12,31,12,0,0) + timedelta(days=${netcdf:variable:time:max})).strftime(\"%Y-%m-%dT%H:%M\")",
                "directPositions": "[(datetime(1978,12,31,12,0,0) + timedelta(days=x)).strftime(\"%Y-%m-%dT%H:%M\") for x in ${netcdf:variable:time}]",
                "irregular": true,
  	            "resolution": "1",
                "gridOrder": 0,
                "crsOrder": 0,
                "type": "ansidate"
              },


Python functions imported in this way override the :ref:`special functions
<data-import-expressions-special-functions>` provided by wcst_import. For
example, the special utility function ``datetime(date_time_string, format)`` to
convert a string of datetime to an ISO date time format will be overridden when
the ``datetime`` module is imported with a ``statements`` setting.


.. _local-metadata:

Local metadata from input files
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Beside the *global metadata* of a coverage, you can add *local metadata*
for each file which is a part of the whole coverage (e.g. a 3D time-series
coverage mosaiced from 2D GeoTiff files).

Under the metadata section add a "local" object with keys and values extracted
by using format type expression. Example of extracting an attribute from a
netCDF input file:

.. hidden-code-block:: json

    "metadata": {
      "type": "xml",
      "global": {
        ...
      },
      "local": {
        "LocalMetadataKey": "${netcdf:metadata:LOCAL_METADATA}"
      }
    }

Each file's envelope (geo domain) and its local metadata will be added to the
coverage metadata under ``<slice>...</slice>`` element if coverage metadata is
imported in XML format. Example of a coverage containing local metadata in XML
from 2 netCDF files:

.. hidden-code-block:: xml

    <slices>

      <!--- Begin Local Metadata from netCDF file 1 -->
      <slice>
        <boundedBy>
          <Envelope>
            <axisLabels>Lat Long ansi forecast</axisLabels>
            <srsDimension>4</srsDimension>
            <lowerCorner>34.4396675 29.6015625
                         "2017-01-10T00:00:00+00:00" 0</lowerCorner>
            <upperCorner>34.7208095 29.8828125
                         "2017-01-10T00:00:00+00:00" 0</upperCorner>
          </Envelope>
        </boundedBy>
        <LocalMetadataKey>FROM FILE 1</LocalMetadataKey>
        <fileReferenceHistory>
        /tmp/wcs_local_metadata_netcdf_in_xml/20170110_0_ecfire_fwi_dc.nc
        </fileReferenceHistory>
      </slice>
      <!--- End Local Metadata from netCDF file 1 -->

      <!--- Begin Local Metadata from netCDF file 2 -->
      <slice>
        <boundedBy>
          <Envelope>
            <axisLabels>Lat Long ansi forecast</axisLabels>
            <srsDimension>4</srsDimension>
            <lowerCorner>34.4396675 29.6015625
                         "2017-02-10T00:00:00+00:00" 3</lowerCorner>
            <upperCorner>34.7208095 29.8828125
                         "2017-02-10T00:00:00+00:00" 3</upperCorner>
          </Envelope>
        </boundedBy>
        <LocalMetadataKey>FROM FILE 2</LocalMetadataKey>
        <fileReferenceHistory>
        /tmp/wcs_local_metadata_netcdf_in_xml/20170210_3_ecfire_fwi_dc.nc
        </fileReferenceHistory>
      </slice>
      <!--- End Local Metadata from netCDF file 2 -->

    </slices>

Since v10.0, local metadata for input files can be also fetched from
corresponding external text files with the optional ``metadata_file`` option.
For example:

.. hidden-code-block:: json

     "local": {
        "local_metadata_key": "${gdal:metadata:local_metadata_key}",
        "metadata_file": {
           // The metadata from the external XML file will be created
           // as a child element of this root element
           "root_element": "INSPIRE",
           // Path to the external XML file corresponding to
           // the importing input file
           "path": "replace('${file:path}', '.tiff', '.xml')"
        }
      }

When subsetting a coverage which contains a local metadata section from input
files (via WC(P)S requests), if the geo domains of subsetted coverage intersect
with some input files' envelopes, only local metadata of these files will be
added to the output coverage metadata.

For example: a ``GetCoverage`` request with a trim such that
crs axis subsets are within netCDF file 1:

.. hidden-code-block:: text

   http://localhost:8080/rasdaman/ows?service=WCS&version=2.0.1
          &request=GetCoverage
          &subset=ansi("2017-01-10T00:00:00+00:00")
          &subset=Lat(34.4396675,34.4396675)
          &subset=Long(29.6015625,29.6015625)
          &subset=forecast(0)

The coverage's metadata result will contain local metadata *only* from netCDF
file 1:

.. hidden-code-block:: xml

   <slices>
      <!--- Begin Local Metadata from netCDF file 1 -->
      <slice>
        <boundedBy>
          <Envelope>
            <axisLabels>Lat Long ansi forecast</axisLabels>
            <srsDimension>4</srsDimension>
            <lowerCorner>34.4396675 29.6015625
                         "2017-01-10T00:00:00+00:00" 0</lowerCorner>
            <upperCorner>34.7208095 29.8828125
                         "2017-01-10T00:00:00+00:00" 0</upperCorner>
          </Envelope>
        </boundedBy>
        <LocalMetadataKey>FROM FILE 1</LocalMetadataKey>
        <fileReferenceHistory>
        /tmp/wcs_local_metadata_netcdf_in_xml/20170110_0_ecfire_fwi_dc.nc
        </fileReferenceHistory>
      </slice>
      <!--- End Local Metadata from netCDF file 1 -->
   <slices>


.. _customized-axis-labels:

Customized axis labels
^^^^^^^^^^^^^^^^^^^^^^

By default, the axes to be configured must be matched by their name as defined
by the coverage CRS. For example, a CRS ``OGC/0/AnsiDate@EPSG:4326`` defines three
axes with labels ansi, Long, and Lat. To configure them, we would have a
section as bellow:

.. hidden-code-block:: json

  "axes": {
    "AnsiDate": { ... },
    "Long":     { ... },
    "Lat":      { ... }
  }

Since v9.8, one can change the default axis label defined by the CRS through
indicating the axis index in the CRS (0-based) with the ``"crsOrder"`` setting.
For example, to change the axis labels to MyDateTimeAxis, MyLatAxis, and 
MyLongAxis:

.. hidden-code-block:: json

  "axes": {
    "MyDateTimeAxis": {
      // Match ansi axis in AnsiDate CRS
      "crsOrder": 0,
      ...
    },
    "MyLongAxis": {
      // Match Long axis in EPSG:4326
      "crsOder": 2,
      ...
    },
    "MyLatAxis": {
      // Match Lat axis in EPSG:4326
      "crsOder": 1,
      ...
    }
  }


.. _slice-group-size:

Group coverage slices
^^^^^^^^^^^^^^^^^^^^^

Since v9.8, wcst_import allows to group input files on irregular axes (with
``"dataBound": false``) through the ``sliceGroupSize`` option, which would 
specify the group size as a positive number. E.g:

.. hidden-code-block:: json

    "time": {
        "min": "datetime(regex_extract('${file:name}', '(.*)\\.(.*)',1), 'YYYYMMDD')",
        "gridOrder": 0,
        "type": "ansidate",
        "irregular": true,
        "sliceGroupSize": 7,
        "dataBound": false
    }

If each input slice corresponds to index *X*, and one wants to have slice
groups of size *N*, then the index would be translated with this option to
``X - (X % N)``.

Typical use case is importing 3D coverage from 2D satellite imagery where the
time axis is irregular and its values are fetched from input files by regex
expression. Then, all input files which belong to the same time window (e.g 7
days in AnsiDate CRS with ``"sliceGroupSize": 7``) will have the same value,
which is the first date of the week.


.. _band-and-dim-metadata:

Band and axis metadata in global metadata
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Metadata can be individually specified for each *band* and *axis* in the
ingredient file. Example:

.. hidden-code-block:: json

    "metadata": {
      "type": "xml",
      "global": {
        "description": "'3-band data.'",
        "resolution": "'1'"
      },
      "bands": {
        "red": {
          "metadata1": "metadata_red1",
          "metadata2": "metadata_red2"
        },
        "green": {
          "metadata3": "metadata_green3",
          "metadata4": "metadata_green4"
        },
        "blue": {
          "metadata5": "metadata_blue5"
        }
      },
      "axes": {
        "i": {
          "metadata_i_1": "metadata_1",
          "metadata_i_2": "metadata_2"
        },
        "j": {
          "metadata_j_1": "metadata_3"
        }
      }
    }

Since v9.7, the following metadata can also be automatically derived from the
input netCDF files.

band metadata
~~~~~~~~~~~~~

* For netCDF: If ``"bands"`` is set to ``"auto"`` or does not exist under ``"metadata"``
  in the ingredient file, all user-specified bands will have metadata which is
  fetched directly from the netCDF file. Metadata for 1 band is
  **collected automatically** if: 1) band is not added. 2) band is set to ``"auto"``.

* Otherwise, the user could specify metadata explicitly by a dictionary of keys/values.
  Example:

  .. hidden-code-block:: json

      "metadata": {
        "type": "xml",
        "global": {
          "description": "'3-band data.'",
          "resolution": "'1'"
        },
        "bands": {
          "red": {
            "metadata1": "metadata_red1",
            "metadata2": "metadata_red2"
          },
          "green": {
            "metadata3": "metadata_green3",
            "metadata4": "metadata_green4"
          }
        }
      }


axis metadata
~~~~~~~~~~~~~

* For netCDF: If ``"axes"`` is set to ``"auto"`` or does not exist under ``"metadata"``
  in the ingredient file, all user-specified axes will have metadata which is
  fetched directly from the netCDF file. Metadata for 1 axis is 
  **collected automatically** if: 1) axis is not added. 2) axis is set
  to ``"auto"``. 3) axis is set with ``${netcdf:variable:Name:metadata}``.
  The axis label for variable is detected from the ``min`` or ``max`` value
  of CRS axis configuration under ``"slicer/axes"`` section. For example:

  .. hidden-code-block:: json

      "slicer": {
         ...
         "axes": {
            "Long": {
               # 'lon' is variable name in netCDF file for CRS axis 'Long'.
               "min": "${netcdf:variable:lon:min}"
                ...
             }
          }
       }

* Otherwise, the user could specify metadata explicitly as a dictionary of 
  keys/values.

  .. hidden-code-block:: json

      "metadata": {
        "type": "xml",
        "global": {
          "description": "'3-band data.'",
          "resolution": "'1'"
        },
        "axes": {
          "i": {
            "metadata_i_1": "metadata_1",
            "metadata_i_2": "metadata_2"
          },
          "j": {
            "metadata_j_1": "metadata_3"
          }
        }
      }


.. _data-import-recipe-wcs_extract:

Recipe wcs_extract
------------------

Allows to import a coverage from a remote petascope endpoint into the local
petascope. Parameters are explained below.

.. hidden-code-block:: json

    {
      "config": {
        "service_url": "http://localhost:8080/rasdaman/ows",
        "default_crs": "http://localhost:8080/def/crs/EPSG/0/4326",
        "automated": true
      },
      "input": {
        "coverage_id": "test_wcs_extract"
      },
      "recipe": {
        // name of recipe
        "name": "wcs_extract",
        "options": {
          // remote coverage id in remote petascope
          "coverage_id": "test_time3d",
          // remote petascope endpoint
          "wcs_endpoint" : "http://localhost:8080/rasdaman/ows",
          // the partitioning scheme as a list of the maximum number of pixels on each
          // axis dimension e.g. [500, 500, 1] will split the 3-D coverage in 2-D slices
          // of 500 by 500.
          "partitioning_scheme" : [0, 0, 500],
          // The tiling to be applied in rasdaman
          "tiling": "ALIGNED [0:2000, 0:2000]"
        }
      }
    }


.. _data-import-recipe-sentinel1:

Recipe sentinel1
----------------

This is a convenience recipe for importing Sentinel 1 data in particular;
currently only GRD/SLC product types are supported, and only geo-referenced
tiff files. Below is an example:

.. hidden-code-block:: json

    {
      "config": {
        "service_url": "http://localhost:8080/rasdaman/ows",
        "automated": true,
        "track_files": false
      },
      "input": {
        "coverage_id": "S1_GRD_${modebeam}_${polarisation}",

         // (e.g: a geo-referenced tiff file to CRS: EPSG:4326, mode beam IW,
         //  singler polarisation VH:
         // s1a-iw-grd-vh-20190226t171654-20190326t171719-026512-02f856-002.tiff)
        "paths": [ "*.tiff" ],

        // If not specified, default product is "GRD"
        "product": "SLC"

        "modebeams": ["EW", "IW"],
        "polarisations": ["HH", "HV", "VV", "VH"]
      },
      "recipe": {
        "name": "sentinel1",
        "options": {
          "coverage": {
            "metadata": {
              "type": "xml",
              "global": {
                "Title": "'Sentinel-1 GRD data served by rasdaman'"
              }
            }
          },
          "tiling": "ALIGNED [0:0, 0:1999, 0:1999] TILE SIZE 32000000",
          "wms_import": true
        }
      }
    }

The recipe extends :ref:`general_coverage <data-import-recipe-general>` so
the ``"recipe"`` section has the same structure. However, a lot of information
is automatically filled in by the recipe now, so the ingredients file is much
simpler as the example above shows.

The other obvious difference is that the ``"coverage_id"`` is templated with
several variables enclosed in ``${`` and ``}`` which are automatically replaced
to generate the actual coverage name during import:

- ``modebeam`` - the mode beam of input files, e.g. ``IW/EW``.

- ``polarisation`` - single polarisation of input files, e.g: ``HH/HV/VV/VH``

If the files collected by ``"paths"`` are varying in any of these parameters,
the corresponding variables must appear somewhere in the ``"coverage_id"``
(as for each combination a separate coverage will be constructed). Otherwise,
the data import will either fail or result in invalid coverages. E.g. if all
data's mode beam is  ``IW``, but still different polarisations, the
``"coverage_id"`` could be ``"MyCoverage_${polarisation}"``;

In addition, the data to be imported can be optionally filtered with the
following options in the ``"input"`` section:

- ``modebeams`` - specify a subset of mode beams to import from the data,
  e.g. only the ``IW`` mode beam; if not specified, data of all supported
  mode beams will be ingested.

- ``polarisations`` - specify a subset of polarisations to import,
  e.g. only the ``HH`` polarisation; if not specified, data of all supported
  polarisations will be imported.

**Limitations:**

- Only GRD/SLC products are supported.
- Data must be geo-referenced.
- Filenames are assumed to be of the format:
  ``s1[ab]-(.*?)-grd(.?)-(.*?)-(.*?)-(.*?)-(.*?)-(.*?)-(.*?).tiff`` or
  ``s1[ab]-(.*?)-slc(.?)-(.*?)-(.*?)-(.*?)-(.*?)-(.*?)-(.*?).tiff``.

.. _data-import-recipe-sentinel2:

Recipe sentinel2
----------------

This is a convenience recipe for importing Sentinel 2 data in particular. It
relies on support for Sentinel 2 in `more recent GDAL versions
<https://gdal.org/frmt_sentinel2.html>`__. Importing zipped Sentinel 2 is also
possible and automatically handled.

Below is an example:

.. hidden-code-block:: json

    {
      "config": {
        "service_url": "http://localhost:8080/rasdaman/ows",
        "automated": true
      },
      "input": {
        "coverage_id": "S2_${crsCode}_${resolution}_${level}",
        "paths": [ "S2*.zip" ],
        // Optional filtering settings
        "resolutions": ["10m", "20m", "60m", "TCI"],
        "levels": ["L1C", "L2A"],
        "crss": ["32757"] // remove or leave empty to import any CRS
      },
      "recipe": {
        "name": "sentinel2",
        "options": {
          "coverage": {
            "metadata": {
              "type": "xml",
              "global": {
                "Title": "'Sentinel-2 data served by rasdaman'"
              }
            }
          },
          "tiling": "ALIGNED [0:0, 0:1999, 0:1999] TILE SIZE 32000000",
          "wms_import": true
        }
      }
    }

The recipe extends :ref:`general_coverage <data-import-recipe-general>` so
the ``"recipe"`` section has the same structure. However, a lot of information
is automatically filled in by the recipe now, so the ingredients file is much
simpler as the example above shows.

The other obvious difference is that the ``"coverage_id"`` is templated with
several variables enclosed in ``${`` and ``}`` which are automatically replaced
to generate the actual coverage name during import:

- ``crsCode`` - the CRS EPSG code of the imported files, e.g. ``32757`` for
  WGS 84 / UTM zone 57S.

- ``resolution`` - Sentinel 2 products bundle several subdatasets of different
  resolutions:

  - ``10m`` - bands B4, B3, B2, and B8 (base type unsigned short)

  - ``20m`` - bands B5, B6, B7, B8A, B11, and B12 (base type unsigned short)

  - ``60m`` - bands B1, B8, and B10 (base type unsigned short)

  - ``TCI`` - True Color Image (red, green, blue char bands); also 10m as it is
    derived from the B2, B3, and B4 10m bands.

- ``level`` - ``L1C`` or ``L2A``

If the files collected by ``"paths"`` are varying in any of these parameters,
the corresponding variables must appear somewhere in the ``"coverage_id"`` (as
for each combination a separate coverage will be constructed). Otherwise, the
import will either fail or result in invalid coverages. E.g. if all data is
level ``L1C`` with CRS ``32757``, but still different resolutions, the
``"coverage_id"`` could be ``"MyCoverage_${resolution}"``; the other variables
can still be specified though, so  ``"MyCoverage_${resolution}_${crsCode}"`` is
valid as well.

In addition, the data to be imported can be optionally filtered with the
following options in the ``"input"`` section:

- ``resolutions`` - specify a subset of resolutions to import from the data,
  e.g. only the "10m" subdataset; if not specified, data of all supported
  resolutions will be ingested.

- ``levels`` - specify a subset of levels to import, so that files of other
  levels will be fully skipped; if not specified, data of all supported levels
  will be ingested.

- ``crss`` - specify a list of CRSs (EPSG codes as strings) to import; if not
  specified or empty, data of any CRS will be imported.


.. _data-import-recipe-create-own:

Creating your own recipe
------------------------

The recipes above cover a frequent but limited subset of what is possible to
model using a coverage. WCSTImport allows to define your own recipes in
order to fill these gaps. In this tutorial we will create a recipe that can
construct a 3D coverage from 2D georeferenced files. The 2D files that we want
to target have all the same CRS and cover the same geographic area. The time
information that we want to retrieve is stored in each file in a GDAL readable
tag. The tag name and time format differ from dataset to dataset so we want to
take this information as an option to the recipe. We would also want to be
flexible with the time crs that we require so we will add this option as well.

Based on this usecase, the following ingredient file seems to fulfill our need:

.. hidden-code-block:: json

    {
      "config": {
        "service_url": "http://localhost:8080/rasdaman/ows",
        "mock": false,
        "automated": false
      },
      "input": {
        "coverage_id": "MyCoverage",
        "paths": [ "/var/data/*" ]
      },
      "recipe": {
        "name": "my_custom_recipe",
        "options": {
          "time_format": "auto",
          "time_crs": "http://localhost:8080/def/crs/OGC/0/AnsiDate",
          "time_tag": "MY_SPECIAL_TIME_TAG",
        }
      }
    }

To create a new recipe start by creating a new folder in the recipes folder.
Let's call our recipe ``my_custom_recipe``:

.. hidden-code-block:: bash

    $ cd $RMANHOME/share/rasdaman/wcst_import/recipes_custom/
    $ mkdir my_custom_recipe
    $ touch __init__.py

The last command is needed to tell python that this folder is containing python
sources, if you forget to add it, your recipe will not be automatically
detected. Let's first create an example of our ingredients file so we get a
feeling for what we will be dealing with in the recipe. Our recipe will just
request from the user two parameters Let's now create our recipe, by creating a
file called ``recipe.py``

.. hidden-code-block:: bash

    $ touch recipe.py
    $ editor recipe.py

Use your favorite editor or IDE to work on the recipe (there are type
annotations for most WCSTImport classes so an IDE like PyCharm would give out of
the box completion support). First, let's add the skeleton of the recipe (please
note that in this tutorial, we will omit the import section of the files (your
IDE will help you auto import them)):

.. hidden-code-block:: python

    class Recipe(BaseRecipe):
        def __init__(self, session):
            """
            The recipe class for my_custom_recipe.
            :param Session session: the session for the import tun
            """
            super(Recipe, self).__init__(session)
            self.options = session.get_recipe()['options']

        def validate(self):
            super(Recipe, self).validate()
            pass

        def describe(self):
            """
            Implementation of the base recipe describe method
            """
            pass

        def ingest(self):
            """
            Imports the input files
            """
            pass

        def status(self):
            """
            Implementation of the status method
            :rtype (int, int)
            """
            pass

        @staticmethod
        def get_name():
            return "my_custom_recipe"

The first thing you need to do is to make sure the ``get_name()`` method returns
the name of your recipe. This name will be used to determine if an ingredient file
should be processed by your recipe. Next, you will need to focus on the
constructor. Let's examine it. We get a single parameter called ``session`` which
contains all the information collected from the user plus a couple more useful
things. You can check all the available methods of the class in the session.py
file, for now we will just save the options provided by the user that are
available in ``session.get_recipe()`` in a class attribute.

In the ``validate()`` method, you will validate the options for the recipe
provided by the user. It's generally a good idea to call the super method to
validate some of the general things like the WCST Service availability and so on
although it is not mandatory. We also want to validate our custom recipe options
here. This is how the recipe looks like now:

.. hidden-code-block:: python

    class Recipe(BaseRecipe):
        def __init__(self, session):
            """
            The recipe class for my_custom_recipe.
            :param Session session: the session for the import tun
            """
            super(Recipe, self).__init__(session)
            self.options = session.get_recipe()['options']

        def validate(self):
            super(Recipe, self).validate()
            if "time_crs" not in self.options:
                raise RecipeValidationException(
                    "No valid time crs provided")

            if 'time_tag' not in self.options:
                raise RecipeValidationException(
                    "No valid time tag parameter provided")

            if 'time_format' not in self.options:
                raise RecipeValidationException(
                    "You have to provide a valid time format")

        def describe(self):
            """
            Implementation of the base recipe describe method
            """
            pass

        def ingest(self):
            """
            Imports the input files
            """
            pass

        def status(self):
            """
            Implementation of the status method
            :rtype (int, int)
            """
            pass

        @staticmethod
        def get_name():
            return "my_custom_recipe"

Now that our recipe can validate the recipe options, let's move to the
``describe()`` method. This method allows you to let your users know any
relevant information about the data import before it actually starts. The
``irregular_timeseries`` recipe prints the timestamp for the first couple of
slices for the user to check if they are correct. Similar behaviour should be
done based on what your recipe has to do.

Next, we should define the import behaviour. The framework does not make any
assumptions about how the correct method of data import is, however it offers a
lot of utility functionality that help you do it in a more standardized way. We
will continue this tutorial by describing how to take advantage of this
functionality, however, note that this is not required for the recipe to work.
The first thing that you need to do is to define an *importer* object. This
importer object, takes a *coverage* object and imports it using WCST requests. The
object has two public methods, ``ingest()``, which imports the coverage into the
WCS-T service (note: this can be an insert operation when the coverage was not
defined, or update if the coverage exists. The importer will handle both cases
for you, so you don't have to worry if the coverage already exists.) and
``get_progress()`` which returns a tuple containing the number of imported slices and
the total number of slices. After adding the importer, the code should look like
this:

.. hidden-code-block:: python

    class Recipe(BaseRecipe):
        def __init__(self, session):
            """
            The recipe class for my_custom_recipe.
            :param Session session: the session for the import tun
            """
            super(Recipe, self).__init__(session)
            self.options = session.get_recipe()['options']
            self.importer = None

        def validate(self):
            super(Recipe, self).validate()
            if "time_crs" not in self.options:
                raise RecipeValidationException(
                    "No valid time crs provided")

            if 'time_tag' not in self.options:
                raise RecipeValidationException(
                    "No valid time tag parameter provided")

            if 'time_format' not in self.options:
                raise RecipeValidationException(
                    "You have to provide a valid time format")

        def describe(self):
            """
            Implementation of the base recipe describe method
            """
            pass

        def ingest(self):
            """
            Imports the input files
            """
            self._get_importer().ingest()

        def status(self):
            """
            Implementation of the status method
            :rtype (int, int)
            """
            pass

        def _get_importer():
          if self.importer is None:
            self.importer = Importer(self._get_coverage())
          return self.importer

        def _get_coverage():
          pass

        @staticmethod
        def get_name():
            return "my_custom_recipe"


In order to build the importer, we need to create a coverage object. Let's see
how we can do that. The coverage constructor requires a

* ``coverage_id``: the id of the coverage

* ``slices``: a list of slices that compose the coverage. Each slice defines
  the position in the coverage and the data that should be defined at the specified
  position

* ``range_fields``: the range fields for the coverage

* ``crs``: the crs of the coverage

* ``pixel_data_type``: the type of the pixel in gdal format, e.g. Byte, Float32 etc

The coverage object can be built in many ways, we will present one such method.
Let's start from the crs of the coverage.
For our recipe, we want a 3D crs, composed of the CRS of the 2D images and a time CRS
as indicated. The following lines of code give us exactly this:

.. hidden-code-block:: python

    # Get the crs of one of the images using a GDAL helper class.
    # We are assuming all images have the same CRS.
    gdal_dataset = GDALGmlUtil(self.session.get_files()[0].get_filepath())
    # Get the crs of the coverage by compounding the two crses
    crs = CRSUtil.get_compound_crs([gdal_dataset.get_crs(), self.options['time_crs']])

Let's also get the range fields for this coverage. We can extract them again
from the 2D image using a helper class that can use GDAL to get the relevant
information:

.. hidden-code-block:: python

  fields = GdalRangeFieldsGenerator(gdal_dataset).get_range_fields()

Let's also get the pixel base type, again using the gdal helper:

.. code-block:: python

  pixel_type = gdal_dataset.get_band_gdal_type()

Let's see what we have so far:

.. hidden-code-block:: python

    class Recipe(BaseRecipe):
        def __init__(self, session):
            """
            The recipe class for my_custom_recipe.
            :param Session session: the session for the import tun
            """
            super(Recipe, self).__init__(session)
            self.options = session.get_recipe()['options']
            self.importer = None

        def validate(self):
            super(Recipe, self).validate()
            if "time_crs" not in self.options:
                raise RecipeValidationException(
                    "No valid time crs provided")

            if 'time_tag' not in self.options:
                raise RecipeValidationException(
                    "No valid time tag parameter provided")

            if 'time_format' not in self.options:
                raise RecipeValidationException(
                    "You have to provide a valid time format")

        def describe(self):
            """
            Implementation of the base recipe describe method
            """
            pass

        def ingest(self):
            """
            Import the input files
            """
            self._get_importer().ingest()

        def status(self):
            """
            Implementation of the status method
            :rtype (int, int)
            """
            pass

        def _get_importer(self):
          if self.importer is None:
            self.importer = Importer(self._get_coverage())
          return self.importer

        def _get_coverage(self):
          # Get the crs of one of the images using a GDAL helper class.
          # We are assuming all images have the same CRS.
          gdal_dataset = GDALGmlUtil(self.session.get_files()[0].get_filepath())
          # Get the crs of the coverage by compounding the two crses
          crs = CRSUtil.get_compound_crs(
            [gdal_dataset.get_crs(), self.options['time_crs']])
          fields = GdalRangeFieldsGenerator(gdal_dataset).get_range_fields()
          pixel_type = gdal_dataset.get_band_gdal_type()
          coverage_id = self.session.get_coverage_id()
          slices = self._get_slices(crs)
          return Coverage(coverage_id, slices, fields, crs, pixel_type)

        def _get_slices(self, crs):
          pass

        @staticmethod
        def get_name():
            return "my_custom_recipe"

As you can notice, the only thing left to do is to implement the _get_slices()
method. To do so we need to iterate over all the input files and create a slice
for each. Here's an example on how we could do that

.. hidden-code-block:: python

    def _get_slices(self, crs):
      # Let's first extract all the axes from our crs
      crs_axes = CRSUtil(crs).get_axes()
        # Prepare a list container for our slices
        slices = []
        # Iterate over the files and create a slice for each one
        for infile in self.session.get_files():
          # We need to create the exact position in time and space in which to
          # place this slice # For the space coordinates we can use the GDAL
          # helper to extract it for us, which will return a list of subsets
          # based on the crs axes that we extracted # and will fill the
          # coordinates for the ones that it can (the easting and northing axes)
          subsets = GdalAxisFiller(
            crs_axes, GDALGmlUtil(infile.get_filepath())).fill()
          # fill the time axis as well and indicate the position in time
          for subset in subsets:
            # Find the time axis
            if subset.coverage_axis.axis.crs_axis.is_future():
              # Set the time position for it. Our recipe extracts it from
              # a GDAL tag provided by the user
              subset.interval.low = GDALGmlUtil(infile).get_datetime(
                self.options["time_tag"])
          slices.append(Slice(subsets, FileDataProvider(tpair.file)))
      return slices

And we are done we now have a valid coverage object. The last thing needed is to
define the status method. This method need to provide a status update to the
framework in order to display it to the user. We need to return the number of
finished work items and the number of total work items. In our case we can
measure this in terms of slices and the importer can already provide this for
us. So all we need to do is the following:

.. hidden-code-block:: python

    def status(self):
        return self._get_importer().get_progress()

We now have a functional recipe. You can try the ingredients file against it and
see how it works.

.. hidden-code-block:: python

    class Recipe(BaseRecipe):
        def __init__(self, session):
            """
            The recipe class for my_custom_recipe.
            :param Session session: the session for the import tun
            """
            super(Recipe, self).__init__(session)
            self.options = session.get_recipe()['options']
            self.importer = None

        def validate(self):
            super(Recipe, self).validate()
            if "time_crs" not in self.options:
                raise RecipeValidationException(
                    "No valid time crs provided")

            if 'time_tag' not in self.options:
                raise RecipeValidationException(
                    "No valid time tag parameter provided")

            if 'time_format' not in self.options:
                raise RecipeValidationException(
                    "You have to provide a valid time format")

        def describe(self):
            """
            Implementation of the base recipe describe method
            """
            pass

        def ingest(self):
            """
            Import the input files
            """
            self._get_importer().ingest()

        def status(self):
            """
            Implementation of the status method
            :rtype (int, int)
            """
            pass

        def _get_importer(self):
          if self.importer is None:
            self.importer = Importer(self._get_coverage())
          return self.importer

        def _get_coverage(self):
          # Get the crs of one of the images using a GDAL helper class.
          # We are assuming all images have the same CRS.
          gdal_dataset = GDALGmlUtil(self.session.get_files()[0].get_filepath())
          # Get the crs of the coverage by compounding the two crses
          crs = CRSUtil.get_compound_crs(
            [gdal_dataset.get_crs(), self.options['time_crs']])
          fields = GdalRangeFieldsGenerator(gdal_dataset).get_range_fields()
          pixel_type = gdal_dataset.get_band_gdal_type()
          coverage_id = self.session.get_coverage_id()
          slices = self._get_slices(crs)
          return Coverage(coverage_id, slices, fields, crs, pixel_type)

        def _get_slices(self, crs):
          # Let's first extract all the axes from our crs
          crs_axes = CRSUtil(crs).get_axes()
          # Prepare a list container for our slices
          slices = []
          # Iterate over the files and create a slice for each one
          for infile in self.session.get_files():
            # We need to create the exact position in time and space in which to
            # place this slice # For the space coordinates we can use the GDAL
            # helper to extract it for us, which will return a list of subsets
            # based on the crs axes that we extracted # and will fill the
            # coordinates for the ones that it can (the easting and northing axes)
            subsets = GdalAxisFiller(
                crs_axes, GDALGmlUtil(infile.get_filepath())).fill()
            # fill the time axis as well and indicate the position in time
            for subset in subsets:
                # Find the time axis
                if subset.coverage_axis.axis.crs_axis.is_future():
                # Set the time position for it. Our recipe extracts it from
                # a GDAL tag provided by the user
                subset.interval.low = GDALGmlUtil(infile).get_datetime(
                    self.options["time_tag"])
            slices.append(Slice(subsets, FileDataProvider(tpair.file)))
          return slices

        @staticmethod
        def get_name():
            return "my_custom_recipe"



Data export
===========

**WCS** formats are requested via the **format** KVP key (``<gml:format>``
elements for XML POST requests), and take a valid **MIME type** as value. Output
encoding is passed on to the the GDAL library, so the limitations on output
formats are devised accordingly by the `supported raster formats
<http://www.gdal.org/formats_list.html>`__ of GDAL. The valid MIME types which
Petascope may support can be checked from the WCS 2.0.1 GetCapabilities
response:

.. hidden-code-block:: xml

    <wcs:formatSupported>application/gml+xml</wcs:formatSupported>
    <wcs:formatSupported>image/jpeg</wcs:formatSupported>
    <wcs:formatSupported>image/png</wcs:formatSupported>
    <wcs:formatSupported>image/tiff</wcs:formatSupported>
    <wcs:formatSupported>image/bmp</wcs:formatSupported>
    <wcs:formatSupported>image/jp2</wcs:formatSupported>
    <wcs:formatSupported>application/netcdf</wcs:formatSupported>
    <wcs:formatSupported>text/csv</wcs:formatSupported>
    <wcs:formatSupported>application/json</wcs:formatSupported>
    <wcs:formatSupported>application/dem</wcs:formatSupported>
    ...

In case of *encode* processing expressions, besides MIME types **WCPS** (and
*rasql*) can also accept GDAL format identifiers or other commonly-used format
abbreviations like "CSV" for Comma-Separated-Values for instance.


rasdaman / petascope Geo Service Administration
===============================================

The petascope conpoment, which geo services contact through its OGC APIs,
uses rasdaman for storing the raster arrays; geo-related data parts
(such as geo-referencing), as per coverage standard, are maintained
by petascope itself.

Petascope is implemented as a war file of Java servlets.
Internally, incoming requests requiring coverage evaluation are translated
by petascope, with the help of the coverage metadata, into rasql queries
executed by rasdaman as the central workhorse. Results returned from rasdaman
are forwarded by petascope to the client.

.. NOTE::

   rasdaman can maintain arrays not visible via petascope
   (such as non-geo objects like human brain images).
   Data need to be imported via :ref:`data-import`, not rasql,
   for being visible as coverages.

For further internal documentation on petascope see
`Developer introduction to petascope and its metadata database <http://rasdaman.org/wiki/PetascopeDevGuide>`__.

Service Startup and Shutdown
----------------------------

Depending of how ``java_server`` is configured in ``petascope.properties``,
starting the petascope Web application is different as follows:

- If set to ``external``, then managing the petascope Web application is done
  via the system Tomcat in which it is deployed, e.g. ``service tomcat
  start/stop/restart``.

- If set to :ref:`embedded <start-stop-embedded-applications>` then petascope is
  managed along with rasdaman, e.g. ``start_rasdaman.sh`` and
  ``stop_rasdaman.sh``, or ``service rasdaman start/stop``.

Configuration
-------------

The petascope service is configured in file
``$RMANHOME/etc/petascope.properties``. For changes to take effect, system
Tomcat (if deployment is ``external``) or rasdaman (if deployment is
``embedded``) needs to be restarted after editing this file.


Security
--------

By default only local IP addresses are allowed to make *write requests* to
petascope (e.g. ``InsertCoverage`` and ``UpdateCoverage`` when importing data,
or ``DeleteCoverage``, etc). This is configured through the
``allow_write_requests_from`` setting in ``petascope.properties``.

Any write requests from a non-listed IP address will be blocked. However, if one
has petascope admin user credentials, configured through the
``petascope_admin_user`` and ``petascope_admin_pass`` settings, then one can
send write requests with these credentials via basic authentication header.
This authentication mechanism is used by the WSClient for example when logged
in with the petascope admin credentials, to enable deleting coverages, updating
metadata, styles, etc.

.. _petascope-database-connection:

Meta Database Connectivity
--------------------------

Non-array data of coverages (here loosely called metadata) are stored in another
database, separate from the rasdaman database. This backend is configured in
``petascope.properties``.

As a first action it is highly recommended to substitute {db-username}
and {db-password} by some safe settings; keeping obvious values constitutes
a major security risk.

Note that the choice is exclusive: only one such database can be used at any time.
Changing to another database system requires a database migration
which is entirely the responsibility of the service operator
and involves substantially more effort than just changing these entries;
generally, it is strongly discouraged to change the meta database backend.

If necessary, add the path to the JDBC jar driver to ``petascope.properties``
using ``metadata_jdbc_jar_path`` and ``spring.datasource.jdbc_jar_path``.

Several different systems are supported as metadata backends.
Below is a list of ``petascope.properties`` settings for different systems
that have been tested successfully with rasdaman.

Postgresql (default)
^^^^^^^^^^^^^^^^^^^^

The following configuration in ``petascope.properties`` enables PostgreSQL
as metadata backend:

.. hidden-code-block:: text

    spring.datasource.url=jdbc:postgresql://localhost:5432/petascopedb
    spring.datasource.username={db-username}
    spring.datasource.password={db-password}
    spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect

HSQLDB
^^^^^^

The following configuration in ``petascope.properties`` enables HSQLDB
as metadata backend:

.. hidden-code-block:: text

    spring.datasource.url=jdbc:hsqldb:file://{path-to-petascopedb}/petascopedb.db
    spring.datasource.username={db-username}
    spring.datasource.password={db-password}

H2
^^

The following configuration in ``petascope.properties`` enables H2
as metadata backend:

.. hidden-code-block:: text

    spring.datasource.url=jdbc:h2:file://{path-to-petascopedb}/petascopedb.db;DB_CLOSE_ON_EXIT=FALSE
    spring.datasource.username={db-username}
    spring.datasource.password={db-password}

.. _start-stop-embedded-applications:

petascope Standalone Deployment
-------------------------------

The petascope Web application can be deployed through any suitable servlet
container, or (recommended) can be operated standalone using its built-in
embedded container. The embedded variant is activated through setting
``java_server=embedded`` in ``$RMANHOME/etc/petascope.properties``.

To configure embedded mode, the following options will need to be checked and
adjusted:

- ``petascope.properties``

   .. code-block:: ini
 
      java_server=embedded
      server.port=8080
      # a path writable by the rasdaman user
      log4j.appender.rollingFile.File=/opt/rasdaman/log/petascope.log
      # or
      log4j.appender.rollingFile.rollingPolicy.ActiveFileName=/opt/rasdaman/log/petascope.log

- ``secore.properties``

   .. code-block:: ini

      # paths writable by the rasdaman user
      secoredb.path=/opt/rasdaman/data/secore
      log4j.appender.rollingFile.File=/opt/rasdaman/log/secore.log
      log4j.appender.rollingFile.rollingPolicy.ActiveFileName=/opt/rasdaman/log/secore.log

In the standalone mode petascope can be started individually using the central
startup/shutdown scripts of rasdaman: ::

    $ sudo -u rasdaman start_rasdaman.sh --service petascope
    $ sudo -u rasdaman stop_rasdaman.sh --service petascope

The Web application can be even be started from the command line: ::

    $ java -jar rasdaman.war [ --petascope.confDir={path-to-etc-dir} ]

The port required by the embedded tomcat will be fetched from the
``server.port`` setting in ``petascope.properties``. Assuming the port is set 
to 8080, petascope can be accessed via URL ``http://localhost:8080/rasdaman/ows``.


Serving Static Content 
----------------------

Serving external static content (such as HTML, CSS, and Javascript)
residing outside ``rasdaman.war`` through petascope can be enabled
with the following setting in ``petascope.properties``: ::

    static_html_dir_path={absolute-path-to-index.html}

with an absolute path to a directory readable by the user running petascope. The
directory must contain an ``index.html``, which will be served as the root, ie:
at URL ``http://localhost:8080/rasdaman/``.


Logging
-------

Configuration file ``petascope.properties`` also defines logging. The log level
can be adjusted in verbosity, log file path can be set, etc. Tomcat restart is
required for new settings to become effective.

The user running Tomcat (``tomcat`` or so) must have write permissions to the
``petascope.log`` file specified if ``java_server=external``; usually the file
should be placed in the Tomcat log directory in this case, e.g.
``/var/log/tomcat/petascope.log``.

Otherwise, if ``java_server=embedded``, then the user running rasdaman must have
write permissions to the specified log file; usually the file would be placed
in the rasdaman log directory in this case, e.g.
``/opt/rasdaman/log/petascope.log``.


Geo Service Standards Compliance
================================

rasdaman community is OGC WCS reference implementation and supports the following conformance classes:

- OGC CIS:

 - CIS 1.0:

  - Class GridCoverage
  - Class RectifiedGridCoverage
  - Class ReferenceableGridCoverage
  - Class gml-coverage
  - Class multipart-coverage

 - CIS 1.1:

  - Class grid-regular
  - Class grid-irregular
  - Class gml-coverage
  - Class json-coverage
  - Class other-format-coverage

- OGC WCS

 - WCS 2.0:

  - WCS Core
  - WCS Range Subsetting
  - WCS Processing (supporting WCPS 1.0)
  - WCS Transaction
  - WCS CRS
  - WCS Scaling
  - WCS Interpolation

 - WMS 1.3.0:

   - all raster functionality, including SLD ``ColorMap`` styling

  .. NOTE::

     With WCS 2.1, petascope provides an additional proprietary parameter to request
     CIS 1.0 coverages to be returned as CIS 1.1 coverages.
     This is specified by adding parameter ``outputType=GeneralGridCoverage``.

