#include "common/geo/geobbox.hh"
#include "common/uuid/uuid.hh"
#include "common/util/fileutils.hh"
#include "conversion/convertor.hh"
#include "conversion/convfactory.hh"
#include "conversion/tmpfile.hh"
#include "logging.hh"
#include "qlparser/qtoperation.hh"
#include "qlparser/qtscalardata.hh"

#include "qlparser/qtmdd.hh"
#include "../common/util/system.hh"

#include "qtdata.hh"
#include "qtmshapedata.hh"
#include "raslib/marraytype.hh"
#include "raslib/nullvalues.hh"
#include "relcatalogif/basetype.hh"
#include "relcatalogif/mddbasetype.hh"
#include "mddmgr/mddobj.hh"
#include "tilemgr/tile.hh"

#include "qlparser/qtnode.hh"

#include <cstddef>
#include <cstdlib>
#include <iostream>
#include <memory>
#include <sstream>
#include <string>
#include <filesystem>
#include <vector>
#include "relcatalogif/typefactory.hh"

#include "qtpolygonize.hh"
#include "../common/util/fileutils.hh"
#include "conversion/mimetypes.hh"
//issue, cannot include the above file. It has some internal include file issues.
#include "rasodmg/gmarray.hh"
#include "rasodmg/marray.hh"
#include "raslib/point.hh"
#include "raslib/minterval.hh"

#ifdef HAVE_GDAL
#include "conversion/gdalincludes.hh"
#include <cpl_conv.h>
#include <gdal_alg.h>
#include "conversion/gdal.hh"
#include "/usr/include/gdal/ogr_api.h"
#include "/usr/include/gdal/ogrsf_frmts.h"
#include <gdal.h>
#include <gdal_priv.h>
#endif

#include "conversion/convutil.hh"
#include "raslib/primitivetype.hh"

#include "config.h"
#include "qlparser/qtmintervaldata.hh"
#include "raslib/error.hh"
#include "raslib/type.hh"
#include "raslib/primitivetype.hh"
#include "raslib/structuretype.hh"
#include "raslib/point.hh"
#include "catalogmgr/ops.hh"
#include "conversion/convutil.hh"
#include "relcatalogif/basetype.hh"
#include "tilemgr/tile.hh"
#include "mddmgr/mddobj.hh"
#include "mymalloc/mymalloc.h"
#include "qtproject.hh"
#include "common/util/scopeguard.hh"

#include <iostream>
#include <string.h>

using namespace std;

const QtNode::QtNodeType QtPolygonize::nodeType = QtNode::QT_POLYGONIZE;

bool QtPolygonize::init = []()
{
#ifdef HAVE_GDAL
    GDALAllRegister();
    CPLSetErrorHandler(customGdalErrorHandler);
#endif
    return true;
}();

QtPolygonize::QtPolygonize(QtOperation *newInput, int conn)
    : QtOperation(), input(newInput), connected(conn), dstFormat("ESRI Shapefile")

{
}

QtPolygonize::QtPolygonize(QtOperation *newInput, const std::string &format)
    : QtOperation(), input(newInput), connected(4), dstFormat(format)
{
}

QtPolygonize::QtPolygonize(QtOperation *newInput, const std::string &format, int conn)
    : QtOperation(), input(newInput), connected(conn), dstFormat(format)
{
}

QtPolygonize::QtPolygonize(QtOperation *newInput, const std::string &format, const std::string &crsIn, const std::string &boundsIn)
    : QtOperation(), input(newInput), dstFormat(format), bbox(new common::GeoBbox{crsIn, boundsIn, 0, 0})
{
}

QtPolygonize::QtPolygonize(QtOperation *newInput, const std::string &format, int conn, const std::string &crsIn, const std::string &boundsIn)
    : QtOperation(), input(newInput), connected(conn), dstFormat(format), bbox(new common::GeoBbox{crsIn, boundsIn, 0, 0})
{
}


