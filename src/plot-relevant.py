import json
from plot import Plot, plt
import progressbar
import numpy as np
import sys

directory = "../data"
if len(sys.argv) == 2:
    directory = sys.argv[1]

queries = {}
queries_invert = {}
id = 0
with open('../data/queries.txt') as q:
    lines = (line.rstrip('\r\n') for line in q)
    for line in lines:
        queries[id] = line
        queries_invert[line] = id
        id += 1

with open('%s/relevant.json' % directory) as f:
    relevant = json.load(f)

with open('%s/results.json' % directory) as f:
    results = json.load(f)

linenr = 0
question_ids = {}
with open('../data/questionids.txt') as q:
    lines = (line.rstrip('\r\n') for line in q)
    for line in lines:
        question_ids[line] = linenr
        linenr += 1

pavg, ravg, tavg, favg = None, None, None, None
first = True
cnt = 0


def avg(p1: np.ndarray, r1: np.ndarray, p2: np.ndarray, r2: np.ndarray):
    if p1 is None or r1 is None:
        return p2, r2
    if p2 is None or r2 is None:
        return p1, r1
    basis = np.concatenate((r1, r2), axis=None)
    basis = np.sort(basis)
    basis = np.unique(basis)
    image1, image2 = [], []
    i1, i2 = 0, 0
    for i in range(len(basis)):
        x = basis[i]
        if x in r1:
            i1 = np.where(r1 == x)[0][0]
            image1.append(p1[i1])
        else:
            image1.append(p1[i1])
        if x in r2:
            i2 = np.where(r2 == x)[0][0]
            image2.append(p2[i2])
        else:
            image2.append(p2[i2])

    if image1 > image2:
        image = image1
    else:
        image = image2

    return np.asarray(image), np.asarray(basis)


with progressbar.ProgressBar(max_value=id) as bar:
    for i in range(id):
        query = queries[i]

        if query in relevant and len(relevant[query]) > 2:
            docscore = {}
            for key in results:
                docscore[key] = (results[key].get(str(i), 0.0), 1 if key in relevant[query] else 0)

            p, r, f, t, _ = Plot.pr_roc_info2(docscore)
            pavg, ravg = avg(pavg, ravg, p, r)
            tavg, favg = avg(tavg, favg, t, f)
            cnt += 1
        bar.update(i)

fig2, (ax1, ax2) = plt.subplots(1, 2)
Plot.pr_plot(ax1, pavg, ravg)
Plot.roc_plot(ax2, tavg, favg)
plt.subplots_adjust(wspace=0.3)
plt.savefig("../report/images/customAna-%s.png" % directory.split("/")[-1])
plt.show()
