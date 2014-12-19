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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU  General Public License for more details.
 *
 * You should have received a copy of the GNU  General Public License
 * along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright 2003 - 2014 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */

package petascope.wms2.service.insertwcslayer;

import org.jetbrains.annotations.NotNull;
import petascope.core.CoverageMetadata;
import petascope.core.DbMetadataSource;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.SecoreException;
import petascope.util.Vectors;
import petascope.wcps.metadata.Bbox;
import petascope.wcps.metadata.CellDomainElement;
import petascope.wcps.metadata.DomainElement;
import petascope.wms2.metadata.*;
import petascope.wms2.metadata.dao.PersistentMetadataObjectProvider;
import petascope.wms2.service.base.Handler;
import petascope.wms2.service.exception.error.*;
import petascope.wms2.util.CrsComputer;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Handler for the InsertWCSLayer requests. It will use an existing WCS coverage and it will make it available to WMS.
 *
 * @author <a href="mailto:merticariu@rasdaman.com">Vlad Merticariu</a>
 */
public class InsertWCSLayerHandler implements Handler<InsertWCSLayerRequest, InsertWCSLayerResponse> {

    /**
     * Class constructor.
     *
     * @param persistentMetadataObjectProvider the wms metadata source.
     */
    public InsertWCSLayerHandler(PersistentMetadataObjectProvider persistentMetadataObjectProvider) {
        this.persistentMetadataObjectProvider = persistentMetadataObjectProvider;
    }

    /**
     * Handles the insertion of a wcs coverage as wms layer.
     *
     * @param request the typed wms request.
     * @return the response containing the title of the newly added layer.
     * @throws WMSException
     */
    @NotNull
    @Override
    public InsertWCSLayerResponse handle(@NotNull InsertWCSLayerRequest request) throws WMSException {
        Layer persistentWmsLayer = null;
        try {
            //get the wcs coverage metadata
            CoverageMetadata coverageMetadata = getCoverageMetadata(request.getWcsCoverageId());
            //create a wms layer
            Layer wmsLayer = LayerParser.fromWcsCoverage(coverageMetadata, persistentMetadataObjectProvider);
            //persist it
            wmsLayer = persistentMetadataObjectProvider.getLayer().createIfNotExists(wmsLayer);
            persistentWmsLayer = persistentMetadataObjectProvider.getLayer().queryForId(wmsLayer.getId());
            //add the style
            Style style = new Style(persistentWmsLayer.getName(), STYLE_DEFAULT_TITLE, STYLE_DEFAULT_ABSTRACT, null, "", persistentWmsLayer);
            persistentMetadataObjectProvider.getStyle().createIfNotExists(style);
            //add bounding box
            addBoundingBox(coverageMetadata, persistentWmsLayer);
            //add extra dimensions if the coverage is more than 2d
            addExtraDimensions(coverageMetadata.getDomainList(), 2, persistentWmsLayer);
            //add rasdaman layers
            addRasdamanLayers(coverageMetadata, persistentWmsLayer);
            //return the response containing the layer name
            return new InsertWCSLayerResponse(persistentWmsLayer.getTitle());
        } catch (WMSException e) {
            //delete the layer if something went wrong
            if (persistentWmsLayer != null) {
                try {
                    persistentMetadataObjectProvider.getLayer().delete(persistentWmsLayer);
                } catch (SQLException e1) {
                    throw new WMSInternalException(e);
                }
            }
            throw e;
        } catch (Exception e) {
            //delete the layer if something went wrong
            if (persistentWmsLayer != null) {
                try {
                    persistentMetadataObjectProvider.getLayer().delete(persistentWmsLayer);
                } catch (SQLException e1) {
                    throw new WMSInternalException(e);
                }
            }
            throw new WMSInternalException(e);
        }
    }

    /**
     * Creates and persists the rasdaman layer associated with the layer.
     *
     * @param coverageMetadata   the wcs coverage metadata object.
     * @param persistentWmsLayer the wms layer to be associated with the rasdaman layer.
     * @throws WMSInvalidDimensionalityException
     * @throws SQLException
     */
    private void addRasdamanLayers(CoverageMetadata coverageMetadata, Layer persistentWmsLayer) throws WMSInvalidDimensionalityException, SQLException {
        List<CellDomainElement> cellDomainList = coverageMetadata.getCellDomainList();
        if (cellDomainList.size() < 2) {
            //can't handle 1d data
            throw new WMSInvalidDimensionalityException(String.valueOf(cellDomainList.size()));
        }
        Integer xOrder = cellDomainList.get(0).getOrder();
        Integer yOrder = cellDomainList.get(1).getOrder();
        //compute width and height
        Integer width = cellDomainList.get(0).getHiInt() - cellDomainList.get(1).getLoInt() + 1;
        Integer height = cellDomainList.get(1).getHiInt() - cellDomainList.get(1).getLoInt() + 1;
        RasdamanLayer rasdamanLayer = new RasdamanLayer(coverageMetadata.getRasdamanCollection().snd,
                coverageMetadata.getRasdamanCollection().fst,
                1, xOrder, yOrder, width, height, persistentWmsLayer);
        //persist the layer
        persistentMetadataObjectProvider.getRasdamanLayer().create(rasdamanLayer);
    }

