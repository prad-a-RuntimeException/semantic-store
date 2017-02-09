import argparse
import re
import string
import sys
from pyformance.registry import MetricsRegistry
from pyformance.reporters import ConsoleReporter
from urllib import quote_plus

'''
The webcommons data needs to cleaned up before most triplestores and semantic libraries will
handle it. Among the problems are malformed http, bad node (name that are are bnodes) and
bad escape characters
'''

parser = argparse.ArgumentParser(description='Provide input nquad file for cleanup')
parser.add_argument('fileName', metavar='N', type=str,
                    help='File name of the input nquad file')
parser.add_argument('output', metavar='N', type=str,
                    help='File name for output')

quad_stop_words = '\n'
quoted_string_stop_words = '\\\"\n'
reg = MetricsRegistry()
console = ConsoleReporter(reg, 10, sys.stdout)
console.start()
meter = reg.meter("QuadCleanerTimer")


def evaluate_token(token):
    http_tokens_search = re.search('(https?)[:]{0,2}[//]{0,3}(\w[^\s]*)', token)
    if http_tokens_search and len(http_tokens_search.groups()) == 2:
        http_tokens = http_tokens_search.groups()
        try:
            return '<' + http_tokens[0].strip() + '://' + '/'.join(
                [quote_plus(t.strip()) for t in http_tokens[1].split("/")]) + '>'
        except:
            print ("Failed on  {}".format(token))
            return '<' + quote_plus(http_tokens[1]) + '>'
    elif re.match(r"^[a-zA-Z].*", token):
        return "<" + token + ">"

    return token


def process_line(line):
    # exit signal
    if line == None:
        return

    line = remove_stopwords(line, quad_stop_words)
    try:
        line = line.decode('unicode_escape').encode('ascii', 'ignore')
    except:
        # Do Nothing
        print "Failed ascii coding {}".format(line)

    elements = [evaluate_token(x) for x in re.split('<|>', line) if x.strip() not in [""]]

    tokens = []
    for element in elements:
        quoted_text = re.findall("\".*\"", element, re.DOTALL)
        if len(quoted_text):
            for quotedText in quoted_text:
                if re.findall("window|document|script|eval", quotedText.lower()):
                    return None
                tokens.append(
                    "\"{}\"".format(element.translate(string.maketrans("", "", ), quoted_string_stop_words)
                                    .strip()))
        else:
            tokens.append(element)

    if len(tokens) is 5:
        meter.mark()
        return ' '.join(tokens) + '\n'
    return None


def remove_stopwords(element, stop_words):
    return element.translate(string.maketrans("", "", ), stop_words).strip()


def submit_process(input_file, output_file_name):
    output_file = open(output_file_name, 'w', buffering=100000)
    with open(input_file) as f:
        for line in (process_line(line) for line in f):
            output_file.write(line)


if __name__ == "__main__":
    submit_process('../data/Schema_Recipe.nq', '../data/output.nq')
