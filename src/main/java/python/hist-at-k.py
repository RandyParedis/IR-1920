import numpy as np
import matplotlib.pyplot as plt
import json

with open('data/weighted_scores.json') as f:
  data = json.load(f)

N = len(data["Score"])
labels = []
pr_k = []
rec_k = []

for i in data["Score"]:
    labels.append(i["Classifier"])
    pr_k.append(i["PR"])
    rec_k.append(i["R"])


width = 0.35
ind = np.arange(N)
fig, ax = plt.subplots()
rects1 = ax.bar(ind, pr_k, width, color='b')
rects2 = ax.bar(ind+width, rec_k, width, color='grey')

# add some text for labels, title and axes ticks
ax.set_ylabel('Values')
ax.set_title('Weighted Precision@k and Recall@k values for different analyzers (k=20)')
ax.set_xticks(ind+width)
ax.set_xticklabels( labels )

ax.legend( (rects1[0], rects2[0]), ('Precision@k', 'Recall@k') )

plt.show()
