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
 * Copyright 2003 - 2017 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package org.rasdaman.repository.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.rasdaman.domain.wms.Layer;
import org.rasdaman.repository.interfaces.LayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.exceptions.PetascopeRuntimeException;
import petascope.util.ThreadUtil;

/**
 *
 * Service class to access to WMS repository (data access class) in database
 *
 * @author <a href="mailto:bphamhuu@jacobs-university.net">Bang Pham Huu</a>
 */
@Service
@Transactional
public class WMSRepostioryService {

    @Autowired
    private LayerRepository layerRepository;
    @Autowired
    private CoverageRepositoryService coverageRepositoryService;
    @Autowired
    private WMTSRepositoryService wmtsRepositoryService;

    // NOTE: for migration, Hibernate caches the object in first-level cache internally
    // and recheck everytime a new entity is saved, then with thousands of cached objects for nothing
    // it will slow significantly the speed of next saving coverage, then it must be clear this cache.        
    // As targetEntityManagerFactory is set with Primary in bean application of migration application, no need to specify the unitName=target for this PersistenceContext
    @PersistenceContext
    private EntityManager entityManager;   

    private static final Logger log = LoggerFactory.getLogger(WMSRepostioryService.class);

    // Cache all the metadata for WMS layers
    public static final Map<String, Layer> localLayersCacheMap = new ConcurrentSkipListMap<>();

    /**
     * Return a layer object if a layer name (or qualified layer name) exists from local cache map
     */
    private Layer getLocalLayerByNameFromCache(String inputLayerName) throws PetascopeException {
        Layer result = null;
        String layerName = inputLayerName;
        result = this.localLayersCacheMap.get(layerName);

        return result;
    }
    
    public boolean isInCache(String layerName) throws PetascopeException {
        return this.isInLocalCache(layerName);
    }
    
    /**
     * Check if a layer already exists from local loaded cache map
     */
    public boolean isInLocalCache(String layerName) throws PetascopeException {
        if (localLayersCacheMap.isEmpty()) {
            for (String layerNameTmp : this.readAllLocalLayerNames()) {
                this.readLayerByName(layerNameTmp);
            }
        }
        
        return this.getLocalLayerByNameFromCache(layerName) != null;
    }

    /**
     * Validate if layer exist in local
     */
    public void validateInLocalCache(String layerName) throws PetascopeException {
        if (!this.isInLocalCache(layerName)) {
            throw new PetascopeException(ExceptionCode.NoSuchCoverage, "Layer '" + layerName + "' does not exist in local.");
        }
    }

    /**
     * Read persisted OwsServiceMetadata from cache. NOTE: only used when read
     * layer's metadata to GetMap, not for updating or deleting as it will have
     * error in Hibernate (Constraint violation with cached object)
     */
    public Layer readLayerByName(String inputLayerName) throws PetascopeException {
        Layer layer = null;
        
        String layerName = inputLayerName;

        // Check if layer already cached in local cache
        layer = localLayersCacheMap.get(layerName);

        return layer;
    }

    /**
    * Read the layer from the local cache
    */
    public Layer readLayerByNameFromLocalCache(String layerName) throws PetascopeException {
        Layer layer = this.getLocalLayerByNameFromCache(layerName);
        return layer;
    }

    /**
     *
     * Read persisted Layer from database. NOTE: used only when
     * update/insert/delete layer.
     *
     */
    public Layer readLayerByNameFromDatabase(String layerName) throws PetascopeException {
        
        // This happens when Petascope starts and user sends a WMS GetMap query to a coverage instead of WMS GetCapabilities
        if (localLayersCacheMap.isEmpty()) {
            this.readAllLayers();
        }
        
        final long start = System.currentTimeMillis();
        
        Layer layer = this.layerRepository.findOneByName(layerName);
        if (layer == null) {
            throw new PetascopeException(ExceptionCode.NoSuchLayer, "Layer '" + layerName + "' does not exist in local petascopedb.");
        }
        
        final long end = System.currentTimeMillis();
        final long totalTime = end - start;
        log.debug("Time to read WMS layer " + layerName + " from local database is: " + String.valueOf(totalTime) + " ms.");
        
        // put to cache
        localLayersCacheMap.put(layerName, layer);
        
        return layer;
    }

