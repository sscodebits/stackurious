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
 * This class read the xml file from s3 and parses it and creating a data frame
 * and store it in the hdfs in parquet format
 * 
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

		SparkSession spark = new SparkSession.Builder().appName("Posts Ingestion").getOrCreate();

		@SuppressWarnings("unchecked")
		JavaRDD<TagObject> tagsRDD = spark.read().textFile(inputFile + "/Tags.xml").javaRDD()
				.flatMap(new FlatMapFunction<String, TagObject>() {
					@Override
					public Iterator<TagObject> call(String line) throws Exception {
						Element e = ParseUtil.parseString(line);
						List<TagObject> tagList = TagObject.flattenTag(TagObject.parseElement(e));
						return tagList.iterator();
					}
				});

		Dataset<Row> tagDF = spark.createDataFrame(tagsRDD, TagObject.class);
		tagDF.show();
		tagDF.write().save(output + "/tags");

		@SuppressWarnings("unchecked")
		JavaRDD<UserObject> usersRDD = spark.read().textFile(inputFile + "/Users.xml").javaRDD()
				.flatMap(new FlatMapFunction<String, UserObject>() {
					@Override
					public Iterator<UserObject> call(String line) throws Exception {
						Element e = ParseUtil.parseString(line);
						List<UserObject> userList = UserObject.flattenUser(UserObject.parseElement(e));
						return userList.iterator();
						// return RecordUtil.parseTag(e);
					}
				});

		Dataset<Row> userDF = spark.createDataFrame(usersRDD, UserObject.class);
		userDF.show();
		userDF.write().save(output + "/users");

		@SuppressWarnings("unchecked")
		JavaRDD<PostObject> postRDD = spark.read().textFile(inputFile + "/Posts.xml").javaRDD()
				.flatMap(new FlatMapFunction<String, PostObject>() {// converting tags in post into list of tags 
					@Override
					public Iterator<PostObject> call(String line) throws Exception {
						Element e = ParseUtil.parseString(line);
						List<PostObject> postList = PostObject.flattenTags(PostObject.parseElement(e));
						return postList.iterator();
					}
				});

		Dataset<Row> postDF = spark.createDataFrame(postRDD, PostObject.class);
		postDF.show(100);
		postDF.write().save(output + "/posts");

	}

}
