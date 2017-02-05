package com.shikha.stackoverflow;

import java.util.Iterator;
import java.util.List;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.w3c.dom.Element;

import com.shikha.stackoverflow.common.PostObject;
import com.shikha.stackoverflow.common.TagObject;
import com.shikha.stackoverflow.common.UserObject;
import com.shikha.stackoverflow.util.ParseUtil;

/**
 * This class read the xml file from s3 and parses it  
 * and creating a data frame and store it in the hdfs in parquet format
 * @author shikha
 *
 */
public class BatchIngestion {
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
        		.master("spark://ip-172-31-2-73:7077").getOrCreate();

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

        /*
        Schema schema = null;
        
        try {
           schema= new Schema.Parser().parse(new File("tag.avsc"));
        } catch (IOException e) {
        	e.printStackTrace();
        }
        */
        
        
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
        		 //return RecordUtil.parseTag(e);
        	 }
         });
        
        Dataset<Row> tagDF = spark.createDataFrame(tagsRDD, TagObject.class);
        
        tagDF.write()
          .save(output + "/tags");
        
        @SuppressWarnings("unchecked")
		JavaRDD<UserObject> usersRDD =
        spark.read()
         .textFile(inputFile + "/Users.xml")
         .javaRDD()
         .mapPartitionsWithIndex(removeHeader, false)
         .map(new Function<String, UserObject>() {
        	 @Override
        	 public UserObject call(String line) throws Exception {
        		 Element e = ParseUtil.parseString(line);
                 return UserObject.parseElement(e);
        		 //return RecordUtil.parseTag(e);
        	 }
         });
        
        Dataset<Row> userDF = spark.createDataFrame(usersRDD, UserObject.class);
        
        userDF.write()
          .save(output + "/users");
        

        @SuppressWarnings("unchecked")
		JavaRDD<PostObject> postRDD =
        spark.read()
         .textFile(inputFile + "/Posts.xml")
         .javaRDD()
         .mapPartitionsWithIndex(removeHeader, false)
         .flatMap(new FlatMapFunction<String, PostObject>() {//converting tags in post into list of tags
        	 @Override
        	 public Iterator<PostObject> call(String line) throws Exception {
        		 Element e = ParseUtil.parseString(line);
                 List<PostObject> postList = PostObject.flattenTags(PostObject.parseElement(e));
                 return postList.iterator();
        	 }
         });
        
        //.schema(schema)
        
        Dataset<Row> postDF = spark.createDataFrame(postRDD, PostObject.class);
        postDF.show(100);
        postDF.write() 
        .save(output + "/posts");

        /*
        tagDF.createOrReplaceTempView("tags");
        Dataset<Row> tagData = spark.sql("SELECT id, name, count from tags");
        JavaRDD<Row> tagDataRdd = tagData.javaRDD();
        tagDataRdd.saveAsTextFile(output + "/tags2");


        // Saves the Avro records read in
        tagDF.write()
          .format("com.databricks.spark.avro")
          .option("avroSchema", schema.toString())
          .save(output + "/tags");
        */
         
 	}
	
}
