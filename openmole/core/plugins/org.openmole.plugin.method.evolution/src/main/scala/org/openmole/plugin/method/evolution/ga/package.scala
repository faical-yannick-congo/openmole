/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.plugin.method.evolution

import fr.iscpif.mgo._
import org.openmole.core.implementation.mole._
import org.openmole.core.implementation.puzzle._
import org.openmole.core.implementation.task._
import org.openmole.core.implementation.transition._
import org.openmole.core.implementation.data._
import org.openmole.core.model.data._
import org.openmole.core.model.mole._
import org.openmole.core.model.task._
import org.openmole.core.model.transition._
import org.openmole.plugin.method.evolution.algorithm._
import org.openmole.plugin.task.tools._

import scala.util.Random

package object ga {

  implicit def seqOfTuplesToInputsConversion[T](s: Seq[(Prototype[Double], (T, T))]) =
    Inputs[T](s.map { case (p, (min, max)) ⇒ Scalar[T](p, min, max) })

  implicit def seqOfTuplesStringToInputsDoubleConversion(s: Seq[(Prototype[Double], (Double, Double))]) =
    Inputs[String](s.map { case (p, (min, max)) ⇒ Scalar[String](p, min.toString, max.toString) })

  implicit def seqToInputsConversion[T](s: Seq[Input[T]]) = Inputs[T](s)

  trait GAPuzzle[+ALG <: GAAlgorithm] {
    val evolution: ALG

    def archive: Prototype[evolution.A]
    def genome: Prototype[evolution.G]
    def individual: Prototype[Individual[evolution.G, evolution.P, evolution.F]]
    def generation: Prototype[Int]
  }

  private def components[ALG <: GAAlgorithm](
    name: String,
    evolution: ALG)(implicit plugins: PluginSet) = new { components ⇒
    import evolution._

    val genome = Prototype[evolution.G](name + "Genome")(gManifest)
    val individual = Prototype[Individual[evolution.G, evolution.P, evolution.F]](name + "Individual")
    val newIndividual = Prototype[Individual[evolution.G, evolution.P, evolution.F]](name + "NewIndividual")
    //val population = Prototype[Population[evolution.G, evolution.F, evolution.MF]](name + "Population")
    val archive = Prototype[evolution.A](name + "Archive")
    //val newArchive = Prototype[evolution.A](name + "NewArchive")
    val state = Prototype[evolution.STATE](name + "State")
    val fitness = Prototype[evolution.F](name + "Fitness")
    val generation = Prototype[Int](name + "Generation")
    val terminated = Prototype[Boolean](name + "Terminated")

    val firstTask = EmptyTask(name + "First")
    firstTask addInput (Data(archive, Optional))
    firstTask addInput (Data(individual.toArray, Optional))
    firstTask addOutput (Data(archive, Optional))
    firstTask addOutput (Data(individual.toArray, Optional))

    val scalingGenomeTask = ScalingGAGenomeTask(evolution)(name + "ScalingGenome", genome)

    val toIndividualTask = ToIndividualTask(evolution)(name + "ToIndividual", genome, individual)

    val elitismTask =
      ElitismTask(evolution)(
        name + "ElitismTask",
        individual.toArray,
        newIndividual.toArray,
        archive)

    val terminationTask = TerminationTask(evolution)(
      name + "TerminationTask",
      individual.toArray,
      archive,
      generation,
      state,
      terminated)

    val scalingIndividualsTask = ScalingGAIndividualsTask(evolution)(name + "ScalingIndividuals", individual.toArray)

    scalingIndividualsTask addInput state
    scalingIndividualsTask addInput generation
    scalingIndividualsTask addInput terminated
    scalingIndividualsTask addInput archive

    scalingIndividualsTask addOutput state
    scalingIndividualsTask addOutput generation
    scalingIndividualsTask addOutput terminated
    scalingIndividualsTask addOutput individual.toArray
    scalingIndividualsTask addOutput archive

    val renameIndividualsTask = AssignTask(name + "RenameIndividuals")
    renameIndividualsTask.assign(individual.toArray, newIndividual.toArray)

    val terminatedCondition = Condition(terminated.name + " == true")

    val _evolution = evolution
    val _inputs = inputs
    val _objectives = objectives

    def puzzle(puzzle: Puzzle, _output: ICapsule) =
      new Puzzle(puzzle) with GAPuzzle[ALG] {
        val evolution = _evolution

        val archive = components.archive.asInstanceOf[Prototype[evolution.A]]
        val genome = components.genome.asInstanceOf[Prototype[evolution.G]]
        val individual = components.individual.asInstanceOf[Prototype[Individual[evolution.G, evolution.P, evolution.F]]]

        def output = _output
        def state = components.state
        def generation = components.generation
      }

  }

  def generationalGA[ALG <: GAAlgorithm](evolution: ALG)(
    name: String,
    model: Puzzle,
    lambda: Int)(implicit plugins: PluginSet) = {

    val cs = components[ALG](name, evolution)
    import cs._

    val firstCapsule = StrainerCapsule(firstTask)

    val breedTask = ExplorationTask(name + "Breed", BreedSampling(evolution)(individual.toArray, archive, genome, lambda))
    breedTask.addParameter(individual.toArray -> Array.empty[Individual[evolution.G, evolution.P, evolution.F]])
    breedTask.addParameter(archive -> evolution.initialArchive)

    breedTask addInput generation
    breedTask addInput state

    breedTask addOutput individual.toArray
    breedTask addOutput archive
    breedTask addOutput generation
    breedTask addOutput state

    breedTask addParameter (generation -> 0)
    breedTask addParameter Parameter.delayed(state, evolution.initialState)

    val breedingCaps = Capsule(breedTask)
    val breedingCapsItSlot = Slot(breedingCaps)

    val scalingGenomeCaps = Capsule(scalingGenomeTask)
    val toIndividualSlot = Slot(InputStrainerCapsule(toIndividualTask))
    val elitismSlot = Slot(elitismTask)

    terminationTask addOutput archive
    terminationTask addOutput individual.toArray

    val terminationSlot = Slot(StrainerCapsule(terminationTask))
    val scalingIndividualsSlot = Slot(Capsule(scalingIndividualsTask))
    val endSlot = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val exploration = firstCapsule -- breedingCaps -< scalingGenomeCaps -- (model, filter = Block(genome)) -- toIndividualSlot >- renameIndividualsTask -- elitismSlot -- terminationSlot -- scalingIndividualsSlot -- (endSlot, terminatedCondition, filter = Keep(individual.toArray))

    val loop = terminationSlot -- (breedingCapsItSlot, !terminatedCondition)

    val dataChannels =
      (scalingGenomeCaps -- (toIndividualSlot, filter = Keep(genome))) +
        (breedingCaps -- (elitismSlot, filter = Keep(individual.toArray, archive))) +
        (breedingCaps oo (model.first, filter = Block(archive, individual.toArray, genome.toArray))) +
        (breedingCaps -- (endSlot, filter = Block(archive, individual.toArray, state, generation, terminated, genome.toArray))) +
        (breedingCaps -- (terminationSlot, filter = Block(archive, individual.toArray, genome.toArray)))

    val gaPuzzle = exploration + loop + dataChannels

    cs.puzzle(gaPuzzle, scalingIndividualsSlot.capsule)
  }

  def steadyGA[ALG <: GAAlgorithm](evolution: ALG)(
    name: String,
    model: Puzzle,
    lambda: Int = 1)(implicit plugins: PluginSet) = {

    val cs = components[ALG](name, evolution)
    import cs._

    val breedTask = ExplorationTask(name + "Breed", BreedSampling(evolution)(individual.toArray, archive, genome, lambda))
    breedTask.addParameter(individual.toArray -> Array.empty[Individual[evolution.G, evolution.P, evolution.F]])
    breedTask.addParameter(archive -> evolution.initialArchive)

    val firstCapsule = StrainerCapsule(firstTask)
    val scalingCaps = Capsule(scalingGenomeTask)

    val toIndividualSlot = Slot(InputStrainerCapsule(toIndividualTask))

    val toIndividualArrayCaps = StrainerCapsule(ToArrayTask(name + "IndividualToArray", individual))

    //mergeArchiveTask addParameter (archive -> evolution.initialArchive)
    //val mergeArchiveCaps = MasterCapsule(mergeArchiveTask, archive)

    elitismTask addParameter (individual.toArray -> Array.empty[Individual[evolution.G, evolution.P, evolution.F]])
    elitismTask addParameter (archive -> evolution.initialArchive)
    val elitismCaps = MasterCapsule(elitismTask, individual.toArray, archive)

    terminationTask addParameter Parameter.delayed(state, evolution.initialState)
    terminationTask addParameter generation -> 0
    terminationTask addOutput archive
    terminationTask addOutput individual.toArray

    val terminationSlot = Slot(MasterCapsule(terminationTask, generation, state))

    val scalingIndividualsSlot = Slot(Capsule(scalingIndividualsTask))

    val steadyBreedingTask = ExplorationTask(name + "Breeding", BreedSampling(evolution)(individual.toArray, archive, genome, 1))
    val steadyBreedingCaps = Capsule(steadyBreedingTask)

    val endCapsule = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val skel =
      firstCapsule --
        breedTask -<
        scalingCaps --
        (model, filter = Block(genome)) --
        (toIndividualSlot, filter = Keep(evolution.objectives.map(_.name).toSeq: _*)) --
        toIndividualArrayCaps --
        renameIndividualsTask --
        elitismCaps --
        terminationSlot --
        scalingIndividualsSlot >| (endCapsule, terminatedCondition, Block(archive))

    val loop =
      scalingIndividualsSlot --
        steadyBreedingCaps -<-
        scalingCaps

    val dataChannels =
      (scalingCaps -- (toIndividualSlot, filter = Keep(genome))) +
        (firstCapsule oo (model.first, filter = Block(archive, individual.toArray))) +
        (firstCapsule -- (endCapsule, filter = Block(archive, individual.toArray))) +
        (firstCapsule oo (elitismCaps, filter = Keep(individual.toArray, archive)))

    val gaPuzzle = skel + loop + dataChannels

    cs.puzzle(gaPuzzle, scalingIndividualsSlot.capsule)
  }

  def islandSteadyGA[ALG <: GAAlgorithm](evolution: ALG, model: Puzzle)(
    name: String,
    number: Int,
    termination: GATermination { type G >: evolution.G; type P >: evolution.P; type F >: evolution.F; type MF >: evolution.MF },
    sampling: Int)(implicit plugins: PluginSet) = {
    val puzzle: Puzzle with GAPuzzle[ALG] =
      steadyGA[ALG](evolution)(
        s"${name}Island",
        model
      )

    islandGA[ALG](puzzle)(
      name,
      number,
      termination.asInstanceOf[GATermination { type G = puzzle.evolution.G; type P = puzzle.evolution.P; type F = puzzle.evolution.F; type MF = puzzle.evolution.MF }],
      sampling)

  }

  def islandGA[AG <: GAAlgorithm](model: Puzzle with GAPuzzle[AG])(
    name: String,
    number: Int,
    termination: GATermination { type G >: model.evolution.G; type P >: model.evolution.P; type F >: model.evolution.F; type MF >: model.evolution.MF },
    sampling: Int)(implicit plugins: PluginSet) = {

    import model.evolution
    import evolution._

    val islandElitism = new Elitism with Termination with Modifier with Archive with TerminationManifest {
      type G = evolution.G
      type P = evolution.P
      type A = evolution.A
      type MF = evolution.MF
      type F = evolution.F

      type STATE = termination.STATE

      implicit val stateManifest = termination.stateManifest

      def initialArchive = evolution.initialArchive
      def archive(a: A, oldIndividuals: Seq[Individual[G, P, F]], offspring: Seq[Individual[G, P, F]]) = evolution.archive(a, oldIndividuals, offspring)
      def modify(individuals: Seq[Individual[G, P, F]], archive: A) = evolution.modify(individuals, archive)
      def elitism(individuals: Seq[Individual[G, P, F]], newIndividuals: Seq[Individual[G, P, F]], archive: A)(implicit rng: Random) = evolution.elitism(individuals, newIndividuals, archive)

      def initialState = termination.initialState
      def terminated(population: ⇒ Population[G, P, F, MF], terminationState: STATE) = termination.terminated(population, terminationState)
    }

    val archive = model.archive.asInstanceOf[Prototype[A]]
    val originalArchive = Prototype[A](name + "OriginalArchive")

    val individual = model.individual.asInstanceOf[Prototype[Individual[G, P, F]]]
    val newIndividual = Prototype[Individual[G, P, F]](name + "NewIndividual")

    val state = Prototype[islandElitism.STATE](name + "State")(islandElitism.stateManifest)
    val generation = Prototype[Int](name + "Generation")
    val terminated = Prototype[Boolean](name + "Terminated")

    val firstCapsule = StrainerCapsule(EmptyTask(name + "First"))

    val renameIndividualsTask = AssignTask(name + "RenameIndividuals")
    renameIndividualsTask.assign(individual.toArray, newIndividual.toArray)

    val elitismTask = ElitismTask(islandElitism)(
      name + "ElitismTask",
      individual.toArray,
      newIndividual.toArray,
      archive)

    elitismTask addParameter (individual.toArray -> Array.empty[Individual[evolution.G, evolution.P, evolution.F]])
    elitismTask addParameter (archive -> islandElitism.initialArchive)
    val elitismCaps = MasterCapsule(elitismTask, individual.toArray, archive)

    val terminationTask = TerminationTask(islandElitism)(
      name + "TerminationTask",
      individual.toArray,
      archive,
      generation,
      state,
      terminated)

    terminationTask addParameter Parameter.delayed(state, islandElitism.initialState)
    terminationTask addParameter (generation -> 0)

    terminationTask addOutput archive
    terminationTask addOutput individual.toArray
    val terminationSlot = Slot(MasterCapsule(terminationTask, generation, state))

    val endCapsule = Slot(StrainerCapsule(EmptyTask(name + "End")))

    val preIslandTask = EmptyTask(name + "PreIsland")
    preIslandTask addInput individual.toArray
    preIslandTask addInput archive
    preIslandTask addOutput individual.toArray
    preIslandTask addOutput archive

    preIslandTask addParameter (individual.toArray -> Array.empty[Individual[evolution.G, evolution.P, evolution.F]])
    preIslandTask addParameter (archive -> evolution.initialArchive)

    val preIslandCapsule = Capsule(preIslandTask)

    val islandSlot = Slot(MoleTask(name + "MoleTask", model))

    val scalingIndividualsTask = ScalingGAIndividualsTask(evolution)(name + "ScalingIndividuals", individual.toArray)

    scalingIndividualsTask addInput archive
    scalingIndividualsTask addInput terminated
    scalingIndividualsTask addInput state
    scalingIndividualsTask addInput generation
    scalingIndividualsTask addOutput archive
    scalingIndividualsTask addOutput individual.toArray
    scalingIndividualsTask addOutput terminated
    scalingIndividualsTask addOutput state
    scalingIndividualsTask addOutput generation
    val scalingIndividualsSlot = Slot(scalingIndividualsTask)

    val selectIndividualsTask = SelectIndividualsTask(evolution)(
      name + "Breeding",
      individual.toArray,
      sampling)

    selectIndividualsTask addInput archive
    selectIndividualsTask addOutput archive

    val skel =
      firstCapsule -<
        (preIslandCapsule, size = Some(number.toString)) --
        islandSlot --
        renameIndividualsTask --
        elitismCaps --
        terminationSlot --
        scalingIndividualsSlot >| (endCapsule, terminated.name + " == true")

    val loop =
      scalingIndividualsSlot --
        selectIndividualsTask --
        preIslandCapsule

    val dataChannels =
      (firstCapsule oo islandSlot) +
        (firstCapsule -- endCapsule)

    val puzzle = skel + loop + dataChannels

    val (_state, _generation) = (state, generation)

    new Puzzle(puzzle) with GAPuzzle[AG] {
      val evolution = model.evolution
      def output = scalingIndividualsSlot.capsule
      def state = _state
      def generation = _generation
      def genome = model.genome.asInstanceOf[Prototype[evolution.G]]
      def island = islandSlot.capsule
      def archive = model.archive.asInstanceOf[Prototype[evolution.A]]
      def individual = model.individual.asInstanceOf[Prototype[Individual[evolution.G, evolution.P, evolution.F]]]
    }
  }

}
