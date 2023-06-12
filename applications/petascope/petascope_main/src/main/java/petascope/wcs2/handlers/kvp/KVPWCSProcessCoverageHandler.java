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
package petascope.wcs2.handlers.kvp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import petascope.core.service.GdalFileToCoverageTranslatorService;
import petascope.util.ListUtil;
import petascope.wcps.metadata.service.CoverageAliasRegistry;
import petascope.exceptions.WCSException;
import petascope.core.response.Response;
import org.slf4j.LoggerFactory;
import petascope.exceptions.*;
import petascope.wcps.result.executor.WcpsExecutor;
import petascope.wcps.result.executor.WcpsExecutorFactory;
import petascope.wcps.parser.WcpsTranslator;
import petascope.wcps.result.VisitorResult;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

import org.rasdaman.config.ConfigManager;
import static org.rasdaman.config.ConfigManager.UPLOADED_FILE_DIR_TMP;
import org.rasdaman.domain.cis.Coverage;
import org.rasdaman.repository.service.CoverageRepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static petascope.controller.AbstractController.getValueByKeyAllowNull;
import static petascope.controller.AbstractController.getValueByKey;
import petascope.controller.PetascopeController;
import petascope.controller.handler.service.XMLWCSServiceHandler;
import petascope.core.KVPSymbols;

import static petascope.core.KVPSymbols.KEY_QUERY;
import static petascope.core.KVPSymbols.KEY_QUERY_SHORT_HAND;
import petascope.core.Pair;
import petascope.util.StringUtil;
import static petascope.util.StringUtil.POSITIONAL_PARAMETER_PATTERN;

import petascope.wcps.metadata.service.TempCoverageRegistry;
import petascope.wcps.result.WcpsMetadataResult;
import petascope.wcps.result.WcpsResult;
import petascope.wcst.handlers.InsertCoverageHandler;

/**
 * Handler for the Process Coverages Extension
 *
 * @author <a href="mailto:alex@flanche.net">Alex Dumitru</a>
 * @author <a href="mailto:vlad@flanche.net">Vlad Merticariu</a>
 */
@Service
public class KVPWCSProcessCoverageHandler extends KVPWCSAbstractHandler {

    private static org.slf4j.Logger log = LoggerFactory.getLogger(KVPWCSProcessCoverageHandler.class);

    @Autowired
    private WcpsTranslator wcpsTranslator;
    @Autowired
    private WcpsExecutorFactory wcpsExecutorFactory;
    @Autowired
    private CoverageAliasRegistry coverageAliasRegistry;
    @Autowired
    private GdalFileToCoverageTranslatorService gdalFileToCoverageTranslatorService;
    @Autowired
    private CoverageRepositoryService coverageRepositoryService;
    @Autowired
    private TempCoverageRegistry tempCoverageRegistry;
    @Autowired
    private InsertCoverageHandler insertCoverageHandler;
    @Autowired
    private HttpServletRequest httpServletRequest;
    @Autowired
    private PetascopeController petascopeController;    
    @Autowired
    private XMLWCSServiceHandler xmlWCSServiceHandler;

    final Pattern forClausePattern = Pattern.compile("FOR\\s+");
    final Pattern letClausePattern = Pattern.compile("LET\\s+");
    final Pattern whereClausePattern = Pattern.compile("WHERE\\s+");
    final Pattern returnClausePattern = Pattern.compile("RETURN\\s+");


    public KVPWCSProcessCoverageHandler() {
    }

    @Override
    public void validate(Map<String, String[]> kvpParameters) throws PetascopeException, SecoreException, WMSException {
    }

