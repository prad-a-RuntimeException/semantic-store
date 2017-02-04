import argparse
import re
import string
from urllib import quote_plus

'''
The webcommons data needs to cleaned up before most triplestores and semantic libraries will
handle it. Among the problems are malformed http, bad node (name that are are bnodes) and
bad escape characters
'''

parser = argparse.ArgumentParser(description='Provide input nquad file for cleanup')
parser = argparse.ArgumentParser(description='Provide output nquad file')
parser.add_argument('fileName', metavar='N', type=str,
                    help='File name of the input nquad file')
parser.add_argument('output', metavar='N', type=str,
                    help='File name for output')

stop_words = '\\\"'


def evaluate_token(token):
    http_tokens_search = re.search('(https?)[:]{0,2}[//]{0,3}(\w[^\s]*)', token)
    if http_tokens_search and len(http_tokens_search.groups()) == 2:
        http_tokens = http_tokens_search.groups()
        try:
            return '<' + http_tokens[0] + '://' + '/'.join(
                [quote_plus(t.strip()) for t in http_tokens[1].split("/")]) + '>'
        except:
            print ("Failed on  {}".format(token))
            return '<' + quote_plus(http_tokens[1]) + '>'
    elif re.match(r"^[a-zA-Z].*", token):
        return "<" + token + ">"

    return token


def process_line(line):
    line = line.replace('\\n', ' ').replace("\\r", "")
    try:
        line = line.decode('unicode_escape').encode('ascii', 'ignore')
    except:
        # Do Nothing
        print "Failed ascii coding {}".format(line)

    elements = [evaluate_token(x) for x in re.split('<|>', line) if x.strip() not in [".\n", ""]]

    tokens = []
    for element in elements:
        quoted_text = re.findall("\".*\"", element, re.MULTILINE)
        if len(quoted_text):
            for quotedText in quoted_text:
                if re.findall("window|document|script|eval", quotedText.lower()):
                    return None
                element.translate(string.maketrans("", "", ), stop_words)
                tokens.append("\"{}\"".format(element.translate(string.maketrans("", "", ), stop_words).strip()))
        else:
            tokens.append(element)
    if len(tokens) is 5:
        return ' '.join(tokens)
    return None


args = parser.parse_args()
output = open(args.output, 'w')
with open(args.fileName, "r") as input_file:
    for line in input_file:
        try:
            processed_line = process_line(line)
            if processed_line is not None:
                output.write(processed_line)
        except:
            pass
