import org.apache.spark.sql.SparkSession
import org.apache.spark.storage.StorageLevel

object VehicleDelay2 {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("Spark basic example")
      .config("spark.master", "local")
      .getOrCreate()

    //----------------------------------------------------------------//
    //Read vehicle csv file and generating dataframe                  //
    //----------------------------------------------------------------//
    val C00df = spark.read.format("csv").option("header", "true").csv("C:/Users/z011348/Desktop/Spark/VehInput/C00.csv")
    val P38df = spark.read.format("csv").option("header", "true").csv("C:/Users/z011348/Desktop/Spark/VehInput/P38.csv")
    val P33df = spark.read.format("csv").option("header", "true").csv("C:/Users/z011348/Desktop/Spark/VehInput/P33.csv")
    val P46df = spark.read.format("csv").option("header", "true").csv("C:/Users/z011348/Desktop/Spark/VehInput/P46.csv")
    val P35df = spark.read.format("csv").option("header", "true").csv("C:/Users/z011348/Desktop/Spark/VehInput/P35.csv")
    val P50df = spark.read.format("csv").option("header", "true").csv("C:/Users/z011348/Desktop/Spark/VehInput/P50.csv")
    val G0Udf = spark.read.format("csv").option("header", "true").csv("C:/Users/z011348/Desktop/Spark/VehInput/G0U.csv")

    // Join
    val jn1 = C00df.join(P38df, C00df("INVARBCV") === P38df("INVARBCV"), "inner")
      .drop(P38df("INVARBCV"))

    val jn2 = jn1.join(P33df, jn1("INVARBCV") === P33df("INVARBCV"), "inner")
      .drop(P33df("INVARBCV"))

    val jn3 = jn2.join(P46df, jn2("INVARBCV") === P46df("INVARBCV"), "inner")
      .drop(P46df("INVARBCV"))

    val jn4 = jn3.join(P35df, jn3("INVARBCV") === P35df("INVARBCV"), "inner")
      .drop(P35df("INVARBCV"))

    val jn5 = jn4.join(P50df, jn4("INVARBCV") === P50df("INVARBCV"), "inner")
      .drop(P50df("INVARBCV"))

    val jn6 = jn5.join(G0Udf, jn5("INVARBCV") === G0Udf("INVARBCV"), "inner")
      .drop(G0Udf("INVARBCV"))

    jn6.createOrReplaceTempView("tempvehic")

    val tempvehic = spark.sql("select \nINVARBCV, \nPLANT,\nDEALER,\nMODELE_C, \nBRAND, \nC00, \nC00DATE, \nP38, \nP38DATE, \nP33, \nP33DATE,\nP46, \nP46DATE,\nP35, \nP35DATE,\nP50, \nP50DATE,\nG0U, \nG0UDATE,\ndatediff(P38DATE,C00DATE) as Till_flexi,\ndatediff(P33DATE,P38DATE) as Till_flexi_end,\ndatediff(P46DATE,P33DATE) as Till_admin_launch,\ndatediff(P35DATE,P46DATE) as Till_veh_starts,\ndatediff(P50DATE,P35DATE) as Till_veh_build,\ndatediff(G0UDATE,P50DATE) as Till_plant_exit\nFROM tempvehic").toDF()

    val plantAVG = tempvehic.groupBy("PLANT")
      .mean("Till_flexi", "Till_flexi_end", "Till_admin_launch", "Till_veh_starts", "Till_veh_build", "Till_plant_exit")
      .toDF("PLANT", "Till_flexi", "Till_flexi_end","Till_admin_launch", "Till_veh_starts", "Till_veh_build", "Till_plant_exit")

    val modelAVG = tempvehic.groupBy("MODELE_C")
      .mean("Till_flexi", "Till_flexi_end", "Till_admin_launch", "Till_veh_starts", "Till_veh_build", "Till_plant_exit")
      .toDF("PLANT", "Till_flexi", "Till_flexi_end","Till_admin_launch", "Till_veh_starts", "Till_veh_build", "Till_plant_exit")

    //================= identify a more delay ============//

    val vehDF = tempvehic.toDF()

    import org.apache.spark.sql.functions.when

    val delayAT = tempvehic.withColumn("more_delay_at",
      when(vehDF("Till_flexi") > vehDF("Till_flexi_end") &&
        vehDF("Till_flexi") > vehDF("Till_admin_launch") &&
        vehDF("Till_flexi") > vehDF("Till_veh_starts") &&
        vehDF("Till_flexi") > vehDF("Till_veh_build") &&
        vehDF("Till_flexi") > vehDF("Till_plant_exit"), "flexi")
        .otherwise(when(vehDF("Till_flexi_end") > vehDF("Till_flexi") &&
          vehDF("Till_flexi_end") > vehDF("Till_admin_launch") &&
          vehDF("Till_flexi_end") > vehDF("Till_veh_starts") &&
          vehDF("Till_flexi_end") > vehDF("Till_veh_build") &&
          vehDF("Till_flexi_end") > vehDF("Till_plant_exit"), "flexi_end")
          .otherwise(when(vehDF("Till_admin_launch") > vehDF("Till_flexi") &&
            vehDF("Till_admin_launch") > vehDF("Till_flexi_end") &&
            vehDF("Till_admin_launch") > vehDF("Till_veh_starts") &&
            vehDF("Till_admin_launch") > vehDF("Till_veh_build") &&
            vehDF("Till_admin_launch") > vehDF("Till_plant_exit"), "admin_launch")
            .otherwise(when(vehDF("Till_veh_starts") > vehDF("Till_flexi") &&
              vehDF("Till_veh_starts") > vehDF("Till_flexi_end") &&
              vehDF("Till_veh_starts") > vehDF("Till_admin_launch") &&
              vehDF("Till_veh_starts") > vehDF("Till_veh_build") &&
              vehDF("Till_veh_starts") > vehDF("Till_plant_exit"), "veh_starts")
              .otherwise(when(vehDF("Till_veh_build") > vehDF("Till_flexi") &&
                vehDF("Till_veh_build") > vehDF("Till_flexi_end") &&
                vehDF("Till_veh_build") > vehDF("Till_admin_launch") &&
                vehDF("Till_veh_build") > vehDF("Till_veh_starts") &&
                vehDF("Till_veh_build") > vehDF("Till_plant_exit"), "veh_build")
                .otherwise(when(vehDF("Till_plant_exit") > vehDF("Till_flexi") &&
                  vehDF("Till_plant_exit") > vehDF("Till_flexi_end") &&
                  vehDF("Till_plant_exit") > vehDF("Till_admin_launch") &&
                  vehDF("Till_plant_exit") > vehDF("Till_veh_starts") &&
                  vehDF("Till_plant_exit") > vehDF("Till_veh_build"), "plant_exit")
                  .otherwise("Equal delay at few phases")))))))

    delayAT.persist(StorageLevel.MEMORY_AND_DISK) /* Store in memory and disk */

    delayAT.write.format("csv").option("header", "true")
      .save("C:/Users/z011348/Desktop/Spark/VehOutput/vehicleDelay")

    //----------------------------------------------------//
    //=================   Save parquet files =================//
    tempvehic.write.format("parquet")
      .save("C:/Users/z011348/Desktop/Spark/VehOutput/Vehicledata")

    modelAVG.write.format("parquet")
      .save("C:/Users/z011348/Desktop/Spark/VehOutput/Avgmod")

    plantAVG.write.format("parquet")
      .save("C:/Users/z011348/Desktop/Spark/VehOutput/Avgplant")
    //====================================================//

    spark.stop()

  }

}
