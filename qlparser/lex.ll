%{
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
 * COMMENTS:
 * - token BY seems unused
 *
 ************************************************************/
#pragma GCC diagnostic ignored "-Wsign-conversion"
#pragma GCC diagnostic ignored "-Wswitch-default"
#pragma GCC diagnostic ignored "-Wsign-compare"

#include <math.h>
#include "qlparser/qtoperation.hh"
#include "qlparser/querytree.hh"
#include "qlparser/qtmddaccess.hh"
#include "qlparser/qtcomplexdata.hh"
#include "qlparser/qtiterator.hh"
#include "qlparser/qtunaryinduce.hh"
#include "qlparser/qtnullvaluesop.hh"
#include "qlparser/qtmshapeop.hh"
#include "qlparser/qtgeometryop.hh"
#include "rasodmg/dirdecompose.hh"
#include "relcatalogif/typefactory.hh"

std::list<ParseInfo> infoList;

ParseInfo *currInfo=0;

struct QtUpdateSpecElement
{
  QtOperation* iterator;
  QtOperation* domain;
};

#if defined(BISON_USE_PARSER_H_EXTENSION)
#   include "oql.h"
#else
#   include "oql.hh"
#endif

QueryTree* parseQueryTree = NULL;
char* beginParseString = NULL;
char* iterParseString = NULL;
void yyreset();

unsigned int lineNo   = 1;
unsigned int columnNo = 1;
void llerror(const char* s);
int  yyparse( );
int string_yyinput( char* buf, int max_size );

int string_yyinput( char* buf, int max_size )
{
  int lenParseString = strlen( iterParseString );
  int bufLength = max_size < lenParseString ? max_size : lenParseString;
  if( bufLength > 0 )
  {
    memcpy( buf, iterParseString, static_cast<unsigned int>(bufLength) );
    iterParseString += bufLength;
  }
  return bufLength;
}

#undef YY_INPUT
#define YY_INPUT( buff, buffLen, maxSize ) ( buffLen = string_yyinput( buff, maxSize ) )

#define SETTOKEN( TOKEN, TYPE, VALUE )                                   \
  yylval.TYPE.value = VALUE;                                             \
  if(!infoList.empty()) {                                                \
      currInfo = new ParseInfo(infoList.front());                        \
      infoList.pop_front();                                              \
  }    else {                                                            \
      currInfo = new ParseInfo( yytext, lineNo, columnNo );              \
  }                                                                      \
  yylval.TYPE.info = currInfo;                                           \
  columnNo += static_cast<unsigned int>(yyleng);                         \
  parseQueryTree->addDynamicObject( yylval.TYPE.info );                  \
  return TOKEN;


#define SETSTRTOKEN( TOKEN, TYPE, VALUE )                                \
  char* temp = strdup(VALUE);                                            \
  parseQueryTree->addCString( temp );                                    \
  yylval.TYPE.value = temp;                                              \
  if(!infoList.empty()) {                                                \
      currInfo = new ParseInfo(infoList.front());                        \
      infoList.pop_front();                                              \
  } else {                                                               \
      currInfo = new ParseInfo( yytext, lineNo, columnNo );              \
  }                                                                      \
  yylval.TYPE.info = currInfo;                                           \
  columnNo += static_cast<unsigned int>(yyleng);                         \
  parseQueryTree->addDynamicObject( yylval.TYPE.info );                  \
  return TOKEN;


#define SETINTTOKEN( VALUE, NEGATIVE, BYTES )                            \
  yylval.integerToken.negative = NEGATIVE;                               \
  yylval.integerToken.bytes    = BYTES;                                  \
  if( NEGATIVE )                                                         \
        yylval.integerToken.svalue = VALUE;                              \
  else                                                                   \
        yylval.integerToken.uvalue = (unsigned long)VALUE;               \
  if(!infoList.empty()) {                                                \
      currInfo = new ParseInfo(infoList.front());                        \
      infoList.pop_front();                                              \
  }    else {                                                            \
      currInfo = new ParseInfo( yytext, lineNo, columnNo );              \
  }                                                                      \
  yylval.integerToken.info = currInfo;                                   \
  columnNo += static_cast<unsigned int>(yyleng);                         \
  parseQueryTree->addDynamicObject( yylval.integerToken.info );          \
  return IntegerLit;


#define SETFLTTOKEN( VALUE, BYTES )                                      \
  yylval.floatToken.value  = VALUE;                                      \
  yylval.floatToken.bytes  = BYTES;                                      \
  if(!infoList.empty()) {                                                \
      currInfo = new ParseInfo(infoList.front());                        \
      infoList.pop_front();                                              \
  }    else {                                                            \
      currInfo = new ParseInfo(yytext, lineNo, columnNo);                \
  }                                                                      \
  yylval.floatToken.info = currInfo;                                     \
  columnNo += static_cast<unsigned int>(yyleng);                         \
  parseQueryTree->addDynamicObject( yylval.floatToken.info );            \
  return FloatLit;

%}

