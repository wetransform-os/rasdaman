#!/usr/bin/env python3
#
# Generates openEO process descriptions that work with rasdaman WCPS.
# The generated descriptions deviate from the standard by adding an extra "wcps"
# property, which specifies a template for translating the process to a
# corresponding WCPS query expression.

import json
import os
from collections import OrderedDict

from tabulate import tabulate


# openEO process descriptions: https://github.com/Open-EO/openeo-processes


def save_process_description(process):
    filename = process['id'] + '.json'
    with open(filename, 'w') as f:
        json.dump(process, f, indent=4)


def save_process_descriptions(category: str, processes):
    for p in processes:
        p['categories'] = [category]
        save_process_description(p)


def get_type_coercion_table(rules: list[list[str]]):
    if len(rules[0]) == 3:
        headers = ["first operand type", "second operand type", "result type"]
    else:
        headers = ["operand type", "result type"]
    return '```\n' + tabulate(rules, headers=headers, tablefmt="github") + '\n```'


type_rules_for_add_div_mod = [
    ['X', 'double', 'double'],
    ['[unsigned] int', 'float', 'double'],
    ['X', 'float', 'float'],
    ['unsigned X', 'unsigned <y>', 'unsigned max(X, Y) + 1'],
    ['X', 'Y', 'signed(max(X, Y) + 1)']
]


def get_binary_description(preamble, type_rules):
    ret = preamble
    ret += """\n\nEach operand can be a scalar number, or a datacube. If both operands are datacubes, they must have matching domain sets.\n\n
The result is a scalar number if both operands are scalars, and otherwise a datacube. Operand elements which are null result in a null element in the result.\n\n"""
    if type_rules is not None:
        ret += "The element type of the result depends on the element types of the operands:\n\n"
        ret += get_type_coercion_table(type_rules)
    return ret


def get_unary_description(preamble, type_rules):
    ret = preamble
    ret += """\n\nThe operand can be a scalar number or a datacube.\n\n
The result is a scalar number if the operand is scalar, and otherwise a datacube. Operand elements which are null result in a null element in the result.\n\n"""
    if type_rules is not None:
        ret += "The element type of the result depends on the element type of the operand:\n\n"
        ret += get_type_coercion_table(type_rules)
    return ret


def get_aggregation_description(preamble, type_rules):
    ret = preamble
    ret += """\n\nThe operand must be a datacube.\n\n
The result is a scalar number. Operand elements which are null are ignored while computing the result.\n\n"""
    if type_rules is not None:
        ret += "The element type of the result depends on the element type of the operand:\n\n"
        ret += get_type_coercion_table(type_rules)
    return ret


def get_schema(types):
    if isinstance(types, dict):
        return types

    if len(types) == 1:
        if types[0] == 'datacube':
            return {'type': 'object', 'subtype': 'datacube'}
        elif types[0] == 'band-name':
            return {'type': 'string', 'subtype': 'band-name'}
        else:
            return {'type': types[0]}

    datacube = None
    nondatacube = []
    for t in types:
        if t == 'datacube' or t == 'process-graph':
            datacube = {'type': 'object', 'subtype': t}
        elif t == 'band-name':
            datacube = {'type': 'string', 'subtype': t}
        else:
            nondatacube.append(t)

    if datacube is not None:
        return [{'type': nondatacube}, datacube]

    return {'type': nondatacube}


schema_num_array = get_schema(['datacube', 'boolean', 'number', 'null'])
schema_bool_array = get_schema(['boolean', 'datacube', 'null'])
schema_datacube = get_schema(['datacube'])


def get_unary_param(descr, schema=schema_num_array, name='x'):
    x = OrderedDict()
    x['name'] = name
    x['description'] = descr
    x['schema'] = schema
    return [x]


def get_binary_params(descr_left, descr_right, schema1=schema_num_array, schema2=schema_num_array):
    x = OrderedDict()
    x['name'] = 'x'
    x['description'] = descr_left
    x['schema'] = schema1
    y = OrderedDict()
    y['name'] = 'y'
    y['description'] = descr_right
    y['schema'] = schema2
    return [x, y]


def get_returns(descr, schema=schema_num_array):
    ret = OrderedDict()
    ret['description'] = descr
    ret['schema'] = schema
    return ret


def get_num_binary_examples(scalar_result, floats=True):
    y = 2.5 if floats else 2
    return [
        {'arguments': {'x': 5, 'y': y}, 'returns': scalar_result},
        {'arguments': {'x': 5, 'y': 'DatacubeA'}, 'returns': 'DatacubeB'},
        {'arguments': {'x': 'DatacubeA', 'y': 'DatacubeB'}, 'returns': 'DatacubeC'},
        {'arguments': {'x': 5, 'y': None}, 'returns': None},
    ]


def get_bool_binary_examples(scalar_result1, scalar_result2):
    return [
        {'arguments': {'x': True, 'y': True}, 'returns': scalar_result1},
        {'arguments': {'x': True, 'y': False}, 'returns': scalar_result2},
        {'arguments': {'x': True, 'y': 'DatacubeA'}, 'returns': 'DatacubeB'},
        {'arguments': {'x': 'DatacubeA', 'y': 'DatacubeB'}, 'returns': 'DatacubeC'},
        {'arguments': {'x': None, 'y': True}, 'returns': None},
    ]


def get_num_unary_examples(scalar_result):
    return [
        {'arguments': {'x': 5}, 'returns': scalar_result},
        {'arguments': {'x': 'DatacubeA'}, 'returns': 'DatacubeB'},
        {'arguments': {'x': None}, 'returns': None},
    ]


def get_bool_unary_examples(scalar_result1, scalar_result2):
    return [
        {'arguments': {'x': True}, 'returns': scalar_result1},
        {'arguments': {'x': False}, 'returns': scalar_result2},
        {'arguments': {'x': 'DatacubeA'}, 'returns': 'DatacubeB'},
        {'arguments': {'x': None}, 'returns': None},
    ]


def get_num_aggregation_examples(scalar_result):
    return [
        {'arguments': {'x': [0, 1, 2, None, 3]}, 'returns': scalar_result},
    ]


def get_bool_aggregation_examples(scalar_result1, scalar_result2):
    return [
        {'arguments': {'x': [True, None, True]}, 'returns': scalar_result1},
        {'arguments': {'x': [True, None, False]}, 'returns': scalar_result2},
    ]


link_induced_docs = ["Documentation on induced operations in rasdaman",
                     "https://doc.rasdaman.org/04_ql-guide.html#induced-operations"]
link_scalar_ops_docs = ["Documentation on operations resulting in scalar values in rasdaman",
                        "https://doc.rasdaman.org/11_cheatsheets.html#scalar-operations"]
link_ieee_floats = ["IEEE Standard 754-2019 for Floating-Point Arithmetic",
                    "https://ieeexplore.ieee.org/document/8766229"]
logical_links = link_induced_docs + ["Boolean algebra explained by Wikipedia",
                                     "https://en.wikipedia.org/wiki/Boolean_algebra",
                                     ]


