import subprocess
import xml.etree.ElementTree as et
import html
import os
import shutil
import progressbar
import json

file = 'smallPosts.xml'
lastid = int(str(subprocess.check_output('tail -n 2 %s | grep -o " Id=\\"\\([0-9]*\\)\\""' % file, shell=True))[7:-4])

shutil.rmtree('posts', ignore_errors=True)
os.mkdir('posts')

f = open("errors.txt", "w+")
f.close()

context = et.iterparse(file, events=("end",))
context = iter(context)

ids = []
with progressbar.ProgressBar(max_value=lastid) as bar:
    for event, elem in context:
        try:
            if elem.tag == 'posts':
                continue

            attr = elem.attrib
            body = html.escape(attr['Body'].replace('\n', ''))
            id = attr['Id']
            if attr['PostTypeId'] == '1':
                title = attr['Title']
                tags = attr['Tags'].replace('><', ',').replace('<', '').replace('>', '')
                ids.append(id)

                f = open("posts/question" + id + ".xml", "w+")
                f.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n")
                f.write("<qroot>\n")
                f.write("<question>\n")
                f.write("<Title>" + title + "</Title>\n")
                f.write("<Body>" + body + "</Body>\n")
                f.write("<Tags>" + tags + "</Tags>\n")
                f.write("</question>\n")
                f.close()

            elif attr['PostTypeId'] == '2':
                parentid = attr['ParentId']

                f = open("posts/question" + parentid + ".xml", "a+")
                f.write("<answer>\n")
                f.write("<Body>" + body + "</Body>\n")
                f.write("</answer>\n")
                f.close()

            else:
                f = open("errors.txt", "a+")
                f.write("UNKNOWN PostTypeId, id: " + id + "\n")
                f.close()

            elem.clear()
            bar.update(int(id))

        except:
            f = open("errors.txt", "a+")
            f.write("UNKNOWN ERROR " + elem.tag + " " + json.dumps(elem.attrib) + "\n")
            f.close()

for i in ids:
    f = open("posts/question" + i + ".xml", "a+")
    f.write("</qroot>\n")
    f.close()
