# Stackurious

# Table of Contents

1. [Introduction] (README.md#introduction)
2. [Details of Implementation] (README.md#details-of-implementation)
3. [Description of Data] (README.md#description-of-data)
4. [Data Conversion](README.md#data-conversion)
5. [Repo directory structure] (README.md#repo-directory-structure)
6. [Testing your directory structure and output format] (README.md#testing-your-directory-structure-and-output-format)
7. [FAQ] (README.md#faq)


##Introduction

Stackurious is a data pipeline to provide trends in StackOverFlow dataset and find the top experts for different areas e.g expert in machine learning, java etc.

Also, provides real-time assignment of experts to incoming stream of questions.

### Queries - Batch
*What queries are you asking on your data?
*Find top 10 tags based on count
*Find FAQ (based on Posts view and favorite counts)
*Top Answered Questions (based on Accepted answers)
*Top UnAnswered Questions
*Find Experts for particular tag (based on UserIds of Accepted answer posts)



### Queries - Streaming

*For Incoming Posts
*Attach top 2 experts to New Questions based on Tag
*Current Trending tags

###Stakurious - batch
Using batch processing found Top 10 tags based on count on historical dataset

<img src="./images/tagbycount" width="600">

Frequently asked questions based on tag chosen. Also, Expert is identified based on tag chosen

<img src="./images/faq_ans6" width="600">

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

##Repo directory structure
[Back to Table of Contents] (README.md#table-of-contents)

Example Repo Structure

	├── README.md
	├── run.sh
	├── src
	│  	└── antifraud.java
	├── paymo_input
	│   └── batch_payment.txt
	|   └── stream_payment.txt
	├── paymo_output
	│   └── output1.txt
	|   └── output2.txt
	|   └── output3.txt
	└── insight_testsuite
	 	   ├── run_tests.sh
		   └── tests
	        	└── test-1-paymo-trans
        		│   ├── paymo_input
        		│   │   └── batch_payment.txt
        		│   │   └── stream_payment.txt
        		│   └── paymo_output
        		│       └── output1.txt
        		│       └── output2.txt
        		│       └── output3.txt
        		└── your-own-test
            		 ├── paymo_input
        		     │   └── batch_payment.txt
        		     │   └── stream_payment.txt
        		     └── paymo_output
        		         └── output1.txt
        		         └── output2.txt
        		         └── output3.txt

The contents of `src` do not have to contain the single file called `"antifraud.java"`, you are free to include one or more files and name them as required by your implementation.

##Testing your directory structure and output format
[Back to Table of Contents] (README.md#table-of-contents)

To make sure that your code has the correct directory structure and the format of the output data in `output1.txt`, `output2.txt` and `output3.txt` is correct, we included a test script, called `run_tests.sh` in the `insight_testsuite` folder.

The tests are stored simply as text files under the `insight_testsuite/tests` folder. Each test should have a separate folder and each should contain a `paymo_input` folder -- where `batch_payment.txt` and `stream_payment.txt` files can be found. There also should be a `paymo_output` folder where `output1.txt`, `output2.txt` and `output3.txt` should reside.

From the `insight_testsuite` folder, you can run the test with the following command:

	insight_testsuite$ ./run_tests.sh

The output of `run_tests.sh` should look like:

    [PASS]: test-2-paymo-trans (output1.txt)
    [FAIL]: test-2-paymo-trans (output2.txt)
    1c1
    < trusted
    ---
    > unverified
    [PASS]: test-2-paymo-trans (output3.txt
    [Fri Nov  4 13:20:25 PDT 2016] 2 of 3 tests passed

on failed tests and

	[PASS]: test-1-paymo-trans (output1.txt)
	[PASS]: test-1-paymo-trans (output2.txt)
	[PASS]: test-1-paymo-trans (output3.txt)
	[Fri Nov  4 13:20:25 PDT 2016] 3 of 3 tests passed
on success.

One test has been provided as a way to check your formatting and simulate how we will be running tests when you submit your solution. We urge you to write your own additional tests here as well as for your own programming language. `run_tests.sh` should alert you if the directory structure is incorrect.

Your submission must pass at least the provided test in order to pass the coding challenge.

#FAQ

Here are some common questions we've received.  If you have additional questions, please email us at cc@insightdataengineering.com and we'll answer your questions as quickly as we can, and update this FAQ.

* *Which Github link should I submit?*  
You should submit the URL for the top-level root of your repository.  For example, this repo would be submitted by copying the URL `https://github.com/InsightDataScience/digital-wallet` into the appropriate field on the application.  Do NOT try to submit your coding challenge using a pull request, which would make your source code publicly available.  

* *Do I need a private Github repo?*  
No, you may use a public repo, there is no need to purchase a private repo.  You may also submit a link to a Bitbucket repo if you prefer.

* *If User A sends a payment to User B, is that different than if User B sends a payment to User A?*  
No, for simplicity all relationships should be undirected. Users are "friends" regardless of who initiated the payment.  

* *May I use R, Matlab, or other analytics programming languages to solve the challenge?*  
It's important that your implementation scales to handle large amounts of data. While many of our Fellows have experience with R and Matlab, applicants have found that these languages are unable to process data in a scalable fashion, so you should consider another language.  

* *May I use distributed technologies like Hadoop or Spark?*  
While you're welcome to do so, your code will be tested on a single machine so there may not be a significant benefit to using these technologies prior to the program. With that said, learning about distributed systems is a valuable skill for all data engineers.

* *What sort of system should I use to run my program on (Windows, Linux, Mac)?*  
You may write your solution on any system, but your source code should be portable and work on all systems. Additionally, your `run.sh` must be able to run on either Unix or Linux, as that's the system that will be used for testing. Linux machines are the industry standard for most data engineering teams, so it is helpful to be familiar with this. If you're currently using Windows, we recommend using tools like Cygwin or Docker, or a free online IDE such as [Cloud9](http://c9.io).  

* *Can I use pre-built packages, modules, or libraries?*   
This coding challenge can be completed without any "exotic" packages. While you may use publicly available packages, modules, or libraries, you must document any dependencies in your accompanying `README` file. When we review your submission, we will download these libraries and attempt to run your program. If you do use a package, you should always ensure that the module you're using works efficiently for the specific use-case in the challenge, since many libraries are not designed for large amounts of data.

* *Can I use a database engine?*   
This coding challenge can be completed without the use of a database. However, if you must use one, it must be a publicly available one that can be easily installed with minimal configuration.

* *Will you email me if my code doesn't run?*   
Unfortunately, we receive hundreds of submissions in a very short time and are unable to email individuals if code doesn't compile or run. This is why it's so important to document any dependencies you have, as described in the previous question. We will do everything we can to properly test your code, but this requires good documentation. More so, we have provided a test suite so you can confirm that your directory structure and format are correct.

* *Do I need to use multi-threading?*   
No, your solution doesn't necessarily need to include multi-threading - there are many solutions that don't require multiple threads/cores or any distributed systems, but instead use efficient data structures.  

* *Do I need to account for an updating `stream_payment.txt` file?*   
No, your solution doesn't have to re-process `stream_payment.txt` multiple times. If you were doing this project as a data engineer in industry, you would probably connect to a RESTful API to get one transaction at a time, but this is beyond the scope of this challenge. Instead, you should imagine that each line corresponds to a new sequential transaction.

* *What should the format of the output be?*  
In order to be tested correctly, you must use the format described above. You can ensure that you have the correct format by using the testing suite we've included. If you are still unable to get the correct format from the debugging messages in the suite, please email us at cc@insightdataengineering.com.

* *Should I check if the files in the input directory are text files or non-text files(binary)?*  
No, for simplicity you may assume that all of the files in the input directory are text files, with the format as described above.

* *Can I use an IDE like Eclipse or IntelliJ to write my program?*  
Yes, you can use what ever tools you want -  as long as your `run.sh` script correctly runs the relevant target files and creates the `output1.txt`, `output2.txt`, `output3.txt` files in the `paymo_output` directory.

* *What should be in the `paymo_input` directory?*  
You can put any text file you want in the directory since our testing suite will replace it. Indeed, using your own input files would be quite useful for testing.

* *How will the coding challenge be evaluated?*  
Generally, we will evaluate your coding challenge with a testing suite that provides a variety of inputs and checks the corresponding output.  This suite will attempt to use your `run.sh` and is fairly tolerant to different runtime environments.  Of course, there are many aspects (e.g. clean code, documentation) that cannot be tested by our suite, so each submission will also be reviewed manually by a data engineer.

* *How long will it take for me to hear back from you about my submission?*  
We receive hundreds of submissions and try to evaluate them all in a timely manner.  We try to get back to all applicants **within two or three weeks of submission**, but if you have a specific deadline that requires expedited review, you may email us at cc@insightdataengineering.com.  
