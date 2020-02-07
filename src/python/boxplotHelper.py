"""This file contains the main functionality for having more control over boxplots.

These can be used for custom computation of boxplots, histograms and some other features,
based upon certain algorithms. This way, it is possible to compute these values in
situations where you don't have access over the entire dataset, but only over a few values
thereof.
There are also some additional helper functions that can be useful in other circumstances
as well.

Author: Randy Paredis
Date:   11-24-2019
"""

from enum import Enum
import math


class BoxplotAlgorithm(Enum):
    """helper.Helper enum for creating boxplots.

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
    MENDENHALL_SINCICH = 5      # Often closest to pure definition
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
        N = b + j + 2   # (b+1) observations before loop + currently on (j+1) observation in loop
        histostep(xj, b, q, n, N)

    return q, [x/len(series) for x in n]


def histostep(xj, b: int, q: list, n: list, N: int):
    """helper.Helper function for the histogram function.

    This function was extracted to make it easier for the algorithm
    to be used without knowledge of the entire series.

    Args:
        xj (numeric):   The new item in the series.
        b (int):        The amount of cells to compute.
        q (list):       The datavalues of the bars in the histogram.
        n (list):       The positions of the bars in the histogram.
        N (int):        The current observation index.
    """
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
        n_ = i * (N - 1) / b
        di = n_ - n[i]
        # Prevent markers moving to the same position.
        if (di >= 1 and n[i+1] - n[i] > 1) or \
                (di <= -1 and n[i-1] - n[i] < 1):
            di = sign(di)
            qi = PPP(q, n, i, di)
            if q[i-1] < qi < q[i+1]:
                q[i] = qi
            else:
                q[i] += di * (q[i + di] - q[i]) / (n[i + di] - n[i])
            n[i] += di


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


def sign(num):
    """Apply the sign function.

    Args:
        num (numeric):    The value of which the sign needs to be determined.

    Returns:
        1   if num > 0
        -1  if num < 0
        0   if num == 0
    """
    return 1 if num > 0 else -1 if num < 0 else 0


def binomial(n, k):
    """The binomial computation.

    The amount of possible ways to choose k out of n.
    This is the computation of the binomial coefficient C(n, k).
    Instead of the basic formula of
        n! / (k! * (n - k)!)    where 'x!' stands for 'x factorial',
    this method will try and compute a space-efficient binomial
    (i.e. O(1)). It is based on the explanation on:
    https://www.geeksforgeeks.org/space-and-time-efficient-binomial-coefficient/

    Args:
        n (numeric):    The total number (top number).
        k (numeric):    The amount to choose (bottom number).

    Returns:

    """
    # We know C(n, k) = C(n, n - k) and therefore we can simplify
    #   Basically, we choose the least amount of operations needed.
    if k > n - k:
        k = n - k
    res = 1.0
    for i in range(k):
        res *= float(n - i)
        res /= float(i + 1)
    return res


def binprob(t, n, p):
    """Compute the probability w.r.t. a binomial distribution.

    Assume X follows a discrete binomial distribution with size n
    and probability p:
                X ~ Bin(n, p)

    Args:
        t (numeric):    The value to compute.
        n (numeric):    The size of the distribution.
        p (float):      The probability of the distribution.

    Returns:
        The probability that X is less then t:
                Pr[X <= t]
    """
    # print("\tX ~ Bin(%f, %f); Pr[X <= %i]" % (n, p, t))
    return sum([binomial(n, i)*(p**i)*((1-p)**(n-i)) for i in range(math.floor(t))])


def draw_boxplot(minimum, Q1, median, Q3, maximum, outliers=False, labels=None):
    """Get a boxplot-data object in matplotlib, based on its values.

    Using this function, it is possible to quickly draw a boxplot, based on
    its internal values. By default, matplotlib uses either bootstrapping or
    Gaussian-based asymptotic approximation (Tukey).

    This function therefore allows drawing boxplots based on other algorithms,
    but it does NOT draw the boxplots themselves. It yields a mere object, which
    can be used in the Axes.bpx method of matplotlib.

    Args:
        minimum (numeric):  The smallest number to be drawn on the boxplot.
        Q1 (numeric):       The lower quartile edge of the box.
        median (numeric):   The median position indicator in the box.
        Q3 (numeric):       The upper quartile edge of the box.
        maximum (numeric):  The largest number to be drawn on the boxplot.
        outliers (bool):    When true, this will make sure the whiskers are at most
                            1.5 * IQR away from the box. If the `minimum` and the
                            `maximum` are outside of this range, they will be
                            indicated as outliers.
                            Do note that, as was the case with the compute_boxplot
                            function, this method is a mere approximation of the
                            boxplot, which becomes clear when comparing it to the
                            actual boxplot. Theoretically, it's a good approximation
                            nonetheless.
                            Defaults to ``False``.
        labels (iterable):  Labels for each dataset.
                            See also cbook.boxplot_stats.labels

    Returns:
        A dictionary containing the results for each column of data.
        This is basically a transformed dictionary, based on:
            >>> cbook.boxplot_stats([minimum, Q1, median, Q3, maximum], labels=labels)[0]

    Example:
        >>> import matplotlib.pyplot as plt
        >>>
        >>> fig, ax = plt.subplots()
        >>> boxplot = draw_boxplot(minimum, Q1, median, Q3, maximum)
        >>> ax.bxp(boxplot)       # draw the actual boxplot
        >>> plt.show()
    """
    from matplotlib import cbook

    s = cbook.boxplot_stats([minimum, Q1, median, Q3, maximum], labels=labels)[0]
    iqr = Q3 - Q1
    s["q1"] = Q1
    s["median"] = median
    s["q3"] = Q3
    s["iqr"] = iqr
    s["fliers"] = []
    if outliers:
        L = Q1 - (1.5 * iqr)
        H = Q3 + (1.5 * iqr)
        if minimum < L:
            s["fliers"].append(minimum)
        if maximum > H:
            s["fliers"].append(maximum)
        s["whislo"] = max(minimum, L)
        s["whishi"] = min(maximum, H)
    else:
        s["whislo"] = minimum
        s["whishi"] = maximum

    return s


def mean(seq):
    """Computes the mean of a sequence.

    Args:
        seq (iterable): The sequence to compute the mean for.

    Returns:
        The mean of the sequence.
    """
    if len(seq) == 0:
        return 0
    return sum(seq) / len(seq)


def var(seq):
    """Computes the variance of a sequence.

    Args:
        seq (iterable): The sequence to compute the variance for.

    Returns:
        The variance of the sequence.
    """
    mu = mean(seq)
    S = sum([(X - mu)**2 for X in seq])
    return S / len(seq)


def std(seq):
    """Computes the standard derivation of a sequence.

    Args:
        seq (iterable): The sequence to compute the standard
                        derivation for.

    Returns:
        The standard derivation of the sequence.
    """
    return var(seq) ** 0.5


def harmonic(n: int, m=1):
    """Computes the n-th generalized harmonic number.

    Args:
        n (int):        The number to obtain.
        m (numeric):    The power of each denominator.
                        Defaults to 1, which also allows non-generalized numbers.

    Returns:
        The n-th generalized harmonic number.
    """
    return sum([1 / (q ** m) for q in range(1, n+1)])


def harmonicinv(y, m=1):
    """Compute the inverse general harmonic number.

    I.e. GH(n, m) = y <==> IH(y, m) = n

    Args:
        y (numeric):    The general harmonic number.
        m (numeric):    The power to compute.
                        Defaults to 1.

    Returns:
        The inverse general harmonic number.
    """
    s = 0
    while y > 0:
        s += 1
        y -= 1 / (s ** m)
    return s


if __name__ == '__main__':
    import numpy.random as rng
    import pandas as pd
    import progressbar
    import progressbar.widgets as widgets

    algos = [
        BoxplotAlgorithm.MEDIAN,
        BoxplotAlgorithm.TUKEY,
        BoxplotAlgorithm.MINITAB,
        BoxplotAlgorithm.FREUND_PERLES,
        BoxplotAlgorithm.MOORE_MCCABE,
        BoxplotAlgorithm.MENDENHALL_SINCICH,
        BoxplotAlgorithm.FAME,
        BoxplotAlgorithm.P_SQUARE,
    ]
    cols = [a.name for a in algos]

    N = 10000
    Q1_tests = {A.name: [] for A in algos}
    Q3_tests = {A.name: [] for A in algos}
    EC = []
    errors = []

    print("\nRUNNING ALGORITHMIC TESTS")
    print("=========================\n")
    bar = progressbar.ProgressBar(maxval=N*len(algos), widgets=[
        ' ', widgets.Percentage(), ' (', widgets.Counter(), ' of %i) ' % (N*len(algos)),
        widgets.AdaptiveETA(), ' ', widgets.Bar()
    ])
    bar.start()
    try:
        for l in range(N):
            x = rng.uniform(1.0, 100.0, 1000)
            x = [float(n) for n in x]

            frame = pd.DataFrame(columns=cols)

            for r in ["Q1", "Q3", "IQR"]:
                frame.loc[r] = 0

            for A in algos:
                r = compute_boxplot(x, A)
                frame.at["Q1", A.name] = r["Q1"]
                frame.at["Q3", A.name] = r["Q3"]
                frame.at["IQR", A.name] = r["Q3"] - r["Q1"]

                Q1T = binprob(frame.at["Q1", A.name], len(x), 0.25)
                Q3T = binprob(frame.at["Q3", A.name], len(x), 0.75)
                frame.at["Q1 test", A.name] = Q1T
                frame.at["Q3 test", A.name] = Q3T

                Q1_tests[A.name].append(Q1T)
                Q3_tests[A.name].append(Q3T)
                if abs(Q1T) > 5 or abs(Q3T) > 5:
                    EC.append(r)

                bar.update(l*len(algos)+algos.index(A)+1)
        bar.finish()
    except KeyboardInterrupt as k:
        # Flush ^C character
        print('\r  ', end='\r')
        bar.update(bar.currval)


    print("\nSUMMARY:")
    frame = pd.DataFrame(columns=cols)
    frame.loc["Q1 mean"] = 0
    frame.loc["Q1 std"] = 0
    frame.loc["Q3 mean"] = None
    frame.loc["Q3 std"] = None
    for A in algos:
        frame.at["Q1 mean", A.name] = mean(Q1_tests[A.name])
        frame.at["Q1 std", A.name] = std(Q1_tests[A.name])
        frame.at["Q3 mean", A.name] = mean(Q3_tests[A.name])
        frame.at["Q3 std", A.name] = std(Q3_tests[A.name])
    print(frame)
    print()

    if len(EC) > 0:
        print("\n%i EDGE CASES FOUND:" % len(EC))
        for ec in EC:
            print("\t", ec)
        print()

    if len(errors) > 0:
        print("\n%i ERROR(S) OCCURRED\tsee 'errors.log' for more info" % len(errors))
        print()