/*
 * RobolabSim
 * Copyright (C) 2013  Max Leuthaeuser
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/].
 */

package tud.robolab.model

import PointJsonProtocol._
import spray.json._
import Direction._

/** Case class representing a maze. Points (see [[tud.robolab.model.Point]]) are stored in a Seq.
  *
  * @param data the initial Seq of points ([[tud.robolab.model.Point]]) representing all intersections
  * @param robot an instance of [[tud.robolab.model.Robot]] representing the initial position.
  */
case class Maze(private val data: Seq[Seq[Option[Point]]], robot: Robot = Robot()) extends Observer[Point] {
  assert(data != null && data(0) != null)
  assert(data != None && data(0) != None)
  robotPosition(robot.x, robot.y)

  /**
   * @return the height of this maze as Int
   */
  def height: Int = data(0).size

  /**
   * @return the width of this maze as Int
   */
  def width: Int = data.size

  /**
   * @param x x coordinate (min: 0, max: width)
   * @param y y coordinate (min: 0, max: height)
   * @return the [[tud.robolab.model.Point]] at the given coordinates if there is one.
   */
  def apply(x: Int)(y: Int): Option[Point] = data(x)(y)

  /**
   * @return all points ([[tud.robolab.model.Point]]) as Seq
   */
  def points: Seq[Seq[Option[Point]]] = data

  /**
   * Set the robot position.
   * @param x x coordinate (min: 0, max: width)
   * @param y y coordinate (min: 0, max: height)
   * @return true if `x` and `y` are representing a valid position, false otherwise
   */
  def robotPosition(x: Int, y: Int): Boolean = {
    if (x >= width || y >= height || x < 0 || y < 0) return false
    data(robot.x)(robot.y).get.robot = false
    robot.x = x
    robot.y = y
    data(x)(y).get.robot = true
    true
  }

  /**
   * @param p an instance of [[tud.robolab.model.Point]]
   * @return the coordinates for `p` as a Tuple of the type `(Int, Int)`
   */
  private def getXY(p: Point): Option[(Int, Int)] = {
    (0 to width - 1).foreach(x => {
      (0 to height - 1).foreach(y => {
        val r = data(x)(y)
        if (r.isDefined && r.get == p) {
          return Option((x, y))
        }
      })
    })
    Option.empty
  }

  /** Calculates the neighbour for the [[tud.robolab.model.Point]] `p` in the given
    * [[tud.robolab.model.Direction.Direction]] `dir`.
    *
    * @param p an instance of [[tud.robolab.model.Point]]
    * @param dir an instance of [[tud.robolab.model.Direction.Direction]]
    * @return the neighbour for the [[tud.robolab.model.Point]] `p` in the given [[tud.robolab.model.Direction.Direction]] `dir`
    */
  private def neighbour(p: Point, dir: Direction): Option[Point] = dir match {
    case NORTH => {
      val c = getXY(p)
      if (!c.isDefined) return Option.empty
      val (x, y) = c.get
      if (x == 0) return Option.empty
      data(x - 1)(y)
    }
    case EAST => {
      val c = getXY(p)
      if (!c.isDefined) return Option.empty
      val (x, y) = c.get
      if (y == height - 1) return Option.empty
      data(x)(y + 1)
    }
    case SOUTH => {
      val c = getXY(p)
      if (!c.isDefined) return Option.empty
      val (x, y) = c.get
      if (x == width - 1) return Option.empty
      data(x + 1)(y)
    }
    case WEST => {
      val c = getXY(p)
      if (!c.isDefined) return Option.empty
      val (x, y) = c.get
      if (y == 0) return Option.empty
      data(x)(y - 1)
    }
    case _ => throw new IllegalArgumentException
  }

  def receiveUpdate(subject: Point) {
    Direction.values.foreach(d => subject.has(d) match {
      case true => {
        val n = neighbour(subject, d)
        if (n.isDefined)
          n.get +(Direction.opposite(d), notify = false)
      }
      case false => {
        val n = neighbour(subject, d)
        if (n.isDefined)
          n.get -(Direction.opposite(d), notify = false)
      }
    })
  }
}

/** Companion object for [[tud.robolab.model.Maze]] functioning as factory. */
object Maze {
  /**
   * @param width the width of the new maze
   * @param height the height of the new maze
   * @return a new [[tud.robolab.model.Maze]] with the given `width` and `height`
   */
  def empty(width: Int, height: Int): Maze = {
    val max_x = width - 1
    val max_y = height - 1
    Maze((0 to max_x).map(x =>
      (0 to max_y).map(y => {
        val p = (x, y) match {
          case (0, 0) => Point(Seq(SOUTH, EAST))
          case (xs, xy) if xs == max_x && xy == max_y => Point(Seq(NORTH, WEST))
          case (xs, xy) if xs == 0 && xy == max_y => Point(Seq(WEST, SOUTH))
          case (xs, xy) if xs == max_x && xy == 0 => Point(Seq(EAST, NORTH))
          case (xs, xy) if xs == max_x => Point(Seq(NORTH, EAST, WEST))
          case (xs, xy) if xy == max_y => Point(Seq(SOUTH, WEST, NORTH))
          case (xs, xy) if xs == 0 => Point(Seq(SOUTH, EAST, WEST))
          case (xs, xy) if xy == 0 => Point(Seq(SOUTH, EAST, NORTH))
          case _ => Point()
        }
        Option(p)
      }).toSeq
    ).toSeq)
  }

  /**
   * @return a new [[tud.robolab.model.Maze]] with the `width` = 6 and `height` = 6
   */
  def empty: Maze = empty(6, 6)
}

/** Implicit conversions from [[tud.robolab.model.Maze]] to json. */
object MazeJsonProtocol extends DefaultJsonProtocol {

  implicit object MazeJsonFormat extends RootJsonFormat[Maze] {
    def write(p: Maze) = {
      val points: List[List[JsValue]] = p.points.map(ys => {
        ys.map(_ match {
          case None => JsString("None")
          case Some(e) => e.toJson
        }).toList
      }).toList

      JsArray(points.map(JsArray(_)))
    }

    def read(value: JsValue) = value match {
      case s: JsArray => {
        val points = s.elements.map(_ match {
          case l: JsArray => l.elements.map(_ match {
            case e if e.compactPrint.contains("None") => Option.empty
            case e => Option(e.convertTo[Point])
          }).toSeq
          case _ => deserializationError("Maze expected!")
        }).toSeq

        Maze(points)
      }
      case _ => deserializationError("Maze expected!")
    }
  }

}
