package com.shikha.stackoverflow;

import java.util.HashMap;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

//http://spark.apache.org/docs/latest/sql-programming-guide.html

public class BatchDataAnalyzer {
	public static void main(String[] args) {
		String inputFile, output = null, cassandraHost = null;
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

		SparkSession spark = new SparkSession.Builder().appName("Batch Analyzer")
				.config("spark.cassandra.connection.host", cassandraHost).getOrCreate();

		processData(spark, inputFile, output, "tags", "SELECT CURRENT_TIMESTAMP as rundate, id, name, count from tags",
				"tag_counts");

		Dataset<Row> postsDF = getDataFrame(spark, inputFile, "posts");

		postsDF.createOrReplaceTempView("POSTS2");
		// get all questions
		Dataset<Row> questions = spark.sql(
				"SELECT origp.id, origp.tags, origp.creation_date, origp.view_count, origp.title, origp.accepted_answer_id, origp.owner_user_id from POSTS2 origp WHERE origp.post_type_id = '1'");
		System.out.println("############################## Posts " + postsDF.count() + "Questions " + questions.count());

		questions.createOrReplaceTempView("Questions");

		// get tag counts by month
		Dataset<Row> tagCountByMonth =
		storeResults(spark, questions, output, "tag_counts_by_m",
				"SELECT tags as name, year(creation_date) as year, month(creation_date) as month, count(*) as count FROM tag_counts_by_m GROUP BY year(creation_date), month(creation_date), tags",
				"tag_counts_by_month");

		storeResults(spark, tagCountByMonth, output, "tagCountByMonth",
				"SELECT 'tag_counts_by_month' as table_name, concat(first(year), '_', format_string('%02d',first(month))) as group_val FROM tagCountByMonth group by year, month",
				"posts_data", SaveMode.Append);

		// dense_rank and other spark window function
		// https://databricks.com/blog/2015/07/15/introducing-window-functions-in-spark-sql.html
		// http://stackoverflow.com/questions/36660625/spark-sql-top-n-per-group

		// get all unanswered questions
		Dataset<Row> unansweredPosts = spark.sql(
				"SELECT origp.id, origp.tags, origp.creation_date, origp.view_count, origp.title from Questions origp WHERE  origp.accepted_answer_id is null");
		System.out.println("############################## questions " + questions.count() + " unanswer "
				+ unansweredPosts.count());
		// store top 10 unanswered questions per tag
		storeResults(spark, unansweredPosts, output, "posts_u",
				"SELECT id,tags, creation_date, unix_timestamp(creation_date) as cdate, view_count,title FROM (SELECT id,tags, creation_date,view_count,title,dense_rank() OVER (PARTITION BY tags ORDER BY view_count DESC) as rank  FROM posts_u) tmp where rank <=5",
				"faq_unanswered");

		// get all answered questions
		Dataset<Row> answeredPosts = spark.sql(
				"SELECT origp.id, origp.tags, origp.creation_date, origp.view_count, origp.title, origp.accepted_answer_id from POSTS2 origp WHERE origp.accepted_answer_id is not null");
		System.out.println(
				"############################## questions " + questions.count() + " answer " + answeredPosts.count());
		// store top 10 answered questions per tag
		storeResults(spark, answeredPosts, output, "posts_a",
				"SELECT id,tags, creation_date, unix_timestamp(creation_date) as cdate,view_count,title FROM (SELECT id,tags, creation_date,view_count,title,dense_rank() OVER (PARTITION BY tags ORDER BY view_count DESC) as rank  FROM posts_a) tmp where rank <=5",
				"faq_answered");

		// expert
		// foreach each tag find all users with answered questions
		answeredPosts.createOrReplaceTempView("answeredPosts");
		Dataset<Row> expertsByTag = spark.sql(
				"SELECT origp.id, origp.creation_date, origp.view_count, origp.title, origp.tags as tag, ansp.owner_user_id as expert_id from answeredPosts origp JOIN (SELECT * from POSTS2) ansp ON origp.accepted_answer_id=ansp.id");
		System.out.println(
				"############################## questions " + questions.count() + " answer " + expertsByTag.count());

		// load user data
		Dataset<Row> usersDF = getDataFrame(spark, inputFile, "users");
		usersDF.createOrReplaceTempView("users");

		// count the number of answers of each user per tag
		storeResults(spark, expertsByTag, output, "experts",
				"SELECT tag, count(*) as ans_count, user.display_name as expert_name FROM experts JOIN (SELECT * from users) user ON experts.expert_id=user.id GROUP BY tag, user.display_name HAVING count(*) > 10",
				"tag_experts");

	}

	/**
	 * Processing data and storing results in Cassandra
	 * 
	 * @param spark
	 * @param inputFile
	 * @param outFile
	 * @param type
	 * @param query
	 * @param table
	 */
	static void processData(SparkSession spark, String inputFile, String outFile, String type, String query,
			final String table) {

		Dataset<Row> tagDF = getDataFrame(spark, inputFile, type);
		storeResults(spark, tagDF, outFile, type, query, table);

	}

	/**
	 * Retrieving dataset from parquet inputfile
	 * 
	 * @param spark
	 * @param inputFile
	 * @param type
	 * @return
	 */
	static Dataset<Row> getDataFrame(SparkSession spark, String inputFile, String type) {
		Dataset<Row> tagDF = spark.read().parquet(inputFile + "/" + type);

		tagDF.printSchema();

		System.out.println("############################## " + type + " Count " + tagDF.count());
		return tagDF;
	}

	/**
	 * Executing query and Storing results in Cassandra
	 */
	static Dataset<Row> storeResults(SparkSession spark, Dataset<Row> tagDF, String outFile, String type, String query,
			final String table, SaveMode saveMode) {
		tagDF.createOrReplaceTempView(type);
		Dataset<Row> tagCounts = spark.sql(query);

		tagCounts.show();
		// tagCounts.javaRDD().saveAsTextFile(outFile + "/" + type);
		tagCounts.write().format("org.apache.spark.sql.cassandra").options(new HashMap<String, String>() {
			{
				put("keyspace", "stackoverflow");
				put("table", table);
			}
		}).mode(saveMode).save();
		return tagCounts;
	}
	static Dataset<Row> storeResults(SparkSession spark, Dataset<Row> tagDF, String outFile, String type, String query,
			final String table) {
		return storeResults(spark, tagDF, outFile, type, query, table, SaveMode.Overwrite);
	}
}
