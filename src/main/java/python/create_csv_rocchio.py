"""Runs all possible combinations of arguments and plots.

Eventually, a CSV file with a generic overview is created.
For all combinations, both the normal version and the
Rocchio version are computed.
"""
import os
import subprocess
import itertools as it
import numpy as np


ARGUMENTS = {
    "--a": [str(x) for x in list(np.linspace(0,1,21))],
    "--b": [str(x) for x in list(np.linspace(0,1,21))]
}


def run(options, file, rocchio=False):
    cmd = ["java", "-jar", "target/searcher.jar", "--rocchio", "true"] + list(options)
    print(" RUNNING", " ".join(cmd))
    subprocess.run(cmd)
    out2 = subprocess.run(["python3", "src/main/java/python/plots.py"], stdout=subprocess.PIPE).stdout.decode("utf-8")
    with open(file, 'a') as f:
        f.write("RESULTS FOR " + " ".join(cmd) + "\n")
        f.write(out2)
        f.write("\n\n")
    avg = out2.split("\n")[-4:]
    return [float(x.split("=")[1].strip()) for x in avg if x != ""]


if __name__ == '__main__':
    file = "results/result_rocchio.txt"
    if os.path.isfile(file):
        os.remove(file)

    csv = "results/combinations_rocchio.csv"
    if os.path.isfile(csv):
        os.remove(csv)

    keys = list(ARGUMENTS.keys())
    with open(csv, 'a') as f:
        f.write(", ".join(keys + ["P@k (Rocchio)", "R@k (Rocchio)", "A@k (Rocchio)"]) + "\n")

    records = []
    for option in list(it.product(*ARGUMENTS.values())):
        opts = zip(keys, option)
        opts = [item for sublist in opts for item in sublist]
        rec = list(option)
        rec += run(opts, file)
        with open(csv, 'a') as f:
            f.write(", ".join([str(x) for x in rec]) + "\n")
        records.append(rec)
