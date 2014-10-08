package petascope.wcps2.error.managed.processing;

import petascope.wcps2.metadata.Interval;

/**
 * Error occurring when the lowerBound is larger than the upperBound
 *
 * @author <a href="mailto:alex@flanche.net">Alex Dumitru</a>
 * @author <a href="mailto:vlad@flanche.net">Vlad Merticariu</a>
 */
public class UnorderedSubsetException extends InvalidSubsettingException {

    /**
     * Constructor for the class
     *
     * @param axisName the axis on which the subset is being made
     * @param subset   the offending subset
     */
    public UnorderedSubsetException(String axisName, Interval<String> subset) {
        super(axisName, subset);
    }
}