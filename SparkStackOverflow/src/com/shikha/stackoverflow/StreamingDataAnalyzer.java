package com.shikha.stackoverflow;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.streaming.kafka.*;

import com.shikha.stackoverflow.common.StreamPost;
import com.shikha.stackoverflow.common.StreamTag;

import kafka.serializer.StringDecoder;
import scala.Tuple2;
import scala.Tuple3;

import org.apache.spark.streaming.Time;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.*;
import org.apache.spark.api.java.function.*;


public final class StreamingDataAnalyzer {
	public static Properties loadProps(JavaSparkContext ctx, String propsFile) throws IOException {
		InputStream inputStream;
		Path pt = new Path(propsFile);
		FileSystem fs = FileSystem.get(ctx.hadoopConfiguration());
		inputStream = fs.open(pt);
		
		Properties properties = new Properties();
		properties.load(inputStream);
		
		for (Object key : properties.keySet()) {
			System.out.println("Prop " + (String) key + " " + (String) properties.get(key));
		}
		return properties;
	}
	
	
	public static void main(String[] args) throws Exception {
		int batchDuration = 5;
		int slidingDuration = 10;
		int windowDuration = 20;
		
		//Setting streaming spark context
        SparkConf conf = new SparkConf().setAppName("Streaming Posts Handler");
        JavaSparkContext sc = new JavaSparkContext(conf);
        Properties props = null;

		if (args.length >= 1) {
	      props = loadProps(sc, args[0]);
	  	      
		  if (args.length >= 4) {
			batchDuration = Integer.parseInt(args[1]);
			slidingDuration = Integer.parseInt(args[2]);
			windowDuration = Integer.parseInt(args[3]);
		  }
		} else {
			System.err.println("Expected: kafkaTopic ");
            return;
		}
		
        sc.setCheckpointDir(props.getProperty("spark.checkpointdir"));
        JavaStreamingContext ssc = new JavaStreamingContext(sc, Durations.seconds(batchDuration));
       
       //Creating Kafka parameters
       Set<String> topicsSet = Collections.singleton(props.getProperty("kafkaTopic"));
       Map<String, String> kafkaParams = new HashMap<String, String>();
       //kafkaParams.put("metadata.broker.list", "ip-172-31-2-73:9092,ip-172-31-2-70:9092,ip-172-31-2-77:9092,ip-172-31-2-75:9092");
       kafkaParams.put("metadata.broker.list", props.getProperty("metadata.broker.list"));
       
       //creating spark session object from spark conf for caching experts
       SparkSession spark = JavaSparkSessionSingleton.getInstance(conf, props.getProperty("spark.cassandra.connection.host"));
    	  // load the experts from Cassandra
 	   Dataset<Row> expertsDF = spark.read()
 	        .format("org.apache.spark.sql.cassandra")
 	        .options(new HashMap<String, String>() {
 	            {
 	                put("keyspace", "stackoverflow");
 	                put("table", "tag_experts");
 	            }
 	        })
 	        .load();
 	  //expertsDF.show();
 	  expertsDF.createOrReplaceTempView("EXPERTS");
 	  // Cache EXPERTS in memory
 	  expertsDF.cache();
 	  
 	  //Create stream of Posts from Kafka topic 
       JavaPairInputDStream<String, String> directKafkaStream =
  		     KafkaUtils.createDirectStream(ssc,
  		    	  String.class,
  		          String.class,
  		          StringDecoder.class,
  		          StringDecoder.class,
  		         kafkaParams, topicsSet);
       

       // parse the input stream and create Post object (Function<Tuple2<arg1, arg2>, returnType>)
       JavaDStream<StreamPost> postStream =  directKafkaStream.map(new Function<Tuple2<String, String>, StreamPost>() {
    	      @Override
    	      public StreamPost call(Tuple2<String, String> tuple2) {
    	        return StreamPost.parseString(tuple2._2());
    	      }
    	    });
 
       // store the postStream DStream
       postStream.foreachRDD(new VoidFunction2<JavaRDD<StreamPost>, Time>() {
    	      @Override
    	      public void call(JavaRDD<StreamPost> rdd, Time time) {
    	    	  SparkSession spark = JavaSparkSessionSingleton.getInstance(rdd.context().getConf(), null);
    	    	  Dataset<Row> postDF =	spark.createDataFrame(rdd, StreamPost.class);
    	    	      	    	  
    	    	  //Store incoming Post in the live_posts_by_day table in cassandra with group_day as partition key
     	    	  storeResults(spark, postDF, 
    	    			  "POSTS", 
    	    			  "SELECT date_format(creation_date, 'yyyy.MM.dd') as group_day, id, creation_date, title, post_type_id, accepted_answer_id, parent_id, tags from POSTS",
    	    			  "live_posts_by_day");
    	      }
       });

  	   // filter posts to get all the questions
       JavaDStream<StreamPost> questions = postStream.filter(filterQuestions);
       
       // Map to Tag, count Pair
       //https://github.com/apache/spark/blob/master/examples/src/main/java/org/apache/spark/examples/streaming/JavaDirectKafkaWordCount.java
       JavaPairDStream<String, Integer> streamTags = questions.mapToPair(new PairFunction<StreamPost, String, Integer>() {
           @Override
           public Tuple2<String, Integer> call(StreamPost p) {
             return new Tuple2<>(p.getTags(), 1);
           }
       });
       
       // Use reduceFunc and inverseReduceFunc capability for best performance
       // http://spark.apache.org/docs/latest/streaming-programming-guide.html#transformations-on-dstreams - Window Operations
	   JavaPairDStream<String, Integer> windowTagCount = streamTags.reduceByKeyAndWindow(reduceTagFunc, invReduceTagFunc, 
			                        Durations.seconds(windowDuration), Durations.seconds(slidingDuration));
	   JavaDStream<StreamTag> windowTagStream = windowTagCount.map(new Function<Tuple2<String, Integer>, StreamTag>() {
 	      @Override
 	      public StreamTag call(Tuple2<String, Integer> tuple2) {
 	        return StreamTag.parseTag(tuple2._1(), tuple2._2());
 	      }
 	    });
	   windowTagStream.foreachRDD(new VoidFunction2<JavaRDD<StreamTag>, Time>() {
    	      @Override
    	      public void call(JavaRDD<StreamTag> rdd, Time time) {
    	    	  SparkSession spark = JavaSparkSessionSingleton.getInstance(rdd.context().getConf(), null);
                  Dataset<Row> tag = spark.createDataFrame(rdd, StreamTag.class);
                  storeResults(spark, tag, "live_tag_counts", SaveMode.Overwrite);
    	      }
       });
     	    	  
       // process the postStream DStream
       questions.foreachRDD(new VoidFunction2<JavaRDD<StreamPost>, Time>() {
    	      @Override
    	      public void call(JavaRDD<StreamPost> rdd, Time time) {
    	    	  SparkSession spark = JavaSparkSessionSingleton.getInstance(rdd.context().getConf(), null);
    	    	  Dataset<Row> questions = spark.createDataFrame(rdd, StreamPost.class);
    	    	  //questions.show();
    	    	  //questions.createOrReplaceTempView("Questions");
    	    	  
    	    	  //Attaching Experts to questions
    	    	  Dataset<Row> post_experts = storeResults(spark, questions,
    	    			  "QUESTIONS", 
    	    			  "SELECT date_format(creation_date, 'yyyy.MM.dd HH') as group_hour, id, creation_date, title, tags, e.expert_name as experts from QUESTIONS q JOIN (SELECT * From EXPERTS) e ON q.tags = e.tag",
    	    			  "live_posts_experts_by_hour");
    	    	  
    	    	  //Storing last group_hour processed for posts_experts - need for querying data
    	          storeResults(spark, post_experts,
    	    				  "POSTS_EXPERTS", 
    	    				  "SELECT 'live_posts_experts_by_hour' as table_name, group_hour as group_val FROM POSTS_EXPERTS ",
    	    				  "posts_data");
    	    	 
    	    	  
    	    	  //spark.sql("SELECT id,  from POST_EXPERT GROUP BY id");
    	    	  
    	    	  /*
    	    	  rdd.map(new Function<StreamPost, StreamPost>() {
					@Override
					public StreamPost call(StreamPost arg0) throws Exception {
						System.out.println("******************* PROCESSING a post ##############");
						if (arg0 != null && arg0.getTags() != null) {
							String tag = arg0.getTags();
							String sql = "SELECT * from EXPERTS WHERE tag = '" + tag + "'";
							System.out.println("********************** Getting two experts " + sql);
							// get two experts
							Dataset<Row> tagExpert = spark.sql(sql);
							tagExpert.show();
							Row[] expertRows = (Row[]) tagExpert.take(2);
							if (expertRows != null) {
								
							}
						}
						return arg0;
					}
    	    		  
    	    	  });
    	    	  */
    	    	
    	      
    	    	  
    	      }
       
       });
       
        
       // start spark job
       ssc.start();
       ssc.awaitTermination();
	}
	
