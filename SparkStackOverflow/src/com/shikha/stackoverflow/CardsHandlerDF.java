package com.shikha.stackoverflow;

import java.util.Iterator;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SQLContext;
import org.apache.spark.sql.SparkSession;

import com.shikha.stackoverflow.common.JavaCard;

// http://spark.apache.org/docs/latest/sql-programming-guide.html

public class CardsHandlerDF {
	public static void main(String[] args) {
    	String inputFile, output;
        if (args.length == 2) {
        	inputFile = args[0];
            output = args[1];
        } else {
            System.err.println("Expected: input output");
            return;
        }

        // Create a Java Spark Context
        //SparkConf conf = new
        //SparkConf().setMaster("local").setAppName("Posts Handler");
        //JavaSparkContext sc = new JavaSparkContext(conf);

        //SQLContext sqlContext = new SQLContext(sc);
        
        
        SparkSession spark = new SparkSession.Builder().appName("Card DF Handler").master("local").getOrCreate();
        
        
        
        @SuppressWarnings("unchecked")
		JavaRDD<JavaCard> cardRDD =
        spark.read()
         .textFile(inputFile)
         .javaRDD()
         .map(new Function<String, JavaCard>() {
        	 @Override
        	 public JavaCard call(String line) throws Exception {
        		 String[] split = line.split("\t");

                 int cardValue = Integer.parseInt(split[0]);
                 String cardSuit = split[1];
                 JavaCard card = new JavaCard();
                 card.setNum(cardValue);
                 card.setSuit(cardSuit);
                 return card;
        	 }
         });
        
        Dataset<Row> cardDF = spark.createDataFrame(cardRDD, JavaCard.class);
        
        cardDF.createOrReplaceTempView("cards");
        
        Dataset<Row> results = spark.sql("SELECT suit FROM cards");
        
        JavaRDD<Row> resultRdd = results.javaRDD();
        resultRdd.saveAsTextFile(output);
	}
}
