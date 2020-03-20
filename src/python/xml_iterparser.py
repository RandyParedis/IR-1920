import subprocess
import xml.etree.ElementTree as et
import html
import sys
import json
import os
import shutil
import progressbar
import progressbar.widgets as widgets


def preprocess(file, directory):
    f = open("errors.log", "w+")
    f.close()

    lastid = int(subprocess.check_output('wc -l %s | grep -o " [0-9]*"' % file, shell=True))
    lastid -= 3

    ids = []

    def error(msg):
        with open("errors.txt", "a+") as f:
            f.write(msg + "\n")

    with open(file, 'r') as xml:
        bar = progressbar.ProgressBar(maxval=lastid, widgets=[
            ' ', widgets.Percentage(), ' (', widgets.Counter(), ' of %i) ' % lastid,
            widgets.AdaptiveETA(), ' ', widgets.Bar()
        ])
        bar.start()
        xml.readline()  # Read the com.stackoverflow.helper.XML Info
        xml.readline()  # Read the root opening tag
        for linenr in range(lastid):
            line = xml.readline()
            try:
                elem = et.fromstring(line)

                attr = elem.attrib
                body = html.escape(attr['Body'].replace('\n', ''))
                id = attr['Id']
                if attr['PostTypeId'] == '1':
                    title = html.escape(attr['Title'])
                    tags = attr['Tags'].replace('><', ',').replace('<', '').replace('>', '')
                    ids.append(id)

                    with open(directory + "/question" + id + ".xml", "w+") as f:
                        f.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
                        f.write("<qroot>\n")
                        f.write("<question>\n")
                        f.write("<Title>" + title + "</Title>\n")
                        f.write("<Body>" + body + "</Body>\n")
                        f.write("<Tags>" + tags + "</Tags>\n")
                        f.write("</question>\n")
                elif attr['PostTypeId'] == '2':
                    parentid = attr['ParentId']

                    with open(directory + "/question" + parentid + ".xml", "a+") as f:
                        f.write("<answer>\n")
                        f.write("<Body>" + body + "</Body>\n")
                        f.write("</answer>\n")
                else:
                    error("UNKNOWN PostTypeId, id: " + id)

                bar.update(linenr)
            except Exception as e:
                error("[ERROR]: %s:%i\t%s\n\t%s %s" % (file, linenr + 2, "%s: %s" % (type(e), str(e)), elem.tag,
                                                       json.dumps(elem.attrib)))
        xml.readline()  # Read the root closing tag

        for i in ids:
            f = open(directory + "/question" + i + ".xml", "a+")
            f.write("</qroot>\n")
            f.close()

        bar.finish()


if __name__ == '__main__':
    args = sys.argv
    fname = "smallPosts.xml"
    if len(args) > 1 and os.path.isfile(args[1]):
        fname = args[1]

    dir = 'posts'
    shutil.rmtree(dir, ignore_errors=True)
    if len(args) > 2:
        dir = args[2]
    if not os.path.isdir(dir):
        os.mkdir(dir)

    preprocess(fname, dir)
