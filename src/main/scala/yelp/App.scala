package yelp

import org.apache.spark.{SparkConf, SparkContext}
import org.apache.log4j.Logger
import org.apache.log4j.Level

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

    val training_set = sc.textFile(TEST_INPUT)

    tfidf.train(training_set)
    
    // for some reason, the idf map only works when you get it like this first
    val idfMap = tfidf.getIDF;
    // printing word at tf-idf of each word in reviews array
    tfidf.getReviews.foreach(review =>
      sc.parallelize(review.getWordVec())
        .map{ case (word, tf) => {(word, tf * idfMap.getOrElse(word, 0.0))}}
        .collect().foreach(println)
    )
    
    // cosine similarity

    // vectorize tfidf
    // get count of total words over all documents
    val total_words = tfidf.getDF.size
    // make a word map. word to num to mark index in array
    val wordMap = mutable.Map[String,Int]() // word -> index (a value between 0 and the total num of words)
    val docVectorMap = mutable.Map[Int,Array[Double]]() // docId -> tfidf values if word @ index is in doc
    documentFreq.keySet.foreach(word =>{
      wordMap put (word, IndexFactory.create)
    })
    tfidfMap.foreach({case((word, doc), tfidf) => {
      docVectorMap.get(doc) match {
        case None => {
          var arr = Array.fill[Double](total_words)(0) // each document starts with [0 * total words]
          arr(wordMap.get(word).get) = tfidf // if a word is in the document, the value will be the tfidf of word in doc
          docVectorMap put (doc, arr)
        }
        case Some(arr) => {
          arr(wordMap.get(word).get) = tfidf
        }
      }
    }})

    docVectorMap.foreach({case (k, v) => println(k, "[", v.mkString(" "), "]")})
  }
}