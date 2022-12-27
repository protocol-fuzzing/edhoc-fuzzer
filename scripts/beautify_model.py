import argparse
import pydot


def dict_of_remove_pattern(pattern_string):
    remove_dct = {
        'i': set(),
        'o': set(),
        'l': set()
    }

    if pattern_string is None or pattern_string == "":
        return remove_dct

    for patt in pattern_string.strip('\' "').split(','):
        p = patt.strip()
        if p[0] not in ['i', 'o', 'l'] or p[1] != '_':
            raise Exception("Invalid remove pattern provided: " + p)

        if p[0] == 'l':
            i, o = map(lambda s: s.strip(), p[2:].split('/'))
            remove_dct[p[0]].add(i + " / " + o)
        else:
            remove_dct[p[0]].add(p[2:])

    return remove_dct


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


def get_info_from_graph(graph_name, shorten_node_names, remove_dct, replacement_dct,
                        initial_hidden_node_name, initial_edge_label):
    """
        remove_dct = {
            'i': {i1, i2, ...},
            'o': {o1, o2, ...},
            'l': {i1 / o1, i2 / o2, ...}
        }

        replacement_dct = {
            old_name: new_name,
            ...
        }

        (returned) new_edge_info_dct = {
            (source, dest): {
                output1: [input1, input2, ...],
                output2: [input1, input2, ...],
                ...
            },
            ...
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

    def replace_label(label):
        if label == "" or ' / ' not in label:
            return label

        i, o = label.strip('\' "').split(' / ')
        ri = replacement_dct[i] if i in replacement_dct else i
        ro = replacement_dct[o] if o in replacement_dct else o
        return ri + ' / ' + ro

    initial_edge = None

    # read graph, implies that graph is connected and there is a single graph
    graph = pydot.graph_from_dot_file(graph_name)[0]

    # shorten node names, if applicable and remove any falsely read nodes, most common '\\n\\n'
    for nd in graph.get_nodes():
        name = nd.get_name()
        if name.startswith('s'):
            if shorten_node_names:
                nd.set_label('"' + nd.get_label().strip('\' "').lstrip('s') + '"')

        elif name != initial_hidden_node_name:
            if graph.del_node(nd):
                print(f"ignored redundant parsed node with name: {name}")

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
                print(f"removed edge: {s} -> {d} with label: {e.get_label()}")
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

    return graph.get_nodes(), new_edge_info_dct, initial_edge


def create_new_graph(nodes, edge_info_dct, initial_edge, label_info_dct):
    """
    nodes: node object list that should contain the initial_hidden_node

    edge_info_dct = {
        (source, dest): {
            output1: [input1, input2, ...],
            output2: [input1, input2, ...],
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
        'end_padding': str
    }
    """
    def stack_op(source_dest_pair):
        labels = []
        # gather all labels to be stacked
        for output, inputs in edge_info_dct[source_dest_pair].items():
            labels.extend([f"{i} / {output}" for i in inputs])

        # sort them in ascending length order
        labels = sorted(labels, key=lambda s: len(s))
        return label_info_dct['stack_sep'].join(labels)

    def merge_op(source_dest_pair):
        labels = []
        # gather all labels to be stacked
        for output, inputs in edge_info_dct[source_dest_pair].items():
            merged_inputs = label_info_dct['merge_input_sep'].join(inputs)
            labels.append(f"{merged_inputs} / {output}")

        # sort them in ascending length order
        labels = sorted(labels, key=lambda s: len(s))
        return label_info_dct['merge_label_sep'].join(labels)

    graph = pydot.Dot(graph_name='g', graph_type='digraph')

    # add nodes (first hidden node should be included)
    for nd in nodes:
        graph.add_node(nd)

    # add initial edge
    initial_edge_label = initial_edge.get_label()
    if initial_edge_label:
        initial_padded_label = label_info_dct['start_padding'] + initial_edge_label + label_info_dct['end_padding']
        initial_edge.set_label(initial_padded_label)
    graph.add_edge(initial_edge)

    # add other edges after stacking or merging the labels of similar ones
    for source_dest_pair in edge_info_dct:
        if label_info_dct['same_edges_op'] == 'stack':
            new_label = stack_op(source_dest_pair)
        elif label_info_dct['same_edges_op'] == 'merge':
            new_label = merge_op(source_dest_pair)
        else:
            raise Exception("Unsupported same_edges_op in label_info_dct: " + label_info_dct['same_edges_op'])

        # add padding
        padded_new_label = label_info_dct['start_padding'] + new_label + label_info_dct['end_padding']
        # create and add the new edge
        graph.add_edge(pydot.Edge(source_dest_pair[0], source_dest_pair[1], label=padded_new_label))

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


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Beautify a .dot file by merging similar edges and replacing labels',
        formatter_class=argparse.ArgumentDefaultsHelpFormatter)

    parser.add_argument('i', metavar='Input', help='Input .dot file')
    parser.add_argument('-r', help='Replacements file with non-empty lines of the default format: old -> new')
    parser.add_argument('-o', help='Output .dot file')

    parser.add_argument('--disable-shorten-nodes', default=False, help='Disable node name conversion: "sN" -> "N"')
    parser.add_argument('--start-node-name', default='__start0', help='Name of the hidden starting node')
    parser.add_argument('--start-edge-label', default='', help='Label of the starting visible edge')
    parser.add_argument('--replacement-sep', default=' -> ', help='Separator of names in replacements file')

    parser.add_argument('--same-edges-op', default='stack', help='Operation to be performed on same edges, available: '
                                                                 'stack, merge')
    parser.add_argument('--stack-sep', default='\l ', help='Separator of stacked input/output pairs of same edges')
    parser.add_argument('--merge-input-sep', default=' | ', help='Separator of merged inputs with common output')
    parser.add_argument('--merge-label-sep', default='\l ', help='Separator of merged labels of same edges')

    parser.add_argument('--start-padding', default=1, help='Left padding of labels (in spaces)')
    parser.add_argument('--end-padding', default=5, help='Right padding of labels (in spaces)')

    parser.add_argument('--remove-edge-pattern', help="Remove the edges that have labels of the provided pattern. "
                        "Format: i_<input> (match input), o_<output> (match output), l_<input / output> (match label). "
                        "So the general format is: (i|o|l)_<patt>[,(i|o|l)_<patt>]*")

    args = parser.parse_args()
    # check same edges op
    if args.same_edges_op not in ['stack', 'merge']:
        print(f"Invalid --same-edge-op value '{args.same_edges_op}'. Available: stack, merge.")
        exit(1)

    # print some visual separators in command line
    cmd_line_sep = 100 * '='
    print(cmd_line_sep)

    remove_dct = dict_of_remove_pattern(args.remove_edge_pattern)
    replacement_dct = read_replacement_file(args.r, args.replacement_sep)

    nodes, edge_info_dct, initial_edge = get_info_from_graph(
        args.i, not args.disable_shorten_nodes, remove_dct, replacement_dct,
        args.start_node_name, args.start_edge_label)

    label_info_dct = {
        'same_edges_op': args.same_edges_op,
        'stack_sep': args.stack_sep,
        'merge_input_sep': args.merge_input_sep,
        'merge_label_sep': args.merge_label_sep,
        'start_padding': args.start_padding * " ",
        'end_padding': args.end_padding * " "
    }

    new_graph = create_new_graph(nodes, edge_info_dct, initial_edge, label_info_dct)

    if args.o:
        new_graph_dot_name = args.o
        new_graph_pdf_name = new_graph_dot_name.replace('.dot', '') + '.pdf'
    else:
        prefix = args.i.replace('.dot', '') + 'btf'
        new_graph_dot_name = prefix + '.dot'
        new_graph_pdf_name = prefix + '.pdf'

    print(cmd_line_sep)

    # alternative without formatting: new_graph.write(new_graph_dot_name, format='raw')
    format_and_write_dot_string(new_graph.to_string(), args.start_node_name, new_graph_dot_name)
    print(f"written {new_graph_dot_name}")

    new_graph.write(new_graph_pdf_name, format='pdf')
    print(f"written {new_graph_pdf_name}")

    print(cmd_line_sep)
