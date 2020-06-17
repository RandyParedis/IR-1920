"""This file is meant to be used for plotting data from the corresponding Java-files.

The files need to be parsed first and afterwards they can be compared.
"""
import matplotlib.pyplot as plt
from sklearn.metrics import roc_curve, precision_recall_curve, average_precision_score, auc
from sklearn.utils.fixes import signature
import argparse


def parse(filename):
    """Parses a file, which is used by parseManual and parseLabels.

    Args:
        filename (str): The filename to parse.
    """
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
    """Parses a manual labeled file. This file contains the expected results.

    Args:
        filename (str): The filename to read.
    """
    r = parse(filename)
    res = {}
    for qid in r:
        res[qid] = [int(y) for y in r[qid].split(",")]
    return res


def parseLabels(filename):
    """Parses a measured labeling file. This file is outputted by Lucene.

    Args:
        filename (str): The filename to read.
    """
    r = parse(filename)
    res = {}
    for qid in r:
        lst = []
        for y in r[qid].split(";"):
            doc, score = y.split(",")
            lst.append((int(doc), float(score)))
        res[qid] = lst
    return res


def ids(filename):
    """Gets all the question ids."""
    with open(filename) as file:
        contents = file.read()
    return [int(x) for x in contents.split(",")]


def transform(quids, mlist, alist):
    """Transforms the manual labels and measured labels into boolean vectors.
    A 1 indicates that, for the query, the label was expected and measured.

    Args:
        quids (list):   The question ids.
        mlist (list):   The manual label list (obtained from parseLabels).
        alist (list):   The actual label list (obtained from parseManual).
    """
    d = {}
    for quid in quids:
        res = [x for x in alist if x[0] == quid]
        d[quid] = (1 if quid in mlist else 0, res[0][1] if len(res) > 0 else 0)
    return [p[1] for p in sorted(d.items(), key=lambda kv: kv[0])]


def plot_pr_curve(ax, y_true, y_pred):
    """Plots the ROC-curve.

    Args:
        y_true (list):  The expected labels. (Boolean list)
        y_pred (list):  The measured labels. (Boolean list)
    """
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
    """Plots the ROC-curve.

    Args:
        y_true (list):  The expected labels. (Boolean list)
        y_score (list): The measured labels. (Boolean list)
    """
    fpr_rf, tpr_rf, _ = roc_curve(y_true, y_score)

    ax.plot([0, 1], [0, 1], 'k--')
    ax.plot(fpr_rf, tpr_rf, label='RF')
    ax.set_xlabel('False positive rate')
    ax.set_ylabel('True positive rate')
    ax.set_title('ROC Curve')
    ax.legend(loc='best')


def pr_at_k(rels, expected_count, k):
    """Computes the precision and recall @ k.

    Args:
        rels (list):            A boolean list that indicates that all
                                obtained results belong to the expected
                                results, in the order that they were
                                returned.
        expected_count (int):   The expected amount of results.
        k (int):                The maximal level to look for.
    """
    k = min(k, len(rels))
    TP = sum(rels[:k])
    FP = k - TP
    FN = expected_count - TP
    return TP / (TP + FP), TP / (TP + FN)


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
    parser.add_argument("-k", type=int, default=20,
                        help="The maximal value of k when computing Precision@k / Recall@k.")
    args = parser.parse_args()

    manual = parseManual(args.manuallabel)
    actual = parseLabels(args.actuallabel)
    idlist = ids(args.idquestions)
    queries = parse(args.queries)

    assert len(set(manual.keys()) - set(actual.keys())) >= 0

    y_exp = []    # expected
    y_pred = []   # predicted
    y_score = []  # scores
    wpk, wrk = 0.0, 0.0     # weighted precision@k / recall@k
    for qid in actual:
        trs = transform(idlist, manual[qid], actual[qid])
        rel = [x[0] for x in trs]
        score = [x[1] for x in trs]
        y_exp += rel
        y_pred += [1 if s != 0 else 0 for s in score]
        y_score += score

#         Plot Graphs per query
#         fig2, (ax1, ax2) = plt.subplots(1, 2)
#         plot_pr_curve(ax1, rel, [1 if s != 0 else 0 for s in score])
#         plot_roc_curve(ax2, rel, score)
#         plt.subplots_adjust(wspace=0.3)
#         plt.show()

        pk, rk = pr_at_k([int(x[0] in manual[qid]) for x in actual[qid]], len(manual[qid]), args.k)
        print(str(qid).rjust(3), "|", queries[qid].ljust(35), "P@k = %.5f; R@k =%.5f" % (pk, rk))
        wpk += pk
        wrk += rk
    wpk /= len(actual)
    wrk /= len(actual)

    print("Weighted Precision@%i =" % args.k, wpk)
    print("Weighted Recall@%i    =" % args.k, wrk)

    fig2, (ax1, ax2) = plt.subplots(1, 2)
    plot_pr_curve(ax1, y_exp, y_pred)
    plot_roc_curve(ax2, y_exp, y_score)
    plt.subplots_adjust(wspace=0.3)
    plt.show()
