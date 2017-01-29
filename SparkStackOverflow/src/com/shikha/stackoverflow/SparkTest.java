package com.shikha.stackoverflow;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;

public class SparkTest {
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
        SparkConf conf = new
        SparkConf().setMaster("local").setAppName("Card Count");
        JavaSparkContext sc = new JavaSparkContext(conf);

        // Load our input data.
        JavaRDD<String> input = sc.textFile(inputFile);

        // Split up into suits and numbers and
        // transform into pairs
        JavaPairRDD<String, Integer> suitsAndValues = input.mapToPair(w -> {
            String[] split = w.split("\t");

            int cardValue = Integer.parseInt(split[0]);
            String cardSuit = split[1];
            return new Tuple2<String, Integer>(cardSuit, cardValue);
        });

        JavaPairRDD<String, Integer> counts = suitsAndValues.reduceByKey((x,
                                              y) -> x + y);

        counts.saveAsTextFile(output);
    }

}
