/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: characteristics/IRadarResult.java 2014-10-28 buixuan.
 * ******************************************************/
package characteristics;

public interface IRadarResult {
  public static enum Types { OpponentMainBot, OpponentSecondaryBot, TeamMainBot, TeamSecondaryBot, Wreck, BULLET };
  //--------------------------------------------------------//
  //---SIMULATOR-PROVIDED-METHODS---------------------------//
  //------implemented-in-robotsimulator.FrontSensorResult---//
  //--------------------------------------------------------//
  public Types getObjectType(); //return the type of observed object. Object can only be detected is getObjectDistance() is at most the bot's range value
  public double getObjectDirection(); //return the direction of observed object, from center to center
  public double getObjectDistance(); //return the distance of observed object, from center to center
  public double getObjectRadius(); //return the radius of observed object
}
