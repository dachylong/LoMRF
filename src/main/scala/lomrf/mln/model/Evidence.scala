/*
 * o                        o     o   o         o
 * |             o          |     |\ /|         | /
 * |    o-o o--o    o-o  oo |     | O |  oo o-o OO   o-o o   o
 * |    | | |  | | |    | | |     |   | | | |   | \  | |  \ /
 * O---oo-o o--O |  o-o o-o-o     o   o o-o-o   o  o o-o   o
 *             |
 *          o--o
 * o--o              o               o--o       o    o
 * |   |             |               |    o     |    |
 * O-Oo   oo o-o   o-O o-o o-O-o     O-o    o-o |  o-O o-o
 * |  \  | | |  | |  | | | | | |     |    | |-' | |  |  \
 * o   o o-o-o  o  o-o o-o o o o     o    | o-o o  o-o o-o
 *
 * Logical Markov Random Fields.
 *
 * Copyright (C) 2012  Anastasios Skarlatidis.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package lomrf.mln.model

import java.io.{File, BufferedReader, FileReader}
import lomrf.logic._
import lomrf.util._
import scala.collection
import scala.collection.{mutable, breakOut}
import scala.Some

/**
 * @author Anastasios Skarlatidis
 */

private[model] class Evidence(
                               val constants: Map[String, ConstantsSet],
                               val atomsEvDB: Map[AtomSignature, AtomEvidenceDB],
                               val functionMappers: Map[AtomSignature, FunctionMapper],
                               val identities: Map[AtomSignature, AtomIdentityFunction],
                               val orderedStartIDs: Array[Int],
                               val orderedAtomSignatures: Array[AtomSignature],
                               val queryStartID: Int,
                               val queryEndID: Int)



