"""This file is meant to be used for plotting data from the corresponding Java-files.

Internally, this is a wrapper for some fairly common matplotlib plots, but reduces
the need to write a lot of individual lines of code. If you want something that is not
in this file, you can still use matplotlib as default.
"""
import matplotlib.pyplot as plt
from sklearn.metrics import roc_curve, precision_recall_curve, average_precision_score
from sklearn.utils.fixes import signature
from boxplotHelper import compute_boxplot, draw_boxplot
import sys
import os
import json
import re


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

    @staticmethod
    def pr_curve(ax, y_test, y_pred):
        precision, recall, _ = precision_recall_curve(y_test, y_pred)
        step_kwargs = ({'step': 'post'} if 'step' in signature(plt.fill_between).parameters else {})
        average_precision = average_precision_score(y_test, y_pred)

        ax.step(recall, precision, color='b', alpha=0.2, where='post')
        ax.fill_between(recall, precision, alpha=0.2, color='b', **step_kwargs)
        ax.set_xlabel('Recall')
        ax.set_ylabel('Precision')
        ax.set_ylim([0.0, 1.05])
        ax.set_xlim([0.0, 1.0])
        ax.set_title('Precision-Recall curve: AP={0:0.2f}'.format(average_precision))

    @staticmethod
    def roc_curve(ax, y_true, y_score):
        fpr_rf, tpr_rf, _ = roc_curve(y_true, y_score)

        ax.plot([0, 1], [0, 1], 'k--')
        ax.plot(fpr_rf, tpr_rf, label='RF')
        ax.set_xlabel('False positive rate')
        ax.set_ylabel('True positive rate')
        ax.set_title('ROC curve')
        ax.legend(loc='best')


def normalize(vector: list):
    S = sum(vector)
    if S != 0:
        return [e / S for e in vector]
    else:
        return vector


def prediction(idxs, directory=""):
    pred = []
    for path in idxs:
        secs = path.split("/")
        secs[-1] = "question" + secs[-1] + ".xml"
        myfile = os.path.join(os.getcwd(), directory, *secs)
        if os.path.isfile(myfile):
            s = None
            with open(myfile, 'r') as file:
                s = re.search(r'\bpython\b', file.read())
            pred.append(1 if s is not None else 0)
    return pred



if __name__ == '__main__':
    args = sys.argv
    if len(args) == 2:
        data = {
            "scores": {},
            "idxs": [],
            "directory": ""
        }
        name = args[1]
        if os.path.isdir(name):
            raise NotImplementedError("Review Before use!")
            dir = args[1]
            files = os.listdir(name)
            tabs = '    ' * 13
            for i in range(len(files)):
                fname = files[i]
                print("\r\tFILE %i / %i [%.2f %%] {%s}%s" % (i, len(files), i / len(files) * 100, fname, tabs), end='')
                if fname.endswith('.json'):
                    with open(name + fname) as json_file:
                        d = json.load(json_file)
                        for sc in d['scores']:
                            if sc in data["scores"]:
                                raise 4
                            data["scores"][sc] = d['scores'][sc]
                        data["idxs"] += d["idxs"]
                        data["time"] += d["time"]
            print("\r\tFILE %i / %i [100 %%]%s" % (len(files), len(files), tabs))
        elif os.path.isfile(name) and name.endswith('.json'):
            with open(name) as json_file:
                d = json.load(json_file)
                data["scores"] = d["scores"]
                data["directory"] = d["directory"]
                data["idxs"] = d["idxs"]
        else:
            raise ValueError("Invalid argument!")
        S = len(data["idxs"])
        print(S, "records in dataset")
        R1 = range(S-1)
        R2 = range(S)
        L = data["scores"]

        fig2, (ax1, ax2) = plt.subplots(1, 2)
        fig2.suptitle("Benchmark Performance")
        y_score = [1 if str(x) in L and L[str(x)][0] > 0 else 0 for x in R1] + \
                  [1 if str(x) in L and L[str(x)][1] > 0 else 0 for x in R2]
        y_true = ([1] * (S-1)) + ([0] * (S-1)) + [1]
        Plot.pr_curve(ax1, y_true, y_score)
        Plot.roc_curve(ax2, y_true, y_score)
        plt.show()

        # Plot time
        # fig, (ax1, ax2) = plt.subplots(1, 2, sharey=True, gridspec_kw={'wspace': 0, 'width_ratios': [5, 1]})
        # ax1.set_xmargin(0.01)
        # ax2.set_xmargin(0.01)
        # ax1.set_ylabel('seconds')
        # for t in ax1.get_xticklabels():
        #    t.set_rotation(90)
        # fig.suptitle("Lucene Self-Scoring Duration")
        # Plot.bar(ax1, ["Q%s" % x for x in data['idxs']], data['time'])
        # Plot.boxplot(ax2, data['time'])
        # plt.show()
        #
        # fig1, ax = plt.subplots()
        # ax.set_xlabel("Question ID")
        # ax.set_ylabel("Score")
        # Plot.bar(ax, ['' for x in L if x in L[x]], [L[x][x] for x in L if x in L[x]],
        #          "Self-Scoring Performance")
        # plt.show()
        #
        # for i in range(S):
        #     if MP[i] > 0 or MN[i] > 0:
        #         print(MP[i], MN[i])
        #
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