QtData *
QtPolygonize::evaluate(QtDataList *inputList)
{
    QtData *returnValue = NULL;
    QtData *operand = input->evaluate(inputList);

    QtMDD *qtMDD = static_cast<QtMDD *>(operand);
    MDDObj *currentMDDObj = qtMDD->getMDDObject();
    auto *nullValues = currentMDDObj->getNullValues();

    std::vector<std::shared_ptr<Tile>> *tiles = NULL;
    tiles = currentMDDObj->intersect(qtMDD->getLoadDomain());
    Tile *sourceTile = NULL;
    sourceTile = new Tile(tiles, qtMDD->getLoadDomain(), qtMDD->getMDDObject());

    auto [numBands, bandType] = getNumberOfBands(qtMDD);

#ifdef HAVE_GDAL
    auto srcDs = prepareSourceData(numBands, std::move(bandType), sourceTile, nullValues);

    const std::string driverName = getDriverName();

    GDALDriver *DstDriver;
    DstDriver = GetGDALDriverManager()->GetDriverByName(driverName.c_str());

    const char *isCreationSupported;

    LDEBUG << "Driver is created, checking creation support";

    if (DstDriver != NULL)
    {
        LDEBUG << "Fetching metadata item, driver is not null";

        isCreationSupported = DstDriver->GetMetadataItem(GDAL_DCAP_CREATE);

        LDEBUG << "is creation supported? " << isCreationSupported;
    }

    if (DstDriver == NULL || isCreationSupported == NULL)
    {
        LERROR << "No driver with name " << driverName << " is found or the driver does not support creation. The requested format is " << dstFormat;
        throw r_Error(r_Error::r_Error_General, DRIVERNOTSUPPORTEDERROR);
    }

    const auto [baseFilename, additionalFilename] = prepareFilenames(DstDriver);

    polygonize(srcDs, numBands, DstDriver, baseFilename, additionalFilename);
#endif

    LDEBUG << "Polygonize is over";

    const auto targetFile = prepareResultFile(baseFilename + additionalFilename);

    auto desc = prepareConvDesc(targetFile, baseFilename);

    auto result = prepareResultTile(std::move(desc), nullValues);

    return result;
}

GDALDataset *QtPolygonize::prepareSourceData(int numBands, std::unique_ptr<r_Primitive_Type> bandType, Tile *sourceTile, r_Nullvalues *nullValues)
{
    // ==== Getting the input source ====
    const auto &domain = sourceTile->getDomain();
    auto wi = static_cast<int>(domain[0].high() - domain[0].low() + 1);
    auto hi = static_cast<int>(domain[1].high() - domain[1].low() + 1);
    auto ni = numBands;

    if (bbox)
    {
        bbox->width = wi;
        bbox->height = hi;
        bbox->updateGeoTransform();
    }

    r_Primitive_Type *rBandType = bandType.get();
    const auto bandCellSz = rBandType->size();
    const auto gBandType = ConvUtil::rasTypeToGdalType(rBandType);

    GDALDriver *driver = (GDALDriver *)GDALGetDriverByName("MEM");
    if (driver == NULL)
    {
        LERROR << "Could not init GDAL driver.";
        throw r_Error(r_Error::r_Error_RuntimeProjectionError);
    }
    GDALDataset *srcDs;
    srcDs = driver->Create("mem_in", wi, hi, ni, gBandType, NULL);

    if (bbox)
    {
        srcDs->SetGeoTransform(bbox->gt);
        srcDs->SetProjection(bbox->wkt.c_str());
    }

    double nullValue = 0;
    if (nullValues != NULL && !nullValues->getNullvalues().empty())
    {
        LDEBUG << "set null value to " << nullValue << " in " << ni << " source bands.";
        nullValue = nullValues->getFirstNullValue();
        for (int band = 1; band <= ni; ++band)
            srcDs->GetRasterBand(band)->SetNoDataValue(nullValue);
    }

    transpose(wi, hi, ni, bandCellSz, gBandType, sourceTile, srcDs);

    return srcDs;
}

const QtTypeElement &
QtPolygonize::checkType(QtTypeTuple *typeTuple)
{
    dataStreamType.setDataType(QT_MDD);

    // check operand branches
    if (input)
    {
        // get the input type
        const QtTypeElement &inputType1 = input->checkType(typeTuple);

        if (inputType1.getDataType() != QT_MDD)
        {
            LERROR << "QtPolygonize::checkType() - input operand is not of type MDD.";
            parseInfo.setErrorNo(380);
            throw parseInfo;
        }
        dataStreamType.setType(inputType1.getType());
    }
    else
    {
        LERROR << "QtPolygonize::checkType() - input branch invalid.";
    }

    return dataStreamType;
}

