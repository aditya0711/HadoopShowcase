package analysis

/**
 * @author training
 */
import org.apache.spark._
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.DataFrameNaFunctions
import org.apache.spark.sql.types._

object OutlierExample {def main(args:Array[String]): Unit = {
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("Outlier_Detection")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    //Loading data
    val titanic_data =
      sqlContext.read.format("com.databricks.spark.csv")
        .option("header", "true")
        .option("inferSchema", "true")
        .load("/home/training/git/HadoopShowcase/SparkAnalysisExamples/data/train.csv")
    val summary = titanic_data.describe()
    summary.show()

    val fareVaues_AtDiffr_Itervals =
      scala.collection.mutable.ListBuffer[Long]()
    val minValue = 0.0
    val maxValue = 513
    val bins = 5
    val range = (maxValue - minValue) / 5.0
    var minCounter = minValue
    var maxCounter = range


    while (minCounter < maxValue) {
      val valuesBetweenRange = titanic_data.filter(titanic_data("Fare").between
      (minCounter, maxCounter))
      fareVaues_AtDiffr_Itervals.+=(valuesBetweenRange.count())
      minCounter = maxCounter
      maxCounter = maxCounter + range
    }
    println("Fare Values at Different Ranges:")
    fareVaues_AtDiffr_Itervals.foreach(println)

  //Outlier Detection mean+2(sigma) or mean-2(sigma)
    val meanFare = titanic_data.select(mean("Fare"))
      .first()(0).asInstanceOf[Double]
    val stddevFare = titanic_data.select(stddev("Fare"))
      .first()(0).asInstanceOf[Double]
    val upper_threshold = meanFare + 2*stddevFare
    val lower_threshold = meanFare - 2*stddevFare
    val fareValues_MoreThanUpperthrshold =
      titanic_data.select("Fare").filter(titanic_data("Fare")
        > upper_threshold)
    val fareValues_LessThanLowerthrshold =
      titanic_data.select("Fare").filter(titanic_data("Fare") <
        lower_threshold)
    val summary_FareValuesMoreThanUppr =
      fareValues_MoreThanUpperthrshold.describe()
    println("Summary Of Fare Values Greater Than Upper Threshold:")
    summary_FareValuesMoreThanUppr.show()
    val summary_FareValuesLessThanLowr =
      fareValues_LessThanLowerthrshold.describe()
    println("Summary Of Fare Values Less Than Lower Threshold:")
    summary_FareValuesLessThanLowr.show()

    //Outlier Detection mean+3(sigma) or mean-3(sigma)
    val upper_threshold1 = meanFare + 3*stddevFare
    val lower_threshold1 = meanFare - 3*stddevFare
    val fareValues_MoreThanUpperthrshold1 =
      titanic_data.select("Fare").filter(titanic_data("Fare") >
        upper_threshold1)
    val fareValues_LessThanLowerthrshold1 =
      titanic_data.select("Fare").filter(titanic_data("Fare") <
        lower_threshold1)
    val summary_FareValuesMoreThanUppr1 =
      fareValues_MoreThanUpperthrshold1.describe()
    println("Summary Of Fare Values Greater Than Upper Threshold:")
    summary_FareValuesMoreThanUppr1.show()
    val summary_FareValuesLessThanLowr1 =
      fareValues_LessThanLowerthrshold1.describe()
    println("Summary Of Fare Values Less Than Lower Threshold:")
    summary_FareValuesLessThanLowr1.show()

    // Calculating z scores and apply outlier detection method:
    val titanic_Data_StdFareValues =
      titanic_data.withColumn("StdFareValue", (titanic_data("Fare")-
        meanFare)/stddevFare)
    val mean_FareStdvalue =
      titanic_Data_StdFareValues.select(mean("StdFareValue"))
        .first()(0).asInstanceOf[Double]
    val stddev_FareStdvalue =
      titanic_Data_StdFareValues.select(stddev("StdFareValue")
      ).first()(0).asInstanceOf[Double]
    val upper_threshold_std = mean_FareStdvalue +
      3*stddev_FareStdvalue
    val lower_threshold_std = mean_FareStdvalue -
      3*stddev_FareStdvalue
    val fareValues_MoreThanUpperthrshold_std =
      titanic_Data_StdFareValues.select("StdFareValue")
        .filter(titanic_Data_StdFareValues("StdFareValue") >
          upper_threshold_std)
    val fareValues_LessThanLowerthrshold_std =
      titanic_Data_StdFareValues.select("StdFareValue")
        .filter(titanic_Data_StdFareValues("StdFareValue") < lower_threshold_std)

