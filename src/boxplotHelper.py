from enum import Enum
import math


class BoxplotAlgorithm(Enum):
    """Helper enum for creating boxplots.

    Boxplots are created with 5 values:
        [min, Q1, median, Q3, max]
    Even though Q1 and Q3 are also defined as being medians,
    some software packages use other algorithms for computing
    these values. This enum allows a quick and clean access
    of a set of these other algorithms.
    """
    MEDIAN             = 0      # Pure definition of Q1, Q3
    TUKEY              = 1
    MINITAB            = 2
    FREUND_PERLES      = 3      # Used in Excel
    MOORE_MCCABE       = 4      # Used in TI-83
    MENDENHALL_SINCICH = 5
    FAME               = 6      # Fast Algorithm for Median Estimation

    # P^2 Algorithm for Dynamic Calculation of Quantiles and Histograms without Scoring Observations
    P_SQUARE           = 7


def compute_boxplot(series: list, algorithm: BoxplotAlgorithm = BoxplotAlgorithm.MEDIAN):
    """Compute the boxplot values for a series.

    This function computes the [min, Q1, median, Q3, max] set of a series,
    based upon a certain algorithm. This information can be found on:
    https://smallbusiness.chron.com/convert-data-set-quartile-50676.html

    Note as well that the median in the result is the definition-wise median and not computed
         with some other algorithm.

    Args:
        series (list):  A list of numeric vales from which a boxplot
                        must be computed.

        algorithm (BoxplotAlgorithm):
                        The algorithm to use for computing the data.
                        Even though the Q1 and Q3 values are technically defined as medians,
                        some software actually use other computations for these values.
                        Defaults to 'BoxplotAlgorithm.MEDIAN'.

    Returns:
        A dictionary with keys [min, Q1, median, Q3, max] and their corresponding values.
    """
    s = sorted(series)
    N = len(s)
    M = lambda x: math.ceil(x / 2) if x % 2 == 1 else (x + 1) / 2      # Compute 1-based median index of series
    res = {
        "min": min(s),
        "max": max(s),
        "median": linear_interpolation(M(N) - 1, s)
    }

    if algorithm is BoxplotAlgorithm.MEDIAN:
        Mix = M(N) - 1
        L = len(s[:math.floor(Mix)])
        idx1 = M(L)
        U = len(s[math.ceil(Mix) + 1:])
        idx3 = M(U) + math.ceil(Mix) + 1
    elif algorithm is BoxplotAlgorithm.TUKEY:
        idx1 = (N + (2 if N % 2 == 0 else 3)) / 4
        idx3 = (N * 3 + (2 if N % 2 == 0 else 1)) / 4
    elif algorithm is BoxplotAlgorithm.MINITAB:
        idx1 = (N + 1) / 4
        idx3 = (N * 3 + 3) / 4
    elif algorithm is BoxplotAlgorithm.FREUND_PERLES:
        idx1 = (N + 3) / 4
        idx3 = (N * 3 + 1) / 4
    elif algorithm is BoxplotAlgorithm.MOORE_MCCABE:
        idx1 = (N + (2 if N % 2 == 0 else 1)) / 4
        idx3 = (N * 3 + (2 if N % 2 == 0 else 3)) / 4
    elif algorithm is BoxplotAlgorithm.MENDENHALL_SINCICH:
        idx1 = math.floor((N + 1) / 4)
        idx3 = math.ceil((N * 3 + 3) / 4)
    elif algorithm in [BoxplotAlgorithm.FAME, BoxplotAlgorithm.P_SQUARE]:
        # run it afterwards for efficiency, so set dummy data
        # This data will not be used anyways, but it prevents
        #    an error in future lines.
        idx1 = 1
        idx3 = 1
    else:
        raise ValueError("Unknown Algorithm")

    res["Q1"] = linear_interpolation(idx1 - 1, s)
    res["Q3"] = linear_interpolation(idx3 - 1, s)

    if algorithm is BoxplotAlgorithm.FAME:
        Mix = M(N) - 1
        res["Q1"] = FAME(s[:math.floor(Mix)])
        res["Q3"] = FAME(s[math.ceil(Mix)+1:])
    elif algorithm is BoxplotAlgorithm.P_SQUARE:
        x, _ = histogram(series, 4)
        res["Q1"] = x[1]
        res["Q3"] = x[3]

    return res



def FAME(series: list, b = 0.1):
    """Do the FAME algorithm based upon a series.

    Loops throughout the entire series and tries to estimate the median.
    Please note that this is a mere estimate and not at all the actual
    median!
    The source for this algorithm can be found on:
    http://www.eng.tau.ac.il/~shavitt/courses/LargeG/streaming-median.pdf

    Args:
        series (list):  The series to loop through.
        b (numeric):    Minimal initial step size. Defaults to 0.1.

    Returns:
        The estimated median of the series.
    """
    M = series[0]
    step = max(abs(M) / 2, b)

    for i in series:
        if M > i:
            M -= step
        elif M < i:
            M += step
        if abs(i - M) < step:
            step /= 2

    return M



