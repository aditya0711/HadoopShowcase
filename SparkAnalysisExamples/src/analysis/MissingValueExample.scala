package analysis

/**
 * @author training
 */

import org.apache.spark._
import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.DataFrameNaFunctions
import org.apache.spark.sql.types._
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.stat.
{MultivariateStatisticalSummary, Statistics}

object MissingValueExample {
  
  def main(args:Array[String]): Unit = {
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("MissingValue_Treatment")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    //Loading data
    val loan_Data = sqlContext.read.format ("com.databricks.spark.csv")
      .option("header", "true")
      .option("inferSchema", "true")
      .load("hdfs://localhost:8020/user/training/Loan_Prediction_Data.csv")

    val summary = loan_Data.describe()
    summary.show()

    val newDf_afterDroppedRows =
      loan_Data.na.drop(Seq("LoanAmount",
        "Loan_Amount_Term", "Credit_History"))
    println("Total Rows Count after Deleting null value records: "+newDf_afterDroppedRows.count())

    val schemaString = "Loan_ID,Gender,Married,Dependents,Education, Self_Employed,ApplicantIncome, CoapplicantIncome,LoanAmount,Loan_Amount_Term, Credit_History,Property_Area,Loan_Status"
      val schema = schemaString.split(",").map{
      field =>
      if(field == "ApplicantIncome" || field == "CoapplicantIncome" ||
      field == "LoanAmount" || field == "Loan_Amount_Term" || field ==
      "Credit_History")
      StructField(field, DoubleType)
      else
        StructField(field, StringType)
      }
    val schema_Applied = StructType(schema)

      /* Fill missing values (null or NaN) with a
      specific value for all columns */
      val filledWith_half = loan_Data.na.fill(0.5)
      /* Fill missing values (null or NaN) with a specific
      value for certain columns */
      val filledWith_halfFewColumns = loan_Data.na.fill(0.5,
      Seq("Credit_History"))
      /* Fill missing values of each column with specified value */
      val fill_FewColumns = loan_Data.na.fill(
      Map(
      "ApplicantIncome" -> 1000.0,
      "LoanAmount" -> 500.0,
      "Credit_History" -> 0.5
      ) )

    val df_CreditHistoryNull =
      loan_Data.filter(loan_Data("Credit_History").isNull)
    println("Missing rows for Credit_History: "+df_CreditHistoryNull.count)
    val mean_CreditHist = loan_Data.select(mean("Credit_History"))
      .first()(0).asInstanceOf[Double]
    val fill_MissingValues_CrediHist =
      loan_Data.na.fill(mean_CreditHist,Seq("Credit_History"))
  }
  
}