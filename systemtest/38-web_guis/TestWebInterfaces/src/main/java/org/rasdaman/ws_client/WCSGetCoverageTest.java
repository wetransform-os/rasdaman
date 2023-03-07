/*
  *  This file is part of rasdaman community.
  * 
  *  Rasdaman community is free software: you can redistribute it and/or modify
  *  it under the terms of the GNU General Public License as published by
  *  the Free Software Foundation, either version 3 of the License, or
  *  (at your option) any later version.
  * 
  *  Rasdaman community is distributed in the hope that it will be useful,
  *  but WITHOUT ANY WARRANTY; without even the implied warranty of
  *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
  *  See the GNU  General Public License for more details.
  * 
  *  You should have received a copy of the GNU  General Public License
  *  along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
  * 
  *  Copyright 2003 - 2017 Peter Baumann / rasdaman GmbH.
  * 
  *  For more information please see <http://www.rasdaman.org>
  *  or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package org.rasdaman.ws_client;

import java.io.IOException;
import org.apache.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.Select;

/**
 * Class to test wcs_client, tab WCS/GetCapabilities
 *
 * @author <a href="mailto:bphamhuu@jacobs-university.net">Bang Pham Huu</a>
 */
public class WCSGetCoverageTest extends WSAbstractSectionWebPageTest {

    private static final Logger log = Logger.getLogger(WCSGetCapabilitiesTest.class);
    
    protected String downloadCoverageButtonXPath = "/html/body/div[2]/div/div/div/div/div/div/div[1]/div/ul/div/div/div/div[3]/div/div/div[1]/div[5]/button";

    public WCSGetCoverageTest() {
        super();
        this.sectionName = "wcs_get_coverage";
    }

    /**
     * Focus on the iframe then select the specified tab.
     *
     * @param webDriver
     * @throws InterruptedException
     */
    protected void focusOnGetCoverageTab(WebDriver webDriver) throws InterruptedException, IOException {
        // NOTE: As the first window lost focus, so it has to refous to iframe and GetCoverage tab, before it can select elements in this tab.
        // Switch to iframe to parse the web element
//        this.switchToIFirstIframe(webDriver);
        
        // First, change to tab GetCoverage
        this.clickOnElement(webDriver, "/html/body/div[2]/div/div/div/div/div/div/div[1]/div/ul/div/div/ul/li[3]/a");
    }
    
