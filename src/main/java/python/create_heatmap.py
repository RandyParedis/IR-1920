import pandas as pd
import seaborn as sns
import matplotlib.pyplot as plt
import csv
import copy

with open('results/combinations_rocchio.csv', newline='') as f:
    reader = csv.reader(f)
    data = list(reader)

p_at_k, r_at_k, a_at_k = [], [], []
for x in data[1:]:
    ab = [str(round(float(i), 2)) for i in x[:2]]
    p, r, a = copy.deepcopy(ab), copy.deepcopy(ab), copy.deepcopy(ab)
    p.append(x[2])
    r.append(x[3])
    a.append(x[4])
    p_at_k.append(p)
    r_at_k.append(r)
    a_at_k.append(a)

# data = [[0.0, 0.0, 0.2], [0.0, 0.5, 0.3], [0.5, 0.0, 0.6], [0.5, 0.5, 0.9]]
data = a_at_k
df = pd.DataFrame(data, columns=['Alpha', 'Beta', 'Value'], dtype=float)
table = df.pivot('Alpha', 'Beta', 'Value')

ax = sns.heatmap(table, cmap="Greens")
plt.title('Heatmap of alpha-beta values Rocchio resulting Accuracy@k')
plt.show()