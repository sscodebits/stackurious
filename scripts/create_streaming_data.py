#!/usr/bin/env python

from datetime import datetime
from time import sleep
import sys
import random
import json

#Load list of tags from file
def loadTagList(tagFile):
    myfile = open(tagFile, "r")
    tagList = []
    #process each line
    for line in myfile:
        tagList.append(line.strip())
    myfile.close()
    return tagList

# create list of tags from randomly selected taglist
def createTagList(tagList):
    singleTagList = []
    singleTagList.append(tagList[random.randint(0, len(tagList)-1)])
    return singleTagList

def main():

    if len(sys.argv) != 6:
        print "Usage: ./create_streaming_data.py #outputFile #tagfile #numminutes #numentries #startid "
        sys.exit(1)

    #init params
    n_minutes = int(sys.argv[3])
    n_lines = int(sys.argv[4])
    n_startId = int(sys.argv[5])
    # open post file to output data
    s_outFile = open(sys.argv[1], "a+")
    # load tag file
    tagList = loadTagList(sys.argv[2])
    # pause time of 5s between runs
    n_pauseTime = 1
    #newid,postTypeId=1,creationDate,tags,title,
    #id,postTypeId=2,parentId,creationDate,tags,title,AcceptedAnswer=true/false
    idPrefix="p"
    currentId=n_startId
    acceptedAnswer="0"

    random.seed(1)
    # calc time taken by single run
    singlerun = n_pauseTime 
    n_repeats = int(n_minutes*60/singlerun)

    for repeat in range(0, n_repeats):
      print "Posting Set " + str(repeat+1) + " of " + str(n_repeats)
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
        # get current time in milisec
        creationDate=datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]
        tags=";".join(createTagList(tagList))
        title="What do you mean by tags " + tags + "?"
        # write line to file
        s_outFile.write( ",".join(map(str, [id,postTypeId,parentId,creationDate,tags,title,acceptedAnswer])) + '\n' )
      #pause between Set
      sleep(n_pauseTime)
    s_outFile.close()

if __name__ == "__main__":
  main()