%option noyywrap

%%

"//".*					 { columnNo += static_cast<unsigned int>(yyleng); }
"--".*                                   { columnNo += static_cast<unsigned int>(yyleng); }

"complex"                                { SETTOKEN( COMPLEX, commandToken, COMPLEX ) }
"re"                                     { SETTOKEN( RE, commandToken, RE ) }
"im"                                     { SETTOKEN( IM, commandToken, IM ) }

"struct"                                 { SETTOKEN( STRCT, commandToken, STRCT ) }
"fastscale"                              { SETTOKEN( FASTSCALE, commandToken, FASTSCALE ) }
"members"                                { SETTOKEN( MEMBERS, commandToken, MEMBERS ) }
"add"                                    { SETTOKEN( ADD, commandToken, ADD ) }
"alter"                                  { SETTOKEN( ALTER, commandToken, ALTER ) }
"list"                                   { SETTOKEN( LIST, commandToken, LIST ) }
"select"                                 { SETTOKEN( SELECT, commandToken, SELECT ) }
"from"                                   { SETTOKEN( FROM, commandToken, FROM ) }
"where"                                  { SETTOKEN( WHERE, commandToken, WHERE ) }
"as"                                     { SETTOKEN( AS, commandToken, AS ) }
"restrict"                               { SETTOKEN( RESTRICT, commandToken, RESTRICT ) }
"to"                                     { SETTOKEN( TO, commandToken, TO ) }
"extend"                                 { SETTOKEN( EXTEND, commandToken, EXTEND ) }
"by"                                     { SETTOKEN( BY, commandToken, BY ) }

"project"                                { SETTOKEN( PROJECT, commandToken, PROJECT ) }
"near"                                   { SETTOKEN( RA_NEAR, commandToken, RA_NEAR ) }
"bilinear"                               { SETTOKEN( RA_BILINEAR, commandToken, RA_BILINEAR ) }
"cubic"                                  { SETTOKEN( RA_CUBIC, commandToken, RA_CUBIC ) }
"cubicspline"                            { SETTOKEN( RA_CUBIC_SPLINE, commandToken, RA_CUBIC_SPLINE ) }
"lanczos"                                { SETTOKEN( RA_LANCZOS, commandToken, RA_LANCZOS ) }
"average"                                { SETTOKEN( RA_AVERAGE, commandToken, RA_AVERAGE ) }
"mode"                                   { SETTOKEN( RA_MODE, commandToken, RA_MODE ) }
"med"                                    { SETTOKEN( RA_MED, commandToken, RA_MED ) }
"q1"                                     { SETTOKEN( RA_QFIRST, commandToken, RA_QFIRST ) }
"q3"                                     { SETTOKEN( RA_QTHIRD, commandToken, RA_QTHIRD ) }