def get_links(links: list[str]):
    ret = []
    for i in range(len(links) // 2):
        ret.append({
            "rel": "about",
            "href": links[i * 2 + 1],
            "title": links[i * 2]
        })
    return ret


def get_exceptions(exceptions: list[str]):
    ret = {}
    for i in range(len(exceptions) // 2):
        ret[exceptions[i * 2]] = {'message': exceptions[i * 2 + 1]}
    return ret


def generate(id: str, summary: str, description: str, parameters, returns, examples, links, wcps=None, exceptions=None, categories=None):
    ret = OrderedDict([
        ('id', id),
        ('summary', summary),
        ('description', description),
        ('parameters', parameters),
        ('returns', returns),
        ('examples', examples),
        ('links', links),
    ])
    if exceptions is not None:
        ret['exceptions'] = exceptions
    if categories is not None:
        ret['categories'] = categories
    ret['wcps'] = wcps
    return ret


# ------------------------------------------------------------------------------
# Arithmetic: +, -, *, /, div, mod, abs, round, floor, ceil
# ------------------------------------------------------------------------------


def gen_add():
    return generate('add', 'Add two operands',
                    get_binary_description('Add two operands `x` and `y` (*`x + y`*).', type_rules_for_add_div_mod),
                    get_binary_params('The first summand.', 'The second summand.'),
                    get_returns("The computed sum of the two operands."),
                    get_num_binary_examples(7.5),
                    get_links(link_induced_docs + [
                        "Sum explained by Wolfram MathWorld",
                        "https://mathworld.wolfram.com/Sum.html"] + link_ieee_floats),
                    wcps='<x> + <y>'
                    )


def gen_subtract():
    type_rules = type_rules_for_add_div_mod
    del type_rules[-2]  # remove the second last row about unsigned result
    return generate('subtract', 'Subtract two operands',
                    get_binary_description('Subtracts argument `y` from the argument `x` (*`x - y`*).', type_rules),
                    get_binary_params('The minuend.', 'The subtrahend.'),
                    get_returns("The computed difference of the two operands."),
                    get_num_binary_examples(2.5),
                    get_links(link_induced_docs + [
                        "Subtraction explained by Wolfram MathWorld",
                        "https://mathworld.wolfram.com/Subtraction.html"] + link_ieee_floats),
                    wcps='<x> - <y>',
                    )


def gen_multiply():
    type_rules = [['X', 'bool', 'X'], ['bool', 'X', 'X']]
    type_rules += type_rules_for_add_div_mod
    return generate('multiply', 'Multiply two operands',
                    get_binary_description('Multiply two operands `x` and `y` (*`x * y`*).', type_rules),
                    get_binary_params('The multiplier.', 'The multiplicand.'),
                    get_returns("The computed product of the two operands."),
                    get_num_binary_examples(12.5),
                    get_links(link_induced_docs + [
                        "Product explained by Wolfram MathWorld",
                        "https://mathworld.wolfram.com/Product.html"] + link_ieee_floats),
                    wcps='<x> * <y>'
                    )


def gen_divide():
    type_rules = [
        ['[unsigned] char,short,float', '[unsigned] char,short,float', 'float'],
        ['X', 'Y', 'double'],
    ]
    return generate('divide', 'Division of two operands',
                    get_binary_description('Division of `x` by `y` (*`x / y`*).', type_rules),
                    get_binary_params('The dividend.', 'The divisor.'),
                    get_returns("The quotient of the arguments."),
                    get_num_binary_examples(2),
                    get_links(link_induced_docs + [
                        "Division explained by Wolfram MathWorld",
                        "http://mathworld.wolfram.com/Division.html"] + link_ieee_floats),
                    wcps='<x> / <y>',
                    exceptions=get_exceptions(['DivisionByZero', 'Division by zero'])
                    )


def gen_divide_floor():
    return generate('divide_floor', 'Floor division of two operands',
                    get_binary_description('Floor division of `x` by `y` (*`div(x, y)`*) for both integer and floating-point numbers; floor() is '
                                           'applied to the result if one or both arguments are integers.', type_rules_for_add_div_mod),
                    get_binary_params('The dividend.', 'The divisor.'),
                    get_returns("The quotient of the arguments."),
                    get_num_binary_examples(2.0),
                    get_links(link_induced_docs + [
                        "Division explained by Wolfram MathWorld",
                        "http://mathworld.wolfram.com/Division.html"] + link_ieee_floats),
                    wcps='div(<x>, <y>)',
                    exceptions=get_exceptions(['DivisionByZero', 'Division by zero'])
                    )


def gen_mod():
    return generate('mod',
                    'Modulo',
                    get_binary_description('Remainder after division of `x` by `y` (*`mod(x, y)`*) for both integer and floating-point numbers.',
                                           type_rules_for_add_div_mod),
                    get_binary_params('The dividend.', 'The divisor.'),
                    get_returns("The computed remainder after division."),
                    get_num_binary_examples(0.0),
                    get_links(link_induced_docs + [
                        "Modulo explained by Wikipedia",
                        "https://en.wikipedia.org/wiki/Modulo_operation",
                    ]),
                    wcps='mod(<x>, <y>)',
                    exceptions=get_exceptions(['DivisionByZero', 'Division by zero'])
                    )


def gen_absolute():
    return generate('absolute', 'Absolute value',
                    get_unary_description('Computes the absolute value of `x`, which is the non-negative value of `x` (*`abs(x)`*).',
                                          [['complex', 'error'], ['X', 'X']]),
                    get_unary_param('The operand.'),
                    get_returns("Null where `x` is null, the absolute value of `x` otherwise."),
                    get_num_unary_examples(5),
                    get_links(link_induced_docs + [
                        "Absolute value explained by Wolfram MathWorld",
                        "http://mathworld.wolfram.com/AbsoluteValue.html",
                    ]),
                    wcps='abs(<x>)',
                    )


def gen_round():
    return generate('round', 'Round to an integer',
                    get_unary_description('Computes the nearest integer value to `x` (*`round(x)`*).', [['X', 'X']]),
                    get_unary_param('The operand.'),
                    get_returns("Null where `x` is null, the nearest integer to `x` otherwise."),
                    get_num_unary_examples(5),
                    get_links(link_induced_docs + [
                        "round function in C++",
                        "https://en.cppreference.com/w/cpp/numeric/math/round"] + link_ieee_floats),
                    wcps='round(<x>)',
                    )


def gen_floor():
    return generate('floor', 'Round down to an integer',
                    get_unary_description('Computes the largest integer value not greater than `x` (*`floor(x)`*).', [['X', 'X']]),
                    get_unary_param('The operand.'),
                    get_returns("Null where `x` is null, the largest integer value not greater than `x` otherwise."),
                    get_num_unary_examples(5),
                    get_links(link_induced_docs + [
                        "floor function in C++",
                        "https://en.cppreference.com/w/cpp/numeric/math/floor"] + link_ieee_floats),
                    wcps='floor(<x>)',
                    )


def gen_ceil():
    return generate('floor', 'Round up to an integer',
                    get_unary_description('Computes the least integer value not smaller than `x` (*`ceil(x)`*).', [['X', 'X']]),
                    get_unary_param('The operand.'),
                    get_returns("Null where `x` is null, the least integer value not smaller than `x` otherwise."),
                    get_num_unary_examples(5),
                    get_links(link_induced_docs + [
                        "ceil function in C++",
                        "https://en.cppreference.com/w/cpp/numeric/math/ceil"] + link_ieee_floats),
                    wcps='ceil(<x>)',
                    )


save_process_descriptions(
    'Arithmetic',
    [gen_multiply(), gen_add(), gen_mod(), gen_divide(), gen_subtract(), gen_divide_floor(),
     gen_absolute(), gen_round(), gen_floor(), gen_ceil()]
)


# ------------------------------------------------------------------------------
# Comparison: <, <=, >, >=, =, !=, max, min, overlay
# ------------------------------------------------------------------------------


def gen_comparison(id: str, comptype: str, example_result: bool, op: str,
                   type_rules=[['X', 'Y', 'bool']]):
    summary = comptype.capitalize() + ' comparison'
    descr = f'Compares whether `x` is {comptype} `y` (*`x {op} y`*).'
    returns = f'null where `x` or `y` is null, true where `x` is {comptype} `y`, false otherwise.'
    return generate(id, summary,
                    get_binary_description(descr, type_rules),
                    get_binary_params('The first operand.', 'The second operand.'),
                    get_returns(returns, schema=schema_bool_array),
                    get_num_binary_examples(example_result),
                    get_links(link_induced_docs),
                    wcps=f'<x> {op} <y>',
                    )


def gen_max_min_overlay(id: str, summary: str, example_result, op: str, preamble: str, returns: str):
    schema = get_schema(['number', 'boolean', 'datacube', 'null'])
    return generate(id, summary,
                    get_binary_description(preamble, [['X', 'X', 'X']]),
                    get_binary_params('The first operand.', 'The second operand.', schema1=schema, schema2=schema),
                    get_returns(returns),
                    get_num_binary_examples(example_result),
                    get_links(link_induced_docs),
                    wcps=f'<x> {op} <y>',
                    )


def gen_sort():
    description = """Sorts a datacube `x` along a given `dimension`. The sorting is done by slicing the datacube along the dimensions,
calculating a slice rank for each of the slices, and then rearranging the slices according to their ranks, in an ascending or descending order.\n\n
The result datacube has same domain as the input datacube `x`."""

    # parameters
    x = get_unary_param('Operand datacube to be sorted.', schema=schema_datacube)[0]
    dimension = get_unary_param("The name of the dimension to sort along.", get_schema(['string', 'null']), name='dimension')[0]
    asc = \
        get_unary_param(
            'The default sort order is ascending, from smaller to larger values. To sort in reverse (descending) order, set this parameter to `false`.',
            get_schema(['boolean']), name='asc')[0]
    asc['default'] = True
    asc['optional'] = True
    rank_exp = get_unary_param('A process that accepts a datacube, and returns a *rank* as a double floating-point number.',
                               schema=get_schema(['process-graph']), name='rank_process')[0]
    rank_exp['schema']['parameters'] = get_unary_param('The datacube slice to be aggregated into a rank value.',
                                                       schema=schema_datacube)
    rank_exp['schema']['returns'] = get_returns('The rank value calculated for the given datacube.', schema=get_schema(['number']))

    return generate('sort', 'Sort a datacube along a dimension', description,
                    [x, dimension, rank_exp, asc],
                    get_returns('The sorted datacube.', schema=schema_datacube),
                    [
                        {'arguments': {'x': [3, 2, 5], 'asc': True, 'rank_process': 'sum(x[dimension])'}, 'returns': [2, 3, 5]},
                    ],
                    ['Sort operator in rasdaman WCPS',
                     'https://doc.rasdaman.org/05_geo-services-guide.html#sort-operator-in-wcps'],
                    wcps='SORT <x> ALONG <dimension> <if(asc)>ASC<else>DESC<endif> BY <rank_exp>'
                    )


save_process_descriptions(
    'Comparison',
    [gen_comparison('lt', 'less than', False, '<'),
     gen_comparison('lte', 'less than or equal', False, '<='),
     gen_comparison('gt', 'greater than', True, '>'),
     gen_comparison('gte', 'greater than or equal to', True, '>='),
     gen_comparison('eq', 'equality', False, '='),
     gen_comparison('neq', 'non-equality', False, '!='),
     gen_max_min_overlay('max_binary', 'Larger of two operands', 5, 'max',
                         'Computes the larger of `x` and `y` (*`x max y`*).',
                         'null where `x` or `y` is null, `x` if it is larger than `y`, `y` otherwise.'),
     gen_max_min_overlay('min_binary', 'Smaller of two operands', 2.5, 'min',
                         'Computes the smaller of `x` and `y` (*`x min y`*).',
                         'null where `x` or `y` is null, `x` if it is smaller than `y`, `y` otherwise.'),
     gen_max_min_overlay('overlay', 'Overlay two values', 2.5, 'overlay',
                         'Overlays `x` on top of `y` where `y` is null (*`x overlay y`*).',
                         '`y` where it is not null, `x` otherwise.'),
     gen_sort()
     ]
)


# ------------------------------------------------------------------------------
# Logical: and, or, xor, is, not
# ------------------------------------------------------------------------------


def gen_logical(id: str, comp: str, ex1_result: bool, ex2_result: bool, returns: str):
    summary = 'Logical ' + id
    descr = f'Performs logical {comp} on `x` and `y` (*`x {id} y`*).'
    returns = f'null where `x` or `y` is null, {returns}, false otherwise.'
    return generate(id, summary,
                    get_binary_description(descr, [['bool', 'bool', 'bool']]),
                    get_binary_params('The first operand: a boolean scalar or datacube.', 'The second operand: a boolean scalar or datacube.',
                                      schema1=schema_bool_array, schema2=schema_bool_array),
                    get_returns(returns, schema=schema_bool_array),
                    get_bool_binary_examples(ex1_result, ex2_result),
                    get_links(logical_links),
                    wcps=f'<x> {id} <y>'
                    )


def gen_not():
    return generate('not', 'Logical negation',
                    get_unary_description(f'Computes the logical negation of `x` (*`not x`*).', [['bool', 'bool']]),
                    get_unary_param('The operand.'),
                    get_returns(f"Null where `x` is null, true where `x` is false, false otherwise.", schema=schema_bool_array),
                    get_bool_unary_examples(False, True),
                    get_links(logical_links),
                    wcps='not <x>',
                    )


def gen_is_nan():
    return generate('is_nan', 'Value is not a number',
                    get_unary_description(f'Checks which elements of `x` are nan (*`x = nan`*).', [['X', 'bool']]),
                    get_unary_param('The operand.'),
                    get_returns(f"Null where `x` is null, true where `x` is nan, false otherwise."),
                    [{'arguments': {'x': 'NaN'}, 'returns': True},
                     {'arguments': {'x': 1.0}, 'returns': False},
                     {'arguments': {'x': [1.0, 'NaN', 2.0]}, 'returns': [False, True, False]}],
                    get_links(link_induced_docs),
                    wcps='<x> = nan',
                    )


def gen_is_nodata():
    return generate('is_nodata', 'Value is not a no-data (null) value',
                    get_unary_description(f'Checks which elements of `x` are nodata (null) values (*`x IS NULL`*).', [['X', 'bool']]),
                    get_unary_param('The operand.'),
                    get_returns(f"True where `x` is null, false otherwise."),
                    [{'arguments': {'x': None}, 'returns': True},
                     {'arguments': {'x': 1.0}, 'returns': False},
                     {'arguments': {'x': [1.0, 'NaN', None]}, 'returns': [False, False, True]}],
                    get_links(link_induced_docs),
                    wcps='<x> IS NULL',
                    )


def gen_is_valid():
    return generate('is_valid', 'Value is valid data',
                    get_unary_description(f'Checks which elements of `x` are valid (*`x IS NULL`*). Valid values are all finite numerical '
                                          f'values (excluding NaN according to [IEEE Standard 754](https://ieeexplore.ieee.org/document/4610935)), '
                                          f'which are not nodata values according to ``is_nodata()``.', [['X', 'bool']]),
                    get_unary_param('The operand.'),
                    get_returns(f"True where `x` is valid, false otherwise."),
                    [{'arguments': {'x': None}, 'returns': False},
                     {'arguments': {'x': 1.0}, 'returns': True},
                     {'arguments': {'x': [1.0, 'NaN', None]}, 'returns': [True, False, False]}],
                    get_links(link_induced_docs + link_ieee_floats),
                    wcps='<x> != nan and x is not null',
                    )


def gen_if():
    description = """Where `x` elements are `true`, returns `accept` elements, otherwise returns `reject` elements.\n\n
The result is a scalar number if all parameters are scalars, and otherwise a datacube. Operand elements which are null result in a null element in the result.\n\n
The `accept` and `reject` parameters must be of the same element type, while `x` must be of type boolean; all datacube parameters must have matching domains."""

    x = get_unary_param("Scalar or datacube condition operand of type boolean.", schema=schema_bool_array)[0]
    accept = get_unary_param("A value that is returned where elements of `x` are `true`.", name='accept')[0]
    reject = get_unary_param("A value that is returned where elements of `x` are not `true`; defaults to null.", name='reject')[0]
    reject['default'] = None
    reject['optional'] = True
    parameters = [x, accept, reject]

    examples = [{'arguments': {'x': True, 'accept': 1, 'reject': 3}, 'returns': 1},
                {'arguments': {'x': [True, False], 'accept': [1, 2], 'reject': [3, 4]}, 'returns': [1, 4]},
                {'arguments': {'x': [True, False], 'accept': [1, 2], 'reject': 0}, 'returns': [1, 0]},
                {'arguments': {'x': False, 'accept': [1, 2], 'reject': 0}, 'returns': [0, 0]}]

    # id: str, summary: str, description: str, parameters, returns, examples, links, wcps: str, exceptions=None, categories=None
    return generate('if', 'If-Then-Else conditional', description,
                    parameters,
                    get_returns("Either the `accept` or `reject` elements depending on the condition."),
                    examples,
                    get_links(link_induced_docs + ["Case Distinction in rasdaman WCPS",
                                                   "https://doc.rasdaman.org/stable/05_geo-services-guide.html#case-distinction"]),
                    wcps='switch case <x> return <accept> default return <reject>'
                    )


save_process_descriptions(
    'Logical',
    [gen_logical('and', 'conjunction', True, False, 'true where `x` and `y` are true'),
     gen_logical('or', 'disjunction', True, True, 'true where at least one of `x` or `y` is true'),
     gen_logical('xor', 'exclusive disjunction', False, True, 'true where `x` != `y`'),
     gen_logical('is', 'equality', True, False, 'true where `x` = `y`'),
     gen_not(),
     gen_is_nan(),
     gen_is_nodata(),
     gen_is_valid(),
     gen_if()
     ]
)

# ------------------------------------------------------------------------------
# Exponential: pow, exp, log, ln, sqrt
# ------------------------------------------------------------------------------


trig_type_rules = [
    ['[unsigned] char,short,float', 'float'],
    ['[unsigned] int,double', 'double'],
]


def gen_exponential(id: str, summary: str, example_result):
    link = ''.join([p.capitalize() for p in summary.split()])
    return generate(id, summary.capitalize(),
                    get_unary_description(f'Computes the {summary} of `x` (*`{id}(x)`*); ', trig_type_rules),
                    get_unary_param('The operand.'),
                    get_returns(f"Null where `x` is null, otherwise the computed {summary} of `x`."),
                    get_num_unary_examples(example_result),
                    get_links(link_induced_docs + [
                        f"{summary.capitalize()} explained by Wolfram MathWorld",
                        f"https://mathworld.wolfram.com/{link}.html",
                    ]),
                    wcps=f'{id}(<x>)',
                    )


def gen_power():
    return generate('power', 'Exponentiation',
                    get_binary_description('Computes base `x` raised to the power of `y` (*`pow(x, y)`*); `y` must be of floating-point type.',
                                           [['[unsigned] char,short,float', 'float,double', 'float'],
                                            ['[unsigned] long,double', 'float,double', 'double']]),
                    get_binary_params('The base.', 'The exponent.'),
                    get_returns("The computed value for `x` raised to the power of `y`."),
                    get_num_binary_examples(55.90169943749474),
                    get_links(link_induced_docs + [
                        "Power explained by Wolfram MathWorld",
                        "https://mathworld.wolfram.com/Power.html",
                    ]),
                    wcps='pow(<x>, <y>)',
                    )


# Exponential: pow, exp, log, ln, sqrt
save_process_descriptions(
    'Exponential',
    [gen_power(),
     gen_exponential('exp', 'natural exponential', 148.4131591025766),
     gen_exponential('log', 'common logarithm', 0.6989700043360189),
     gen_exponential('ln', 'natural logarithm', 1.6094379124341),
     gen_exponential('sqrt', 'square root', 2.23606797749979),
     ]
)


# ------------------------------------------------------------------------------
# Trigonometric: sin  cos  tan  sinh  cosh  tanh  arcsin  arccos  arctan arctan2
# ------------------------------------------------------------------------------


def gen_trig(id: str, summary: str, example_result):
    link = ''.join([p.capitalize() for p in summary.split()])
    return generate(id, summary.capitalize(),
                    get_unary_description(f'Computes the {summary} of `x` (*`{id}(x)`*); ', trig_type_rules),
                    get_unary_param('The operand (angle in radians).'),
                    get_returns(f"Null where `x` is null, otherwise the computed {summary} of `x`."),
                    get_num_unary_examples(example_result),
                    get_links(link_induced_docs + [
                        f"{summary.capitalize()} explained by Wolfram MathWorld",
                        f"https://mathworld.wolfram.com/{link}.html",
                    ]),
                    wcps=f'{id}(<x>)',
                    )


save_process_descriptions(
    'Trigonometric',
    [
        gen_trig('sin', 'sine', -0.9589242746631385),
        gen_trig('cos', 'cosine', 0.2836621854632262),
        gen_trig('tan', 'tangent', -3.380515006246586),
        gen_trig('sinh', 'hyperbolic sine', 74.20321057778875),
        gen_trig('cosh', 'hyperbolic cosine', 74.20994852478785),
        gen_trig('tanh', 'hyperbolic tangent', 0.9999092042625951),
        gen_trig('arcsin', 'inverse sine', 'NaN'),
        gen_trig('arccos', 'inverse cosine', 'NaN'),
        gen_trig('arctan', 'inverse tangent', 1.373400766945016),
        # gen_trig('arctan2', '2-argument inverse tangent', -0.9589242746631385),
    ]
)

# ------------------------------------------------------------------------------
# Bitwise: bit, &, |, ^, ~
# ------------------------------------------------------------------------------


bitwise_links = link_induced_docs + [
    "Bitwise logic operators in C++",
    "https://en.cppreference.com/w/cpp/language/operator_arithmetic#Bitwise_logic_operators",
    "Bitwise operations by Wikipedia",
    "https://en.wikipedia.org/wiki/Bitwise_operation",
]


def gen_bitwise(id: str, op: str, example_result):
    return generate('bitwise_' + id, 'Bitwise ' + id.upper(),
                    get_binary_description(f'Computes the bitwise {id.upper()} of `x` and `y` (*`x {op} y`*).', [['X', 'X', 'X']]),
                    get_binary_params('The first operand.', 'The second operand.'),
                    get_returns(f"Null where `x` is null, otherwise the bitwise {id.upper()} of `x` and `y`."),
                    get_num_binary_examples(example_result, floats=False),
                    get_links(bitwise_links),
                    wcps=f'<x> {op} <y>',
                    )


def gen_bitwise_not():
    return generate('bitwise_not', 'Bitwise NOT',
                    get_unary_description(f'Computes the bitwise NOT of `x` (*`~x`*); ', [['X', 'X']]),
                    get_unary_param('The operand.'),
                    get_returns(f"Null where `x` is null, otherwise the bitwise NOT of `x`."),
                    get_num_unary_examples(2147483642),
                    get_links(bitwise_links),
                    wcps=f'~<x>',
                    )


def gen_bit():
    return generate('bit', 'Extract bit value',
                    get_binary_description('Extract the value of a bit at a non-negative position `y` in the binary representation of an integer `x` '
                                           '(*`bit(x, y)`*). Position counting starts from 0, and runs from least to most significant bit; the '
                                           'operation is effectively equivalent to `(x >> y) & 1`.',
                                           [['[unsigned] char,short,int', '[unsigned] char,short,int', 'bool']]),
                    get_binary_params('The operand.', 'The bit position.'),
                    get_returns(f"Null where `x` is null, otherwise the extracted bit as a boolean value.", schema=schema_bool_array),
                    get_num_binary_examples(True, floats=False),
                    get_links(bitwise_links),
                    wcps=f'bit(<x>, <y>)',
                    )


save_process_descriptions(
    'Bitwise',
    [
        gen_bitwise('and', '&', 0),
        gen_bitwise('or', '|', 7),
        gen_bitwise('xor', '^', 7),
        gen_bitwise_not(),
        gen_bit(),
    ]
)


# ------------------------------------------------------------------------------
# Type conversion
# ------------------------------------------------------------------------------


def gen_cast():
    x = get_unary_param('The operand.')[0]
    y = get_unary_param('The new type. [unsigned] char, short, and int are 8, 16, and 32-bit integer respectively; cint16 and cint32 are complex of '
                        '16 and 32-bit integer respectively; complex and complex2 are complex of single and double-precision floating-point.', name='y')[0]
    y['schema'] = {
        "type": "string",
        "enum": ["boolean", "char", "short", "int", "unsigned char", "unsigned short", "unsigned int",
                 "float", "double", "cint16", "cint32", "complex", "complex2"]
    }
    parameters = [x, y]
    examples = [{'arguments': {'x': 5.4, 'y': 'int'}, 'returns': 5},
                {'arguments': {'x': 'DatacubeA', 'y': 'float'}, 'returns': 'DatacubeB'}]

    return generate('cast', 'Type conversion',
                    get_unary_description(f'Change the type of `x` to a new type `y` (*`(y)x`*); the type conversion rules of C++ apply.',
                                          [['X', 'Y', 'Y']]),
                    parameters,
                    get_returns(f"Null where `x` is null, otherwise the original values of `x` cast to the new type specified by `y`."),
                    examples,
                    get_links(link_induced_docs + [
                        "Type conversion explained by Wikipedia"
                        "https://en.wikipedia.org/wiki/Type_conversion"
                    ]),
                    wcps=f'(<y>)<x>',
                    )


save_process_descriptions('Type conversion', [gen_cast()])

# ------------------------------------------------------------------------------
# Aggregation: avg, add / sum, min, max, all, some, count, sd, var
# ------------------------------------------------------------------------------


type_rules_sum = [['any complex type', 'CFloat64'], ['float,double', 'double'], ['char,short,long', 'long'],
                  ['[unsigned] char,short,long', 'unsigned long']]


def get_num_preamble(summary: str, wcps_op: str):
    return f'Computes the {summary} of a datacube `x` (*`{wcps_op}(x)`*).'


def get_bool_preamble(summary: str, wcps_op: str):
    return f'{summary} in a datacube `x` (*`{wcps_op}(x)`*).'


def gen_aggregation(id: str, wcps_op: str, summary: str, get_preamble, type_rules, ex1_result, ex2_result=None):
    return generate(id, summary.capitalize(),
                    get_aggregation_description(get_preamble(summary, wcps_op), type_rules),
                    get_unary_param('A datacube.', schema=schema_datacube),
                    get_returns(f"The computed {summary} of `x`; null elements are ignored."),
                    # num example: [0,1,2,null,3], bool: [True,null,True] and [True,null,False]
                    get_num_aggregation_examples(ex1_result) if ex2_result is None else get_bool_aggregation_examples(ex1_result, ex2_result),
                    get_links(link_scalar_ops_docs),
                    wcps=f'{wcps_op}(<x>)',
                    )


def gen_aggregate_temporal():
    descr = """Computes a temporal aggregation based on an array of temporal intervals.\n\nFor common regular calendar hierarchies such as year, month, week or
seasons ``aggregate_temporal_period()`` can be used. Other calendar hierarchies must be transformed into specific intervals by the clients.\n\n
For each interval, all data along the dimension will be passed through the reducer.\n\nThe computed values will be projected to the labels.
If no labels are specified, the start of the temporal interval will be used as label for the corresponding values. In case of a conflict (i.e. the
user-specified values for the start times of the temporal intervals are not distinct), the user-defined labels must be specified in the parameter
`labels` as otherwise a `DistinctDimensionLabelsRequired` exception would be thrown. The number of user-defined labels and the number of intervals need to
be equal.\n\nIf the dimension is not set or is set to `null`, the data cube is expected to only have one temporal dimension."""

    parameters = [
        {"name": "data", "description": "A data cube.",
         "schema": {"type": "object", "subtype": "datacube", "dimensions": [{"type": "temporal"}]}},
        {
            "name": "extent",
            "description": "Closed temporal interval, i.e. an array with exactly two elements:\n\n"
                           "1. The first element is the start of the temporal interval. The specified time instant is **included** in the interval.\n"
                           "2. The second element is the end of the temporal interval. The specified time instant is **included** in the interval.\n\n"
                           "The second element must always be equal or greater than the first element. Otherwise, a `TemporalExtentEmpty` exception is thrown.\n\n"
                           "Also supports unbounded intervals by setting one of the boundaries to `null`, but never both.",
            "schema": {
                "type": "array", "subtype": "temporal-interval", "minItems": 2, "maxItems": 2,
                "items": {
                    "anyOf": [
                        {"type": "string", "format": "date-time", "subtype": "date-time", "description": "Date and time with a time zone."},
                        {"type": "string", "format": "date", "subtype": "date",
                         "description": "Date only, formatted as `YYYY-MM-DD`. The time zone is UTC. Missing time components are all 0."},
                        {"type": "null"}
                    ]
                },
                "examples": [["2015-01-01T00:00:00Z", "2016-01-01T00:00:00Z"], ["2015-01-01", "2016-01-01"]]
            }
        },
        {
            "name": "reducer",
            "description": """A reducer to be applied for the values contained in each interval. A reducer is a single process such as ``mean()`` or a set of
processes, which computes a single value for a list of values, see the category 'reducer' for such processes. Intervals may not contain any values, which for
most reducers leads to no-data (`null`) values by default.""",
            "schema": {
                "type": "object",
                "subtype": "process-graph",
                "parameters": [
                    {
                        "name": "data", "description": "A datacube.",
                        "schema": {"type": "object", "subtype": "datacube", "items": {"description": "Any data type."}}
                    },
                    {
                        "name": "context", "description": "Additional data passed by the user.",
                        "schema": {"description": "Any data type."}, "optional": True, "default": None
                    }
                ],
                "returns": {"description": "The value to be set in the new data cube.", "schema": {"description": "Any data type."}}
            }
        },
        {
            "name": "dimension",
            "description": "The name of the temporal dimension to filter on. If no specific dimension is specified, the filter applies to all "
                           "temporal dimensions. Fails with a `DimensionNotAvailable` exception if the specified dimension does not exist.",
            "schema": {"type": ["string", "null"]}, "default": None, "optional": True
        },
        {
            "name": "context", "description": "Additional data to be passed to the reducer.", "schema": {"description": "Any data type."}, "optional": True,
            "default": None
        }
    ]
    returns = {
        "description": "A new data cube with the same dimensions. The dimension properties (name, type, labels, reference system and resolution) remain "
                       "unchanged, except for the resolution and dimension labels of the given temporal dimension.",
        "schema": {"type": "object", "subtype": "datacube", "dimensions": [{"type": "temporal"}]}
    }
    return generate('aggregate_temporal', 'Temporal aggregation', descr,
                    parameters, returns,
                    [],
                    get_links([]),
                    wcps="condense",
                    exceptions=get_exceptions([
                        'DimensionNotAvailable',
                        "A dimension with the specified name does not exist.",
                        'TemporalExtentEmpty',
                        'The temporal extent is empty. The second instant in time must always be greater/later than the first instant in time.',
                        'DistinctDimensionLabelsRequired',
                        "The dimension labels have duplicate values. Distinct labels must be specified.",
                        'TooManyDimensions',
                        "The data cube contains multiple temporal dimensions. The parameter `dimension` must be specified."
                    ])
                    )


save_process_descriptions(
    'Aggregation',
    [
        gen_aggregation('mean', 'avg', "arithmetic mean (average)", get_num_preamble, [['any complex type', 'CFloat64'], ['X', 'double']], 1.5),
        gen_aggregation('sum', 'add', "sum of all elements", get_num_preamble, type_rules_sum, 6),
        gen_aggregation('min', 'min', "minimum value", get_num_preamble, [['X', 'X']], 0),
        gen_aggregation('max', 'max', "maximum value", get_num_preamble, [['X', 'X']], 3),
        gen_aggregation('sd', 'stats.stddev_samp', "sample standard deviation", get_num_preamble, [['any complex type', 'error'], ['X', 'double']],
                        1.290994449),
        gen_aggregation('sd_pop', 'stats.stddev_pop', "population standard deviation", get_num_preamble, [['any complex type', 'error'], ['X', 'double']],
                        1.118033989),
        gen_aggregation('variance', 'stats.var_samp', "sample variance", get_num_preamble, [['any complex type', 'error'], ['X', 'double']], 1.666666667),
        gen_aggregation('variance_pop', 'stats.var_pop', "population variance", get_num_preamble, [['any complex type', 'error'], ['X', 'double']], 1.25),
        gen_aggregation('count', 'count', "count of true elements", get_num_preamble, [['bool', 'unsigned long']], 2, 1),
        gen_aggregation('any', 'some', "Checks if at least one elements is true", get_bool_preamble, [['bool', 'bool']], True, True),
        gen_aggregation('all', 'all', "Checks if all elements are true", get_bool_preamble, [['bool', 'bool']], True, False),
        # gen_aggregate_temporal(),  # TODO
    ]
)


# ------------------------------------------------------------------------------
# Subsetting: band, axis
# ------------------------------------------------------------------------------


def gen_subset():
    descr = """Select a subset of the datacube `x` by restricting some (or all) dimensions of `x`
(*`x[dim1(lower:upper), dim2(slice), dim3:"non-native-crs"(...)]`*).\n\n
The result is a datacube in which the sliced dimensions are removed from `x`, while the trimmed dimensions are reduced to the new
lower/upper bounds (lower <= upper). If all dimensions are sliced then the result is a datacube of dimension 0, i.e. a scalar value.
Dimensions which are not specified in the subset_list are not changed in the result."""
    subset_schema = {
        "type": "array",
        "subtype": "subset-list",
        "title": "One or more dimension trims or slices.",
        "description": "Dimension trims or slices, with coordinates optionally specified in a CRS different from the native CRS of `x`",
        "minItems": 1,
        "uniqueItems": True,
        "items": {
            "type": "object",
            "subtype": "subset-spec",
            "title": "A slice or trim specification",
            "description": "A subset specification with required fields `dimension` and `lower`, and optionally `upper`, and `crs`. If `upper` is not specified"
                           " then the subset is a *slice* operation, otherwise it's a *trim*. If the `crs` is not specified, then the `lower` and `upper` "
                           "coordinates must be specified in the native CRS of `x`, otherwise in the specified `crs`.",
            "required": ['dimension', 'lower'],
            "properties": {
                'dimension': {'type': 'string', 'description': 'The name of the dimension to be subsetted.'},
                'lower': {'type': ['number', 'string'], 'description': 'The slice or lower trim coordinate.'},
                'upper': {'type': ['number', 'string', 'null'], 'description': 'The upper trim coordinate.', 'default': None, 'optional': True},
                'crs': {'type': ['string', 'null'], 'description': 'A specific CRS for the `lower`/`upper coordinates.', 'default': None, 'optional': True}
            },
            "examples": [
                {'dimension': 'Lat', 'lower': 12.5, 'upper': 59},
                {'dimension': 'Lat', 'lower': 12.5, 'crs': 'EPSG:4326'},
                {'dimension': 'ansi', 'lower': '2023-08-01'}
            ]
        },
        'examples': [
            [{'dimension': 'Lat', 'lower': 12.5, 'upper': 59},
             {'dimension': 'Lon', 'lower': 48, 'crs': 'EPSG:4326'},
             {'dimension': 'ansi', 'lower': '2023-08-01'}]
        ]
    }
    parameters = [
        get_unary_param("A datacube.", schema=schema_datacube, name="data")[0],
        get_unary_param('A list of dimension subsets (trims or slices).', schema=subset_schema, name="subset")[0]
    ]
    return generate('subset', 'Select a datacube subset', descr, parameters,
                    get_returns("The elements of `x` which are within the specified subset."),
                    [],
                    get_links([
                        "Subsetting as a coverage operation in rasdaman",
                        "https://doc.rasdaman.org/11_cheatsheets.html#coverage-operations"
                    ]),
                    wcps='f(data,subset) ::= <%(<data>)[<subset:sub(); separator=",">]%>\n'
                         'sub(s) ::= <%<s.dimension><if(s.crs)>:"<s.crs>"<endif>(<s.lower><if(s.upper)>:<s.upper><endif>)%>'
                    )


def gen_subset_band():
    descr = get_unary_description('Extract the band by name or (0-based) index from `x` (*`x.y`*).', None)
    descr += "The result is single-band, with same type and nullset as the selected band in `x`."
    return generate('subset_band', 'Select a band by name or index', descr,
                    get_binary_params('A multiband operand.', 'The band name or index.',
                                      schema1=schema_num_array, schema2=get_schema(['band-name', 'integer'])),
                    get_returns("Null where `x` is null, otherwise the extracted band scalar or datacube."),
                    [
                        {'arguments': {'x': {'red': 1, 'green': 2, 'blue': 3}, 'y': 'green'}, 'returns': 2},
                        {'arguments': {'x': {'red': 1, 'green': 2, 'blue': 3}, 'y': 1}, 'returns': 2},
                        {'arguments': {'x': 'DatacubeSentinel2', 'y': 'B04'}, 'returns': 'DatacubeB04'},
                    ],
                    get_links([
                        "Struct component selection in rasdaman",
                        "https://doc.rasdaman.org/04_ql-guide.html#struct-component-selection"
                    ]),
                    wcps='<x>.<y>',
                    )


def gen_filter_bands():
    descr = """Filters the bands in the data cube so that bands that don't match any of the criteria are dropped from the data cube.
The data cube is expected to have only one dimension of type `bands`. Fails with a `DimensionMissing` exception if no such dimension exists.\n\n

The following criteria can be used to select bands:\n\n

* `bands`: band name or common band name (e.g. `B01`, `B8A`, `red` or `nir`)\n
* `wavelengths`: ranges of wavelengths in micrometers (μm) (e.g. 0.5 - 0.6)\n\n

All these information are exposed in the band metadata of the collection. To keep algorithms interoperable it is recommended to prefer the common
band names or the wavelengths over band names that are specific to the collection and/or back-end.\n\nIf multiple criteria are specified, any of
them must match and not all of them, i.e. they are combined with an OR-operation. If no criteria are specified, the `BandFilterParameterMissing`
exception must be thrown.\n\n

**Important:** The order of the specified array defines the order of the bands in the data cube, which can be important
for subsequent processes. If multiple bands are matched by a single criterion (e.g. a range of wavelengths), they stay in the original order."""

    data = OrderedDict()
    data['name'] = 'data'
    data['description'] = "A data cube with bands."
    data['schema'] = {"type": "object", "subtype": "datacube"}
    bands = OrderedDict()
    bands['name'] = 'bands'
    bands['description'] = """A list of band names. Either the unique band name (metadata field `name` in bands) or one of the common band names
(metadata field `common_name` in bands). If the unique band name and the common name conflict, the unique band name has a higher priority.\n\n
The order of the specified array defines the order of the bands in the data cube. If multiple bands match a common name, all matched bands are
included in the original order."""
    bands['schema'] = {"type": "array", "items": {"type": "string", "subtype": "band-name"}}
    bands['default'] = []
    bands['optional'] = True
    # wavelengths: unsupported
    parameters = [data, bands]

    returns = {
        "description": "A data cube limited to a subset of its original bands. The dimensions and dimension properties (name, type, labels, "
                       "reference system and resolution) remain unchanged, except that the dimension of type `bands` has less (or the same) dimension labels.",
        "schema": {
            "type": "object",
            "subtype": "datacube",
            "dimensions": [
                {
                    "type": "bands"
                }
            ]
        }
    }
    return generate('filter_bands', 'Filter the bands by names', descr, parameters, returns,
                    [],
                    get_links([]),
                    wcps='<x>.<y>',  # TODO
                    exceptions=get_exceptions([
                        'BandFilterParameterMissing',
                        "The process `filter_bands` requires any of the parameters `bands`, `common_names` or `wavelengths` to be set.",
                        'DimensionMissing',
                        'A band dimension is missing.'])
                    )


def gen_filter_bbox():
    descr = """Limits the data cube to the specified bounding box. The filter retains a pixel in the data cube if the point at the pixel center
intersects with the bounding box (as defined in the Simple Features standard by the OGC). Alternatively, ``filter_spatial()`` can be used to filter by geometry."""
    parameters = [
        get_unary_param("A data cube.")[0],
        {
            "name": "extent",
            "description": "A bounding box, which may include a vertical axis (see `base` and `height`).",
            "schema": {
                "type": "object", "subtype": "bounding-box",
                "required": ["west", "south", "east", "north"],
                "properties": {
                    "west": {"description": "West (lower left corner, coordinate axis 1).", "type": "number"},
                    "south": {"description": "South (lower left corner, coordinate axis 2).", "type": "number"},
                    "east": {"description": "East (upper right corner, coordinate axis 1).", "type": "number"},
                    "north": {"description": "North (upper right corner, coordinate axis 2).", "type": "number"},
                    "base": {"description": "Base (optional, lower left corner, coordinate axis 3).", "type": ["number", "null"], "default": None},
                    "height": {"description": "Height (optional, upper right corner, coordinate axis 3).", "type": ["number", "null"], "default": None},
                    "crs": {
                        "description": "Coordinate reference system of the extent, specified as an [EPSG code](http://www.epsg-registry.org/). "
                                       "Defaults to `4326` (EPSG code 4326) unless the client explicitly requests a different coordinate reference system.",
                        "title": "EPSG Code", "type": "integer", "subtype": "epsg-code", "minimum": 1000, "examples": [3857], "default": 4326
                    }
                }
            }
        }
    ]
    returns = {
        "description": "A data cube restricted to the bounding box. The dimensions and dimension properties (name, type, labels, reference system "
                       "and resolution) remain unchanged, except that the spatial dimensions have less (or the same) dimension labels.",
        "schema": {"title": "Raster data cube", "type": "object", "subtype": "datacube", "dimensions": [{"type": "spatial", "axis": ["x", "y"]}]}
    }
    return generate('filter_bbox', 'Spatial filter using a bounding box', descr, parameters, returns,
                    [],
                    get_links([]),
                    wcps='<x>['
                         'x:"EPSG:<extent.crs>"(<extent.west>:<extent.east),'
                         'y:"EPSG:<extent.crs>"(<extent.south>:<extent.north)'
                         '<if(extent.base)>, <baseAxis>(<extent.base>:<extent.height>)<endif>'
                         ']',
                    )


def gen_filter_spatial():
    descr = """Limits the datacube `x` to the specified WKT geometry.\n\nAlternatively, use ``filter_bbox()`` to filter by bounding box."""
    parameters = [
        get_unary_param('A datacube.')[0],
        get_unary_param("A WKT string specifying the geometry to be clipped from `x`; the areas outside of the geometry are set to null.",
                        schema=get_schema(['string']), name='wkt')[0]
    ]
    return generate('filter_spatial', 'Spatial filtering of raster data cubes with a geometry', descr, parameters,
                    get_returns("A raster data cube restricted to the specified WKT geometry."),
                    [],
                    get_links([]),
                    wcps='clip(<x>, <wkt>)',
                    )


def gen_filter_temporal():
    descr = """Limits the data cube to the specified interval of dates and/or times.\n\n

More precisely, the filter checks whether each of the temporal dimension labels is greater than or equal to the lower boundary (start date/time) and
less than the value of the upper boundary (end date/time). This corresponds to a left-closed interval, which contains the lower boundary but not the
upper boundary."""
    parameters = [
        {"name": "data", "description": "A data cube.",
         "schema": {"type": "object", "subtype": "datacube", "dimensions": [{"type": "temporal"}]}},
        {
            "name": "extent",
            "description": "Left-closed temporal interval, i.e. an array with exactly two elements:\n\n"
                           "1. The first element is the start of the temporal interval. The specified time instant is **included** in the interval.\n"
                           "2. The second element is the end of the temporal interval. The specified time instant is **excluded** from the interval.\n\n"
                           "The second element must always be greater/later than the first element. Otherwise, a `TemporalExtentEmpty` exception is thrown.\n\n"
                           "Also supports unbounded intervals by setting one of the boundaries to `null`, but never both.",
            "schema": {
                "type": "array", "subtype": "temporal-interval", "minItems": 2, "maxItems": 2,
                "items": {
                    "anyOf": [
                        {"type": "string", "format": "date-time", "subtype": "date-time", "description": "Date and time with a time zone."},
                        {"type": "string", "format": "date", "subtype": "date",
                         "description": "Date only, formatted as `YYYY-MM-DD`. The time zone is UTC. Missing time components are all 0."},
                        {"type": "null"}
                    ]
                },
                "examples": [["2015-01-01T00:00:00Z", "2016-01-01T00:00:00Z"], ["2015-01-01", "2016-01-01"]]
            }
        },
        {
            "name": "dimension",
            "description": "The name of the temporal dimension to filter on. If no specific dimension is specified, the filter applies to all "
                           "temporal dimensions. Fails with a `DimensionNotAvailable` exception if the specified dimension does not exist.",
            "schema": {"type": ["string", "null"]}, "default": None, "optional": True
        }
    ]
    returns = {
        "description": "A data cube restricted to the specified temporal extent. The dimensions and dimension properties (name, type, labels, "
                       "reference system and resolution) remain unchanged, except that the temporal dimensions (determined by `dimensions` "
                       "parameter) may have less dimension labels.",
        "schema": {"type": "object", "subtype": "datacube", "dimensions": [{"type": "temporal"}]}
    }
    return generate('filter_temporal', 'Temporal filter based on temporal intervals', descr,
                    parameters, returns,
                    [],
                    get_links([]),
                    wcps=None,  # TODO
                    exceptions=get_exceptions([
                        'DimensionNotAvailable',
                        "A dimension with the specified name does not exist.",
                        'TemporalExtentEmpty',
                        'The temporal extent is empty. The second instant in time must always be greater/later than the first instant in time.'
                    ])
                    )


def gen_load_collection():
    descr = """Loads a collection by its id and returns it as a processable data cube. The data that is added to the data cube can be restricted
with the parameters `spatial_extent`, `temporal_extent`, `bands` and `properties`. If no data is available for the given extents, a
`NoDataAvailable` exception is thrown.\n\n
**Remarks:**\n\n
* The bands (and all dimensions that specify nominal dimension labels) are expected to be ordered as specified in the metadata if the `bands`
parameter is set to `null`.\n
* If no additional parameter is specified this would imply that the whole data set is expected to be loaded. Due to the large size of many data
sets, this is not recommended and may be optimized by back-ends to only load the data that is actually required after evaluating subsequent
processes such as filters. This means that the values in the data cube should be processed only after the data has been limited to the required
extent and as a consequence also to a manageable size."""

    id = OrderedDict()
    id['name'] = 'id'
    id['description'] = 'The collection id.'
    id['schema'] = {"type": "string", "subtype": "collection-id", "pattern": "^[\\w\\-\\.~/]+$"}
    spatial_extent = OrderedDict()
    spatial_extent['name'] = 'spatial_extent'
    spatial_extent['description'] = """Limits the data to load from the collection to the specified bounding box or WKT geometry.\n\n
The process loads a pixel into the data cube if the point at the pixel center intersects with the bounding box or the WKT geometry.\n\n
Set this parameter to `null` to set no limit for the spatial extent."""
    spatial_extent['schema'] = [
        {
            "title": "Bounding Box", "type": "object", "subtype": "bounding-box", "required": ["west", "south", "east", "north"],
            "properties": {
                "west": {"description": "West (lower left corner, coordinate axis 1).", "type": "number"},
                "south": {"description": "South (lower left corner, coordinate axis 2).", "type": "number"},
                "east": {"description": "East (upper right corner, coordinate axis 1).", "type": "number"},
                "north": {"description": "North (upper right corner, coordinate axis 2).", "type": "number"},
                "base": {"description": "Base (optional, lower left corner, coordinate axis 3).", "type": ["number", "null"], "default": None},
                "height": {"description": "Height (optional, upper right corner, coordinate axis 3).", "type": ["number", "null"], "default": None},
                "crs": {
                    "description": "Coordinate reference system of the extent, specified as as [EPSG code](http://www.epsg-registry.org/) "
                                   "or [WKT2 CRS string](http://docs.opengeospatial.org/is/18-010r7/18-010r7.html). "
                                   "Defaults to `4326` (EPSG code 4326) unless the client explicitly requests a different coordinate reference system.",
                    "anyOf": [{"title": "EPSG Code", "type": "integer", "subtype": "epsg-code", "minimum": 1000, "examples": [3857]},
                              {"title": "WKT2", "type": "string", "subtype": "wkt2-definition"}],
                    "default": 4326
                }
            }
        },
        get_unary_param("A WKT string specifying the geometry to be clipped from `x`; the areas outside of the geometry are set to null.",
                        schema=get_schema(['string']), name='wkt')[0],
        {"title": "No filter", "description": "Don't filter spatially. All data is included in the data cube.", "type": "null"}
    ]
    temporal_extent = OrderedDict()
    temporal_extent['name'] = 'temporal_extent'
    temporal_extent['description'] = """Limits the data to load from the collection to the specified left-closed temporal interval. Applies to all
temporal dimensions. The interval has to be specified as an array with exactly two elements:\n\n
1. The first element is the start of the temporal interval. The specified time instant is **included** in the interval.\n
2. The second element is the end of the temporal interval. The specified time instant is **excluded** from the interval.\n\n
The second element must always be greater/later than the first element. Otherwise, a `TemporalExtentEmpty` exception is thrown.\n\n
Also supports unbounded intervals by setting one of the boundaries to `null`, but never both.\n\n
Set this parameter to `null` to set no limit for the temporal extent."""
    temporal_extent['schema'] = [
        {
            "type": "array", "subtype": "temporal-interval", "uniqueItems": True, "minItems": 2, "maxItems": 2,
            "items": {
                "anyOf": [
                    {"type": "string", "format": "date-time", "subtype": "date-time", "description": "Date and time with a time zone."},
                    {"type": "string", "format": "date", "subtype": "date",
                     "description": "Date only, formatted as `YYYY-MM-DD`. The time zone is UTC. Missing time components are all 0."},
                    {"type": "null"}
                ]
            },
            "examples": [["2015-01-01T00:00:00Z", "2016-01-01T00:00:00Z"], ["2015-01-01", "2016-01-01"]]
        },
        {"title": "No filter", "description": "Don't filter temporally. All data is included in the data cube.", "type": "null"}
    ]
    bands = OrderedDict()
    bands['name'] = 'bands'
    bands['description'] = """Only adds the specified bands into the data cube so that bands that don't match the list of band names are not
available. Applies to all dimensions of type `bands`.\n\n
Either the unique band name (metadata field `name` in bands) or one of the common band names (metadata field `common_name` in bands) can be specified.
If the unique band name and the common name conflict, the unique band name has a higher priority.\n\n
The order of the specified array defines the order of the bands in the data cube. If multiple bands match a common name, all matched bands are
included in the original order."""
    bands['schema'] = [
        {"type": "array", "minItems": 1, "items": {"type": "string", "subtype": "band-name"}},
        {"title": "No filter", "description": "Don't filter bands. All bands are included in the data cube.", "type": "null"}
    ]
    bands['default'] = None
    bands['optional'] = True
    properties = OrderedDict()
    properties['name'] = 'properties'
    properties['description'] = """Limits the data by metadata properties to include only data in the data cube which all given conditions return
`true` for (AND operation).\n\n
Specify key-value-pairs with the key being the name of the metadata property, which can be retrieved with the openEO Data Discovery for Collections.
The value must be a condition (user-defined process) to be evaluated against the collection metadata, see the example."""
    properties['schema'] = [
        {
            "type": "object", "subtype": "metadata-filter", "title": "Filters",
            "description": "A list of filters to check against. Specify key-value-pairs with the key being the name of the metadata property name "
                           "and the value being a process evaluated against the metadata values.",
            "additionalProperties": {
                "type": "object", "subtype": "process-graph",
                "parameters": [
                    {"name": "value", "description": "The property value to be checked against.", "schema": {"description": "Any data type."}}],
                "returns": {"description": "`true` if the data should be loaded into the data cube, otherwise `false`.", "schema": {"type": "boolean"}}
            }
        },
        {"title": "No filter", "description": "Don't filter by metadata properties.", "type": "null"}
    ]
    properties['default'] = None
    properties['optional'] = True
    parameters = [id, spatial_extent, temporal_extent, bands, properties]

    returns = get_returns("A data cube for further processing. The dimensions and dimension properties (name, type, labels, reference system "
                          "and resolution) correspond to the collection's metadata, but the dimension labels are restricted as specified in the parameters.",
                          schema=schema_datacube)

    examples = [{'arguments': {
        'description': 'Loading `S2_L2A` collection for 2018 and a given bbox.',
        'id': 'S2_L2A',
        "spatial_extent": {"west": 16.1, "east": 16.6, "north": 48.6, "south": 47.2},
        "temporal_extent": ["2018-01-01", "2019-01-01"],
    }}, ]

    return generate('load_collection', 'Load a collection', descr, parameters,
                    returns, examples,
                    get_links([]),
                    wcps=None,  # manually generate in petascope
                    exceptions=get_exceptions([
                        "NoDataAvailable",
                        "There is no data available for the given extents.",
                        "TemporalExtentEmpty",
                        "The temporal extent is empty. The second instant in time must always be greater/later than the first instant in time."
                    ])
                    )


save_process_descriptions(
    'Subsetting',
    [gen_subset(), gen_subset_band(), gen_load_collection(), gen_filter_spatial(), gen_filter_bbox(),
     # gen_filter_bands(), gen_filter_temporal()
     ]
)


# ------------------------------------------------------------------------------
# range constructor: merge_cubes?, General constructor/condenser
# ------------------------------------------------------------------------------


# ------------------------------------------------------------------------------
# Scale, Extend, crsTransform
# ------------------------------------------------------------------------------


def gen_resample_spatial():
    description = """Resamples the spatial dimensions (x,y) of the data cube to a specified resolution and/or warps the data cube to the
target projection. At least `resolution` or `projection` must be specified.\n\n
Related processes:\n\n
* Use ``filter_bbox()`` to set the target spatial extent.\n
* To spatially align two data cubes with each other (e.g. for merging), better use the process ``resample_cube_spatial()``."""
    parameters = [
        get_unary_param('A datacube')[0],
        {
            "name": "resolution",
            "description": "Resamples the data cube to the target resolution, which can be specified either as separate values for x and y or as a "
                           "single value for both axes. Specified in the units of the target projection. Doesn't change the resolution by default (`0`).",
            "schema": [
                {"description": "A single number used as the resolution for both x and y.", "type": "number", "minimum": 0},
                {
                    "description": "A two-element array to specify separate resolutions for x (first element) and y (second element).",
                    "type": "array", "minItems": 2, "maxItems": 2, "items": {"type": "number", "minimum": 0}
                }
            ],
            "default": 0,
            "optional": True
        },
        {
            "name": "projection",
            "description": "Warps the data cube to the target projection, specified as as [EPSG code](http://www.epsg-registry.org/) or "
                           "[WKT2 CRS string](http://docs.opengeospatial.org/is/18-010r7/18-010r7.html). By default (`null`), the projection is "
                           "not changed.",
            "schema": [
                {"title": "EPSG Code", "type": "integer", "subtype": "epsg-code", "minimum": 1000, "examples": [3857]},
                {"title": "Don't change projection", "type": "null"}
            ],
            "default": None,
            "optional": True
        },
        {
            "name": "method",
            "description": """Resampling method to use. The following options are available and are meant to align with
[`gdalwarp`](https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-r):\n\n* `average`: average (mean) resampling, computes the weighted
average of all valid pixels\n* `bilinear`: bilinear resampling\n* `cubic`: cubic resampling\n* `cubicspline`: cubic spline resampling\n
* `lanczos`: Lanczos windowed sinc resampling\n* `max`: maximum resampling, selects the maximum value from all valid pixels\n
* `med`: median resampling, selects the median value of all valid pixels\n* `min`: minimum resampling, selects the minimum value from all valid pixels\n
* `mode`: mode resampling, selects the value which appears most often of all the sampled points\n* `near`: nearest neighbour resampling (default)\n
* `q1`: first quartile resampling, selects the first quartile value of all valid pixels\n
* `q3`: third quartile resampling, selects the third quartile value of all valid pixels\n
* `rms` root mean square (quadratic mean) of all valid pixels\n* `sum`: compute the weighted sum of all valid pixels\n\n
Valid pixels are determined based on the function ``is_valid()``.""",
            "schema": {
                "type": "string",
                "enum": ["average", "bilinear", "cubic", "cubicspline", "lanczos", "max", "med", "min", "mode", "near", "q1", "q3", "rms", "sum"]
            },
            "default": "near",
            "optional": True
        },
        # {
        #     "name": "align",
        #     "description": "Specifies to which corner of the spatial extent the new resampled data is aligned to.",
        #     "schema": {"type": "string", "enum": ["lower-left", "upper-left", "lower-right", "upper-right"]},
        #     "default": "upper-left",
        #     "optional": True
        # }
    ]
    return generate('resample_spatial', 'Resample and warp the spatial dimensions', description,
                    parameters,
                    get_returns("A raster data cube with values warped onto the new projection. It has the same dimensions and the same dimension "
                                "properties (name, type, labels, reference system and resolution) for all non-spatial or vertical spatial dimensions. "
                                "For the horizontal spatial dimensions the name and type remain unchanged, but reference system, labels and "
                                "resolution may change depending on the given parameters.",
                                schema={"type": "object", "subtype": "datacube", "dimensions": [{"type": "spatial", "axis": ["x", "y"]}]}),
                    [],
                    get_links([
                        'Coverage operations in rasdaman',
                        'https://doc.rasdaman.org/11_cheatsheets.html#coverage-operations'
                        'Geographic projection in rasdaman',
                        'https://doc.rasdaman.org/04_ql-guide.html#geographic-projection',
                        'gdalwarp resampling methods',
                        'https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-r'
                    ]),
                    wcps='crsTransform( '
                         '<x>, '
                         '"EPSG:<projection>" '
                         '<if(method)>, { <method> }<endif> '
                         '<if(resolution)>, { x:resolution, y:resolution }<endif>'
                         ')',
                    )


def gen_resample_cube_spatial():
    description = """Resamples the spatial dimensions (x,y) from a source data cube `x` to align with the corresponding dimensions of the given
`target` data cube. Returns a new data cube with the resampled dimensions.\n\n

To resample a data cube to a specific resolution or projection regardless of an existing target data cube, refer to ``resample_spatial()``."""
    parameters = [
        get_unary_param('A datacube')[0],
        get_unary_param('A raster data cube that describes the spatial target resolution.', name='target')[0],
        {
            "name": "method",
            "description": """Resampling method to use. The following options are available and are meant to align with
        [`gdalwarp`](https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-r):\n\n* `average`: average (mean) resampling, computes the weighted
        average of all valid pixels\n* `bilinear`: bilinear resampling\n* `cubic`: cubic resampling\n* `cubicspline`: cubic spline resampling\n
        * `lanczos`: Lanczos windowed sinc resampling\n* `max`: maximum resampling, selects the maximum value from all valid pixels\n
        * `med`: median resampling, selects the median value of all valid pixels\n* `min`: minimum resampling, selects the minimum value from all valid pixels\n
        * `mode`: mode resampling, selects the value which appears most often of all the sampled points\n* `near`: nearest neighbour resampling (default)\n
        * `q1`: first quartile resampling, selects the first quartile value of all valid pixels\n
        * `q3`: third quartile resampling, selects the third quartile value of all valid pixels\n
        * `rms` root mean square (quadratic mean) of all valid pixels\n* `sum`: compute the weighted sum of all valid pixels\n\n
        Valid pixels are determined based on the function ``is_valid()``.""",
            "schema": {
                "type": "string",
                "enum": ["average", "bilinear", "cubic", "cubicspline", "lanczos", "max", "med", "min", "mode", "near", "q1", "q3", "rms", "sum"]
            },
            "default": "near",
            "optional": True
        },
    ]
    return generate('resample_cube_spatial', 'Resample the spatial dimensions to match a target data cube', description,
                    parameters,
                    get_returns("A raster data cube with the same dimensions. The dimension properties (name, type, labels, reference system and "
                                "resolution) remain unchanged, except for the resolution and dimension labels of the spatial dimensions.",
                                schema={"type": "object", "subtype": "datacube", "dimensions": [{"type": "spatial", "axis": ["x", "y"]}]}),
                    [],
                    get_links([
                        'Coverage operations in rasdaman',
                        'https://doc.rasdaman.org/11_cheatsheets.html#coverage-operations'
                        'Geographic projection in rasdaman',
                        'https://doc.rasdaman.org/04_ql-guide.html#geographic-projection',
                        'gdalwarp resampling methods',
                        'https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-r'
                    ]),
                    wcps='crsTransform( '
                         'crsSet(<target>) '  # TODO this variant with crsSet(target) is not yet supported
                         '<if(method)>, { <method> }<endif>, '
                         '{ domain(<target>) }'
                         ')',
                    )


def gen_resample_cube_temporal():
    description = """Resamples one or more given temporal dimensions from a source data cube to align with the corresponding dimensions
of the given target data cube using the nearest neighbor method. Returns a new data cube with the resampled dimensions.\n\n

By default, this process simply takes the nearest neighbor independent of the value (including values such as no-data / `null`). Depending on the data
cubes this may lead to values being assigned to two target timestamps."""
    parameters = [
        get_unary_param('A datacube')[0],
        get_unary_param('A raster data cube that describes the temporal target resolution.', name='target')[0],
        {
            "name": "dimension",
            "description": """The name of the temporal dimension to resample, which must exist with this name in both data cubes. If the dimension
is not set or is set to `null`, the process resamples all temporal dimensions that exist with the same names in both data cubes.\n\n
The following exceptions may occur:\n\n
* A dimension is given, but it does not exist in any of the data cubes: `DimensionNotAvailable`\n
* A dimension is given, but one of them is not temporal: `DimensionMismatch`\n
* No specific dimension name is given and there are no temporal dimensions with the same name in the data: `DimensionMismatch`""",
            "schema": {"type": ["string", "null"]},
            "default": None,
            "optional": True
        }
    ]
    return generate('resample_cube_temporal', 'Resample temporal dimensions to match a target data cube', description,
                    parameters,
                    get_returns("A data cube with the same dimensions and the same dimension properties (name, type, labels, reference system and "
                                "resolution) for all non-temporal dimensions. For the temporal dimension, the name and type remain unchanged, but the "
                                "dimension labels, resolution and reference system may change.",
                                schema=schema_datacube),
                    [],
                    get_links([
                        'Coverage operations in rasdaman',
                        'https://doc.rasdaman.org/11_cheatsheets.html#coverage-operations'
                        'Geographic projection in rasdaman',
                        'https://doc.rasdaman.org/04_ql-guide.html#geographic-projection',
                        'gdalwarp resampling methods',
                        'https://gdal.org/programs/gdalwarp.html#cmdoption-gdalwarp-r'
                    ]),
                    # TODO this variant with domain(target, dimension) is not yet supported
                    wcps='scale( <x>, { domain(<target>, <dimension>) } )',
                    )


def gen_scale():
    description = """Resample a datacube to a new resolution. The up- or down-scaling can be specified in several ways:\n\n
- A list of target grid extents per dimension. If only one of the x/y spatial axes is specified,
the other one will be auto-scaled to preserve the original ratio, while any other non-specified dimensions are not changed in the result.\n
- Another datacube can provide the target grid extents, which allows to easily match datacubes for binary operations.\n
- A factor by which all dimensions are resampled equally (factor > 1 for scaling up, 0 < factor < 1 for scaling down).\n
- A factor per dimension (factor > 1 for scaling up, 0 < factor < 1 for scaling down).\n\n
Only nearest-neighbour interpolation is supported currently."""

    scale_spec_schema = [{
        "title": "Target dimension grid extents.",
        "type": "array",
        "subtype": "extend-list",
        "description": "A list of target grid extents per dimension. If only one of the x/y spatial axes is specified, the other one will be auto-scaled "
                       "to preserve the original ratio, while any other non-specified dimensions are not changed in the result.",
        "minItems": 1,
        "uniqueItems": True,
        "items": {
            "type": "object",
            "subtype": "trim-spec",
            "title": "A dimension target grid extent.",
            "description": "A dimension name and target lower and upper grid extents.",
            "required": ['dimension', 'lower', 'upper'],
            "properties": {
                'dimension': {'type': 'string', 'description': 'The name of the dimension to be scaled.'},
                'lower': {'type': 'integer', 'description': 'The lower grid bound.'},
                'upper': {'type': 'integer', 'description': 'The upper grid bound.'}
            },
            "examples": [{'dimension': 'Lat', 'lower': 0, 'upper': 1280}, {'dimension': 'ansi', 'lower': 0, 'upper': 100}]
        },
        'examples': [
            [{'dimension': 'Lat', 'lower': 0, 'upper': 1280}, {'dimension': 'ansi', 'lower': 0, 'upper': 100}]
        ]},
        {"title": "Target datacube.", "type": "object", "subtype": "datacube", "description": "A datacube that provides the target grid extents."},
        {"title": "A scale factor.", "type": "number", "exclusiveMinimum": 0,
         "description": "A factor by which all dimensions are resampled equally (factor > 1 for scaling up, 0 < factor < 1 for scaling down)"},
        {
            "type": "array",
            "subtype": "slice-list",
            "title": "Per-dimension scale factors.",
            "description": "A factor per dimension (factor > 1 for scaling up, 0 < factor < 1 for scaling down).",
            "minItems": 1,
            "uniqueItems": True,
            "items": {
                "type": "object",
                "subtype": "slice-spec",
                "title": "A dimension scale factor.",
                "description": "A scale factor for a specific dimension.",
                "required": ['dimension', 'factor'],
                "properties": {
                    'dimension': {'type': 'string', 'description': 'The name of the dimension to be scaled.'},
                    'factor': {'type': 'number', "exclusiveMinimum": 0, 'description': 'The factor by which to scale the dimension.'}
                },
                "examples": [
                    {'dimension': 'Lat', 'factor': 0.5, "description": "Downscale spatial dimension Lat by 2x."},
                    {'dimension': 'ansi', 'factor': 3, "description": "Upscale temporal dimension ansi by 3x."}
                ]
            },
            'examples': [[
                {'dimension': 'Lat', 'factor': 0.5, "description": "Downscale spatial dimension Lat by 2x."},
                {'dimension': 'ansi', 'factor': 3, "description": "Upscale temporal dimension ansi by 3x."}
            ]]
        }
    ]
    parameters = [
        get_unary_param('A datacube', schema=schema_datacube, name='data')[0],
        get_unary_param('A specification for the how to scale `data`.', schema=scale_spec_schema, name='scale_spec')[0]
    ]
    return generate('scale', 'Resample a datacube', description,
                    parameters,
                    get_returns("The resampled datacube, with same dimension names and CRS but new resolutions on some or all dimensions.",
                                schema=schema_datacube),
                    [],
                    get_links([
                        'The scale coverage operation in rasdaman',
                        'https://doc.rasdaman.org/11_cheatsheets.html#coverage-operations'
                    ]),
                    wcps='scale(<data>,<scale_spec>)',
                    )


save_process_descriptions(
    'Geometric',
    [gen_resample_spatial(),
     gen_scale(),
     # gen_resample_cube_spatial(),
     # gen_resample_cube_temporal(),
     ]
)


# ------------------------------------------------------------------------------
# Encode
# ------------------------------------------------------------------------------


def gen_save_result():
    description = """Encodes `x` in the given file `format`; the encoding can be optionally customized with format `options`."""
    parameters = [
        get_unary_param('A datacube to be encoded in the target data format.')[0],
        {
            "name": "format",
            "description": "The data format to encode the datacube to. It must be one of the values that the server reports as supported output "
                           "file formats, which usually correspond to the short GDAL/OGR codes. This parameter is *case insensitive*.",
            "schema": {"type": "string", "subtype": "output-format"}
        },
        {
            "name": "options",
            "description": "The file format parameters to be used to create the file(s). Must correspond to the parameters that the server reports "
                           "as supported parameters for the chosen `format`. The parameter names and valid values usually correspond to the "
                           "GDAL/OGR format options.",
            "schema": {"type": "object", "subtype": "output-format-options"},
            "default": {},
            "optional": True
        }
    ]
    return generate('save_result', 'Save processed data', description, parameters,
                    get_returns("The datacube encoded to the target data format, as a 1-dimensional byte datacube.",
                                schema=schema_datacube),
                    [],
                    get_links([
                        'Coverage operations in rasdaman',
                        'https://doc.rasdaman.org/11_cheatsheets.html#coverage-operations'
                        'Data format in rasdaman',
                        'https://doc.rasdaman.org/04_ql-guide.html#rasql-encode-function-data-format',
                        'Format options in rasdaman',
                        'https://doc.rasdaman.org/04_ql-guide.html#rasql-encode-format-parameters',
                    ]),
                    wcps='encode('
                         '<x>, '
                         '"<format>"'
                         '<if(options)>, "<options>"<endif>'
                         ')',
                    )


save_process_descriptions(
    'Data exchange',
    [gen_save_result(),
     ]
)

os.system("ls *.json > index.txt")
