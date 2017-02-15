# Stackurious

# Table of Contents

1. [Introduction] (README.md#introduction)
2. [Stackurious Features] (README.md#stackurious-features)
3. [Data Pipeline] (README.md#data-pipeline)
  * [Historical Data Analysis Details](README.md#historical-data-analysis-details)
  * [Streaming Data Analysis Details](README.md#streaming-data-analysis-details)
5. [AWS Cluster Details] (README.md#aws-cluster-details)
6. [Visualization] (README.md#visualization)
7. [Running Stackurious] (README.md#running-stackurious)


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
  - Top Answered Questions per Tag  
  - Top UnAnswered Questions per Tag
- Experts for a Tag based on User of Accepted Answer for Questions

![Tags by Count] (images/tagbycount.PNG)
![FAQ] (images/faq.PNG)


## Real-time Data Analysis

Posts data is simulated in real-time and analyzed using Spark Streaming to produce the following information:

- Trending Tags
- Attach Top Experts based on Tags associated to the Incoming Posts

![Trending Tag Counts] (images/live_trending_tags.JPG)
![Live Posts] (images/liveposts.PNG)


# Data Pipeline

![Data pipeline] (images/datapipeline.JPG)

## Historical Data Analysis Details

### Data Format

StackOverflow dataset is in XML format. For this project, Posts.xml, Users.xml and tags.xml are being utilized. Total Posts Data is 50GB.

Here are the details of data fields:

```
Posts.xml
- Id
- PostTypeId (1: Question, 2: Answer ...)
- ParentID (only present if PostTypeId is 2)
- AcceptedAnswerId (only present if PostTypeId is 1)
- CreationDate
- Score
- ViewCount
- Body
- OwnerUserId
- ClosedDate="2009-03-11T12:51:01.480"
- Title
- Tags
- AnswerCount
- CommentCount
- FavoriteCount

 Users.xml
- Id
- Reputation
- CreationDate
- DisplayName
- EmailHash
- UpVotes
- DownVotes
```
Sample of a line of Post 
```
  <row Id="1" PostTypeId="1" AcceptedAnswerId="3" CreationDate="2016-08-02T15:39:14.947" Score="5" ViewCount="154" Body="&lt;p&gt;What does &quot;backprop&quot; mean? I've Googled it, but it's showing backpropagation.&lt;/p&gt;&#xA;&#xA;&lt;p&gt;Is the &quot;backprop&quot; term basically the same as &quot;backpropagation&quot; or does it have a different meaning?&lt;/p&gt;&#xA;" OwnerUserId="8" LastEditorUserId="42" LastEditDate="2016-08-18T13:20:02.773" LastActivityDate="2016-08-18T13:20:02.773" Title="What is &quot;backprop&quot;?" Tags="&lt;neural-networks&gt;&lt;definitions&gt;" AnswerCount="3" CommentCount="3" />
```



### Data Ingestion

The Ingestion is done in Spark which reads XML data files from Amazon's S3 storage and parses data using a custom parser to convert to Java objects. Data is converted to dataframes in Spark and stored in HDFS in Parquet format. This process also does the Data Cleanup to remove the invalid rows.

### Historical Data Analyzer

Data Analysis is done with the help of Spark to read the data (in Parquet format) directly into DataFrame. This data is analyzed using queries written in Spark Sql and the results are stored in Cassandra tables.

- Top 10 Tags of All time based on usage count
  - Uses the Tags data from Tags.xml and persists it to tag_counts table in Cassandra
- Top Tags based on usage count by Month
  - Uses the Posts data, filters for Questions based on PostTypeId, extracts the Tags
  - Extracts Year / Month from CreationDate
  - Groups on Year and Month and finds count of Posts
  - Results are stored in tagcounts_by_month in Cassandra
- Frequently Asked Questions co-related based on Posts tags 
  - Top Answered Questions per Tag
    - Uses the Spark Sql dense_rank() Window Function
    - Finds the top 5 Answered Questions based on view_count for each Tag
    - Answered Questions are partitioned over Tags column and ranked by view_count in descending order
    - The top 5 are choosen for each Tag
    - Results are stored in faq_answered table in Cassandra
  - Top UnAnswered Questions per Tag
    - Similar logic as above except that the Set of Unanwered Questions with no Accepted answers are used 
- Experts for a Tag based on User of Accepted Answer for Questions
  - Questions with Accepted Answer Post Id are joined with Posts to find the UserId who had the Accepted Answer
  - This is then joined with Users to get the Display_Name
  - The data is grouped by Tag in the Questions and only users with >10 accepted answers are considered as Experts for that Tag
  - Results are stored in tag_experts table in Cassandra

### Cassandra Schema

```sql
CREATE TABLE tag_counts (rundate timestamp, id text, name text, count int,
    PRIMARY KEY ((rundate), count, name)) WITH CLUSTERING ORDER BY (count DESC, name DESC);
create table tag_counts_by_month (year int, month int, name text, count int,
    PRIMARY KEY ((year, month), count, name)) WITH CLUSTERING ORDER BY (count DESC, name DESC);
CREATE TABLE faq_answered (id text, tags text, creation_date text, cdate timestamp, title text, view_count int, pop_count int,
   PRIMARY KEY ((tags), view_count, id) ) WITH CLUSTERING ORDER BY (view_count DESC, id ASC);
CREATE TABLE faq_unanswered (id text, tags text, creation_date text, cdate timestamp, title text, view_count int, pop_count int,
    PRIMARY KEY ((tags), view_count, id) ) WITH CLUSTERING ORDER BY (view_count DESC, id ASC);
CREATE TABLE tag_experts (tag text, expert_id text, expert_name text, ans_count int,
    PRIMARY KEY ((tag), ans_count, expert_name)) WITH CLUSTERING ORDER BY (ans_count DESC, expert_name DESC);
```

## Streaming Data Analysis Details

Streaming posts data is simulated using a python script and ingested to Kafka using Kafka connect. 1000 posts/sec are ingested. Using Spark Streaming, Live Posts data is first stored in Cassandra table. It is then filtered to pull out just the questions from the post. Then, looking at the tag for incoming questions, experts are assigned using the pre processed data from tag_experts table in Cassandra. Results are stored in live_posts_experts_by_hour table.

### Streaming Data Analyzer

- Incoming Posts
  - Posts are Streamed from Kafka
  - Persisted to live_posts_by_day table in Cassandra which is partitioned by day
- Trending Tags
  - Tags are extracted from Incoming Posts
  - They are convered to Pairs based on Tags so that reduceByKeyAndWindow can be used 
  - This uses a Window and Sliding interval to evaluate the posts in the last minute
  - ReduceTag and inverseReduceTag functions are provided to improve performance
  - Results are stored in live_tag_counts table in Cassandra
- Attach Top Experts based on Tags associated to the Incoming Posts
  - Experts data is Read from tag_experts and Cached at the beginning of the Streaming process
  - Incoming Questions are joined with Experts DataFrame on Tags
  - This is grouped by Post Id and expert list is created by using collect_set Spark Sql Functions
  - Results are stored in live_posts_experts_by_hour table in Cassandra

### Cassandra Schema

```sql
CREATE TABLE live_posts_by_day (group_day text, id text, creation_date timestamp, post_type_id text, accepted_answer_id text, parent_id text, title text, tags text,
    PRIMARY KEY ((group_day), creation_date, id)) WITH CLUSTERING ORDER BY (creation_date DESC, id DESC);

CREATE TABLE live_tag_counts (rundate timestamp, id text, name text, count int,
    PRIMARY KEY ((rundate), count, name)) WITH CLUSTERING ORDER BY (count DESC, name DESC);
CREATE TABLE live_posts_experts_by_hour (group_hour text, id text, creation_date timestamp, title text, tags text, experts text,
    PRIMARY KEY ((group_hour), creation_date, id)) WITH CLUSTERING ORDER BY (creation_date DESC, id DESC);
```

# AWS Cluster Details

Stackurious runs on 3 clusters on AWS:

- 5 node M4 Large Cluster for Kafka, Spark and Spark Streaming
- 4 node M4 Large Cluster for Cassandra
- 1 node M3 Medium for Flask

# Visualization

- Flask is integrated with Cassandra and data is fetched using API calls
- Dimple on D3.js is used as the Charting solution
  - It fetches data from APIs written in views.py
  
# Running Stackurious

- Start Servers for Kafka, Spark Streaming
- Start Kafka Connect producing Real-time posts to Kafka
- Start Spark Streaming job
   - Setup spark.props for AWS environment
- Start python script to produce posts to file
