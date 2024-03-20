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
 * Copyright 2003 - 2022 Peter Baumann / rasdaman GmbH.
 *
 * For more information please see <http://www.rasdaman.org>
 * or contact Peter Baumann via <baumann@rasdaman.com>.
 */
package petascope.wcps.handler;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import petascope.exceptions.ExceptionCode;
import petascope.exceptions.PetascopeException;
import petascope.wcps.result.VisitorResult;

/**
 * Abstract class for other WCPS handlers.
 * Each handler has:
 * - A parent (root handler doesn't have parent) 
 * - A list of children handlers (if a handler is terminal, then it has no children)
 * 
 * @author Bang Pham Huu <b.phamhuu@jacobs-university.de>
 */

// NOTE: this annotation is used for Jackson to know which subclass of this class should be used to serialize / deserialize
@JsonTypeInfo(
        use = JsonTypeInfo.Id.MINIMAL_CLASS,
        include = JsonTypeInfo.As.PROPERTY,
        property = "@class")
public abstract class Handler {

    @JsonIgnore
    private Handler parent;
    private List<Handler> children;

    @JsonIgnore
    // NOTE: this list contains injected (@Autowired) services in children handlers
    // which they are null when being cloned by Jackson.
    // Used when substituting $coverageVariableNames (in SCALE() handler) referenced to the ones specified in LET clause handler.
    protected List<Object> injectedServicesRegistries;

    /**
     * This list contains a list of handler nodes (Class name) which has been updated for optimization
     * so they will be not touched anymore
     */
    private List<String> updatedHandlers = new ArrayList<>();
    // NOTE: to tell which index of in its parent's children list
    private int indexInParentHandler = 0;

    // If a node is removed from the query tree -> true
    protected boolean isQueryTreeUpdated = false;


    public Handler() {
        
    }

    public Handler(List<Handler> childHandlers) {
        
        if (childHandlers != null) {
            this.children = new ArrayList<>();
            for (Handler handler : childHandlers) {
                if (handler != null) {
                    this.children.add(handler);
                }
            }
        }
        
        if (childHandlers != null) {
            for (Handler childHandler : childHandlers) {
                if (childHandler != null) {
                    childHandler.setParent(this);
                }
            }
        }
    }
    
//    /**
//     * NOTE: cannot use constructor directly, it has this below problem with Spring.
//     * Error creating bean with name: Requested bean is currently in creation.
//     */
//    public static Handler create(Handler self, List<Handler> children) {
//        Handler obj = new Handler(self, children);
//        return obj;
//    }
    
    public Handler getParent() {
        return parent;
    }

    public void setParent(Handler parent) {
        this.parent = parent;
    }
    
    public void setChildren(List<Handler> handlers) {
        if (handlers != null) {
            this.children = new ArrayList<>();
            for (Handler handler : handlers) {
                this.children.add(handler);
                
                if (handler != null) {
                    handler.setParent(this);
                }
            }
        }
    }

    public List<Handler> getChildren() {
        if (children == null) {
            children = new ArrayList<>();
        }
        return children;
    }

    public List<Handler> getNonNullChildren() {
        List<Handler> results = new ArrayList<>();
        for (Handler handler : this.getChildren()) {
            if (handler != null) {
                results.add(handler);
            }
        }

        return results;
    }

    @JsonIgnore
    public Handler getFirstChild() throws PetascopeException {
        if (children.size() < 1) {
            throw new PetascopeException(ExceptionCode.InternalComponentError, 
                    "Handler: " + this.getClass().getSimpleName() + " has not enough children to get the first child handler.");
        }
        return children.get(0);
    }

    @JsonIgnore
    public Handler getSecondChild() throws PetascopeException {
        if (children.size() < 2) {
            throw new PetascopeException(ExceptionCode.InternalComponentError, 
                    "Handler: " + this.getClass().getSimpleName() + " has not enough children to get the second child handler.");
        }        
        return children.get(1);
    }

    @JsonIgnore
    public Handler getThirdChild() throws PetascopeException {
        if (children.size() < 3) {
            throw new PetascopeException(ExceptionCode.InternalComponentError, 
                    "Handler: " + this.getClass().getSimpleName() + " has not enough children to get the thrid child handler.");
        }  
        return children.get(2);
    }

