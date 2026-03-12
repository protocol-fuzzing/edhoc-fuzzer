import argparse
import pydot

def create_pattern_dct(pattern_string):
    pattern_dct = {
        'i': set(),
        'o': set(),
        'l': set()
    }

    if pattern_string is None or pattern_string == "":
        return pattern_dct

    for patt in pattern_string.strip('\' "').split(','):
        p = patt.strip()
        if p[0] not in ['i', 'o', 'l'] or p[1] != '_':
            raise Exception("Invalid pattern provided: " + p)

        if p[0] == 'l':
            i, o = map(lambda s: s.strip(), p[2:].split('/'))
            pattern_dct[p[0]].add(f"{i} / {o}")
        else:
            pattern_dct[p[0]].add(p[2:])

    return pattern_dct


def create_pattern_dct_with_label_symbol(label_symbol_pattern_string_list):
    label_pattern_dct = {
        'label_symbol': '/',
        'i': set(),
        'o': set(),
        'l': set()
    }

    if label_symbol_pattern_string_list is None or len(label_symbol_pattern_string_list) != 2:
        return label_pattern_dct

    label_symbol, pattern_string = label_symbol_pattern_string_list
    pattern_dct = create_pattern_dct(pattern_string)

    return {
        'label_symbol': label_symbol,
        **pattern_dct
    }


def read_replacement_file(filename, sep):
    """
    replacement_dct = {
        old_name: new_name,
        ...
    }
    """
    if not filename:
        return {}

    replacement_dct = {}
    with open(filename) as f:
        for line in f:
            pair = line.split(sep)

            if len(pair) != 2:
                print(f"Violated replacement format: 'old -> new' with line: {line}")
                exit(1)

            # remove any surrounding whitespace
            old, new = map(lambda s: s.strip(), pair)

            # in case of old name is already seen, inform and overwrite new name
            if old in replacement_dct:
                print(f"Overwriting replacement: [{old} -> {new}] will overwrite [{old} -> {replacement_dct[old]}]")

            replacement_dct[old] = new

    return replacement_dct


def get_info_from_graph(graph_name, shorten_node_names, remove_dct, label_symbol_dct, replacement_dct,
                        initial_hidden_node_name, initial_edge_label):
    """
        remove_dct = {
            'i': {i1, i2, ...},
            'o': {o1, o2, ...},
            'l': {i1 / o1, i2 / o2, ...}
        }

        label_symbol_dct = {
            'i': {i1, i2, ...},
            'o': {o1, o2, ...},
            'l': {i1 / o1, i2 / o2, ...}
        }

        replacement_dct = {
            old_name: new_name,
            ...
        }

        prefix 'r' means new replaced symbol name

        (returned) new_edge_info_dct = {
            (source, dest): {
                r_o1: [r_i1, r_i2, ...],
                r_o2: [r_i1, r_i2, ...],
                ...
            },
            ...
        }

        (returned) new_label_symbol_dct = {
            'i': {r_i1, r_i2, ...},
            'o': {r_o1, r_o2, ...},
            'l': {r_i1 / r_o1, r_i2 / r_o2, ...}
        }
    """
    def should_remove_label(label):
        i, o = label.strip('\' "').split(' / ')
        return (i in remove_dct['i']) or (o in remove_dct['o']) or (label in remove_dct['l'])

    def find_index_of_same_edges(edge):
        for idx, e in enumerate(graph.get_edge(edge.get_source(), edge.get_destination())):
            if edge.get_label() == e.get_label():
                return idx
        return -1

    def replace_symbol(s):
        return replacement_dct[s] if s in replacement_dct else s

    def replace_label(label):
        if label == "" or ' / ' not in label:
            return label
        
        i, o = label.strip('\' "').split(' / ')
        ri = replace_symbol(i)
        ro = replace_symbol(o)
        return f"{ri} / {ro}"

    initial_edge = None

    # read graph, implies that graph is connected and there is a single graph
    graph = pydot.graph_from_dot_file(graph_name)[0]

    # shorten node names, if applicable and remove any falsely read nodes, most common '\\n\\n'
    for node in graph.get_nodes():
        name = node.get_name()
        if name.startswith('s'):
            node.set_label('"' + node.get_label().strip('\' "').lstrip('s') + '"')

        elif name != initial_hidden_node_name:
            if graph.del_node(node):
                print(f"Ignored redundant parsed node with name: {name}")

    # read edges and prepare info dict
    new_edge_info_dct = {}
    for e in graph.get_edges():
        source_dest_pair = e.get_source(), e.get_destination()

        # initial edge is stored separately from info dict
        if source_dest_pair[0] == initial_hidden_node_name:
            initial_edge = e
            if initial_edge_label is not None and initial_edge_label != '':
                initial_edge.set_label(replace_label(initial_edge_label))
            continue

        # in case of label to be removed, delete it from graph and continue
        if should_remove_label(e.get_label()):
            idx = find_index_of_same_edges(e)
            if idx > -1 and graph.del_edge(*source_dest_pair, idx):
                s, d = source_dest_pair
                print(f"Removed edge: {s} -> {d} with label: {e.get_label()}")
            continue

        # create or retrieve sub-dict of source_dest pair
        if source_dest_pair not in new_edge_info_dct:
            new_edge_info_dct[source_dest_pair] = {}

        # replace the old label
        in_label, out_label = replace_label(e.get_label()).split(' / ')

        # append the input to the inputs list of the output key in source_dest sub-dict
        if out_label not in new_edge_info_dct[source_dest_pair]:
            new_edge_info_dct[source_dest_pair][out_label] = []

        new_edge_info_dct[source_dest_pair][out_label].append(in_label)

    # create new label_symbol_dct with replaced symbols
    new_label_symbol_dct = {
        'label_symbol': label_symbol_dct['label_symbol'],
        'i': set(map(replace_symbol, label_symbol_dct['i'])),
        'o': set(map(replace_symbol, label_symbol_dct['o'])),
        'l': set(map(replace_symbol, label_symbol_dct['l'])),
    }

    return graph.get_nodes(), new_edge_info_dct, initial_edge, new_label_symbol_dct