void QtPolygonize::optimizeLoad(QtTrimList *trimList)
{
    QtNode::QtTrimList *list1 = NULL;

    if (input)
    {
        list1 = trimList;

        if (input)
        {
            input->optimizeLoad(list1);
        }
    }
    else
    {
        for (QtNode::QtTrimList::iterator iter = trimList->begin(); iter != trimList->end(); iter++)
        {
            delete *iter;
            *iter = NULL;
        }
        delete trimList;
        trimList = NULL;
    }
}

void QtPolygonize::printTree(int tab, std::ostream &s, QtChildType mode)
{
    s << SPACE_STR(static_cast<size_t>(tab)).c_str() << "QtOperation Object: type " << std::flush;
    dataStreamType.printStatus(s);
    s << std::endl;

    if (mode != QtNode::QT_DIRECT_CHILDS)
    {
        s << SPACE_STR(static_cast<size_t>(tab)).c_str() << "no operation" << std::endl;
    }
}

void QtPolygonize::printAlgebraicExpression(std::ostream &s)
{
    s << "op<";
    s << "no ops";
    s << ">";
}

std::pair<int, std::unique_ptr<r_Primitive_Type>> QtPolygonize::getNumberOfBands(QtMDD *qtMDD) const
{
    std::unique_ptr<r_Type> baseSchema;
    {
        auto typeStructure = qtMDD->getCellType()->getTypeStructure();
        baseSchema.reset(r_Type::get_any_type(typeStructure));
    }
    int numBands = 1;
    std::unique_ptr<r_Primitive_Type> bandType;
    if (baseSchema->isComplexType())
    {
        LERROR << "Complex types are not supported";
        throw r_Error(r_Error::r_Error_General, COMPLEXTYPESARENOTSUPPORTED);
    }
    if (auto dim = qtMDD->getLoadDomain().get_origin().dimension(); dim != 2)
    {
        LERROR << "Too high dimension: " << dim;
        throw r_Error(r_Error::r_Error_General, HIGHDIMENSIONSARENOTSUPPORTED);
    }
    if (baseSchema->isPrimitiveType())  // = one band
    {
        numBands = 1;
        bandType.reset(static_cast<r_Primitive_Type *>(baseSchema.release()));
    }
    else if (baseSchema->isStructType())  // = multiple bands
    {
        auto *stype = static_cast<r_Structure_Type *>(baseSchema.get());
        numBands = static_cast<int>(stype->count_elements());
        for (const auto &att: stype->getAttributes())
        {
            // check the band types, they have to be of the same type
            if (att.type_of().isPrimitiveType())
            {
                const auto pt = static_cast<const r_Primitive_Type &>(att.type_of());
                if (bandType)
                {
                    if (bandType->type_id() != pt.type_id())
                    {
                        LERROR << "Can not handle bands of different types.";
                        throw r_Error(r_Error::r_Error_General);
                    }
                }
                else
                {
                    bandType.reset(static_cast<r_Primitive_Type *>(pt.clone()));
                }
            }
            else
            {
                LERROR << "Can not handle composite bands.";
                throw r_Error(r_Error::r_Error_General);
            }
        }
    }

    return {numBands, std::move(bandType)};
}

void QtPolygonize::polygonize(GDALDataset *srcDs, int numBands, GDALDriver *DstDriver, const std::string &baseFilename, const std::string &additionalFilename) const
{
    GDALRasterBand *hSrcBand = srcDs->GetRasterBand(numBands);
    GDALRasterBand *hMaskBand = hSrcBand->GetMaskBand();

    GDALDataset *DstDataset;
    DstDataset = DstDriver->Create((baseFilename + additionalFilename).c_str(), 0, 0, 0, GDT_CInt32, NULL);

    LDEBUG << "dataset created with filename " << baseFilename + additionalFilename;

    const char *SrcCrs = srcDs->GetProjectionRef();
    OGRSpatialReference *DstCrs = new OGRSpatialReference(SrcCrs);

    OGRLayer *hOutLayer = DstDataset->CreateLayer("polygonize", DstCrs, wkbPolygon, NULL);
    OGRFeature *DstFeature;
    DstFeature = OGRFeature::CreateFeature(hOutLayer->GetLayerDefn());

    OGRFieldDefn dst_field("DN", OFTInteger);
    hOutLayer->CreateField(&dst_field);

    int iPixValField = DstFeature->GetFieldIndex("DN");

    char **papszOptions = NULL;

    std::string conn_string = std::to_string(connected);
    char const *pszValue = conn_string.c_str();

    LDEBUG << "checking connectedness";

    if (connected == 8)
    {
        papszOptions = CSLSetNameValue(papszOptions, "8CONNECTED", pszValue);
    }
    else
    {
        papszOptions = CSLSetNameValue(papszOptions, "CONNECTED", pszValue);
    }

    LDEBUG << "Starting polygonize";

    GDALPolygonize(hSrcBand, hMaskBand, hOutLayer, iPixValField, papszOptions, NULL, NULL);

    GDALClose(hSrcBand);
    GDALClose(DstDataset);
    if (DstCrs) DstCrs->Release();

    LDEBUG << "GDAL is closed";
}