    /**
     *
     * Read all persisted WMS layers from database
     *
     * @return
     */
    public List<Layer> readAllLayers() throws PetascopeException {

        // Read all layers from database
        List<Layer> layers = this.readAllLocalLayersFromCache();
        final WMTSRepositoryService wmtsRepostioryService = this.wmtsRepositoryService;
        
        Runnable localTask = new Runnable() {
            @Override
            public void run() {
                try {
                    wmtsRepostioryService.initializeLocalTileMatrixSetsMapCache();
                } catch (PetascopeException ex) {
                    throw new PetascopeRuntimeException(ex);
                }
            }
        };
        Thread thread1 = new Thread(localTask);
        thread1.start();

        return layers;
    }

    /**
     * Read all stored layers in local cache and from outpeer nodes
     */
    public List<Layer> readAllLayersFromCaches() throws PetascopeException {
        List<Layer> layers = new ArrayList<>(localLayersCacheMap.values());
        
        List<Layer> results = new ArrayList<>();
        
        for (Layer layer : layers) {
            String layerName = layer.getName();
            if (!this.coverageRepositoryService.isInCache(layerName)) {
                log.warn("Coverage associated with the layer: " + layer.getName() + " does not exist.");
            } else {
                results.add(layer);
            }
        }

        return results;
    }


     /**
     * This one should return only local layer of this node and not contain any remote layers.
     */
    public List<Layer> readAllLocalLayersFromCache() throws PetascopeException {
        List<Layer> layers = new ArrayList<>();
        
        final long start = System.currentTimeMillis();        
        if (localLayersCacheMap.isEmpty()) {
            for (Layer layer : this.layerRepository.findAll()) {
                layers.add(layer);
                
                String layerName = layer.getName();
                localLayersCacheMap.put(layerName, layer);
            }

            final long end = System.currentTimeMillis();
            final long totalTime = end - start;
            log.debug("Time to read all WMS layers from local database is: " + String.valueOf(totalTime) + " ms.");
        } else {
            layers = new ArrayList<>(localLayersCacheMap.values());
        }

        return layers;
    }

    /**
     * Save a WMS Layer object to persistent database
     */
    public void saveLayer(Layer layer) {
        this.layerRepository.save(layer);
        
        // add to WMS layers cache if it does not exist or update the existing one
        String layerName = layer.getName();
        localLayersCacheMap.put(layerName, layer);
        
        entityManager.flush();
        entityManager.clear();

        log.debug("WMS Layer: " + layer.getName() + " is persisted to database.");
    }

    /**
     * Delete a WMS Layer object to persistent database
     */
    public void deleteLayer(Layer layer) {
        this.layerRepository.delete(layer);
        // remove layer from cache
        localLayersCacheMap.remove(layer.getName());

        entityManager.flush();
        entityManager.clear();

        log.debug("WMS Layer: " + layer.getName() + " is removed from database.");
    }
    
    /**
     * Remove the layer from local cache map when needed
     */
    public void removeLayerFromLocalCache(String layerName) {
        localLayersCacheMap.remove(layerName);
    }

    /**
     * Rename a WMS layer to a new name, e.g: layerA -> layerB
     */
    public void updateLayerName(String currentLayerName, String newLayerName) throws PetascopeException {
        Layer layer = null;
        try {
            layer = this.readLayerByNameFromDatabase(currentLayerName);
        } catch (PetascopeException ex) {
            if (!ex.getExceptionCode().equals(ExceptionCode.NoSuchLayer)) {
                throw ex;
            }
        }
        if (layer != null) {
            layer.setName(newLayerName);
            
            this.saveLayer(layer);
            this.localLayersCacheMap.remove(currentLayerName);
        
            log.info("Renamed layer name from '" + currentLayerName + "' to '" + newLayerName + "'.");
        }
    }
    

    // For migration only
    
    /**
     * Check if layer name already migrated in new database
     */
    public boolean layerNameExist(String legacyWMSLayerName) {
        Layer layer = this.layerRepository.findOneByName(legacyWMSLayerName);

        return layer != null;
    }
    
    /**
     * Fetch all the layer names from wms13 layer table
     */
    public List<String> readAllLocalLayerNames() {
        List<String> layerNames = layerRepository.readAllLayerNames();
        
        return layerNames;
    }
}
