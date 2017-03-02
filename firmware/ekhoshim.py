#!/usr/bin/python
#
# See README.md for program description!

import sys
import os
import tempfile
import argparse
import re
import csv
import inspect

from pycparser import parse_file, c_parser, c_generator, c_ast

lst = []

class Record():
    """
    A branch point in a program that will probably be annotated later. Stores 
    line number, file name, and node type (if, elseif, etc.)
    """


    next_id = 0


    def __init__(self, line, file, name, funcName=None, funcList=None):
        self.line = line
        self.file = file
        self.name = name
        self.funcName = funcName
        self.funcList = funcList
        self.id = self.__class__._get_id()

    @classmethod
    def _get_id(cls):
        i = cls.next_id
        cls.next_id += 1
        return i

    @classmethod
    def final_id(cls):
        return cls.next_id - 2 # Account for main_start and send_id functions declared in c programs

    def __str__(self):
        return "{}: {} of {} at line {} in file {}, dependencies are {}".format(self.id, self.funcName, self.name, self.line, self.file, self.funcList)



class DefaultGenerator(c_generator.CGenerator):
    """
    A C code generator that turns an Abstract Syntax Tree (AST) into an
    annotated version of the same file. Extend this class to change the
    contents of the debug lines.
    """


    ELSE_HELPER = "0xDEADFAAB && 0xFEEDBEEF"
    NAME_TABLE = {
                "if": c_ast.If,
                "else": "else",
                "dowhile": c_ast.DoWhile,
                "for": c_ast.For,
                "funcdef": c_ast.FuncDef,
                "while": c_ast.While,
                "case": c_ast.Case,
                "default": c_ast.Default
                }


    def __init__(self, track):
        super(DefaultGenerator, self).__init__()
        self._current_record = None
        self._insert = False
        self._tracked = self._pick_trackers(track)
        self.records = []

    def _pick_trackers(self,track):
        if "all" in track:
            return list(self.NAME_TABLE.values())

        return [ self.NAME_TABLE[x] for x in track if x in self.NAME_TABLE ]


    def preprocess(self, input_lines):
        lines = self._else_to_elseif(input_lines)
        return lines


    def _else_to_elseif(self, input_lines):
        content = "".join(input_lines)
        content = re.sub(r"else(?!\s*if)", r"else if (" + self.ELSE_HELPER + ")", content)
        input_lines = content.splitlines(True)
        return input_lines

    def visit(self, node):
        if type(node) in self._tracked:
            self._current_record = self._record_from_node(node)
            self._insert = True
        method = 'visit_' + node.__class__.__name__
        return getattr(self, method, self.generic_visit)(node)

    def _common_visit(self, node):
        d = None
        if self._insert:
            d = self._build_tracking_line()
            self.records.append(self._current_record)
        s = getattr(super(DefaultGenerator, self), 'visit_' + node.__class__.__name__)(node)
        if d is not None:
            s = self._shim_line(s, d)
        return s


    def _pick_tracking_statement(self, record):
        r = record
        #return "{} at {} in {}".format(r.name, r.line, r.file)
        return "{}".format(r.id)


    def _wrap_tracking_statement(self, statement):
        #return 'mspsim_printf("{}\\n");'.format(statement)
        return 'send_id({});'.format(statement)

    def _build_tracking_line(self):
        statement = self._pick_tracking_statement(self._current_record)
        s =  ' ' * self.indent_level + self._wrap_tracking_statement(statement)
        self.insert = False
        return s


    def _shim_line(self, original, shim, n = 1):
        s = original.split('\n')
        if len(s) < n:
            return original
        s.insert(n, '  ' + shim)
        return '\n'.join(s)


    def visit_Compound(self, n):
        return self._common_visit(n)


    def visit_Case(self, n):
        return self._common_visit(n)


    def visit_Default(self, n):
        return self._common_visit(n)


    def visit_If(self, n):
        s = ""
        if n.cond:
            cond = self.visit(n.cond)
        if n.cond and cond == self.ELSE_HELPER:
            if "else" in self._tracked:
                self._insert = True
                self._current_record.name = "else"
        else:
            s = 'if ('
            if n.cond: s += cond
            s += ')\n'
            if c_ast.If not in self._tracked:
                self._insert = False
        s += self._generate_stmt(n.iftrue, add_indent=True)
        if n.iffalse:
            s += self._make_indent() + 'else\n'
            s += self._generate_stmt(n.iffalse, add_indent=True)
        return s


    def _create_child_tree(self, node):
        global lst
        #print "node is " + str(node.show())
        my_module_classes = tuple(x[1] for x in inspect.getmembers(c_ast, inspect.isclass))
        for (child_name, child) in node.children():
                if isinstance(child, c_ast.FuncCall) and child.name.name != "mspsim_printf":
                    #print "\tChild: " + str(child.name.name)
                    lst.append(child.name.name)
                elif isinstance(child, my_module_classes):
                    self._create_child_tree(child)


    def _record_from_node(self, node):
        global lst
        c = node.coord
        funcName = None
        childlist = []

        match = {
                c_ast.If: "if",
                c_ast.DoWhile: "dowhile",
                c_ast.For: "for",
                c_ast.FuncDef: "funcdef",
                c_ast.While: "while",
                c_ast.Case: "case",
                c_ast.Default: "default",
                }
        name = match[type(node)]

        
        if isinstance(node, c_ast.FuncDef):
            funcName = node.decl.name  
            lst = [] 
            self._create_child_tree(node)
            #if len(lst) > 0:
            #    print "Parent function " + str(funcName)
            #    print lst   

        return Record(c.line, c.file, name, funcName, lst)



