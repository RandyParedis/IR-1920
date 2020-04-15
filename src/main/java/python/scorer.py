"""This file handles the creation of random queries and it scores them.

Using the 'data/queries.txt' file, this file will search all of these
queries on StackOverflow, using the Py-StackExchange module. The top 50
results that the site rates AND also appear in our dataset (as given by
'data/cache.json') are marked as "relevant".

This marking is done via listing their corresponding ids to the cached data
in another file ('data/relevant.json') per generated query. This is therefore
a mapping from query to ids.

This file belongs to the project for the Information Retrieval course made by
    - Johannes Akkermans
    - Randy Paredis
"""
import time
import json
from lxml import html
import requests

K = 50


QUERIES = []
with open("../data/queries.txt") as file:
    QUERIES = file.read().split("\n")

CACHE = {}
with open("../data/cache.json") as file:
    CACHE = json.load(file)

DATASET = set()
for question in CACHE:
    DATASET.add(int(question[len("question"):-len(".xml")]))


_fetch_cache = {}
def fetch(q: str):
    if q not in _fetch_cache:
        xpath = "//div[@class='flush-left js-search-results'][1]/div/div/attribute::id"
        ids = []
        for p in range(1, 11):
            page = requests.get('https://stackoverflow.com/search?page=%i&pagesize=50&'
                                'q=%s' % (p, ("is:question created:2008-08-21..2019-09-10 " + q)
                                          .replace(" ", "+").replace(":", "%3A")))
            tree = html.fromstring(page.content)
            data = tree.xpath(xpath)
            ids += data
            print("\tPAGE", p, "; IDS:", len(data))
            time.sleep(2.5)
            if len(data) < 50:
                break
        _fetch_cache[q] = [int(x.split("-")[-1]) for x in ids]
    return _fetch_cache[q]


def intersection(lst1, lst2):
    return list(set(lst1) & set(lst2))


def main():
    print("FINDING RELEVANCE FOR %i QUERIES" % len(QUERIES))

    relevant = {}
    for query in QUERIES:
        print("QUERY:", query)
        ids = fetch(query)
        relevant[query] = intersection(ids, DATASET)
        print("RELEVANT", len(relevant[query]))
    with open("../data/relevant.json", "w") as file:
        json.dump(relevant, file)


if __name__ == '__main__':
    main()
