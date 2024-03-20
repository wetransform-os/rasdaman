<?xml version="1.0" encoding="UTF-8"?>
<gmlcov:RectifiedGridCoverage
        xmlns='http://www.opengis.net/gml/3.2'
        xmlns:gml='http://www.opengis.net/gml/3.2'
        xmlns:gmlcov='http://www.opengis.net/gmlcov/1.0'
        xmlns:swe='http://www.opengis.net/swe/2.0'
        xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'
        xmlns:gmlrgrid='http://www.opengis.net/gml/3.3/rgrid'
        gml:id="test_mr_TO_BE_DELETED"
        >

<gmlcov:metadata>
{
  "resolution": "1"
}
</gmlcov:metadata>

<boundedBy>
    <Envelope srsName="http://localhost:8080/rasdaman/def/crs/OGC/0/Index2D" axisLabels="i j" uomLabels="GridSpacing GridSpacing" srsDimension="2">
        <lowerCorner>0 0</lowerCorner>
        <upperCorner>60 40</upperCorner>
    </Envelope>
</boundedBy>


<domainSet>
    <gml:RectifiedGrid dimension="2" gml:id="grid">
        <limits>
            <GridEnvelope>
                <low>0 0</low>
                <high>0 0</high>
            </GridEnvelope>
        </limits>
        <axisLabels>i j</axisLabels>
        <gml:origin>
            <Point gml:id="origin" srsName="http://localhost:8080/rasdaman/def/crs/OGC/0/Index2D"
                   axisLabels="i j" uomLabels="GridSpacing GridSpacing" srsDimension="2">
                <pos>0.50 39.5</pos>
            </Point>
        </gml:origin>

        <gml:offsetVector srsName="http://localhost:8080/rasdaman/def/crs/OGC/0/Index2D" axisLabels="i j" uomLabels="GridSpacing GridSpacing" srsDimension="2">
            1.0 0
        </gml:offsetVector>
         <gml:offsetVector srsName="http://localhost:8080/rasdaman/def/crs/OGC/0/Index2D" axisLabels="i j" uomLabels="GridSpacing GridSpacing" srsDimension="2">
            0 -1
        </gml:offsetVector>


    </gml:RectifiedGrid>
</domainSet>


<gml:rangeSet>
    <gml:DataBlock>
        <gml:rangeParameters/>
        <gml:tupleList>
            0
        </gml:tupleList>
    </gml:DataBlock>
</gml:rangeSet>


<gmlcov:rangeType>
    <swe:DataRecord>
        <swe:field name="value">
            <swe:Quantity definition="">
                <swe:description></swe:description>
                <swe:nilValues>
                    <swe:NilValues>

                    </swe:NilValues>
                </swe:nilValues>
                <swe:uom code=""/>
            </swe:Quantity>
        </swe:field>

    </swe:DataRecord>
</gmlcov:rangeType>


</gmlcov:RectifiedGridCoverage>
