/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.tool

package statistics {

  trait StatisticsPackage {
    implicit class StatisticIterableOfDoubleDecorator(s: Iterable[Double]) {
      def median: Double = Stat.median(s)
      def medianAbsoluteDeviation = Stat.medianAbsoluteDeviation(s)
      def average = Stat.average(s)
      def meanSquaredError = Stat.meanSquaredError(s)
      def rootMeanSquaredError = Stat.rootMeanSquaredError(s)
    }

    implicit def statisticArrayOfDoubleDecorator(s: Array[Double]) = new StatisticIterableOfDoubleDecorator(s)
  }

}

package object statistics extends StatisticsPackage
