import argparse
import os
import re
import string
from multiprocessing import Pool
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
    line = remove_stopwords(line, quad_stop_words)
    try:
        line = line.decode('unicode_escape').encode('ascii', 'ignore')
    except:
        # Do Nothing
        print "Failed ascii coding {}".format(line)

    elements = [evaluate_token(x) for x in re.split('<|>', line) if x.strip() not in [""]]

    tokens = []
    for element in elements:
        quoted_text = re.findall("\".*\"", element, re.MULTILINE)
        if len(quoted_text):
            for quotedText in quoted_text:
                if re.findall("window|document|script|eval", quotedText.lower()):
                    return None
                tokens.append("\"{}\"".format(element.translate(string.maketrans("", "", ), quoted_string_stop_words)
                                              .strip()))
        else:
            tokens.append(element)
    if len(tokens) is 5:
        return ' '.join(tokens) + '\n'
    return None


def remove_stopwords(element, stop_words):
    return element.translate(string.maketrans("", "", ), stop_words).strip()


def handle_file(artifact):
    print("Running mapping {} to output {} ".format(artifact[0], artifact[1]))
    output = open(artifact[1], 'w')
    with open(artifact[0], "r") as input_file:
        for line in input_file:
            try:
                processed_line = process_line(line)
                if processed_line is not None:
                    output.write(processed_line)
            except:
                pass


if __name__ == "__main__":
    quad_files = ["{}{}".format("../data/", quad_file) for quad_file in os.listdir('../data') if
                  (quad_file.startswith("recipe.part"))]
    num_files = 2
    processes = Pool(num_files)
    output_files = ["{}{}_{}{}".format("../data/", "output", range, ".nq") for range in range(1, num_files)]
    file_artifacts = [[i, o] for i, o in zip(quad_files, output_files)]
    processes.map(handle_file, file_artifacts)
