package com.shikha.stackoverflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

//http://spark.apache.org/docs/latest/sql-programming-guide.html

public class BatchDataAnalyzer {
	public static void main(String[] args) {
    	String inputFile, output=null, cassandraHost=null;
        if (args.length >= 2) {
        	cassandraHost = args[0];
        	inputFile = args[1];
        	if (args.length >= 3) {
        		output = args[2];
        	}
        } else {
            System.err.println("Expected: cassandraHost input output");
            return;
        }        
    
        SparkSession spark = new SparkSession
    		.Builder()
    		.appName("Posts Handler")
    		.config("spark.cassandra.connection.host", cassandraHost)
       		.getOrCreate();
    

        processData(spark, inputFile, output, "tags", 
        		"SELECT CURRENT_TIMESTAMP as rundate, id, name, count from tags",
        		"tag_counts");

        Dataset<Row> postsDF = getDataFrame(spark, inputFile, "posts");
//        storeResults(spark, postsDF, output, "posts",
//        		"SELECT origp.id, origp.creation_date, origp.view_count, origp.title  from POSTS origp JOIN (SELECT * from POSTS) ansp ON origp.accepted_answer_id=ansp.id WHERE origp.post_type_id = 1 and origp.accepted_answer_id is not null ORDER BY origp.view_count DESC ",
//        		"faq_answered");
        
      //find all the  posts of questions which are answered  and assign popularity count (view count +favorite count*10)
      //storeResults(spark, postsDF, output, "posts",
	//	"SELECT origp.id, origp.tags, origp.creation_date, origp.view_count, origp.title, (origp.view_count+origp.favorite_count*10) as pop_count from POSTS origp WHERE origp.post_type_id = '1' and origp.accepted_answer_id is not null ORDER BY tags, pop_count DESC ",
		//"faq_answered");
        
      
        
       // Timestamp(DatatypeConverter.parseDateTime();

      postsDF.createOrReplaceTempView("POSTS2");
      // get all questions
      Dataset<Row> questions = spark.sql("SELECT origp.id, origp.tags, origp.creation_date, origp.view_count, origp.title, origp.accepted_answer_id, origp.owner_user_id from POSTS2 origp WHERE origp.post_type_id = '1'");
      System.out.println("############################## Posts " +postsDF.count() + "Questions " +questions.count());
      
      questions.createOrReplaceTempView("Questions");

      // get tag counts by month
      storeResults(spark, questions, output, "tag_counts_by_m",
    		  "SELECT tags as name, year(creation_date) as year, month(creation_date) as month, count(*) as count FROM tag_counts_by_m GROUP BY year(creation_date), month(creation_date), tags",
    		  "tag_counts_by_month");
      
      
      
      //dense_rank and other spark window function
      //https://databricks.com/blog/2015/07/15/introducing-window-functions-in-spark-sql.html
      //http://stackoverflow.com/questions/36660625/spark-sql-top-n-per-group

      // get all unanswered questions
      Dataset<Row> unansweredPosts = spark.sql("SELECT origp.id, origp.tags, origp.creation_date, origp.view_count, origp.title from Questions origp WHERE  origp.accepted_answer_id is null");
      System.out.println("############################## questions " +questions.count() + " unanswer " + unansweredPosts.count());
      // store top 10 unanswered questions per tag
      storeResults(spark, unansweredPosts, output, "posts_u",
    		  "SELECT id,tags, creation_date, unix_timestamp(creation_date) as cdate, view_count,title FROM (SELECT id,tags, creation_date,view_count,title,dense_rank() OVER (PARTITION BY tags ORDER BY view_count DESC) as rank  FROM posts_u) tmp where rank <=5",
    		  "faq_unanswered");

      // get all answered questions
      Dataset<Row> answeredPosts = spark.sql("SELECT origp.id, origp.tags, origp.creation_date, origp.view_count, origp.title, origp.accepted_answer_id from POSTS2 origp WHERE origp.accepted_answer_id is not null");
      System.out.println("############################## questions " +questions.count() + " answer " + answeredPosts.count());
      // store top 10 answered questions per tag
      storeResults(spark, answeredPosts, output, "posts_a",
    		  "SELECT id,tags, creation_date, unix_timestamp(creation_date) as cdate,view_count,title FROM (SELECT id,tags, creation_date,view_count,title,dense_rank() OVER (PARTITION BY tags ORDER BY view_count DESC) as rank  FROM posts_a) tmp where rank <=5",
    		  "faq_answered");

      // expert
      //   foreach each tag find all users with answered questions
      answeredPosts.createOrReplaceTempView("answeredPosts");
      Dataset<Row> expertsByTag = spark.sql("SELECT origp.id, origp.creation_date, origp.view_count, origp.title, origp.tags as tag, ansp.owner_user_id as expert_id from answeredPosts origp JOIN (SELECT * from POSTS2) ansp ON origp.accepted_answer_id=ansp.id");
      System.out.println("############################## questions " +questions.count() + " answer " + expertsByTag.count());

      // load user data
      Dataset<Row> usersDF = getDataFrame(spark, inputFile, "users");
      usersDF.createOrReplaceTempView("users");
      
      //   count the number of answers of each user per tag
      storeResults(spark, expertsByTag, output, "experts",
    		  "SELECT tag, count(*) as ans_count, user.display_name as expert_name FROM experts JOIN (SELECT * from users) user ON experts.expert_id=user.id GROUP BY tag, user.display_name HAVING count(*) > 5",
    		  "tag_experts");


 	}
	