    @Override
    public void runTest(WebDriver webDriver) throws InterruptedException, IOException {
        webDriver.navigate().to(this.testURL);
        log.info("*** Testing test cases on Web URL '" + testURL + "', section '" + this.sectionName + "'. ***");
        
        this.waitForPageLoad(webDriver);
        
        // First, change to tab GetCoverage
        this.clickOnElement(webDriver, "/html/body/div[2]/div/div/div/div/div/div/div[1]/div/ul/div/div/ul/li[3]/a");

        String testCaseName;
        Select dropdown;

        // Then get coverage's metadata on this tab
        testCaseName = this.getSectionTestCaseName("get_a_coverage_metadata");
        log.info("Testing get coverage's metadata...");
        // First change the coverage id in text box
        this.addTextToTextBox(webDriver, "test_mean_summer_airtemp", "/html/body/div[2]/div/div/div/div/div/div/div[1]/div/ul/div/div/div/div[3]/div/div/div[1]/div[1]/div/input");

        String selectCoverageButtonXPath = "/html/body/div[2]/div/div/div/div/div/div/div[1]/div/ul/div/div/div/div[3]/div/div/div[1]/div[1]/div/span[2]/button";
        // Then click on the Get Coverage button
        this.runTestByClickingOnElement(webDriver, testCaseName, selectCoverageButtonXPath);

        // ******************** Test HTTP GET request ***********************
        
        // Download a coverage in PNG with subsettingCRS, outputCRS and clipping polygon
        testCaseName = this.getSectionTestCaseName("get_clipped_crs_2D_coverage_in_png");
        log.info("Testing get clippied coverage (with subsettingCRS, outputCRS) with encoding as 2D PNG...");
        // Then select coverage as png
        dropdown = new Select(webDriver.findElement(By.xpath("//*[@id=\"select-coverage-format\"]")));
        dropdown.selectByVisibleText("image/png");
        // Then need to click on dropdown tab to show contents otherwise it cannot add values to text boxes (CRS and clipping dropdowns)
        this.clickOnElement(webDriver, "/html/body/div[2]/div/div/div/div/div/div/div[1]/div/ul/div/div/div/div[3]/div/div/div[1]/div[4]/div[5]/uib-accordion/div/div/div[1]/h4/a/span/i");
        this.clickOnElement(webDriver, "/html/body/div[2]/div/div/div/div/div/div/div[1]/div/ul/div/div/div/div[3]/div/div/div[1]/div[4]/div[6]/uib-accordion/div/div/div[1]/h4/a/span/i");

        // Then add URL for subsettingCRS
        this.addTextToTextBox(webDriver, "http://localhost:8080/def/crs/EPSG/0/3857", "//*[@id=\"wcs-get-coverage-subsetting-crs\"]");
        // Then add URL for outputCRS
        this.addTextToTextBox(webDriver, "http://localhost:8080/def/crs/EPSG/0/4326", "//*[@id=\"wcs-get-coverage-output-crs\"]");
        // Then add WKT in EPSG:3857
        this.addTextToTextBox(webDriver, "POLYGON((13589894.568 -2015496.69612, 15086830.0246 -1780682.3822, 16867507.7313 -2856914.62008, "
                + "15302077.392 -2269868.21355, 16906647.6642 -3913582.33674, 15096615.0078 -2514467.00655, 16182636.8281 -4510392.53842, "
                + "14812883.8897 -2807984.60711, 15125969.9576 -4128820.82664, 14548722.738 -2964529.36373, "
                + "14548722.738 -3737465.90606, 14333475.3706 -3071780.21294, 13971464.3866 -3854876.71085, "
                + "14088873.0535 -3042805.48806, 13159399.8332 -4167958.77195, 13834485.7532 -2984102.2771, "
                + "12846313.7653 -3590703.84695, 13619249.5177 -2788419.51082, 12699550.1487 -3140648.57605, "
                + "13423560.9848 -2661225.99144, 12777830.0146 -2553611.15296, 13198539.7662 -2445988.6568))",
                "//*[@id=\"wcs-get-coverage-clipping\"]");
        // Then click on the Download Coverage button which will open a new window
        this.runTestByClickingOnElementAndCaptureTheOpenedWindow(webDriver, testCaseName, downloadCoverageButtonXPath);

        // Download a subset coverage in PNG 
        String minLatXPath = "/html/body/div[2]/div/div/div/div/div/div/div[1]/div/ul/div/div/div/div[3]/div/div/div[1]/div[4]/div[2]/uib-accordion/div/div/div[2]/div/div/div[2]/div/div[1]/div/div[2]/ul/li[1]/input[3]";
        String minLongXPath = "/html/body/div[2]/div/div/div/div/div/div/div[1]/div/ul/div/div/div/div[3]/div/div/div[1]/div[4]/div[2]/uib-accordion/div/div/div[2]/div/div/div[2]/div/div[2]/div/div[2]/ul/li[1]/input[3]";
        
        testCaseName = this.getSectionTestCaseName("get_subset_2D_coverage_in_png");
        log.info("Testing get subset coverage with encoding as 2D PNG...");
        // Clear what has been done above
        this.addTextToTextBox(webDriver, " ", "//*[@id=\"wcs-get-coverage-subsetting-crs\"]");
        this.addTextToTextBox(webDriver, " ", "//*[@id=\"wcs-get-coverage-output-crs\"]");
        this.addTextToTextBox(webDriver, " ", "//*[@id=\"wcs-get-coverage-clipping\"]");
        // Then select coverage as png
        dropdown = new Select(webDriver.findElement(By.xpath("//*[@id=\"select-coverage-format\"]")));
        dropdown.selectByVisibleText("image/png");
        // Then subset on Lat axis (min lat)
        this.addTextToTextBox(webDriver, "-20.5", minLatXPath);
        // Then subset on Lon axis (min Lon)
        this.addTextToTextBox(webDriver, "135.5", minLongXPath);
        // Then click on the Download Coverage button which will open a new window
        this.runTestByClickingOnElementAndCaptureTheOpenedWindow(webDriver, testCaseName, downloadCoverageButtonXPath);

        // Refocus on the tab
        this.focusOnGetCoverageTab(webDriver);

    }
}