class IdGenerator(DefaultGenerator):


    def _pick_tracking_statement(self, record):
        return "{}".format(record.id)



class PrintGenerator(IdGenerator):


    def _wrap_tracking_statement(self, statement):
        return 'coverage("{}\\n");'.format(statement)



class SerialGenerator(IdGenerator):


    def _wrap_tracking_statement(self, statement):
        return 'serial("{}\\n");'.format(statement)



def _extract_directives(input_lines):
    extract = ['#include', '#ifdef', '#ifndef', '#endif']
    code_lines = []
    directive_lines = []
    for line in input_lines:
        if any(word in line.lower() for word in extract):
            directive_lines.append(line.rstrip())
            code_lines.insert(0, '\n')
        else:
            code_lines.append(line)
    return (code_lines, directive_lines)


def generate(input_lines, track=None, generator_class=DefaultGenerator):

    if track is None:
        track = ["all"]
    generator = (generator_class)(track)

    input_lines = generator.preprocess(input_lines)
    code_lines, directive_lines = _extract_directives(input_lines)

    temp_file = tempfile.mkstemp()[1]
    with open(temp_file, 'w') as t:
        t.writelines(code_lines)
    ast = parse_file(temp_file, use_cpp=True,
                     cpp_args=r'-Iutils/fake_libc_include')
    os.remove(temp_file)

    output_lines = []
    output_lines.extend(directive_lines)
    output_lines.append('')
    output_lines.extend(generator.visit(ast).splitlines())

    return (output_lines, generator.records)


def lines_from_file(input_path):
    if input_path == '-':
        input_lines = sys.stdin.readlines()
    else:
        with open(input_path, 'r') as f:
            input_lines = f.readlines()
    return input_lines


def write_to_file(output, output_path):
    if output_path == '-':
        print(output)
    elif output_path == "+":
        sys.stderr.write(output)
    else:
        with open(output_path, 'w') as f:
            f.write(output)


def dump_record_index(records, record_path):
    if record_path == "-":
        csvfile = sys.stdout
    elif record_path == "+":
        csvfile = sys.stderr
    else:
        csvfile = open(record_path, 'w')

    writer = csv.writer(csvfile, quoting=csv.QUOTE_MINIMAL)
    writer.writerow(["ID", "AST Node Type", "Line Number", "File", "Function Name", "Dependencies"])
    for r in records:
        writer.writerow([r.id, r.name, r.line, r.file, r.funcName, r.funcList])

    if record_path not in ("-", "+"):
        csvfile.close()


def generate_from_file(input_path, output_path, record_path, track=None, generator_class=DefaultGenerator):
    input_lines = lines_from_file(input_path)

    output_lines, records = generate(input_lines, track, generator_class)
    output = "\n".join(output_lines)
    write_to_file(output, args.outputfile)
    dump_record_index(records, record_path)


def generate_from_multiple_files(number_of_files, paths, track=None, generator_class=DefaultGenerator):
    output_list = []
    records_list = []
    for i in range(0, number_of_files):
        input_lines = lines_from_file(paths[i * 2])

        output_lines, records = generate(input_lines, track, generator_class)
        output = "\n".join(output_lines)
        
        for record in records:
            records_list.append(record)
        output_list.append(output)    
        
    dump_record_index(records_list, paths[i * 2 + 2])

    for j in range(0, len(output_list)):
        output_list[j] = output_list[j].replace("int main(void)\n{\n", "int main (void)\n{\n\tmain_start(" + str(Record.final_id()) + ");\n")  
        write_to_file(output_list[j], paths[j * 2 + 1])

if __name__ == "__main__":

    parser = argparse.ArgumentParser(description="Ecko Shim: insert debug lines into C code at branching structures (like in loops and ifs). Warning: preprocessor directives EXCEPT for #include, #ifdef, #ifndef, and #endif will cause the program to crash. The other directives will ALL be moved to the top of the file.")
    parser.add_argument("--track", nargs='+', default='all', choices=["all", "if", "else", "dowhile", "for", "funcdef", "while", "case", "default"], metavar="tracking_choice", help="Provide any subset of {all, else, dowhile, for, funcdef, while, case, default}, e.g. '--track if else for'. The default is 'all'.")
    parser.add_argument("-n", type=int, default=1, help="Number of files to be converted:")
    parser.add_argument("files", help="List of <C input file> <C output file>, followed by a single <Record CSV file path>; - for stdin", nargs="+")
    #parser.add_argument("outputfile", help="C output file; - for stdout; + for stderr", nargs='+')
    #parser.add_argument("recordfile", help="Record CSV file path; - for stdout, + for stderr", nargs='+')
    parser.add_argument("--generator", choices=["print", "serial", "default"], default="default", help="Generator to use; print inserts print statements and serial does serial stuff. default for debugging.")
    args = parser.parse_args()

    classes = { "default": DefaultGenerator,
                "print": PrintGenerator,
                "serial": SerialGenerator,
    }
    generator_class = classes[args.generator]

    generate_from_multiple_files(args.n, args.files, args.track, generator_class)
    #generate_from_file(args.n, args.inputfile, args.outputfile, args.recordfile, args.track, generator_class)
    print "Total number of debug statements = "  + str(Record.final_id())