    @JsonIgnore
    public Handler getFourthChild() throws PetascopeException {
        if (children.size() < 4) {
            throw new PetascopeException(ExceptionCode.InternalComponentError, 
                    "Handler: " + this.getClass().getSimpleName() + " has not enough children to get the fourth child handler.");
        }  
        return children.get(3);
    }

    @JsonIgnore
    public Handler getFifthChild() throws PetascopeException {
        if (children.size() < 5) {
            throw new PetascopeException(ExceptionCode.InternalComponentError, 
                    "Handler: " + this.getClass().getSimpleName() + " has not enough children to get the fifth child handler.");
        }  
        return children.get(4);
    }

    @JsonIgnore
    public Handler getSixthChild() throws PetascopeException {
        if (children.size() < 6) {
            throw new PetascopeException(ExceptionCode.InternalComponentError, 
                    "Handler: " + this.getClass().getSimpleName() + " has not enough children to get the sixth child handler.");
        }  
        return children.get(5);
    }

    @JsonIgnore
    public Handler getSeventhChild() throws PetascopeException {
        if (children.size() < 7) {
            throw new PetascopeException(ExceptionCode.InternalComponentError, 
                    "Handler: " + this.getClass().getSimpleName() + " has not enough children to get the seventh child handler.");
        }  
        return children.get(6);
    }

    @JsonIgnore
    public Handler getEighthChild() throws PetascopeException {
        if (children.size() < 8) {
            throw new PetascopeException(ExceptionCode.InternalComponentError, 
                    "Handler: " + this.getClass().getSimpleName() + " has not enough children to get the eighth child handler.");
        }  
        return children.get(7);
    }

    @JsonIgnore
    public Handler getNithChild() throws PetascopeException {
        if (children.size() < 9) {
            throw new PetascopeException(ExceptionCode.InternalComponentError, 
                    "Handler: " + this.getClass().getSimpleName() + " has not enough children to get the nith child handler.");
        }  
        return children.get(8);
    }

    @JsonIgnore
    public Handler getTenthChild() throws PetascopeException {
        if (children.size() < 10) {
            throw new PetascopeException(ExceptionCode.InternalComponentError, 
                    "Handler: " + this.getClass().getSimpleName() + " has not enough children to get the tenth child handler.");
        }  
        return children.get(9);
    }

    @JsonIgnore
    public void addUpdatedHandler(Handler handler) {
        this.updatedHandlers.add(handler.getClass().getName());
    }

    @JsonIgnore
    public boolean isUpdatedHandlerAlready(Handler handler) {
        for (String name : this.updatedHandlers) {
            if (name.equals(handler.getClass().getName())) {
                return true;
            }
        }

        return false;
    }

    @JsonIgnore
    public abstract VisitorResult handle(List<Object> serviceRegistries) throws PetascopeException;

    @JsonIgnore
    public Object getServiceRegistry(String qualifiedClassName) throws PetascopeException {
        for (Object service : this.injectedServicesRegistries) {
            if (service.getClass().getName().split("\\$")[0].equals(qualifiedClassName)) {
                return service;
            }
        }

        // In case qualified class name not found, then find only class name
        // NOTE: used for Spring proxy (e.g. injected HttpServletRequest)
        String[] parts = qualifiedClassName.split("\\.");
        String className = parts[parts.length - 1];

        for (Object service : this.injectedServicesRegistries) {
            String serviceName = service.toString();
            if (serviceName.contains(className)) {
                return service;
            }
        }

        throw new PetascopeException(ExceptionCode.NoApplicableCode,
                                "Injected service registry is not found. Given class name: " + qualifiedClassName);
    }

    @JsonIgnore
    protected int getChildIndexInParentsList() throws PetascopeException {
        for (int i = 0; i < this.getParent().getChildren().size(); i++) {
            Handler childHandler = this.getParent().getChildren().get(i);
            if (childHandler == this) {
                return i;
            }
        }

        throw new PetascopeException(ExceptionCode.InternalComponentError,
                                    "Cannot find index of current handler in its parent's children handlers list.");
    }

    @JsonIgnore
    // override by some subclasses when needed
    protected Handler create(Handler firstHandler, Handler secondHandle) {
        return null;
    }

