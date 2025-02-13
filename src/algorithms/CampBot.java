/* ******************************************************
 * Simovies - Eurobot 2015 Robomovies Simulator.
 * Copyright (C) 2014 <Binh-Minh.Bui-Xuan@ens-lyon.org>.
 * GPL version>=3 <http://www.gnu.org/licenses/>.
 * $Id: algorithms/CampBot.java 2014-11-04 buixuan.
 * ******************************************************/
package algorithms;

import robotsimulator.Brain;
import characteristics.IFrontSensorResult;
import characteristics.Parameters;

public class CampBot extends Brain {
  private boolean turnTask,turnRight,finished,taskOne;
  private double endTaskDirection;
  private int endTaskCounter;
  private static IFrontSensorResult.Types WALL=IFrontSensorResult.Types.WALL;
  private static IFrontSensorResult.Types TEAMMAIN=IFrontSensorResult.Types.TeamMainBot;

  public CampBot() { super(); }

  public void activate() {
    turnTask=true;
    finished=false;
    taskOne=true;
    endTaskDirection=getHeading()+0.4*Math.PI;
    stepTurn(Parameters.Direction.RIGHT);
    sendLogMessage("Moving and healthy.");
  }
  public void step() {
    if (getHealth()<=0) { sendLogMessage("I'm dead.");return; }
    if (finished) { sendLogMessage("Camping point. Task complete.");return; }
    if (turnTask) {
      if (isHeading(endTaskDirection)) {
	turnTask=false;
        if (taskOne) endTaskCounter=200; else endTaskCounter=100;
	move();
      } else {
        stepTurn(Parameters.Direction.RIGHT);
      }
      return;
    }
    if (endTaskCounter>0) {
      endTaskCounter--;
      move();
      return;
    } else {
      taskOne=false;
      finished=(detectFront().getObjectType()==WALL||detectFront().getObjectType()==TEAMMAIN);
      if (finished) return;
      turnTask=true;
      endTaskDirection=getHeading()+0.5*Math.PI;
      stepTurn(Parameters.Direction.RIGHT);
      return;
    }
  }
  private boolean isHeading(double dir){
    return Math.abs(Math.sin(getHeading()-dir))<Parameters.teamBSecondaryBotStepTurnAngle;
  }
}
