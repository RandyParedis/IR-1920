"""This file is meant to be used for plotting data from the corresponding Java-files.

The files need to be parsed first and afterwards they can be compared.
"""
import matplotlib.pyplot as plt
from sklearn.metrics import roc_curve, precision_recall_curve, average_precision_score, auc
from sklearn.utils.fixes import signature
import argparse


def parse(filename):
    with open(filename) as file:
        contents = file.read()
    lines = contents.split("\n")  # Python reads all endings by default as \n
    res = {}
    for line in lines:
        if line.startswith("#") or line == "":
            continue
        x = line.split(":")
        if x[1] != '':
            res[int(x[0])] = x[1]
    return res


def parseManual(filename):
    res = parse(filename)
    for qid in res:
        res[qid] = [int(y) for y in res[qid].split(",")]
    return res


def parseLabels(filename):
    res = parse(filename)
    for qid in res:
        lst = []
        for y in res[qid].split(";"):
            rel, score = y.split(",")
            lst.append((int(rel), float(score)))
        res[qid] = lst
    return res


def ids(filename):
    with open(filename) as file:
        contents = file.read()
    return [int(x) for x in contents.split(",")]


def transform(quids, mlist, alist):
    d = {}
    for quid in quids:
        res = [x for x in alist if x[0] == quid]
        d[quid] = (1 if quid in mlist else 0, res[0][1] if len(res) > 0 else 0)
    return [p[1] for p in sorted(d.items(), key=lambda kv: kv[0])]


def plot_pr_curve(ax, y_true, y_pred):
    precision, recall, _ = precision_recall_curve(y_true, y_pred)
    step_kwargs = ({'step': 'post'} if 'step' in signature(plt.fill_between).parameters else {})
    average_precision = average_precision_score(y_true, y_pred)

    ax.step(recall, precision, color='b', alpha=0.2, where='post')
    ax.fill_between(recall, precision, alpha=0.2, color='b', **step_kwargs)
    ax.set_xlabel('Recall')
    ax.set_ylabel('Precision')
    ax.set_ylim([0.0, 1.05])
    ax.set_xlim([0.0, 1.0])
    ax.set_title('Precision-Recall Curve: AP={0:0.2f}'.format(average_precision))


def plot_roc_curve(ax, y_true, y_score):
    fpr_rf, tpr_rf, _ = roc_curve(y_true, y_score)
    auc_rf = auc(fpr_rf, tpr_rf)

    ax.plot([0, 1], [0, 1], 'k--')
    ax.plot(fpr_rf, tpr_rf, label='RF')
    ax.set_xlabel('False positive rate')
    ax.set_ylabel('True positive rate')
    ax.set_title('ROC Curve (AUC={0:0.2f})'.format(auc_rf))
    ax.legend(loc='best')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument("-m", "--manuallabel", type=str, default="data/manualLabel.txt",
                        help=".txt file with the manual labels for the given queries.")
    parser.add_argument("-a", "--actuallabel", type=str, default="data/labels.txt",
                        help=".txt file with the scored queries of Lucene.")
    parser.add_argument("-i", "--idquestions", type=str, default="data/questions.txt",
                        help=".txt file with the list of all ids of the documents.")
    parser.add_argument("-q", "--queries", type=str, default="data/suggestionsQuery.txt",
                        help=".txt file with all the queries to score.")
    args = parser.parse_args()

    manual = parseManual(args.manuallabel)
    actual = parseLabels(args.actuallabel)
    idlist = ids(args.idquestions)
    queries = parse(args.queries)

    assert len(set(manual.keys()) - set(actual.keys())) > 0

    y_exp = []    # expected
    y_pred = []   # predicted
    y_score = []  # scores
    for qid in actual:
        trs = transform(idlist, manual[qid], actual[qid])
        rel = [x[0] for x in trs]
        score = [x[1] for x in trs]
        y_exp += rel
        y_pred += [1 if s != 0 else 0 for s in score]
        y_score += score

        print(str(qid).rjust(3), "|", queries[qid])
        fig2, (ax1, ax2) = plt.subplots(1, 2)
        plot_pr_curve(ax1, y_exp, y_pred)
        plot_roc_curve(ax2, y_exp, y_score)
        plt.subplots_adjust(wspace=0.3)
        plt.show()
