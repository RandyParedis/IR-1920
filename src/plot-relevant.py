import json
from plot import Plot, plt
import progressbar

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

pavg, ravg, tavg, favg = None, None, None, None
first = True
cnt = 0
with progressbar.ProgressBar(max_value=id) as bar:
    for i in range(id):
        query = queries[i]

        if query in relevant and len(relevant[query]) > 2:
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
            cnt += 1
        bar.update(i)


pavg = [float(x) / cnt for x in pavg]
ravg = [float(x) / cnt for x in ravg]
favg = [float(x) / cnt for x in favg]

p_, r_, t_, f_ = Plot.prf_transform(pavg, ravg, favg)

print("Precision:", p_)
print("Recall:", r_)
print("Recall2:", t_)
print("Fallout:", f_)

fig2, (ax1, ax2) = plt.subplots(1, 2)
Plot.pr_plot(ax1, p_, r_)
Plot.roc_plot(ax2, t_, f_)
plt.subplots_adjust(wspace=0.3)
plt.show()