    val summary_FareValuesMoreThanUppr_Std =
      fareValues_MoreThanUpperthrshold_std.describe()
    println("Summary Of Standardized Fare Values Greater Than Upper Threshold")
      summary_FareValuesMoreThanUppr_Std.show()
      val summary_FareValuesLessThanLowr_Std =
      fareValues_LessThanLowerthrshold_std.describe()
      println("Summary Of Standardized Fare Values Less Than Lower Threshold")
      summary_FareValuesLessThanLowr_Std.show()


    // Mean of Fare variable
    val fare_Details_Df = titanic_data.select("Fare")
    val fare_DetailsRdd = fare_Details_Df.map{row => row.getDouble(0)}
    val countOfFare = fare_DetailsRdd.count()
    val sortedFare_Rdd = fare_DetailsRdd.sortBy(fareVal => fareVal )
    val sortedFareRdd_WithIndex = sortedFare_Rdd.zipWithIndex()

    val median_Fare = if(countOfFare%2 ==1)

      sortedFareRdd_WithIndex.filter{case(fareVal:Double, index:Long) => index == (countOfFare-1)/2}.first._1
    else{
      val elementAtFirstIndex = sortedFareRdd_WithIndex.filter{case(fareVal:Double, index:Long) => index == (countOfFare/2)-1}.first._1
      val elementAtSecondIndex = sortedFareRdd_WithIndex.filter{case(fareVal:Double, index:Long) => index == (countOfFare/2)}.first._1
      (elementAtFirstIndex+elementAtSecondIndex)/2.0
    }
    // Apply MAD for outlier detection
    val sqlFunc = udf(coder)
    val fare_Details_WithAbsDeviations = fare_Details_Df.withColumn("AbsDev_FromMedian",sqlFunc(col("Fare"), lit(median_Fare)))
    val fare_AbsDevs_Rdd = fare_Details_WithAbsDeviations.map{row =>
      row.getDouble(1)}
    val count = fare_AbsDevs_Rdd.count()
    val sortedFareAbsDev_Rdd = fare_AbsDevs_Rdd.sortBy(fareAbsVal =>
      fareAbsVal )
    val sortedFare_AbsDevRdd_WithIndex =
      sortedFareAbsDev_Rdd.zipWithIndex()
    val median_AbsFareDevs = if(count%2 ==1)
      sortedFare_AbsDevRdd_WithIndex.filter{case(fareAbsVal:Double,
      index:Long) =>
        index == (count-1)/2}.first._1
    else{
      val elementAtFirstIndex =
        sortedFare_AbsDevRdd_WithIndex.filter{case(fareAbsVal:Double,
        index:Long) =>
          index == (count/2)-1}.first._1
      val elementAtSecondIndex =
        sortedFare_AbsDevRdd_WithIndex.filter{case(fareAbsVal:Double,
        index:Long) =>
          index == (count/2)}.first._1
      (elementAtFirstIndex+elementAtSecondIndex)/2.0
    }
    val mad = 1.4826*median_AbsFareDevs
    println("Median Absolute Deviation is:"+mad)

    // Outlier based on MAD
    val upper_mad = median_Fare + 3 * mad
    val lower_mad = median_Fare - 3 * mad
    val fareValues_MoreThanUpperthrshold_mad=
      titanic_data.select("Fare").filter(titanic_data("Fare") >
        upper_mad)
    val fareValues_LessThanLowerthrshold_mad =
      titanic_data.select("Fare").filter(titanic_data("Fare") <
        lower_mad)
    val summary_FareValuesMoreThanUppr_MAD =
      fareValues_MoreThanUpperthrshold_mad.describe()
    println("Summary Of Fare Values Greater Than Upper Threshold In MAD Approach:")
    summary_FareValuesMoreThanUppr_MAD.show()
    val summary_FareValuesLessThanLowr_MAD =
      fareValues_LessThanLowerthrshold_mad.describe()
    println("Summary Of Fare Values Less Than Lower Threshold In MAD Approach:")
    summary_FareValuesLessThanLowr_MAD.show()

  }

  //UDF Code
  val coder= (fareValue:Double, medianValue:Double) =>
    if((fareValue-medianValue)
      < 0) -1*(fareValue-medianValue)
    else (fareValue-medianValue)
}