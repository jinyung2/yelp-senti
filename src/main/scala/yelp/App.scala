package yelp

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.log4j.Logger
import org.apache.log4j.Level
import yelp.CosineSimilarity._

import scala.collection.mutable.TreeSet
import scala.collection.mutable

object App {

  val INPUT_FILE = "script/yelp_review_trimmed.json"
  val TEST_INPUT = "test_output.txt"
  val TRAINING_SET = "training_set.txt"

  val STOP_WORDS = List("a", "about", "above", "after", "again", "against", "ain",
    "all", "am", "an", "and", "any", "are", "aren", "aren't", "as", "at", "be", "because",
    "been", "before", "being", "below", "between", "both", "but", "by", "can", "couldn",
    "couldn't", "d", "did", "didn", "didn't", "do", "does", "doesn", "doesn't", "doing",
    "don", "don't", "down", "during", "each", "few", "for", "from", "further", "had",
    "hadn", "hadn't", "has", "hasn", "hasn't", "have", "haven", "haven't", "having",
    "he", "her", "here", "hers", "herself", "him", "himself", "his", "how", "i", "if",
    "in", "into", "is", "isn", "isn't", "it", "it's", "its", "itself", "just", "ll", "m",
    "ma", "me", "mightn", "mightn't", "more", "most", "mustn", "mustn't", "my", "myself",
    "needn", "needn't", "no", "nor", "not", "now", "o", "of", "off", "on", "once", "only",
    "or", "other", "our", "ours", "ourselves", "out", "over", "own", "re", "s", "same",
    "shan", "shan't", "she", "she's", "should", "should've", "shouldn", "shouldn't", "so",
    "some", "such", "t", "than", "that", "that'll", "the", "their", "theirs", "them",
    "themselves", "then", "there", "these", "they", "this", "those", "through", "to",
    "too", "under", "until", "up", "ve", "very", "was", "wasn", "wasn't", "we", "were",
    "weren", "weren't", "what", "when", "where", "which", "while", "who", "whom", "why",
    "will", "with", "won", "won't", "wouldn", "wouldn't", "y", "you", "you'd", "you'll",
    "you're", "you've", "your", "yours", "yourself", "yourselves", "could", "he'd", "he'll",
    "he's", "here's", "how's", "i'd", "i'll", "i'm", "i've", "let's", "ought", "she'd",
    "she'll", "that's", "there's", "they'd", "they'll", "they're", "they've", "we'd",
    "we'll", "we're", "we've", "what's", "when's", "where's", "who's", "why's", "would")


  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("akka").setLevel(Level.OFF)

  val conf = new SparkConf().setAppName("NameOfApp")
    .setMaster("local[4]")
  val sc = new SparkContext(conf)

  def main(args: Array[String]) = {

    val tfidf = new TfIdf with Serializable;

    val trainingSet = sc.textFile(TEST_INPUT)

    tfidf.train(trainingSet)
    
    // for some reason, the idf map only works when you get it like this first
    val idfMap = tfidf.getIDF;
    // printing word at tf-idf of each word in reviews array
    val trainingtfidf = tfidf.getReviews.map(review =>
      sc.parallelize(review.getWordVec())
        .map{ case (word, tf) => ((word, tf * idfMap.getOrElse(word, 0.0)))}
        .map(x => (x, review.getStar()))
    )
    // cosine similarity
    val testInput = sc.textFile(TRAINING_SET)
      .map(_.split(", ", 2))
      .map(x => (x(0).trim.toInt, x(1)))
      .collect()
      .map { case (star, text) => {
        val temp = text
          .replaceAll("[^a-zA-Z ]", "")
          .toLowerCase()
          .split(" ")
          .filter(x => x != "")
          .filterNot(STOP_WORDS.contains(_))
          .map(word => (word, 1))
        sc.parallelize(temp)
          .reduceByKey((x, y) => x + y)
          .map { case (word, tf) => (word, (tf.toDouble / temp.length)) }
          .map(x => (x._1, x._2 * idfMap.getOrElse(x._1, 0.0)))
      }}

    println("Running KNN with K = 3:\n")
    testInput.foreach(testReview => {
      var topN: List[(Double, Int)] = List.empty
      trainingtfidf.foreach(train => {
        val C = train.map(_._1._1).subtract(testReview.map(_._1))
          .map(x => (x, 0.0))
        val D = testReview.map(_._1).subtract(train.map(_._1._1))
          .map(x => (x, 0.0))
        val A = testReview.union(C).map(_._2).collect()
        val B = train.map(_._1).union(D).map(_._2).collect()
        val cosSimVal = cosineSimilarity(A, B)
        val star = train.map(_._2).collect()(0)
        topN = (cosSimVal, star)::topN
        })
      val total = topN.sortBy(_._1).take(3).map(_._2).sum
      val average = total / 3.0
      printf("TOTAL: %4d\nAVERAGE: %.3f\n", total, average)
      print("INPUTTED REVIEW IS: ")
      if (average >= 3.5)
        println("POSITIVE\n")
      else
        println("NEGATIVE\n")
      })
  }
}