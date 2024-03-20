/*
 * This file is part of rasdaman community.
 *
 * Rasdaman community is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Rasdaman community is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Peter Baumann /
rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */

#include "conversion/mimetypes.hh"

#include <boost/algorithm/string/replace.hpp>

using namespace std;

std::map<std::string, std::string> r_MimeTypes::mimeTypeToFormatName = initMimeTypeToFormatNameMap();
std::map<std::string, std::string> r_MimeTypes::mimeTypeToVectorFormatName = initMimeTypeToVectorFormatNameMap();

bool r_MimeTypes::isMimeType(const std::string &mimeType)
{
    return mimeTypeToFormatName.count(mimeType) == 1;
}

bool r_MimeTypes::isVectorMimeType(const std::string &mimeType) 
{
    return mimeTypeToVectorFormatName.count(mimeType) == 1;
}

std::string r_MimeTypes::getFormatName(const std::string &mimeType)
{
    if (isMimeType(mimeType))
    {
        return mimeTypeToFormatName[mimeType];
    }
    else
    {
        return std::string();
    }
}

std::string r_MimeTypes::getVectorFormatName(const std::string &mimeType)
{
    if (isVectorMimeType(mimeType)) 
    {
        return mimeTypeToVectorFormatName[mimeType];
    }
    else
    {
        return std::string();
    }
}

std::map<std::string, std::string> r_MimeTypes::initMimeTypeToVectorFormatNameMap() 
{
    std::map<std::string, std::string> ret;
    ret["application/x-ogc-amigocloud"] = "AmigoCloud";
    ret["application/vnd.apache.arrow.file"] = "Arrow";
    ret["application/vnd.apache.arrow.stream"] = "Arrow";
    ret["application/x-ogc-arc/info-binary-coverage"] = "Arc/Info Binary Coverage";
    ret["application/x-ogc-arc/info-e00-(ascii)-coverage"] = "Arc/Info E00 (ASCII) Coverage";
    ret["application/x-ogc-autocad-dwg"] = "AutoCAD DWG";
    ret["application/x-ogc-carto"] = "Carto";
    ret["text/csv"] = "CSV";
    ret["application/x-ogc-csw"] = "CSW";
    ret["application/x-ogc-dgn"] = "DGN";
    ret["application/x-ogc-dgnv8"] = "DGNv8";
    ret["image/vnd.dxf"] = "DXF";
    ret["application/x-ogc-edigeo"] = "EDIGEO";
    ret["application/vnd.google-earth.kmz"] = "EEDA";
    ret["application/x-ogc-elasticsearch"] = "Elasticsearch";
    ret["application/x-ogc-esrijson"] = "ESRIJSON";
    ret["application/x-ogc-filegdb"] = "FileGDB";
    ret["application/x-ogc-flatgeobuf"] = "FlatGeobuf";
    ret["application/x-ogc-geoconcept"] = "Geoconcept";
    ret["application/geo+json"] = "GeoJSON";
    ret["application/geo+json-seq"] = "GeoJSONSeq";
    ret["application/x-ogc-georss"] = "GeoRSS";
    ret["application/gml+xml"] = "GML";
    ret["application/x-ogc-gmlas"] = "GMLAS";
    ret["application/x-ogc-gmt"] = "GMT";
    ret["application/x-ogc-gpkg"] = "GPKG";
    ret["application/x-ogc-gpsbabel"] = "GPSBabel";
    ret["application/vnd.gpxsee.map+xml"] = "GPX";
    ret["application/x-ogc-grass"] = "GRASS";
    ret["application/x-ogc-gtfs"] = "GTFS";
    ret["application/x-ogc-hana"] = "HANA";
    ret["application/x-ogc-idb"] = "IDB";
    ret["application/x-ogc-idrisi"] = "IDRISI";
    ret["application/x-ogc-interlis-1"] = "INTERLIS 1";
    ret["application/x-ogc-interlis-2"] = "INTERLIS 2";
    ret["application/x-ogc-jml"] = "JML";
    ret["application/x-ogc-jsonfg"] = "JSONFG";
    ret["application/vnd.google-earth.kml+xml"] = "KML";
    ret["application/vnd.google-earth.kml+xml"] = "LIBKML";
    ret["application/vnd.google-earth.kmz"] = "LIBKML";
    ret["application/x-ogc-lvbag"] = "LVBAG";
    ret["application/x-ogc-mapml"] = "MapML";
    ret["application/x-ogc-mapinfo-file"] = "MapInfo File";
    ret["application/x-ogc-mongodbv3"] = "MongoDBv3";
    ret["application/x-ogc-mssqlspatial"] = "MSSQLSpatial";
    ret["application/vnd.mapbox-vector-tile"] = "MVT";
    ret["application/x-ogc-mysql"] = "MySQL";
    ret["application/x-ogc-nas"] = "NAS";
    ret["application/x-ogc-netcdf"] = "netCDF";
    ret["application/x-ogc-ngw"] = "NGW";
    ret["application/x-ogc-uk-.ntf"] = "UK .NTF";
    ret["application/x-ogc-oapif"] = "OAPIF";
    ret["application/x-ogc-oci"] = "OCI";
    ret["application/x-ogc-odbc"] = "ODBC";
    ret["application/vnd.oasis.opendocument.spreadsheet"] = "ODS";
    ret["application/x-ogc-ogdi"] = "OGDI";
    ret["application/x-ogc-openfilegdb"] = "OpenFileGDB";
    ret["application/vnd.openstreetmap.data+xml"] = "OSM";
    ret["application/x-ogc-parquet"] = "Parquet";
    ret["application/pdf"] = "PDF";
    ret["application/x-ogc-pds"] = "PDS";
    ret["application/x-ogc-postgresql"] = "PostgreSQL";
    ret["application/x-ogc-pgdump"] = "PGDump";
    ret["application/x-ogc-pgeo"] = "PGeo";
    ret["application/x-ogc-plscenes"] = "PLScenes";
    ret["application/x-ogc-pmtiles"] = "PMTiles";
    ret["application/x-ogc-s57"] = "S57";
    ret["application/x-ogc-sdts"] = "SDTS";
    ret["application/x-ogc-selafin"] = "Selafin";
    ret["application/vnd.dbf"] = "ESRI Shapefile";
    ret["text/vnd.sosi"] = "SOSI";
    ret["application/geopackage+sqlite3"] = "SQLite";
    ret["application/vnd.sqlite3"] = "SQLite";
    ret["image/svg+xml"] = "SVG";
    ret["application/x-ogc-sxf"] = "SXF";
    ret["application/x-ogc-tiger"] = "TIGER";
    ret["application/x-ogc-tiledb"] = "TileDB";
    ret["application/geo+json"] = "TopoJSON";
    ret["application/x-ogc-vdv"] = "VDV";
    ret["application/x-ogc-vfk"] = "VFK";
    ret["application/x-ogc-vrt"] = "VRT";
    ret["application/x-ogc-wasp"] = "WAsP";
    ret["application/x-ogc-wfs"] = "WFS";
    ret["application/vnd.ms-excel"] = "XLS";
    ret["application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"] = "XLSX";
    return ret;
}

