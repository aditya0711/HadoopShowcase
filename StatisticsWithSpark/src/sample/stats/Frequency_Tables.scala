package sample.stats

/**
 * @author training
 */
import org.apache.spark._
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.types.{
  StructType,
  StringType,
  DoubleType,
  StructField
}
object Frequency_Tables {
  def main(args: Array[String]): Unit = {
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("Frequency_Tables")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    val schemaString =
      "Loan_ID,Gender,Married,Dependents,Education,Self_Employed,ApplicantIncome,CoapplicantIncome,LoanAmount,Loan_Amount_Term,Credit_History,Property_Area,Loan_Status"
    val schema = schemaString.split(",").map {
      field =>
        if (field == "ApplicantIncome" || field ==
          "CoapplicantIncome" || field == "LoanAmount" || field ==
          "Loan_Amount_Term" || field == "Credit_History")
          StructField(field, DoubleType)
        else
          StructField(field, StringType)
    }
    val schema_Applied = StructType(schema)
    val loan_Data =
      sqlContext.read.format("com.databricks.spark.csv")
        .option("header", "true")
        .schema(schema_Applied)
        .load("hdfs://localhost:8020/user/training/Loan_Prediction_Data.csv")
    val crossTab_Df = loan_Data.stat.crosstab("Credit_History",
      "Loan_Status")
    crossTab_Df.show()
  }
}