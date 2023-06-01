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
 * Copyright 2003 - 2018 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.wcps.metadata.service;

import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;
import petascope.core.Pair;
import petascope.exceptions.PetascopeException;
import petascope.util.ListUtil;
import petascope.util.StringUtil;
import petascope.wcps.handler.CoverageVariableNameHandler;
import petascope.wcps.handler.Handler;
import petascope.wcps.handler.ReturnClauseHandler;
import petascope.wcps.handler.StringScalarHandler;

import static petascope.wcps.handler.ForClauseHandler.AS;

/**
 * This class has the purpose of keeping information about coverage aliases inside 1 query (e.g. "for c in mr" means
 * that c is an alias for mr in this query).  1 instance of this object is created for every Wcps query.
 *
 * @author <a href="merticariu@rasdaman.com">Vlad Merticariu</a>
 * @author <a href="mailto:b.phamhuu@jacobs-university.de">Bang Pham Huu</a>
 */
@Service
// Create a new instance of this bean for each request (so it will not use the old object with stored data)
@Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CoverageAliasRegistry {

    @Autowired
    private CollectionAliasRegistry collectionAliasRegistry;

    // e.g. FOR $c in (test_cov1, test_cov2)
    // Pair<String, String> of test_cov1:coverageId and test_cov1:rasdamanCollectionName
    private LinkedHashMap<String, Pair<String, String>> forClauseListCoverageMappings = new LinkedHashMap<>();

    // NOTE: a coverage variable can be alias for multiple coverage names
    private LinkedHashMap<String, Pair<String, String>> coverageMappings = new LinkedHashMap<>();

    // Used internally in petascope for SCALE() when a pyramid member is selected instead of a base coverage exists in FOR clause list
    public static final String DOWNSCALED_COVERAGE_ALIAS_PATTERN = "_";
    
    public CoverageAliasRegistry() {
        
    }

    /**
     * e.g.baseCoverageAlias is co -> return co_0 which points to a pyramid member coverage
     */
    public String getNextDownscaledCoverageAlias(String baseCoverageAlias) {
        int i = 0;
        for (String coverageAlias : this.forClauseListCoverageMappings.keySet()) {
            if (coverageAlias.startsWith(baseCoverageAlias + DOWNSCALED_COVERAGE_ALIAS_PATTERN)) {
                i++;
            }
        }

        return baseCoverageAlias + DOWNSCALED_COVERAGE_ALIAS_PATTERN + i;
    }

    // -- For clause list

    public void addCoverageToForClauseListMapping(String coverageAlias, String coverageName, String rasdamanCollectionName) {
        forClauseListCoverageMappings.put(coverageAlias, new Pair<>(coverageName, rasdamanCollectionName));
    }

    public boolean existsInForClauseListMapping(String coverageAlias) {
        return this.forClauseListCoverageMappings.containsKey(coverageAlias);
    }

    /**
     * Mark that this coverageAlias from FOR clause should exist in the final FROM clause in rasql query
     */
    public void copyFromForClauseMappingToUsedCoverageAliasMapping(String coverageAlias) {
        Pair<String, String> pair = forClauseListCoverageMappings.get(coverageAlias);
        coverageMappings.put(coverageAlias, pair);
    }

    // -- Common functions

    /**
     * e.g. c0 -> base_cov changes to c0 -> pyramid_cov
     */
    public void updateCoverageMapping(String coverageAlias, String coverageName, String rasdamanCollectionName) {
         coverageMappings.put(coverageAlias, new Pair<> (coverageName, rasdamanCollectionName));
    }

    public void remove(String coverageAlias) {
        coverageMappings.remove(coverageAlias);
    }

    /* Always get the first coverageName in the arrayList to create defaultRasql query
    * e.g: for c in (mr, rgb) ... then rgb can be used later to create another Rasql query for multipart
    * NOTE: due to coverage variable name (e.g: c) and axis iterator is same syntax (e.g: $px)
    * then check if the alias is for an existing coverage first, if not then check if it is an axis iterator.
    */
    public String getCoverageName(String alias) {
        String coverageName = null;
        if (coverageMappings.get(alias) != null) {
            coverageName = coverageMappings.get(alias).fst;
        }

        return coverageName;
    }
    
    /**
     * Get rasdamanCollectionName by coverage's alias (e.g: for c in (test_mean_summer_airtemp))
     * test_mean_summer_airtemp is coverageName, test_mean_sumer_airtemp_datetime is rasdamanCollectionName
     * @param alias: coverage iterator (c)
     * @return rasdamanCollectionName
     */
    public String getRasdamanCollectionNameByAlias(String alias) {
        String rasdamanCollectionName = null;
        if (coverageMappings.get(alias) != null) {
            rasdamanCollectionName = coverageMappings.get(alias).snd;
        }
        return rasdamanCollectionName;
    }
    
    /**
     * Return the alias by a coverage name (e.g: for c in (test_mr, test_rgb))
     * 
     * @param coverageName input coverage name (e.g: test_mr)
     * @return alias (e.g: c)
     */
    public String getAliasByCoverageName(String coverageName) {
        for (Map.Entry<String, Pair<String, String>> entry : this.coverageMappings.entrySet()) {
            Pair<String, String> pair = entry.getValue();
            if (pair.fst.equals(coverageName)) {
                return entry.getKey();
            }

        }

        return null;
    }


    /**
     * Return the map of coverage Ids -> coverage alias
     * e.g: test_mean_summer_airtemp -> $c
     */
    public Map<String, String> getCoverageAliasMap() {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, Pair<String, String>> entry : this.coverageMappings.entrySet()) {
            String alias = entry.getKey();
            Pair<String, String> pair = entry.getValue();
            String coverageId = pair.fst;
            map.put(coverageId, alias.replace("$", ""));
        }
        
        return map;
    }

    // -- Final steps

    public LinkedHashMap<String, Pair<String, String>> getFinalCoverageAliasMappings(ReturnClauseHandler returnClauseHandler) throws PetascopeException {
        LinkedHashMap<String, Pair<String, String>> result = new LinkedHashMap<>();
        Queue<Handler> queue = new ArrayDeque<>();
        queue.add(returnClauseHandler);

        while (!queue.isEmpty()) {
            Handler currentHandler = queue.remove();
            if (currentHandler instanceof CoverageVariableNameHandler) {
                // e.g. co or co_0
                String coverageAlias = ((StringScalarHandler)(currentHandler.getFirstChild())).getValue();
                Pair<String, String> forClauseListPair = this.forClauseListCoverageMappings.get(coverageAlias);
                result.put(coverageAlias, forClauseListPair);
            }


            for (Handler childHandler : currentHandler.getChildren()) {
                if (childHandler != null) {
                    queue.add(childHandler);
                }
            }
        }

        return result;

    }

    /**
     * Return the string representing this Map of coverage iterators and rasdaman collection names
     * e.g: c -> test_mean_summer_airtemp, d -> test_mean_summer_airtemp_repeat
     * @return String: test_mean_summer_airtemp as c, test_mean_summer_airtemp_repeat as d
     */
    public String getRasqlFromClause() {

        List<String> list = new ArrayList<>();
        for (Map.Entry<String, Pair<String, String>> entry : this.coverageMappings.entrySet()) {
            String coverageIterator = StringUtil.stripDollarSign(entry.getKey());
            List<String> tmpList = new ArrayList<>();
            Pair<String, String> pair = entry.getValue();
            String coverageId = pair.fst;
            String rasdamanCollectionName = pair.snd;
            // e.g: test_mean_summer_airtemp as c, not test_mean_summer_airtemp as $c
            if (pair.snd != null) {
                tmpList.add(pair.snd + " " + AS + " " + coverageIterator);
            }

            // e.g: test_mean_summer_airtemp as c
            if (tmpList.size() > 0) {
                String tmpOuput = ListUtil.join(tmpList, ", ");
                list.add(tmpOuput);
            }

            // Populate alias expressions to collection alias registry as well
            if (this.collectionAliasRegistry.getAliasName(coverageIterator) == null && rasdamanCollectionName != null) {
                this.collectionAliasRegistry.add(coverageIterator, coverageId, rasdamanCollectionName);
            }
        }

        String result = "";

        // FROM test_mean_summer_airtemp as c, test_mean_summer_airtemp as d
        if (list.isEmpty()) {
            // In case there is no coverageVariableName used in RETURN clause
            for (Map.Entry<String, Pair<String, String>> entry : this.forClauseListCoverageMappings.entrySet()) {
                String coverageAlias = entry.getKey();
                String rasdamanCollectionName = entry.getValue().snd;

                list.add(rasdamanCollectionName + " AS " + StringUtil.stripDollarSign(coverageAlias));
                break;
            }

        }

        result = " FROM " + ListUtil.join(list, ", ");

        return result;
    }

    /**
     * As this bean exists for a HTTP request, so in case of WCS multipart, it still contains the data from the first WCPS query, so need to clear it
     */
    public void clear() {
        coverageMappings = new LinkedHashMap<>();
        forClauseListCoverageMappings = new LinkedHashMap<>();
    }
}
