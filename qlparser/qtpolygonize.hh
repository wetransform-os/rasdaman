#ifndef QTPOLYGONIZE_HH
#define QTPOLYGONIZE_HH

#include "conversion/convertor.hh"
#include "qlparser/qtoperation.hh"

#include "qlparser/qtmdd.hh"
#include "qlparser/qtatomicdata.hh"
#include "qlparser/qtpolygonutil.hh"

#include "conversion/gdalincludes.hh"

#include "common/geo/geobbox.hh"
#include "qtdata.hh"
#include "raslib/primitivetype.hh"
#include "tilemgr/tile.hh"
#include <common/geo/resamplingalg.hh>
#include <gdal.h>
#include <gdal_priv.h>
#include <string>
#include <vector>

#ifdef HAVE_GDAL
class GDALDataset;
using GDALDatasetPtr = std::unique_ptr<GDALDataset, void (*)(GDALDataset *)>;
#endif

class QtPolygonize : public QtOperation
{
public:
    QtPolygonize(QtOperation *newInput, const std::string &format);

    // QtPolygonize constructor with connectedness parameter
    QtPolygonize(QtOperation *newInput, int conn);

    QtPolygonize(QtOperation *newInput, const std::string &format, int conn);

    QtPolygonize(QtOperation *newInput, const std::string &format, const std::string &crsIn, const std::string &boundsIn);

    QtPolygonize(QtOperation *newInput, const std::string &format, int conn, const std::string &crsIn, const std::string &boundsIn);

    QtData *evaluate(QtDataList *inputList);

    virtual const QtTypeElement &checkType(QtTypeTuple *typeTuple = NULL);

    void optimizeLoad(QtTrimList *trimList);

    void printAlgebraicExpression(std::ostream &s);

    void printTree(int tab, std::ostream &s, QtChildType mode = QT_ALL_NODES);

private:
    static bool init;
    
    QtOperation *input;

    int connected;

    const std::string dstFormat;

    static const QtNodeType nodeType;

    QtDataList *dataList;

    common::GeoBbox *bbox = nullptr;

    GDALDataset *prepareSourceData(int numBands, std::unique_ptr<r_Primitive_Type> bandType, Tile *sourceTile, r_Nullvalues *nullValues);

    std::pair<int, std::unique_ptr<r_Primitive_Type>> getNumberOfBands(QtMDD *qtMDD) const;

    void polygonize(GDALDataset *srcDs, int numBands, GDALDriver *DstDriver, const std::string &baseFilename, const std::string &additionalFilename) const;

    void transpose(int wi, int hi, int ni, r_Bytes bandCellSz, const GDALDataType gBandType, Tile *sourceTile, GDALDataset *srcDs) const;

    std::pair<std::string, std::string> prepareFilenames(GDALDriver *driver) const;

    std::string prepareResultFile(const std::string &baseFilename) const;

    r_Conv_Desc prepareConvDesc(const std::string &targetFile, const std::string &baseFilename) const;

    QtData *prepareResultTile(r_Conv_Desc &&desc, r_Nullvalues *nullValues) const;

    /**
     * Check if the passed dst format is one of registered MIME types and return the corresponding driver name if it is.
     * If not, return the dst format itself, assuming it is the name of the driver.
     * Matching is performed by `conversion::r_MimeType` class
     */
    std::string getDriverName() const;
};

#endif
