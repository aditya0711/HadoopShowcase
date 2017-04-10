package analysis

/**
 * @author training
 */
import org.apache.spark._
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.DataFrameNaFunctions
import org.apache.spark.sql.types._
import org.apache.spark.mllib.linalg.{Matrices, Vectors}
import org.apache.spark.mllib.stat.
{MultivariateStatisticalSummary, Statistics}

object Bivariate_Analysis {
  
  def main(args:Array[String]): Unit = {
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("Bivariate_Analysis")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    //Loading data
    val titanic_data =
      sqlContext.read.format("com.databricks.spark.csv")
        .option("header", "true")
        .option("inferSchema", "true")
        .load("/home/training/git/HadoopShowcase/SparkAnalysisExamples/data/train.csv")
    //Correlation and Covariance between Age and Fare
    val correlated_value_AgeFare = titanic_data.stat.corr("Age",
      "Fare")
    val covariance_AgeFare = titanic_data.stat.cov("Age","Fare")
    println("Correlation Value For Age and Fare: "+correlated_value_AgeFare)
    println("Covariance For Age and Fare: "+ covariance_AgeFare)
    //Correlation and Covariance between Pclass and Fare
    val correlated_value_PclassFare =
      titanic_data.stat.corr("Pclass", "Fare")
    val covariance_PclassFare =
      titanic_data.stat.cov("Pclass","Fare")
    println("Correlation Value For Pclass and Fare: "+correlated_value_PclassFare)
    println("Covariance For Pclass and Fare: "+
      covariance_PclassFare)

    // Creating two-way table between Pclass and Sex variables
    println("Frequency distribution of Pclass against variable Sex:")
    val twoWayTable_PclassSex = titanic_data.stat.crosstab("Pclass", "Sex")
    twoWayTable_PclassSex.show()
    // Creating two-way table between Sex and Embarked variables
    println("Frequency distribution of Sex variable against Embarked:")
    titanic_data.stat.crosstab("Sex","Embarked").show()

    val PclassSex_Array = twoWayTable_PclassSex
      .drop("Pclass_Sex")
      .collect()
      .map{row => val female = row.getLong(0).toDouble
        val male = row.getLong(1).toDouble
        (female,male)}
    val femaleValues = PclassSex_Array.map{case(female, male) =>
      female}
    val maleValues = PclassSex_Array.map{case(female, male) => male}
    val goodnessOfFitTestResult =
      Statistics.chiSqTest(Matrices.dense(
        twoWayTable_PclassSex.count().toInt,
        twoWayTable_PclassSex.columns.length-1,
        femaleValues ++ maleValues ))
    println("Chi Square Test Value: "+goodnessOfFitTestResult)

    // Analysis between categorical and continuous variables
    titanic_data.groupBy("Pclass").agg(sum("Fare"), count("Fare"),
      max("Fare"), min("Fare"), stddev("Fare") ).show()
  }
}