import json
from plot import *

queries = {}
queries_invert = {}
id = 0
with open('../data/queries.txt') as q:
    lines = (line.rstrip('\r\n') for line in q)
    for line in lines:
        queries[id] = line
        queries_invert[line] = id
        id += 1

with open('../data/relevant.json', 'r') as f:
    relevant = json.load(f)

with open('../data/results.json', 'r') as f:
    results = json.load(f)

linenr = 0
question_ids = {}
with open('../data/questionids.txt') as q:
    lines = (line.rstrip('\r\n') for line in q)
    for line in lines:
        question_ids[line] = linenr
        linenr += 1

pavg, ravg, favg = None, None, None
for i in range(id):
    query = queries[i]
    first = True
    if len(relevant[query]) > 2:
        docscore = {}
        for key in results:
            if str(i) in results[key].keys():
                docscore[key] = (results[key][str(i)], 1 if key in relevant[query] else 0)
        print(docscore)

        p, r, f = Plot.pr_roc_info(docscore)
        if first:
            pavg = p
            ravg = r
            favg = f

        else:
            for j in range(len(p)):
                pavg[j] += p[j]

            for j in range(len(r)):
                ravg[j] += r[j]

            for j in range(len(f)):
                favg[j] += f[j]
    first = False

pavg /= id
ravg /= id
favg /= id

print('precision ', pavg)
print('recall ', ravg)
print('fallout ', favg)
fig2, (ax1, ax2) = plt.subplots(1, 2)
Plot.pr_plot(ax1, p, r)
Plot.roc_plot(ax2, r, f)
plt.subplots_adjust(wspace=0.3)
plt.show()

pass
