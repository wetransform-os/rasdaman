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
 * Copyright 2003 - 2023 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package org.rasdaman.repository.service;

import org.rasdaman.domain.openeo.ProcessGraph;
import org.rasdaman.repository.interfaces.ProcessGraphRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;

import javax.annotation.PostConstruct;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Class to provide services to connect to database for OpenEO process graph
 * https://openeo.org/documentation/1.0/developers/api/reference.html#tag/User-Defined-Processes
 *
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */
@Service
public class ProcessGraphRepositoryService {

    private static final Logger log = LoggerFactory.getLogger(ProcessGraphRepositoryService.class);

    private static Map<String, ProcessGraph> processGraphsCacheMap = new ConcurrentSkipListMap<>();

    @Autowired
    private ProcessGraphRepository processGraphRepository;

    @PostConstruct
    public void loadProcessGraphsToCache() {
        this.readAllProcessGraphsFromDBToCache();
    }

    /**
     * Insert/Update the new collected country object to the statistic table
     */
    public void save(ProcessGraph processGraph) {
        processGraphRepository.save(processGraph);
        processGraphsCacheMap.put(processGraph.getProcessGraphId(), processGraph);
    }

    public void delete(String processGraphId) throws PetascopeException {
        if (!processGraphsCacheMap.containsKey(processGraphId)) {
            throw new PetascopeException(ExceptionCode.InvalidRequest, "Process graph does not exist: " + processGraphId);
        }

        ProcessGraph processGraph = processGraphsCacheMap.get(processGraphId);
        processGraphRepository.delete(processGraph);
        processGraphsCacheMap.remove(processGraphId);
    }

    public ProcessGraph getProcessGraph(String username, String processGraphId) throws PetascopeException {
        if (!processGraphsCacheMap.containsKey(processGraphId)) {
            ProcessGraph processGraph = processGraphsCacheMap.get(processGraphId);

            if (processGraph == null || !processGraph.getUsername().equals(username)) {
                throw new PetascopeException(ExceptionCode.InvalidRequest, "Process graph does not exist: " + processGraphId);
            }
        }

        ProcessGraph result = processGraphsCacheMap.get(processGraphId);
        return result;
    }

    /**
     * Get all process graphs created by the authenticatd user
     */
    public Set getAllCachedProcessGraphsByUsername(String username) {
        Set<ProcessGraph> results = new LinkedHashSet<>();
        for (ProcessGraph processGraph : processGraphsCacheMap.values()) {
            if (processGraph.getUsername().equals(username)) {
                results.add(processGraph);
            }
        }
        return results;
    }

    /**
     * Read all stored process graphs from database
     */
    private void readAllProcessGraphsFromDBToCache() {
        for (ProcessGraph processGraph : this.processGraphRepository.findAll()) {
            processGraphsCacheMap.put(processGraph.getProcessGraphId(), processGraph);
        }
    }
}