"at"                                     { SETTOKEN( AT, commandToken, AT ) }
"dimension"                              { SETTOKEN( DIMENSION, commandToken, DIMENSION ) }
"all_cell"|"all_cells"                   { SETTOKEN( ALL, commandToken, ALL ) }
"some_cell"|"some_cells"                 { SETTOKEN( SOME, commandToken, SOME ) }
"count_cell"|"count_cells"               { SETTOKEN( COUNTCELLS, commandToken, COUNTCELLS ) }
"add_cell"|"add_cells"                   { SETTOKEN( ADDCELLS, commandToken, ADDCELLS ) }
"avg_cell"|"avg_cells"                   { SETTOKEN( AVGCELLS, commandToken, AVGCELLS ) }
"min_cell"|"min_cells"                   { SETTOKEN( MINCELLS, commandToken, MINCELLS ) }
"max_cell"|"max_cells"                   { SETTOKEN( MAXCELLS, commandToken, MAXCELLS ) }
"var_pop"                                { SETTOKEN( VAR_POP, commandToken, VAR_POP ) }
"var_samp"                               { SETTOKEN( VAR_SAMP, commandToken, VAR_SAMP ) }
"stddev_pop"                             { SETTOKEN( STDDEV_POP, commandToken, STDDEV_POP ) }
"stddev_samp"                            { SETTOKEN( STDDEV_SAMP, commandToken, STDDEV_SAMP ) }
"sdom"                                   { SETTOKEN( SDOM, commandToken, SDOM ) }
"over"                                   { SETTOKEN( OVER, commandToken, OVER ) }
"overlay"                                { SETTOKEN( OVERLAY, commandToken, OVERLAY ) }
"using"                                  { SETTOKEN( USING, commandToken, USING ) }
"lo"                                     { SETTOKEN( LO, commandToken, LO ) }
"hi"                                     { SETTOKEN( HI, commandToken, HI ) }
"concat"                                 { SETTOKEN( CONCAT, commandToken, CONCAT ) }
"along"                                  { SETTOKEN( ALONG, commandToken, ALONG ) }
"case"                                   { SETTOKEN( CASE, commandToken, CASE ) }
"when"                                   { SETTOKEN( WHEN, commandToken, WHEN ) }
"then"                                   { SETTOKEN( THEN, commandToken, THEN ) }
"else"                                   { SETTOKEN( ELSE, commandToken, ELSE ) }
"end"                                    { SETTOKEN( END, commandToken, END ) }

"insert"                                 { SETTOKEN( INSERT, commandToken, INSERT ) }
"into"                                   { SETTOKEN( INTO, commandToken, INTO ) }
"values"                                 { SETTOKEN( VALUES, commandToken, VALUES ) }
"delete"                                 { SETTOKEN( DELETE, commandToken, DELETE ) }
"drop"                                   { SETTOKEN( DROP, commandToken, DROP ) }
"create"                                 { SETTOKEN( CREATE, commandToken, CREATE ) }
"collection"                             { SETTOKEN( COLLECTION, commandToken, COLLECTION ) }
"type"                                   { SETTOKEN( TYPE, commandToken, TYPE ) }

"update"                                 { SETTOKEN( UPDATE, commandToken, UPDATE ) }
"set"                                    { SETTOKEN( SET, commandToken, SET ) }
"assign"                                 { SETTOKEN( ASSIGN, commandToken, ASSIGN ) }
"in"                                     { SETTOKEN( IN, commandToken, IN ) }
"marray"                                 { SETTOKEN( MARRAY, commandToken, MARRAY ) }
"mdarray"                                { SETTOKEN( MDARRAY, commandToken, MDARRAY ) }
"condense"                               { SETTOKEN( CONDENSE, commandToken, CONDENSE ) }
"null"                                   { SETTOKEN( NULLKEY, commandToken, NULLKEY ) }
"commit"                                 { SETTOKEN( COMMIT, commandToken, COMMIT ) }

