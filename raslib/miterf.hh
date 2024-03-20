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

#ifndef D_MITERF_HH
#define D_MITERF_HH

#include "raslib/mddtypes.hh"
#include <math.h>

class r_Minterval;
class Tile;

/// Use in conditionals to signal that the condition is very unlikely
/// https://gcc.gnu.org/onlinedocs/gcc/Other-Builtins.html#index-_005f_005fbuiltin_005fexpect
#if !defined(unlikely)
#define unlikely(x)     __builtin_expect(!!(x), 0)
#endif
/// Use in conditionals to signal that the condition is very likely
/// https://gcc.gnu.org/onlinedocs/gcc/Other-Builtins.html#index-_005f_005fbuiltin_005fexpect
#if !defined(likely)
#define likely(x)       __builtin_expect(!!(x), 1)
#endif

/**
  * \ingroup raslib
  */

/**
 * A fixed-point representation of a double, with 30 digit fractional part
 * precision.
 */
class r_FixedPointNumber
{
public:
    r_FixedPointNumber() = default;
    explicit r_FixedPointNumber(const double &);

    r_FixedPointNumber(const r_FixedPointNumber &) = default;
    r_FixedPointNumber &operator=(const r_FixedPointNumber &) = default;
    r_FixedPointNumber &operator=(const double &);

    /// Add the given value to this value.
    /// @return carry of fracPart
    inline bool stepForwardFlag(const r_FixedPointNumber &);

    /// @return the integer part
    inline r_Range getIntPart() const;

    std::string toString() const;
private:
    /// decompose the double operand into its integer and fractional parts
    void init(const double &);

    r_Range intPart{};
    r_Range fracPart{};

    static const int FIXPREC;
    static const r_Range carryPos;
    static const r_Range fracMask;
    static const double fixOne;

    friend std::ostream &operator<<(std::ostream &, r_FixedPointNumber &);
};

inline bool r_FixedPointNumber::stepForwardFlag(const r_FixedPointNumber &f)
{
    intPart += f.intPart;
    fracPart += f.fracPart;
    if (fracPart & carryPos)
    {
        ++intPart;
        fracPart &= fracMask;
        return true;
    }
    return false;
}
inline r_Range r_FixedPointNumber::getIntPart() const
{
    return intPart;
}

// -------------------------------------------------------------------------- //

//@ManMemo: Module: {\bf raslib}
/**
  * \ingroup raslib
  */

/**
  r_MiterFloat is used for iterating through parts of
  multidimensional intervals with arbitrary stepping size using
  nearest neighbours. It is given the tile, the source domain
  and the destination domain.
  Apart from that behaviour it is exactly as in r_Miter.
*/
class r_MiterFloat
{
public:
    /// Constructor getting the source tile, the source domain in that tile,
    /// and the destination domain
    r_MiterFloat(r_Bytes srcCellSize,
                 const char *srcTile,
                 const r_Minterval &fullTileDomain,
                 const r_Minterval &srcDomain,
                 const r_Minterval &dstDomain);
    
    r_MiterFloat(const r_MiterFloat&) = delete;
    r_MiterFloat& operator=(const r_MiterFloat&) = delete;
    r_MiterFloat(r_MiterFloat&&) = delete;
    r_MiterFloat& operator=(r_MiterFloat&&) = delete;
    
    ~r_MiterFloat();

    /// iterator reset
    void reset();
    /// get the next cell
    inline char *nextCell();
    /// true if done
    inline bool isDone() const;

protected:
    /// Iteration information for each axis
    struct iter_desc
    {
        r_FixedPointNumber min;     // source axis lower bound
        r_FixedPointNumber pos;     // source axis current position, initially = min
        r_FixedPointNumber step;    // source / destination axis extents ratio

        r_Range countSteps;         // initially = maxSteps, decreased on every nextCell()
        r_Range maxSteps;           // dest axis extent

        r_Range dimStep;            // step one index over the full source data
        r_Range scaleStep;          // step one scaled index over the full source data
        
        char *cell;                 // the current cell returned by nextCell(), initially = firstCell
    };

    char *currentCell;
    /// start of the source tile cell data at the origin of srcDomain
    const char *firstCell;
    /// Iteration information for each axis
    iter_desc *iterDesc;
    iter_desc *iterDescEnd;
    /// source tile dimension
    r_Dimension dim;
    /// true when the iteration over dstDomain is finished
    bool done;
};

inline char *r_MiterFloat::nextCell()
{
    if (likely(!done))
    {
        iter_desc *id = iterDescEnd;
        currentCell = id->cell;
    
        r_Dimension i = dim;
        while (likely(i > 0))
        {
            --id->countSteps;
            if (likely(id->countSteps))
            {
                // one more step in this dimension
                if (id->pos.stepForwardFlag(id->step))
                {
                    id->cell += id->dimStep;
                }
                id->cell += id->scaleStep;
                break;
            }
            // else we are finished with this dimension
            id->pos = id->min;
            id->countSteps = id->maxSteps;
            --id;
            --i;
        }
    
        if (unlikely(i < dim))
        {
            if (i == 0)
            {
                done = true;
            }
            else
            {
                for (r_Dimension j = i; j < dim; j++)
                    iterDesc[j].cell = iterDesc[i - 1].cell;
            }
        }
    }
    
    return currentCell;
}

inline bool r_MiterFloat::isDone() const
{
    return done;
}

#endif
