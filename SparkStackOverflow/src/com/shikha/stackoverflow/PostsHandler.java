package com.shikha.stackoverflow;

import java.util.HashMap;

import java.util.Iterator;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

import com.shikha.stackoverflow.common.PostObject;
import com.shikha.stackoverflow.common.TagObject;
import com.shikha.stackoverflow.util.ParseUtil;
import org.w3c.dom.Element;


// http://spark.apache.org/docs/latest/sql-programming-guide.html
// https://github.com/databricks/spark-xml

public class PostsHandler {
	public static void main(String[] args) {
    	String inputFile, output;
        if (args.length == 2) {
        	inputFile = args[0];
            output = args[1];
        } else {
            System.err.println("Expected: input output");
            return;
        }        
        
        SparkSession spark = new SparkSession
        		.Builder()
        		.appName("Posts Handler")
        		.config("spark.cassandra.connection.host", "ip-172-31-2-73")
        		
        		.master("local").getOrCreate();
        //          .config("spark.sql.caseSensitive", "false")
        //        		.config("spark.cassandra.connection.port", "")

  		/*Dataset<Row> postDF =
        spark.read()
        .format("com.databricks.spark.xml")
        .option("rowTag", "row")
        .load(inputFile);
        
        long postsCount = postDF.count();
        System.out.println("TOTAL COUNT ##################### " + postsCount);
        postDF.printSchema();
        */
        // removes the first two lines
        // need to remove the last line too
        Function2 removeHeader= new Function2<Integer, Iterator<String>, Iterator<String>>(){

			@Override
			public Iterator<String> call(Integer arg0, Iterator<String> arg1) throws Exception {
                if((arg0==0 || arg0==1) && arg1.hasNext()){
                	arg1.next();
                    return arg1;
                } else {
                    return arg1;
                }
            }
        };

        
        
        @SuppressWarnings("unchecked")
		JavaRDD<TagObject> tagsRDD =
        spark.read()
         .textFile(inputFile + "/Tags.xml")
         .javaRDD()
         .mapPartitionsWithIndex(removeHeader, false)
         .map(new Function<String, TagObject>() {
        	 @Override
        	 public TagObject call(String line) throws Exception {
        		 Element e = ParseUtil.parseString(line);
                 return TagObject.parseElement(e);
        	 }
         });
        
        Dataset<Row> tagDF = spark.createDataFrame(tagsRDD, TagObject.class);
        tagDF.createOrReplaceTempView("tags");
        Dataset<Row> tagCounts = spark.sql("SELECT id, tagname, count from tags ORDER BY count DESC LIMIT 10");
        JavaRDD<Row> tagCountsRdd = tagCounts.javaRDD();
        tagCountsRdd.saveAsTextFile(output + "/tags");
 
        tagCounts
          .write()
          .format("org.apache.spark.sql.cassandra")
          .options(new HashMap<String, String> () {
        	  {
        		  put("keyspace", "stackoverflow");
        		  put("table", "test_tags");
        	  }
          }).mode(SaveMode.Overwrite).save();
        
        

        
        @SuppressWarnings("unchecked")
		JavaRDD<PostObject> postRDD =
        spark.read()
         .textFile(inputFile + "/Posts.xml")
         .javaRDD()
         .mapPartitionsWithIndex(removeHeader, false)
         .map(new Function<String, PostObject>() {
        	 @Override
        	 public PostObject call(String line) throws Exception {
        		 Element e = ParseUtil.parseString(line);
                 return PostObject.parseElement(e);
        	 }
         });
        
        //.schema(schema)
        
        Dataset<Row> postDF = spark.createDataFrame(postRDD, PostObject.class);
        postDF.createOrReplaceTempView("posts");
        Dataset<Row> results = spark.sql("SELECT id FROM posts");
        JavaRDD<Row> resultRdd = results.javaRDD();
        resultRdd.saveAsTextFile(output + "/posts");

	}
}