	/**
	 * Processing data and storing results in Cassandra
	 * @param spark
	 * @param inputFile
	 * @param outFile
	 * @param type
	 * @param query
	 * @param table
	 */
	 static void processData(SparkSession spark, String inputFile, String outFile, String type, String query, final String table) {
		 
		 Dataset<Row> tagDF = getDataFrame(spark, inputFile, type);
		 storeResults(spark, tagDF, outFile, type, query, table);
		
	}
	 
	 /**
	  * Retrieving dataset from parquet inputfile
	  * @param spark
	  * @param inputFile
	  * @param type
	  * @return
	  */
	 static Dataset<Row> getDataFrame(SparkSession spark, String inputFile, String type) {
		 Dataset<Row> tagDF = 
	        		spark.read()
	                .parquet(inputFile + "/" + type);
	        
	        tagDF.printSchema();
	        
	        System.out.println("############################## " + type + " Count " + tagDF.count());
	        return tagDF;
	 }
	 
	 /**
	  * Executing query and Storing results in Cassandra
	  * @param spark
	  * @param tagDF
	  * @param outFile
	  * @param type
	  * @param query
	  * @param table
	  */
	 static void storeResults(SparkSession spark, Dataset<Row> tagDF, String outFile, String type, String query, final String table) {
		 tagDF.createOrReplaceTempView(type);
	        Dataset<Row> tagCounts = spark.sql(query);
	 
	        tagCounts.show();
	        //tagCounts.javaRDD().saveAsTextFile(outFile + "/" +  type);
	        tagCounts
	          .write()
	          .format("org.apache.spark.sql.cassandra")
	          .options(new HashMap<String, String> () {
	        	  {
	        		  put("keyspace", "stackoverflow");
	        		  put("table", table);
	        	  }
	          }).mode(SaveMode.Overwrite).save();
	 }
     /*
     postsDF.toJavaRDD().flatMap(new FlatMapFunction<Row, Row>() {

			@Override
			public Iterator<Row> call(Row arg0) throws Exception {
				// TODO Auto-generated method stub
				int tagI = arg0.fieldIndex("tags");
				String tagList = arg0.getString(tagI);
				List<Row> newList = new ArrayList<Row>();
				
				if (tagList != null && !tagList.isEmpty()) {	
					// copy current row for each tag
					for (String tag : tagList.split(",")) {
						// copy current row
						Object[] values = new Object[arg0.length()];
						for (int i=0; i<arg0.length();i++) {
							values[i] = arg0.get(i);
						}
						// override tag value
						values[tagI] = tag;
						Row newrow = RowFactory.create(values);
						newList.add(newrow);
					}
				}
				return newList.iterator();
			}
     	
     });
     */
     

}
