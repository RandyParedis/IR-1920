"""This file is meant to be used for plotting data from the corresponding Java-files.

Internally, this is a wrapper for some fairly common matplotlib plots, but reduces
the need to write a lot of individual lines of code. If you want something that is not
in this file, you can still use matplotlib as default.
"""
import matplotlib.pyplot as plt
import numpy as np
import sys
import os
import json


class Plot:
    @staticmethod
    def plot(ax, x, y, title='', **kwargs):
        if len(title) > 0:
            ax.set_title(title)
        ax.plot(range(len(x)), y, **kwargs)

    @staticmethod
    def bar(ax, x, y, title='', **kwargs):
        if len(title) > 0:
            ax.set_title(title)
        ax.bar(range(len(x)), y, **kwargs)
        ax.set_xticks(range(len(x)))
        ax.set_xticklabels(x)

    @staticmethod
    def boxplot(ax, x, title='', **kwargs):
        if len(title) > 0:
            ax.set_title(title)
        ax.boxplot(x, **kwargs)

    @staticmethod
    def confusion_matrix(ax, cm, classes, title, cmap=plt.cm.Blues):
        im = ax.imshow(cm, interpolation='nearest', cmap=cmap)
        ax.figure.colorbar(im, ax=ax)
        ax.set(xticks=range(len(cm)),
               yticks=range(len(cm)),
               xticklabels=classes, yticklabels=classes,
               title=title,
               ylabel='Queried Question',
               xlabel='Score of Query')


def normalize(vector: list):
    S = sum(vector)
    if S != 0:
        return [e / S for e in vector]
    else:
        return vector


if __name__ == '__main__':
    args = sys.argv
    if len(args) == 2 and os.path.isdir(args[1]):
        data = {
            "scores": {},
            "idxs": [],
            "time": []
        }
        dname = args[1]
        files = os.listdir(dname)
        tabs = '    ' * 13
        for i in range(len(files)):
            fname = files[i]
            print("\r\tFILE %i / %i [%.2f %%] {%s}%s" % (i, len(files), i / len(files) * 100, fname, tabs), end='')
            if fname.endswith('.json'):
                with open(dname + fname) as json_file:
                    d = json.load(json_file)
                    for sc in d['scores']:
                        if sc in data["scores"]:
                            raise 4
                        data["scores"][sc] = d['scores'][sc]
                    data["idxs"] += d["idxs"]
                    data["time"] += d["time"]
        print("\r\tFILE %i / %i [100 %%]%s" % (len(files), len(files), tabs))
        print(len(data["idxs"]), "records in dataset")

        # Plot time
        #fig, (ax1, ax2) = plt.subplots(1, 2, sharey=True, gridspec_kw={'wspace': 0, 'width_ratios': [5, 1]})
        #ax1.set_xmargin(0.01)
        #ax2.set_xmargin(0.01)
        #ax1.set_ylabel('seconds')
        #for t in ax1.get_xticklabels():
        #    t.set_rotation(90)
        #fig.suptitle("Lucene Self-Scoring Duration")
        #Plot.bar(ax1, ["Q%s" % x for x in data['idxs']], data['time'])
        #Plot.boxplot(ax2, data['time'])
        #plt.show()

        L = data["scores"]
        fig, ax = plt.subplots()
        ax.set_xlabel("Question ID")
        ax.set_ylabel("Score")
        Plot.bar(ax, ['' for x in L if x in L[x]], [L[x][x] for x in L if x in L[x]],
                 "Self-Scoring Performance")
        plt.show()


        # Plot scores
        # N = len(data['idxs'])
        # matrix = [[0 for r in range(N)] for c in range(N)]
        # for x in data['scores']:
        #     for y in data['scores'][x]:
        #         matrix[int(x)][int(y)] = data['scores'][x][y]
        #
        # # Compute PR-values
        # alpha = 0.5
        # TP = FP = TN = FN = 0
        # for r in range(N):
        #     matrix[r] = normalize(matrix[r])
        #     l = [1 if x > alpha else 0 for x in matrix[r]]
        #     TP += l[r]
        #     TN += sum([(x + 1) % 2 for x in l])
        #     FN += 1 if l[r] == 0 else 0
        #     l[r] = 0
        #     FP += sum(l)
        #
        # print("TP:", TP)
        # print("FP:", FP)
        # print("TN:", TN)
        # print("FN:", FN)
        # print("Precision:", TP / (TP + FP))
        # print("Recall:   ", TP / (TP + FN))
        # print("Accuracy: ", (TP + TN) / (N * N))
        #
        # # Plot CM
        # fig, ax = plt.subplots()
        # Plot.confusion_matrix(ax, matrix, data['idxs'], 'Confusion Matrix')
        # plt.show()