    /**
     *  NOTE: Used only for SCALE() and SUBSET handler to create a wrapper handler with the input dimensionIntervalsList outside of an input handler.
     *  For example: coverageExpressionHandler: $c[i(0:30)] and childDimensionIntervalListHandler: [j(0:30)]
     *  then create a new node (in this case is ShorthandSubsetHandler: firstChild is coverageExpressionHandler and secondChild is childDimensionIntervalListHandler
     */
    private boolean createWrapperHandler(Handler coverageExpressionHandler, Handler childDimensionIntervalListHandler) throws PetascopeException {
        int index = coverageExpressionHandler.getChildIndexInParentsList();
        Handler currentParentHandlerTmp = coverageExpressionHandler.getParent();

        // Create a wrapper handler (SCALE() or ShorthandSubset) which is parent of the current checking handler
        Handler newWrapperHandler = this.create(coverageExpressionHandler, childDimensionIntervalListHandler);
        coverageExpressionHandler.addUpdatedHandler(this);
        newWrapperHandler.addUpdatedHandler(this);

        newWrapperHandler.setParent(currentParentHandlerTmp);
        currentParentHandlerTmp.getChildren().set(index, newWrapperHandler);

        return true;
    }

    /**
     * if a given Handler is an instance of one object in this list, then
     * WCPS query traversing should not go inside it.
     */
    protected boolean stopToTraverseFurtherInTree(Handler coverageExpressionHandler) {
        return (coverageExpressionHandler instanceof ReduceExpressionHandler
                || coverageExpressionHandler instanceof ScaleExpressionByDimensionIntervalsHandler
                || coverageExpressionHandler instanceof CrsTransformHandler
                || coverageExpressionHandler instanceof CrsTransformShorthandHandler
                || coverageExpressionHandler instanceof ExtendExpressionHandler
                || coverageExpressionHandler instanceof CoverageConstantHandler
                || coverageExpressionHandler instanceof CoverageConstructorHandler
                || coverageExpressionHandler instanceof CoverageGeneralCondenserHandler
                || coverageExpressionHandler instanceof AbstractClipExpressionHandler
                || coverageExpressionHandler instanceof FlipExpressionHandler
        );
    }

    /**
     * Pushdown handlers (scale() and slice/trim() to further suitable children nodes).
     *
     * e.g. scale((c + d), {Lat:"CRS:1"(0:30)}) -> scale(c, {Lat:"CRS:1"(0:30)}) + scale(d, {Lat:"CRS:1"(0:30)})
     * childCoverageExpressionHandler = (c + d)
     * and childDimensionIntervalListHandler = {Lat:"CRS:1"(0:30)}
     */
    protected void updateQueryTree(Handler coverageExpressionHandler, Handler childDimensionIntervalListHandler) throws PetascopeException {
        if (
            (coverageExpressionHandler.isUpdatedHandlerAlready(this))
            || this.stopToTraverseFurtherInTree(coverageExpressionHandler)
        ) {
            // NOTE: e.g. $c[i(0:30)][i(0)] then it should not move [i(0)]
            // as child of $c to $c[i(0)][i(0:30)] as it is invalid to trim after slicing on i axis

            // If this node is not created implicitly by petascope, then don't touch it
            return;

        }

        if (coverageExpressionHandler instanceof ShorthandSubsetHandler
                || coverageExpressionHandler instanceof ShortHandSubsetWithLetClauseVariableHandler) {
            this.isQueryTreeUpdated = this.createWrapperHandler(coverageExpressionHandler, childDimensionIntervalListHandler);
            return;
        }

        if (coverageExpressionHandler.getChildren() != null) {
            for (int i = 0; i < coverageExpressionHandler.getChildren().size(); i++) {
                Handler currentChildHandlerTmp = coverageExpressionHandler.getChildren().get(i);
                if (currentChildHandlerTmp == null
                        || currentChildHandlerTmp instanceof StringScalarHandler
                        || currentChildHandlerTmp instanceof RealNumberConstantHandler
                        || currentChildHandlerTmp instanceof DimensionIntervalListHandler) {
                    continue;
                }

                if (currentChildHandlerTmp instanceof CoverageVariableNameHandler) {
                    this.isQueryTreeUpdated = this.createWrapperHandler(currentChildHandlerTmp, childDimensionIntervalListHandler);
                } else {
                    this.updateQueryTree(currentChildHandlerTmp, childDimensionIntervalListHandler);
                }
            }
        }
    }
}