"oid"                                    { SETTOKEN( OID, commandToken, OID ) }
"shift"                                  { SETTOKEN( SHIFT, commandToken, SHIFT ) }
"clip"                                   { SETTOKEN( CLIP, commandToken, CLIP ) }
"subspace"                               { SETTOKEN( SUBSPACE, commandToken, SUBSPACE)}
"multipolygon"                           { SETTOKEN( MULTIPOLYGON, commandToken, MULTIPOLYGON)}
"projection"                             { SETTOKEN( PROJECTION, commandToken, PROJECTION)}
"polygon"                                { SETTOKEN( POLYGON, commandToken, POLYGON)}
"curtain"                                { SETTOKEN( CURTAIN, commandToken, CURTAIN)}
"corridor"                               { SETTOKEN( CORRIDOR, commandToken, CORRIDOR)}
"linestring"                             { SETTOKEN( LINESTRING, commandToken, LINESTRING)}
"coordinates"                            { SETTOKEN( COORDINATES, commandToken, COORDINATES)}
"multilinestring"                        { SETTOKEN( MULTILINESTRING, commandToken, MULTILINESTRING)}
"discrete"                               { SETTOKEN( DISCRETE, commandToken, DISCRETE)}
"range"                                  { SETTOKEN( RANGE, commandToken, RANGE)}
"scale"                                  { SETTOKEN( SCALE, commandToken, SCALE ) }
"dbinfo"                                 { SETTOKEN( DBINFO, commandToken, DBINFO ) }
"version"                                { SETTOKEN( RAS_VERSION, commandToken, RAS_VERSION ) }

"sort"                                   { SETTOKEN( SORT, commandToken, SORT)}
"flip"                                 { SETTOKEN( FLIP, commandToken, FLIP)}

"."                                      { SETTOKEN( DOT, commandToken, DOT ) }
","                                      { SETTOKEN( COMMA, commandToken, COMMA ) }
"is"                                     { SETTOKEN( IS, commandToken, IS ) }
"not"                                    { SETTOKEN( NOT, commandToken, NOT ) }
"sqrt"                                   { SETTOKEN( SQRT, commandToken, SQRT ) }

"tiff"                                   { SETTOKEN( TIFF, commandToken, TIFF ) }
"bmp"                                    { SETTOKEN( BMP, commandToken, BMP ) }
"hdf"                                    { SETTOKEN( HDF, commandToken, HDF ) }
"netcdf"                                 { SETTOKEN( NETCDF, commandToken, NETCDF ) }
"jpeg"                                   { SETTOKEN( JPEG, commandToken, JPEG ) }
"csv"                                    { SETTOKEN( CSV, commandToken, CSV ) }
"png"                                    { SETTOKEN( PNG, commandToken, PNG ) }
"vff"                                    { SETTOKEN( VFF, commandToken, VFF ) }
"tor"                                    { SETTOKEN( TOR, commandToken, TOR ) }
"dem"                                    { SETTOKEN( DEM, commandToken, DEM ) }
"encode"                                 { SETTOKEN( ENCODE, commandToken, ENCODE ) }
"decode"                                 { SETTOKEN( DECODE, commandToken, DECODE ) }

"inv_tiff"                               { SETTOKEN( INV_TIFF, commandToken, INV_TIFF ) }
"inv_bmp"                                { SETTOKEN( INV_BMP, commandToken, INV_BMP ) }
"inv_hdf"                                { SETTOKEN( INV_HDF, commandToken, INV_HDF ) }
"inv_netcdf"                             { SETTOKEN( INV_NETCDF, commandToken, INV_NETCDF ) }
"inv_jpeg"                               { SETTOKEN( INV_JPEG, commandToken, INV_JPEG ) }
"inv_csv"                                { SETTOKEN( INV_CSV, commandToken, INV_CSV ) }
"inv_png"                                { SETTOKEN( INV_PNG, commandToken, INV_PNG ) }
"inv_vff"                                { SETTOKEN( INV_VFF, commandToken, INV_VFF ) }
"inv_tor"                                { SETTOKEN( INV_TOR, commandToken, INV_TOR ) }
"inv_dem"                                { SETTOKEN( INV_DEM, commandToken, INV_DEM ) }
"inv_grib"                               { SETTOKEN( INV_GRIB, commandToken, INV_GRIB ) }

