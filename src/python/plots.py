"""This file is meant to be used for plotting data from the corresponding Java-files.

The files need to be parsed first and afterwards they can be compared.
"""
import matplotlib.pyplot as plt
from sklearn.metrics import roc_curve, precision_recall_curve, average_precision_score, auc
from sklearn.utils.fixes import signature


def parse(filename):
    with open(filename) as file:
        contents = file.read()
    lines = contents.split("\n")  # Python reads all endings by default as \n
    res = {}
    for line in lines:
        if line.startswith("#") or line == "":
            continue
        x = line.split(":")
        res[int(x[0])] = [int(y) for y in x[1].split(",") if y != '']
    return res


def ids(filename):
    with open(filename) as file:
        contents = file.read()
    return [int(x) for x in contents.split(",")]


def transform(quids, tt):
    d = {q: 1 if q in tt else 0 for q in quids}
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
    ax.set_title('Precision-Recall curve: AP={0:0.2f}'.format(average_precision))


def plot_roc_curve(ax, y_true, y_pred):
    fpr_rf, tpr_rf, _ = roc_curve(y_true, y_pred)
    auc_rf = auc(fpr_rf, tpr_rf)

    ax.plot([0, 1], [0, 1], 'k--')
    ax.plot(fpr_rf, tpr_rf, label='RF')
    ax.set_xlabel('False positive rate')
    ax.set_ylabel('True positive rate')
    ax.set_title('ROC curve (AUC={0:0.2f})'.format(auc_rf))
    ax.legend(loc='best')


if __name__ == '__main__':
    # TODO: make the filenames command arguments
    manual = parse("manualLabel.txt")
    actual = parse("data/labels.txt")
    idlist = ids("data/questions.txt")

    assert manual.keys() == actual.keys()

    y_exp = []
    y_score = []
    for qid in manual:
        y_exp += transform(idlist, manual[qid])
        y_score += transform(idlist, actual[qid])

    fig2, (ax1, ax2) = plt.subplots(1, 2)
    plot_pr_curve(ax1, y_exp, y_score)
    plot_roc_curve(ax2, y_exp, y_score)
    plt.subplots_adjust(wspace=0.3)
    plt.show()
