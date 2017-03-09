package pair

/**
 * @author training
 */
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

object PairRDD_Transformations {
   def main(args:Array[String]) {
    val conf = new SparkConf
    conf.setMaster("local[*]").setAppName("PairRDD_transformations")
    val sc = new SparkContext(conf)

    val baseRdd =
      sc.parallelize(Array("this,is,a,classroom","that,is,a,monitor","students,are, dreaming,of,teabreak"))
        val inputRdd = sc.makeRDD(List(("is",2), ("that",2), ("monitor",8), ("this",6),("students",5),("a",1)))
        val wordsRdd = baseRdd.flatMap(record => record.split(","))
        val wordPairs = wordsRdd.map(word => (word, word.length))
        val filteredWordPairs = wordPairs.filter{case(word, length) =>
        length >=2}

    val textFile = sc.textFile("/home/training/git/HadoopShowcase/SimpleSparkExamples/data/stock.txt")
      val stocksPairRdd = textFile.map{record => val colData =
      record.split(",")
      (colData(0),colData(6))}

    val stocksGroupedRdd = stocksPairRdd.groupByKey
    val stocksReducedRdd = stocksPairRdd.reduceByKey((x,y)=>x+y)
    val subtractedRdd = wordPairs.subtractByKey(inputRdd)
    val cogroupedRdd = wordPairs.cogroup(inputRdd)
    val joinedRdd = filteredWordPairs.join(inputRdd)
    val sortedRdd = wordPairs.sortByKey(true)
    val leftOuterJoinRdd = inputRdd.leftOuterJoin(filteredWordPairs)
    val rightOuterJoinRdd = wordPairs.rightOuterJoin(inputRdd)
    val flatMapValuesRdd = filteredWordPairs.flatMapValues(length =>
      1 to 5)
    val mapValuesRdd = wordPairs.mapValues(length => length*2)
    val keys = wordPairs.keys
    val values = filteredWordPairs.values
  }
}