    /**
     * Handles a general WCPS request and delegates the execution to the
     * corresponding internal services based on the version of the request.
     *
     * @return the result of the processing as a Response object
     */
    @Override
    public Response handle(Map<String, String[]> kvpParameters) throws PetascopeException, WCSException, SecoreException, WMSException, Exception {
        // Validate before handling the request
        this.validate(kvpParameters);

        String coverageID = null;
        String inputWcpsQuery = getValueByKeyAllowNull(kvpParameters, KEY_QUERY);
        if (inputWcpsQuery == null) {
            // don't allow null, it's an error if no query is provided
            inputWcpsQuery = getValueByKey(kvpParameters, KEY_QUERY_SHORT_HAND);
        }

        inputWcpsQuery = inputWcpsQuery.trim();
        
        if (inputWcpsQuery.startsWith("<")) {
            // In this case, this wcps query is encoded in XML wrapper
            Map<String, String[]> tmpMaps = this.xmlWCSServiceHandler.parseRequestBodyToKVPMaps(inputWcpsQuery);
            inputWcpsQuery = getValueByKey(tmpMaps, KEY_QUERY);
        }
        // get response
        String mimeType = null;

        List<String> wcpsQueries = this.getWcpsQueries(inputWcpsQuery);
        boolean multipart = wcpsQueries.size() > 1;

        List<String> finalRasqlQueries = new ArrayList<>();
        WcpsResult wcpsResult = null;
        WcpsExecutor executor = null;
        VisitorResult visitorResult = null;

        List<byte[]> results = new ArrayList<>();

        for (String wcpsQuery : wcpsQueries) {
            log.debug("Handling WCPS query: " + wcpsQuery);
            wcpsQuery = this.adjustWcpsQueryByPositionalParameters(kvpParameters, wcpsQuery);

            visitorResult = wcpsTranslator.translate(wcpsQuery);
            executor = wcpsExecutorFactory.getExecutor(visitorResult);

            if (visitorResult instanceof WcpsMetadataResult) {
                byte[] bytes = executor.execute(visitorResult);
                results.add(bytes);
            } else {
                wcpsResult = (WcpsResult) visitorResult;
                // In case of 0D, metadata is null
                if (wcpsResult.getMetadata() != null) {
                    coverageID = wcpsResult.getMetadata().getCoverageName();
                }

                // create multiple rasql queries from a Rasql query result (if it is multipart)
                String finalRasqlQuery = wcpsResult.getFinalRasqlQuery();
                finalRasqlQueries.add(finalRasqlQuery);
            }

            // set metadata and return
            mimeType = visitorResult.getMimeType();
        }

        try {
            // Run all the Rasql queries and get result
            for (String rasqlQuery : finalRasqlQueries) {
                // Execute multiple Rasql queries with different coverageIDs to get List of byte arrays
                wcpsResult.setRasql(rasqlQuery);
                byte[] bytes = executor.execute(visitorResult);
                results.add(bytes);
            }
        } finally {
            this.coverageAliasRegistry.clear();
            this.tempCoverageRegistry.clear();
        }

        return new Response(results, mimeType, coverageID);
    }

    /**
     * Given input WCPS query, separate it to multiple smaller WCPS queries in case it should return multipart result.
     * e.g. for c in (test1, test2, d in (test3)
     * then the result is a multipart from two queries:
     *  - for c in (test1), d in (test3)
     *  - for c in (test2), d in (test3)
     *
     */
    private List<String> getWcpsQueries(String inputWcpsQuery) {
        String upperCaseQueryTmp = inputWcpsQuery.toUpperCase();

        int forClauseIndex = StringUtil.getIndexOfPattern(forClausePattern, upperCaseQueryTmp);

        int letClauseIndex = StringUtil.getIndexOfPattern(letClausePattern, upperCaseQueryTmp);
        int whereClauseIndex = StringUtil.getIndexOfPattern(whereClausePattern, upperCaseQueryTmp);
        int returnClauseIndex = StringUtil.getIndexOfPattern(returnClausePattern, upperCaseQueryTmp);

        int lastIndex = letClauseIndex;
        if (letClauseIndex == -1) {
            if (whereClauseIndex != -1) {
                lastIndex = whereClauseIndex;
            } else {
                lastIndex = returnClauseIndex;
            }

            if (whereClauseIndex > returnClauseIndex) {
                // In this case: WHERE clause exists in condenser of RETURN clause
                lastIndex = returnClauseIndex;
            }
        }

        // e.g. $c in test1, test2, d in test3
        String forClauseListStr = inputWcpsQuery.trim().substring(forClauseIndex + 3, lastIndex);

        // e.g. return ...
        String restOfQuery = inputWcpsQuery.substring(lastIndex);

        List<String> parts = new ArrayList<>();
        // case insensitive
        String[] tmps = forClauseListStr.split(" (?i)in ");

        // co
        parts.add(tmps[0]);

        String tmp = StringUtil.stripOpenAndCloseParentheses(tmps[1]);

        // test1, test2
        parts.add(tmp);


        for (int i = 2; i < tmps.length; i++) {
            String previousTmp = tmps[i - 1].replaceAll("\\s+", "").trim();
            previousTmp = StringUtil.stripOpenAndCloseParentheses(previousTmp);

            tmp = tmps[i].trim();
            tmp = StringUtil.stripOpenAndCloseParentheses(tmp);

            String[] partsTmp = previousTmp.split(",");
            if (partsTmp.length > 1) {
                String fixedPreviousTmp = "";
                for (int j = 0; j < partsTmp.length - 1; j++) {
                    fixedPreviousTmp += partsTmp[j] + ", ";
                }

                fixedPreviousTmp = fixedPreviousTmp.substring(0, fixedPreviousTmp.length() - 2);

                String alias = partsTmp[partsTmp.length - 1];

                parts.set(parts.size() - 1, fixedPreviousTmp);
                parts.add(parts.size(), alias);
                parts.add(parts.size(), tmp);

            }
        }

        List<List<String>> nestedList = new ArrayList<>();

        for (int i = 0; i < parts.size(); i += 2) {
            String coverageAlias = parts.get(i);
            List<String> coverageIds = Arrays.asList(parts.get(i + 1).split(", "));
            for (int j = 0; j < coverageIds.size(); j++) {
                String coverageId = coverageIds.get(j).trim();
                if (!coverageId.startsWith("(")) {
                    coverageId = "( " + coverageId + " )";
                }
                coverageIds.set(j, coverageAlias + " in " + coverageId);
            }

            nestedList.add(coverageIds);
        }

        List<List<String>> mergedList = ListUtil.cartesianProduct(nestedList);
        List<String> results = new ArrayList<>();
        for (int i = 0; i < mergedList.size(); i++) {
            String newWcpsQuery = "FOR " + ListUtil.join(mergedList.get(i), ", ") + "\n " + restOfQuery;
            results.add(newWcpsQuery);
        }

        return results;
    }
    