	// adding tag counts
	static Function2<Integer, Integer, Integer> reduceTagFunc = new Function2<Integer, Integer, Integer> () {
		@Override
		public Integer call(Integer arg0, Integer arg1) throws Exception {
			return arg0+arg1;
		}
       };
    // subtracting tag counts
    static Function2<Integer, Integer, Integer> invReduceTagFunc = new Function2<Integer, Integer, Integer> () {
   		@Override
   		public Integer call(Integer arg0, Integer arg1) throws Exception {
   			return arg0-arg1;
   		}
          };
          
	static Function<StreamPost, Boolean> filterQuestions = new Function<StreamPost, Boolean>() {
		@Override
		public Boolean call(StreamPost arg0) throws Exception {
			return "1".equals(arg0.getPost_type_id());
		}
       };
	
	//https://github.com/apache/spark/blob/master/examples/src/main/java/org/apache/spark/examples/streaming/JavaSqlNetworkWordCount.java
	/** Lazily instantiated singleton instance of SparkSession */
	static class JavaSparkSessionSingleton {
	  private static transient SparkSession instance = null;
	  public static SparkSession getInstance(SparkConf sparkConf, String cassandraHost) {
	    if (instance == null) {
	      instance = SparkSession
	        .builder()
	        .config(sparkConf)
	        .config("spark.cassandra.connection.host", cassandraHost)
	        .getOrCreate();
	    }
	    return instance;
	  }
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
	 static Dataset<Row> storeResults(SparkSession spark, Dataset<Row> tagDF, 
			 String type, String query, final String table) {
		 
		 Dataset<Row> tagCounts = null;
		 
		 if (query != null) {
		 	//create a temp table view
		 	tagDF.createOrReplaceTempView(type);
		 	
		 	//Execute the query
	        tagCounts = spark.sql(query);
		 } else {
			 tagCounts = tagDF;
		 }
	        //tagCounts.show();
	        //tagCounts.javaRDD().saveAsTextFile(outFile + "/" +  type);
	        
	        //Save to database
	        tagCounts
	          .write()
	          .format("org.apache.spark.sql.cassandra")
	          .options(new HashMap<String, String> () {
	        	  {
	        		  put("keyspace", "stackoverflow");
	        		  put("table", table);
	        	  }
	          }).mode(SaveMode.Append).save();
	        
	        return tagCounts;
	 }

	 static Dataset<Row> storeResults(SparkSession spark, Dataset<Row> tagDF, 
			                         final String table, SaveMode savemode) {
		//Save to database
		 tagDF
	          .write()
	          .format("org.apache.spark.sql.cassandra")
	          .options(new HashMap<String, String> () {
	        	  {
	        		  put("keyspace", "stackoverflow");
	        		  put("table", table);
	        	  }
	          }).mode(savemode).save();
	     return tagDF;      
	 }
}
