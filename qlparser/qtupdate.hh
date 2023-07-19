#ifndef __QTUPDATE_HH__
#define __QTUPDATE_HH___

#include "qlparser/qtexecute.hh"
#include "qlparser/qtoncstream.hh"
#include "qlparser/qtoperation.hh"
#include "qlparser/qtmddcfgop.hh"
#include "qlparser/qtmdd.hh"
#include "catalogmgr/ops.hh"

#include <iostream>

class Tile;

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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with rasdaman community.  If not, see <http://www.gnu.org/licenses/>.
*
* Copyright 2003, 2004, 2005, 2006, 2007, 2008, 2009 Peter Baumann /
rasdaman GmbH.
*
* For more information please see <http://www.rasdaman.org>
* or contact Peter Baumann via <baumann@rasdaman.com>.
*/
/*************************************************************
 *
 *
 * COMMENTS:
 *
 ************************************************************/

//@ManMemo: Module: {\bf qlparser}

/*@Doc:

*/

class QtUpdate : public QtExecute
{
public:
    /// constructor getting target, domain, and source expressions of the update
    QtUpdate(QtOperation *initUpdateTarget, QtOperation *initUpdateDomain, QtOperation *initUpdateSource);

    /// constructor getting storage layout
    QtUpdate(QtOperation *storage);

    /// virtual destructor
    virtual ~QtUpdate();

    /// method for evaluating the node
    virtual QtData *evaluate();

    /// return childs of the node
    virtual QtNodeList *getChilds(QtChildType flag);

    /// prints the tree
    virtual void printTree(int tab, std::ostream &s = std::cout, QtChildType mode = QT_ALL_NODES);

    /// prints the algebraic expression
    virtual void printAlgebraicExpression(std::ostream &s = std::cout);

    //@Man: Read/Write methods:
    //@{
    ///
    ///
    void setStreamInput(QtONCStream *newInput);
    ///
    /// returns updateTarget
    QtOperation *getUpdateTarget();
    /// returns updateDomain
    QtOperation *getUpdateDomain();
    ///returns updateSource
    QtOperation *getUpdateSource();
    ///returns input
    QtONCStream *getInput();
    //@}

    /// method for identification of nodes
    inline virtual QtNodeType getNodeType() const;

    /// method for query rewrite
    inline virtual void setInput(QtOperation *child, QtOperation *input);

    /// type checking
    virtual void checkType();

    /// tiling functions
    r_Data_Format getDataFormat(QtMDDConfig *config);
    r_Index_Type getIndexType(QtMDDConfig *config);
    r_Tiling_Scheme getTilingScheme(QtMDDConfig *cfg);
    vector<r_Minterval> getIntervals(QtMDDConfig *cfg);
    r_Minterval getTileConfig(QtMDDConfig *cfg, int baseTypeSize, r_Dimension sourceDimension);

private:
    /// evaluate one tuple of the input stream
    void evaluateTuple(QtNode::QtDataList *nextTuple);

    /// repartition one tuple of the input stream
    void repartitionTuple(QtNode::QtDataList *nextTuple, QtMDDConfig *mddConfig);

    /// check validity of operands
    bool checkOperands(QtNode::QtDataList *nextTuple, QtData *target, QtData *source);

    /// test for update domain compatibility
    void checkDomainCompatibility(QtNode::QtDataList *nextTuple, QtData *target,
                                  QtData *source, QtData *domainData,
                                  QtMDD *targetMDD, QtMDD *sourceMDD);

    /// generic method to handle errors
    void throwError(QtNode::QtDataList *nextTuple, QtData *target,
                    QtData *source, int errorNumber, QtData *domainData = NULL);

    /// one input stream
    /// Used to iterate through the collection.
    ///Specified by the UPDATE clause.
    QtONCStream *input;

    /// target expression
    /// Specifies the target collection on which the update is performed.
    /// Specified by the SET clause.
    QtOperation *updateTarget;

    /// target domain expression
    /// Specifies the domain of the target collection on which the update is performed.
    /// Specified by the SET clause.
    QtOperation *updateDomain;

    /// target expression
    /// Specifies the source collection to apply to the target collection.
    /// Specified by the ASSIGN clause.
    QtOperation *updateSource;

    // Storage and Tiling type
    QtOperation *stgLayout;

    /// attribute for identification of nodes
    static const QtNodeType nodeType;
};

#include "qlparser/qtupdate.icc"

#endif