void QtPolygonize::transpose(int wi, int hi, int ni, r_Bytes bandCellSz, const GDALDataType gBandType, Tile *sourceTile, GDALDataset *srcDs) const
{
    auto w = static_cast<size_t>(wi);
    auto h = static_cast<size_t>(hi);
    auto n = static_cast<size_t>(ni);

    const auto cellSz = n * bandCellSz;

    const char *srcCells = reinterpret_cast<const char *>(sourceTile->getContents());
    char *dstCells RAS_ALIGNED = static_cast<char *>(mymalloc(w * h * bandCellSz));
    //
    // for all bands:
    //  1. transpose src tile data
    //  2. add transposed data to the GDAL dataset
    //
    for (int band = 0; band < ni; ++band)
    {
        const char *src = srcCells + (static_cast<size_t>(band) * bandCellSz);
        char *dst = dstCells;

        // 1. transpose
        if (bandCellSz == 1)
        {
            // optimization to avoid memcpy for char/octet/boolean
            for (size_t row = 0; row < h; ++row)
                for (size_t col = 0; col < w; ++col, ++dst)
                {
                    *dst = src[(row + h * col) * cellSz];
                }
        }
        else
        {
            for (size_t row = 0; row < h; ++row)
                for (size_t col = 0; col < w; ++col, dst += bandCellSz)
                {
                    memcpy(dst, src + ((row + h * col) * cellSz), bandCellSz);
                }
        }
        // 2. add to GDAL data set
        if (srcDs->GetRasterBand(band + 1)->RasterIO(
                GF_Write, 0, 0, wi, hi, (void *)dstCells, wi, hi, gBandType, 0, 0) != CE_None)
        {
            LERROR << "failed writing data to GDAL raster band, reason: " << CPLGetLastErrorMsg();
            free(dstCells);
            throw r_Error(r_Error::r_Error_RuntimeProjectionError);
        }
    }
    free(dstCells);
}

std::pair<std::string, std::string> QtPolygonize::prepareFilenames(GDALDriver *driver) const
{
    const auto rawFileExts = driver->GetMetadataItem(GDAL_DMD_EXTENSIONS);

    LDEBUG << "file extensions handled by driver " << rawFileExts;

    std::vector<std::string> extensions;
    std::string ext;
    std::istringstream extStream(rawFileExts);

    while (extStream >> ext)
    {
        const auto dotedExt = "." + ext;
        extensions.push_back(dotedExt);
    }

    LDEBUG << "preparing to create dst dataset";

    std::string baseFilename = "/tmp/rasdaman/polygonize/";
    if (!common::FileUtils::dirExists(baseFilename))
    {
        const auto creationRes = common::FileUtils::createDirectory(baseFilename);
        if (!creationRes)
        {
            LERROR << "Error creating directory of base filename " << baseFilename;
            throw r_Error(r_Error::r_Error_General, OUTPUTDIRECTORYNONEXISTENT);
        }
    }

    std::string additionalFilename = common::UUID::generateUUID();
    LDEBUG << "additional filename got ID, the filename is " << additionalFilename;

    if (extensions.size() == 1)
    {
        additionalFilename += extensions.back();
        if (!common::FileUtils::dirExists(baseFilename))
        {
            LERROR << "base directory does not exist";
            throw r_Error(r_Error::r_Error_General, OUTPUTDIRECTORYNONEXISTENT);
        }
    }

    return {baseFilename, additionalFilename};
}