"abs"                                    { SETTOKEN( ABS, commandToken, ABS ) }
"exp"                                    { SETTOKEN( EXP, commandToken, EXP ) }
"pow"                                    { SETTOKEN( POW, commandToken, POW ) }
"power"                                  { SETTOKEN( POWER, commandToken, POWER ) }
"log"                                    { SETTOKEN( LOGFN, commandToken, LOGFN ) }
"ln"                                     { SETTOKEN( LN, commandToken, LN ) }
"sin"                                    { SETTOKEN( SIN, commandToken, SIN ) }
"cos"                                    { SETTOKEN( COS, commandToken, COS ) }
"tan"                                    { SETTOKEN( TAN, commandToken, TAN ) }
"sinh"                                   { SETTOKEN( SINH, commandToken, SINH ) }
"cosh"                                   { SETTOKEN( COSH, commandToken, COSH ) }
"tanh"                                   { SETTOKEN( TANH, commandToken, TANH ) }
"arcsin"                                 { SETTOKEN( ARCSIN, commandToken, ARCSIN ) }
"asin"                                   { SETTOKEN( ASIN, commandToken, ASIN ) }
"arccos"                                 { SETTOKEN( ARCCOS, commandToken, ARCCOS ) }
"acos"                                   { SETTOKEN( ACOS, commandToken, ACOS ) }
"arctan"                                 { SETTOKEN( ARCTAN, commandToken, ARCTAN ) }
"atan"                                   { SETTOKEN( ATAN, commandToken, ATAN ) }
"round"                                  { SETTOKEN( ROUND, commandToken, ROUND ) }
"ceil"                                   { SETTOKEN( CEIL, commandToken, CEIL ) }
"floor"                                  { SETTOKEN( FLOOR, commandToken, FLOOR ) }

"index"                 { SETTOKEN( INDEX, commandToken, INDEX ) }
"rc_index"              { SETTOKEN( RC_INDEX, commandToken, RC_INDEX ) }
"tc_index"              { SETTOKEN( TC_INDEX, commandToken, TC_INDEX ) }
"a_index"               { SETTOKEN( A_INDEX, commandToken, A_INDEX ) }
"d_index"               { SETTOKEN( D_INDEX, commandToken, D_INDEX ) }
"rd_index"              { SETTOKEN( RD_INDEX, commandToken, RD_INDEX ) }
"rpt_index"             { SETTOKEN( RPT_INDEX, commandToken, RPT_INDEX ) }
"rrpt_index"            { SETTOKEN( RRPT_INDEX, commandToken, RRPT_INDEX ) }
"it_index"              { SETTOKEN( IT_INDEX, commandToken, IT_INDEX ) }
"auto"                  { SETTOKEN( AUTO, commandToken, AUTO ) }
"tiling"                { SETTOKEN( TILING, commandToken, TILING ) }
"aligned"               { SETTOKEN( ALIGNED, commandToken, ALIGNED ) }
"regular"               { SETTOKEN( REGULAR, commandToken, REGULAR ) }
"directional"           { SETTOKEN( DIRECTIONAL, commandToken, DIRECTIONAL ) }
"with"                  { SETTOKEN( WITH, commandToken, WITH ) }
"subtiling"             { SETTOKEN( SUBTILING, commandToken, SUBTILING ) }
"no_limit"              { SETTOKEN( P_NO_LIMIT, commandToken, P_NO_LIMIT ) }
"regroup"               { SETTOKEN( P_REGROUP, commandToken, P_REGROUP ) }
"regroup_and_subtiling" { SETTOKEN( P_REGROUP_AND_SUBTILING, commandToken, P_REGROUP_AND_SUBTILING ) }
"area"                  { SETTOKEN( AREA, commandToken, AREA ) }
"of"                    { SETTOKEN( OF, commandToken, OF ) }
"interest"              { SETTOKEN( INTEREST, commandToken, INTEREST ) }
"statistic"             { SETTOKEN( STATISTIC, commandToken, STATISTIC ) }
"tile"                  { SETTOKEN( TILE, commandToken, TILE ) }
"size"                  { SETTOKEN( SIZE, commandToken, SIZE ) }
"border"                { SETTOKEN( BORDER, commandToken, BORDER ) }
"threshold"             { SETTOKEN( THRESHOLD, commandToken, THRESHOLD ) }

