import json
from plot import Plot, plt

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
first = True
for i in range(id):
    query = queries[i]

    if len(relevant[query]) > 2:
        docscore = {}
        for key in results:
            docscore[key] = (results[key].get(str(i), 0.0), 1 if key in relevant[query] else 0)

        p, r, f = Plot.pr_roc_info(docscore)
        if first:
            pavg = p
            ravg = r
            favg = f
            first = False

        else:
            for j in range(len(p)):
                pavg[j] += p[j]
                ravg[j] += r[j]
                favg[j] += f[j]


pavg = [float(x) / id for x in pavg]
ravg = [float(x) / id for x in ravg]
favg = [float(x) / id for x in favg]

print('precision ', pavg)
print('recall ', ravg)
print('fallout ', favg)
fig2, (ax1, ax2) = plt.subplots(1, 2)
Plot.pr_plot(ax1, p, r)
Plot.roc_plot(ax2, r, f)
plt.subplots_adjust(wspace=0.3)
plt.show()

pass