    /**
     * Build a wcpsQuery which will return a Rasql query
     */
    public String buildRasqlQuery(String wcpsQuery) throws WCPSException, PetascopeException {
        VisitorResult visitorResult = wcpsTranslator.translate(wcpsQuery);
        WcpsResult wcpsResult = (WcpsResult) visitorResult;        
        String rasql = wcpsResult.getFinalRasqlQuery();
        
        return rasql;
    }

    /**
     * Process a WCPS query and returns the Response
     */
    public Response processQuery(final String wcpsQuery) throws Exception {
        Map<String, String[]> kvpParameters = new HashMap<String, String[]>() {};
        StringUtil.putKeyToKVPMaps(kvpParameters, KVPSymbols.KEY_QUERY, wcpsQuery);
        return this.handle(kvpParameters);
    }

    
    /**
     * Replace the positional parameters with proper values
     * For example: for $c in (covA), $d in (decode($1)) return $c + $d + $2
     * -> for $c in (covA), $d in (decode($1)) return $c + $d + 5
     * with $1 is TMP_COV (created from an uploaded file) and $2=5 (uploaded in query POST body)
     */
    private String adjustWcpsQueryByPositionalParameters(Map<String, String[]> kvpParameters, String wcpsQuery) throws PetascopeException, SecoreException {
        
        StringBuffer stringBuffer = new StringBuffer();
        Matcher matcher = POSITIONAL_PARAMETER_PATTERN.matcher(wcpsQuery);
        List<String> uploadedFilePaths = new ArrayList<>();
        
        try {
        
            while (matcher.find()) {
                // e.g: $1, $2,...
                String positionalParameter = matcher.group();
                String value = getValueByKeyAllowNull(kvpParameters, StringUtil.stripDollarSign(positionalParameter));

                if (value != null) {
                    if (value.startsWith(UPLOADED_FILE_DIR_TMP)) {
                        String filePath = value;
                        uploadedFilePaths.add(filePath);

                        // e.g: $1 -> /tmp/rasdaman_petacope/rasdaman...tif (uploaded file in POST body)
                        //      $2 -> 5 (uploaded value in POST body)
                        Coverage coverage = this.gdalFileToCoverageTranslatorService.translate(filePath);

                        // @TODO: until rasdaman works without error with SELECT decode() in rasql, a temp collection will be created for a temp coverage
                        String decodeExpression = coverage.getRasdamanRangeSet().getDecodeExpression();
                        this.insertCoverageHandler.insertTempCoverage(coverage, decodeExpression);
                        // @TODO: a coverage from decode() does not have a rasdaman collection name. 
                        // It sets this temp collection until the rasql SELECT decode() works fine
                        coverage.getRasdamanRangeSet().setCollectionName(coverage.getCoverageId());

                        String coverageId = coverage.getCoverageId();
                        this.coverageRepositoryService.putToLocalCacheMap(coverageId, new Pair<>(coverage, true));

                        // e.g: $1 -> (TEMP_COV_abc_202001010, /tmp/rasdaman_petacope/rasdaman...tif)
                        this.tempCoverageRegistry.add(positionalParameter, coverageId, filePath, coverage.getRasdamanRangeSet().getCollectionType());
                    } else {                
                        // e.g: replace $2 in query with 5
                        String replacement = Matcher.quoteReplacement(value);
                        matcher.appendReplacement(stringBuffer, replacement);
                    }
                }
            }
                        
        } finally {
            // remove the uploaded file afterwards
            for (String filePath : uploadedFilePaths) {
                try {
                    log.debug("Removing temporary uploaded file: " + filePath);
                    Files.deleteIfExists(Paths.get(filePath));
                } catch (IOException ex) {
                    log.warn("Cannot delete uploaded file '" + filePath + "'. Reason: " + ex.getMessage());
                }
            }
        }
        
        matcher.appendTail(stringBuffer);
        String result = stringBuffer.toString();

        log.debug("Adjusted positional parameters in WCPS query, result: " + result);
        return result;
    }
}
