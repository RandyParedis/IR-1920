"""This file handles the creation of random queries and it scores them.

Based on the dictionary in 'data/vocabulary.txt', this file will create 10
random queries that is searched on StackOverflow, using the Py-StackExchange
module. The top 50 questions that the site rates AND also appear in our
dataset (as given by 'data/cache.json') are marked as "relevant".

This marking is done via listing their corresponding ids to the cached data
in another file ('data/relevant.json') per generated query. This is therefore
a mapping from query to ids.

Random queries are generated as follows:
    - A random number between 1 and 5 is selected, this is the amount of words
    - For each word, a random word is taken from the vocabulary and added to our query

This file belongs to the project for the Information Retrieval course made by
    - Johannes Akkermans
    - Randy Paredis
"""
import stackexchange
import requests
import random as rng
import json
rng.seed(0)


N_QUERIES = 10
MAX_QLENGTH = 5
K = 50


def real_word(word: str):
    r = requests.get("https://api.datamuse.com/words?ml=%s&qe=ml" % word)
    if not r.ok:
        r.raise_for_status()
    data = r.json()
    return len(data) > 1


WORDLIST = []
with open("../data/vocabulary.txt") as file:
    _wl = file.read().split("\n")
print("CHECKING REAL WORDS...")
for w in range(len(_wl)):
    print("\tCHECKING (%s / %s): %s\t\t\t" % (w, len(_wl), _wl[w]), end='\r')
    if real_word(_wl[w]):
        WORDLIST.append(w)
with open("../data/vocabulary.txt", 'w') as file:
    file.writelines(WORDLIST)
print("REAL WORDS CHECKED!")

CACHE = {}
with open("../data/cache.json") as file:
    CACHE = json.load(file)

DATASET = set()
for question in CACHE:
    DATASET.add(int(question[len("question"):-len(".xml")]))


def main():
    print("WORDLIST CONSISTS OF %i WORDS" % len(WORDLIST))
    relevant = {}
    # so = stackexchange.Site(stackexchange.StackOverflow)
    for i in range(N_QUERIES):
        wordcnt = rng.randint(1, MAX_QLENGTH)
        while True:
            query = []
            for word in range(wordcnt):
                query.append(rng.choice(WORDLIST))
            query = " ".join(query)
            if query not in relevant:
                break

        print("QUERY:", query)
        relevant[query] = []
        # u = so.search_advanced(q=query, sort='relevance', pagesize=10000)
        # items = u.items
        # for item in items:
        #     id = item.id
        #     if id in DATASET:
        #         relevant[query].append(CACHE["question%i.xml" % id])
        #     if len(relevant[query]) >= K:
        #         break
        print("RELEVANT", len(relevant[query]))
    with open("../data/relevant.json", "w") as file:
        json.dump(relevant, file)


if __name__ == '__main__':
    main()