    /**
     * Adds and persists a bounding box to a wms layer, from a wcsBbox.
     *
     * @param wcsCoverage        the wcs coverage.
     * @param persistentWmsLayer the layer to which the bounding box is to be persisted.
     * @throws SQLException
     */
    private void addBoundingBox(CoverageMetadata wcsCoverage, Layer persistentWmsLayer) throws SQLException, WMSInvalidCrsUriException, WMSInvalidBbox {
        Bbox wcsBbox = wcsCoverage.getBbox();
        //create a crs object
        Crs crs = new Crs(CrsComputer.convertCrsUriToWmsCrs(wcsBbox.getCrsName()));
        //persist it
        Crs persistentCrs = persistentMetadataObjectProvider.getCrs().createIfNotExists(crs);
        //create and persist the bbox
        BoundingBox boundingBox = create2DBboxFromWcs(persistentCrs, persistentWmsLayer, wcsCoverage);
        persistentMetadataObjectProvider.getBoundingBox().createIfNotExists(boundingBox);
    }

    /**
     * Creates a bounding box from a wcs coverage. In wcs the bounding box is considered shifted , in wms we need it
     * aligned.
     * @param persistentCrs the crs of the bbox that has already been persisted.
     * @param persistentWmsLayer the layer of the bbox that has aready been persisted.
     * @param wcsCoverage the wcs coverage.
     * @return the wms bbox.
     * @throws WMSInvalidBbox
     */
    private static BoundingBox create2DBboxFromWcs(Crs persistentCrs, Layer persistentWmsLayer, CoverageMetadata wcsCoverage) throws WMSInvalidBbox {
        List<BigDecimal> origin = wcsCoverage.getGridOrigin();
        BigDecimal offsetX = null;
        BigDecimal offsetY = null;

        Integer xSize = wcsCoverage.getCellDomain(0).getHiInt() - wcsCoverage.getCellDomain(0).getLoInt() + 1;
        Integer ySize = wcsCoverage.getCellDomain(1).getHiInt() - wcsCoverage.getCellDomain(0).getLoInt() + 1;

        //compute the offsets on the first 2 axes
        for (Map.Entry<List<BigDecimal>,BigDecimal> axis : wcsCoverage.getGridAxes().entrySet()) {
            List<Integer> axisNonZeroIndices = Vectors.nonZeroComponentsIndices(
                axis.getKey().toArray(new BigDecimal[axis.getKey().size()])
            );
            //first axis offset vector if not set already
            if(offsetX == null){
                offsetX = axis.getKey().get(axisNonZeroIndices.get(0));
            }
            else {
                if(offsetY == null) {
                    //second axis offset vector
                    offsetY = axis.getKey().get(axisNonZeroIndices.get(0));
                }
            }
        }
        //compute the origin
        Double minX = Double.valueOf(origin.get(0).toString());
        Double maxY = Double.valueOf(origin.get(1).toString());
        Double maxX = minX + Double.valueOf((new BigDecimal(xSize)).multiply(offsetX).toString());
        Double minY = maxY - Double.valueOf((new BigDecimal(ySize)).multiply(offsetY).toString());
        //check that the mins are actually smaller since some times axis are inverted
        if(minX > maxX){
            Double aux = minX;
            minX = maxX;
            maxX = aux;
        }
        if(minY > maxY){
            Double aux = minY;
            minY = maxY;
            maxY = aux;
        }
        return new BoundingBox(persistentCrs, minX, minY, maxX, maxY, persistentWmsLayer);
    }

    /**
     * Adds and persists extra dimensions to a wms layer.
     *
     * @param wcsDimensions      the dimensions list of the wcs coverage.
     * @param dimensionOffset    how many dimensions to pe skipped (in wms, the first 2 dimensions are treated separately).
     * @param persistentWmsLayer the layer to which the extra dimensions are to be persisted.
     * @throws SQLException
     */
    private void addExtraDimensions(List<DomainElement> wcsDimensions, Integer dimensionOffset, Layer persistentWmsLayer) throws SQLException {
        if (wcsDimensions.size() > dimensionOffset) {
            for (Integer i = dimensionOffset; i < wcsDimensions.size(); i++) {
                DomainElement wcsDimension = wcsDimensions.get(i);
                Dimension wmsDimension = new Dimension(wcsDimension.getLabel(), wcsDimension.getUom(), null,
                        wcsDimension.getMinValue().toString(), true, false, false,
                        wcsDimension.getMinValue() + "/" + wcsDimension.getMaxValue(), wcsDimension.getOrder(), persistentWmsLayer);
                //persist it
                persistentMetadataObjectProvider.getDimension().create(wmsDimension);
            }
        }
    }

    /**
     * Reads the metadata information for the given coverageId. For now, metadata is read from the current server.
     * If, in the future, the service should be able to insert coverages from a remote server, change this method to
     * fire a DescribeCoverage request.
     *
     * @param wcsCoverageId the id of the wcs coverage
     * @return a metadata object associated with the coverage
     * @throws SecoreException
     * @throws PetascopeException
     */
    private CoverageMetadata getCoverageMetadata(String wcsCoverageId) throws SecoreException, PetascopeException {
        //init metadata source
        DbMetadataSource metadataSource = new DbMetadataSource(petascope.ConfigManager.METADATA_DRIVER,
                petascope.ConfigManager.METADATA_URL,
                petascope.ConfigManager.METADATA_USER,
                petascope.ConfigManager.METADATA_PASS, false);
        //get the coverage that is needed
        return metadataSource.read(wcsCoverageId);
    }

    private final PersistentMetadataObjectProvider persistentMetadataObjectProvider;
    private final String STYLE_DEFAULT_NAME = "default_raster_style";
    private final String STYLE_DEFAULT_TITLE = "Default Raster Style";
    private final String STYLE_DEFAULT_ABSTRACT = "A default style that returns the layer exactly as it is stored";
}