"unsigned"                { SETTOKEN( TUNSIG, typeToken, TUNSIG) }
"bool"                    { SETTOKEN( TBOOL, typeToken, TBOOL) }
"char"                    { SETTOKEN( TCHAR, typeToken, TCHAR) }
"octet"                   { SETTOKEN( TOCTET, typeToken, TOCTET)     }
"short"                   { SETTOKEN( TSHORT, typeToken, TSHORT)     }
"ushort"                  { SETTOKEN( TUSHORT, typeToken, TUSHORT)     }
"long"                    { SETTOKEN( TLONG, typeToken, TLONG)    }
"ulong"                   { SETTOKEN( TULONG, typeToken, TULONG)    }
"float"                   { SETTOKEN( TFLOAT, typeToken, TFLOAT)     }
"double"                  { SETTOKEN( TDOUBLE, typeToken, TDOUBLE) }
"CFloat32"                { SETTOKEN( TCOMPLEX1, typeToken, TCOMPLEX1)  }
"CFloat64"                { SETTOKEN( TCOMPLEX2, typeToken, TCOMPLEX2)  }
"CInt16"                  { SETTOKEN( TCINT16, typeToken, TCINT16) }
"CInt32"                  { SETTOKEN( TCINT32, typeToken, TCINT32) }

"nan"                     { SETFLTTOKEN( NAN, 8 ) }
"nanf"                    { SETFLTTOKEN( NAN, 4 ) }
"inf"                     { SETFLTTOKEN( INFINITY, 8 ) }
"inff"                    { SETFLTTOKEN( INFINITY, 4 ) }

"max"                                    { SETTOKEN( MAX_BINARY, commandToken, MAX_BINARY ) }
"min"                                    { SETTOKEN( MIN_BINARY, commandToken, MIN_BINARY ) }
"bit"                                    { SETTOKEN( BIT, commandToken, BIT ) }
"and"                                    { SETTOKEN( AND, commandToken, AND ) }
"or"                                     { SETTOKEN( OR, commandToken, OR ) }
"xor"                                    { SETTOKEN( XOR, commandToken, XOR ) }
"+"                                      { SETTOKEN( PLUS, commandToken, PLUS ) }
"-"                                      { SETTOKEN( MINUS, commandToken, MINUS ) }
"*"                                      { SETTOKEN( MULT, commandToken, MULT ) }
"/"                                      { SETTOKEN( DIV, commandToken, DIV ) }
"div"                                    { SETTOKEN( INTDIV, commandToken, INTDIV ) }
"mod"                                    { SETTOKEN( MOD, commandToken, MOD ) }
"="                                      { SETTOKEN( EQUAL, commandToken, EQUAL ) }
"<"                                      { SETTOKEN( LESS, commandToken, LESS ) }
">"                                      { SETTOKEN( GREATER, commandToken, GREATER ) }
"<="                                     { SETTOKEN( LESSEQUAL, commandToken, LESSEQUAL ) }
">="                                     { SETTOKEN( GREATEREQUAL, commandToken, GREATEREQUAL ) }
"<>"                                     { SETTOKEN( NOTEQUAL, commandToken, NOTEQUAL ) }
"!="                                     { SETTOKEN( NOTEQUAL, commandToken, NOTEQUAL ) }
":"                                      { SETTOKEN( COLON, commandToken, COLON ) }
";"                                      { SETTOKEN( SEMICOLON, commandToken, SEMICOLON ) }
"["                                      { SETTOKEN( LEPAR, commandToken, LEPAR ) }
"]"                                      { SETTOKEN( REPAR, commandToken, REPAR ) }
"("                                      { SETTOKEN( LRPAR, commandToken, LRPAR ) }
")"                                      { SETTOKEN( RRPAR, commandToken, RRPAR ) }
"{"                                      { SETTOKEN( LCPAR, commandToken, LCPAR ) }
"}"                                      { SETTOKEN( RCPAR, commandToken, RCPAR ) }
#MDD[0-9]+#                              { SETTOKEN( MDDPARAM, commandToken, atoi(&(yytext[1])) ) }
$[0-9]+                                  { llerror("unresolved query parameter"); columnNo++; }

  /*
  *ASC is true
  *DESC is false
  **as sortAsc defined in qtsort.hh, used for SORT operation
  */

