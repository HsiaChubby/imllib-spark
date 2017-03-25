import com.intel.imllib.lr.LogisticRegressionWithAda
import com.intel.imllib.optimization.{AdagradUpdater, AdamUpdater}
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.regression._
import org.apache.spark.mllib.util.MLUtils



object LRWithAdaExample extends App {

  override def main(args: Array[String]): Unit = {

    val sc = new SparkContext(new SparkConf().setAppName("TESTADAOPTIMIZER").setMaster("local[4]"))
    val training = MLUtils.loadLibSVMFile(sc, "data/lr/a9a").repartition(4)
    val testing = MLUtils.loadLibSVMFile(sc, "data/lr/a9a.t")
    val lr = new LogisticRegressionWithAda().setIntercept(false)
    Array(new AdagradUpdater, new AdamUpdater).foreach{ updater =>
      lr.optimizer
        .setRegParam(0.0)
        .setNumIterations(10)
        .setConvergenceTol(0.0005)
        .setUpdater(updater)
        .setStepSize(0.1)

      val currentTime = System.currentTimeMillis()
      val model = lr.run(training)
      val elapsedTime = System.currentTimeMillis() - currentTime
      // Compute raw scores on the test set.
      val predictionAndLabels = testing.map { case LabeledPoint(label, features) =>
        val prediction = model.predict(features)
        (prediction, label)
      }
      // Get evaluation metrics.
      val metrics = new MulticlassMetrics(predictionAndLabels)
      val accuracy = metrics.accuracy
      println(s"Accuracy = $accuracy, time elapsed: $elapsedTime millisecond.")
    }
    training.unpersist()
    sc.stop()
  }
}
