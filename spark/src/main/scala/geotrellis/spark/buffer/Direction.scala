/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.spark.buffer

sealed trait Direction

case object CenterDirection extends Direction
case object TopDirection extends Direction
case object TopRightDirection extends Direction
case object RightDirection extends Direction
case object BottomRightDirection extends Direction
case object BottomDirection extends Direction
case object BottomLeftDirection extends Direction
case object LeftDirection extends Direction
case object TopLeftDirection extends Direction
