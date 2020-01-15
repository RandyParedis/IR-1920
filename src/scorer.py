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
import stackexchange
from datetime import datetime
import json

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


def main():
    print("FINDING RELEVANCE FOR %i QUERIES" % len(QUERIES))

    # We add some leeway in the dates below and assume the question id has an AUTO_INCREMENT
    # MIN ID = 21454 ==> Created on 2008-08-21
    fromdate = datetime.timestamp(datetime.strptime("2008-08-01", "%Y-%m-%d"))
    # MAX ID = 57743117 ==> Edited on 2019-09-01
    todate = datetime.timestamp(datetime.strptime("2019-09-10", "%Y-%m-%d"))

    relevant = {}
    so = stackexchange.Site(stackexchange.StackOverflow)
    for query in QUERIES:
        print("QUERY:", query)
        relevant[query] = []
        u = so.search_advanced(q=query, sort='creation', pagesize=100, fromdate=fromdate, todate=todate)
        g = u
        while g.quota_remaining > 1000:
            items = g.items
            for item in items:
                id = item.id
                if id in DATASET:
                    relevant[query].append(id)
                if len(relevant[query]) >= K:
                    break
            if g.has_more:
                g = g.fetch_next()
            print("RELEVANT", len(relevant[query]))
        break
    with open("../data/relevant.json", "w") as file:
        json.dump(relevant, file)


if __name__ == '__main__':
    main()