private[model] object Evidence extends Logging {

  def apply(kb: KB, queryPredicates: collection.Set[AtomSignature], hiddenPredicates: collection.Set[AtomSignature], filenames: List[String]): Evidence = {
    load(kb, queryPredicates, hiddenPredicates, if(filenames.isEmpty) List(emptyDBFile) else filenames.map(filename => new File(filename)))
  }

  def apply(kb: KB, queryPredicates: collection.Set[AtomSignature], hiddenPredicates: collection.Set[AtomSignature], filename: String): Evidence = {
    val evidenceFile: File = if ((filename eq null) || (filename.trim == "")) this.emptyDBFile else new File(filename)
    load(kb, queryPredicates, hiddenPredicates, List(evidenceFile))
  }


  def load(kb: KB, queryPredicates: collection.Set[AtomSignature], hiddenPredicates: collection.Set[AtomSignature], files: Iterable[File]): Evidence = {
    load(kb.predicateSchema, kb.functionSchema, kb.constants, queryPredicates, hiddenPredicates, files)
  }

  def load(schema: Map[AtomSignature, Seq[String]],
           functionSchema: Map[AtomSignature, (String, Seq[String])],
           kb_constants: mutable.HashMap[String, ConstantsSetBuilder],
           queryPredicates: collection.Set[AtomSignature],
           hiddenPredicates: collection.Set[AtomSignature],
           files: Iterable[File]): Evidence = {

    val constantMap = kb_constants

    val evidenceParser = new ProbEvidenceParser
    val evidenceExpressionsDB =
    for (file <- files; fileReader = new BufferedReader(new FileReader(file)))
      yield evidenceParser.parseAll(evidenceParser.evidence, fileReader) match {
        case evidenceParser.Success(expr, _) =>  expr
        case x => fatal("Can't parse the following expression: " + x+" in file: "+file.getPath)
      }

    info("Stage 1: Parsing constants")
    for (evidenceExpressions <- evidenceExpressionsDB; expr <- evidenceExpressions) expr match {
      case f: FunctionMapping =>
        //Collect information for functionMappings
        functionSchema.get(f.signature) match {
          case Some(fSchema) =>
            val (returnType, argTypes) = fSchema
            constantMap.get(returnType) match {
              case Some(builder) => builder += f.retValue
              case None => error("Type " + returnType + " in function " + f.signature + " is not defined.")
            }
            for ((argType, argValue) <- argTypes.zip(f.values)) {
              constantMap.get(argType) match {
                case Some(builder) => builder += argValue
                case None => error("Type " + argType + " in function " + f.signature + " is not defined.")
              }
            }
          case None => fatal("The function definition of " + f.signature + " does not appear in the KB.")
        }
      case atom: EvidenceAtom =>
        schema.get(atom.signature) match {
          case Some(argTypes) =>
            //append its constants into constantMap
            for ((value, index) <- atom.constants.view.zipWithIndex) {
              val constantType = argTypes(index)
              constantMap.get(constantType) match {
                case Some(x) => x += value.symbol
                case None =>
                  val currMap = new ConstantsSetBuilder()
                  currMap += value.symbol
                  constantMap(constantType) = currMap
              }
            }
          case _ => fatal("The type of " + atom + " is not defined")
        }
      case _ => //ignore
    }

    val constants: Map[String, ConstantsSet] = (for ((symbol, builder) <- constantMap) yield symbol -> builder.result)(breakOut)

    //constants.foreach(entry => println("|"+entry._1+"|="+entry._2.size))


    val functionMapperBuilders = mutable.HashMap[AtomSignature, FunctionMapperBuilder]()
    val atomsEvDBBuilders = mutable.HashMap[AtomSignature, AtomEvidenceDBBuilder]()

    info("Stage 2: Creating atom unique identity functions.")
    var identities: Map[AtomSignature, AtomIdentityFunction] = Map[AtomSignature, AtomIdentityFunction]()

    var currentID = 1
    val orderedStartIDs = new Array[Int](schema.size)
    val orderedAtomSignatures = new Array[AtomSignature](schema.size)
    var index = 0


    // Query predicates
    for ((signature, atomSchema) <- schema; if queryPredicates.contains(signature)) {
      orderedStartIDs(index) = currentID
      orderedAtomSignatures(index) = signature
      index += 1
      val idFunction = AtomIdentityFunction(signature, atomSchema, constants, currentID)
      currentID += idFunction.length + 1
      identities += (signature -> idFunction)
      //println(signature + " {[" + idFunction.startID + "," + (idFunction.length + idFunction.startID) + "], length:" + idFunction.length + "}")
    }

    val queryStartID = 1
    val queryEndID = currentID - 1


    // Other OWA predicates
    for ((signature, atomSchema) <- schema; if hiddenPredicates.contains(signature)) {
      orderedStartIDs(index) = currentID
      orderedAtomSignatures(index) = signature
      index += 1
      val idFunction = AtomIdentityFunction(signature, atomSchema, constants, currentID)
      currentID += idFunction.length + 1
      identities += (signature -> idFunction)
      //println(signature + " {[" + idFunction.startID + "," + (idFunction.length + idFunction.startID) + "], length:" + idFunction.length + "}")
    }

    val predicatesOWA = queryPredicates ++ hiddenPredicates

    // CWA predicates (Evidence predicates)
    for ((signature, atomSchema) <- schema; if !predicatesOWA.contains(signature)) {
      orderedStartIDs(index) = currentID
      orderedAtomSignatures(index) = signature
      index += 1
      val idFunction = AtomIdentityFunction(signature, atomSchema, constants, currentID)
      currentID += idFunction.length + 1
      identities += (signature -> idFunction)
      //println(signature + " {[" + idFunction.startID + "," + (idFunction.length + idFunction.startID) + "], length:" + idFunction.length + "}")
    }


    //orderedAtomSignatures.zip(orderedStartIDs).foreach{case (sig, startid) => println(sig+" -> "+startid) }
    //sys.exit(0)

    info("Stage 3: Creating function mappings, and evidence atoms database.")

    //var currentAtomStartID = 1
    for (evidenceExpressions <- evidenceExpressionsDB; expr <- evidenceExpressions) expr match {
      case fm: FunctionMapping =>
        functionMapperBuilders.get(fm.signature) match {
          case Some(fMappingBuilder) => fMappingBuilder +=(fm.values, fm.retValue)
          case None =>
            val idFunction = AtomIdentityFunction(fm.signature, functionSchema(fm.signature)._2, constants, 1)
            val builder = new FunctionMapperBuilder(idFunction)
            builder +=(fm.values, fm.retValue)
            functionMapperBuilders += (fm.signature -> builder)
        }
      case atom: EvidenceAtom =>
        atomsEvDBBuilders.get(atom.signature) match {
          case Some(builder) => builder += atom
          case None =>
            val signature = atom.signature
            val atomSchema = schema(signature)
            val db = AtomEvidenceDBBuilder(signature, atomSchema, identities(signature), !predicatesOWA.contains(signature))
            db += atom
            atomsEvDBBuilders += (signature -> db)
        }
      case _ => //ignore
    }

    val atomsEvDB: Map[AtomSignature, AtomEvidenceDB] = (for ((signature, builder) <- atomsEvDBBuilders) yield signature -> builder.toAtomEvidenceDB)(breakOut)
    val functionMappers: Map[AtomSignature, FunctionMapper] = (for ((signature, builder) <- functionMapperBuilders) yield signature -> builder.result)(breakOut)

    new Evidence(constants, atomsEvDB, functionMappers, identities, orderedStartIDs, orderedAtomSignatures, queryStartID, queryEndID)
  }

  private def emptyDBFile: File = {
    val tmpfile = File.createTempFile("mlnc_empty_" + System.currentTimeMillis(), ".db")
    tmpfile.createNewFile()
    tmpfile.deleteOnExit()
    tmpfile
  }

}