# Stackurious

# Table of Contents

1. [Introduction] (README.md#introduction)
2. [Details of Implementation] (README.md#details-of-implementation)
3. [Description of Data] (README.md#description-of-data)
4. [Data Conversion](README.md#data-conversion)
5. [Repo directory structure] (README.md#repo-directory-structure)
6. [Testing your directory structure and output format] (README.md#testing-your-directory-structure-and-output-format)
7. [FAQ] (README.md#faq)


# Introduction

Stackurious is a data pipeline to provide trends in StackOverFlow dataset and find the top experts for different areas e.g expert in machine learning, java etc.

Also, it provides real-time assignment of experts to incoming stream of questions. It is built using the following technologies:

- Spark 2.1.0
- Spark Streaming 2.1.0
- Apache Cassandra 3
- Apache HDFS
- Apache Kafka 0.10
- Flask with Tornado, Bootstrap, Ajax and Dimple on D3.js

# Stackurious Features

Stackurious provides the user with trends on Tags based on real-time and historical analysis of Posts and User data.

## Historical Data Analysis

Stackurious has published their dataset of Posts and related info since 2008. This dataset is analyzed to provide the following information:

- Top 10 Tags of All time based on usage count
- Top Tags based on usage count by Month
- Frequently Asked Questions co-related based on Posts tags 
-- Top Answered Questions per Tag
-- Top UnAnswered Questions per Tag
- Experts for a Tag based on User of Accepted Answer for Questions

![Tags by Count] (images/tagbycount.png)
![FAQ] (images/faq_ans1.png)


## Real-time Data Analysis

Posts data is simulated in real-time and analyzed using Spark Streaming to produce the following information:

- Trending Tags
- Attach Top Experts based on Tags associated to the Incoming Posts

![Trending Tag Counts] (images/livetags.png)
![Live Posts] (images/livepostexperts.png)


# Historical Data Details

## Data Format



###Other considerations and optional features

It's critical that these features don't take too long to run.


##Details of implementation
<img src="./images/pipeline" width="600">

[Back to Table of Contents] (README.md#table-of-contents)



###Input




###Output


##Description of Data

[Back to Table of Contents] (README.md#table-of-contents)

StackOverflow data is in xml format. For this project just using Posts.xml, Users.xml and tags.xml

Here are the details of data fields:

- **posts**.xml
       - Id
       - PostTypeId
          - 1: Question
          - 2: Answer
       - ParentID (only present if PostTypeId is 2)
       - AcceptedAnswerId (only present if PostTypeId is 1)
       - CreationDate
       - Score
       - ViewCount
       - Body
       - OwnerUserId
       - LastEditorUserId
       - LastEditorDisplayName="Jeff Atwood"
       - LastEditDate="2009-03-05T22:28:34.823"
       - LastActivityDate="2009-03-11T12:51:01.480"
       - CommunityOwnedDate="2009-03-11T12:51:01.480"
       - ClosedDate="2009-03-11T12:51:01.480"
       - Title=
       - Tags=
       - AnswerCount
       - CommentCount
       - FavoriteCount

   **users**.xml
     - Id
     - Reputation
     - CreationDate
     - DisplayName
     - EmailHash
     - LastAccessDate
     - WebsiteUrl
     - Location
     - Age
     - AboutMe
     - Views
     - UpVotes
     - DownVotes

The few lines of posts.xml looks like this:
<?xml version="1.0" encoding="utf-8"?>
<posts>
  <row Id="1" PostTypeId="1" AcceptedAnswerId="3" CreationDate="2016-08-02T15:39:14.947" Score="5" ViewCount="154" Body="&lt;p&gt;What does &quot;backprop&quot; mean? I've Googled it, but it's showing backpropagation.&lt;/p&gt;&#xA;&#xA;&lt;p&gt;Is the &quot;backprop&quot; term basically the same as &quot;backpropagation&quot; or does it have a different meaning?&lt;/p&gt;&#xA;" OwnerUserId="8" LastEditorUserId="42" LastEditDate="2016-08-18T13:20:02.773" LastActivityDate="2016-08-18T13:20:02.773" Title="What is &quot;backprop&quot;?" Tags="&lt;neural-networks&gt;&lt;definitions&gt;" AnswerCount="3" CommentCount="3" />

  </posts>




##Data Conversion
[Back to Table of Contents] (README.md#table-of-contents)

Spark Ingester reads XML data files from S3 and  parses data using a custom parser and Spark. Data is converted to dataframes in Spark and stored in HDFS in Parquet format.

Data Analyzer class reads the data from hdfs and query this data using spark sql and stores the result in Cassandra.

Streaming posts data is simulated using a python script and ingested to Spark Streaming using Kafka connect.
Live data is filtered to pull out just the questions from the post. Then looking at the tag for incoming questions, experts are assigned using the pre processed data from tag_experts table in Cassandra.