std::string QtPolygonize::prepareResultFile(const std::string &baseFilename) const
{
    LDEBUG << "Base filename is: " << baseFilename;
    std::string targetFile = baseFilename;

    if (common::FileUtils::dirExists(baseFilename)) 
    {
        LDEBUG << "target file is a directory";

        auto createdFiles = common::FileUtils::listFiles(baseFilename);
        std::sort(createdFiles.begin(), createdFiles.end()); // mostly for test purposes and consistency

        for (const auto &file: createdFiles)
        {
            LDEBUG << "created file: " << file;
        }

        if (createdFiles.size() > 1)
        {
            LDEBUG << "archivation is needed, number of files is " << createdFiles.size();

            std::ostringstream files;
            for (const auto &file: createdFiles)
            {
                files << file << " ";
            }

            LDEBUG << "files to be archived: " << files.str();

            targetFile += "/polygonize.zip";

            std::ostringstream archiveCmd;
            archiveCmd << "zip -j " << targetFile << " " << files.str(); 

            const auto executionRes = common::SystemUtil::executeSystemCommand(archiveCmd.str().c_str());

            if (!executionRes.empty())
            {
                LERROR << "archivation failed, reason: " << executionRes;
                throw r_Error(r_Error::r_Error_General, ARCHIVATIONFAILED);
            }
        }
        else
        {
            targetFile = createdFiles.back();
        }
    }

    if (common::FileUtils::fileExists(targetFile.c_str()) == false)
    {
        LERROR << "target file " << targetFile << " was not created";
        throw r_Error(r_Error::r_Error_General, OUTPUTFILECREATEDNOTFOUND);
    }

    return targetFile;
}

r_Conv_Desc QtPolygonize::prepareConvDesc(const std::string &targetFile, const std::string &baseFilename) const
{
    r_Conv_Desc desc;

    std::ifstream fin(targetFile, ios::in | ios::binary);
    vector<char> fileData(std::istreambuf_iterator<char>(fin), {});

    LDEBUG << "File is read, the contents size is: " << fileData.size() << '\n';

    desc.dest = new char[fileData.size()];
    memcpy(desc.dest, fileData.data(), fileData.size());

    const auto removedFiles = common::FileUtils::removeDirRecursive(baseFilename);
    LDEBUG << "removed " << removedFiles << " files within base directory";

    auto size = fileData.size();

    desc.destInterv = r_Minterval(1) << r_Sinterval(static_cast<r_Range>(0),
                                                    static_cast<r_Range>(size) - 1);

    LDEBUG << "Interval is calculated";

    desc.destType = r_Type::get_any_type("char");

    return desc;
}

QtData *QtPolygonize::prepareResultTile(r_Conv_Desc &&desc, r_Nullvalues *nullValues) const
{
    auto baseType = std::unique_ptr<const BaseType>(TypeFactory::mapType(desc.destType->name()));

    LDEBUG << "Base type ptr created";

    r_Bytes convResultSize = static_cast<r_Bytes>(desc.destInterv.cell_count()) *
                             static_cast<r_Bytes>(baseType->getSize());

    LDEBUG << "Result size calculated: " << convResultSize << ", creating result Tile";

    std::unique_ptr<Tile> resultTile;
    resultTile.reset(new Tile(desc.destInterv, baseType.get(), true,
                              desc.dest, convResultSize, r_Data_Format::r_GDAL_OGR));

    LDEBUG << "Releasing base type\n";

    baseType.release();
    if (desc.destType)
    {
        delete desc.destType;
        desc.destType = NULL;
    }

    LDEBUG << "Creating return value\n";

    const auto *mddType = static_cast<const MDDBaseType *>(dataStreamType.getType());
    std::unique_ptr<MDDObj> resultMDD;
    resultMDD.reset(new MDDObj(mddType, desc.destInterv, nullValues));
    resultMDD->insertTile(resultTile.get());
    resultTile.release();

    auto resultQtMDD = std::unique_ptr<QtMDD>(new QtMDD(resultMDD.get()));
    resultMDD.release();

    resultQtMDD->setFromConversion(true);
    auto returnValue = resultQtMDD.release();
    returnValue->setNullValues(nullValues);

    return returnValue;
}

std::string QtPolygonize::getDriverName() const
{
    std::string mappedMimeType = r_MimeTypes::getVectorFormatName(dstFormat);
    return mappedMimeType.empty() ? dstFormat : mappedMimeType;
}