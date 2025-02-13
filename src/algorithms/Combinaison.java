/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/Combinaison.java 2014-10-19 buixuan.
 * ******************************************************/
package algorithms;

import java.util.ArrayList;

import robotsimulator.Brain;
import characteristics.Parameters;
import characteristics.IFrontSensorResult;

public class Combinaison extends Brain {
  //---PARAMETERS---//
  private static final double HEADINGPRECISION = 0.001;

  //---VARIABLES---//
  private boolean turnRightTask,fallBackCoveringFireTask;
  private double endTaskDirection,endMoveTask,distance;
  private static enum Action { NONE, MOVE, MOVEBACK, FIRELEFT, FIRERIGHT, FIRE };
  private static Action[] fallBackCoveringFireScheme = { Action.FIRE, Action.MOVEBACK, Action.FIRELEFT, Action.MOVEBACK, Action.FIRERIGHT, Action.MOVEBACK };
  //private static Action[] fallBackCoveringFireScheme = { Action.FIRE, Action.MOVEBACK};
  private int schemeIndex;

  //---CONSTRUCTORS---//
  public Combinaison() { super(); }

  //---ABSTRACT-METHODS-IMPLEMENTATION---//
  public void activate() {
    turnRightTask=false;
    move();
    sendLogMessage("Moving a head. Waza!");
  }
  public void step() {
    if (fallBackCoveringFireTask) {
      if (distance>endMoveTask) {
        fallBackCoveringFireTask=false;
      } else {
        switch (fallBackCoveringFireScheme[schemeIndex]){
          case MOVEBACK:
            moveBack();
            distance+=Parameters.teamAMainBotSpeed;
            break;
          case FIRE:
            fire(getHeading());
            break;
          case FIRELEFT:
            fire(getHeading()-0.01*Math.PI);
            break;
          case FIRERIGHT:
            fire(getHeading()+0.01*Math.PI);
            break;
        }
        schemeIndex=(schemeIndex+1)%fallBackCoveringFireScheme.length;
      }
      return;
    }
    if (turnRightTask) {
      if (isHeading(endTaskDirection)) {
	turnRightTask=false;
      } else {
	stepTurn(Parameters.Direction.RIGHT);
      }
      return;
    }
    if (detectFront().getObjectType()==IFrontSensorResult.Types.WALL) {
      fallBackCoveringFireTask=false;
      turnRightTask=true;
      endTaskDirection=getHeading()+Parameters.RIGHTTURNFULLANGLE;
      stepTurn(Parameters.Direction.RIGHT);
      sendLogMessage("Iceberg at 12 o'clock. Heading to my three!");
      return;
    }
    if (detectFront().getObjectType()==IFrontSensorResult.Types.OpponentMainBot) {
      turnRightTask=false;
      fallBackCoveringFireTask=true;
      endMoveTask=300;
      moveBack();
      distance=Parameters.teamAMainBotSpeed;
      schemeIndex=0;
      sendLogMessage("Enemy at 12 o'clock. Fall back covering fire for 30cm!");
      return;
    }
    move(); //And what to do when blind blocked?
    sendLogMessage("Moving a head. Waza!");
    return;
  }
  private boolean isHeading(double dir){
    return Math.abs(Math.sin(getHeading()-dir))<HEADINGPRECISION;
  }
}