[aA][sS][cC]                              { SETTOKEN( sortOrderLit, sortOrderToken,  true  ) }
[dD][eE][sS][cC]                            { SETTOKEN( sortOrderLit, sortOrderToken,  false ) }

"true"|"false"|"TRUE"|"FALSE"            { SETTOKEN( BooleanLit, booleanToken, yytext[0] == 't' || yytext[0] == 'T') }
[a-zA-Z_][a-zA-Z0-9_]*                   {
                                            if (TypeFactory::mapType( yytext )) {
                                                SETSTRTOKEN( TypeName, identifierToken, yytext );
                                            } else {
                                                SETSTRTOKEN( Identifier, identifierToken, yytext );
                                            }
                                         }
"'"[^']"'"                               { SETTOKEN( CharacterLit, characterToken, yytext[1] ) }
\"([^"]|\\["\n])*\"                      {
                                           yytext[strlen(yytext)-1] = '\0';
                                           SETSTRTOKEN( StringLit, stringToken, &(yytext[1]) )
					 }

0x[0-9A-Fa-f]+[cC]                       { SETINTTOKEN( strtoul( yytext, (char**)NULL, 16 ), 0, 1 ) }
0x[0-9A-Fa-f]+"us"|"US"                  { SETINTTOKEN( strtoul( yytext, (char**)NULL, 16 ), 0, 2 ) }
0x[0-9A-Fa-f]+"ul"|"UL"                  { SETINTTOKEN( strtoul( yytext, (char**)NULL, 16 ), 0, 4 ) }
0x[0-9A-Fa-f]+[oO]                       { SETINTTOKEN( strtol ( yytext, (char**)NULL, 16 ), 1, 1 ) }
0x[0-9A-Fa-f]+[sS]                       { SETINTTOKEN( strtol ( yytext, (char**)NULL, 16 ), 1, 2 ) }
0x[0-9A-Fa-f]+[lL]?                      { SETINTTOKEN( strtol ( yytext, (char**)NULL, 16 ), 1, 4 ) }

[0-9]+[cC]                               { SETINTTOKEN( strtoul( yytext, (char**)NULL, 10 ), 0, 1 ) }
[0-9]+"us"|"US"                          { SETINTTOKEN( strtoul( yytext, (char**)NULL, 10 ), 0, 2 ) }
[0-9]+"ul"|"UL"                          { SETINTTOKEN( strtoul( yytext, (char**)NULL, 10 ), 0, 4 ) }
[0-9]+[oO]                               { SETINTTOKEN( strtol ( yytext, (char**)NULL, 10 ), 1, 1 ) }
[0-9]+[sS]                               { SETINTTOKEN( strtol ( yytext, (char**)NULL, 10 ), 1, 2 ) }
[0-9]+[lL]?                              { SETINTTOKEN( strtol ( yytext, (char**)NULL, 10 ), 1, 4 ) }

([0-9]+|([0-9]+(\.[0-9]+)?)([eE][-+]?[0-9]+)?)[dD]? { SETFLTTOKEN( strtod( yytext, (char**)NULL ), 8 ) }
([0-9]+|([0-9]+(\.[0-9]+)?)([eE][-+]?[0-9]+)?)[fF]  { SETFLTTOKEN( strtod( yytext, (char**)NULL ), 4 ) }

[ ]+                                     { columnNo += static_cast<unsigned int>(yyleng);              }
\t                                       { columnNo += 3;                            }
\r                                       {                                           }
\n                                       { columnNo  = 1; lineNo++;                  }
.                                        { SETTOKEN(UNKNOWN, commandToken, UNKNOWN ) }


%%

void yyreset()
{
  // Reset the input buffer of the scanner so that the next call of yylex() invokes
  // YY_INPUT to fill the buffer with new data.
  if ( YY_CURRENT_BUFFER )
    yy_flush_buffer( YY_CURRENT_BUFFER );
  lineNo = 1;
  columnNo = 1;
  currInfo = 0;
}


void llerror(const char* s)
{
   LERROR << "Lex error: line " << lineNo << ", " << s << " at " << yytext;
}
#pragma GCC diagnostic warning "-Wsign-conversion"
#pragma GCC diagnostic warning "-Wswitch-default"
