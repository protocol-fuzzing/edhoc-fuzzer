import argparse
import pydot


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


def get_info_from_graph(graph_name, shorten_node_names, replacement_dct, initial_hidden_node_name, initial_edge_label):
    """
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
    def replace_label(label):
        return replacement_dct[label] if label in replacement_dct else label

    initial_edge = None

    # read graph, implies that graph is connected and there is a single graph
    graph = pydot.graph_from_dot_file(graph_name)[0]

    # shorten node names, if applicable and remove any falsely read nodes, most common '\\n\\n'
    for nd in graph.get_nodes():
        name = nd.get_name()
        if name.startswith('s'):
            if shorten_node_names:
                nd.set_label(nd.get_label().strip('"')[1:])

        elif name != initial_hidden_node_name:
            if graph.del_node(nd):
                print(f"removed redundant node with name: {name}")

    # read edges and prepare info dict
    new_edge_info_dct = {}
    for e in graph.get_edges():
        source_dest_pair = e.get_source(), e.get_destination()

        # initial edge is stored separately from info dict
        if source_dest_pair[0] == initial_hidden_node_name:
            initial_edge = e
            initial_edge.set_label(initial_edge_label)
            continue

        # create or retrieve sub-dict of source_dest pair
        if source_dest_pair not in new_edge_info_dct:
            new_edge_info_dct[source_dest_pair] = {}

        # split the mealy input_output label and replace the names according to replacement_dct
        in_label, out_label = map(replace_label, e.get_label().strip('"').split(" / "))

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
        'input_sep': str,
        'label_sep': str,
        'multiline_labels': bool,
        'start_padding': str,
        'end_padding': str
    }
    """
    graph = pydot.Dot(graph_type='digraph')

    # add nodes (first hidden node should be included)
    for nd in nodes:
        graph.add_node(nd)

    # add initial edge
    initial_edge_label = initial_edge.get_label()
    if initial_edge_label:
        initial_padded_label = label_info_dct['start_padding'] + initial_edge_label + label_info_dct['end_padding']
        initial_edge.set_label(initial_padded_label)
    graph.add_edge(initial_edge)

    # traverse edge_info_dct and for each source -> dest edge merge all labels into one
    #  - for each 'output: [input1, input2, ...]' ---> 'input1, input2, ... / output' as label l0
    #  - concatenate those different labels: l0 | l1 | l2 | ... | lN
    # create and add the new edge with label = '<start_padding>l0 | l1 | l2 | ... | lN<end_padding>'
    for source_dest_pair in edge_info_dct:
        def merge_inputs_label(out_in_list_pair):
            output, inputs = out_in_list_pair
            merged_inputs = label_info_dct['input_sep'].join(inputs)
            return f"{merged_inputs} / {output}"

        # label separator
        label_sep = label_info_dct['label_sep'] + ('\n' if label_info_dct['multiline_labels'] else '')

        # merge inputs, then sort labels from smaller to larger and join them
        new_label = label_sep.join(sorted(
                map(merge_inputs_label, edge_info_dct[source_dest_pair].items()),
                key=lambda s: len(s)))
        # add , padding
        padded_new_label = label_info_dct['start_padding'] + new_label + label_info_dct['end_padding']
        # create and add the new edge
        graph.add_edge(pydot.Edge(source_dest_pair[0], source_dest_pair[1], label=padded_new_label))

    return graph


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Beautify a .dot file by merging similar edges',
        formatter_class=argparse.ArgumentDefaultsHelpFormatter)

    parser.add_argument('i', metavar='Input', help='Input .dot file')
    parser.add_argument('-r', help='Replacements file with non-empty lines of the default format: old -> new')
    parser.add_argument('-o', help='Output .dot file')
    parser.add_argument('--shorten_node_names', default=True, action=argparse.BooleanOptionalAction,
                        help='Disable node name conversion: "sN" -> "N"')
    parser.add_argument('--start-node-name', default='__start0', help='Name of the hidden starting node')
    parser.add_argument('--start-edge-label', default='', help='Label of the starting visible edge')
    parser.add_argument('--replacement-sep', default=' -> ', help='Separator of names in replacements file')
    parser.add_argument('--input-sep', default=', ', help='Separator of merged inputs with common output')
    parser.add_argument('--label-sep', default=' | ', help='Separator of merged labels of same edges')
    parser.add_argument('--multiline-labels', default=True, action=argparse.BooleanOptionalAction,
                        help='Multiline layout of merged labels')
    parser.add_argument('--start-padding', default=1, help='Left padding of labels (in spaces)')
    parser.add_argument('--end-padding', default=5, help='Right padding of labels (in spaces)')

    args = parser.parse_args()

    replacement_dct = read_replacement_file(args.r, args.replacement_sep)
    nodes, edge_info_dct, initial_edge = get_info_from_graph(args.i, args.shorten_node_names, replacement_dct,
                                                             args.start_node_name, args.start_edge_label)
    label_info_dct = {
        'input_sep': args.input_sep,
        'label_sep': args.label_sep,
        'multiline_labels': args.multiline_labels,
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

    new_graph.write(new_graph_dot_name)
    print(f"{new_graph_dot_name} is written")

    new_graph.write(new_graph_pdf_name, format='pdf')
    print(f"{new_graph_pdf_name} is written")
