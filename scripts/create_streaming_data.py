#!/usr/bin/env python

from datetime import datetime
from time import sleep
import sys
import random
import json

def loadTagList(tagFile):
    myfile = open(tagFile, "r")
    tagList = []
    for line in myfile:
        tagList.append(line.strip())
    myfile.close()
    return tagList

def createTagList(tagList):
    singleTagList = []
    singleTagList.append(tagList[random.randint(0, len(tagList)-1)])
    #singleTagList.append(tagList[random.randint(0, len(tagList)-1)])
    return singleTagList

def main():

    if len(sys.argv) != 5:
        print "Usage: ./create_streaming_data.py #outputFile #numlines #startid #tagfile "
        sys.exit(1)

    n_lines = int(sys.argv[2])
    n_startId = int(sys.argv[3])
    tagList = loadTagList(sys.argv[4])

    s_outFile = open(sys.argv[1], "w")

    #newid,postTypeId=1,creationDate,tags,title,
    #id,postTypeId=2,parentId,creationDate,tags,title,AcceptedAnswer=true/false
    idPrefix="p"
    currentId=n_startId
    #postTypeId="1"
    #parentId=""
    #tags="abc"
    acceptedAnswer="0"

    random.seed(1)

    for line in range(0, n_lines):
        currentId += 1
        id=idPrefix + str(currentId)
        postTypeId=random.randint(1,2)
        if postTypeId == 1:
            parentId=""
            acceptedAnswer=""
        else:
            parentId=idPrefix + str(random.randint(n_startId, currentId-1))
            acceptedAnswer=random.randint(0,1)

        creationDate=datetime.now()
        tags=";".join(createTagList(tagList))
        title="some title appended with tags " + tags

        s_outFile.write( ",".join(map(str, [id,postTypeId,parentId,creationDate,tags,title,acceptedAnswer])) + '\n' )
        sleep(.1)

    s_outFile.close()

if __name__ == "__main__":
  main()