def create_new_graph(nodes, edge_info_dct, initial_edge, label_info_dct, label_symbol_dct):
    """
    nodes: node object list that should contain the initial_hidden_node

    edge_info_dct = {
        (source, dest): {
            r_o1: [r_i1, r_i2, ...],
            r_o2: [r_i1, r_i2, ...],
            ...
        },
        ...
    }

    initial_edge: the initial edge not contained in edge_info_dct

    label_info_dct = {
        'same_edges_op': ('stack' | 'merge'),
        'stack_sep': str,
        'merge_input_sep': str,
        'merge_label_sep': str,
        'start_padding': str,
        'end_padding': str,
        'html_like_labels': boolean
    }

    label_symbol_dct:  {
        'label_symbol': string
        'i': {r_i1, r_i2, ...},
        'o': {r_o1, r_o2, ...},
        'l': {r_i1 / r_o1, r_i2 / r_o2, ...}
    }
    """
    def sort_key_of_label(label):
        # ascending sort based on the label's input
        # first by length and then alphabetically
        input = label.split(' / ')[0]
        return (len(input), input)

    def create_label(i, o):
        label = f"{i} / {o}"
        if (i in label_symbol_dct['i']) or (o in label_symbol_dct['o']) or (label in label_symbol_dct['l']):
            label_symbol = label_symbol_dct['label_symbol']
            label = f"{i} {label_symbol} {o}"
        return label

    def stack_op(source_dest_pair):
        labels = []
        # gather all labels to be stacked
        for output, inputs in edge_info_dct[source_dest_pair].items():
            labels.extend([create_label(i, output) for i in inputs])

        # sort and then join labels
        labels = sorted(labels, key=sort_key_of_label)
        return label_info_dct['stack_sep'].join(labels)

    def merge_op(source_dest_pair):
        labels = []
        # gather all labels to be stacked
        for output, inputs in edge_info_dct[source_dest_pair].items():
            merged_inputs = label_info_dct['merge_input_sep'].join(inputs)
            labels.append(create_label(merged_inputs, output))

        # sort and then join labels
        labels = sorted(labels, key=sort_key_of_label)
        return label_info_dct['merge_label_sep'].join(labels)

    def pad_label(label):
        return label_info_dct['start_padding'] + label + label_info_dct['end_padding']

    def html_like_wrap_label(label):
        return f"<{label}>" if label_info_dct['html_like_labels'] else label

    def finalize_label(label):
        return html_like_wrap_label(pad_label(label))

    graph = pydot.Dot(graph_name='g', graph_type='digraph')

    # add nodes (first hidden node should be included)
    for node in nodes:
        graph.add_node(node)

    # add initial edge
    initial_edge_label = initial_edge.get_label()
    if initial_edge_label:
        new_initial_edge_label = finalize_label(initial_edge_label)
        initial_edge.set_label(new_initial_edge_label)
    graph.add_edge(initial_edge)

    # add other edges after stacking or merging the labels of similar ones
    for source_dest_pair in edge_info_dct:
        if label_info_dct['same_edges_op'] == 'stack':
            new_label = stack_op(source_dest_pair)
        elif label_info_dct['same_edges_op'] == 'merge':
            new_label = merge_op(source_dest_pair)
        else:
            raise Exception("Unsupported same_edges_op in label_info_dct: " + label_info_dct['same_edges_op'])

        # finalize label and create and add the new edge
        new_label = finalize_label(new_label)
        graph.add_edge(pydot.Edge(source_dest_pair[0], source_dest_pair[1], label=new_label))

    return graph


