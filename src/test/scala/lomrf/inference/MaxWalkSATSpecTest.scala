package lomrf.inference

import java.io.{FileOutputStream, PrintStream}
import lomrf.logic.AtomSignature
import lomrf.mln.grounding.MRFBuilder
import lomrf.mln.inference.MaxWalkSAT
import lomrf.mln.model.MLN
import org.scalatest.{Matchers, FunSpec}
import scala.collection.immutable.HashMap
import scala.io.Source
import lomrf.util.Utilities.io.{findFiles, strToFile}

/**
 * Specification test for MaxWalkSAT algorithm used for MAP inference.
 *
 * @author Anastasios Skarlatidis
 * @author Vagelis Michelioudakis
 */
final class MaxWalkSATSpecTest extends FunSpec with Matchers {

  private val sep = System.getProperty("file.separator")
  private val testFilesPath = System.getProperty("user.dir") + sep + "Examples" + sep + "data" + sep +
                              "tests" + sep + "inference" + sep

  private val mlnFiles = findFiles(strToFile(testFilesPath), f => f.getName.contains(".mln"))
  private val dbFilesList = findFiles(strToFile(testFilesPath), f => f.getName.contains(".db"))
  private val goldenFilesList = findFiles(strToFile(testFilesPath), f => f.getName.contains(".mws.golden"))

  describe("Caviar diagonal newton test in path: '" + testFilesPath + "'") {

    for(weightType <- List("HI", "SI", "SI_h")) {
      for (fold <- 0 to 9) {
        val mlnFile = mlnFiles.filter(f => f.getAbsolutePath.contains("fold_" + fold) &&
                                      f.getAbsolutePath.contains(sep + weightType + sep))
        val dbFiles = dbFilesList.filter(f => f.getAbsolutePath.contains("fold_" + fold) &&
                                     f.getAbsolutePath.contains(sep + weightType + sep))
        val goldenFiles = goldenFilesList.filter(f => f.getAbsolutePath.contains("fold_" + fold) &&
                                                 f.getAbsolutePath.contains(sep + weightType + sep))

        for(db <- dbFiles) {
          describe("MLN from file '" + mlnFile(0) + "' with evidence from file '" + db) {
            val mln = MLN(
              mlnFileName = mlnFile(0).getAbsolutePath,
              evidenceFileName = db.getAbsolutePath,
              queryAtoms = Set(AtomSignature("HoldsAt", 2)),
              cwa = Set(AtomSignature("Happens", 2), AtomSignature("Close", 4), AtomSignature("Next", 2),
                AtomSignature("OrientationMove", 3), AtomSignature("StartTime", 1)))

            info("Found " + mln.formulas.size + " formulas")
            info("Found " + mln.constants.size + " constant types")
            info("Found " + mln.schema.size + " predicate schemas")
            info("Found " + mln.functionSchema.size + " function schemas")

            it("should contain 25 formulas") {
              mln.formulas.size should be(25)
            }

            it("should constants 5 constants sets (domains)") {
              mln.constants.size should be(5)
            }

            it("should contain 6 predicate schemas") {
              mln.schema.size should be(6)
            }

            it("should contain 7 function schemas") {
              mln.functionSchema.size should be(7)
            }

            describe("Creating MRF from previous MLN") {

              info("Creating MRF...")
              val mrfBuilder = new MRFBuilder(mln)
              val mrf = mrfBuilder.buildNetwork

              info("Created " + mrf.numberOfAtoms + " ground atoms")
              info("Created " + mrf.numberOfConstraints + " ground clauses")

              describe("Running MAP inference using MaxWalkSAT") {

                val prefix = db.getName.split(".db")(0)
                val golden = goldenFiles.find(f => f.getName.contains(prefix)).get

                val resultsWriter = new PrintStream(
                                    new FileOutputStream(
                                    mlnFile(0).getParent.getAbsolutePath + sep + prefix + ".mws.result"), true)

                val solver = new MaxWalkSAT(mrf)
                solver.infer()
                solver.writeResults(resultsWriter)

                var results = HashMap[String, Int]()
                for (line <- Source.fromFile(mlnFile(0).getParent.getAbsolutePath + sep + prefix + ".mws.result").getLines()) {
                  val element = line.split(" ")
                  results += ((element(0), element(1).toInt))
                }

                var standard = HashMap[String, Int]()
                for (line <- Source.fromFile(golden.getAbsolutePath).getLines()) {
                  val element = line.split(" ")
                  standard += ((element(0), element(1).toInt))
                }

                it("should be identical to the golden standard") {
                  assert(results == standard)
                }

              }
            }
          }

        }

      }
    }
  }
}