def histogram(series: list, b: int):
    """Computes a b-cell histogram estimate of the series.

    Loops throughout the series and plots an estimate for
    a b+1 point histogram, using b equiprobable cells.
    When there are less then b observations, the series
    itself becomes the histogram.

    The source for this algorithm is based upon
    https://www.cse.wustl.edu/~jain/papers/ftp/psqr.pdf

    Args:
        series (list):  The series of which to compute the
                        histogram.
        b (int):        The amount of cells to compute.
                        Note that b+1 will identify the amount
                        of 'bars'in the histogram.

    Returns:
        An (x, y) tuple where x contains the x-locations of all
        values on the x-axis of the histogram and y contains all
        heights of all bars. Both x and y are lists of numbers of
        size b+1.
    """

    if len(series) <= b:
        return sorted(series), [x/len(series) for x in range(len(series))]

    q = sorted(series[:b+1])
    o = series[b+1:]
    n = list(range(b+1))

    for j in range(len(o)):
        xj = o[j]
        if xj < q[0]:
            q[0] = xj
            k = 0
        for i in range(b):
            if q[i] <= xj < q[i+1]:
                k = i
                break
        if q[b] < xj:
            q[b] = xj
            k = b-1

        for i in range(k+1, b+1):
            n[i] += 1

        for i in range(1, b):
            # Calculate desired marker position
            N = b + j + 2   # (b+1) observations before loop + currently on (j+1) observation in loop
            n_ = i * (N - 1) / b
            di = n_ - n[i]
            if abs(di) >= 1 and abs(n[i+1] - n[i]) > 1:
                di = sign(di)
                qi = PPP(q, n, i, di)
                if q[i-1] < qi < q[i+1]:
                    q[i] = qi
                else:
                    q[i] += di * (q[i + di] - q[i]) / (n[i + di] - n[i])
                n[i] += di

    return q, [x/len(series) for x in n]



def PPP(q: list, n: list, i: int, d=1):
    """Do a Piecewise-Parabolic Prediction.

    Assume we have a parabola passing through
    (n[i-1], q[i-1]), (n[i], q[i]) and (n[i+1], q[i+1])
    which is of the form
                y = ax^2 + bx + c
    where (x, y) are coordinates in these points.
    The interpolated value (prediction) at location i can
    be determined via solving the equations set above.

    Based upon the PP-algorithm from
    https://www.cse.wustl.edu/~jain/papers/ftp/psqr.pdf

    Args:
        q (list):   The heights of the points.
        n (list):   The location of the points.
        i (int):    The index where we will look.
        d (int):    Either -1 (left) or 1 (right)
    """
    assert abs(d) == 1

    def int_n(j, f=1):
        return n[j] - n[j-1] + d*f

    def int_q(j):
        return (q[j+1]-q[j]) / (n[j+1] - n[j])

    return q[i] + (d / (n[i+1] - n[i-1])) * (int_n(i) * int_q(i) + int_n(i+1, -1) * int_q(i-1))



def linear_interpolation(x, data: list):
    """Compute the linear interpolation.

    Using the data, it will compute the value that should appear at index idx.
    This function will do a linear interpolation between the datapoints
    i-1 and i around x. (Whereas i-1=floor(x) and i=ceil(x).)
    We assume the line through these points follows the equation
                y = mx + q
        <=>     y - y1 = (y2 - y1) / (x2 - x1) * (x - x1)

    Args:
        x (numeric):    The location to do the interpolation at. (the x-value)
        data (list):    The list of y-values at integer values.

    Returns:
        The value that corresponds with the idx in the list.
    """
    if math.floor(x) == x:
        return data[int(x)]

    x0 = math.floor(x)
    y0 = data[x0]
    x1 = math.ceil(x)
    y1 = data[x1]

    return y0 + (x - x0) * (y1 - y0) / (x1 - x0)



def sign(x):
    """Apply the sign function.

    Args:
        x (numeric):    The value of which the sign needs to be determined.

    Returns:
        1   if x > 0
        -1  if x < 0
        0   if x == 0
    """
    return 1 if x > 0 else -1 if x < 0 else 0



if __name__ == '__main__':
    import numpy.random as rng

    x = rng.uniform(0.0, 100.0, 1000000)
    algos = [
        BoxplotAlgorithm.MEDIAN,
        # BoxplotAlgorithm.TUKEY,
        # BoxplotAlgorithm.MINITAB,
        # BoxplotAlgorithm.FREUND_PERLES,
        # BoxplotAlgorithm.MOORE_MCCABE,
        # BoxplotAlgorithm.MENDENHALL_SINCICH,
        # BoxplotAlgorithm.FAME,
        BoxplotAlgorithm.P_SQUARE,
    ]

    import pandas as pd

    cols = [a.name for a in algos]
    frame = pd.DataFrame(columns=cols)

    for r in ["Q1", "Q3", "IQR"]:  # ["min", "Q1", "median", "Q3", "max", "IQR"]:
        frame.loc[r] = 0

    bxdata = {}
    for A in algos:
        r = compute_boxplot(x, A)
        bxdata[A.name] = r
        # frame.at["min", A.name] = r["min"]
        frame.at["Q1", A.name] = r["Q1"]
        # frame.at["median", A.name] = r["median"]
        frame.at["Q3", A.name] = r["Q3"]
        # frame.at["max", A.name] = r["max"]
        frame.at["IQR", A.name] = r["Q3"] - r["Q1"]
    mean = frame.mean(axis=1)
    std = frame.std(axis=1)
    frame["mean"] = mean
    frame["std"] = std

    # TODO: figure out a way to compare the two

    # print("SERIES:", x)
    print(frame)

    # from matplotlib import pyplot as plt, cbook
    #
    # data = {'default': cbook.boxplot_stats(x, labels=['default'])[0]}
    # for A in algos:
    #     r = bxdata[A.name]
    #     s = cbook.boxplot_stats(x, labels=[A.name])[0]
    #     s["q1"] = r["Q1"]
    #     s["q3"] = r["Q3"]
    #     s["iqr"] = r["Q3"] - r["Q1"]
    #     s["whislo"] = r["min"]
    #     s["whishi"] = r["max"]
    #     s["fliers"] = []
    #     data[A.name] = s
    #
    # fig, ax = plt.subplots(1, 1)
    # ax.bxp([data[d] for d in data], positions=range(len(algos) + 1))
    # plt.show()