def format_and_write_dot_string(graph_raw_string, initial_hidden_node_name, filename):
    prefix = '\t'
    initial_hidden_node_lines = ""

    with open(filename, 'w') as f:
        for line in graph_raw_string.strip().splitlines():
            line = line.strip()
            if line == 'digraph g {':
                # start of graph
                f.write(line + '\n\n')
            elif line.startswith(initial_hidden_node_name):
                # store them to move them to the bottom
                initial_hidden_node_lines += (line + '\n')
            elif line == '}':
                # end of graph
                f.write('\n' + initial_hidden_node_lines + '\n' + '}' + '\n')
            elif line != '':
                # other non-empty line
                f.write(prefix + line + '\n')

def beautify_ra(args):
    replacement_dict = read_replacement_file(args.r, args.replacement_sep)

    input_file = args.i
    output_file = args.i if args.overwrite_dot_input else args.i.replace('.dot', 'btf.dot')
    
    with open(input_file, 'r') as dot_file:
        lines = dot_file.readlines()
    
    for old, new in replacement_dict.items():
        for index, line in enumerate(lines):
            lines[index] = line.replace(old, new)

    with open(output_file, 'w') as dot_file:
        dot_file.writelines(lines)

    graph, *_ = pydot.graph_from_dot_file(output_file)

    prefix = args.o.replace('.dot', '') if args.o else (args.i.replace('.dot', '') + 'btf')
    other_format_pairs = [ (prefix + '.' + fmt, fmt) for fmt in args.other_formats ]
    for graph_name, format in other_format_pairs:
        graph.write(graph_name, format=format)
        print(f"Written {graph_name} with {format} formatting.")
    


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Beautify a .dot file by merging similar edges and replacing labels',
        formatter_class=argparse.ArgumentDefaultsHelpFormatter)

    parser.add_argument('i', metavar='Input', help='Input .dot file')
    parser.add_argument('-r', help='Replacements file with non-empty lines of the default format: old -> new')
    parser.add_argument('-o', help='Output .dot file')

    parser.add_argument('--disable-shorten-nodes', default=False, action='store_true', help='Disable node name conversion: "sN" -> "N"')
    parser.add_argument('--start-node-name', default='__start0', help='Name of the hidden starting node')
    parser.add_argument('--start-edge-label', default='', help='Label of the starting visible edge')
    parser.add_argument('--replacement-sep', default=' -> ', help='Separator of names in replacements file')

    parser.add_argument('--same-edges-op', default='stack', help='Operation to be performed on same edges, available: stack, merge')
    parser.add_argument('--stack-sep', default='\l ', help='Separator of stacked input/output pairs of same edges')
    parser.add_argument('--merge-input-sep', default=' | ', help='Separator of merged inputs with common output')
    parser.add_argument('--merge-label-sep', default='\l ', help='Separator of merged labels of same edges')

    parser.add_argument('--start-padding', type=int, default=1, help='Left padding of labels (in spaces)')
    parser.add_argument('--end-padding', type=int, default=5, help='Right padding of labels (in spaces)')

    parser.add_argument('--remove-edge-pattern', help="Remove the edges that have labels of the provided pattern. "
                        "Format: i_<input> (match input), o_<output> (match output), l_<input / output> (match label). "
                        "So the general format is: (i|o|l)_<name>[,(i|o|l)_<name>]*")

    parser.add_argument('--set-label-symbol-pattern', nargs=2, metavar=('NEW_LABEL_SYMBOL', 'PATTERN'),
                        help="Change the label symbol (which is '/' by default) of the edges that have labels of the provided pattern. "
                        "Pattern Format: i_<input> (match input), o_<output> (match output), l_<input / output> (match label). "
                        "So the general format is: (i|o|l)_<name>[,(i|o|l)_<name>]*")

    parser.add_argument('--html-like-labels', default=False, action='store_true', help="Enable html-like syntax in label names. "
                        "It defaults --stack-sep and --merge-label-sep to ' <br align=\"left\"/> '. "
                        "It defaults --end-padding to 1.")

    parser.add_argument('--overwrite-dot-input', default=False, action='store_true', help="Overwrite input dot file "
                        "with the output dot file in case they have the same name")
    parser.add_argument('--no-dot-output', default=False, action='store_true', help='Do not output the resulting .dot file')
    parser.add_argument('--other-formats', nargs='*', default=['pdf'], help='Additional output formats other than .dot')
    parser.add_argument('--register-automata', default=False, action='store_true', help="Beautify a Register Automata instead of a Mealy Machine.")
    parser.set_defaults(register_automata=False)

    args = parser.parse_args()

    if args.register_automata:
        beautify_ra(args)
    else:
        def set_if_default(var, new, default):
            return new if var == default else var

        # check same edges op
        if args.same_edges_op not in ['stack', 'merge']:
            print(f"Invalid --same-edge-op value '{args.same_edges_op}'. Available: stack, merge.")
            exit(1)

        if args.html_like_labels:
            args.stack_sep = set_if_default(args.stack_sep, ' <br align="left"/> ', '\l ')
            args.merge_label_sep = set_if_default(args.merge_label_sep, ' <br align="left"/> ', '\l ')
            args.end_padding = set_if_default(args.end_padding, 1, 5)

        # print some visual separators in command line
        cmd_line_sep = 100 * '='
        print(cmd_line_sep)

        remove_dct = create_pattern_dct(args.remove_edge_pattern)
        original_label_symbol_dct = create_pattern_dct_with_label_symbol(args.set_label_symbol_pattern)
        replacement_dct = read_replacement_file(args.r, args.replacement_sep)

        nodes, edge_info_dct, initial_edge, label_symbol_dct = get_info_from_graph(
            args.i, not args.disable_shorten_nodes, remove_dct, original_label_symbol_dct,
            replacement_dct, args.start_node_name, args.start_edge_label)

        label_info_dct = {
            'same_edges_op': args.same_edges_op,
            'stack_sep': args.stack_sep,
            'merge_input_sep': args.merge_input_sep,
            'merge_label_sep': args.merge_label_sep,
            'start_padding': args.start_padding * " ",
            'end_padding': args.end_padding * " ",
            'html_like_labels': args.html_like_labels
        }

        new_graph = create_new_graph(nodes, edge_info_dct, initial_edge, label_info_dct, label_symbol_dct)

        prefix = args.o.replace('.dot', '') if args.o else (args.i.replace('.dot', '') + 'btf')
        new_graph_dot_name = prefix + '.dot'
        other_format_pairs = [ (prefix + '.' + fmt, fmt) for fmt in args.other_formats ]

        print(cmd_line_sep)

        if not args.no_dot_output:
            if args.i == new_graph_dot_name:
                print("Output dot file name coincides with the input dot file name")

                if not args.overwrite_dot_input:
                    new_graph_dot_name = new_graph_dot_name.replace('.dot', '') + 'btf.dot'
                    print(f"The new output dot file name will be {new_graph_dot_name}")
                    print("Add --overwrite-dot-input option to overwrite the input dot file")
                else:
                    print("Option --overwrite-dot-input is applicable")
                    print("Input dot file will be overwritten by output dot file")

                print(cmd_line_sep)

            # alternative without formatting: new_graph.write(new_graph_dot_name, format='raw')
            format_and_write_dot_string(new_graph.to_string(), args.start_node_name, new_graph_dot_name)
            print(f"Written {new_graph_dot_name}")

        for graph_name, fmt in other_format_pairs:
            new_graph.write(graph_name, format=fmt)
            print(f"Written {graph_name}")

        print(cmd_line_sep)