/**
 * TODO: we should make this configurable, e.g. a table in RASBASE or a configuration file.
 */
std::map<std::string, std::string> r_MimeTypes::initMimeTypeToFormatNameMap()
{
    std::map<std::string, std::string> ret;
    ret["text/csv"] = "csv";
    ret["application/json"] = "json";
    ret["text/x-r"] = "R";
    ret["image/x-sgi"] = "SGI";
    ret["image/xpm"] = "XPM";
    ret["image/x-mrsid"] = "MrSID";
    ret["image/x-imagewebserver-ecw"] = "ECW";
    ret["image/x-gtx"] = "GTX";
    ret["image/x-aaigrid"] = "AAIGrid";
    ret["image/tiff"] = "GTiff";
    ret["tiff"] = "GTiff";  //not a mimetype, but enables us to encode queries to "tiff"
    ret["image/png"] = "PNG";
    ret["image/jpip-stream"] = "JPIPKAK";
    ret["image/jpeg"] = "JPEG";
    ret["image/jp2"] = "JPEG2000";
    ret["image/gif"] = "GIF";
    ret["image/bmp"] = "BMP";
    ret["application/netcdf"] = "netCDF";
    ret["application/x-netcdf"] = "netCDF";
    ret["application/x-ogc-zmap"] = "ZMap";
    ret["application/x-ogc-xyz"] = "XYZ";
    ret["application/x-ogc-wms"] = "WMS";
    ret["application/x-ogc-webp"] = "WEBP";
    ret["application/x-ogc-wcs"] = "WCS";
    ret["application/x-ogc-vrt"] = "VRT";
    ret["application/x-ogc-usgsdem"] = "USGSDEM";
    ret["application/x-ogc-tsx"] = "TSX";
    ret["application/x-ogc-til"] = "TIL";
    ret["application/x-ogc-terragen"] = "TERRAGEN";
    ret["application/x-ogc-srtmhgt"] = "SRTMHGT";
    ret["application/x-ogc-srp"] = "SRP";
    ret["application/x-ogc-snodas"] = "SNODAS";
    ret["application/x-ogc-sdts"] = "SDTS";
    ret["application/x-ogc-sde"] = "SDE";
    ret["application/x-ogc-sar_ceos"] = "SAR_CEOS";
    ret["application/x-ogc-saga"] = "SAGA";
    ret["application/x-ogc-rst"] = "RST";
    ret["application/x-ogc-rs2"] = "RS2";
    ret["application/x-ogc-rpftoc"] = "RPFTOC";
    ret["application/x-ogc-rmf"] = "RMF";
    ret["application/x-ogc-rik"] = "RIK";
    ret["application/x-ogc-rasterlite"] = "Rasterlite";
    ret["application/x-ogc-rasdaman"] = "RASDAMAN";
    ret["application/x-ogc-postgisraster"] = "PostGISRaster";
    ret["application/x-ogc-pnm"] = "PNM";
    ret["application/x-ogc-pds"] = "PDS";
    ret["application/x-ogc-pdf"] = "PDF";
    ret["application/x-ogc-pcraster"] = "PCRaster";
    ret["application/x-ogc-pcidsk"] = "PCIDSK";
    ret["application/x-ogc-paux"] = "PAux";
    ret["application/x-ogc-ozi"] = "OZI";
    ret["application/x-ogc-ogdi"] = "OGDI";
    ret["application/x-ogc-nwt_grd"] = "NWT_GRD";
    ret["application/x-ogc-nwt_grc"] = "NWT_GRC";
    ret["application/x-ogc-ntv2"] = "NTv2";
    ret["application/x-ogc-nitf"] = "NITF";
    ret["application/x-ogc-ngsgeoid"] = "NGSGEOID";
    ret["application/x-ogc-ndf"] = "NDF";
    ret["application/x-ogc-msgn"] = "MSGN";
    ret["application/x-ogc-msg"] = "MSG";
    ret["application/x-ogc-mg4lidar"] = "MG4Lidar";
    ret["application/x-ogc-mff2"] = "MFF2 (HKV)";
    ret["application/x-ogc-mff"] = "MFF";
    ret["application/x-ogc-mem"] = "MEM";
    ret["application/x-ogc-mbtiles"] = "MBTiles";
    ret["application/x-ogc-loslas"] = "LOSLAS";
    ret["application/x-ogc-leveller"] = "Leveller";
    ret["application/x-ogc-lcp"] = "LCP";
    ret["application/x-ogc-l1b"] = "L1B";
    ret["application/x-ogc-kmlsuperoverlay"] = "KMLSUPEROVERLAY";
    ret["application/x-ogc-jdem"] = "JDEM";
    ret["application/x-ogc-jaxapalsar"] = "JAXAPALSAR";
    ret["application/x-ogc-isis3"] = "ISIS3";
    ret["application/x-ogc-isis2"] = "ISIS2";
    ret["application/x-ogc-ingr"] = "INGR";
    ret["application/x-ogc-ilwis"] = "ILWIS";
    ret["application/x-ogc-ida"] = "IDA";
    ret["application/x-ogc-hf2"] = "HF2";
    ret["application/x-ogc-gxf"] = "GXF";
    ret["application/x-ogc-gta"] = "GTA";
    ret["application/x-ogc-gsc"] = "GSC";
    ret["application/x-ogc-gsbg"] = "GSBG";
    ret["application/x-ogc-gsag"] = "GSAG";
    ret["application/x-ogc-gs7bg"] = "GS7BG";
    ret["application/x-ogc-grib"] = "GRIB";
    ret["application/x-ogc-grass_asciigrid"] = "GRASSASCIIGrid";
    ret["application/x-ogc-grass"] = "GRASS";
    ret["application/x-ogc-gff"] = "GFF";
    ret["application/x-ogc-georaster"] = "GEORASTER";
    ret["application/x-ogc-genbin"] = "GENBIN";
    ret["application/x-ogc-fujibas"] = "FujiBAS";
    ret["application/x-ogc-fits"] = "FITS";
    ret["application/x-ogc-fit"] = "FIT";
    ret["application/x-ogc-fast"] = "FAST";
    ret["application/x-ogc-esat"] = "ESAT";
    ret["application/x-ogc-ers"] = "ERS";
    ret["application/x-ogc-epsilon"] = "EPSILON";
    ret["application/x-ogc-envi"] = "ENVI";
    ret["application/x-ogc-elas"] = "ELAS";
    ret["application/x-ogc-eir"] = "EIR";
    ret["application/x-ogc-ehdr"] = "EHdr";
    ret["application/x-ogc-ecrgtoc"] = "ECRGTOC";
    ret["application/x-ogc-e00grid"] = "E00GRID";
    ret["application/x-ogc-dted"] = "DTED";
    ret["application/x-ogc-doq2"] = "DOQ2";
    ret["application/x-ogc-doq1"] = "DOQ1";
    ret["application/x-ogc-dods"] = "DODS";
    ret["application/x-ogc-dipex"] = "DIPEx";
    ret["application/x-ogc-dimap"] = "DIMAP";
    ret["application/x-ogc-ctg"] = "CTG";
    ret["application/x-ogc-cpg"] = "CPG";
    ret["application/x-ogc-cosar"] = "COSAR";
    ret["application/x-ogc-coasp"] = "COASP";
    ret["application/x-ogc-ceos"] = "CEOS";
    ret["application/x-ogc-bt"] = "BT";
    ret["application/x-ogc-bsb"] = "BSB";
    ret["application/x-ogc-blx"] = "BLX";
    ret["application/x-ogc-bag"] = "BAG";
    ret["application/x-ogc-airsar"] = "AIRSAR";
    ret["application/x-ogc-aig"] = "AIG";
    ret["application/x-ogc-adrg"] = "ADRG";
    ret["application/x-ogc-ace2"] = "ACE2";
    ret["application/x-netcdf-gmt"] = "GMT";
    ret["application/x-hdf5"] = "HDF5";
    ret["application/x-hdf4"] = "HDF4";
    ret["application/x-erdas-lan"] = "LAN";
    ret["application/x-erdas-hfa"] = "HFA";
    return ret;
}
