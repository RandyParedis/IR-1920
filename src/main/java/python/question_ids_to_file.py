import os

ids = []

dir = '/Users/joostakkermans/Documents/UAntwerpen/M_DataScience/Information Retrieval/data/python_cpp'
for d in os.listdir(dir):
    if d == '.DS_Store':
        continue

    for f in os.listdir(os.path.join(dir, d)):
        id = f[8:-4]
        ids.append(int(id))

ids.sort()
for i in ids:
    with open('../data/questionids.txt', 'a') as out:
        out.write(str(i) + '\n')